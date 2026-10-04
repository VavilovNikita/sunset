package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.InHouseReport;
import com.sunsetbeach.model.InHouseRow;
import com.sunsetbeach.model.ManagerReportDay;
import com.sunsetbeach.model.OccupancyStatus;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
 * {@code checkedInAt}/{@code checkedOutAt} come from the shared {@link Clock}, stored as UTC
 * wall-clock - exactly what the bare {@code LocalDateTime.now()} they replaced stored on a
 * UTC server. The clock is pinned to the production zone, Asia/Bangkok, at 20:00 Bangkok on
 * 2033-11-10 (13:00Z): the one hour of the day where storing the hotel's wall-clock instead would
 * cross the report's night boundary (17:00Z) and put a guest who left that evening in-house.
 */
@SpringBootTest
@Transactional
class BookingOccupancyClockTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final Instant NOW = LocalDateTime.parse("2033-11-10T20:00").atZone(BANGKOK).toInstant();

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, BANGKOK);
        }
    }

    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private ReportService reportService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private EntityManager entityManager;

    private RoomEntity room;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Clock Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by BookingOccupancyClockTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);
    }

    @Test
    void checkInAndOut_storeTheClockInstantAsUtcWallClock() {
        BookingEntity booking = persistStay("2033-11-09", "2033-11-11");
        LocalDateTime expected = LocalDateTime.parse("2033-11-10T13:00");

        occupancyService.checkIn(booking.getId());
        Booking checkedOut = occupancyService.checkOut(booking.getId(), null).getBooking();

        entityManager.clear();
        BookingEntity stored = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(stored.getCheckedInAt()).isEqualTo(expected);
        assertThat(stored.getCheckedOutAt()).isEqualTo(expected);
        // What LocalDateTime.now() produced before the fix, on a UTC server.
        assertThat(stored.getCheckedOutAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        // And the API still reports the real instant.
        assertThat(checkedOut.getCheckedOutAt().get()).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    /**
     * Left at 20:00 Bangkok on the 10th, so not in-house that night; a guest still checked in is.
     * The in-house list and the manager report's guest statistic have to agree on both.
     */
    @Test
    void checkOutThroughTheService_inHouseAndManagerReportAgree() {
        BookingEntity left = persistStay("2033-11-09", "2033-11-12");
        BookingEntity stayed = persistStay("2033-11-09", "2033-11-12");
        occupancyService.checkIn(left.getId());
        occupancyService.checkIn(stayed.getId());
        occupancyService.checkOut(left.getId(), null);
        entityManager.clear();

        InHouseReport inHouse = reportService.inHouse("2033-11-10");
        ManagerReportDay manager = reportService.manager("2033-11-10").getToday();

        assertThat(inHouse.getRooms()).extracting(InHouseRow::getBookingId).containsExactly(stayed.getId());
        assertThat(manager.getGuests().getAdultsInHouse()).isEqualTo(inHouse.getTotal().getAdults()).isEqualTo(1);
        assertThat(manager.getGuests().getChildrenInHouse()).isEqualTo(inHouse.getTotal().getChildren()).isZero();
        assertThat(manager.getGuests().getGuestsInHouse()).isEqualTo(1);
    }

    /** A checked-in stay on its own room unit, one segment covering the whole stay. */
    private BookingEntity persistStay(String checkIn, String checkOut) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Clock-" + UUID.randomUUID().toString().substring(0, 8));
        unit.setActive(true);
        unit = roomUnitRepository.saveAndFlush(unit);

        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setRoomUnitId(unit.getId());
        booking.setChannel(BookingChannel.DIRECT);
        booking.setAdults(1);
        booking.setChildren(0);
        booking.setGuestName("Clock Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.parse(checkIn));
        booking.setCheckOut(LocalDate.parse(checkOut));
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setOccupancyStatus(OccupancyStatus.EXPECTED);
        booking = bookingRepository.saveAndFlush(booking);

        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(booking.getId());
        segment.setRoomId(room.getId());
        segment.setRoomUnitId(unit.getId());
        segment.setCheckIn(LocalDate.parse(checkIn));
        segment.setCheckOut(LocalDate.parse(checkOut));
        segment.setTotalPrice(new BigDecimal("1000.00"));
        segmentRepository.saveAndFlush(segment);
        return booking;
    }
}
