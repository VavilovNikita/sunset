package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.AvailabilityEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.repository.AvailabilityRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
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
 * <p>Covers the room-as-type availability engine: {@code available(date) = quantity -
 * blockedCount(date) - active bookings covering date}, checked for every night in a requested
 * range before a booking is allowed to sell.
 */
@SpringBootTest
class BookingAvailabilityEngineTests {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    private final List<String> createdRoomIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String roomId : createdRoomIds) {
            bookingRepository.deleteAll(bookingRepository.findByRoomId(roomId));
            availabilityRepository.deleteAll(
                    availabilityRepository.findByRoomIdAndDateGreaterThanEqual(roomId, LocalDate.of(2000, 1, 1)));
            roomRepository.deleteById(roomId);
        }
        createdRoomIds.clear();
    }

    private RoomEntity createRoom(int quantity) {
        RoomEntity room = new RoomEntity();
        room.setName("Engine Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingAvailabilityEngineTests");
        room.setCapacity(2);
        room.setQuantity(quantity);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        createdRoomIds.add(saved.getId());
        return saved;
    }

    private static BookingCreateInput bookingInput(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return new BookingCreateInput(roomId, "Guest", "guest@example.com", "+66800000000", checkIn.toString(), checkOut.toString());
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

        AvailabilityEntity block = new AvailabilityEntity();
        block.setRoomId(room.getId());
        block.setDate(checkIn);
        block.setBlockedCount(1);
        availabilityRepository.saveAndFlush(block);

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

        List<BookingEntity> activeBookings =
                bookingRepository.findByRoomId(room.getId()).stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED).toList();
        assertThat(activeBookings).hasSize(1);
    }
}
