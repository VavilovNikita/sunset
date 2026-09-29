package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.ForecastDay;
import com.sunsetbeach.model.ForecastReport;
import com.sunsetbeach.model.ForecastRow;
import com.sunsetbeach.model.ManagerReportDay;
import com.sunsetbeach.model.OccupancyReport;
import com.sunsetbeach.model.OccupancyReportRow;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /reports/forecast} (scoped Z440) against hand-built fixtures in 2033, clear of the
 * baseline dump - and in the future, the range this report is meant for. Every test passes
 * explicit dates; the forecast never reads the clock.
 */
@SpringBootTest
@Transactional
class ReportForecastTests extends AbstractIntegrationTest {

    @Autowired private ReportService reportService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private RoomUnitBlockRepository blockRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private UserRepository userRepository;

    private RoomEntity roomA;
    private RoomEntity roomB;
    private RoomUnitEntity a1;
    private RoomUnitEntity a2;
    private RoomUnitEntity a3;
    private RoomUnitEntity aInactive;
    private RoomUnitEntity b1;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        roomA = persistRoom("Forecast A");
        roomB = persistRoom("Forecast B");
        a1 = persistUnit(roomA, "A1", true);
        a2 = persistUnit(roomA, "A2", true);
        a3 = persistUnit(roomA, "A3", true);
        aInactive = persistUnit(roomA, "A4", false);
        b1 = persistUnit(roomB, "B1", true);

        UserEntity newUser = new UserEntity();
        newUser.setEmail("forecast-" + UUID.randomUUID() + "@example.com");
        newUser.setName(newUser.getEmail());
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.MANAGER);
        user = userRepository.saveAndFlush(newUser);
    }

    /**
     * 2033-12-20 .. 12-23. Room type A has three active units (A4 is inactive, and its block is
     * ignored), B has one.
     * <ul>
     *   <li>P: A1, 12-20 -> 12-22. Arrives the 20th, departs the 22nd.
     *   <li>Q: A2, 12-18 -> 12-20. Departs the 20th.
     *   <li>R: relocated - A2 12-21 -> 12-22, then B1 12-22 -> 12-24. Its {@code roomId} is B (the
     *       last segment's, as {@code BookingWriter} syncs it), but it arrives in A.
     *   <li>S: cancelled, A3 12-21 -> 12-23. Counts nowhere.
     *   <li>T: A, unassigned, 12-23 -> 12-25.
     *   <li>A3 blocked 12-22 .. 12-23.
     * </ul>
     */
    @Test
    void forecast_matchesHandComputedFigures_perRoomTypeAndDate() {
        persistStay(roomA, a1, "2033-12-20", "2033-12-22");
        persistStay(roomA, a2, "2033-12-18", "2033-12-20");
        BookingEntity r = persistBooking(roomB, "2033-12-21", "2033-12-24");
        persistSegment(r, roomA, a2, "2033-12-21", "2033-12-22");
        persistSegment(r, roomB, b1, "2033-12-22", "2033-12-24");
        BookingEntity s = persistStay(roomA, a3, "2033-12-21", "2033-12-23");
        s.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(s);
        persistStay(roomA, null, "2033-12-23", "2033-12-25");
        persistBlock(a3, "2033-12-22", "2033-12-23");
        persistBlock(aInactive, "2033-12-20", "2033-12-23");

        ForecastReport report = reportService.forecast("2033-12-20", "2033-12-23");

        assertThat(report.getFrom()).isEqualTo("2033-12-20");
        assertThat(report.getTo()).isEqualTo("2033-12-23");
        assertThat(report.getDays()).extracting(ForecastDay::getDate)
                .containsExactly("2033-12-20", "2033-12-21", "2033-12-22", "2033-12-23");

        //                                        arr dep total ooo occ vacant  occ%
        assertRow(row(report, 0, roomA), 1, 1, 3, 0, 1, 2, "33.33");
        assertRow(row(report, 1, roomA), 1, 0, 3, 0, 2, 1, "66.67"); // R arrives in A, not B
        assertRow(row(report, 2, roomA), 0, 1, 3, 1, 0, 2, "0.00");
        assertRow(row(report, 3, roomA), 1, 0, 3, 1, 1, 1, "50.00");

        assertRow(row(report, 0, roomB), 0, 0, 1, 0, 0, 1, "0.00");
        assertRow(row(report, 1, roomB), 0, 0, 1, 0, 0, 1, "0.00");
        assertRow(row(report, 2, roomB), 0, 0, 1, 0, 1, 0, "100.00");
        assertRow(row(report, 3, roomB), 0, 0, 1, 0, 1, 0, "100.00");
    }

    /**
     * Each date's figures are the other reports' own for that single night: occupied is
     * {@code GET /reports/occupancy?from=d&to=d}'s room-nights sold, per room type and in total;
     * the total's arrivals, departures, inventory and occupancy % are the manager report's.
     */
    @Test
    void forecast_eachDate_agreesWithOccupancyAndManagerReportsForThatNight() {
        persistStay(roomA, a1, "2033-12-20", "2033-12-22");
        persistStay(roomA, a2, "2033-12-18", "2033-12-20");
        BookingEntity r = persistBooking(roomB, "2033-12-21", "2033-12-24");
        persistSegment(r, roomA, a2, "2033-12-21", "2033-12-22");
        persistSegment(r, roomB, b1, "2033-12-22", "2033-12-24");
        persistStay(roomA, null, "2033-12-23", "2033-12-25");
        persistBlock(a3, "2033-12-22", "2033-12-23");

        for (ForecastDay day : reportService.forecast("2033-12-19", "2033-12-24").getDays()) {
            OccupancyReport occupancy = reportService.occupancy(day.getDate(), day.getDate());
            assertThat(day.getTotal().getOccupied()).as(day.getDate()).isEqualTo(occupancy.getTotal().getRoomNightsSold());
            for (OccupancyReportRow room : occupancy.getRooms()) {
                ForecastRow forecastRow = day.getRooms().stream()
                        .filter(f -> f.getRoomId().get().equals(room.getRoomId().get()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("No forecast row for " + room.getRoomName().get() + " on " + day.getDate()));
                assertThat(forecastRow.getOccupied()).as(day.getDate()).isEqualTo(room.getRoomNightsSold());
            }

            ManagerReportDay manager = reportService.manager(day.getDate()).getToday();
            assertThat(day.getTotal().getArrivals()).as(day.getDate()).isEqualTo(manager.getAccounts().getArrivals());
            assertThat(day.getTotal().getDepartures()).as(day.getDate()).isEqualTo(manager.getAccounts().getDepartures());
            assertThat(day.getTotal().getTotalRooms()).as(day.getDate()).isEqualTo(manager.getRooms().getTotalRooms());
            assertThat(day.getTotal().getOutOfOrder()).as(day.getDate()).isEqualTo(manager.getRooms().getOutOfOrder());
            assertThat(day.getTotal().getOccupied()).as(day.getDate()).isEqualTo(manager.getRooms().getOccupied());
            assertThat(day.getTotal().getOccupancyPercent().get()).as(day.getDate()).isEqualTo(manager.getRooms().getOccupancyPercent().get());
            assertThat(day.getTotal().getVacant()).as(day.getDate())
                    .isEqualTo(manager.getRooms().getTotalRooms() - manager.getRooms().getOccupied() - manager.getRooms().getOutOfOrder());
        }
    }

    /** More rooms sold than sellable - an overbooking, or a booked room blocked since - shows as negative vacant, not zero. */
    @Test
    void forecast_vacant_isNotClampedAtZero() {
        persistStay(roomB, b1, "2033-12-20", "2033-12-21");
        persistStay(roomB, null, "2033-12-20", "2033-12-21");

        ForecastRow b = row(reportService.forecast("2033-12-20", "2033-12-20"), 0, roomB);

        assertThat(b.getOccupied()).isEqualTo(2);
        assertThat(b.getVacant()).isEqualTo(-1);
        assertThat(b.getOccupancyPercent().get()).isEqualTo("200.00");
    }

    /** Far in the future is the normal case here, not an error. */
    @Test
    void forecast_acceptsAFutureRange() {
        ForecastReport report = reportService.forecast("2040-06-01", "2040-06-03");

        assertThat(report.getDays()).extracting(ForecastDay::getDate).containsExactly("2040-06-01", "2040-06-02", "2040-06-03");
        assertThat(row(report, 0, roomA).getTotalRooms()).isEqualTo(3);
    }

    @Test
    void forecast_invalidRange_isValidationError() {
        assertThatThrownBy(() -> reportService.forecast("2033-12-21", "2033-12-20")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> reportService.forecast("2033-02-30", "2033-03-01")).isInstanceOf(ValidationException.class);
    }

    // --- Fixtures -----------------------------------------------------------------------------

    private static ForecastRow row(ForecastReport report, int dayIndex, RoomEntity room) {
        return report.getDays().get(dayIndex).getRooms().stream()
                .filter(r -> room.getId().equals(r.getRoomId().get()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No row for " + room.getName()));
    }

    private static void assertRow(
            ForecastRow row, int arrivals, int departures, int totalRooms, int outOfOrder, int occupied, int vacant, String occupancyPercent) {
        assertThat(List.of(row.getArrivals(), row.getDepartures(), row.getTotalRooms(), row.getOutOfOrder(), row.getOccupied(), row.getVacant()))
                .as("arrivals, departures, totalRooms, outOfOrder, occupied, vacant")
                .containsExactly(arrivals, departures, totalRooms, outOfOrder, occupied, vacant);
        assertThat(row.getOccupancyPercent().get()).isEqualTo(occupancyPercent);
    }

    private RoomEntity persistRoom(String name) {
        RoomEntity room = new RoomEntity();
        room.setName(name + " " + UUID.randomUUID());
        room.setDescription("Room used only by ReportForecastTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        return roomRepository.saveAndFlush(room);
    }

    private RoomUnitEntity persistUnit(RoomEntity room, String label, boolean active) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel(label + "-" + UUID.randomUUID().toString().substring(0, 8));
        unit.setActive(active);
        return roomUnitRepository.saveAndFlush(unit);
    }

    private void persistBlock(RoomUnitEntity unit, String from, String to) {
        RoomUnitBlockEntity block = new RoomUnitBlockEntity();
        block.setRoomUnitId(unit.getId());
        block.setFromDate(LocalDate.parse(from));
        block.setToDate(LocalDate.parse(to));
        block.setReason("Forecast test");
        block.setCreatedByUserId(user.getId());
        blockRepository.saveAndFlush(block);
    }

    /** A booking with one segment covering the whole stay - the never-relocated case. */
    private BookingEntity persistStay(RoomEntity room, RoomUnitEntity unit, String checkIn, String checkOut) {
        BookingEntity booking = persistBooking(room, checkIn, checkOut);
        persistSegment(booking, room, unit, checkIn, checkOut);
        return booking;
    }

    private BookingEntity persistBooking(RoomEntity room, String checkIn, String checkOut) {
        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setOccupancyStatus(OccupancyStatus.EXPECTED);
        booking.setAdults(1);
        booking.setChildren(0);
        booking.setRoomId(room.getId());
        booking.setGuestName("Forecast Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.parse(checkIn));
        booking.setCheckOut(LocalDate.parse(checkOut));
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.saveAndFlush(booking);
    }

    private void persistSegment(BookingEntity booking, RoomEntity room, RoomUnitEntity unit, String checkIn, String checkOut) {
        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(booking.getId());
        segment.setRoomId(room.getId());
        segment.setRoomUnitId(unit != null ? unit.getId() : null);
        segment.setCheckIn(LocalDate.parse(checkIn));
        segment.setCheckOut(LocalDate.parse(checkOut));
        segment.setTotalPrice(new BigDecimal("1000.00"));
        segmentRepository.saveAndFlush(segment);
    }
}
