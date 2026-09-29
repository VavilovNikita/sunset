package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.BookingSegmentNightlyRateEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.JournalEntryEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.JournalSourceType;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.SiteMinderImportAction;
import com.sunsetbeach.model.SiteMinderImportResult;
import com.sunsetbeach.model.SiteMinderReservationInput;
import com.sunsetbeach.model.SiteMinderReservationStatus;
import com.sunsetbeach.model.SiteMinderRoomTypeMappingInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentNightlyRateRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.JournalEntryRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.SiteMinderRoomTypeMappingRepository;
import com.sunsetbeach.security.IntegrationPrincipal;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The SiteMinder reservation import end to end against the real database: idempotency on the
 * SiteMinder reference, updates through the staff paths, cancellation with the same downstream
 * effects as a staff cancellation (room released, ledger reversed, counted by the manager report),
 * the unmapped-room-type rejection, and the name-only guest card.
 *
 * <p>Not {@code @Transactional}: the import deliberately runs several independently committed
 * writes (see {@link SiteMinderImportService}'s javadoc), the race test needs real commits, and
 * audit rows commit on their own anyway. Everything created is removed by id in {@link #cleanUp}.
 * Each test uses its own room type, and stay dates far in the future, so nothing here reads or
 * contends with the real clock or another test's inventory.
 */
@SpringBootTest
class SiteMinderImportServiceTests extends AbstractIntegrationTest {

    @Autowired private SiteMinderImportService importService;
    @Autowired private SiteMinderRoomTypeMappingService mappingService;
    @Autowired private BookingService bookingService;
    @Autowired private ReportService reportService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private BookingSegmentNightlyRateRepository nightlyRateRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private GuestRepository guestRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private SiteMinderRoomTypeMappingRepository mappingRepository;
    @Autowired private Clock clock;

    private static final OffsetDateTime BOOKED_AT = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.ofHours(7));

    private final List<String> roomIds = new ArrayList<>();
    private final List<String> guestIds = new ArrayList<>();

    @BeforeEach
    void actAsSiteMinder() {
        actAs(siteMinderAuthentication());
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String roomId : roomIds) {
            for (BookingEntity booking : bookingRepository.findAll().stream().filter(b -> roomId.equals(b.getRoomId())).toList()) {
                auditLogRepository.deleteAll(auditEntries(booking.getId()));
                if (booking.getGuestId() != null) {
                    guestIds.add(booking.getGuestId());
                }
                bookingRepository.deleteById(booking.getId());
            }
            mappingRepository.findAll().stream().filter(m -> roomId.equals(m.getRoomId())).forEach(mappingRepository::delete);
            for (RoomUnitEntity unit : roomUnitRepository.findByRoomId(roomId)) {
                roomUnitRepository.deleteById(unit.getId());
            }
            roomRepository.deleteById(roomId);
        }
        for (String guestId : guestIds) {
            auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                    .filter(e -> e.getEntityType() == AuditEntityType.GUEST && guestId.equals(e.getEntityId()))
                    .toList());
            guestRepository.deleteById(guestId);
        }
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.SITEMINDER_ROOM_TYPE_MAPPING && e.getSummary().contains("SM Test"))
                .toList());
    }

    // --- Fixtures -----------------------------------------------------------------------------

    private static UsernamePasswordAuthenticationToken siteMinderAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                IntegrationPrincipal.SITEMINDER, null, List.of(new SimpleGrantedAuthority(IntegrationPrincipal.SITEMINDER_AUTHORITY)));
    }

    private static UsernamePasswordAuthenticationToken staffAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new StaffPrincipal("sm-test-manager", "sm-test-manager@example.com", Role.MANAGER), null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }

    private static void actAs(UsernamePasswordAuthenticationToken authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** A room type with {@code units} active units, mapped from the SiteMinder name {@code "SM Test <uuid>"}. Returns that name. */
    private RoomEntity room(int units) {
        RoomEntity room = new RoomEntity();
        room.setName("SM Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by SiteMinderImportServiceTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        roomIds.add(saved.getId());
        for (int i = 0; i < units; i++) {
            RoomUnitEntity unit = new RoomUnitEntity();
            unit.setRoomId(saved.getId());
            unit.setLabel("SM " + UUID.randomUUID());
            unit.setActive(true);
            roomUnitRepository.saveAndFlush(unit);
        }
        return saved;
    }

    private String mapped(RoomEntity room) {
        String name = "SM Test " + UUID.randomUUID() + " Villa ABF";
        mappingService.create(new SiteMinderRoomTypeMappingInput(name, room.getId()));
        return name;
    }

    private static SiteMinderReservationInput reservation(String reference, SiteMinderReservationStatus status, String roomTypeName) {
        return new SiteMinderReservationInput(
                        reference, status, "Traveller", "2035-03-10", "2035-03-13", roomTypeName, 2, "9000.00", "Booking.com", BOOKED_AT)
                .firstName("Jane");
    }

    private static String newReference() {
        return "SM-" + UUID.randomUUID();
    }

    private BookingEntity bookingFor(String reference) {
        return bookingRepository.findBySourceAndExternalReference(BookingSource.SITEMINDER, reference).orElseThrow();
    }

    private long bookingsWithReference(String reference) {
        return bookingRepository.findAll().stream().filter(b -> reference.equals(b.getExternalReference())).count();
    }

    private List<BigDecimal> nightlyPrices(String bookingId) {
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        assertThat(segments).hasSize(1);
        return nightlyRateRepository.findBySegmentIdOrderByDateAsc(segments.get(0).getId()).stream()
                .map(BookingSegmentNightlyRateEntity::getPrice)
                .toList();
    }

    private List<AuditLogEntity> auditEntries(String bookingId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.BOOKING && bookingId.equals(e.getEntityId()))
                .toList();
    }

    // --- Idempotency --------------------------------------------------------------------------

    @Test
    void booked_createsConfirmedBookingAtSiteMindersFrozenPrice() {
        RoomEntity room = room(2);
        String reference = newReference();

        SiteMinderImportResult result = importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, mapped(room)));

        assertThat(result.getAction()).isEqualTo(SiteMinderImportAction.CREATED);
        BookingEntity booking = bookingFor(reference);
        assertThat(booking.getId()).isEqualTo(result.getBookingId().orElse(null));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getRoomId()).isEqualTo(room.getId());
        assertThat(booking.getGuestName()).isEqualTo("Jane Traveller");
        assertThat(booking.getGuestEmail()).isNull();
        assertThat(booking.getGuestPhone()).isNull();
        assertThat(booking.getChannel()).isEqualTo(BookingChannel.BOOKING_COM);
        assertThat(booking.getExternalChannel()).isEqualTo("Booking.com");
        assertThat(booking.getAdults()).isEqualTo(2);
        // SiteMinder's 9000.00 over 3 nights, not this room's 1000.00/night base rate.
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("9000.00");
        assertThat(nightlyPrices(booking.getId())).extracting(BigDecimal::toPlainString).containsExactly("3000.00", "3000.00", "3000.00");

        List<AuditLogEntity> audit = auditEntries(booking.getId());
        assertThat(audit).singleElement().satisfies(e -> {
            assertThat(e.getAction()).isEqualTo(AuditAction.BOOKING_CREATED);
            assertThat(e.getActorUserId()).isEqualTo("SITEMINDER");
            assertThat(e.getActorEmail()).isEqualTo("siteminder@sunsetbeach.internal");
            assertThat(e.getActorRole()).isNull();
            assertThat(e.getSummary()).contains(reference).contains("Booking.com");
        });
    }

    @Test
    void sameReservationTwice_secondIsUnchanged_noDuplicateBooking() {
        String roomType = mapped(room(2));
        String reference = newReference();

        SiteMinderImportResult first = importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType));
        BookingEntity afterFirst = bookingFor(reference);
        SiteMinderImportResult second = importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType));

        assertThat(second.getAction()).isEqualTo(SiteMinderImportAction.UNCHANGED);
        assertThat(second.getBookingId().orElse(null)).isEqualTo(first.getBookingId().orElse(null));
        assertThat(bookingsWithReference(reference)).isEqualTo(1);
        // Nothing written at all - not even updatedAt.
        assertThat(bookingFor(reference).getUpdatedAt()).isEqualTo(afterFirst.getUpdatedAt());
    }

    @Test
    void modified_updatesTheSameBookingInPlace_throughTheStaffPaths() {
        String roomType = mapped(room(2));
        String reference = newReference();
        String bookingId = importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType)).getBookingId().orElse(null);

        SiteMinderReservationInput modified = reservation(reference, SiteMinderReservationStatus.MODIFIED, roomType)
                .checkOut("2035-03-14")
                .totalPrice("12000.00")
                .adults(3)
                .children(1)
                .modifiedAt(BOOKED_AT.plusDays(2));
        SiteMinderImportResult result = importService.importReservation(modified);

        assertThat(result.getAction()).isEqualTo(SiteMinderImportAction.UPDATED);
        assertThat(result.getBookingId().orElse(null)).isEqualTo(bookingId);
        assertThat(result.getChanges()).hasSize(3);
        assertThat(bookingsWithReference(reference)).isEqualTo(1);
        BookingEntity booking = bookingFor(reference);
        assertThat(booking.getCheckOut()).isEqualTo(LocalDate.of(2035, 3, 14));
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("12000.00");
        assertThat(nightlyPrices(bookingId)).extracting(BigDecimal::toPlainString).containsExactly("3000.00", "3000.00", "3000.00", "3000.00");
        assertThat(booking.getAdults()).isEqualTo(3);
        assertThat(booking.getChildren()).isEqualTo(1);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(auditEntries(bookingId))
                .filteredOn(e -> e.getAction() != AuditAction.BOOKING_CREATED)
                .allSatisfy(e -> assertThat(e.getActorUserId()).isEqualTo("SITEMINDER"))
                .extracting(AuditLogEntity::getAction)
                .containsExactlyInAnyOrder(AuditAction.BOOKING_SCHEDULE_CHANGED, AuditAction.BOOKING_REPRICED, AuditAction.BOOKING_STATUS_CHANGED);
    }

    @Test
    void dateChange_respreadsTheAgreedTotal_evenWhenOurRateWouldHaveMatchedIt() {
        String roomType = mapped(room(2));
        String reference = newReference();
        String bookingId = importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType)).getBookingId().orElse(null);

        // One more night; SiteMinder's new total (10000) equals the old 9000 plus this room's own
        // 1000.00 base rate for the added night - updateSchedule alone would already sum to it.
        importService.importReservation(reservation(reference, SiteMinderReservationStatus.MODIFIED, roomType)
                .checkOut("2035-03-14")
                .totalPrice("10000.00")
                .modifiedAt(BOOKED_AT.plusDays(1)));

        assertThat(nightlyPrices(bookingId)).extracting(BigDecimal::toPlainString).containsExactly("2500.00", "2500.00", "2500.00", "2500.00");
    }

    @Test
    void olderVersionThanTheOneApplied_isSkippedAsStale() {
        String roomType = mapped(room(2));
        String reference = newReference();
        importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType));
        importService.importReservation(reservation(reference, SiteMinderReservationStatus.MODIFIED, roomType)
                .totalPrice("9500.00")
                .modifiedAt(BOOKED_AT.plusHours(2)));

        SiteMinderImportResult stale = importService.importReservation(reservation(reference, SiteMinderReservationStatus.MODIFIED, roomType)
                .totalPrice("7000.00")
                .modifiedAt(BOOKED_AT.plusHours(1)));

        assertThat(stale.getAction()).isEqualTo(SiteMinderImportAction.SKIPPED);
        assertThat(bookingFor(reference).getTotalPrice()).isEqualByComparingTo("9500.00");
    }

    @Test
    void concurrentImportsOfTheSameNewReservation_createExactlyOneBooking() throws Exception {
        String roomType = mapped(room(5));
        String reference = newReference();
        SiteMinderReservationInput input = reservation(reference, SiteMinderReservationStatus.BOOKED, roomType);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> attempt = () -> {
            actAs(siteMinderAuthentication());
            barrier.await(10, TimeUnit.SECONDS);
            try {
                return importService.importReservation(input);
            } catch (ConflictException e) {
                return e;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Object> outcomes = new ArrayList<>();
        try {
            List<Future<Object>> futures = List.of(pool.submit(attempt), pool.submit(attempt));
            for (Future<Object> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(bookingsWithReference(reference)).isEqualTo(1);
        // One creates it; the other either lost the race (409, retried next poll) or ran after it
        // committed and found nothing to change.
        assertThat(outcomes).filteredOn(o -> o instanceof SiteMinderImportResult r && r.getAction() == SiteMinderImportAction.CREATED).hasSize(1);
    }

    // --- Cancellation -------------------------------------------------------------------------

    @Test
    void cancelled_hasTheSameEffectsAsAStaffCancellation() {
        RoomEntity room = room(1);
        String roomType = mapped(room);
        String reference = newReference();
        String bookingId = importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType)).getBookingId().orElse(null);

        // The only unit is taken: a staff booking for the same nights is refused.
        actAs(staffAuthentication());
        StaffBookingCreateInput sameNights = new StaffBookingCreateInput(room.getId(), "Walk In", "2035-03-10", "2035-03-13", BookingChannel.WALK_IN, 1);
        assertThatThrownBy(() -> bookingService.createStaffBooking(sameNights)).isInstanceOf(ConflictException.class);
        // Staff take payment - the ledger posts the room settlement.
        bookingService.updateStatus(bookingId, new BookingStatusInput(BookingStatus.PAID));
        assertThat(journalEntryRepository.findBySourceTypeAndSourceIdOrderByCreatedAtAsc(JournalSourceType.BOOKING, bookingId)).hasSize(1);
        String today = LocalDate.now(clock).toString();
        int cancellationsBefore = reportService.manager(today).getToday().getAccounts().getCancellations();

        actAs(siteMinderAuthentication());
        SiteMinderImportResult result = importService.importReservation(reservation(reference, SiteMinderReservationStatus.CANCELLED, roomType)
                .cancelledAt(BOOKED_AT.plusDays(3)));

        assertThat(result.getAction()).isEqualTo(SiteMinderImportAction.CANCELLED);
        assertThat(bookingFor(reference).getStatus()).isEqualTo(BookingStatus.CANCELLED);
        // Ledger: the settlement is reversed, exactly as when staff move a PAID booking to CANCELLED.
        List<JournalEntryEntity> entries = journalEntryRepository.findBySourceTypeAndSourceIdOrderByCreatedAtAsc(JournalSourceType.BOOKING, bookingId);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(1).getReversesEntryId()).isEqualTo(entries.get(0).getId());
        // Counted in the manager report's cancellations for today.
        assertThat(reportService.manager(today).getToday().getAccounts().getCancellations()).isEqualTo(cancellationsBefore + 1);
        // Audited like a staff cancellation, attributed to SiteMinder.
        assertThat(auditEntries(bookingId))
                .filteredOn(e -> e.getActorUserId().equals("SITEMINDER") && e.getAction() == AuditAction.BOOKING_STATUS_CHANGED)
                .singleElement()
                .satisfies(e -> assertThat(e.getSummary()).contains("PAID to CANCELLED"));
        // The room is released: the same staff booking now succeeds.
        actAs(staffAuthentication());
        Booking walkIn = bookingService.createStaffBooking(sameNights);
        assertThat(walkIn.getRoomId()).isEqualTo(room.getId());
    }

    @Test
    void cancelledAgain_writesNothing() {
        String roomType = mapped(room(2));
        String reference = newReference();
        importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType));
        SiteMinderReservationInput cancelled = reservation(reference, SiteMinderReservationStatus.CANCELLED, roomType).cancelledAt(BOOKED_AT.plusDays(1));
        importService.importReservation(cancelled);
        BookingEntity afterCancel = bookingFor(reference);

        SiteMinderImportResult again = importService.importReservation(cancelled);

        assertThat(again.getAction()).isEqualTo(SiteMinderImportAction.UNCHANGED);
        // updatedAt is what the manager report dates a cancellation by - a re-poll must not move it.
        assertThat(bookingFor(reference).getUpdatedAt()).isEqualTo(afterCancel.getUpdatedAt());
    }

    @Test
    void bookedAgainAfterACancellationHere_isNotReinstated() {
        String roomType = mapped(room(2));
        String reference = newReference();
        String bookingId = importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType)).getBookingId().orElse(null);
        actAs(staffAuthentication());
        bookingService.updateStatus(bookingId, new BookingStatusInput(BookingStatus.CANCELLED));
        actAs(siteMinderAuthentication());

        SiteMinderImportResult result = importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType));

        assertThat(result.getAction()).isEqualTo(SiteMinderImportAction.SKIPPED);
        assertThat(bookingFor(reference).getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void modifiedOrCancelled_forAReservationNeverImported_isSkipped() {
        String roomType = mapped(room(2));
        String modifiedRef = newReference();
        String cancelledRef = newReference();

        SiteMinderImportResult modified = importService.importReservation(reservation(modifiedRef, SiteMinderReservationStatus.MODIFIED, roomType));
        SiteMinderImportResult cancelled = importService.importReservation(reservation(cancelledRef, SiteMinderReservationStatus.CANCELLED, roomType));

        assertThat(modified.getAction()).isEqualTo(SiteMinderImportAction.SKIPPED);
        assertThat(modified.getBookingId().orElse(null)).isNull();
        assertThat(modified.getMessage().orElse(null)).contains(modifiedRef);
        assertThat(cancelled.getAction()).isEqualTo(SiteMinderImportAction.SKIPPED);
        assertThat(bookingsWithReference(modifiedRef) + bookingsWithReference(cancelledRef)).isZero();
    }

    // --- Rejections ---------------------------------------------------------------------------

    @Test
    void unmappedRoomType_isRejectedNamingIt_andCreatesNothing() {
        room(2);
        String reference = newReference();
        String unmapped = "SM Test Unmapped " + UUID.randomUUID() + " Room ABF";

        assertThatThrownBy(() -> importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, unmapped)))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors().get("roomTypeName")).singleElement().asString().contains(unmapped));
        assertThat(bookingsWithReference(reference)).isZero();
    }

    @Test
    void roomTypeNames_matchIgnoringCaseAndSpacing() {
        RoomEntity room = room(2);
        String roomType = mapped(room);
        String reference = newReference();

        importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, "  " + roomType.toUpperCase().replace(" ", "   ") + " "));

        assertThat(bookingFor(reference).getRoomId()).isEqualTo(room.getId());
    }

    @Test
    void noAvailability_isAConflict_andCreatesNothing() {
        String roomType = mapped(room(1));
        importService.importReservation(reservation(newReference(), SiteMinderReservationStatus.BOOKED, roomType));
        String second = newReference();

        assertThatThrownBy(() -> importService.importReservation(reservation(second, SiteMinderReservationStatus.BOOKED, roomType)))
                .isInstanceOf(ConflictException.class);
        assertThat(bookingsWithReference(second)).isZero();
    }

    @Test
    void roomTypeChangedInSiteMinder_isAConflict_bookingUntouched() {
        String firstType = mapped(room(2));
        String otherType = mapped(room(2));
        String reference = newReference();
        importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, firstType));

        assertThatThrownBy(() -> importService.importReservation(
                        reservation(reference, SiteMinderReservationStatus.MODIFIED, otherType).totalPrice("1.00").modifiedAt(BOOKED_AT.plusDays(1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(otherType);
        assertThat(bookingFor(reference).getTotalPrice()).isEqualByComparingTo("9000.00");
    }

    @Test
    void aPriceInAnotherCurrency_isRejected() {
        String roomType = mapped(room(2));

        assertThatThrownBy(() -> importService.importReservation(reservation(newReference(), SiteMinderReservationStatus.BOOKED, roomType).currency("USD")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void dateChange_whenTheAssignedRoomIsTaken_unassignsItWithAWarning() {
        RoomEntity room = room(2);
        String roomType = mapped(room);
        String reference = newReference();
        String bookingId = importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType)).getBookingId().orElse(null);
        List<RoomUnitEntity> units = roomUnitRepository.findByRoomId(room.getId());
        actAs(staffAuthentication());
        bookingService.assignRoomUnit(bookingId, new RoomUnitAssignmentInput().roomUnitId(units.get(0).getId()));
        // Someone else holds that unit on the night the SiteMinder change would add.
        Booking other = bookingService.createStaffBooking(new StaffBookingCreateInput(room.getId(), "Other", "2035-03-13", "2035-03-14", BookingChannel.PHONE, 1)
                .roomUnitId(units.get(0).getId()));
        assertThat(other.getRoomUnitId().orElse(null)).isEqualTo(units.get(0).getId());
        actAs(siteMinderAuthentication());

        SiteMinderImportResult result = importService.importReservation(reservation(reference, SiteMinderReservationStatus.MODIFIED, roomType)
                .checkOut("2035-03-14")
                .modifiedAt(BOOKED_AT.plusDays(1)));

        assertThat(result.getAction()).isEqualTo(SiteMinderImportAction.UPDATED);
        assertThat(result.getWarnings()).singleElement().asString().contains(units.get(0).getLabel());
        BookingEntity booking = bookingFor(reference);
        assertThat(booking.getCheckOut()).isEqualTo(LocalDate.of(2035, 3, 14));
        assertThat(booking.getRoomUnitId()).isNull();
    }

    // --- Guest cards --------------------------------------------------------------------------

    @Test
    void guestCard_isNewAndNameOnly_neverMatchedToAnExistingCardByName() {
        String roomType = mapped(room(3));
        GuestEntity existing = new GuestEntity();
        existing.setName("Jane Traveller");
        existing.setEmail("jane.traveller." + UUID.randomUUID() + "@example.com");
        existing = guestRepository.saveAndFlush(existing);
        guestIds.add(existing.getId());

        String firstRef = newReference();
        String secondRef = newReference();
        importService.importReservation(reservation(firstRef, SiteMinderReservationStatus.BOOKED, roomType));
        importService.importReservation(reservation(secondRef, SiteMinderReservationStatus.BOOKED, roomType));

        String firstGuestId = bookingFor(firstRef).getGuestId();
        String secondGuestId = bookingFor(secondRef).getGuestId();
        // Same name three times, three different cards: the known cost of having no contacts yet.
        assertThat(firstGuestId).isNotNull().isNotEqualTo(existing.getId());
        assertThat(secondGuestId).isNotNull().isNotEqualTo(existing.getId()).isNotEqualTo(firstGuestId);
        GuestEntity card = guestRepository.findById(firstGuestId).orElseThrow();
        assertThat(card.getName()).isEqualTo("Jane Traveller");
        assertThat(card.getEmail()).isNull();
        assertThat(card.getPhone()).isNull();

        // What a later enrichment pass will do once SiteMinder unmasks contacts: find the card by
        // the booking's reference and fill them in - a plain update, nothing in the way.
        GuestEntity enriched = guestRepository.findById(bookingFor(firstRef).getGuestId()).orElseThrow();
        enriched.setEmail("jane@example.com");
        enriched.setPhone("+66800000001");
        guestRepository.saveAndFlush(enriched);
        assertThat(guestRepository.findById(firstGuestId).orElseThrow().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void repeatImport_keepsTheSameGuestCard() {
        String roomType = mapped(room(2));
        String reference = newReference();
        importService.importReservation(reservation(reference, SiteMinderReservationStatus.BOOKED, roomType));
        String guestId = bookingFor(reference).getGuestId();

        importService.importReservation(reservation(reference, SiteMinderReservationStatus.MODIFIED, roomType).adults(1).modifiedAt(BOOKED_AT.plusDays(1)));

        assertThat(bookingFor(reference).getGuestId()).isEqualTo(guestId);
    }

    // --- Pure mapping -------------------------------------------------------------------------

    @Test
    void channelNames_mapToExistingValuesOnly() {
        assertThat(SiteMinderImportService.mapChannel("Booking.com")).isEqualTo(BookingChannel.BOOKING_COM);
        assertThat(SiteMinderImportService.mapChannel("Expedia Collect")).isEqualTo(BookingChannel.EXPEDIA);
        assertThat(SiteMinderImportService.mapChannel("Agoda")).isEqualTo(BookingChannel.AGODA);
        assertThat(SiteMinderImportService.mapChannel("Airbnb")).isEqualTo(BookingChannel.AIRBNB);
        assertThat(SiteMinderImportService.mapChannel("SiteMinder Booking Engine")).isEqualTo(BookingChannel.DIRECT);
        assertThat(SiteMinderImportService.mapChannel("Direct")).isEqualTo(BookingChannel.DIRECT);
        assertThat(SiteMinderImportService.mapChannel("Trip.com")).isEqualTo(BookingChannel.OTHER);
    }

    @Test
    void agreedTotal_isSpreadSoTheNightsSumExactly() {
        var prices = BookingWriter.spreadEvenly(
                new BigDecimal("1000.00"), List.of(LocalDate.of(2035, 1, 1), LocalDate.of(2035, 1, 2), LocalDate.of(2035, 1, 3)));

        assertThat(prices.values()).extracting(BigDecimal::toPlainString).containsExactly("333.33", "333.33", "333.34");
    }
}
