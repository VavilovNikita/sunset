package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AvailabilityDay;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.RoomUnitAvailability;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * The staff availability breakdown names the guest on a room that is booked that day, so a screen
 * can show who is in it instead of the reason of a block laid over the booking. A room nobody holds
 * carries no guest name. Dates are fixed (2034), never the wall clock.
 */
@SpringBootTest
@Transactional
class AvailabilityGuestNameTests extends AbstractIntegrationTest {

    private static final LocalDate DAY = LocalDate.of(2034, 5, 10);

    @Autowired private AvailabilityService availabilityService;
    @Autowired private BookingService bookingService;
    @Autowired private RoomUnitService roomUnitService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void aBookedRoom_namesItsGuest_evenWithABlockOverIt_andAFreeRoomNamesNobody() {
        RoomEntity room = new RoomEntity();
        room.setName("Guest Name Room " + UUID.randomUUID());
        room.setDescription("Used only by AvailabilityGuestNameTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(room);
        RoomUnitEntity booked = unit(room, "GN-A-");
        RoomUnitEntity free = unit(room, "GN-B-");
        UserEntity manager = new UserEntity();
        manager.setEmail("avail-name-" + UUID.randomUUID() + "@example.com");
        manager.setName(manager.getEmail());
        manager.setPasswordHash("unused");
        manager.setRole(Role.MANAGER);
        manager = userRepository.saveAndFlush(manager);

        Booking booking = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.getId(), "Anna Guest", DAY.minusDays(1).toString(), DAY.plusDays(2).toString(), BookingChannel.WALK_IN, 1));
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(booked.getId()));
        roomUnitService.createBlock(booked.getId(), new RoomUnitBlockInput(DAY.toString(), DAY.toString(), "Broken air conditioner"), manager.getId());

        AvailabilityDay day = availabilityService.getAvailability(room.getId(), YearMonth.from(DAY).toString()).getDays().stream()
                .filter(d -> d.getDate().equals(DAY.toString()))
                .findFirst()
                .orElseThrow();

        RoomUnitAvailability bookedUnit = day.getUnits().stream().filter(u -> u.getRoomUnitId().equals(booked.getId())).findFirst().orElseThrow();
        assertThat(bookedUnit.getIsBooked()).isTrue();
        assertThat(bookedUnit.getIsBlocked()).isTrue();
        assertThat(bookedUnit.getGuestName().orElse(null)).isEqualTo("Anna Guest");
        assertThat(bookedUnit.getBlockReason().orElse(null)).isEqualTo("Broken air conditioner");

        RoomUnitAvailability freeUnit = day.getUnits().stream().filter(u -> u.getRoomUnitId().equals(free.getId())).findFirst().orElseThrow();
        assertThat(freeUnit.getIsBooked()).isFalse();
        assertThat(freeUnit.getGuestName().orElse(null)).isNull();
    }

    private RoomUnitEntity unit(RoomEntity room, String prefix) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel(prefix + UUID.randomUUID().toString().substring(0, 8));
        unit.setActive(true);
        return roomUnitRepository.saveAndFlush(unit);
    }
}
