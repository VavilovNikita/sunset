package com.sunsetbeach.service;

import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * What a new booking may look like: nothing in the past from the public form (back-entry by staff
 * is a manager's call), at most 90 nights, and a party that fits the room - refused on the public
 * form, a warning at the front desk (an extra bed is a real thing). "Today" is a fixed Clock.
 */
@SpringBootTest
class BookingCreationLimitsTests extends AbstractIntegrationTest {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(LocalDateTime.parse("2031-06-15T10:00:00").atZone(HOTEL_ZONE).toInstant(), HOTEL_ZONE);
        }
    }

    @Autowired private BookingService bookingService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;

    private final List<String> roomIds = new ArrayList<>();
    private final List<String> bookingIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        bookingIds.forEach(bookingRepository::deleteById);
        for (String roomId : roomIds) {
            roomUnitRepository.findByRoomId(roomId).forEach(u -> roomUnitRepository.deleteById(u.getId()));
            roomRepository.deleteById(roomId);
        }
    }

    private RoomEntity room(int capacity) {
        RoomEntity room = new RoomEntity();
        room.setName("Limits Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingCreationLimitsTests");
        room.setCapacity(capacity);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        roomIds.add(saved.getId());
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(saved.getId());
        unit.setLabel("L" + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);
        return saved;
    }

    private BookingCreateInput publicInput(RoomEntity room, String in, String out, int adults) {
        return new BookingCreateInput(room.getId(), "Web Guest", "web@example.com", "+66800000000", in, out, adults);
    }

    private StaffBookingCreateInput staffInput(RoomEntity room, String in, String out, int adults) {
        return new StaffBookingCreateInput(room.getId(), "Desk Guest", in, out, BookingChannel.WALK_IN, adults);
    }

    @Test
    void public_checkInInThePast_isRefused() {
        RoomEntity room = room(2);

        assertThatThrownBy(() -> bookingService.createBooking(publicInput(room, "2031-06-14", "2031-06-16", 1))).isInstanceOf(ValidationException.class);
        assertThat(bookingRepository.findAll().stream().filter(b -> room.getId().equals(b.getRoomId()))).isEmpty();
    }

    @Test
    void public_checkInToday_isFine() {
        RoomEntity room = room(2);

        Booking booking = bookingService.createBooking(publicInput(room, "2031-06-15", "2031-06-16", 1));
        bookingIds.add(booking.getId());

        assertThat(booking.getCheckIn()).isEqualTo("2031-06-15");
    }

    @Test
    void public_moreThan90Nights_isRefused_andExactly90IsFine() {
        RoomEntity room = room(2);

        assertThatThrownBy(() -> bookingService.createBooking(publicInput(room, "2031-07-01", "2031-10-01", 1))).isInstanceOf(ValidationException.class); // 92 nights
        Booking ok = bookingService.createBooking(publicInput(room, "2031-07-01", "2031-09-29", 1)); // 90 nights
        bookingIds.add(ok.getId());
    }

    @Test
    void public_partyLargerThanTheRoomSleeps_isRefused() {
        RoomEntity room = room(2);

        assertThatThrownBy(() -> bookingService.createBooking(publicInput(room, "2031-07-01", "2031-07-02", 3))).isInstanceOf(ValidationException.class);
        BookingCreateInput withChildren = publicInput(room, "2031-07-01", "2031-07-02", 2).children(1);
        assertThatThrownBy(() -> bookingService.createBooking(withChildren)).isInstanceOf(ValidationException.class);
    }

    @Test
    void staff_partyLargerThanTheRoomSleeps_isAccepted_withAWarning() {
        RoomEntity room = room(2);

        Booking booking = bookingService.createStaffBooking(staffInput(room, "2031-07-01", "2031-07-02", 3));
        bookingIds.add(booking.getId());

        assertThat(booking.getWarning()).contains("3 guests").contains("sleeps 2");
    }

    @Test
    void staff_partyThatFits_hasNoWarning() {
        RoomEntity room = room(2);

        Booking booking = bookingService.createStaffBooking(staffInput(room, "2031-07-01", "2031-07-02", 2));
        bookingIds.add(booking.getId());

        assertThat(booking.getWarning()).isNull();
    }

    @Test
    void staff_moreThan90Nights_isRefused() {
        RoomEntity room = room(2);

        assertThatThrownBy(() -> bookingService.createStaffBooking(staffInput(room, "2031-07-01", "2034-03-27", 1))).isInstanceOf(ValidationException.class);
    }

    @Test
    void staff_backEntryNeedsAManager() {
        RoomEntity room = room(2);

        assertThatThrownBy(() -> bookingService.createStaffBooking(staffInput(room, "2031-06-10", "2031-06-12", 1), false)).isInstanceOf(ForbiddenException.class);
        Booking byManager = bookingService.createStaffBooking(staffInput(room, "2031-06-10", "2031-06-12", 1), true);
        bookingIds.add(byManager.getId());
        Booking todayByCashier = bookingService.createStaffBooking(staffInput(room, "2031-06-15", "2031-06-16", 1), false);
        bookingIds.add(todayByCashier.getId());
    }
}
