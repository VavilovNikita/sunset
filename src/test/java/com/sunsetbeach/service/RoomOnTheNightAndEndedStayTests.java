package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.GymReservationEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.GymReservation;
import com.sunsetbeach.model.GymReservationStatus;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.NightAudit;
import com.sunsetbeach.model.NightAuditBooking;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.GymReservationRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.support.FixedHotelClockConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * The room shown for a guest who moves is the room of the night asked about, not the booking's last one (the gym
 * schedule and the night audit); and a stay that is over is not checked into. "Today" is pinned to 2034-03-10.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
@Transactional
class RoomOnTheNightAndEndedStayTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private GymReservationService gymReservationService;
    @Autowired private NightAuditService nightAuditService;
    @Autowired private GymReservationRepository gymReservationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;

    private record Room(RoomEntity type, RoomUnitEntity unit) {}

    private Room room(String name) {
        RoomEntity r = new RoomEntity();
        r.setName(name + " " + UUID.randomUUID());
        r.setDescription("Used only by RoomOnTheNightAndEndedStayTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(r);
        RoomUnitEntity u = new RoomUnitEntity();
        u.setRoomId(saved.getId());
        u.setLabel("RN-" + UUID.randomUUID().toString().substring(0, 8));
        u.setActive(true);
        u.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        return new Room(saved, roomUnitRepository.saveAndFlush(u));
    }

    private Booking booking(Room room, String guest, LocalDate checkIn, LocalDate checkOut) {
        Booking b = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.type().getId(), guest, checkIn.toString(), checkOut.toString(), BookingChannel.WALK_IN, 1));
        return bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(room.unit().getId()));
    }

    private void moveFrom(Booking booking, LocalDate date, Room to) {
        bookingService.relocate(booking.getId(), new RelocationInput(date.toString(), to.type().getId()).roomUnitId(to.unit().getId()));
    }

    private void gymSession(Booking booking, LocalDate date, int hour) {
        GymReservationEntity r = new GymReservationEntity();
        r.setBookingId(booking.getId());
        r.setDate(date);
        r.setStartTime(LocalTime.of(hour, 0));
        r.setDurationMinutes(60);
        r.setStatus(GymReservationStatus.BOOKED);
        r.setCreatedByUserId(staff().getId());
        gymReservationRepository.saveAndFlush(r);
    }

    private UserEntity staff() {
        UserEntity user = new UserEntity();
        user.setEmail("room-night-" + UUID.randomUUID() + "@test.local");
        user.setName(user.getEmail());
        user.setPasswordHash("unused");
        user.setRole(Role.CASHIER);
        user.setActive(true);
        return userRepository.saveAndFlush(user);
    }

    private String gymRoom(Booking booking, LocalDate date) {
        return gymReservationService.schedule(date.toString(), date.toString(), false).getReservations().stream()
                .filter(r -> r.getBookingId().equals(booking.getId()))
                .map(GymReservation::getRoomLabel)
                .findFirst()
                .orElseThrow()
                .orElse(null);
    }

    private static NightAuditBooking row(List<NightAuditBooking> rows, Booking booking) {
        return rows.stream().filter(r -> r.getId().equals(booking.getId())).findFirst().orElseThrow();
    }

    @Test
    void theGymSchedule_namesTheRoomOfTheSessionsDate_aroundAMovePlannedForTomorrow() {
        Room from = room("From");
        Room to = room("To");
        Booking guest = booking(from, "Moves Tomorrow", TODAY.minusDays(2), TODAY.plusDays(3));
        occupancyService.checkIn(guest.getId());
        moveFrom(guest, TODAY.plusDays(1), to);
        gymSession(guest, TODAY.minusDays(1), 8);
        gymSession(guest, TODAY, 9);
        gymSession(guest, TODAY.plusDays(1), 10);

        assertThat(gymRoom(guest, TODAY.minusDays(1))).isEqualTo(from.unit().getLabel());
        assertThat(gymRoom(guest, TODAY)).isEqualTo(from.unit().getLabel());
        assertThat(gymRoom(guest, TODAY.plusDays(1))).isEqualTo(to.unit().getLabel());
    }

    @Test
    void theNightAudit_namesTheRoomAndTypeOfTheReviewedNight_forMovesYesterdayTodayAndTomorrow() {
        for (int moveOffset : new int[] {-1, 0, 1}) {
            Room from = room("Audit From");
            Room to = room("Audit To");
            // Never checked in, so listed as a missed arrival for every date from check-in on.
            Booking guest = booking(from, "Moves " + moveOffset, TODAY.minusDays(3), TODAY.plusDays(3));
            LocalDate move = TODAY.plusDays(moveOffset);
            moveFrom(guest, move, to);

            for (LocalDate reviewed : List.of(move.minusDays(1), move, TODAY)) {
                Room expected = reviewed.isBefore(move) ? from : to;
                NightAudit audit = nightAuditService.get(reviewed.toString());
                NightAuditBooking row = row(audit.getMissedArrivals(), guest);
                assertThat(row.getRoomUnitLabel().orElse(null)).as("move %s, reviewed %s", move, reviewed).isEqualTo(expected.unit().getLabel());
                assertThat(row.getRoomName()).isEqualTo(expected.type().getName());
            }
        }
    }

    @Test
    void checkIn_afterTheCheckOutDate_isRefusedAndSaidOnTheBookingAndTheAudit_untilTheDatesChange() {
        Room room = room("Ended");
        Booking ended = booking(room, "Never Came", TODAY.minusDays(3), TODAY.minusDays(1));
        String reason = "This stay ended on 9 Mar 2034. Change the dates first, then check in.";

        assertThat(bookingService.getById(ended.getId()).getCheckInClosedReason()).isEqualTo(reason);
        assertThat(row(nightAuditService.get(TODAY.minusDays(2).toString()).getMissedArrivals(), ended).getCheckInClosedReason()).isEqualTo(reason);
        assertThat(row(nightAuditService.get(null).getNotArrived(), ended).getCheckInClosedReason()).isEqualTo(reason);
        assertThatThrownBy(() -> occupancyService.checkIn(ended.getId())).isInstanceOf(ConflictException.class).hasMessage(reason);
        occupancyService.markNoShow(ended.getId());
        assertThatThrownBy(() -> occupancyService.checkIn(ended.getId())).isInstanceOf(ConflictException.class).hasMessage(reason);

        Booking extended = bookingService.updateSchedule(ended.getId(),
                new BookingScheduleInput(TODAY.minusDays(3).toString(), TODAY.plusDays(1).toString()).roomUnitId(room.unit().getId()));
        assertThat(extended.getCheckInClosedReason()).isNull();
        assertThat(occupancyService.checkIn(ended.getId()).getBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
    }

    @Test
    void checkIn_onTheCheckOutDateItself_isStillAllowed() {
        Room room = room("Last Day");
        Booking afterMidnight = booking(room, "After Midnight", TODAY.minusDays(1), TODAY);

        assertThat(bookingService.getById(afterMidnight.getId()).getCheckInClosedReason()).isNull();
        Booking checkedIn = occupancyService.checkIn(afterMidnight.getId()).getBooking();
        assertThat(checkedIn.getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
        assertThat(checkedIn.getCheckInClosedReason()).isNull();
    }
}
