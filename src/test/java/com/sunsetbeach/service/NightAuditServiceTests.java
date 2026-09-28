package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.NightAudit;
import com.sunsetbeach.model.NightAuditBooking;
import com.sunsetbeach.model.NightAuditCloseInput;
import com.sunsetbeach.model.NightAuditClosure;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Far-future dates (2033) keep the fixtures clear of the baseline dump, but the missed lists are
 * property-wide ("on or before date"), so every assertion is about this test's own booking ids,
 * never a list's size. Every test passes an explicit date - none reads the wall clock.
 * {@code @Transactional} rolls the fixtures back; the audit rows {@link AuditLogService#record}
 * commits on its own are removed {@code @AfterTransaction} (see LifecycleEmailSettingsServiceTests).
 */
@SpringBootTest
@Transactional
class NightAuditServiceTests extends AbstractIntegrationTest {

    private static final String ACTOR_EMAIL = "night-audit-test@example.com";

    @Autowired private NightAuditService nightAuditService;
    @Autowired private ReportService reportService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private RoomEntity room;
    private RoomUnitEntity unit;
    private UserEntity cashier;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Night Audit Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by NightAuditServiceTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);

        RoomUnitEntity newUnit = new RoomUnitEntity();
        newUnit.setRoomId(room.getId());
        newUnit.setLabel("NA-" + UUID.randomUUID().toString().substring(0, 8));
        unit = roomUnitRepository.saveAndFlush(newUnit);

        UserEntity newUser = new UserEntity();
        newUser.setEmail("night-audit-" + UUID.randomUUID() + "@example.com");
        newUser.setName("Night Audit Cashier");
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.CASHIER);
        cashier = userRepository.saveAndFlush(newUser);

        StaffPrincipal principal = new StaffPrincipal(cashier.getId(), ACTOR_EMAIL, Role.CASHIER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterTransaction
    void deleteAuditRows() {
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.NIGHT_AUDIT_CLOSED)
                .filter(e -> ACTOR_EMAIL.equals(e.getActorEmail()))
                .toList());
    }

    // --- Checklist ----------------------------------------------------------------------------

    @Test
    void missedArrival_fromYesterday_staysListedOnYesterdayAndToday() {
        BookingEntity late = persistStay(BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED, "2033-03-10", "2033-03-12");

        assertThat(ids(nightAuditService.get("2033-03-10").getMissedArrivals())).contains(late.getId());
        assertThat(ids(nightAuditService.get("2033-03-11").getMissedArrivals())).contains(late.getId());
        // Not yet due the day before.
        assertThat(ids(nightAuditService.get("2033-03-09").getMissedArrivals())).doesNotContain(late.getId());
    }

    @Test
    void missedArrivals_excludeCancelledAndAnyoneAlreadyResolved() {
        BookingEntity cancelled = persistStay(BookingStatus.CANCELLED, OccupancyStatus.EXPECTED, "2033-03-10", "2033-03-12");
        BookingEntity noShow = persistStay(BookingStatus.CONFIRMED, OccupancyStatus.NO_SHOW, "2033-03-10", "2033-03-12");
        BookingEntity inHouse = persistStay(BookingStatus.CONFIRMED, OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-12");

        NightAudit audit = nightAuditService.get("2033-03-10");

        assertThat(ids(audit.getMissedArrivals())).doesNotContain(cancelled.getId(), noShow.getId(), inHouse.getId());
        // Checked in and not due out until the 12th - not a missed departure on the 10th either.
        assertThat(ids(audit.getMissedDepartures())).doesNotContain(inHouse.getId());
    }

    @Test
    void missedDeparture_isListedFromItsCheckOutDateOnward() {
        BookingEntity overstaying = persistStay(BookingStatus.PAID, OccupancyStatus.CHECKED_IN, "2033-03-08", "2033-03-10");

        assertThat(ids(nightAuditService.get("2033-03-09").getMissedDepartures())).doesNotContain(overstaying.getId());
        assertThat(ids(nightAuditService.get("2033-03-10").getMissedDepartures())).contains(overstaying.getId());
        assertThat(ids(nightAuditService.get("2033-03-11").getMissedDepartures())).contains(overstaying.getId());

        NightAuditBooking row = nightAuditService.get("2033-03-10").getMissedDepartures().stream()
                .filter(b -> b.getId().equals(overstaying.getId())).findFirst().orElseThrow();
        assertThat(row.getRoomName()).isEqualTo(room.getName());
        assertThat(row.getRoomUnitLabel().get()).isEqualTo(unit.getLabel());
        assertThat(row.getCheckOut()).isEqualTo("2033-03-10");
    }

    @Test
    void sameDayCheckInAndOut_appearsInNeitherList() {
        BookingEntity done = persistStay(BookingStatus.PAID, OccupancyStatus.CHECKED_OUT, "2033-03-10", "2033-03-11");

        for (String date : List.of("2033-03-10", "2033-03-11", "2033-03-12")) {
            NightAudit audit = nightAuditService.get(date);
            assertThat(ids(audit.getMissedArrivals())).doesNotContain(done.getId());
            assertThat(ids(audit.getMissedDepartures())).doesNotContain(done.getId());
        }
    }

    @Test
    void snapshot_isExactlyTheOccupancyReportTotalForThatSingleDate() {
        // Prorated: 3 nights for ฿3000, one of them on the reviewed date.
        persistStay(BookingStatus.CONFIRMED, OccupancyStatus.CHECKED_IN, "2033-03-09", "2033-03-12", "3000.00");
        persistStay(BookingStatus.NEW, OccupancyStatus.EXPECTED, "2033-03-10", "2033-03-11", "800.00");

        NightAudit audit = nightAuditService.get("2033-03-10");

        assertThat(audit.getSnapshot()).isEqualTo(reportService.occupancy("2033-03-10", "2033-03-10").getTotal());
        assertThat(audit.getSnapshot().getRoomNightsSold()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void invalidDate_isValidationError() {
        assertThatThrownBy(() -> nightAuditService.get("2033-02-30")).isInstanceOf(ValidationException.class);
    }

    // --- Close --------------------------------------------------------------------------------

    @Test
    void close_recordsWhoAndIsReflectedInTheChecklist_andDoesNotRequireAnEmptyList() {
        persistStay(BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED, "2033-03-10", "2033-03-12");
        assertThat(nightAuditService.get("2033-03-10").getClosure().orElse(null)).isNull();

        NightAuditClosure closure = nightAuditService.close(new NightAuditCloseInput("2033-03-10").notes("  genuine no-show, marking tomorrow  "));

        assertThat(closure.getDate()).isEqualTo("2033-03-10");
        assertThat(closure.getClosedByUserId()).isEqualTo(cashier.getId());
        assertThat(closure.getClosedByName()).isEqualTo("Night Audit Cashier");
        assertThat(closure.getNotes().get()).isEqualTo("genuine no-show, marking tomorrow");
        assertThat(nightAuditService.get("2033-03-10").getClosure().get()).isEqualTo(closure);
        // Another date is untouched.
        assertThat(nightAuditService.get("2033-03-11").getClosure().orElse(null)).isNull();
    }

    @Test
    void closingTwoDifferentDates_bothSucceed_butTheSameDateTwiceIsConflict() {
        nightAuditService.close(new NightAuditCloseInput("2033-03-10"));
        nightAuditService.close(new NightAuditCloseInput("2033-03-11"));

        // Last: a unique violation aborts the surrounding (test) transaction.
        assertThatThrownBy(() -> nightAuditService.close(new NightAuditCloseInput("2033-03-10")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("2033-03-10");
    }

    @Test
    void close_isAudited() {
        nightAuditService.close(new NightAuditCloseInput("2033-03-10"));

        List<AuditLogEntity> rows = auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.NIGHT_AUDIT_CLOSED && ACTOR_EMAIL.equals(e.getActorEmail()))
                .toList();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getSummary()).startsWith("Closed night audit for 2033-03-10");
    }

    // --- Fixtures -----------------------------------------------------------------------------

    private BookingEntity persistStay(BookingStatus status, OccupancyStatus occupancy, String checkIn, String checkOut) {
        return persistStay(status, occupancy, checkIn, checkOut, "1000.00");
    }

    private BookingEntity persistStay(BookingStatus status, OccupancyStatus occupancy, String checkIn, String checkOut, String totalPrice) {
        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setRoomId(room.getId());
        booking.setRoomUnitId(unit.getId());
        booking.setGuestName("Night Audit Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.parse(checkIn));
        booking.setCheckOut(LocalDate.parse(checkOut));
        booking.setTotalPrice(new BigDecimal(totalPrice));
        booking.setStatus(status);
        booking.setOccupancyStatus(occupancy);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(saved.getId());
        segment.setRoomId(room.getId());
        segment.setRoomUnitId(unit.getId());
        segment.setCheckIn(LocalDate.parse(checkIn));
        segment.setCheckOut(LocalDate.parse(checkOut));
        segment.setTotalPrice(new BigDecimal(totalPrice));
        segmentRepository.saveAndFlush(segment);
        return saved;
    }

    private static List<String> ids(List<NightAuditBooking> bookings) {
        return bookings.stream().map(NightAuditBooking::getId).toList();
    }
}
