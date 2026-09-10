package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@code PUT /bookings/{id}/segments/{segmentId}/room-unit} - the segment-scoped sibling of
 * {@code assignRoomUnit}/{@code unassignRoomUnit} (see {@link BookingWriter}'s class javadoc for
 * why both shapes coexist). Same setup/cleanup pattern as {@link BookingRelocationTests}.
 */
@SpringBootTest
class BookingSegmentRoomUnitTests extends AbstractIntegrationTest {

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
        List<BookingSegmentEntity> segmentsInTrackedRooms = createdRoomIds.isEmpty()
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
        room.setName("Segment Room Unit Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingSegmentRoomUnitTests");
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
        unit.setLabel("Segment Room Unit Test Unit " + UUID.randomUUID());
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
    void reassignSegmentRoomUnit_onASoleSegment_movesItToTheNewUnit() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(300);
        LocalDate checkOut = checkIn.plusDays(3);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        String segmentId = segmentsOf(booking.getId()).get(0).getId();
        BigDecimal priceBefore = new BigDecimal(bookingService.getById(booking.getId()).getTotalPrice());

        Booking updated = bookingService.assignSegmentRoomUnit(
                booking.getId(), segmentId, new RoomUnitAssignmentInput().roomUnitId(unitB.getId()));

        assertThat(segmentsOf(booking.getId())).hasSize(1);
        assertThat(segmentsOf(booking.getId()).get(0).getRoomUnitId()).isEqualTo(unitB.getId());
        assertThat(updated.getRoomUnitId().get()).isEqualTo(unitB.getId());
        // Same room type - no reprice.
        assertThat(new BigDecimal(updated.getTotalPrice())).isEqualByComparingTo(priceBefore);
    }

    @Test
    void unassignSegmentRoomUnit_clearsTheUnit() {
        RoomEntity room = createRoom(1, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(303);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity unit = roomUnitRepository.findByRoomId(room.getId()).get(0);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
        String segmentId = segmentsOf(booking.getId()).get(0).getId();

        Booking updated = bookingService.assignSegmentRoomUnit(booking.getId(), segmentId, new RoomUnitAssignmentInput().roomUnitId(null));

        assertThat(segmentsOf(booking.getId()).get(0).getRoomUnitId()).isNull();
        // roomUnitId is a JsonNullable that's always present on this DTO shape (see Booking's own
        // constructor) - "unassigned" means the wrapped value is null, not that the field is absent.
        assertThat(updated.getRoomUnitId().get()).isNull();
    }

    @Test
    void reassignSegmentRoomUnit_toADifferentRoomType_isRejected() {
        RoomEntity roomA = createRoom(1, new BigDecimal("1000.00"));
        RoomEntity roomB = createRoom(1, new BigDecimal("1500.00"));
        LocalDate checkIn = LocalDate.now().plusDays(306);
        LocalDate checkOut = checkIn.plusDays(2);
        RoomUnitEntity foreignUnit = roomUnitRepository.findByRoomId(roomB.getId()).get(0);

        Booking booking = createBooking(roomA.getId(), checkIn, checkOut);
        String segmentId = segmentsOf(booking.getId()).get(0).getId();

        assertThatCode(() -> bookingService.assignSegmentRoomUnit(
                        booking.getId(), segmentId, new RoomUnitAssignmentInput().roomUnitId(foreignUnit.getId())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("different room type");
    }

    @Test
    void reassignSegmentRoomUnit_intoAlreadyOccupiedUnit_isRejected() {
        RoomEntity room = createRoom(2, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(309);
        LocalDate checkOut = checkIn.plusDays(3);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        Booking blocker = createBooking(room.getId(), checkIn, checkOut);
        bookingService.assignRoomUnit(blocker.getId(), new RoomUnitAssignmentInput().roomUnitId(unitB.getId()));

        String segmentId = segmentsOf(booking.getId()).get(0).getId();
        assertThatCode(() -> bookingService.assignSegmentRoomUnit(
                        booking.getId(), segmentId, new RoomUnitAssignmentInput().roomUnitId(unitB.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked");
    }

    /**
     * The test explicitly required alongside this feature: reassigning a segment's unit and then
     * undoing the relocation that segment belongs to must not interfere with each other.
     * {@link BookingWriter#undoRelocation} only ever looks at the *earlier* segment's own
     * preserved history to restore it and unconditionally discards the *later* segment's row - so
     * whatever the later segment's room unit happens to be at the moment of undo (its original
     * relocation target, or something reassigned since, as here) must not change the outcome.
     */
    @Test
    void reassignSegmentRoomUnit_thenUndoTheRelocationItBelongsTo_undoStillRestoresCorrectly() {
        RoomEntity room = createRoom(3, new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(312);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);
        RoomUnitEntity unitC = roomUnitRepository.findByRoomId(room.getId()).get(2);

        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        assertThat(new BigDecimal(booking.getTotalPrice())).isEqualByComparingTo("4000.00"); // 4 x 1000
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unitA.getId()));
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));
        assertThat(segmentsOf(booking.getId())).hasSize(2);

        // Reassign the later (relocated-into) segment's own unit from B to C - orthogonal to the
        // relocation boundary itself, touches only roomUnitId, no dates.
        BookingSegmentEntity laterSegment =
                segmentsOf(booking.getId()).stream().sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn)).toList().get(1);
        Booking reassigned = bookingService.assignSegmentRoomUnit(
                booking.getId(), laterSegment.getId(), new RoomUnitAssignmentInput().roomUnitId(unitC.getId()));
        assertThat(segmentsOf(booking.getId()).stream().filter(s -> s.getId().equals(laterSegment.getId())).findFirst().orElseThrow()
                .getRoomUnitId()).isEqualTo(unitC.getId());
        // Same room type reassignment - no reprice, price unaffected by the swap of unit B for C.
        assertThat(new BigDecimal(reassigned.getTotalPrice())).isEqualByComparingTo("4000.00");

        Booking undone = bookingService.undoRelocation(booking.getId(), new RelocationUndoInput(splitDate.toString()));

        List<BookingSegmentEntity> segments = segmentsOf(booking.getId());
        assertThat(segments).hasSize(1);
        assertThat(segments.get(0).getCheckIn()).isEqualTo(checkIn);
        assertThat(segments.get(0).getCheckOut()).isEqualTo(checkOut);
        assertThat(segments.get(0).getRoomUnitId()).isEqualTo(unitA.getId());
        assertThat(undone.getRoomUnitId().get()).isEqualTo(unitA.getId());
        assertThat(new BigDecimal(undone.getTotalPrice())).isEqualByComparingTo("4000.00");
    }
}
