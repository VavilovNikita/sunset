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
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RelocationQuote;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.TodayBoardEntry;
import com.sunsetbeach.repository.BookingRepository;
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
 * Two rules about a room type oversold by an overstaying guest: nobody is checked into a room
 * another guest is still checked into, and a move inside one type is not refused for the type's
 * load (it changes no count). "Today" is pinned to 2034-03-10.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
@Transactional
class OccupiedRoomAndOwnTypeMoveTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private OverstayChargeService overstayCharges;

    private record Type(RoomEntity room, List<RoomUnitEntity> units) {}

    private Type type(int unitCount) {
        RoomEntity r = new RoomEntity();
        r.setName("OwnType Room " + UUID.randomUUID());
        r.setDescription("Used only by OccupiedRoomAndOwnTypeMoveTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(r);
        List<RoomUnitEntity> units = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            RoomUnitEntity u = new RoomUnitEntity();
            u.setRoomId(saved.getId());
            u.setLabel("OT-" + UUID.randomUUID().toString().substring(0, 8));
            u.setActive(true);
            u.setHousekeepingStatus(HousekeepingStatus.CLEAN);
            units.add(roomUnitRepository.saveAndFlush(u));
        }
        return new Type(saved, units);
    }

    private Booking booking(Type type, RoomUnitEntity unit, String guest, LocalDate checkIn, LocalDate checkOut) {
        Booking b = bookingService.createStaffBooking(new StaffBookingCreateInput(
                type.room().getId(), guest, checkIn.toString(), checkOut.toString(), BookingChannel.WALK_IN, 1));
        if (unit != null) {
            bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
        }
        return b;
    }

    private TodayBoardEntry arrival(String bookingId) {
        return occupancyService.getTodayBoard().getArrivingToday().stream()
                .filter(e -> e.getBooking().getId().equals(bookingId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void checkIn_intoARoomAnOverstayingGuestStillHolds_isRefused_andTheTodayBoardSaysSoFirst() {
        Type type = type(2);
        RoomUnitEntity unit = type.units().get(0);
        // Both assigned while the first guest's stay had simply not been checked in yet.
        Booking late = booking(type, unit, "Late Leaver", TODAY.minusDays(3), TODAY.minusDays(1));
        Booking arriving = booking(type, unit, "New Arrival", TODAY, TODAY.plusDays(2));
        com.sunsetbeach.support.Overstayers.leftCheckedIn(bookingRepository, overstayCharges, late.getId());

        assertThat(arrival(arriving.getId()).getRoomOccupant().getGuestName()).isEqualTo("Late Leaver");
        assertThat(arrival(arriving.getId()).getRoomOccupant().getOverdueDays()).isEqualTo(1);
        assertThatThrownBy(() -> occupancyService.checkIn(arriving.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Room " + unit.getLabel() + " is still occupied by Late Leaver (checked in, past check-out)."
                        + " Check them out or move this arrival to another room first.");
        assertThat(bookingRepository.findById(arriving.getId()).orElseThrow().getOccupancyStatus()).isEqualTo(OccupancyStatus.EXPECTED);

        // The other way out: the arrival goes to the free room of the same type.
        bookingService.assignRoomUnit(arriving.getId(), new RoomUnitAssignmentInput().roomUnitId(type.units().get(1).getId()));
        assertThat(arrival(arriving.getId()).getRoomOccupant()).isNull();
        assertThat(occupancyService.checkIn(arriving.getId()).getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
    }

    @Test
    void checkIn_onTheDepartureDayOfAGuestNotYetCheckedOut_waitsForTheCheckOut() {
        Type type = type(1);
        RoomUnitEntity unit = type.units().get(0);
        Booking leaving = booking(type, unit, "Due Out", TODAY.minusDays(2), TODAY);
        Booking arriving = booking(type, unit, "Turnover Arrival", TODAY, TODAY.plusDays(2));
        occupancyService.checkIn(leaving.getId());

        assertThat(arrival(arriving.getId()).getRoomOccupant().getOverdueDays()).isZero();
        assertThatThrownBy(() -> occupancyService.checkIn(arriving.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Due Out (checked in, due out today)");

        occupancyService.checkOut(leaving.getId(), null);
        assertThat(arrival(arriving.getId()).getRoomOccupant()).isNull();
        assertThat(occupancyService.checkIn(arriving.getId()).getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
    }

    @Test
    void checkIn_isNotBlockedByAGuestInAnotherRoom() {
        Type type = type(2);
        Booking neighbour = booking(type, type.units().get(0), "Neighbour", TODAY.minusDays(1), TODAY.plusDays(2));
        Booking arriving = booking(type, type.units().get(1), "Arrival", TODAY, TODAY.plusDays(2));
        occupancyService.checkIn(neighbour.getId());

        assertThat(arrival(arriving.getId()).getRoomOccupant()).isNull();
        assertThat(occupancyService.checkIn(arriving.getId()).getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
    }

    /** Three rooms, today oversold by one: three stays booked over tonight, plus a guest who never left. */
    private record Oversold(Type type, Booking mover, Booking overstay) {}

    private Oversold oversoldType() {
        Type type = type(3);
        Booking overstay = booking(type, type.units().get(0), "Overstayer", TODAY.minusDays(3), TODAY.minusDays(1));
        Booking mover = booking(type, type.units().get(1), "Mover", TODAY, TODAY.plusDays(3));
        booking(type, null, "Unassigned One", TODAY, TODAY.plusDays(3));
        booking(type, null, "Unassigned Two", TODAY, TODAY.plusDays(3));
        com.sunsetbeach.support.Overstayers.leftCheckedIn(bookingRepository, overstayCharges, overstay.getId());
        return new Oversold(type, mover, overstay);
    }

    @Test
    void relocation_ontoAFreeRoomOfTheSameOversoldType_isAllowed_inTheQuoteAndTheApply() {
        Oversold o = oversoldType();
        RelocationInput input =
                new RelocationInput(TODAY.toString(), o.type().room().getId()).roomUnitId(o.type().units().get(2).getId());

        RelocationQuote quote = bookingService.quoteRelocation(o.mover().getId(), input);
        assertThat(quote.getAvailable()).isTrue();
        assertThat(bookingService.relocate(o.mover().getId(), input).getRoomUnitId().get()).isEqualTo(o.type().units().get(2).getId());
    }

    @Test
    void relocation_ontoATakenRoomOfTheSameType_orIntoAnotherFullType_isStillRefused() {
        Oversold o = oversoldType();
        RelocationInput occupied =
                new RelocationInput(TODAY.toString(), o.type().room().getId()).roomUnitId(o.type().units().get(0).getId());
        assertThat(bookingService.quoteRelocation(o.mover().getId(), occupied).getAvailable()).isFalse();
        assertThatThrownBy(() -> bookingService.relocate(o.mover().getId(), occupied))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("is still occupied by Overstayer");

        Type full = type(1);
        booking(full, full.units().get(0), "Holder", TODAY, TODAY.plusDays(3));
        RelocationInput other = new RelocationInput(TODAY.toString(), full.room().getId());
        assertThat(bookingService.quoteRelocation(o.mover().getId(), other).getAvailable()).isFalse();
        assertThatThrownBy(() -> bookingService.relocate(o.mover().getId(), other))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Selected room type has no availability for that period");
    }

    @Test
    void aScheduleChangeThatAddsNoNight_isNotRefusedForTheTypesLoad_butAnAddedFullNightIs() {
        Oversold o = oversoldType();
        String id = o.mover().getId();
        String freeUnit = o.type().units().get(2).getId();

        // The calendar's room-only drop: same dates, another room of the type.
        BookingScheduleInput roomOnly =
                new BookingScheduleInput(TODAY.toString(), TODAY.plusDays(3).toString()).roomUnitId(freeUnit);
        assertThat(bookingService.quoteSchedule(id, roomOnly).getAvailable()).isTrue();
        assertThat(bookingService.updateSchedule(id, roomOnly).getRoomUnitId().get()).isEqualTo(freeUnit);

        BookingScheduleInput shorter = new BookingScheduleInput(TODAY.toString(), TODAY.plusDays(2).toString()).roomUnitId(freeUnit);
        assertThat(bookingService.updateSchedule(id, shorter).getCheckOut()).isEqualTo(TODAY.plusDays(2).toString());

        // The night given back is full again once somebody else takes it.
        booking(o.type(), null, "Takes The Night", TODAY.plusDays(2), TODAY.plusDays(3));
        BookingScheduleInput longer = new BookingScheduleInput(TODAY.toString(), TODAY.plusDays(3).toString()).roomUnitId(freeUnit);
        assertThat(bookingService.quoteSchedule(id, longer).getAvailable()).isFalse();
        assertThatThrownBy(() -> bookingService.updateSchedule(id, longer))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Selected dates are no longer available");
    }
}
