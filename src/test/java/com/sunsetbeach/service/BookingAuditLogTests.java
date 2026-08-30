package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
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
 * DB-backed (real dev Postgres): confirms each audited booking action writes the entry
 * {@link AuditLogService} is supposed to for it. Deliberately NOT {@code @Transactional} -
 * {@code AuditLogService.record} commits in its own {@code REQUIRES_NEW} transaction (see its
 * javadoc), so a row it writes would survive a rollback of this test's own transaction anyway;
 * cleanup is manual, by id, in {@link #cleanUp()}, same as the other non-transactional DB tests
 * in this package (e.g. {@code BookingAvailabilityEngineTests}).
 */
@SpringBootTest
class BookingAuditLogTests {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdRoomIds = new java.util.ArrayList<>();
    private final List<String> createdBookingIds = new java.util.ArrayList<>();

    @BeforeEach
    void setUpSecurityContext() {
        StaffPrincipal principal = new StaffPrincipal("cashier-actor", "audit-booking-test@example.com", Role.CASHIER);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_CASHIER"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String bookingId : createdBookingIds) {
            auditLogRepository.deleteAll(findEntriesForBooking(bookingId));
            bookingRepository.deleteById(bookingId);
        }
        for (String roomId : createdRoomIds) {
            for (RoomUnitEntity unit : roomUnitRepository.findByRoomId(roomId)) {
                roomUnitRepository.deleteById(unit.getId());
            }
            roomRepository.deleteById(roomId);
        }
    }

    private List<AuditLogEntity> findEntriesForBooking(String bookingId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.BOOKING && bookingId.equals(e.getEntityId()))
                .toList();
    }

    private RoomEntity createRoom() {
        RoomEntity room = new RoomEntity();
        room.setName("Audit Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingAuditLogTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        createdRoomIds.add(saved.getId());
        // A room type with zero active units has zero inventory - availableCount is always 0
        // regardless of dates, so every booking attempt against it conflicts. One default unit
        // keeps every test's room bookable; assignRoomUnit_* creates its own second, specific
        // unit on top of this one where it needs a particular id/label to assert against.
        createUnit(saved);
        return saved;
    }

    private RoomUnitEntity createUnit(RoomEntity room) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Audit Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        return roomUnitRepository.saveAndFlush(unit);
    }

    @Test
    void createStaffBooking_writesBookingCreatedEntry() {
        RoomEntity room = createRoom();
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = LocalDate.now().plusDays(11);

        Booking booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), "Jane Doe", checkIn.toString(), checkOut.toString()));
        createdBookingIds.add(booking.getId());

        List<AuditLogEntity> entries = findEntriesForBooking(booking.getId());
        assertThat(entries).hasSize(1);
        AuditLogEntity entry = entries.get(0);
        assertThat(entry.getAction()).isEqualTo(AuditAction.BOOKING_CREATED);
        assertThat(entry.getEntityType()).isEqualTo(AuditEntityType.BOOKING);
        assertThat(entry.getActorEmail()).isEqualTo("audit-booking-test@example.com");
        assertThat(entry.getSummary()).contains("Jane Doe").contains(room.getName());
    }

    @Test
    void updateStatus_toPaid_writesStatusChangedEntry() {
        RoomEntity room = createRoom();
        Booking booking = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.getId(), "Guest", LocalDate.now().plusDays(5).toString(), LocalDate.now().plusDays(6).toString()));
        createdBookingIds.add(booking.getId());

        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.PAID));

        List<AuditLogEntity> entries = findEntriesForBooking(booking.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_STATUS_CHANGED)
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).contains("NEW").contains("PAID");
    }

    @Test
    void updateStatus_paymentNoteChanged_writesPaymentNoteEntryWithoutLeakingContent() {
        RoomEntity room = createRoom();
        Booking booking = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.getId(), "Guest", LocalDate.now().plusDays(5).toString(), LocalDate.now().plusDays(6).toString()));
        createdBookingIds.add(booking.getId());

        bookingService.updateStatus(
                booking.getId(), new BookingStatusInput(BookingStatus.NEW).paymentNote("Cash deposit ref 4471"));

        List<AuditLogEntity> entries = findEntriesForBooking(booking.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_PAYMENT_NOTE_CHANGED)
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).doesNotContain("4471").contains("updated");
    }

    @Test
    void updateSchedule_writesScheduleChangedEntryWithOldAndNewDates() {
        RoomEntity room = createRoom();
        LocalDate originalCheckIn = LocalDate.now().plusDays(5);
        LocalDate originalCheckOut = LocalDate.now().plusDays(6);
        Booking booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), "Guest", originalCheckIn.toString(), originalCheckOut.toString()));
        createdBookingIds.add(booking.getId());

        LocalDate newCheckIn = LocalDate.now().plusDays(7);
        LocalDate newCheckOut = LocalDate.now().plusDays(9);
        bookingService.updateSchedule(
                booking.getId(), new BookingScheduleInput(newCheckIn.toString(), newCheckOut.toString()).roomUnitId(null));

        List<AuditLogEntity> entries = findEntriesForBooking(booking.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_SCHEDULE_CHANGED)
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).contains(originalCheckIn.toString()).contains(newCheckIn.toString());
    }

    @Test
    void assignRoomUnit_writesRoomAssignedEntryWithUnitLabels() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        Booking booking = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.getId(), "Guest", LocalDate.now().plusDays(5).toString(), LocalDate.now().plusDays(6).toString()));
        createdBookingIds.add(booking.getId());

        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));

        List<AuditLogEntity> entries = findEntriesForBooking(booking.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_ROOM_ASSIGNED)
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).contains(unit.getLabel()).contains("unassigned");
    }

    @Test
    void exportCsv_writesBookingsExportedEntryWithNoSingleEntityId() {
        bookingService.exportCsv(null, null, null);

        List<AuditLogEntity> entries = auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.BOOKINGS_EXPORTED && "audit-booking-test@example.com".equals(e.getActorEmail()))
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getEntityId()).isNull();
        auditLogRepository.deleteAll(entries);
    }
}
