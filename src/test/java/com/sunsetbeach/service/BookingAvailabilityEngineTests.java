package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.RatePlanEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.AvailabilityDay;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DB-backed against the real dev Postgres, like the other {@code @SpringBootTest} suites in
 * this package - but deliberately NOT {@code @Transactional}: the concurrent-request test below
 * needs two genuinely separate top-level transactions. {@link BookingWriter#insert} runs
 * {@code SERIALIZABLE}, and that isolation level only takes effect on a transaction it actually
 * starts - nested inside this test's own {@code @Transactional} it would just silently join
 * that already-open transaction at the default isolation level instead, defeating the point of
 * the race test. Every test cleans up what it wrote in {@link #cleanUp()} instead of relying on
 * rollback.
 *
 * <p>Covers the two-level availability engine: {@code type-level available(date) =
 * activeUnitCount - distinct blocked units(date) - active bookings covering date} (a booking
 * without an assigned unit still occupies one unit of the type), and {@code unit-level
 * available(date) = not blocked && not booked by an assigned booking}, checked before a booking
 * is allowed to sell or a room unit is allowed to be assigned.
 */
@SpringBootTest
class BookingAvailabilityEngineTests extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private RoomUnitBlockRepository roomUnitBlockRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RatePlanRepository ratePlanRepository;

    private final List<String> createdRoomIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String roomId : createdRoomIds) {
            bookingRepository.deleteAll(bookingRepository.findByRoomId(roomId));
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

    private RoomEntity createRoom(int activeUnitCount) {
        RoomEntity room = new RoomEntity();
        room.setName("Engine Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingAvailabilityEngineTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        createdRoomIds.add(saved.getId());
        for (int i = 0; i < activeUnitCount; i++) {
            createUnit(saved, true);
        }
        return saved;
    }

    private RoomUnitEntity createUnit(RoomEntity room, boolean active) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Engine Test Unit " + UUID.randomUUID());
        unit.setActive(active);
        return roomUnitRepository.saveAndFlush(unit);
    }

    private static BookingCreateInput bookingInput(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return new BookingCreateInput(roomId, "Guest", "guest@example.com", "+66800000000", checkIn.toString(), checkOut.toString());
    }

    private Booking assign(String bookingId, String roomUnitId) {
        return bookingService.assignRoomUnit(bookingId, new RoomUnitAssignmentInput().roomUnitId(roomUnitId));
    }

    private Booking updateSchedule(String bookingId, LocalDate checkIn, LocalDate checkOut, String roomUnitId) {
        return bookingService.updateSchedule(
                bookingId, new BookingScheduleInput(checkIn.toString(), checkOut.toString()).roomUnitId(roomUnitId));
    }

    private BookingScheduleQuote quoteSchedule(String bookingId, LocalDate checkIn, LocalDate checkOut, String roomUnitId) {
        return bookingService.quoteSchedule(
                bookingId, new BookingScheduleInput(checkIn.toString(), checkOut.toString()).roomUnitId(roomUnitId));
    }

    @Test
    void createBooking_lastUnit_sellsOnce_thenSecondAttemptOnSameDatesConflicts() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        Booking first = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        assertThat(first.getId()).isNotNull();

        assertThatThrownBy(() -> bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createBooking_quantityTwo_thirdOverlappingBookingConflicts() {
        RoomEntity room = createRoom(2);
        LocalDate checkIn = LocalDate.now().plusDays(20);
        LocalDate checkOut = checkIn.plusDays(3);

        bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));

        assertThatThrownBy(() -> bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createBooking_partialNightOverlap_isStillBlocked() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(30);
        bookingService.createBooking(bookingInput(room.getId(), checkIn, checkIn.plusDays(3)));

        // Only the last shared night (checkIn+2) overlaps - one occupied night in the range is
        // enough to reject the whole request, not just the overlapping portion.
        assertThatThrownBy(
                        () -> bookingService.createBooking(bookingInput(room.getId(), checkIn.plusDays(2), checkIn.plusDays(5))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createBooking_manualBlockCoveringWholeQuantity_isRejected() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(40);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity unit = roomUnitRepository.findByRoomId(room.getId()).get(0);

        RoomUnitBlockEntity block = new RoomUnitBlockEntity();
        block.setRoomUnitId(unit.getId());
        block.setFromDate(checkIn);
        block.setToDate(checkIn);
        block.setReason("Under renovation");
        roomUnitBlockRepository.saveAndFlush(block);

        assertThatThrownBy(() -> bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createBooking_cancelledBookingDoesNotCountTowardCapacity() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(50);
        LocalDate checkOut = checkIn.plusDays(2);

        Booking first = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        bookingService.updateStatus(first.getId(), new BookingStatusInput(BookingStatus.CANCELLED));

        Booking second = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        assertThat(second.getId()).isNotNull();
    }

    @Test
    void createBooking_checkoutDayIsNotOccupied_backToBackBookingsBothSucceed() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(60);
        LocalDate turnoverDay = checkIn.plusDays(2);

        bookingService.createBooking(bookingInput(room.getId(), checkIn, turnoverDay));
        Booking second = bookingService.createBooking(bookingInput(room.getId(), turnoverDay, turnoverDay.plusDays(2)));

        assertThat(second.getId()).isNotNull();
    }

    /**
     * The scenario the SERIALIZABLE isolation in {@link BookingWriter#insert} exists for: two
     * requests race for the single remaining unit on the same dates. Exactly one must win;
     * the other must fail with {@link ConflictException} (either from the up-front remainder
     * check losing the race, or from a serialization failure that
     * {@link BookingService#isSerializationFailure} translates into one) - never both
     * succeeding, and never both failing.
     */
    @Test
    void createBooking_concurrentRequestsForTheLastUnit_exactlyOneSucceeds() throws Exception {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(70);
        LocalDate checkOut = checkIn.plusDays(2);

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> attempt = () -> {
            barrier.await();
            try {
                return bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
            } catch (Exception e) {
                return e;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Object> results;
        try {
            Future<Object> f1 = pool.submit(attempt);
            Future<Object> f2 = pool.submit(attempt);
            results = List.of(f1.get(), f2.get());
        } finally {
            pool.shutdown();
        }

        long successCount = results.stream().filter(r -> r instanceof Booking).count();
        long conflictCount = results.stream().filter(r -> r instanceof ConflictException).count();
        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);

        List<com.sunsetbeach.entity.BookingEntity> activeBookings = bookingRepository.findByRoomId(room.getId()).stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .toList();
        assertThat(activeBookings).hasSize(1);
    }

    @Test
    void booking_withoutAssignedUnit_stillOccupiesOneUnitOfTheType() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(80);
        LocalDate checkOut = checkIn.plusDays(1);

        Booking booking = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        assertThat(booking.getRoomUnitId().get()).isNull();

        AvailabilityDay day = dayFor(room.getId(), checkIn);
        assertThat(day.getBookedCount()).isEqualTo(1);
        assertThat(day.getAvailableCount()).isEqualTo(0);
        // The unit itself is neither booked nor blocked - the occupancy is only reflected at
        // the type level, since nobody assigned a specific physical room to this booking.
        assertThat(day.getUnits()).allSatisfy(u -> {
            assertThat(u.getIsBooked()).isFalse();
            assertThat(u.getIsAvailable()).isTrue();
        });
    }

    @Test
    void assignRoomUnit_differentRoomType_isRejected() {
        RoomEntity room = createRoom(1);
        RoomEntity otherRoom = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(90);
        Booking booking = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkIn.plusDays(1)));
        RoomUnitEntity otherUnit = roomUnitRepository.findByRoomId(otherRoom.getId()).get(0);

        assertThatThrownBy(() -> assign(booking.getId(), otherUnit.getId())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void assignRoomUnit_inactiveUnit_isRejected() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(91);
        Booking booking = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkIn.plusDays(1)));
        RoomUnitEntity inactiveUnit = createUnit(room, false);

        assertThatThrownBy(() -> assign(booking.getId(), inactiveUnit.getId())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void assignRoomUnit_alreadyAssignedToOverlappingBooking_isRejected() {
        RoomEntity room = createRoom(2);
        LocalDate checkIn = LocalDate.now().plusDays(92);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity unit = roomUnitRepository.findByRoomId(room.getId()).get(0);

        Booking first = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        Booking second = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        assign(first.getId(), unit.getId());

        assertThatThrownBy(() -> assign(second.getId(), unit.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void roomUnitBlock_reducesTypeLevelAvailabilityByOne() {
        RoomEntity room = createRoom(2);
        LocalDate date = LocalDate.now().plusDays(95);
        RoomUnitEntity unit = roomUnitRepository.findByRoomId(room.getId()).get(0);

        RoomUnitBlockEntity block = new RoomUnitBlockEntity();
        block.setRoomUnitId(unit.getId());
        block.setFromDate(date);
        block.setToDate(date);
        block.setReason("Deep cleaning");
        roomUnitBlockRepository.saveAndFlush(block);

        AvailabilityDay day = dayFor(room.getId(), date);
        assertThat(day.getBlockedCount()).isEqualTo(1);
        assertThat(day.getAvailableCount()).isEqualTo(1);
    }

    @Test
    void assignRoomUnit_checkoutDayFreesTheUnit_backToBackAssignmentsBothSucceed() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(100);
        LocalDate turnoverDay = checkIn.plusDays(2);
        RoomUnitEntity unit = roomUnitRepository.findByRoomId(room.getId()).get(0);

        Booking first = bookingService.createBooking(bookingInput(room.getId(), checkIn, turnoverDay));
        Booking second = bookingService.createBooking(bookingInput(room.getId(), turnoverDay, turnoverDay.plusDays(2)));

        Booking firstAssigned = assign(first.getId(), unit.getId());
        Booking secondAssigned = assign(second.getId(), unit.getId());

        assertThat(firstAssigned.getRoomUnitId().get()).isEqualTo(unit.getId());
        assertThat(secondAssigned.getRoomUnitId().get()).isEqualTo(unit.getId());
    }

    // --- PATCH /bookings/{id}/schedule (BookingService.updateSchedule / BookingWriter.updateSchedule) ---

    @Test
    void updateSchedule_extendsStay_recalculatesPriceFromRatePlan_notProportionally() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(110);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking booking = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));

        // The night being added by the extension gets a RatePlan override far from basePrice -
        // a naive "scale totalPrice by the new/old night count" implementation would get this
        // wrong; only summing basePrice/RatePlan per night, as BookingWriter.computeTotalPrice
        // does, gets it right.
        RatePlanEntity override = new RatePlanEntity();
        override.setRoomId(room.getId());
        override.setDate(checkOut); // the new night added by extending checkOut by one day
        override.setPrice(new BigDecimal("9000.00"));
        ratePlanRepository.saveAndFlush(override);

        Booking extended = updateSchedule(booking.getId(), checkIn, checkOut.plusDays(1), null);

        // 2 nights at basePrice (1000.00 each, see createRoom) + 1 night at the override (9000.00).
        assertThat(new BigDecimal(extended.getTotalPrice())).isEqualByComparingTo("11000.00");
    }

    @Test
    void updateSchedule_extendIntoOccupiedDates_isRejected() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(120);
        LocalDate turnoverDay = checkIn.plusDays(2);
        Booking booking = bookingService.createBooking(bookingInput(room.getId(), checkIn, turnoverDay));
        // Occupies the room's only unit right where `booking` would need to extend into.
        bookingService.createBooking(bookingInput(room.getId(), turnoverDay, turnoverDay.plusDays(2)));

        assertThatThrownBy(() -> updateSchedule(booking.getId(), checkIn, turnoverDay.plusDays(2), null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateSchedule_transferToDifferentRoomType_isRejectedWithReason() {
        RoomEntity room = createRoom(1);
        RoomEntity otherRoom = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(130);
        Booking booking = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkIn.plusDays(1)));
        RoomUnitEntity otherUnit = roomUnitRepository.findByRoomId(otherRoom.getId()).get(0);

        assertThatThrownBy(() -> updateSchedule(booking.getId(), checkIn, checkIn.plusDays(1), otherUnit.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("different room type");
    }

    @Test
    void updateSchedule_transferToBlockedUnit_isRejected() {
        RoomEntity room = createRoom(2);
        LocalDate checkIn = LocalDate.now().plusDays(140);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking booking = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        RoomUnitEntity targetUnit = roomUnitRepository.findByRoomId(room.getId()).get(1);

        RoomUnitBlockEntity block = new RoomUnitBlockEntity();
        block.setRoomUnitId(targetUnit.getId());
        block.setFromDate(checkIn);
        block.setToDate(checkOut);
        block.setReason("Plumbing repair");
        roomUnitBlockRepository.saveAndFlush(block);

        assertThatThrownBy(() -> updateSchedule(booking.getId(), checkIn, checkOut, targetUnit.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Plumbing repair");
    }

    @Test
    void updateSchedule_backToBackTransfers_bothSucceed() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(150);
        LocalDate turnoverDay = checkIn.plusDays(2);
        RoomUnitEntity unit = roomUnitRepository.findByRoomId(room.getId()).get(0);

        Booking first = bookingService.createBooking(bookingInput(room.getId(), checkIn, turnoverDay));
        Booking second = bookingService.createBooking(bookingInput(room.getId(), turnoverDay, turnoverDay.plusDays(2)));

        Booking firstAssigned = updateSchedule(first.getId(), checkIn, turnoverDay, unit.getId());
        Booking secondAssigned = updateSchedule(second.getId(), turnoverDay, turnoverDay.plusDays(2), unit.getId());

        assertThat(firstAssigned.getRoomUnitId().get()).isEqualTo(unit.getId());
        assertThat(secondAssigned.getRoomUnitId().get()).isEqualTo(unit.getId());
    }

    @Test
    void updateSchedule_concurrentTransferOfTwoBookingsToSameFreeUnit_exactlyOneSucceeds() throws Exception {
        RoomEntity room = createRoom(2);
        LocalDate checkIn = LocalDate.now().plusDays(160);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity unit1 = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity freeUnit = roomUnitRepository.findByRoomId(room.getId()).get(1);

        // Two overlapping bookings the room's 2 units can both hold at the type level - occupy
        // unit1 with one of them first so only `freeUnit` is actually up for grabs below.
        Booking bookingA = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        Booking bookingB = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        assign(bookingA.getId(), unit1.getId());

        // bookingA is already on unit1; race bookingA and bookingB for the one remaining unit via
        // updateSchedule (not assignRoomUnit) - both attempt to (re)assign the same free unit.
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> attemptA = () -> {
            barrier.await();
            try {
                return updateSchedule(bookingA.getId(), checkIn, checkOut, freeUnit.getId());
            } catch (Exception e) {
                return e;
            }
        };
        Callable<Object> attemptB = () -> {
            barrier.await();
            try {
                return updateSchedule(bookingB.getId(), checkIn, checkOut, freeUnit.getId());
            } catch (Exception e) {
                return e;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Object> results;
        try {
            Future<Object> f1 = pool.submit(attemptA);
            Future<Object> f2 = pool.submit(attemptB);
            results = List.of(f1.get(), f2.get());
        } finally {
            pool.shutdown();
        }

        long successCount = results.stream().filter(r -> r instanceof Booking).count();
        long conflictCount = results.stream().filter(r -> r instanceof ConflictException).count();
        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);
    }

    @Test
    void updateSchedule_withoutAssignedUnit_datesChange_staysUnassigned() {
        RoomEntity room = createRoom(1);
        LocalDate checkIn = LocalDate.now().plusDays(170);
        LocalDate checkOut = checkIn.plusDays(1);
        Booking booking = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));
        assertThat(booking.getRoomUnitId().get()).isNull();

        Booking updated = updateSchedule(booking.getId(), checkIn.plusDays(1), checkOut.plusDays(3), null);

        assertThat(updated.getRoomUnitId().get()).isNull();
        assertThat(updated.getCheckIn()).isEqualTo(checkIn.plusDays(1).toString());
        assertThat(updated.getCheckOut()).isEqualTo(checkOut.plusDays(3).toString());
    }

    @Test
    void quoteSchedule_conflictingTransfer_reportsUnavailableWithReason_withoutThrowing() {
        RoomEntity room = createRoom(2);
        LocalDate checkIn = LocalDate.now().plusDays(180);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity blockedUnit = roomUnitRepository.findByRoomId(room.getId()).get(0);
        Booking booking = bookingService.createBooking(bookingInput(room.getId(), checkIn, checkOut));

        RoomUnitBlockEntity block = new RoomUnitBlockEntity();
        block.setRoomUnitId(blockedUnit.getId());
        block.setFromDate(checkIn);
        block.setToDate(checkOut);
        block.setReason("Pest control");
        roomUnitBlockRepository.saveAndFlush(block);

        BookingScheduleQuote quote = quoteSchedule(booking.getId(), checkIn, checkOut, blockedUnit.getId());

        assertThat(quote.getAvailable()).isFalse();
        assertThat(quote.getReason().get()).contains("Pest control");
        // totalPrice is still computed - it's pure arithmetic over dates, independent of
        // whether the requested room assignment would actually succeed.
        assertThat(new BigDecimal(quote.getTotalPrice())).isEqualByComparingTo("2000.00");
    }

    private AvailabilityDay dayFor(String roomId, LocalDate date) {
        String month = "%04d-%02d".formatted(date.getYear(), date.getMonthValue());
        return availabilityService.getAvailability(roomId, month).getDays().stream()
                .filter(d -> d.getDate().equals(date.toString()))
                .findFirst()
                .orElseThrow();
    }
}
