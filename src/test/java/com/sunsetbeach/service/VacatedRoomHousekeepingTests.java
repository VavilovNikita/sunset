package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.support.FixedHotelClockConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * The daily job that marks dirty the room a guest in the house moved out of on the day a planned move
 * takes effect (a move for a later day marks nothing when it is planned). "Today" is pinned to
 * 2034-03-10 (FixedHotelClockConfig); the job runs once per hotel day and never re-dirties a room that
 * housekeeping cleaned afterwards.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
@Transactional
class VacatedRoomHousekeepingTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private VacatedRoomHousekeepingService job;
    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private JdbcTemplate jdbc;

    private RoomEntity room;
    private RoomUnitEntity first;
    private RoomUnitEntity second;

    @BeforeEach
    void setUp() {
        RoomEntity r = new RoomEntity();
        r.setName("Vacated Room " + UUID.randomUUID());
        r.setDescription("Used only by VacatedRoomHousekeepingTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(r);
        first = unit("VR1-");
        second = unit("VR2-");
        // Other tests of the suite may have run the job for this pinned day already (it is one row per day).
        jdbc.update("delete from \"HousekeepingSweep\" where \"day\" = ?", TODAY);
    }

    private RoomUnitEntity unit(String prefix) {
        RoomUnitEntity u = new RoomUnitEntity();
        u.setRoomId(room.getId());
        u.setLabel(prefix + UUID.randomUUID().toString().substring(0, 8));
        u.setActive(true);
        return roomUnitRepository.saveAndFlush(u);
    }

    private void setStatus(RoomUnitEntity u, HousekeepingStatus status) {
        RoomUnitEntity fresh = roomUnitRepository.findById(u.getId()).orElseThrow();
        fresh.setHousekeepingStatus(status);
        roomUnitRepository.saveAndFlush(fresh);
    }

    private HousekeepingStatus statusOf(RoomUnitEntity u) {
        return roomUnitRepository.findById(u.getId()).orElseThrow().getHousekeepingStatus();
    }

    /** In the house in {@code first}, moving to {@code second} on {@code moveOn}. */
    private Booking movingGuest(LocalDate moveOn) {
        Booking b = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.getId(), "Moving Guest", TODAY.minusDays(1).toString(), TODAY.plusDays(4).toString(), BookingChannel.WALK_IN, 1));
        bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(first.getId()));
        occupancyService.checkIn(b.getId());
        bookingService.relocate(b.getId(), new RelocationInput(moveOn.toString(), room.getId()).roomUnitId(second.getId()));
        return b;
    }

    @Test
    void theRoomAGuestMovedOutOfToday_isMarkedDirty_onceAHotelDay() {
        movingGuest(TODAY);
        setStatus(first, HousekeepingStatus.CLEAN); // the move itself marked it; pretend it is still clean at 00:05

        assertThat(job.runForToday()).isEqualTo(1);
        assertThat(statusOf(first)).isEqualTo(HousekeepingStatus.DIRTY);

        // Housekeeping cleans it; a repeat run the same day (hourly catch-up, second instance) leaves it clean.
        setStatus(first, HousekeepingStatus.CLEAN);
        assertThat(job.runForToday()).isZero();
        assertThat(statusOf(first)).isEqualTo(HousekeepingStatus.CLEAN);
    }

    @Test
    void aMoveForAnotherDay_markNothingToday() {
        movingGuest(TODAY.plusDays(2));
        setStatus(first, HousekeepingStatus.CLEAN);

        assertThat(job.runForToday()).isZero();
        assertThat(statusOf(first)).isEqualTo(HousekeepingStatus.CLEAN);
    }

    @Test
    void aGuestWhoIsNotInTheHouse_orARoomAlreadyDirty_isLeftAlone() {
        Booking b = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.getId(), "Not Arrived", TODAY.minusDays(1).toString(), TODAY.plusDays(4).toString(), BookingChannel.WALK_IN, 1));
        bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(first.getId()));
        bookingService.relocate(b.getId(), new RelocationInput(TODAY.toString(), room.getId()).roomUnitId(second.getId()));
        setStatus(first, HousekeepingStatus.CLEAN);

        assertThat(job.runForToday()).isZero();
        assertThat(statusOf(first)).as("the guest never checked in").isEqualTo(HousekeepingStatus.CLEAN);
    }
}
