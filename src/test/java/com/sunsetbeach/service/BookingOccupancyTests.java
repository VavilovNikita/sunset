package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.AvailabilityDay;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.CheckInResult;
import com.sunsetbeach.model.CheckOutResult;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.TodayBoard;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Physical occupancy (check-in/check-out/no-show) and the front desk's "today" board - see
 * {@link BookingOccupancyService}'s class javadoc for the design this exercises. Every test that
 * needs a room-unit assignment uses dates far enough in the future to avoid colliding with any
 * other test's room (rooms are created fresh per test, so this is mostly precautionary).
 *
 * <p>DB-backed against the ephemeral Testcontainers Postgres, {@code @Transactional} (auto
 * rollback) - no concurrency under test here.
 */
@SpringBootTest
@Transactional
class BookingOccupancyTests extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingOccupancyService occupancyService;

    @Autowired
    private RoomUnitService roomUnitService;

    @Autowired
    private com.sunsetbeach.repository.RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private AvailabilityService availabilityService;

    private RoomEntity createRoom() {
        RoomEntity room = new RoomEntity();
        room.setName("Occupancy Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingOccupancyTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        return roomRepository.saveAndFlush(room);
    }

    private RoomUnitEntity createUnit(RoomEntity room) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Occupancy Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        return roomUnitRepository.saveAndFlush(unit);
    }

    private Booking createBooking(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return bookingService.createBooking(
                new BookingCreateInput(roomId, "Guest", "guest@example.com", "+66800000000", checkIn.toString(), checkOut.toString()));
    }

    private Booking assignUnit(String bookingId, String roomUnitId) {
        return bookingService.assignRoomUnit(bookingId, new RoomUnitAssignmentInput().roomUnitId(roomUnitId));
    }

    // --- Check-in requires an assigned room ------------------------------------------------------

    @Test
    void checkIn_withoutARoomUnitAssigned_isRejectedWithAClearReason() {
        RoomEntity room = createRoom();
        createUnit(room); // a unit must exist for the room type to be bookable at all - just never assigned to this booking
        LocalDate checkIn = LocalDate.now().plusDays(400);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));

        assertThatCode(() -> occupancyService.checkIn(booking.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Assign a room");
    }

    // --- Check-in into a dirty room warns but does not block ---------------------------------------

    @Test
    void checkIn_intoADirtyRoom_succeedsWithAWarning() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        roomUnitService.updateHousekeeping(unit.getId(), HousekeepingStatus.DIRTY);
        LocalDate checkIn = LocalDate.now().plusDays(401);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));
        assignUnit(booking.getId(), unit.getId());

        CheckInResult result = occupancyService.checkIn(booking.getId());

        assertThat(result.getWarning().get()).isNotNull().contains(unit.getLabel());
        assertThat(result.getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
        assertThat(result.getBooking().getCheckedInAt().get()).isNotNull();
    }

    @Test
    void checkIn_intoACleanRoom_succeedsWithNoWarning() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room); // CLEAN by default
        LocalDate checkIn = LocalDate.now().plusDays(402);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));
        assignUnit(booking.getId(), unit.getId());

        CheckInResult result = occupancyService.checkIn(booking.getId());

        assertThat(result.getWarning().get()).isNull();
        assertThat(result.getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
    }

    // --- Check-out dirties the room ----------------------------------------------------------------

    @Test
    void checkOut_marksTheRoomDirty() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate checkIn = LocalDate.now().plusDays(403);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));
        assignUnit(booking.getId(), unit.getId());
        occupancyService.checkIn(booking.getId());

        CheckOutResult result = occupancyService.checkOut(booking.getId());

        assertThat(result.getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_OUT);
        assertThat(result.getBooking().getCheckedOutAt().get()).isNotNull();
        RoomUnitEntity reloaded = roomUnitRepository.findById(unit.getId()).orElseThrow();
        assertThat(reloaded.getHousekeepingStatus()).isEqualTo(HousekeepingStatus.DIRTY);
    }

    // --- Check-out with an outstanding balance is reported, not blocked ----------------------------

    @Test
    void checkOut_withAnUnpaidBooking_reportsTheOutstandingBalance_butDoesNotBlock() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate checkIn = LocalDate.now().plusDays(404);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(3)); // status NEW, never marked PAID
        assignUnit(booking.getId(), unit.getId());
        occupancyService.checkIn(booking.getId());

        CheckOutResult result = occupancyService.checkOut(booking.getId());

        assertThat(result.getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_OUT);
        assertThat(new BigDecimal(result.getOutstandingBalance())).isEqualByComparingTo(booking.getTotalPrice());
    }

    @Test
    void checkOut_whenAlreadyPaid_reportsNoOutstandingBalance() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate checkIn = LocalDate.now().plusDays(405);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));
        assignUnit(booking.getId(), unit.getId());
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(com.sunsetbeach.model.BookingStatus.PAID));
        occupancyService.checkIn(booking.getId());

        CheckOutResult result = occupancyService.checkOut(booking.getId());

        assertThat(new BigDecimal(result.getOutstandingBalance())).isEqualByComparingTo("0.00");
    }

    // --- Repeated check-in/check-out is rejected, not silently accepted or double-applied ----------

    @Test
    void checkIn_whenAlreadyCheckedIn_isRejected() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate checkIn = LocalDate.now().plusDays(406);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));
        assignUnit(booking.getId(), unit.getId());
        occupancyService.checkIn(booking.getId());

        assertThatCode(() -> occupancyService.checkIn(booking.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already checked in");
    }

    @Test
    void checkOut_whenNeverCheckedIn_isRejected() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate checkIn = LocalDate.now().plusDays(407);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));
        assignUnit(booking.getId(), unit.getId());

        assertThatCode(() -> occupancyService.checkOut(booking.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("hasn't been checked in");
    }

    @Test
    void checkOut_whenAlreadyCheckedOut_isRejected() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate checkIn = LocalDate.now().plusDays(408);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));
        assignUnit(booking.getId(), unit.getId());
        occupancyService.checkIn(booking.getId());
        occupancyService.checkOut(booking.getId());

        assertThatCode(() -> occupancyService.checkOut(booking.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("hasn't been checked in");
    }

    // --- No-show: a label, reachable from EXPECTED, and a late arrival can still check in ----------

    @Test
    void markNoShow_thenCheckIn_succeeds_aLateArrivalCanStillCheckIn() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate checkIn = LocalDate.now().plusDays(409);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));
        assignUnit(booking.getId(), unit.getId());

        Booking noShow = occupancyService.markNoShow(booking.getId());
        assertThat(noShow.getOccupancyStatus()).isEqualTo(OccupancyStatus.NO_SHOW);
        // A no-show changes nothing about the booking's own dates.
        assertThat(noShow.getCheckIn()).isEqualTo(checkIn.toString());
        assertThat(noShow.getCheckOut()).isEqualTo(checkIn.plusDays(2).toString());

        CheckInResult result = occupancyService.checkIn(booking.getId());
        assertThat(result.getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
    }

    @Test
    void markNoShow_whenNotAwaitingArrival_isRejected() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate checkIn = LocalDate.now().plusDays(410);
        Booking booking = createBooking(room.getId(), checkIn, checkIn.plusDays(2));
        assignUnit(booking.getId(), unit.getId());
        occupancyService.checkIn(booking.getId());

        assertThatCode(() -> occupancyService.markNoShow(booking.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not awaiting arrival");
    }

    // --- Occupancy must never affect the availability engine ---------------------------------------

    @Test
    void occupancyStatus_neverAffectsAvailability() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate checkIn = LocalDate.now().plusDays(411);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        assignUnit(booking.getId(), unit.getId());

        AvailabilityDay before = availabilityOn(room.getId(), checkIn);
        assertThat(before.getBookedCount()).isEqualTo(1);
        int availableBefore = before.getAvailableCount();

        occupancyService.checkIn(booking.getId());
        AvailabilityDay whileCheckedIn = availabilityOn(room.getId(), checkIn);
        assertThat(whileCheckedIn.getBookedCount()).isEqualTo(1);
        assertThat(whileCheckedIn.getAvailableCount()).isEqualTo(availableBefore);

        occupancyService.checkOut(booking.getId());
        AvailabilityDay afterCheckOut = availabilityOn(room.getId(), checkIn);
        assertThat(afterCheckOut.getBookedCount()).isEqualTo(1);
        assertThat(afterCheckOut.getAvailableCount()).isEqualTo(availableBefore);

        // A no-show on a *different* booking, same room, later dates: still occupies its nights.
        LocalDate laterCheckIn = checkOut.plusDays(5);
        Booking laterBooking = createBooking(room.getId(), laterCheckIn, laterCheckIn.plusDays(2));
        assignUnit(laterBooking.getId(), unit.getId());
        AvailabilityDay laterBefore = availabilityOn(room.getId(), laterCheckIn);
        occupancyService.markNoShow(laterBooking.getId());
        AvailabilityDay laterAfterNoShow = availabilityOn(room.getId(), laterCheckIn);
        assertThat(laterAfterNoShow.getBookedCount()).isEqualTo(laterBefore.getBookedCount());
        assertThat(laterAfterNoShow.getAvailableCount()).isEqualTo(laterBefore.getAvailableCount());
    }

    private AvailabilityDay availabilityOn(String roomId, LocalDate date) {
        return availabilityService.getAvailability(roomId, YearMonth.from(date).toString()).getDays().stream()
                .filter(d -> d.getDate().equals(date.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No availability row for " + roomId + " on " + date));
    }

    // --- The today board groups bookings into exactly the right list -------------------------------

    @Test
    void todayBoard_groupsArrivingDepartingAndInHouseCorrectly() {
        RoomEntity room = createRoom();
        RoomUnitEntity arrivingUnit = createUnit(room);
        RoomUnitEntity departingUnit = createUnit(room);
        RoomUnitEntity inHouseUnit = createUnit(room);
        LocalDate today = LocalDate.now();

        // Arriving today: EXPECTED, checkIn = today.
        Booking arriving = createBooking(room.getId(), today, today.plusDays(3));
        assignUnit(arriving.getId(), arrivingUnit.getId());

        // Departing today: CHECKED_IN, checkOut = today (backdate checkIn so checkOut can be today).
        Booking departing = createBooking(room.getId(), today.minusDays(2), today);
        assignUnit(departing.getId(), departingUnit.getId());
        occupancyService.checkIn(departing.getId());

        // In-house: CHECKED_IN, but not departing today.
        Booking inHouse = createBooking(room.getId(), today.minusDays(1), today.plusDays(4));
        assignUnit(inHouse.getId(), inHouseUnit.getId());
        occupancyService.checkIn(inHouse.getId());

        TodayBoard board = occupancyService.getTodayBoard();

        assertThat(board.getArrivingToday().stream().map(e -> e.getBooking().getId())).contains(arriving.getId());
        assertThat(board.getArrivingToday().stream().map(e -> e.getBooking().getId()))
                .doesNotContain(departing.getId(), inHouse.getId());

        assertThat(board.getDepartingToday().stream().map(e -> e.getBooking().getId())).contains(departing.getId());
        assertThat(board.getDepartingToday().stream().map(e -> e.getBooking().getId()))
                .doesNotContain(arriving.getId(), inHouse.getId());

        assertThat(board.getInHouse().stream().map(e -> e.getBooking().getId())).contains(departing.getId(), inHouse.getId());
        assertThat(board.getInHouse().stream().map(e -> e.getBooking().getId())).doesNotContain(arriving.getId());
    }

    @Test
    void todayBoard_excludesANoShowBooking_fromAllThreeLists() {
        RoomEntity room = createRoom();
        RoomUnitEntity unit = createUnit(room);
        LocalDate today = LocalDate.now();
        Booking booking = createBooking(room.getId(), today, today.plusDays(2));
        assignUnit(booking.getId(), unit.getId());
        occupancyService.markNoShow(booking.getId());

        TodayBoard board = occupancyService.getTodayBoard();

        assertThat(board.getArrivingToday().stream().map(e -> e.getBooking().getId())).doesNotContain(booking.getId());
        assertThat(board.getDepartingToday().stream().map(e -> e.getBooking().getId())).doesNotContain(booking.getId());
        assertThat(board.getInHouse().stream().map(e -> e.getBooking().getId())).doesNotContain(booking.getId());
    }
}
