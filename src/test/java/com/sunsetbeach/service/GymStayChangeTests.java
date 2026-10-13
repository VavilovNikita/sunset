package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.GymReservation;
import com.sunsetbeach.model.GymReservationCreateInput;
import com.sunsetbeach.model.GymReservationStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.GymReservationRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.support.MutableClock;
import com.sunsetbeach.tuya.TuyaClient;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a change of the hotel booking's dates does to its gym sessions, through the real
 * {@code BookingService#updateSchedule} (the one path that changes a stay's dates - the staff
 * schedule form, an early check-out and the SiteMinder import all go through it): sessions that
 * fall outside the new stay are cancelled, the ones still inside stay. Rolled back after each
 * test; the equipment side (a session in progress being switched off) is in
 * {@code GymAutomationServiceTests}.
 */
@SpringBootTest(properties = {"app.gym.automation.interval-ms=3600000"})
@Transactional
class GymStayChangeTests extends AbstractIntegrationTest {

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock("2031-06-15T09:00");
        }
    }

    @Autowired private BookingService bookingService;
    @Autowired private GymReservationService gym;
    @Autowired private GymReservationRepository reservations;
    @Autowired private RoomRepository rooms;
    @Autowired private RoomUnitRepository roomUnits;
    @Autowired private UserRepository users;
    @Autowired private MutableClock clock;
    @MockitoBean private TuyaClient tuya;

    private Booking booking;
    private RoomUnitEntity unit;
    private UserEntity staff;

    @BeforeEach
    void setUp() {
        clock.set("2031-06-15T09:00");
        RoomEntity room = new RoomEntity();
        room.setName("Gym Stay Room " + UUID.randomUUID());
        room.setDescription("Used only by GymStayChangeTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        room = rooms.saveAndFlush(room);
        unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Gym Stay Unit " + UUID.randomUUID());
        unit.setActive(true);
        unit = roomUnits.saveAndFlush(unit);
        booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), "Stay Guest", "2031-06-15", "2031-06-20", BookingChannel.WALK_IN, 1).roomUnitId(unit.getId()));
        UserEntity u = new UserEntity();
        u.setEmail("gym-stay-" + UUID.randomUUID() + "@test.local");
        u.setName(u.getEmail());
        u.setPasswordHash("unused");
        u.setRole(Role.CASHIER);
        u.setActive(true);
        staff = users.saveAndFlush(u);
    }

    private GymReservation reserve(String date, String start) {
        return gym.createForStaff(new GymReservationCreateInput(booking.getId(), date, start, 60), staff.getId());
    }

    private GymReservationStatus status(GymReservation r) {
        return reservations.findById(r.getId()).orElseThrow().getStatus();
    }

    private void shortenTo(String checkOut) {
        bookingService.updateSchedule(booking.getId(), new BookingScheduleInput("2031-06-15", checkOut).roomUnitId(unit.getId()));
    }

    @Test
    void shorteningTheStay_cancelsTheSessionsBeyondIt_andKeepsTheOthers() {
        GymReservation inside = reserve("2031-06-17", "10:00");
        GymReservation onDeparture = reserve("2031-06-18", "10:00");
        GymReservation outside = reserve("2031-06-19", "10:00");

        shortenTo("2031-06-18");

        assertThat(status(inside)).isEqualTo(GymReservationStatus.BOOKED);
        assertThat(status(onDeparture)).isEqualTo(GymReservationStatus.BOOKED); // the departure day is still part of the stay
        assertThat(status(outside)).isEqualTo(GymReservationStatus.CANCELLED);
        assertThat(reservations.findById(outside.getId()).orElseThrow().getCancelReason()).isEqualTo("Stay dates changed");
    }

    @Test
    void extendingOrKeepingTheStay_cancelsNothing() {
        GymReservation late = reserve("2031-06-20", "10:00");

        bookingService.updateSchedule(booking.getId(), new BookingScheduleInput("2031-06-15", "2031-06-22").roomUnitId(unit.getId()));

        assertThat(status(late)).isEqualTo(GymReservationStatus.BOOKED);
    }
}
