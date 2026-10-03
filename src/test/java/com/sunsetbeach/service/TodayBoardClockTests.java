package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.TodayBoard;
import com.sunsetbeach.model.TodayBoardEntry;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
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
 * The front desk's today board ({@link BookingOccupancyService#getTodayBoard}) against the shared
 * {@link Clock}, pinned the way {@link BookingOccupancyClockTests} pins it. The instant is 02:00
 * Bangkok on 2033-11-10 - still 2033-11-09 in UTC (19:00Z), inside the 00:00-06:59 window where the
 * bare {@code LocalDate.now()} this replaced, on the UTC production server, read yesterday's date
 * and showed yesterday's arrivals and departures.
 */
@SpringBootTest
@Transactional
class TodayBoardClockTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final LocalDate TODAY = LocalDate.parse("2033-11-10");
    private static final Instant NOW = TODAY.atTime(2, 0).atZone(BANGKOK).toInstant();

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, BANGKOK);
        }
    }

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;

    /** The regression itself: at 02:00 Bangkok the board is the hotel's today, not UTC's yesterday. */
    @Test
    void todayBoard_betweenMidnightAndSevenBangkok_showsTodaysArrivalsAndDepartures_notYesterdays() {
        LocalDate yesterday = TODAY.minusDays(1);
        assertThat(LocalDateTime.ofInstant(NOW, ZoneId.of("UTC")).toLocalDate()).isEqualTo(yesterday); // the window the bug lived in

        RoomEntity room = createRoom();
        Booking arrivingToday = assignedBooking(room, TODAY, TODAY.plusDays(2));
        Booking arrivedYesterday = assignedBooking(room, yesterday, yesterday.plusDays(3)); // still EXPECTED - a late arrival, not today's
        Booking departingToday = assignedBooking(room, TODAY.minusDays(3), TODAY);
        occupancyService.checkIn(departingToday.getId());
        Booking departingYesterday = assignedBooking(room, TODAY.minusDays(4), yesterday); // an overstay still checked in
        occupancyService.checkIn(departingYesterday.getId());

        TodayBoard board = occupancyService.getTodayBoard();

        assertThat(ids(board.getArrivingToday())).contains(arrivingToday.getId()).doesNotContain(arrivedYesterday.getId());
        // The overstay is still due out - listed as departing, but as 1 day overdue (OverstayRule),
        // which is only right if "today" is the hotel's date: on UTC's yesterday it would read 0.
        assertThat(ids(board.getDepartingToday())).contains(departingToday.getId(), departingYesterday.getId());
        assertThat(overdueDays(board.getDepartingToday(), departingToday)).isZero();
        assertThat(overdueDays(board.getDepartingToday(), departingYesterday)).isEqualTo(1);
    }

    private static int overdueDays(java.util.List<com.sunsetbeach.model.TodayBoardEntry> entries, Booking booking) {
        return entries.stream().filter(e -> e.getBooking().getId().equals(booking.getId())).findFirst().orElseThrow().getOverdueDays();
    }

    // --- Moved from BookingOccupancyTests, where they read the wall clock ---------------------------

    @Test
    void todayBoard_groupsArrivingDepartingAndInHouseCorrectly() {
        RoomEntity room = createRoom();

        // Arriving today: EXPECTED, checkIn = today.
        Booking arriving = assignedBooking(room, TODAY, TODAY.plusDays(3));

        // Departing today: CHECKED_IN, checkOut = today.
        Booking departing = assignedBooking(room, TODAY.minusDays(2), TODAY);
        occupancyService.checkIn(departing.getId());

        // In-house: CHECKED_IN, but not departing today.
        Booking inHouse = assignedBooking(room, TODAY.minusDays(1), TODAY.plusDays(4));
        occupancyService.checkIn(inHouse.getId());

        TodayBoard board = occupancyService.getTodayBoard();

        assertThat(ids(board.getArrivingToday())).contains(arriving.getId()).doesNotContain(departing.getId(), inHouse.getId());
        assertThat(ids(board.getDepartingToday())).contains(departing.getId()).doesNotContain(arriving.getId(), inHouse.getId());
        assertThat(ids(board.getInHouse())).contains(departing.getId(), inHouse.getId()).doesNotContain(arriving.getId());
    }

    @Test
    void todayBoard_excludesANoShowBooking_fromAllThreeLists() {
        RoomEntity room = createRoom();
        Booking booking = assignedBooking(room, TODAY, TODAY.plusDays(2));
        occupancyService.markNoShow(booking.getId());

        TodayBoard board = occupancyService.getTodayBoard();

        assertThat(ids(board.getArrivingToday())).doesNotContain(booking.getId());
        assertThat(ids(board.getDepartingToday())).doesNotContain(booking.getId());
        assertThat(ids(board.getInHouse())).doesNotContain(booking.getId());
    }

    private static List<String> ids(List<TodayBoardEntry> entries) {
        return entries.stream().map(e -> e.getBooking().getId()).toList();
    }

    private RoomEntity createRoom() {
        RoomEntity room = new RoomEntity();
        room.setName("Today Board Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by TodayBoardClockTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        return roomRepository.saveAndFlush(room);
    }

    /** A booking through the ordinary create path, on its own freshly created unit. */
    private Booking assignedBooking(RoomEntity room, LocalDate checkIn, LocalDate checkOut) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Today Board Unit " + UUID.randomUUID());
        unit.setActive(true);
        unit = roomUnitRepository.saveAndFlush(unit);

        Booking booking = bookingService.createBooking(
                new BookingCreateInput(room.getId(), "Guest", "guest@example.com", "+66800000000", checkIn.toString(), checkOut.toString(), 1));
        return bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
    }
}
