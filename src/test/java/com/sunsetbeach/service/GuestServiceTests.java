package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingGuestLinkInput;
import com.sunsetbeach.model.Guest;
import com.sunsetbeach.model.GuestCreateInput;
import com.sunsetbeach.model.GuestDetail;
import com.sunsetbeach.model.GuestUpdateInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
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

/**
 * DB-backed (real dev Postgres), NOT {@code @Transactional} - same reasoning as
 * {@code UserRoomUnitAuditLogTests}: {@code AuditLogService.record} commits in its own
 * transaction regardless of what this test's own transaction does, so a rollback-based test
 * would still leave real audit rows behind. Covers {@link GuestService} directly (search,
 * create/update/delete, the delete-blocked-by-a-booking backstop) and {@link
 * BookingService#assignGuest} (link/unlink/relink, including the confirmed no-warning-on-relink
 * behaviour) - see both classes' own javadoc for the design this exercises.
 */
@SpringBootTest
class GuestServiceTests extends AbstractIntegrationTest {

    @Autowired
    private GuestService guestService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSegmentRepository bookingSegmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdGuestIds = new java.util.ArrayList<>();
    private final List<String> createdRoomIds = new java.util.ArrayList<>();
    private final List<String> createdBookingIds = new java.util.ArrayList<>();

    @BeforeEach
    void setUpSecurityContext() {
        StaffPrincipal principal = new StaffPrincipal("cashier-actor", "guest-test-actor@example.com", Role.CASHIER);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_CASHIER"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        // Bookings (and their segments) first - Booking.guestId/roomId are both RESTRICT, so
        // Guest/Room can't go until every booking pointing at them is gone.
        for (String bookingId : createdBookingIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.BOOKING, bookingId));
            bookingSegmentRepository.deleteAll(bookingSegmentRepository.findByBookingIdOrderByCheckInAsc(bookingId));
            bookingRepository.findById(bookingId).ifPresent(bookingRepository::delete);
        }
        for (String guestId : createdGuestIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.GUEST, guestId));
            guestRepository.findById(guestId).ifPresent(guestRepository::delete);
        }
        for (String roomId : createdRoomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
    }

    private List<AuditLogEntity> entriesFor(AuditEntityType entityType, String entityId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == entityType && entityId.equals(e.getEntityId()))
                .toList();
    }

    private Guest createGuest(String name, String email, String phone) {
        Guest guest = guestService.create(new GuestCreateInput(name).email(email).phone(phone));
        createdGuestIds.add(guest.getId());
        return guest;
    }

    private RoomEntity createRoom() {
        RoomEntity room = new RoomEntity();
        room.setName("Guest Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by GuestServiceTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        createdRoomIds.add(saved.getId());
        return saved;
    }

    private Booking createBooking(String guestName) {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Guest Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);

        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), guestName, checkIn.toString(), checkOut.toString()).roomUnitId(unit.getId()));
        createdBookingIds.add(booking.getId());
        return booking;
    }

    // --- search -------------------------------------------------------------------------------

    @Test
    void search_matchesByNameEmailOrPhone_caseInsensitively() {
        String unique = UUID.randomUUID().toString();
        Guest byName = createGuest("Jane Doe " + unique, null, null);
        Guest byEmail = createGuest("Someone Else " + unique, "match-" + unique + "@example.com", null);
        Guest byPhone = createGuest("Another Person " + unique, null, "555-" + unique);
        createGuest("Unrelated Guest " + unique, "unrelated-" + unique + "@example.com", "555-unrelated-" + unique);

        assertThat(guestService.search("jane doe " + unique.toUpperCase())).extracting("id").containsExactly(byName.getId());
        assertThat(guestService.search("MATCH-" + unique.toUpperCase())).extracting("id").containsExactly(byEmail.getId());
        assertThat(guestService.search("555-" + unique)).extracting("id").containsExactly(byPhone.getId());
    }

    @Test
    void search_blankQuery_returnsEveryGuest() {
        String unique = UUID.randomUUID().toString();
        Guest a = createGuest("Search All A " + unique, null, null);
        Guest b = createGuest("Search All B " + unique, null, null);

        List<Guest> all = guestService.search(null);

        assertThat(all).extracting("id").contains(a.getId(), b.getId());
    }

    @Test
    void search_noMatch_returnsEmptyNotError() {
        assertThat(guestService.search("no-such-guest-" + UUID.randomUUID())).isEmpty();
    }

    // --- create / update / delete --------------------------------------------------------------

    @Test
    void create_writesAuditEntry() {
        Guest guest = createGuest("Audit Guest " + UUID.randomUUID(), "audit@example.com", null);

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.GUEST, guest.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.GUEST_CREATED)
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).contains(guest.getName());
    }

    @Test
    void update_isFullReplacement_andWritesAuditEntry() {
        Guest guest = createGuest("Original Name " + UUID.randomUUID(), "original@example.com", "111");

        String newName = "Updated Name " + UUID.randomUUID();
        Guest updated = guestService.update(guest.getId(), new GuestUpdateInput(newName).phone("222"));

        assertThat(updated.getName()).isEqualTo(newName);
        assertThat(updated.getPhone().get()).isEqualTo("222");
        // Full replacement: email, omitted this time, is cleared rather than left alone.
        assertThat(updated.getEmail().get()).isNull();

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.GUEST, guest.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.GUEST_UPDATED)
                .toList();
        assertThat(entries).hasSize(1);
    }

    @Test
    void getDetail_unknownId_isNotFound() {
        assertThatThrownBy(() -> guestService.getDetail("no-such-guest")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getDetail_includesGuestsBookingHistory() {
        Guest guest = createGuest("Detail Guest " + UUID.randomUUID(), null, null);
        Booking booking = createBooking("Detail Guest's Booking");
        bookingService.assignGuest(booking.getId(), new BookingGuestLinkInput().guestId(guest.getId()));

        GuestDetail detail = guestService.getDetail(guest.getId());

        assertThat(detail.getBookings()).extracting("id").containsExactly(booking.getId());
    }

    @Test
    void delete_withNoBookings_succeeds() {
        Guest guest = createGuest("Deletable Guest " + UUID.randomUUID(), null, null);

        guestService.delete(guest.getId());
        createdGuestIds.remove(guest.getId());

        assertThat(guestRepository.existsById(guest.getId())).isFalse();
        List<AuditLogEntity> entries = entriesFor(AuditEntityType.GUEST, guest.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.GUEST_DELETED)
                .toList();
        assertThat(entries).hasSize(1);
        auditLogRepository.deleteAll(entries);
    }

    @Test
    void delete_withABookingOnRecord_isBlockedRegardlessOfStayBeingPastOrFuture() {
        Guest guest = createGuest("Guest With History " + UUID.randomUUID(), null, null);
        Booking booking = createBooking("Guest With History's Booking");
        bookingService.assignGuest(booking.getId(), new BookingGuestLinkInput().guestId(guest.getId()));

        assertThatThrownBy(() -> guestService.delete(guest.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("bookings on record");

        assertThat(guestRepository.existsById(guest.getId())).isTrue();
    }

    // --- booking <-> guest link -----------------------------------------------------------------

    @Test
    void assignGuest_linksGuestAndWritesAuditEntry() {
        Guest guest = createGuest("Linked Guest " + UUID.randomUUID(), null, null);
        Booking booking = createBooking("Booking To Link");

        Booking linked = bookingService.assignGuest(booking.getId(), new BookingGuestLinkInput().guestId(guest.getId()));

        assertThat(linked.getGuestId().get()).isEqualTo(guest.getId());
        assertThat(linked.getGuest()).isNotNull();
        assertThat(linked.getGuest().getId()).isEqualTo(guest.getId());
        // The booking's own frozen snapshot fields never move, regardless of the link.
        assertThat(linked.getGuestName()).isEqualTo(booking.getGuestName());

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.BOOKING, booking.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_GUEST_LINKED)
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).contains(guest.getName());
    }

    @Test
    void assignGuest_null_unlinksGuest() {
        Guest guest = createGuest("Unlink Guest " + UUID.randomUUID(), null, null);
        Booking booking = createBooking("Booking To Unlink");
        bookingService.assignGuest(booking.getId(), new BookingGuestLinkInput().guestId(guest.getId()));

        Booking unlinked = bookingService.assignGuest(booking.getId(), new BookingGuestLinkInput().guestId(null));

        assertThat(unlinked.getGuestId().get()).isNull();
        assertThat(unlinked.getGuest()).isNull();
    }

    /**
     * Confirmed requirement: relinking to a different guest needs no warning and no state check -
     * nothing downstream depends on which Guest a booking points to, unlike room/inventory
     * actions. This just needs to succeed cleanly, twice in a row, with no exception in between.
     */
    @Test
    void assignGuest_relinkToADifferentGuest_needsNoConfirmationAndSucceeds() {
        Guest first = createGuest("First Guest " + UUID.randomUUID(), null, null);
        Guest second = createGuest("Second Guest " + UUID.randomUUID(), null, null);
        Booking booking = createBooking("Booking To Relink");

        bookingService.assignGuest(booking.getId(), new BookingGuestLinkInput().guestId(first.getId()));
        Booking relinked = bookingService.assignGuest(booking.getId(), new BookingGuestLinkInput().guestId(second.getId()));

        assertThat(relinked.getGuestId().get()).isEqualTo(second.getId());

        List<AuditLogEntity> linkEntries = entriesFor(AuditEntityType.BOOKING, booking.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_GUEST_LINKED)
                .toList();
        assertThat(linkEntries).hasSize(2);
    }

    @Test
    void assignGuest_unknownGuestId_isNotFound() {
        Booking booking = createBooking("Booking With Bad Guest");

        assertThatThrownBy(() -> bookingService.assignGuest(booking.getId(), new BookingGuestLinkInput().guestId("no-such-guest")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void assignGuest_unknownBookingId_isNotFound() {
        Guest guest = createGuest("Guest For Bad Booking " + UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> bookingService.assignGuest("no-such-booking", new BookingGuestLinkInput().guestId(guest.getId())))
                .isInstanceOf(NotFoundException.class);
    }
}
