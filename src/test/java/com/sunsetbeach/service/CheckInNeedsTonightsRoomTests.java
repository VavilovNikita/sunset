package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.NightAuditBooking;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.PropertyMap;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
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
 * Check-in needs a room for the part of the stay the guest sleeps in today - not for the last one, which a move
 * planned for a later night may leave without a room. "Today" is pinned to 2034-03-10.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
@Transactional
class CheckInNeedsTonightsRoomTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private PropertyMapService propertyMapService;
    @Autowired private NightAuditService nightAuditService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;

    private record Room(RoomEntity type, RoomUnitEntity unit) {}

    private Room room() {
        RoomEntity r = new RoomEntity();
        r.setName("Tonight Room " + UUID.randomUUID());
        r.setDescription("Used only by CheckInNeedsTonightsRoomTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(r);
        RoomUnitEntity u = new RoomUnitEntity();
        u.setRoomId(saved.getId());
        u.setLabel("TN-" + UUID.randomUUID().toString().substring(0, 8));
        u.setActive(true);
        u.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        return new Room(saved, roomUnitRepository.saveAndFlush(u));
    }

    private Booking booking(Room room, boolean assigned, LocalDate checkIn, LocalDate checkOut) {
        Booking b = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.type().getId(), "Tonight Guest " + UUID.randomUUID(), checkIn.toString(), checkOut.toString(), BookingChannel.WALK_IN, 1));
        return assigned ? bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(room.unit().getId())) : b;
    }

    /** A move on {@code date} into {@code to}, with or without a room picked for it. */
    private Booking move(Booking booking, LocalDate date, Room to, boolean assigned) {
        RelocationInput input = new RelocationInput(date.toString(), to.type().getId());
        if (assigned) {
            input.roomUnitId(to.unit().getId());
        }
        return bookingService.relocate(booking.getId(), input);
    }

    @Test
    void tonightHasARoom_andTheLaterMoveHasNone_checksIn() {
        Room tonight = room();
        Booking guest = booking(tonight, true, TODAY, TODAY.plusDays(4));
        move(guest, TODAY.plusDays(2), room(), false);

        assertThat(occupancyService.checkIn(guest.getId()).getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
        assertThat(propertyMapService.get().getUnits().stream().filter(u -> u.getRoomUnitId().equals(tonight.unit().getId())).findFirst()
                .orElseThrow().getCurrentBooking().getBookingId()).isEqualTo(guest.getId());
    }

    @Test
    void tonightHasNoRoom_thoughTheLaterMoveHasOne_isRefusedNamingTheNight() {
        Room first = room();
        Room later = room();
        Booking guest = booking(first, false, TODAY, TODAY.plusDays(4));
        move(guest, TODAY.plusDays(2), later, true);

        assertThatThrownBy(() -> occupancyService.checkIn(guest.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Assign a room for the night of 10 Mar 2034 before checking in.");
        assertThat(bookingService.getById(guest.getId()).getOccupancyStatus()).isEqualTo(OccupancyStatus.EXPECTED);
    }

    @Test
    void theScreensAreToldWhichPartNeedsTheRoom_andNothingStandsInForIt() {
        Room first = room();
        Room later = room();
        Booking guest = booking(first, false, TODAY, TODAY.plusDays(4));
        guest = move(guest, TODAY.plusDays(2), later, true);
        String segmentId = guest.getSegments().get(0).getId();

        Booking read = bookingService.getById(guest.getId());
        assertThat(read.getCheckInRoomNeeded().getSegmentId()).isEqualTo(segmentId);
        assertThat(read.getCheckInRoomNeeded().getRoomId()).isEqualTo(first.type().getId());
        assertThat(read.getCheckInRoomNeeded().getRoomName()).isEqualTo(first.type().getName());
        assertThat(read.getCheckInRoomNeeded().getNight()).isEqualTo(TODAY.toString());
        assertThat(read.getCheckInRoomNeeded().getCheckOut()).isEqualTo(TODAY.plusDays(2).toString());
        assertThat(read.getCheckInRoomNeeded().getReason()).isEqualTo("Assign a room for the night of 10 Mar 2034 before checking in.");
        // The later room is not where they arrive: nothing is said about whoever is in it, and its tile is empty.
        assertThat(read.getRoomOccupant()).isNull();
        PropertyMap map = propertyMapService.get();
        assertThat(map.getUnits().stream().filter(u -> u.getRoomUnitId().equals(later.unit().getId())).findFirst().orElseThrow().getCurrentBooking())
                .isNull();
        String id = guest.getId();
        assertThat(map.getArrivalsWithoutRoom()).filteredOn(a -> a.getBookingId().equals(id))
                .singleElement()
                .satisfies(a -> assertThat(a.getRoomNeeded().getSegmentId()).isEqualTo(segmentId));
        NightAuditBooking row = nightAuditService.get(null).getMissedArrivals().stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow();
        assertThat(row.getRoomUnitLabel().orElse(null)).isNull();
        assertThat(row.getCheckInRoomNeeded().getSegmentId()).isEqualTo(segmentId);

        bookingService.assignSegmentRoomUnit(id, segmentId, new RoomUnitAssignmentInput().roomUnitId(first.unit().getId()));

        assertThat(bookingService.getById(id).getCheckInRoomNeeded()).isNull();
        assertThat(propertyMapService.get().getArrivalsWithoutRoom()).noneMatch(a -> a.getBookingId().equals(id));
        assertThat(occupancyService.checkIn(id).getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
    }

    @Test
    void aLateArrival_needsOnlyTonightsRoom_notTheOneOfTheNightsAlreadyGone() {
        Room gone = room();
        Room tonight = room();
        Booking guest = booking(gone, false, TODAY.minusDays(2), TODAY.plusDays(2));
        move(guest, TODAY.minusDays(1), tonight, true);

        assertThat(bookingService.getById(guest.getId()).getCheckInRoomNeeded()).isNull();
        assertThat(occupancyService.checkIn(guest.getId()).getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
    }

    @Test
    void onTheDepartureDay_theRoomAskedForIsTheLastNights() {
        Room first = room();
        Booking guest = booking(first, true, TODAY.minusDays(2), TODAY);
        move(guest, TODAY.minusDays(1), room(), false);

        assertThatThrownBy(() -> occupancyService.checkIn(guest.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Assign a room for the night of 9 Mar 2034 before checking in.");
        assertThat(bookingService.getById(guest.getId()).getCheckInRoomNeeded().getNight()).isEqualTo(TODAY.minusDays(1).toString());
    }

    @Test
    void aBookingNotArrivingYet_asksForNothing() {
        Booking future = booking(room(), false, TODAY.plusDays(1), TODAY.plusDays(3));

        assertThat(bookingService.getById(future.getId()).getCheckInRoomNeeded()).isNull();
    }

    @Test
    void aBookingThatNeverMoves_isRefusedWithoutARoom_andChecksInWithOne() {
        Room room = room();
        Booking guest = booking(room, false, TODAY, TODAY.plusDays(2));

        assertThatThrownBy(() -> occupancyService.checkIn(guest.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Assign a room before checking in.");
        bookingService.assignRoomUnit(guest.getId(), new RoomUnitAssignmentInput().roomUnitId(room.unit().getId()));
        assertThat(occupancyService.checkIn(guest.getId()).getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
    }
}
