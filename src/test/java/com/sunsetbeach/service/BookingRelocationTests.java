package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RelocationUndoInput;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
 * DB-backed against the real dev Postgres, deliberately NOT {@code @Transactional} - same reason
 * as {@link BookingAvailabilityEngineTests}: the concurrent-relocation test needs two genuinely
 * separate top-level transactions for {@code SERIALIZABLE} to mean anything. Every test cleans up
 * its own rooms in {@link #cleanUp()} (which cascades to bookings/segments via the DB's own
 * {@code ON DELETE CASCADE} - see V18__booking_segments.sql), never touching manual dev-DB data.
 */
@SpringBootTest
class BookingRelocationTests {

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

    private final List<String> createdRoomIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        // Unlike BookingAvailabilityEngineTests (every booking's segments stay in the one room it
        // was created under), a relocated booking here can end up with segments spanning TWO
        // tracked rooms, and Booking.roomId mirrors only the *last* segment - so
        // bookingRepository.findByRoomId(roomId) alone would miss a booking whose first segment
        // (still referencing this room) predates a relocation into another tracked room. Delete
        // every booking that has ANY segment in ANY tracked room first (cascading to every one of
        // its segments regardless of which room they reference), before touching a single room.
        List<com.sunsetbeach.entity.BookingSegmentEntity> segmentsInTrackedRooms = createdRoomIds.isEmpty()
                ? List.of()
                : segmentRepository.findAll().stream().filter(s -> createdRoomIds.contains(s.getRoomId())).toList();
        List<String> bookingIdsToDelete = segmentsInTrackedRooms.stream().map(BookingSegmentEntity::getBookingId).distinct().toList();
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

    private RoomEntity createRoom(int activeUnitCount, BigDecimal basePrice) {
        RoomEntity room = new RoomEntity();
        room.setName("Relocation Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingRelocationTests");
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
        unit.setLabel("Relocation Test Unit " + UUID.randomUUID());
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

    // --- Continuity invariant ---------------------------------------------------------------

    @Test
    void relocate_producesTwoContiguousSegmentsCoveringTheWholeStay() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(200);
        LocalDate checkOut = checkIn.plusDays(6);
        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));

        LocalDate splitDate = checkIn.plusDays(3);
        bookingService.relocate(
                booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        List<BookingSegmentEntity> segments = segmentsOf(booking.getId()).stream()
                .sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn))
                .toList();
        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).getCheckIn()).isEqualTo(checkIn);
        assertThat(segments.get(0).getCheckOut()).isEqualTo(splitDate);
        assertThat(segments.get(0).getRoomUnitId()).isEqualTo(unitA.getId());
        assertThat(segments.get(1).getCheckIn()).isEqualTo(splitDate);
        assertThat(segments.get(1).getCheckOut()).isEqualTo(checkOut);
        assertThat(segments.get(1).getRoomUnitId()).isEqualTo(unitB.getId());
        // No gap, no overlap: segment 0's checkOut is exactly segment 1's checkIn, and together
        // they span exactly [booking.checkIn, booking.checkOut) - the invariant BookingWriter
        // re-asserts on every segment-mutating write.
        assertThat(segments.get(0).getCheckOut()).isEqualTo(segments.get(1).getCheckIn());

        Booking updated = bookingService.getById(booking.getId());
        assertThat(updated.getCheckIn()).isEqualTo(checkIn.toString());
        assertThat(updated.getCheckOut()).isEqualTo(checkOut.toString());
        // roomId/roomUnitId mirror the *last* segment.
        assertThat(updated.getRoomUnitId().get()).isEqualTo(unitB.getId());
    }

    @Test
    void singleSegmentBooking_isTheSameShapeAsARelocatedOne_notASpecialCase() {
        RoomEntity room = createRoom(1, new BigDecimal("1200.00"));
        LocalDate checkIn = LocalDate.now().plusDays(205);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking booking = createBooking(room.getId(), checkIn, checkOut);

        List<BookingSegmentEntity> segments = segmentsOf(booking.getId());
        assertThat(segments).hasSize(1);
        assertThat(segments.get(0).getCheckIn()).isEqualTo(checkIn);
        assertThat(segments.get(0).getCheckOut()).isEqualTo(checkOut);
        assertThat(segments.get(0).getTotalPrice()).isEqualByComparingTo(booking.getTotalPrice());
    }

    // --- Relocation with occupancy checking ---------------------------------------------------

    @Test
    void relocate_intoAlreadyOccupiedUnit_isRejected() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(210);
        LocalDate checkOut = checkIn.plusDays(4);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        // A second booking occupies unit B for the back half of the first booking's stay.
        LocalDate splitDate = checkIn.plusDays(2);
        Booking blocker = createBooking(room.getId(), splitDate, checkOut);
        bookingService.assignRoomUnit(blocker.getId(), new RoomUnitAssignmentInput().roomUnitId(unitB.getId()));

        assertThatCode(() -> bookingService.relocate(
                        booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked");

        // Rejected relocation must not have mutated anything - still one segment.
        assertThat(segmentsOf(booking.getId())).hasSize(1);
    }

    @Test
    void quoteRelocation_conflictingUnit_reportsUnavailableWithoutThrowing() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(215);
        LocalDate checkOut = checkIn.plusDays(4);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        LocalDate splitDate = checkIn.plusDays(2);
        Booking blocker = createBooking(room.getId(), splitDate, checkOut);
        bookingService.assignRoomUnit(blocker.getId(), new RoomUnitAssignmentInput().roomUnitId(unitB.getId()));

        BookingScheduleQuote quote = bookingService.quoteRelocation(
                booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        assertThat(quote.getAvailable()).isFalse();
        assertThat(quote.getReason().get()).contains("already booked");
        assertThat(segmentsOf(booking.getId())).hasSize(1);
    }

    // --- Concurrent relocation race ------------------------------------------------------------

    @Test
    void concurrentRelocationOfTwoBookingsToTheSameFreeUnit_exactlyOneSucceeds() throws Exception {
        // Three units: A and C hold the two bookings' own (independent, non-conflicting) starting
        // occupancy, B is the one free unit both bookings will race to relocate their back half
        // into - unitA/unitC must differ, or assigning both bookings to the same unit for the
        // same dates would itself conflict before the race ever starts.
        RoomEntity room = createRoom(3, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(220);
        LocalDate checkOut = checkIn.plusDays(4);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity targetUnit = roomUnitRepository.findByRoomId(room.getId()).get(1);
        RoomUnitEntity unitC = roomUnitRepository.findByRoomId(room.getId()).get(2);
        LocalDate splitDate = checkIn.plusDays(2);

        Booking bookingOne = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingOne.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        Booking bookingTwo = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(bookingTwo.getId(), new RoomUnitAssignmentInput().roomUnitId(unitC.getId()));

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> relocateOne = () -> {
            barrier.await();
            try {
                return bookingService.relocate(
                        bookingOne.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(targetUnit.getId()));
            } catch (Exception e) {
                return e;
            }
        };
        Callable<Object> relocateTwo = () -> {
            barrier.await();
            try {
                return bookingService.relocate(
                        bookingTwo.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(targetUnit.getId()));
            } catch (Exception e) {
                return e;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Object> results;
        try {
            Future<Object> f1 = pool.submit(relocateOne);
            Future<Object> f2 = pool.submit(relocateTwo);
            results = List.of(f1.get(), f2.get());
        } finally {
            pool.shutdown();
        }

        long successCount = results.stream().filter(r -> r instanceof Booking).count();
        long conflictCount = results.stream().filter(r -> r instanceof ConflictException).count();
        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);

        // Exactly one of the two bookings actually has two segments now; the other stayed at one.
        long relocatedCount =
                List.of(bookingOne.getId(), bookingTwo.getId()).stream().filter(id -> segmentsOf(id).size() == 2).count();
        assertThat(relocatedCount).isEqualTo(1);
    }

    // --- Price recompute on room-type change ---------------------------------------------------

    @Test
    void relocate_toADifferentRoomType_recomputesPriceForThatTypesRate() {
        RoomEntity cheapRoom = createRoom(1, new BigDecimal("1000.00"));
        RoomEntity expensiveRoom = createRoom(1, new BigDecimal("2500.00"));
        LocalDate checkIn = LocalDate.now().plusDays(230);
        LocalDate checkOut = checkIn.plusDays(4); // 4 nights
        LocalDate splitDate = checkIn.plusDays(2); // 2 nights in each room

        Booking booking = createBooking(cheapRoom.getId(), checkIn, checkOut);
        assertThat(new BigDecimal(booking.getTotalPrice())).isEqualByComparingTo("4000.00"); // 4 x 1000

        RoomUnitEntity expensiveUnit = roomUnitRepository.findByRoomId(expensiveRoom.getId()).get(0);
        Booking relocated = bookingService.relocate(
                booking.getId(), new RelocationInput(splitDate.toString(), expensiveRoom.getId()).roomUnitId(expensiveUnit.getId()));

        // 2 nights at 1000 (unchanged first segment) + 2 nights at 2500 (new segment) = 7000.
        assertThat(new BigDecimal(relocated.getTotalPrice())).isEqualByComparingTo("7000.00");
        assertThat(relocated.getRoomId()).isEqualTo(expensiveRoom.getId());

        List<BookingSegmentEntity> segments =
                segmentsOf(booking.getId()).stream().sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn)).toList();
        assertThat(segments.get(0).getRoomId()).isEqualTo(cheapRoom.getId());
        assertThat(segments.get(0).getTotalPrice()).isEqualByComparingTo("2000.00");
        assertThat(segments.get(1).getRoomId()).isEqualTo(expensiveRoom.getId());
        assertThat(segments.get(1).getTotalPrice()).isEqualByComparingTo("5000.00");
    }

    @Test
    void quoteRelocation_toADifferentRoomType_previewsTheNewWholeBookingTotal() {
        RoomEntity cheapRoom = createRoom(1, new BigDecimal("1000.00"));
        RoomEntity expensiveRoom = createRoom(1, new BigDecimal("2500.00"));
        LocalDate checkIn = LocalDate.now().plusDays(235);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);

        Booking booking = createBooking(cheapRoom.getId(), checkIn, checkOut);
        RoomUnitEntity expensiveUnit = roomUnitRepository.findByRoomId(expensiveRoom.getId()).get(0);

        BookingScheduleQuote quote = bookingService.quoteRelocation(
                booking.getId(), new RelocationInput(splitDate.toString(), expensiveRoom.getId()).roomUnitId(expensiveUnit.getId()));

        assertThat(quote.getAvailable()).isTrue();
        assertThat(quote.getNights()).isEqualTo(2); // the new segment's own nights
        assertThat(new BigDecimal(quote.getTotalPrice())).isEqualByComparingTo("7000.00");
        // A preview must not have written anything.
        assertThat(segmentsOf(booking.getId())).hasSize(1);
    }

    // --- Undo-relocation ------------------------------------------------------------------------

    @Test
    void undoRelocation_mergesSegmentsBackAndRevertsToTheEarlierRoom() {
        RoomEntity cheapRoom = createRoom(1, new BigDecimal("1000.00"));
        RoomEntity expensiveRoom = createRoom(1, new BigDecimal("2500.00"));
        LocalDate checkIn = LocalDate.now().plusDays(240);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);

        Booking booking = createBooking(cheapRoom.getId(), checkIn, checkOut);
        RoomUnitEntity expensiveUnit = roomUnitRepository.findByRoomId(expensiveRoom.getId()).get(0);
        bookingService.relocate(
                booking.getId(), new RelocationInput(splitDate.toString(), expensiveRoom.getId()).roomUnitId(expensiveUnit.getId()));
        assertThat(segmentsOf(booking.getId())).hasSize(2);

        Booking undone = bookingService.undoRelocation(booking.getId(), new RelocationUndoInput(splitDate.toString()));

        List<BookingSegmentEntity> segments = segmentsOf(booking.getId());
        assertThat(segments).hasSize(1);
        assertThat(segments.get(0).getCheckIn()).isEqualTo(checkIn);
        assertThat(segments.get(0).getCheckOut()).isEqualTo(checkOut);
        assertThat(segments.get(0).getRoomId()).isEqualTo(cheapRoom.getId());
        assertThat(undone.getRoomId()).isEqualTo(cheapRoom.getId());
        assertThat(new BigDecimal(undone.getTotalPrice())).isEqualByComparingTo("4000.00");
    }

    // --- Schedule changes on a multi-segment booking: outer-edge extension is allowed ----------

    @Test
    void updateSchedule_extendingLastSegment_oneMoreNight_succeeds() {
        // The most common front-desk request on an already-relocated booking: the guest stays an
        // extra night in whatever room they currently occupy. Must not require undo/extend/
        // re-relocate - just a plain schedule change targeting the last segment.
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(255);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        LocalDate extendedCheckOut = checkOut.plusDays(1);
        BookingScheduleInput input = new BookingScheduleInput(checkIn.toString(), extendedCheckOut.toString()).roomUnitId(unitB.getId());

        BookingScheduleQuote quote = bookingService.quoteSchedule(booking.getId(), input);
        assertThat(quote.getAvailable()).isTrue();
        assertThat(new BigDecimal(quote.getTotalPrice())).isEqualByComparingTo("5000.00");

        Booking updated = bookingService.updateSchedule(booking.getId(), input);
        assertThat(updated.getCheckOut()).isEqualTo(extendedCheckOut.toString());
        assertThat(new BigDecimal(updated.getTotalPrice())).isEqualByComparingTo("5000.00");

        List<BookingSegmentEntity> segments = segmentsOf(booking.getId());
        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).getCheckIn()).isEqualTo(checkIn);
        assertThat(segments.get(0).getCheckOut()).isEqualTo(splitDate); // untouched boundary
        assertThat(segments.get(1).getCheckIn()).isEqualTo(splitDate); // untouched boundary
        assertThat(segments.get(1).getCheckOut()).isEqualTo(extendedCheckOut);
        assertThat(segments.get(1).getRoomUnitId()).isEqualTo(unitB.getId());
        assertThat(segments.get(1).getTotalPrice()).isEqualByComparingTo("3000.00");
    }

    @Test
    void updateSchedule_extendingFirstSegment_earlierArrival_succeeds() {
        // Symmetric case: an earlier check-in only touches the first segment, in whatever room
        // it's already in - the second segment's own boundary/room is untouched either way.
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(258);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        LocalDate earlierCheckIn = checkIn.minusDays(1);
        Booking updated = bookingService.updateSchedule(
                booking.getId(), new BookingScheduleInput(earlierCheckIn.toString(), checkOut.toString()).roomUnitId(unitA.getId()));
        assertThat(updated.getCheckIn()).isEqualTo(earlierCheckIn.toString());

        List<BookingSegmentEntity> segments = segmentsOf(booking.getId());
        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).getCheckIn()).isEqualTo(earlierCheckIn);
        assertThat(segments.get(0).getCheckOut()).isEqualTo(splitDate);
        assertThat(segments.get(0).getRoomUnitId()).isEqualTo(unitA.getId());
        assertThat(segments.get(1).getCheckIn()).isEqualTo(splitDate); // untouched boundary
        assertThat(segments.get(1).getCheckOut()).isEqualTo(checkOut); // untouched boundary
    }

    @Test
    void updateSchedule_shrinkingLastSegmentPastItsOwnStart_isRejected() {
        // Shrinking checkOut back to before the relocation's own split date would require the
        // last segment to have negative length, not a schedule change - that's an undo-relocation.
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(262);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        LocalDate beforeSplit = splitDate.minusDays(1);
        assertThatCode(() -> bookingService.updateSchedule(
                        booking.getId(), new BookingScheduleInput(checkIn.toString(), beforeSplit.toString()).roomUnitId(unitB.getId())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("undo-relocation");
    }

    @Test
    void updateSchedule_bothEndsChangedOnAMultiSegmentBooking_isRejected() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(266);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        assertThatCode(() -> bookingService.updateSchedule(
                        booking.getId(),
                        new BookingScheduleInput(checkIn.minusDays(1).toString(), checkOut.plusDays(1).toString()).roomUnitId(unitB.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("split by a room relocation");
    }

    @Test
    void updateSchedule_roomOnlyChangeOnAMultiSegmentBooking_isRejected() {
        // Same overall dates, only trying to change the room - ambiguous which segment that
        // would apply to, since neither end of the request differs from the booking's own bounds.
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(270);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        assertThatCode(() -> bookingService.updateSchedule(
                        booking.getId(), new BookingScheduleInput(checkIn.toString(), checkOut.toString()).roomUnitId(unitA.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("split by a room relocation");
    }

    @Test
    void quoteSchedule_ambiguousMultiSegmentChange_reportsUnavailableWithoutThrowing() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(274);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        BookingScheduleQuote quote = bookingService.quoteSchedule(
                booking.getId(),
                new BookingScheduleInput(checkIn.minusDays(1).toString(), checkOut.plusDays(1).toString()).roomUnitId(unitB.getId()));
        assertThat(quote.getAvailable()).isFalse();
        assertThat(quote.getReason().get()).contains("split by a room relocation");
    }

    @Test
    void assignRoomUnit_onARelocatedBooking_isRejected() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(250);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        assertThatCode(() -> bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unitB.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("split by a room relocation");
    }

    // --- Migration backfill: existing (pre-feature) bookings got exactly one matching segment ---

    @Test
    void everyExistingBooking_hasExactlyOneSegmentMatchingItsOwnFields() {
        // Read-only assertion against whatever bookings already exist in the dev DB (manual test
        // data included) - proves V18__booking_segments.sql's backfill, without creating or
        // touching a single row of its own.
        List<com.sunsetbeach.entity.BookingEntity> existing = bookingRepository.findAll();
        assertThat(existing).isNotEmpty();
        for (com.sunsetbeach.entity.BookingEntity booking : existing) {
            List<BookingSegmentEntity> segments = segmentsOf(booking.getId());
            assertThat(segments).as("booking %s should have exactly one segment", booking.getId()).hasSize(1);
            BookingSegmentEntity segment = segments.get(0);
            assertThat(segment.getRoomId()).isEqualTo(booking.getRoomId());
            assertThat(segment.getRoomUnitId()).isEqualTo(booking.getRoomUnitId());
            assertThat(segment.getCheckIn()).isEqualTo(booking.getCheckIn());
            assertThat(segment.getCheckOut()).isEqualTo(booking.getCheckOut());
            assertThat(segment.getTotalPrice()).isEqualByComparingTo(booking.getTotalPrice());
        }
    }

}
