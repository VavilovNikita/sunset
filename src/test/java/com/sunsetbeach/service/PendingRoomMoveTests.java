package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.InHouseRow;
import com.sunsetbeach.model.PendingRoomMove;
import com.sunsetbeach.model.RoomUnitUpdateInput;
import com.sunsetbeach.model.TodayBoardEntry;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.PropertyMapUnit;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.support.FixedHotelClockConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * A checked-in guest whose move takes effect with no new room picked stays in the room they slept in: on the map, as
 * an occupant, for housekeeping. "Today" is pinned to 2034-03-10, the day of the move.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
@Transactional
class PendingRoomMoveTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private PropertyMapService propertyMapService;
    @Autowired private VacatedRoomHousekeepingService vacatedRooms;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private com.sunsetbeach.repository.BookingSegmentRepository segmentRepository;
    @Autowired private ReportService reportService;
    @Autowired private RoomUnitService roomUnitService;
    @Autowired private java.time.Clock clock;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;

    private record Room(RoomEntity type, RoomUnitEntity unit) {}

    private Room room() {
        RoomEntity r = new RoomEntity();
        r.setName("Pending Move Room " + UUID.randomUUID());
        r.setDescription("Used only by PendingRoomMoveTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(r);
        RoomUnitEntity u = new RoomUnitEntity();
        u.setRoomId(saved.getId());
        u.setLabel("PM-" + UUID.randomUUID().toString().substring(0, 8));
        u.setActive(true);
        u.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        return new Room(saved, roomUnitRepository.saveAndFlush(u));
    }

    private Booking booking(Room room, boolean assigned, String guest, LocalDate checkIn, LocalDate checkOut) {
        Booking b = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.type().getId(), guest, checkIn.toString(), checkOut.toString(), BookingChannel.WALK_IN, 1));
        return assigned ? bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(room.unit().getId())) : b;
    }

    /** In the house since the arrival day, as an earlier check-in left it. */
    private void arrived(Booking booking) {
        BookingEntity entity = bookingRepository.findById(booking.getId()).orElseThrow();
        entity.setOccupancyStatus(OccupancyStatus.CHECKED_IN);
        entity.setCheckedInAt(entity.getCheckIn().atTime(8, 0));
        bookingRepository.saveAndFlush(entity);
    }

    /** In {@code from} since two days ago, with a move into {@code to} that takes effect today and no room picked. */
    private Booking movingToday(Room from, Room to) {
        Booking guest = booking(from, true, "Moving Guest", TODAY.minusDays(2), TODAY.plusDays(3));
        guest = bookingService.relocate(guest.getId(), new RelocationInput(TODAY.toString(), to.type().getId()));
        arrived(guest);
        return guest;
    }

    private PropertyMapUnit tile(Room room) {
        return propertyMapService.get().getUnits().stream().filter(u -> u.getRoomUnitId().equals(room.unit().getId())).findFirst().orElseThrow();
    }

    private HousekeepingStatus housekeeping(Room room) {
        return roomUnitRepository.findById(room.unit().getId()).orElseThrow().getHousekeepingStatus();
    }

    private TodayBoardEntry inHouse(Booking booking) {
        return occupancyService.getTodayBoard().getInHouse().stream()
                .filter(e -> e.getBooking().getId().equals(booking.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void everyScreenSaysTheOldRoom_andAsksForTheNewOne() {
        Room old = room();
        Room next = room();
        Booking guest = movingToday(old, next);
        String segmentId = guest.getSegments().get(1).getId();

        Booking read = bookingService.getById(guest.getId());
        assertThat(read.getCurrentSegmentId()).isEqualTo(segmentId);
        assertThat(read.getRoomSegmentId()).isEqualTo(guest.getSegments().get(0).getId());
        PendingRoomMove pending = read.getPendingRoomMove();
        assertThat(pending.getSegmentId()).isEqualTo(segmentId);
        assertThat(pending.getRoomId()).isEqualTo(next.type().getId());
        assertThat(pending.getCheckIn()).isEqualTo(TODAY.toString());
        assertThat(pending.getReason()).isEqualTo("Moving today — choose the new room");
        assertThat(pending.getStayingInRoom()).isEqualTo(old.unit().getLabel());
        assertThat(inHouse(guest).getPendingRoomMove().getSegmentId()).isEqualTo(segmentId);
        assertThat(tile(old).getCurrentBooking().getPendingRoomMove().getSegmentId()).isEqualTo(segmentId);

        InHouseRow row = reportService.inHouse(TODAY.toString()).getRooms().stream()
                .filter(r -> r.getBookingId().equals(guest.getId())).findFirst().orElseThrow();
        assertThat(row.getRoomUnitLabel().orElse(null)).isEqualTo(old.unit().getLabel());
        assertThat(row.getRoomName()).isEqualTo(old.type().getName());
    }

    @Test
    void onceTheNewRoomIsChosen_everythingMovesThere_andTheOldRoomIsDirty() {
        Room old = room();
        Room next = room();
        Booking guest = movingToday(old, next);
        vacatedRooms.runForToday();

        Booking moved = bookingService.assignSegmentRoomUnit(
                guest.getId(), guest.getSegments().get(1).getId(), new RoomUnitAssignmentInput().roomUnitId(next.unit().getId()));

        assertThat(moved.getPendingRoomMove()).isNull();
        assertThat(moved.getRoomSegmentId()).isEqualTo(moved.getCurrentSegmentId());
        assertThat(tile(next).getCurrentBooking().getBookingId()).isEqualTo(guest.getId());
        assertThat(tile(next).getCurrentBooking().getPendingRoomMove()).isNull();
        assertThat(tile(old).getCurrentBooking()).isNull();
        assertThat(housekeeping(old)).isEqualTo(HousekeepingStatus.DIRTY);
        assertThat(housekeeping(next)).isEqualTo(HousekeepingStatus.CLEAN);
    }

    @Test
    void theOldRoomIsNotGivenToAnybodyElseForTonight_butItsLaterNightsAre() {
        Room old = room();
        movingToday(old, room());
        Booking tonight = booking(room(), false, "Tonight", TODAY, TODAY.plusDays(1));
        // The type of the old room has its one unit free by count: only the unit check knows somebody is in it.
        Booking sameType = bookingService.createStaffBooking(new StaffBookingCreateInput(
                old.type().getId(), "Same Type", TODAY.toString(), TODAY.plusDays(1).toString(), BookingChannel.WALK_IN, 1));

        assertThatThrownBy(() -> bookingService.assignRoomUnit(sameType.getId(), new RoomUnitAssignmentInput().roomUnitId(old.unit().getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Room " + old.unit().getLabel() + " is still occupied by Moving Guest (checked in, new room not chosen yet)."
                        + " Choose their new room first, or use another room.");
        assertThat(tonight.getId()).isNotNull();
        Booking later = booking(old, false, "Later", TODAY.plusDays(1), TODAY.plusDays(2));
        assertThat(bookingService.assignRoomUnit(later.getId(), new RoomUnitAssignmentInput().roomUnitId(old.unit().getId()))
                        .getRoomUnitId().get())
                .isEqualTo(old.unit().getId());
    }

    @Test
    void takingTheOnlyRoomOffAGuestInTheHouse_isRefused() {
        Room only = room();
        Booking guest = booking(only, true, "One Room", TODAY.minusDays(1), TODAY.plusDays(2));
        arrived(guest);

        assertThatThrownBy(() -> bookingService.assignRoomUnit(guest.getId(), new RoomUnitAssignmentInput().roomUnitId(null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void aDateChangeThatDropsTheOnlyRoom_isRefused_andThePreviewSaysSo() {
        Room only = room();
        Booking guest = booking(only, true, "One Room", TODAY.minusDays(1), TODAY.plusDays(2));
        arrived(guest);
        String id = guest.getId();
        BookingScheduleInput longer = new BookingScheduleInput(guest.getCheckIn(), TODAY.plusDays(3).toString()).roomUnitId(null);

        BookingScheduleQuote quote = bookingService.quoteSchedule(id, longer);
        assertThat(quote.getAvailable()).isFalse();
        assertThat(quote.getReason().orElse(null)).isEqualTo("A checked-in guest needs a room");
        assertThatThrownBy(() -> bookingService.updateSchedule(id, longer)).isInstanceOf(ValidationException.class);
    }

    @Test
    void extendingAStayWhoseNewRoomIsStillOpen_keepsTheGuestWhereTheyAre() {
        Room old = room();
        Booking guest = movingToday(old, room());
        BookingScheduleInput longer = new BookingScheduleInput(guest.getCheckIn(), TODAY.plusDays(4).toString()).roomUnitId(null);

        assertThat(bookingService.quoteSchedule(guest.getId(), longer).getAvailable()).isTrue();
        Booking extended = bookingService.updateSchedule(guest.getId(), longer);

        assertThat(extended.getPendingRoomMove().getStayingInRoom()).isEqualTo(old.unit().getLabel());
        assertThat(housekeeping(old)).isEqualTo(HousekeepingStatus.CLEAN);
    }

    @Test
    void takingTheNewRoomOff_isRefusedWhenSomebodyElseHasTheOldOne() {
        Room old = room();
        Room next = room();
        Booking guest = booking(old, true, "Moved Already", TODAY.minusDays(2), TODAY.plusDays(3));
        guest = bookingService.relocate(
                guest.getId(), new RelocationInput(TODAY.toString(), next.type().getId()).roomUnitId(next.unit().getId()));
        arrived(guest);
        Booking newcomer = booking(old, true, "Newcomer", TODAY, TODAY.plusDays(2));
        occupancyService.checkIn(newcomer.getId());

        String id = guest.getId();
        String segmentId = guest.getSegments().get(1).getId();
        assertThatThrownBy(() -> bookingService.assignSegmentRoomUnit(id, segmentId, new RoomUnitAssignmentInput().roomUnitId(null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Newcomer");
    }

    @Test
    void aMoveStillToCome_isNotFilledInFromTheOldRoom() {
        Room old = room();
        Booking guest = booking(old, true, "Moves Tomorrow", TODAY.minusDays(2), TODAY.plusDays(3));
        guest = bookingService.relocate(guest.getId(), new RelocationInput(TODAY.plusDays(1).toString(), room().type().getId()));
        arrived(guest);

        assertThat(bookingService.getById(guest.getId()).getPendingRoomMove()).isNull();
        assertThat(OverstayRule.unitOn(
                        bookingRepository.findById(guest.getId()).orElseThrow(),
                        segmentRepository.findByBookingIdOrderByCheckInAsc(guest.getId()), TODAY.plusDays(1), clock))
                .isNull();
    }

    @Test
    void aLateArrivalNeverSleptInThePartAlreadyOver_soTheyAreNotPutThere() {
        Room never = room();
        Room tonight = room();
        Booking guest = booking(never, true, "Came Late", TODAY.minusDays(2), TODAY.plusDays(2));
        guest = bookingService.relocate(
                guest.getId(), new RelocationInput(TODAY.toString(), tonight.type().getId()).roomUnitId(tonight.unit().getId()));
        occupancyService.checkIn(guest.getId());

        String id = guest.getId();
        String segmentId = guest.getSegments().get(1).getId();
        assertThatThrownBy(() -> bookingService.assignSegmentRoomUnit(id, segmentId, new RoomUnitAssignmentInput().roomUnitId(null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void aRoomStillLivedInCannotBeSwitchedOff() {
        Room old = room();
        movingToday(old, room());

        assertThatThrownBy(() -> roomUnitService.update(old.unit().getId(), new RoomUnitUpdateInput(old.unit().getLabel(), false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Moving Guest");
    }

    @Test
    void theMapKeepsTheGuestInTheOldRoom() {
        Room old = room();
        Booking guest = movingToday(old, room());

        assertThat(tile(old).getCurrentBooking()).isNotNull();
        assertThat(tile(old).getCurrentBooking().getBookingId()).isEqualTo(guest.getId());
    }

    @Test
    void theMorningSweepLeavesTheRoomStillLivedInClean() {
        Room old = room();
        movingToday(old, room());

        vacatedRooms.runForToday();

        assertThat(housekeeping(old)).isEqualTo(HousekeepingStatus.CLEAN);
    }

    @Test
    void takingTheRoomOffTonightsPartByHand_leavesTheGuestInTheOneBefore() {
        Room old = room();
        Room next = room();
        Booking guest = booking(old, true, "Unassigned By Hand", TODAY.minusDays(2), TODAY.plusDays(3));
        guest = bookingService.relocate(
                guest.getId(), new RelocationInput(TODAY.toString(), next.type().getId()).roomUnitId(next.unit().getId()));
        arrived(guest);

        bookingService.assignSegmentRoomUnit(guest.getId(), guest.getSegments().get(1).getId(), new RoomUnitAssignmentInput().roomUnitId(null));

        assertThat(tile(old).getCurrentBooking()).isNotNull();
    }

    @Test
    void anArrivalIsNotCheckedIntoTheRoomStillLivedIn() {
        Room old = room();
        Booking guest = booking(old, true, "Moving Guest", TODAY.minusDays(2), TODAY.plusDays(3));
        guest = bookingService.relocate(guest.getId(), new RelocationInput(TODAY.toString(), room().type().getId()));
        // Booked into the old room from today while the move was only planned - the room's night is free.
        Booking arrival = booking(old, true, "Arrival", TODAY, TODAY.plusDays(2));
        arrived(guest);

        String arrivalId = arrival.getId();
        assertThatThrownBy(() -> occupancyService.checkIn(arrivalId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Moving Guest");
    }

    @Test
    void checkingOutThatDay_marksTheOldRoomDirty() {
        Room old = room();
        Booking guest = movingToday(old, room());

        occupancyService.checkOut(guest.getId(), null);

        assertThat(housekeeping(old)).isEqualTo(HousekeepingStatus.DIRTY);
    }
}
