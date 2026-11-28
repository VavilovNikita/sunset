package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.CheckOutPreview;
import com.sunsetbeach.model.CheckOutResult;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.PropertyMapUnit;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RelocationQuote;
import com.sunsetbeach.model.RelocationUndoInput;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.SwapSegmentRoomUnitInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.support.FixedHotelClockConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * A guest who is in the house is never moved into a room another guest is checked into - the same block as check-in -
 * and what the screens are told so they can say it first. Also the two figures of an overstaying guest's check-out.
 * "Today" is pinned to 2034-03-10.
 *
 * <p>The occupant used here is one the availability checks do not see: due out today (holds no night from today on).
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
@Transactional
class MoveIntoOccupiedRoomTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private PropertyMapService propertyMapService;
    @Autowired private OverstayChargeService overstayChargeService;
    @Autowired private com.sunsetbeach.repository.BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    private record Type(RoomEntity room, List<RoomUnitEntity> units) {
        String unit(int i) {
            return units.get(i).getId();
        }

        String label(int i) {
            return units.get(i).getLabel();
        }
    }

    private Type type(int unitCount) {
        RoomEntity r = new RoomEntity();
        r.setName("Move Room " + UUID.randomUUID());
        r.setDescription("Used only by MoveIntoOccupiedRoomTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(r);
        List<RoomUnitEntity> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            RoomUnitEntity u = new RoomUnitEntity();
            u.setRoomId(saved.getId());
            u.setLabel("MV-" + UUID.randomUUID().toString().substring(0, 8));
            u.setActive(true);
            u.setHousekeepingStatus(HousekeepingStatus.CLEAN);
            units.add(roomUnitRepository.saveAndFlush(u));
        }
        return new Type(saved, units);
    }

    private Booking booking(Type type, int unit, String guest, LocalDate checkIn, LocalDate checkOut) {
        Booking b = bookingService.createStaffBooking(new StaffBookingCreateInput(
                type.room().getId(), guest, checkIn.toString(), checkOut.toString(), BookingChannel.WALK_IN, 1));
        return bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(type.unit(unit)));
    }

    private Booking inHouse(Type type, int unit, String guest, LocalDate checkIn, LocalDate checkOut) {
        Booking b = booking(type, unit, guest, checkIn, checkOut);
        if (checkOut.isBefore(TODAY)) {
            com.sunsetbeach.support.Overstayers.leftCheckedIn(bookingRepository, overstayChargeService, b.getId());
            return bookingService.getById(b.getId());
        }
        return occupancyService.checkIn(b.getId()).getBooking();
    }

    private String refusal(Type type, int unit, String guest) {
        return "Room " + type.label(unit) + " is still occupied by " + guest + " (checked in, due out today). Check them out first, or choose another room.";
    }

    /** Room 0: a guest due out today, still checked in. Room 1: a guest in the middle of a stay. Room 2: free. */
    private record House(Type type, Booking leaving, Booking staying) {}

    private House house() {
        Type type = type(3);
        Booking leaving = inHouse(type, 0, "Due Out", TODAY.minusDays(2), TODAY);
        Booking staying = inHouse(type, 1, "Staying", TODAY.minusDays(1), TODAY.plusDays(3));
        return new House(type, leaving, staying);
    }

    // ---- every way of moving a guest who is in the house ------------------------------------------------

    @Test
    void relocationFromToday_isRefused_andTheQuoteSaysItFirst_withTheSameSentence() {
        House h = house();
        RelocationInput input = new RelocationInput(TODAY.toString(), h.type().room().getId()).roomUnitId(h.type().unit(0));

        RelocationQuote quote = bookingService.quoteRelocation(h.staying().getId(), input);

        assertThat(quote.getAvailable()).isFalse();
        assertThat(quote.getReason().get()).isEqualTo(refusal(h.type(), 0, "Due Out"));
        assertThatThrownBy(() -> bookingService.relocate(h.staying().getId(), input))
                .isInstanceOf(ConflictException.class)
                .hasMessage(refusal(h.type(), 0, "Due Out"));
    }

    @Test
    void aMovePlannedForALaterNight_isNotAboutWhoIsThereToday() {
        House h = house();
        RelocationInput later = new RelocationInput(TODAY.plusDays(1).toString(), h.type().room().getId()).roomUnitId(h.type().unit(0));

        assertThat(bookingService.quoteRelocation(h.staying().getId(), later).getAvailable()).isTrue();
        Booking moved = bookingService.relocate(h.staying().getId(), later);

        // Tonight the guest is still where they were; the booking's own room is already the later one.
        assertThat(moved.getRoomUnitId().get()).isEqualTo(h.type().unit(0));
        assertThat(currentUnit(moved)).isEqualTo(h.type().unit(1));
        // And undoing it moves nobody either.
        bookingService.undoRelocation(moved.getId(), new RelocationUndoInput(TODAY.plusDays(1).toString()));
    }

    @Test
    void assigningARoom_orASegmentsRoom_isRefused() {
        House h = house();
        RoomUnitAssignmentInput toTaken = new RoomUnitAssignmentInput().roomUnitId(h.type().unit(0));

        assertThatThrownBy(() -> bookingService.assignRoomUnit(h.staying().getId(), toTaken))
                .isInstanceOf(ConflictException.class)
                .hasMessage(refusal(h.type(), 0, "Due Out"));
        assertThatThrownBy(() -> bookingService.assignSegmentRoomUnit(h.staying().getId(), h.staying().getSegments().get(0).getId(), toTaken))
                .isInstanceOf(ConflictException.class)
                .hasMessage(refusal(h.type(), 0, "Due Out"));
        // Their own room and a free one are fine.
        bookingService.assignRoomUnit(h.staying().getId(), new RoomUnitAssignmentInput().roomUnitId(h.type().unit(1)));
        bookingService.assignRoomUnit(h.staying().getId(), new RoomUnitAssignmentInput().roomUnitId(h.type().unit(2)));
    }

    @Test
    void aScheduleChangeIntoTheRoom_isRefused_inTheQuoteToo() {
        House h = house();
        BookingScheduleInput roomOnly =
                new BookingScheduleInput(TODAY.minusDays(1).toString(), TODAY.plusDays(3).toString()).roomUnitId(h.type().unit(0));
        // Their nights overlap too; the answer names who is in the room, as check-in does.
        assertThat(bookingService.quoteSchedule(h.staying().getId(), roomOnly).getReason().get()).isEqualTo(refusal(h.type(), 0, "Due Out"));
        assertThatThrownBy(() -> bookingService.updateSchedule(h.staying().getId(), roomOnly))
                .isInstanceOf(ConflictException.class)
                .hasMessage(refusal(h.type(), 0, "Due Out"));
    }

    @Test
    void anOverdueGuest_isNotReRoomedIntoARoomSomebodyIsIn() {
        Type type = type(2);
        Booking overdue = inHouse(type, 0, "Overdue", TODAY.minusDays(3), TODAY.minusDays(1));
        inHouse(type, 1, "In The Room", TODAY, TODAY.plusDays(1));

        // Their agreed nights are all past, so availability alone would allow it.
        assertThatThrownBy(() -> bookingService.assignRoomUnit(overdue.getId(), new RoomUnitAssignmentInput().roomUnitId(type.unit(1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("is still occupied by In The Room");
    }

    @Test
    void twoGuestsInTheHouse_canExchangeRooms_butNotWithABookingWhoseRoomSomebodyElseIsIn() {
        Type type = type(2);
        Booking a = inHouse(type, 0, "A", TODAY.minusDays(1), TODAY.plusDays(2));
        Booking b = inHouse(type, 1, "B", TODAY.minusDays(1), TODAY.plusDays(2));

        bookingService.swapSegmentRoomUnit(a.getId(), a.getSegments().get(0).getId(), new SwapSegmentRoomUnitInput(b.getSegments().get(0).getId()));
        assertThat(currentUnit(bookingService.getById(a.getId()))).isEqualTo(type.unit(1));
        assertThat(currentUnit(bookingService.getById(b.getId()))).isEqualTo(type.unit(0));

        // A third booking arriving later into room 0 is no way to swap A into it while B is there.
        Booking future = booking(type, 0, "Future", TODAY.plusDays(2), TODAY.plusDays(4));
        assertThatThrownBy(() -> bookingService.swapSegmentRoomUnit(
                        a.getId(), a.getSegments().get(0).getId(), new SwapSegmentRoomUnitInput(future.getSegments().get(0).getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("is still occupied by B");
    }

    @Test
    void undoingTodaysMove_isRefusedWhenSomebodyIsInTheEarlierRoom() {
        Type type = type(3);
        Booking mover = inHouse(type, 0, "Mover", TODAY.minusDays(2), TODAY.plusDays(2));
        bookingService.relocate(mover.getId(), new RelocationInput(TODAY.toString(), type.room().getId()).roomUnitId(type.unit(1)));
        // Somebody else has since checked into the room the guest left.
        inHouse(type, 0, "Late Booked", TODAY, TODAY.plusDays(1));

        assertThatThrownBy(() -> bookingService.undoRelocation(mover.getId(), new RelocationUndoInput(TODAY.toString())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("is still occupied by Late Booked");
    }

    @Test
    void aGuestWhoHasNotArrived_isStillAssignedFreely_andCheckInAsksLater() {
        House h = house();
        Booking arriving = booking(h.type(), 2, "Arrival", TODAY, TODAY.plusDays(2));

        Booking moved = bookingService.assignRoomUnit(arriving.getId(), new RoomUnitAssignmentInput().roomUnitId(h.type().unit(0)));

        assertThat(moved.getRoomUnitId().get()).isEqualTo(h.type().unit(0));
        assertThatThrownBy(() -> occupancyService.checkIn(arriving.getId())).isInstanceOf(ConflictException.class);
    }

    // ---- what the screens are told ------------------------------------------------------------------------

    @Test
    void aSingleBooking_saysItsRoomIsOccupied_untilTheOtherGuestLeaves_andAListNeverDoes() {
        House h = house();
        Booking arriving = booking(h.type(), 0, "Arrival", TODAY, TODAY.plusDays(2));
        Booking nextWeek = booking(h.type(), 0, "Next Week", TODAY.plusDays(7), TODAY.plusDays(9));

        assertThat(bookingService.getById(arriving.getId()).getRoomOccupant().getGuestName()).isEqualTo("Due Out");
        assertThat(bookingService.getById(arriving.getId()).getRoomOccupant().getOverdueDays()).isZero();
        assertThat(bookingService.getById(nextWeek.getId()).getRoomOccupant()).as("they will have left").isNull();
        assertThat(bookingService.getById(h.leaving().getId()).getRoomOccupant()).as("already in the room").isNull();
        // Read from the database, as a list request would: the rows made in this test have no room loaded on them.
        entityManager.flush();
        entityManager.clear();
        assertThat(bookingService.list(TODAY.toString(), TODAY.toString(), null, "Arrival").get(0).getRoomOccupant()).isNull();

        occupancyService.checkOut(h.leaving().getId(), null);
        assertThat(bookingService.getById(arriving.getId()).getRoomOccupant()).isNull();
    }

    @Test
    void theMap_showsAGuestWithAMovePlannedLaterWhereTheyAre_andWhoIsWaitingForAnOccupiedRoom() {
        House h = house();
        bookingService.relocate(h.staying().getId(),
                new RelocationInput(TODAY.plusDays(1).toString(), h.type().room().getId()).roomUnitId(h.type().unit(2)));
        Booking arriving = booking(h.type(), 0, "Arrival", TODAY, TODAY.plusDays(2));

        PropertyMapUnit occupied = mapUnit(h.type().unit(0));
        PropertyMapUnit now = mapUnit(h.type().unit(1));
        PropertyMapUnit later = mapUnit(h.type().unit(2));

        assertThat(now.getCurrentBooking().getGuestName()).isEqualTo("Staying");
        assertThat(later.getCurrentBooking()).isNull();
        assertThat(occupied.getCurrentBooking().getGuestName()).isEqualTo("Due Out");
        assertThat(occupied.getWaitingArrival().getBookingId()).isEqualTo(arriving.getId());
        assertThat(now.getWaitingArrival()).isNull();
        // A guest in the house owes what is unpaid; an arrival shown on a free room does not yet.
        assertThat(now.getCurrentBooking().getOwes()).isTrue();
        occupancyService.checkOut(h.leaving().getId(), null);
        assertThat(mapUnit(h.type().unit(0)).getCurrentBooking().getGuestName()).isEqualTo("Arrival");
        assertThat(mapUnit(h.type().unit(0)).getCurrentBooking().getOwes()).isFalse();
        assertThat(mapUnit(h.type().unit(0)).getWaitingArrival()).isNull();
    }

    // ---- an overstaying guest's check-out -------------------------------------------------------------------

    @Test
    void theCheckOutPreview_givesWhatIsOwedNow_andWhatWillBeOwed_andTheResultIsTheSecond() {
        Type type = type(1);
        Booking late = inHouse(type, 0, "Late", TODAY.minusDays(3), TODAY.minusDays(1));
        overstayChargeService.reconcile(late.getId());
        String dueNow = bookingService.getById(late.getId()).getBalanceDue();

        CheckOutPreview preview = occupancyService.previewCheckOut(late.getId());
        CheckOutResult result = occupancyService.checkOut(late.getId(), null);

        assertThat(preview.getBalanceDueNow()).isEqualTo(dueNow).isEqualTo("4000.00"); // the 2 agreed nights, yesterday's overstay and tonight's
        assertThat(preview.getTonightNotCharged()).isTrue();
        assertThat(preview.getOutstandingBalance()).isEqualTo("3000.00");
        assertThat(result.getOutstandingBalance()).isEqualTo(preview.getOutstandingBalance());
    }

    @Test
    void theCheckOutPreview_ofAStayWithinItsDates_hasOneFigure() {
        House h = house();

        CheckOutPreview preview = occupancyService.previewCheckOut(h.leaving().getId());

        assertThat(preview.getTonightNotCharged()).isFalse();
        assertThat(preview.getBalanceDueNow()).isEqualTo(preview.getOutstandingBalance());
    }

    private PropertyMapUnit mapUnit(String unitId) {
        return propertyMapService.get().getUnits().stream().filter(u -> u.getRoomUnitId().equals(unitId)).findFirst().orElseThrow();
    }

    /** The room of the segment the server says the stay is in today. */
    private static String currentUnit(Booking b) {
        return b.getSegments().stream()
                .filter(s -> s.getId().equals(b.getCurrentSegmentId()))
                .findFirst()
                .orElseThrow()
                .getRoomUnitId()
                .get();
    }
}
