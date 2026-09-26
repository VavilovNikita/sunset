package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingGuestLinkInput;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.GuestAccountRegisterInput;
import com.sunsetbeach.model.GuestBookingView;
import com.sunsetbeach.model.GuestDetail;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@link GuestLinkService}: Guest as the one person record GuestAccount and Booking both point at
 * - find-or-create by email, never overwriting an existing card, never guessing between
 * duplicates - plus the two readers that now follow the link ({@code GET /guest/bookings},
 * {@code GuestDetail.account}).
 *
 * <p>NOT {@code @Transactional}: the booking path links in its own transaction after the booking
 * commits (see {@code BookingService#linkGuestQuietly}), and auto-created cards are audited via
 * {@code AuditLogService}'s REQUIRES_NEW - both would escape a test rollback anyway. Everything is
 * deleted explicitly in {@link #cleanUp}.
 */
@SpringBootTest
class GuestLinkTests extends AbstractIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private GuestAccountService guestAccountService;
    @Autowired private GuestService guestService;
    @Autowired private GuestRepository guestRepository;
    @Autowired private GuestAccountRepository guestAccountRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository bookingSegmentRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private final List<String> emails = new ArrayList<>();
    private final List<String> bookingIds = new ArrayList<>();
    private final List<String> roomIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String bookingId : bookingIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.BOOKING, bookingId));
            bookingSegmentRepository.deleteAll(bookingSegmentRepository.findByBookingIdOrderByCheckInAsc(bookingId));
            bookingRepository.findById(bookingId).ifPresent(bookingRepository::delete);
        }
        for (String email : emails) {
            guestAccountRepository.findByEmail(email).ifPresent(guestAccountRepository::delete);
            for (GuestEntity guest : guestRepository.findByNormalizedEmail(email)) {
                auditLogRepository.deleteAll(entriesFor(AuditEntityType.GUEST, guest.getId()));
                guestRepository.delete(guest);
            }
        }
        for (String roomId : roomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
    }

    // --- Booking -> Guest ----------------------------------------------------------------------

    @Test
    void publicBooking_emailMatchesOneGuest_linksWithoutTouchingTheCard() {
        String email = newEmail();
        GuestEntity existing = persistGuest("Card Name", email, "+66 111", "VIP - allergic to shellfish");

        Booking booking = publicBooking("Typed Name", email.toUpperCase() + "  ", "+66 999");

        assertThat(booking.getGuestId().get()).isEqualTo(existing.getId());
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getGuestId()).isEqualTo(existing.getId());
        GuestEntity reloaded = guestRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Card Name");
        assertThat(reloaded.getPhone()).isEqualTo("+66 111");
        assertThat(reloaded.getNotes()).isEqualTo("VIP - allergic to shellfish");
        assertThat(guestRepository.findByNormalizedEmail(email)).hasSize(1);
    }

    @Test
    void publicBooking_noMatchingGuest_createsOneFromTheBooking_auditedAsSystem() {
        String email = newEmail();

        Booking booking = publicBooking("New Person", email, "+66 222");

        List<GuestEntity> cards = guestRepository.findByNormalizedEmail(email);
        assertThat(cards).hasSize(1);
        GuestEntity created = cards.get(0);
        assertThat(created.getName()).isEqualTo("New Person");
        assertThat(created.getPhone()).isEqualTo("+66 222");
        assertThat(booking.getGuestId().get()).isEqualTo(created.getId());

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.GUEST, created.getId());
        assertThat(entries).extracting(AuditLogEntity::getAction).containsExactly(AuditAction.GUEST_CREATED);
        assertThat(entries.get(0).getActorUserId()).isEqualTo("SYSTEM");
    }

    @Test
    void secondBookingForTheSameNewEmail_reusesTheCardTheFirstOneCreated() {
        String email = newEmail();

        Booking first = publicBooking("Repeat Guest", email, "+66 333");
        Booking second = publicBooking("Repeat Guest", email, "+66 333");

        assertThat(guestRepository.findByNormalizedEmail(email)).hasSize(1);
        assertThat(second.getGuestId().get()).isEqualTo(first.getGuestId().get());
    }

    @Test
    void booking_emailMatchesSeveralGuests_isLeftUnlinked() {
        String email = newEmail();
        persistGuest("Duplicate One", email, null, null);
        persistGuest("Duplicate Two", email.toUpperCase(), null, null);

        Booking booking = publicBooking("Someone", email, "+66 444");

        assertThat(booking.getGuestId().get()).isNull();
        assertThat(guestRepository.findByNormalizedEmail(email)).hasSize(2);
    }

    @Test
    void staffBooking_withoutEmail_isLeftUnlinked_withEmail_isAuditedAsTheStaffMember() {
        actAsCashier();
        Booking walkIn = staffBooking("Walk In", null);
        assertThat(walkIn.getGuestId().get()).isNull();

        String email = newEmail();
        Booking withEmail = staffBooking("Front Desk Guest", email);
        GuestEntity created = guestRepository.findByNormalizedEmail(email).get(0);
        assertThat(withEmail.getGuestId().get()).isEqualTo(created.getId());
        assertThat(entriesFor(AuditEntityType.GUEST, created.getId()))
                .extracting(AuditLogEntity::getActorUserId)
                .containsExactly("guest-link-cashier");
    }

    @Test
    void autoLinkedBooking_canStillBeRelinkedByHand() {
        String email = newEmail();
        Booking booking = publicBooking("Wrong Match", email, "+66 555");
        GuestEntity other = persistGuest("Right Person", newEmail(), null, null);

        actAsCashier();
        Booking relinked = bookingService.assignGuest(booking.getId(), new BookingGuestLinkInput().guestId(other.getId()));

        assertThat(relinked.getGuestId().get()).isEqualTo(other.getId());
    }

    // --- GuestAccount -> Guest -----------------------------------------------------------------

    @Test
    void register_emailMatchesOneGuest_linksWithoutChangingTheCard() {
        String email = newEmail();
        GuestEntity existing = persistGuest("Card Name", email.toUpperCase(), "+66 111", "Staff note");

        guestAccountService.register(registerInput(email, "Signup Name"));

        assertThat(guestAccountRepository.findByEmail(email).orElseThrow().getGuestId()).isEqualTo(existing.getId());
        GuestEntity reloaded = guestRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Card Name");
        assertThat(reloaded.getEmail()).isEqualTo(email.toUpperCase());
        assertThat(reloaded.getPhone()).isEqualTo("+66 111");
        assertThat(reloaded.getNotes()).isEqualTo("Staff note");
    }

    @Test
    void register_noMatchingGuest_createsNothingUntilVerified_thenCreatesAndLinks() {
        String email = newEmail();

        guestAccountService.register(registerInput(email, "Signup Name"));
        GuestAccountEntity pending = guestAccountRepository.findByEmail(email).orElseThrow();
        assertThat(pending.getGuestId()).isNull();
        assertThat(guestRepository.findByNormalizedEmail(email)).isEmpty();

        guestAccountService.verify(pending.getEmailVerificationToken());

        List<GuestEntity> cards = guestRepository.findByNormalizedEmail(email);
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getName()).isEqualTo("Signup Name");
        assertThat(guestAccountRepository.findByEmail(email).orElseThrow().getGuestId()).isEqualTo(cards.get(0).getId());
    }

    @Test
    void registerAndVerify_emailMatchesSeveralGuests_leavesAccountUnlinked() {
        String email = newEmail();
        persistGuest("Duplicate One", email, null, null);
        persistGuest("Duplicate Two", email, null, null);

        guestAccountService.register(registerInput(email, "Signup Name"));
        guestAccountService.verify(guestAccountRepository.findByEmail(email).orElseThrow().getEmailVerificationToken());

        assertThat(guestAccountRepository.findByEmail(email).orElseThrow().getGuestId()).isNull();
        assertThat(guestRepository.findByNormalizedEmail(email)).hasSize(2);
    }

    // --- Readers -------------------------------------------------------------------------------

    @Test
    void listBookings_followsTheGuestLink_sameSetAsTheStaffGuestCard() {
        String email = newEmail();
        GuestEntity card = persistGuest("Linked Guest", email, null, null);
        GuestAccountEntity account = persistAccount(email, card.getId(), true);

        Booking sameEmail = publicBooking("Linked Guest", email, "+66 1");
        // Booked under another address, linked to this card by hand - the old email match missed it.
        BookingEntity manuallyLinked = persistBooking("other-" + email, card.getId());
        // Same address, but staff moved it to a different card - no longer this guest's.
        BookingEntity movedAway = persistBooking(email, persistGuest("Someone Else", newEmail(), null, null).getId());

        List<String> guestSees = guestAccountService.listBookings(account.getId()).stream().map(GuestBookingView::getId).toList();
        List<String> staffSee = guestService.getDetail(card.getId()).getBookings().stream().map(Booking::getId).toList();

        assertThat(guestSees).containsExactlyInAnyOrder(sameEmail.getId(), manuallyLinked.getId());
        assertThat(guestSees).doesNotContain(movedAway.getId());
        assertThat(guestSees).containsExactlyInAnyOrderElementsOf(staffSee);
    }

    @Test
    void listBookings_unlinkedAccount_fallsBackToEmailMatch() {
        String email = newEmail();
        GuestAccountEntity account = persistAccount(email, null, true);
        BookingEntity booking = persistBooking(email.toUpperCase(), null);

        assertThat(guestAccountService.listBookings(account.getId())).extracting(GuestBookingView::getId).containsExactly(booking.getId());
    }

    @Test
    void guestDetail_showsAccountPresenceAndVerification() {
        GuestEntity noAccount = persistGuest("No Account", newEmail(), null, null);
        String unverifiedEmail = newEmail();
        GuestEntity unverified = persistGuest("Unverified", unverifiedEmail, null, null);
        persistAccount(unverifiedEmail, unverified.getId(), false);
        String verifiedEmail = newEmail();
        GuestEntity verified = persistGuest("Verified", verifiedEmail, null, null);
        persistAccount(verifiedEmail, verified.getId(), true);

        GuestDetail none = guestService.getDetail(noAccount.getId());
        assertThat(none.getAccount()).isNull();
        assertThat(guestService.getDetail(unverified.getId()).getAccount().getEmailVerified()).isFalse();
        assertThat(guestService.getDetail(verified.getId()).getAccount().getEmailVerified()).isTrue();
    }

    // --- helpers -------------------------------------------------------------------------------

    private String newEmail() {
        String email = "guest-link-" + UUID.randomUUID() + "@example.com";
        emails.add(email);
        emails.add("other-" + email);
        return email;
    }

    private void actAsCashier() {
        StaffPrincipal principal = new StaffPrincipal("guest-link-cashier", "guest-link-cashier@example.com", Role.CASHIER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER"))));
    }

    private GuestAccountRegisterInput registerInput(String email, String name) {
        GuestAccountRegisterInput input = new GuestAccountRegisterInput(email, "a-good-password1");
        input.name(name);
        return input;
    }

    private GuestEntity persistGuest(String name, String email, String phone, String notes) {
        GuestEntity guest = new GuestEntity();
        guest.setName(name);
        guest.setEmail(email);
        guest.setPhone(phone);
        guest.setNotes(notes);
        return guestRepository.saveAndFlush(guest);
    }

    private GuestAccountEntity persistAccount(String email, String guestId, boolean verified) {
        GuestAccountEntity account = new GuestAccountEntity();
        account.setEmail(email.toLowerCase());
        account.setPasswordHash("not-a-real-hash");
        account.setGuestId(guestId);
        account.setEmailVerifiedAt(verified ? LocalDateTime.now() : null);
        return guestAccountRepository.saveAndFlush(account);
    }

    private RoomEntity roomWithUnit() {
        RoomEntity room = new RoomEntity();
        room.setName("Guest Link Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by GuestLinkTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        roomIds.add(saved.getId());
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(saved.getId());
        unit.setLabel("Guest Link Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);
        return saved;
    }

    // Stay dates don't matter here - every booking gets its own fresh room, so none can collide.
    private Booking publicBooking(String name, String email, String phone) {
        LocalDate checkIn = LocalDate.of(2031, 3, 1);
        Booking booking = bookingService.createBooking(new BookingCreateInput(
                roomWithUnit().getId(), name, email, phone, checkIn.toString(), checkIn.plusDays(2).toString()));
        bookingIds.add(booking.getId());
        return booking;
    }

    private Booking staffBooking(String name, String email) {
        LocalDate checkIn = LocalDate.of(2031, 3, 1);
        StaffBookingCreateInput input = new StaffBookingCreateInput(roomWithUnit().getId(), name, checkIn.toString(), checkIn.plusDays(2).toString());
        if (email != null) {
            input.guestEmail(email);
        }
        Booking booking = bookingService.createStaffBooking(input);
        bookingIds.add(booking.getId());
        return booking;
    }

    private BookingEntity persistBooking(String guestEmail, String guestId) {
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(roomWithUnit().getId());
        booking.setGuestName("Guest");
        booking.setGuestEmail(guestEmail);
        booking.setGuestPhone("+66800000000");
        booking.setGuestId(guestId);
        booking.setCheckIn(LocalDate.of(2031, 3, 1));
        booking.setCheckOut(LocalDate.of(2031, 3, 3));
        booking.setTotalPrice(new BigDecimal("2000.00"));
        booking.setStatus(BookingStatus.NEW);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        bookingIds.add(saved.getId());
        return saved;
    }

    private List<AuditLogEntity> entriesFor(AuditEntityType entityType, String entityId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == entityType && entityId.equals(e.getEntityId()))
                .toList();
    }
}
