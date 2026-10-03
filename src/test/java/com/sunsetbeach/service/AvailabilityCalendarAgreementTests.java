package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.AvailabilityDay;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.RoomTypeDailyAvailability;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * The admin "Rates & availability" grid reads remaining rooms from {@code GET /availability/{roomId}};
 * the booking calendar reads them from {@code GET /bookings/calendar}'s {@code dailyAvailable}. Staff
 * see both screens side by side, so the two must give the same number for every room type and day.
 * Both go through {@link InventoryMath}, but each service gathers its own blocks, segments and
 * overstays - this pins that gathering together across every input that moves the count: a block,
 * an assigned stay, an unassigned stay, an overstay, a cancelled stay, and a stay on a deactivated
 * unit (which drives the count negative). "Today" is 2033-03-20 (Bangkok).
 */
@SpringBootTest
@Transactional
class AvailabilityCalendarAgreementTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final Instant NOW = LocalDateTime.parse("2033-03-20T10:00").atZone(BANGKOK).toInstant();

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, BANGKOK);
        }
    }

    @Autowired private AvailabilityService availabilityService;
    @Autowired private BookingCalendarService calendarService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private RoomUnitBlockRepository blockRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private EntityManager entityManager;

    private RoomEntity room;
    private RoomUnitEntity unitA;
    private RoomUnitEntity unitB;
    private RoomUnitEntity unitC;
    private RoomUnitEntity retiredUnit;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Agreement Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by AvailabilityCalendarAgreementTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);

        unitA = unit(true);
        unitB = unit(true);
        unitC = unit(true);
        retiredUnit = unit(false);
    }

    @Test
    void remainingRooms_matchBetweenAvailabilityAndCalendar_onEveryDayOfTheMonth() {
        block(unitA, "2033-03-05", "2033-03-07");
        stay(unitB, "2033-03-06", "2033-03-09", BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED);
        stay(null, "2033-03-08", "2033-03-12", BookingStatus.NEW, OccupancyStatus.EXPECTED);
        stay(unitC, "2033-03-12", "2033-03-15", BookingStatus.CONFIRMED, OccupancyStatus.CHECKED_IN); // due out the 15th, still in tonight
        stay(unitA, "2033-03-01", "2033-03-31", BookingStatus.CANCELLED, OccupancyStatus.EXPECTED);
        stay(retiredUnit, "2033-03-25", "2033-03-28", BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED);
        stay(null, "2033-03-25", "2033-03-28", BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED);
        stay(null, "2033-03-25", "2033-03-28", BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED);
        stay(null, "2033-03-25", "2033-03-28", BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED);

        Map<String, Integer> fromAvailability = new LinkedHashMap<>();
        for (AvailabilityDay d : availabilityService.getAvailability(room.getId(), "2033-03").getDays()) {
            fromAvailability.put(d.getDate(), d.getAvailableCount());
        }
        Map<String, Integer> fromCalendar = new LinkedHashMap<>();
        for (RoomTypeDailyAvailability d : calendarService.getCalendar(LocalDate.parse("2033-03-01"), LocalDate.parse("2033-04-01"))
                .getRoomTypes().stream().filter(t -> t.getRoomId().equals(room.getId())).findFirst().orElseThrow().getDailyAvailable()) {
            fromCalendar.put(d.getDate(), d.getAvailableCount());
        }

        assertThat(fromCalendar).hasSize(31).isEqualTo(fromAvailability);
        // Spot-check the fixture actually exercised what it claims to, so a broken setup can't
        // make the two agree trivially (e.g. both all-3).
        assertThat(fromAvailability.get("2033-03-06")).isEqualTo(1); // block + assigned stay
        assertThat(fromAvailability.get("2033-03-08")).isEqualTo(1); // assigned + unassigned
        assertThat(fromAvailability.get("2033-03-20")).isEqualTo(2); // tonight's overstay
        assertThat(fromAvailability.get("2033-03-21")).isEqualTo(3); // overstay doesn't promise tomorrow
        assertThat(fromAvailability.get("2033-03-25")).isEqualTo(-1); // oversold, never clamped
    }

    private RoomUnitEntity unit(boolean active) {
        RoomUnitEntity u = new RoomUnitEntity();
        u.setRoomId(room.getId());
        u.setLabel("AG-" + UUID.randomUUID().toString().substring(0, 8));
        u.setActive(active);
        return roomUnitRepository.saveAndFlush(u);
    }

    private void block(RoomUnitEntity unit, String from, String to) {
        RoomUnitBlockEntity b = new RoomUnitBlockEntity();
        b.setRoomUnitId(unit.getId());
        b.setFromDate(LocalDate.parse(from));
        b.setToDate(LocalDate.parse(to));
        b.setReason("Agreement test");
        blockRepository.saveAndFlush(b);
    }

    private void stay(RoomUnitEntity unit, String checkIn, String checkOut, BookingStatus status, OccupancyStatus occupancy) {
        String unitId = unit != null ? unit.getId() : null;
        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setRoomId(room.getId());
        booking.setRoomUnitId(unitId);
        booking.setGuestName("Agreement Guest");
        booking.setGuestEmail("agreement@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.parse(checkIn));
        booking.setCheckOut(LocalDate.parse(checkOut));
        booking.setTotalPrice(new BigDecimal("3000.00"));
        booking.setStatus(status);
        booking.setOccupancyStatus(occupancy);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(saved.getId());
        segment.setRoomId(room.getId());
        segment.setRoomUnitId(unitId);
        segment.setCheckIn(LocalDate.parse(checkIn));
        segment.setCheckOut(LocalDate.parse(checkOut));
        segment.setTotalPrice(new BigDecimal("3000.00"));
        segmentRepository.saveAndFlush(segment);
        entityManager.clear();
    }
}
