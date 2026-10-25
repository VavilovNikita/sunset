package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPage;
import com.sunsetbeach.model.BookingSortField;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.SortDirection;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * With no sort asked for, the admin Bookings list starts at the nearest arrival from today, not at the
 * furthest future date. "Today" is pinned to 2034-06-10 (Bangkok); the guests' names carry a unique tag so
 * the property-wide list can be narrowed to them with {@code q}.
 */
@SpringBootTest
@Transactional
class BookingSearchDefaultOrderTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final Instant NOW = LocalDateTime.parse("2034-06-10T10:00").atZone(BANGKOK).toInstant();

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, BANGKOK);
        }
    }

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void noSortGiven_nearestArrivalsFirst_thenThePastLatestFirst() {
        String tag = "ord" + UUID.randomUUID().toString().substring(0, 8);
        RoomEntity room = new RoomEntity();
        room.setName("Order Room " + tag);
        room.setDescription("x");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(room);
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("O-" + tag);
        unit = roomUnitRepository.saveAndFlush(unit);

        for (String checkIn : List.of("2034-09-01", "2034-06-12", "2034-06-10", "2034-05-01", "2034-06-01", "2034-07-01")) {
            persist(room, unit, tag, checkIn, null, OccupancyStatus.EXPECTED);
        }
        // In the room now: one due out on the 12th, one who should have left on the 8th and stayed on.
        persist(room, unit, tag, "2034-06-05", "2034-06-12", OccupancyStatus.CHECKED_IN);
        persist(room, unit, tag, "2034-06-01", "2034-06-08", OccupancyStatus.CHECKED_IN);
        entityManager.clear();

        BookingPage page = bookingService.search(tag, null, null, null, null, null, 0, 50);
        // In the room now (soonest departure first, the overstaying guest on top), then arrivals to come, then the past.
        assertThat(page.getItems())
                .extracting(b -> b.getCheckIn() + (b.getOccupancyStatus() == OccupancyStatus.CHECKED_IN ? "*" : ""))
                .containsExactly("2034-06-01*", "2034-06-05*", "2034-06-10", "2034-06-12", "2034-07-01", "2034-09-01", "2034-06-01", "2034-05-01");
        assertThat(page.getTotalCount()).isEqualTo(8);

        // An explicit sort is honoured exactly as before.
        BookingPage explicit = bookingService.search(tag, null, null, null, BookingSortField.CHECK_IN, SortDirection.DESC, 0, 50);
        assertThat(explicit.getItems().get(0).getCheckIn()).isEqualTo("2034-09-01");
    }

    private void persist(RoomEntity room, RoomUnitEntity unit, String tag, String checkIn, String checkOut, OccupancyStatus occupancy) {
        LocalDate in = LocalDate.parse(checkIn);
        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setRoomId(room.getId());
        booking.setRoomUnitId(unit.getId());
        booking.setGuestName("Guest " + tag);
        booking.setGuestEmail("g@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(in);
        booking.setCheckOut(checkOut != null ? LocalDate.parse(checkOut) : in.plusDays(1));
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setOccupancyStatus(occupancy);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(saved.getId());
        segment.setRoomId(room.getId());
        segment.setRoomUnitId(unit.getId());
        segment.setCheckIn(in);
        segment.setCheckOut(checkOut != null ? LocalDate.parse(checkOut) : in.plusDays(1));
        segment.setTotalPrice(new BigDecimal("1000.00"));
        segmentRepository.saveAndFlush(segment);
    }
}
