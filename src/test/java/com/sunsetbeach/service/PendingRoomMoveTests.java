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
import com.sunsetbeach.model.CalendarBooking;
import com.sunsetbeach.model.RoomStillHeld;
import com.sunsetbeach.model.RoomStillHeldState;
import com.sunsetbeach.model.RoomUnitAvailability;
import com.sunsetbeach.model.HousekeepingRoom;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * A checked-in guest whose move takes effect with no new room picked stays in the room they slept in: on the map, as
 * an occupant, for housekeeping. And the warning on a guest whose planned move puts them into a room an earlier guest
 * has not left (waiting, past check-out or due out). "Today" is pinned to 2034-03-10.
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
    @Autowired private HousekeepingBoardService housekeepingBoard;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private com.sunsetbeach.repository.BookingSegmentRepository segmentRepository;
    @Autowired private ReportService reportService;
    @Autowired private RoomUnitService roomUnitService;
    @Autowired private BookingCalendarService calendarService;
    @Autowired private AvailabilityService availabilityService;
    @Autowired private jakarta.persistence.EntityManager entityManager;
    @Autowired private java.time.Clock clock;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;

    private record Room(RoomEntity type, RoomUnitEntity unit) {}

    /** A second physical room of a type. */
    private RoomUnitEntity unitOf(RoomEntity type) {
        RoomUnitEntity u = new RoomUnitEntity();
        u.setRoomId(type.getId());
        u.setLabel("PM-" + UUID.randomUUID().toString().substring(0, 8));
        u.setActive(true);
        u.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        return roomUnitRepository.saveAndFlush(u);
    }

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

    private HousekeepingRoom board(Room room) {
        return housekeepingBoard.rooms().stream().filter(r -> r.getRoomUnitId().equals(room.unit().getId())).findFirst().orElseThrow();
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

    private List<CalendarBooking> bars(Booking booking) {
        // As a request of its own reads them: segments written in this transaction carry no loaded booking.
        entityManager.flush();
        entityManager.clear();
        return calendarService.getCalendar(TODAY, TODAY.plusDays(7)).getBookings().stream()
                .filter(b -> b.getBookingId().equals(booking.getId()))
                .toList();
    }

    private int typeCountTonight(Room room) {
        entityManager.flush();
        entityManager.clear();
        return calendarService.getCalendar(TODAY, TODAY.plusDays(1)).getRoomTypes().stream()
                .filter(t -> t.getRoomId().equals(room.type().getId()))
                .findFirst().orElseThrow()
                .getDailyAvailable().get(0).getAvailableCount();
    }

    private RoomUnitAvailability unitOn(Room room, LocalDate date) {
        return availabilityService.getAvailability(room.type().getId(), "2034-03").getDays().stream()
                .filter(d -> d.getDate().equals(date.toString()))
                .findFirst().orElseThrow()
                .getUnits().get(0);
    }

    @Test
    void theCalendarListsTheOldRoomForTonight_besideThePartWithoutARoom_andCountsTheNightOnce() {
        Room old = room();
        Room next = room();
        Booking guest = movingToday(old, next);

        List<CalendarBooking> bars = bars(guest);
        // The old part ended before the window and is listed for its tonight; the new one stays unassigned.
        assertThat(bars).extracting(b -> b.getRoomUnitId().orElse(null), CalendarBooking::getWaitingForRoomNight)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(old.unit().getId(), TODAY.toString()),
                        org.assertj.core.groups.Tuple.tuple(null, null));
        assertThat(typeCountTonight(old)).isEqualTo(1);
        assertThat(typeCountTonight(next)).isZero();
    }

    @Test
    void theRoomListMarksTheOldRoomTakenTonightOnly_withoutCountingIt() {
        Room old = room();
        Booking guest = movingToday(old, room());

        RoomUnitAvailability tonight = unitOn(old, TODAY);
        assertThat(tonight.getIsBooked()).isTrue();
        assertThat(tonight.getIsAvailable()).isFalse();
        assertThat(tonight.getBookingId().get()).isEqualTo(guest.getId());
        assertThat(tonight.getGuestName().get()).isEqualTo("Moving Guest");
        assertThat(tonight.getWaitingForRoom()).isTrue();
        assertThat(unitOn(old, TODAY.plusDays(1)).getIsBooked()).isFalse();
        assertThat(unitOn(old, TODAY.plusDays(1)).getWaitingForRoom()).isFalse();
        var day = availabilityService.getAvailability(old.type().getId(), "2034-03").getDays().get(TODAY.getDayOfMonth() - 1);
        assertThat(day.getBookedCount()).isZero();
        assertThat(day.getAvailableCount()).isEqualTo(1);
    }

    @Test
    void aGuestDueOutTodayHoldsNoNight_andAnOverdueOneStillDoes() {
        Room dueOut = room();
        Booking leaving = booking(dueOut, true, "Leaves Today", TODAY.minusDays(3), TODAY);
        leaving = bookingService.relocate(leaving.getId(), new RelocationInput(TODAY.minusDays(1).toString(), room().type().getId()));
        arrived(leaving);
        Room overdueIn = room();
        Booking overdue = booking(overdueIn, true, "Overdue", TODAY.minusDays(4), TODAY.minusDays(1));
        overdue = bookingService.relocate(overdue.getId(), new RelocationInput(TODAY.minusDays(2).toString(), room().type().getId()));
        arrived(overdue);

        // Nothing of theirs reaches the window: the stay ends today, and no earlier part is listed for tonight.
        assertThat(bars(leaving)).isEmpty();
        assertThat(unitOn(dueOut, TODAY).getIsBooked()).isFalse();
        assertThat(bars(overdue)).extracting(CalendarBooking::getWaitingForRoomNight).contains(TODAY.toString());
        assertThat(unitOn(overdueIn, TODAY).getWaitingForRoom()).isTrue();
    }

    /** Both move today: {@code waiting} out of {@code shared} with no room picked, {@code incoming} into it as planned. */
    private record Doubled(Room shared, Room vacated, Room target, Booking waiting, Booking incoming) {}

    private Doubled twoInOneRoom() {
        Room shared = room();
        Room vacated = room();
        Room target = room();
        Booking waiting = booking(shared, true, "Waits", TODAY.minusDays(2), TODAY.plusDays(3));
        waiting = bookingService.relocate(waiting.getId(), new RelocationInput(TODAY.toString(), target.type().getId()));
        Booking incoming = booking(vacated, true, "Moves In", TODAY.minusDays(2), TODAY.plusDays(3));
        incoming = bookingService.relocate(
                incoming.getId(), new RelocationInput(TODAY.toString(), shared.type().getId()).roomUnitId(shared.unit().getId()));
        arrived(waiting);
        arrived(incoming);
        return new Doubled(shared, vacated, target, waiting, incoming);
    }

    @Test
    void theGuestMovedIntoARoomStillHeld_isWarned_onToday_theBookingAndTheMap() {
        Doubled d = twoInOneRoom();
        String line = "Room " + d.shared().unit().getLabel() + " is still held by Waits (waiting for a new room)";

        RoomStillHeld onToday = inHouse(d.incoming()).getRoomStillHeld();
        assertThat(onToday.getReason()).isEqualTo(line);
        assertThat(onToday.getBookingId()).isEqualTo(d.waiting().getId());
        assertThat(onToday.getState()).isEqualTo(RoomStillHeldState.AWAITING_ROOM);
        assertThat(inHouse(d.waiting()).getRoomStillHeld()).isNull();
        assertThat(inHouse(d.waiting()).getPendingRoomMove()).isNotNull();
        assertThat(bookingService.getById(d.incoming().getId()).getRoomStillHeld().getReason()).isEqualTo(line);
        assertThat(bookingService.getById(d.waiting().getId()).getRoomStillHeld()).isNull();
        assertThat(tile(d.shared()).getCurrentBooking().getBookingId()).isEqualTo(d.incoming().getId());
        assertThat(tile(d.shared()).getCurrentBooking().getRoomStillHeld().getReason()).isEqualTo(line);
        // The calendar has both in the room tonight; the room list names the one whose stay says so.
        assertThat(bars(d.waiting()))
                .anyMatch(b -> d.shared().unit().getId().equals(b.getRoomUnitId().orElse(null)) && b.getWaitingForRoomNight() != null);
        assertThat(unitOn(d.shared(), TODAY).getBookingId().get()).isEqualTo(d.incoming().getId());
        assertThat(unitOn(d.shared(), TODAY).getWaitingForRoom()).isFalse();
    }

    @Test
    void theWarningGoesOnceTheWaitingGuestsRoomIsPicked() {
        Doubled d = twoInOneRoom();

        bookingService.assignSegmentRoomUnit(d.waiting().getId(), d.waiting().getSegments().get(1).getId(),
                new RoomUnitAssignmentInput().roomUnitId(d.target().unit().getId()));

        assertThat(inHouse(d.incoming()).getRoomStillHeld()).isNull();
        assertThat(bookingService.getById(d.incoming().getId()).getRoomStillHeld()).isNull();
        assertThat(tile(d.shared()).getCurrentBooking().getRoomStillHeld()).isNull();
        // Just left by the guest who lived in it.
        assertThat(housekeeping(d.shared())).isEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void theSweepDoesNotDirtyTheRoomAnotherGuestMovedIntoThatDay() {
        Doubled d = twoInOneRoom();

        vacatedRooms.runForToday();

        assertThat(housekeeping(d.shared())).isEqualTo(HousekeepingStatus.CLEAN);
        assertThat(housekeeping(d.vacated())).isEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void movingTheIncomingGuestElsewhere_leavesTheRoomStillLivedInClean() {
        Doubled d = twoInOneRoom();
        Room other = room();

        bookingService.relocate(d.incoming().getId(),
                new RelocationInput(TODAY.toString(), other.type().getId()).roomUnitId(other.unit().getId()));

        assertThat(housekeeping(d.shared())).isEqualTo(HousekeepingStatus.CLEAN);
        assertThat(inHouse(d.incoming()).getRoomStillHeld()).isNull();
    }

    @Test
    void theSweepLeavesAWaitingGuestsRoomClean_whenSomebodyElsesSegmentEndsInIt() {
        Room shared = room();
        Room other = room();
        // Waiting since yesterday; somebody else had the room for last night and moves on today.
        Booking waiting = booking(shared, true, "Waits", TODAY.minusDays(3), TODAY.plusDays(3));
        waiting = bookingService.relocate(waiting.getId(), new RelocationInput(TODAY.minusDays(1).toString(), room().type().getId()));
        Booking passing = booking(shared, true, "Passing", TODAY.minusDays(1), TODAY.plusDays(3));
        passing = bookingService.relocate(
                passing.getId(), new RelocationInput(TODAY.toString(), other.type().getId()).roomUnitId(other.unit().getId()));
        arrived(waiting);
        arrived(passing);

        vacatedRooms.runForToday();

        assertThat(housekeeping(shared)).isEqualTo(HousekeepingStatus.CLEAN);
    }

    /**
     * {@code earlier} stays in {@code shared} until {@code earlierOut} and is not checked out; {@code incoming} was
     * planned into it from {@code movesIn} until {@code incomingOut}.
     */
    private record Overlap(Room shared, Room vacated, Booking earlier, Booking incoming) {}

    private Overlap notLeft(LocalDate earlierOut, LocalDate movesIn, LocalDate incomingOut) {
        Room shared = room();
        Room vacated = room();
        Booking earlier = booking(shared, true, "Stays On", TODAY.minusDays(5), earlierOut);
        Booking incoming = booking(vacated, true, "Moves In", TODAY.minusDays(5), incomingOut);
        incoming = bookingService.relocate(
                incoming.getId(), new RelocationInput(movesIn.toString(), shared.type().getId()).roomUnitId(shared.unit().getId()));
        arrived(earlier);
        arrived(incoming);
        return new Overlap(shared, vacated, earlier, incoming);
    }

    /** The same line, on every screen that carries it, on the incoming guest only. */
    private void assertHeld(Overlap o, RoomStillHeldState state, String why) {
        String line = "Room " + o.shared().unit().getLabel() + " is still held by Stays On (" + why + ")";
        RoomStillHeld onToday = inHouse(o.incoming()).getRoomStillHeld();
        assertThat(onToday.getReason()).isEqualTo(line);
        assertThat(onToday.getState()).isEqualTo(state);
        assertThat(onToday.getBookingId()).isEqualTo(o.earlier().getId());
        assertThat(bookingService.getById(o.incoming().getId()).getRoomStillHeld().getReason()).isEqualTo(line);
        assertThat(tile(o.shared()).getCurrentBooking().getBookingId()).isEqualTo(o.incoming().getId());
        assertThat(tile(o.shared()).getCurrentBooking().getRoomStillHeld().getReason()).isEqualTo(line);
        assertThat(inHouse(o.earlier()).getRoomStillHeld()).isNull();
        assertThat(bookingService.getById(o.earlier().getId()).getRoomStillHeld()).isNull();
    }

    private void assertNotHeld(Overlap o) {
        assertThat(inHouse(o.incoming()).getRoomStillHeld()).isNull();
        assertThat(bookingService.getById(o.incoming().getId()).getRoomStillHeld()).isNull();
        assertThat(tile(o.shared()).getCurrentBooking().getRoomStillHeld()).isNull();
    }

    @Test
    void anEarlierGuestPastCheckOut_isNamedToTheGuestMovedIn() {
        assertHeld(notLeft(TODAY.minusDays(1), TODAY, TODAY.plusDays(3)), RoomStillHeldState.PAST_CHECK_OUT, "past check-out");
    }

    @Test
    void anEarlierGuestDueOutToday_isNamedToTheGuestMovedIn() {
        assertHeld(notLeft(TODAY, TODAY, TODAY.plusDays(3)), RoomStillHeldState.DUE_OUT_TODAY, "due out today");
    }

    /** The tile stands for the guest moved in; housekeeping still has to hear that somebody is leaving this room. */
    @Test
    void housekeepingSeesTheEarlierGuestsDeparture_notOnlyTheGuestMovedIn() {
        Overlap overdue = notLeft(TODAY.minusDays(1), TODAY, TODAY.plusDays(3));
        Overlap dueOut = notLeft(TODAY, TODAY, TODAY.plusDays(3));

        assertThat(board(overdue.shared()).getOverdueDays()).isEqualTo(1);
        assertThat(board(overdue.shared()).getDepartingToday()).isFalse();
        assertThat(board(dueOut.shared()).getDepartingToday()).isTrue();
        assertThat(board(dueOut.shared()).getOverdueDays()).isZero();
    }

    @Test
    void theLineStays_whenTheGuestMovedInIsDueOutOrRunsOverThemselves() {
        assertHeld(notLeft(TODAY.minusDays(1), TODAY.minusDays(1), TODAY), RoomStillHeldState.PAST_CHECK_OUT, "past check-out");
        // Both over: the longer-gone date does not make them the incoming one - they were in the room first.
        assertHeld(notLeft(TODAY.minusDays(3), TODAY.minusDays(3), TODAY.minusDays(1)), RoomStillHeldState.PAST_CHECK_OUT, "past check-out");
    }

    @Test
    void aWaitingGuestWhoIsAlsoPastCheckOut_isCalledPastCheckOut() {
        Room shared = room();
        Booking earlier = booking(shared, true, "Stays On", TODAY.minusDays(5), TODAY.minusDays(1));
        earlier = bookingService.relocate(earlier.getId(), new RelocationInput(TODAY.minusDays(2).toString(), room().type().getId()));
        Booking incoming = booking(room(), true, "Moves In", TODAY.minusDays(5), TODAY.plusDays(3));
        incoming = bookingService.relocate(
                incoming.getId(), new RelocationInput(TODAY.toString(), shared.type().getId()).roomUnitId(shared.unit().getId()));
        arrived(earlier);
        arrived(incoming);

        assertThat(inHouse(incoming).getRoomStillHeld().getState()).isEqualTo(RoomStillHeldState.PAST_CHECK_OUT);
    }

    @Test
    void checkingTheEarlierGuestOut_clearsTheLine_andMarksTheRoomDirty() {
        for (LocalDate earlierOut : List.of(TODAY.minusDays(1), TODAY)) {
            Overlap o = notLeft(earlierOut, TODAY, TODAY.plusDays(3));

            occupancyService.checkOut(o.earlier().getId(), null);

            assertNotHeld(o);
            assertThat(housekeeping(o.shared())).isEqualTo(HousekeepingStatus.DIRTY);
        }
    }

    @Test
    void extendingTheEarlierGuest_needsAnotherRoom_andThatClearsTheLine() {
        Overlap o = notLeft(TODAY.minusDays(1), TODAY, TODAY.plusDays(3));
        RoomUnitEntity spare = unitOf(o.shared().type());
        BookingScheduleInput longer = new BookingScheduleInput(TODAY.minusDays(5).toString(), TODAY.plusDays(2).toString());

        assertThatThrownBy(() -> bookingService.updateSchedule(o.earlier().getId(), longer.roomUnitId(o.shared().unit().getId())))
                .isInstanceOf(ConflictException.class);
        bookingService.updateSchedule(o.earlier().getId(), longer.roomUnitId(spare.getId()));

        assertNotHeld(o);
    }

    @Test
    void movingTheGuestMovedInElsewhere_leavesTheEarlierGuestsRoomClean() {
        for (LocalDate earlierOut : List.of(TODAY.minusDays(1), TODAY)) {
            Overlap o = notLeft(earlierOut, TODAY, TODAY.plusDays(3));
            Room other = room();

            bookingService.relocate(o.incoming().getId(),
                    new RelocationInput(TODAY.toString(), other.type().getId()).roomUnitId(other.unit().getId()));

            assertThat(inHouse(o.incoming()).getRoomStillHeld()).isNull();
            assertThat(housekeeping(o.shared())).isEqualTo(HousekeepingStatus.CLEAN);
        }
    }

    @Test
    void theSweepMarksTheRoomTheGuestMovedFrom_notTheOneTheEarlierGuestIsStillIn() {
        Overlap o = notLeft(TODAY, TODAY, TODAY.plusDays(3));

        vacatedRooms.runForToday();

        assertThat(housekeeping(o.shared())).isEqualTo(HousekeepingStatus.CLEAN);
        assertThat(housekeeping(o.vacated())).isEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void theSweepLeavesTheRoomClean_whenTheGuestMovedInMovesOnWhileTheEarlierOneIsStillThere() {
        Room shared = room();
        Room onward = room();
        Booking earlier = booking(shared, true, "Stays On", TODAY.minusDays(5), TODAY.minusDays(2));
        // In the shared room for the last two nights, planned on into another one today.
        Booking passing = booking(room(), true, "Passing", TODAY.minusDays(5), TODAY.plusDays(3));
        passing = bookingService.relocate(
                passing.getId(), new RelocationInput(TODAY.minusDays(2).toString(), shared.type().getId()).roomUnitId(shared.unit().getId()));
        passing = bookingService.relocate(
                passing.getId(), new RelocationInput(TODAY.toString(), onward.type().getId()).roomUnitId(onward.unit().getId()));
        arrived(earlier);
        arrived(passing);

        vacatedRooms.runForToday();

        assertThat(housekeeping(shared)).isEqualTo(HousekeepingStatus.CLEAN);
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
