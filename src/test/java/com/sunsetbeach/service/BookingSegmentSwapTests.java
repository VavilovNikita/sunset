package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.SwapSegmentRoomUnitInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@code POST /bookings/{id}/segments/{segmentId}/swap-room-unit} - two segments' physical rooms
 * exchanged atomically, the operation a drag that drops one booking's bar onto another's needs
 * (sequential single-segment calls can't do this - see {@link BookingWriter#swapSegmentRoomUnits}'s
 * own javadoc). Same setup/cleanup pattern as {@link BookingRelocationTests}/
 * {@link BookingSegmentRoomUnitTests}, plus a real concurrent race (deliberately NOT
 * {@code @Transactional}, same reason as {@link BookingRelocationTests}'s own race test) since a
 * SERIALIZABLE claim that never observes contention proves nothing.
 */
@SpringBootTest
class BookingSegmentSwapTests extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private RoomUnitBlockRepository roomUnitBlockRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSegmentRepository segmentRepository;

    @Autowired
    private RatePlanRepository ratePlanRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdRoomIds = new ArrayList<>();

    @BeforeEach
    void setUpSecurityContext() {
        // Only the audit-entry test below actually reads this back, but every write here goes
        // through the same AuditLogService.record - matching GuestServiceTests's own setup so a
        // missing/inconsistent principal is never the reason a test passes or fails.
        StaffPrincipal principal = new StaffPrincipal("cashier-actor", "swap-test-actor@example.com", Role.CASHIER);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_CASHIER"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        List<BookingSegmentEntity> segmentsInTrackedRooms = createdRoomIds.isEmpty()
                ? List.of()
                : segmentRepository.findAll().stream().filter(s -> createdRoomIds.contains(s.getRoomId())).toList();
        List<String> bookingIdsToDelete = segmentsInTrackedRooms.stream().map(BookingSegmentEntity::getBookingId).distinct().toList();
        for (String bookingId : bookingIdsToDelete) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.BOOKING, bookingId));
        }
        if (!bookingIdsToDelete.isEmpty()) {
            bookingRepository.deleteAllById(bookingIdsToDelete);
        }
        for (String roomId : createdRoomIds) {
            ratePlanRepository.deleteAll(
                    ratePlanRepository.findByRoomIdAndDateBetween(roomId, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1)));
            for (RoomUnitEntity unit : roomUnitRepository.findByRoomId(roomId)) {
                roomUnitBlockRepository.deleteAll(roomUnitBlockRepository.findByRoomUnitId(unit.getId()));
            }
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
        createdRoomIds.clear();
    }

    private List<AuditLogEntity> entriesFor(AuditEntityType entityType, String entityId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == entityType && entityId.equals(e.getEntityId()))
                .toList();
    }

    private RoomEntity createRoom(int activeUnitCount, BigDecimal basePrice) {
        RoomEntity room = new RoomEntity();
        room.setName("Swap Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingSegmentSwapTests");
        room.setCapacity(2);
        room.setBasePrice(basePrice);
        RoomEntity saved = roomRepository.saveAndFlush(room);
        createdRoomIds.add(saved.getId());
        for (int i = 0; i < activeUnitCount; i++) {
            createUnit(saved);
        }
        return saved;
    }

    private RoomUnitEntity createUnit(RoomEntity room) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Swap Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        return roomUnitRepository.saveAndFlush(unit);
    }

    private Booking createBooking(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return bookingService.createBooking(
                new BookingCreateInput(roomId, "Guest", "guest@example.com", "+66800000000", checkIn.toString(), checkOut.toString()));
    }

    private List<BookingSegmentEntity> segmentsOf(String bookingId) {
        return segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
    }

    @Test
    void swap_twoSameTypeBookings_exchangesTheirUnitsWithNoReprice() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(320);
        LocalDate checkOut = checkIn.plusDays(3);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking bookingOne = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingOne.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        Booking bookingTwo = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingTwo.getId(), new RoomUnitAssignmentInput().roomUnitId(unitB.getId()));
        String segmentOneId = segmentsOf(bookingOne.getId()).get(0).getId();
        String segmentTwoId = segmentsOf(bookingTwo.getId()).get(0).getId();
        BigDecimal priceOneBefore = new BigDecimal(bookingService.getById(bookingOne.getId()).getTotalPrice());
        BigDecimal priceTwoBefore = new BigDecimal(bookingService.getById(bookingTwo.getId()).getTotalPrice());

        Booking result = bookingService.swapSegmentRoomUnit(bookingOne.getId(), segmentOneId, new SwapSegmentRoomUnitInput(segmentTwoId));

        assertThat(result.getRoomUnitId().get()).isEqualTo(unitB.getId());
        assertThat(segmentsOf(bookingOne.getId()).get(0).getRoomUnitId()).isEqualTo(unitB.getId());
        assertThat(segmentsOf(bookingTwo.getId()).get(0).getRoomUnitId()).isEqualTo(unitA.getId());
        // No reprice: each booking keeps its own already-frozen total.
        assertThat(new BigDecimal(bookingService.getById(bookingOne.getId()).getTotalPrice())).isEqualByComparingTo(priceOneBefore);
        assertThat(new BigDecimal(bookingService.getById(bookingTwo.getId()).getTotalPrice())).isEqualByComparingTo(priceTwoBefore);
    }

    @Test
    void swap_crossRoomType_isRejectedAsBadRequest_beforeAnyAvailabilityWork() {
        RoomEntity roomA = createRoom(1, new BigDecimal("1000.00"));
        RoomEntity roomB = createRoom(1, new BigDecimal("1500.00"));
        LocalDate checkIn = LocalDate.now().plusDays(323);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(roomA.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(roomB.getId()).get(0);

        Booking bookingOne = createBooking(roomA.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingOne.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        Booking bookingTwo = createBooking(roomB.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingTwo.getId(), new RoomUnitAssignmentInput().roomUnitId(unitB.getId()));
        String segmentOneId = segmentsOf(bookingOne.getId()).get(0).getId();
        String segmentTwoId = segmentsOf(bookingTwo.getId()).get(0).getId();

        assertThatCode(() -> bookingService.swapSegmentRoomUnit(
                        bookingOne.getId(), segmentOneId, new SwapSegmentRoomUnitInput(segmentTwoId)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("different room types");

        // Rejected up front - neither segment's unit moved.
        assertThat(segmentsOf(bookingOne.getId()).get(0).getRoomUnitId()).isEqualTo(unitA.getId());
        assertThat(segmentsOf(bookingTwo.getId()).get(0).getRoomUnitId()).isEqualTo(unitB.getId());
    }

    @Test
    void swap_oneSideUnassigned_isRejected() {
        RoomEntity room = createRoom(1, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(326);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);

        Booking bookingOne = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingOne.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        Booking bookingTwo = createBooking(room.getId(), checkIn.plusDays(10), checkOut.plusDays(10)); // no unit assigned
        String segmentOneId = segmentsOf(bookingOne.getId()).get(0).getId();
        String segmentTwoId = segmentsOf(bookingTwo.getId()).get(0).getId();

        assertThatCode(() -> bookingService.swapSegmentRoomUnit(
                        bookingOne.getId(), segmentOneId, new SwapSegmentRoomUnitInput(segmentTwoId)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already have a physical room assigned");
    }

    @Test
    void swap_blockedByAThirdSegmentSittingInTheTargetUnitForAnOverlappingSubRange_isRejected() {
        // The scenario named explicitly in the original spec: a third booking occupying part of
        // the target unit for a sub-range that overlaps only one side's dates must still block the
        // swap, even though only the two named segments are excluded from the conflict check.
        // bookingOne and bookingTwo deliberately have back-to-back (non-overlapping) own stay
        // windows - a swap only requires the same room *type*, not the same dates (each side
        // moves into the other's unit for its OWN dates) - so a third segment can sit in unit A
        // during a sub-range that overlaps only bookingTwo's target dates, not bookingOne's
        // current occupancy of that same unit, with no contradictory double-booking to construct.
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(330);
        LocalDate bookingOneCheckOut = checkIn.plusDays(4);
        LocalDate bookingTwoCheckIn = bookingOneCheckOut; // back-to-back turnover, valid
        LocalDate bookingTwoCheckOut = bookingTwoCheckIn.plusDays(4);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking bookingOne = createBooking(room.getId(), checkIn, bookingOneCheckOut);
        bookingService.assignRoomUnit(bookingOne.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        Booking bookingTwo = createBooking(room.getId(), bookingTwoCheckIn, bookingTwoCheckOut);
        bookingService.assignRoomUnit(bookingTwo.getId(), new RoomUnitAssignmentInput().roomUnitId(unitB.getId()));

        // Third booking sits in unit A for a sub-range inside bookingTwo's own window - after a
        // hypothetical swap, bookingTwo would move into unit A for exactly [bookingTwoCheckIn,
        // bookingTwoCheckOut), which this third segment overlaps; it does not touch bookingOne's
        // own [checkIn, bookingOneCheckOut) at all, so assigning it to unit A up front is itself
        // conflict-free.
        LocalDate thirdCheckIn = bookingTwoCheckIn.plusDays(1);
        LocalDate thirdCheckOut = bookingTwoCheckIn.plusDays(3);
        Booking third = createBooking(room.getId(), thirdCheckIn, thirdCheckOut);
        bookingService.assignRoomUnit(third.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));

        String segmentOneId = segmentsOf(bookingOne.getId()).get(0).getId();
        String segmentTwoId = segmentsOf(bookingTwo.getId()).get(0).getId();

        assertThatCode(() -> bookingService.swapSegmentRoomUnit(
                        bookingOne.getId(), segmentOneId, new SwapSegmentRoomUnitInput(segmentTwoId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked");

        assertThat(segmentsOf(bookingOne.getId()).get(0).getRoomUnitId()).isEqualTo(unitA.getId());
        assertThat(segmentsOf(bookingTwo.getId()).get(0).getRoomUnitId()).isEqualTo(unitB.getId());
    }

    @Test
    void swap_writesOneAuditEntryPerBooking() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(340);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking bookingOne = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingOne.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        Booking bookingTwo = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingTwo.getId(), new RoomUnitAssignmentInput().roomUnitId(unitB.getId()));
        String segmentOneId = segmentsOf(bookingOne.getId()).get(0).getId();
        String segmentTwoId = segmentsOf(bookingTwo.getId()).get(0).getId();

        bookingService.swapSegmentRoomUnit(bookingOne.getId(), segmentOneId, new SwapSegmentRoomUnitInput(segmentTwoId));

        List<AuditLogEntity> oneEntries = entriesFor(AuditEntityType.BOOKING, bookingOne.getId()).stream()
                .filter(a -> a.getAction() == AuditAction.BOOKING_ROOMS_SWAPPED)
                .toList();
        List<AuditLogEntity> twoEntries = entriesFor(AuditEntityType.BOOKING, bookingTwo.getId()).stream()
                .filter(a -> a.getAction() == AuditAction.BOOKING_ROOMS_SWAPPED)
                .toList();
        assertThat(oneEntries).hasSize(1);
        assertThat(twoEntries).hasSize(1);
        assertThat(oneEntries.get(0).getSummary()).contains(bookingTwo.getGuestName());
        assertThat(twoEntries.get(0).getSummary()).contains(bookingOne.getGuestName());
    }

    // --- Concurrent swap race ------------------------------------------------------------------

    @Test
    void concurrentSwapsTouchingASharedSegment_exactlyOneSucceeds() throws Exception {
        // Three bookings, three units, same room type. bookingOne/segmentA is the shared segment
        // both racing swaps target - one against bookingTwo, one against bookingThree. Both writes
        // touch segmentA's own row, so SERIALIZABLE must let only one through.
        RoomEntity room = createRoom(3, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(350);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);
        RoomUnitEntity unitC = roomUnitRepository.findByRoomId(room.getId()).get(2);

        Booking bookingOne = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingOne.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        Booking bookingTwo = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingTwo.getId(), new RoomUnitAssignmentInput().roomUnitId(unitB.getId()));
        Booking bookingThree = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingThree.getId(), new RoomUnitAssignmentInput().roomUnitId(unitC.getId()));

        String segmentA = segmentsOf(bookingOne.getId()).get(0).getId();
        String segmentB = segmentsOf(bookingTwo.getId()).get(0).getId();
        String segmentC = segmentsOf(bookingThree.getId()).get(0).getId();

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> swapWithTwo = () -> {
            barrier.await();
            try {
                return bookingService.swapSegmentRoomUnit(bookingOne.getId(), segmentA, new SwapSegmentRoomUnitInput(segmentB));
            } catch (Exception e) {
                return e;
            }
        };
        Callable<Object> swapWithThree = () -> {
            barrier.await();
            try {
                return bookingService.swapSegmentRoomUnit(bookingOne.getId(), segmentA, new SwapSegmentRoomUnitInput(segmentC));
            } catch (Exception e) {
                return e;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Object> results;
        try {
            Future<Object> f1 = pool.submit(swapWithTwo);
            Future<Object> f2 = pool.submit(swapWithThree);
            results = List.of(f1.get(), f2.get());
        } finally {
            pool.shutdown();
        }

        long successCount = results.stream().filter(r -> r instanceof Booking).count();
        long conflictCount = results.stream().filter(r -> r instanceof ConflictException).count();
        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);

        // segmentA ended up with exactly one of unitB/unitC, never both, never neither, and never
        // its own original unitA (a swap always moves it somewhere).
        String finalUnit = segmentsOf(bookingOne.getId()).get(0).getRoomUnitId();
        assertThat(finalUnit).isIn(unitB.getId(), unitC.getId());
    }
}
