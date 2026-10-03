package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPage;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.BookingSortField;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SortDirection;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.StaffBookingQuoteInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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

/**
 * What the admin "New booking" form and Bookings list lean on: the price preview
 * ({@code POST /bookings/staff/quote}) agreeing with what the create call then stores, linking a
 * guest card picked in the form, the cancellation reason reaching the audit trail, the paged
 * search, and a guest status email reaching a linked card's address when the booking has none.
 *
 * <p>NOT {@code @Transactional}: booking creation commits in {@link BookingWriter}'s own
 * SERIALIZABLE transaction and audit rows commit REQUIRES_NEW, so everything is deleted by hand in
 * {@link #cleanUp}.
 */
@SpringBootTest
class StaffBookingFormSupportTests extends AbstractIntegrationTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2031, 5, 10);

    @Autowired private BookingService bookingService;
    @Autowired private EmailService emailService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository bookingSegmentRepository;
    @Autowired private GuestRepository guestRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private final List<String> bookingIds = new ArrayList<>();
    private final List<String> roomIds = new ArrayList<>();
    private final List<String> guestIds = new ArrayList<>();

    @BeforeEach
    void actAsCashier() {
        StaffPrincipal principal = new StaffPrincipal("staff-form-cashier", "staff-form-cashier@example.com", Role.CASHIER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER"))));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String bookingId : bookingIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.BOOKING, bookingId));
            // Nightly rates go with their segment (ON DELETE CASCADE, V21).
            bookingSegmentRepository.deleteAll(bookingSegmentRepository.findByBookingIdOrderByCheckInAsc(bookingId));
            bookingRepository.findById(bookingId).ifPresent(bookingRepository::delete);
        }
        for (String guestId : guestIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.GUEST, guestId));
            guestRepository.deleteById(guestId);
        }
        for (String roomId : roomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
    }

    // --- price preview ---------------------------------------------------------------------------

    @Test
    void quote_isTheTotalTheCreateCallThenStores() {
        RoomUnitEntity unit = roomWithUnit("1250.00");

        BookingScheduleQuote quote = bookingService.quoteStaffBooking(quoteInput(unit, CHECK_IN, CHECK_IN.plusDays(3)));
        Booking created = staffBooking(unit, "Quote Guest", null, null);

        assertThat(quote.getAvailable()).isTrue();
        assertThat(quote.getNights()).isEqualTo(3);
        assertThat(quote.getTotalPrice()).isEqualTo("3750.00");
        assertThat(created.getTotalPrice()).isEqualTo(quote.getTotalPrice());
    }

    @Test
    void quote_forATakenRoom_saysNo_butStillPrices_andWritesNothing() {
        RoomUnitEntity unit = roomWithUnit("1000.00");
        staffBooking(unit, "Already Here", null, null);
        long bookingsBefore = bookingRepository.count();

        BookingScheduleQuote quote = bookingService.quoteStaffBooking(quoteInput(unit, CHECK_IN.plusDays(1), CHECK_IN.plusDays(2)));

        assertThat(quote.getAvailable()).isFalse();
        assertThat(quote.getReason().get()).isNotBlank();
        assertThat(quote.getTotalPrice()).isEqualTo("1000.00");
        assertThat(bookingRepository.count()).isEqualTo(bookingsBefore);
    }

    // --- picked guest card -----------------------------------------------------------------------

    @Test
    void create_withPickedGuest_linksThatCard_notTheOneTheEmailMatches() {
        RoomUnitEntity unit = roomWithUnit("1000.00");
        String typedEmail = "staff-form-" + UUID.randomUUID() + "@example.com";
        GuestEntity emailMatch = persistGuest("Email Match", typedEmail);
        GuestEntity picked = persistGuest("Picked Card", "picked-" + UUID.randomUUID() + "@example.com");

        Booking booking = staffBooking(unit, "Picked Card", typedEmail, picked.getId());

        assertThat(booking.getGuestId().get()).isEqualTo(picked.getId());
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getGuestId()).isEqualTo(picked.getId());
        assertThat(emailMatch.getId()).isNotEqualTo(picked.getId());
    }

    @Test
    void create_withUnknownGuest_is404_andCreatesNoBooking() {
        RoomUnitEntity unit = roomWithUnit("1000.00");
        long bookingsBefore = bookingRepository.count();

        assertThatThrownBy(() -> staffBooking(unit, "Nobody", null, UUID.randomUUID().toString()))
                .isInstanceOf(NotFoundException.class);
        assertThat(bookingRepository.count()).isEqualTo(bookingsBefore);
    }

    // --- cancellation reason ---------------------------------------------------------------------

    @Test
    void cancellationReason_isRecordedOnTheCancelAuditEntry_andIgnoredOnOtherChanges() {
        RoomUnitEntity unit = roomWithUnit("1000.00");
        Booking booking = staffBooking(unit, "Changing Plans", null, null);

        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CONFIRMED).cancellationReason("not a cancel"));
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CANCELLED).cancellationReason("  Flight cancelled  "));

        List<String> summaries = entriesFor(AuditEntityType.BOOKING, booking.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_STATUS_CHANGED)
                .map(AuditLogEntity::getSummary)
                .toList();
        assertThat(summaries).anyMatch(s -> s.contains("to CANCELLED") && s.endsWith("Reason: Flight cancelled"));
        assertThat(summaries).noneMatch(s -> s.contains("not a cancel"));
    }

    // --- paged search ----------------------------------------------------------------------------

    @Test
    void search_matchesNameEmailOrIdPrefix_sortsByTheChosenColumn_andPages() {
        String token = "srch" + UUID.randomUUID().toString().substring(0, 8);
        Booking zed = staffBooking(roomWithUnit("1000.00"), "Zed " + token, null, null);
        Booking amy = staffBooking(roomWithUnit("1000.00"), "amy " + token, null, null);
        Booking mia = staffBooking(roomWithUnit("1000.00"), "Mia " + token, token + "@example.com", null);

        BookingPage byName = bookingService.search(token, null, null, null, BookingSortField.GUEST_NAME, SortDirection.ASC, 0, 2);
        assertThat(byName.getTotalCount()).isEqualTo(3);
        assertThat(byName.getItems()).extracting(Booking::getId).containsExactly(amy.getId(), mia.getId());
        BookingPage secondPage = bookingService.search(token, null, null, null, BookingSortField.GUEST_NAME, SortDirection.ASC, 1, 2);
        assertThat(secondPage.getItems()).extracting(Booking::getId).containsExactly(zed.getId());

        assertThat(bookingService.search(token + "@EXAMPLE", null, null, null, null, null, 0, 50).getItems())
                .extracting(Booking::getId).containsExactly(mia.getId());
        assertThat(bookingService.search(zed.getId().substring(0, 13), null, null, null, null, null, 0, 50).getItems())
                .extracting(Booking::getId).contains(zed.getId());
        assertThat(bookingService.search(token, null, null, BookingStatus.CANCELLED, null, null, 0, 50).getTotalCount()).isZero();
    }

    // --- guest status email address --------------------------------------------------------------

    @Test
    void guestEmail_fallsBackToTheLinkedCard_onlyWhenTheBookingHasNone() {
        RoomUnitEntity unit = roomWithUnit("1000.00");
        GuestEntity card = persistGuest("Card With Email", "card-" + UUID.randomUUID() + "@example.com");
        Booking walkIn = staffBooking(unit, "Card With Email", null, card.getId());
        BookingEntity walkInEntity = bookingRepository.findById(walkIn.getId()).orElseThrow();

        assertThat(walkInEntity.getGuestEmail()).isNull();
        assertThat(emailService.guestEmailFor(walkInEntity)).isEqualTo(card.getEmail());

        walkInEntity.setGuestEmail("own@example.com");
        assertThat(emailService.guestEmailFor(walkInEntity)).isEqualTo("own@example.com");

        walkInEntity.setGuestEmail(null);
        walkInEntity.setGuestId(null);
        assertThat(emailService.guestEmailFor(walkInEntity)).isNull();
    }

    // --- helpers ---------------------------------------------------------------------------------

    private StaffBookingQuoteInput quoteInput(RoomUnitEntity unit, LocalDate checkIn, LocalDate checkOut) {
        StaffBookingQuoteInput input = new StaffBookingQuoteInput(unit.getRoomId(), checkIn.toString(), checkOut.toString());
        input.roomUnitId(unit.getId());
        return input;
    }

    private Booking staffBooking(RoomUnitEntity unit, String name, String email, String guestId) {
        StaffBookingCreateInput input = new StaffBookingCreateInput(
                unit.getRoomId(), name, CHECK_IN.toString(), CHECK_IN.plusDays(3).toString(), BookingChannel.WALK_IN, 1);
        input.roomUnitId(unit.getId());
        if (email != null) {
            input.guestEmail(email);
        }
        if (guestId != null) {
            input.guestId(guestId);
        }
        Booking booking = bookingService.createStaffBooking(input);
        bookingIds.add(booking.getId());
        if (booking.getGuestId().get() != null && !guestIds.contains(booking.getGuestId().get())) {
            guestIds.add(booking.getGuestId().get());
        }
        return booking;
    }

    private GuestEntity persistGuest(String name, String email) {
        GuestEntity guest = new GuestEntity();
        guest.setName(name);
        guest.setEmail(email);
        GuestEntity saved = guestRepository.saveAndFlush(guest);
        guestIds.add(saved.getId());
        return saved;
    }

    private RoomUnitEntity roomWithUnit(String basePrice) {
        RoomEntity room = new RoomEntity();
        room.setName("Staff Form Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by StaffBookingFormSupportTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal(basePrice));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        roomIds.add(saved.getId());
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(saved.getId());
        unit.setLabel("SF-" + UUID.randomUUID().toString().substring(0, 8));
        unit.setActive(true);
        return roomUnitRepository.saveAndFlush(unit);
    }

    private List<AuditLogEntity> entriesFor(AuditEntityType entityType, String entityId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == entityType && entityId.equals(e.getEntityId()))
                .toList();
    }
}
