package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.AvailabilityDay;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCalendarResponse;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPurpose;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.CalendarBooking;
import com.sunsetbeach.model.InHouseRow;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.RoomTypeDailyAvailability;
import com.sunsetbeach.model.TodayBoardEntry;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * A guest still {@code CHECKED_IN} after {@code checkOut} is in the house and holds their room
 * through tonight on every screen - the case that used to be visible on the Today board, invisible
 * to availability/the calendar/the in-house list/{@code GET /bookings}, and so sellable twice.
 * "Today" is pinned to 2033-03-20 (Bangkok); the overdue fixture was due out on 2033-03-15.
 * Each test has its own room type with one unit, so the property-wide overstay list never mixes in
 * anyone else's guest.
 */
@SpringBootTest
@Transactional
class OverstayRuleTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final Instant NOW = LocalDateTime.parse("2033-03-20T10:00").atZone(BANGKOK).toInstant();
    private static final LocalDate TODAY = LocalDate.parse("2033-03-20");

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
    @Autowired private BookingWriter bookingWriter;
    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private ReportService reportService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private EntityManager entityManager;

    private RoomEntity room;
    private RoomUnitEntity unit;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Overstay Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by OverstayRuleTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);

        RoomUnitEntity newUnit = new RoomUnitEntity();
        newUnit.setRoomId(room.getId());
        newUnit.setLabel("OS-" + UUID.randomUUID().toString().substring(0, 8));
        unit = roomUnitRepository.saveAndFlush(newUnit);
    }

    @Test
    void overdueDays_countsFromTheDayAfterCheckOut_onlyWhileCheckedIn() {
        assertThat(OverstayRule.overdueDays(stay(OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-15"), TODAY)).isEqualTo(5);
        // Due out today is not overdue yet - the night is still sellable to the next arrival.
        assertThat(OverstayRule.overdueDays(stay(OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-20"), TODAY)).isZero();
        assertThat(OverstayRule.overdueDays(stay(OccupancyStatus.CHECKED_OUT, "2033-03-10", "2033-03-15"), TODAY)).isZero();
        assertThat(OverstayRule.overdueDays(stay(OccupancyStatus.EXPECTED, "2033-03-10", "2033-03-15"), TODAY)).isZero();
    }

    @Test
    void availability_countsTheRoomTakenFromCheckOutThroughTonight() {
        BookingEntity overdue = persistStay(OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-15");

        List<AvailabilityDay> days = availabilityService.getAvailability(room.getId(), "2033-03").getDays();

        assertThat(day(days, "2033-03-15").getAvailableCount()).isZero();
        AvailabilityDay tonight = day(days, "2033-03-20");
        assertThat(tonight.getAvailableCount()).isZero();
        assertThat(tonight.getUnits().get(0).getBookingId().get()).isEqualTo(overdue.getId());
        // Tomorrow is not promised to anyone - the guest may well leave today.
        assertThat(day(days, "2033-03-21").getAvailableCount()).isEqualTo(1);
    }

    @Test
    void checkedOutGuest_releasesTheRoom() {
        persistStay(OccupancyStatus.CHECKED_OUT, "2033-03-10", "2033-03-15");

        List<AvailabilityDay> days = availabilityService.getAvailability(room.getId(), "2033-03").getDays();

        assertThat(day(days, "2033-03-20").getAvailableCount()).isEqualTo(1);
    }

    @Test
    void calendar_drawsTheLastSegmentWithItsOverstay_evenWhenTheAgreedStayIsOffScreen() {
        BookingEntity overdue = persistStay(OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-15");

        BookingCalendarResponse calendar = calendarService.getCalendar(LocalDate.parse("2033-03-18"), LocalDate.parse("2033-03-25"));

        CalendarBooking bar = calendar.getBookings().stream().filter(b -> b.getBookingId().equals(overdue.getId())).findFirst().orElseThrow();
        assertThat(bar.getCheckOut()).isEqualTo("2033-03-15"); // the agreed date is never rewritten
        assertThat(bar.getOverstayUntil()).isEqualTo("2033-03-21");
        List<RoomTypeDailyAvailability> daily = calendar.getRoomTypes().stream()
                .filter(t -> t.getRoomId().equals(room.getId())).findFirst().orElseThrow().getDailyAvailable();
        assertThat(daily.stream().filter(d -> d.getDate().equals("2033-03-20")).findFirst().orElseThrow().getAvailableCount()).isZero();
        assertThat(daily.stream().filter(d -> d.getDate().equals("2033-03-21")).findFirst().orElseThrow().getAvailableCount()).isEqualTo(1);
    }

    @Test
    void newBooking_cannotTakeTheRoomTonight_butCanFromTomorrow() {
        persistStay(OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-15");
        room = roomRepository.findById(room.getId()).orElseThrow();

        assertThatThrownBy(() -> insertStaff("2033-03-20", "2033-03-22", null)).isInstanceOf(ConflictException.class);
        assertThat(insertStaff("2033-03-21", "2033-03-23", unit.getId()).getRoomUnitId()).isEqualTo(unit.getId());
    }

    @Test
    void assigningTheOverstayedUnit_isRefusedWithTheGuestNamed() {
        room = roomRepository.findById(room.getId()).orElseThrow();
        // A second unit so type-level availability passes and only the unit check can refuse.
        RoomUnitEntity other = new RoomUnitEntity();
        other.setRoomId(room.getId());
        other.setLabel("OS2-" + UUID.randomUUID().toString().substring(0, 8));
        roomUnitRepository.saveAndFlush(other);
        persistStay(OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-15");

        assertThatThrownBy(() -> insertStaff("2033-03-19", "2033-03-21", unit.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Overstay Guest")
                .hasMessageContaining("2033-03-15");
    }

    @Test
    void inHouseList_includesTheOverdueGuestOnEveryNightUpToTonight() {
        BookingEntity overdue = persistStay(OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-15");

        InHouseRow tonight = row(reportService.inHouse("2033-03-20").getRooms(), overdue);
        assertThat(tonight).isNotNull();
        assertThat(tonight.getOverdueDays()).isEqualTo(5);
        assertThat(row(reportService.inHouse("2033-03-16").getRooms(), overdue)).isNotNull();
        assertThat(row(reportService.inHouse("2033-03-21").getRooms(), overdue)).isNull();
    }

    @Test
    void todayBoard_listsTheOverdueGuestAsDepartingWithTheirDaysOverdue() {
        BookingEntity overdue = persistStay(OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-15");

        TodayBoardEntry entry = occupancyService.getTodayBoard().getDepartingToday().stream()
                .filter(e -> e.getBooking().getId().equals(overdue.getId())).findFirst().orElseThrow();

        assertThat(entry.getOverdueDays()).isEqualTo(5);
    }

    @Test
    void bookingList_forToday_findsTheOverdueGuest_soPosAndSpaCanPickThem() {
        BookingEntity overdue = persistStay(OccupancyStatus.CHECKED_IN, "2033-03-10", "2033-03-15");
        BookingEntity departedOnTime = persistStay(OccupancyStatus.CHECKED_OUT, "2033-03-10", "2033-03-15");

        List<String> today = bookingService.list("2033-03-20", "2033-03-20", null, null).stream().map(Booking::getId).toList();
        List<String> spaWindow = bookingService.list("2033-03-19", "2033-03-20", null, null).stream().map(Booking::getId).toList();
        List<String> nextWeek = bookingService.list("2033-03-27", "2033-03-27", null, null).stream().map(Booking::getId).toList();

        assertThat(today).contains(overdue.getId()).doesNotContain(departedOnTime.getId());
        assertThat(spaWindow).contains(overdue.getId());
        assertThat(nextWeek).doesNotContain(overdue.getId());
    }

    private BookingEntity insertStaff(String checkIn, String checkOut, String roomUnitId) {
        return bookingWriter.insertStaff(room, "New Guest", "new@example.com", "+66800000001", LocalDate.parse(checkIn), LocalDate.parse(checkOut),
                BookingChannel.DIRECT, BookingPurpose.STANDARD, 2, 0, roomUnitId);
    }

    private static AvailabilityDay day(List<AvailabilityDay> days, String date) {
        return days.stream().filter(d -> d.getDate().equals(date)).findFirst().orElseThrow();
    }

    private static InHouseRow row(List<InHouseRow> rows, BookingEntity booking) {
        return rows.stream().filter(r -> r.getBookingId().equals(booking.getId())).findFirst().orElse(null);
    }

    private static BookingEntity stay(OccupancyStatus occupancy, String checkIn, String checkOut) {
        BookingEntity booking = new BookingEntity();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setOccupancyStatus(occupancy);
        booking.setCheckIn(LocalDate.parse(checkIn));
        booking.setCheckOut(LocalDate.parse(checkOut));
        return booking;
    }

    private BookingEntity persistStay(OccupancyStatus occupancy, String checkIn, String checkOut) {
        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setRoomId(room.getId());
        booking.setRoomUnitId(unit.getId());
        booking.setGuestName("Overstay Guest");
        booking.setGuestEmail("overstay@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.parse(checkIn));
        booking.setCheckOut(LocalDate.parse(checkOut));
        booking.setTotalPrice(new BigDecimal("5000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setOccupancyStatus(occupancy);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(saved.getId());
        segment.setRoomId(room.getId());
        segment.setRoomUnitId(unit.getId());
        segment.setCheckIn(LocalDate.parse(checkIn));
        segment.setCheckOut(LocalDate.parse(checkOut));
        segment.setTotalPrice(new BigDecimal("5000.00"));
        segmentRepository.saveAndFlush(segment);
        // Read back the way production does - fresh rows with their relations, not the
        // instances just saved (which have only the foreign-key ids set).
        entityManager.clear();
        return saved;
    }
}
