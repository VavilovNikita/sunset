package com.sunsetbeach.service;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.ManagerReportDay;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.TodayBoard;
import com.sunsetbeach.model.TodayBoardEntry;
import com.sunsetbeach.model.TodayShiftState;
import com.sunsetbeach.model.TodayShiftStatus;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.ShiftChangeRequestRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * The managers' summary of the hotel's day, for the bot's "Summary" command and the automatic morning message.
 *
 * <p><b>Nothing here is computed from raw data.</b> Every figure is read from the service that shows it in the admin
 * screens, so the bot and the admin can't disagree: arrivals still expected, departures still to leave, overdue
 * departures (the board's {@code overdueDays}, i.e. {@code OverstayRule}) and the rooms in the house are
 * {@link BookingOccupancyService#getTodayBoard} (the Today board); the totals booked for the day, the guests in the
 * house, the occupancy percentage and the number of blocked rooms are {@link ReportService#manager} (the manager
 * report); the rooms waiting to be cleaned are {@link PropertyMapService#get} (the property map); the roster, who is on
 * shift and who is absent are {@link AttendanceService#getTodayShiftBoard} (the attendance board); the waiting
 * requests are the {@code PENDING} shift requests the admin list shows. The one derived number is "rooms still free
 * tonight" - the manager report's {@code availableForSale} minus its {@code occupied}, two figures it already
 * publishes. A figure with no such source is not shown (arrivals and departures that have <em>already happened</em>
 * today have none: the board lists only the open work, and the report counts all bookings of the day whatever their
 * state, so "already arrived" would have to be counted anew).
 *
 * <p>Only the blocks switched on in {@link StaffBotSettingsEntity} are collected - a switched-off block costs
 * no query - and the data is collected once and worded per reader's language.
 *
 * <p><b>A silent terminal changes the staff block.</b> {@link TerminalHealthService} says whether punches can be
 * trusted; when they can't, "on shift" and "absent" are left out and the line says why instead.
 */
@Service
public class StaffSummaryService {

    /** One block's figures; a null block was switched off and is not shown. */
    public record Arrivals(int expected, int total) {
    }

    public record Departures(int due, int overdue, int total) {
    }

    public record InHouse(int rooms, int guests) {
    }

    public record Occupancy(String percent, int free) {
    }

    public record Rooms(int blocked, int dirty) {
    }

    /** {@code onShift} and {@code absent} are meaningless (and not shown) unless {@code terminalOk}. */
    public record Staff(boolean terminalOk, int rostered, int onShift, int absent) {
    }

    public record Data(LocalDate date, Arrivals arrivals, Departures departures, InHouse inHouse, Occupancy occupancy, Rooms rooms, Staff staff, Integer pendingRequests) {
        public boolean isEmpty() {
            return arrivals == null && departures == null && inHouse == null && occupancy == null && rooms == null && staff == null && pendingRequests == null;
        }
    }

    private final BookingOccupancyService bookingOccupancyService;
    private final ReportService reportService;
    private final PropertyMapService propertyMapService;
    private final AttendanceService attendanceService;
    private final TerminalHealthService terminalHealthService;
    private final ShiftChangeRequestRepository requestRepository;
    private final Clock clock;

    public StaffSummaryService(
            BookingOccupancyService bookingOccupancyService,
            ReportService reportService,
            PropertyMapService propertyMapService,
            AttendanceService attendanceService,
            TerminalHealthService terminalHealthService,
            ShiftChangeRequestRepository requestRepository,
            Clock clock) {
        this.bookingOccupancyService = bookingOccupancyService;
        this.reportService = reportService;
        this.propertyMapService = propertyMapService;
        this.attendanceService = attendanceService;
        this.terminalHealthService = terminalHealthService;
        this.requestRepository = requestRepository;
        this.clock = clock;
    }

    /** Reads the figures of every block the settings switch on, as of the hotel's today. */
    public Data collect(StaffBotSettingsEntity settings) {
        LocalDate today = LocalDate.now(clock);
        boolean needBoard = settings.isSummaryArrivals() || settings.isSummaryDepartures() || settings.isSummaryInHouse();
        boolean needReport = settings.isSummaryArrivals() || settings.isSummaryDepartures() || settings.isSummaryInHouse()
                || settings.isSummaryOccupancy() || settings.isSummaryRooms();
        TodayBoard board = needBoard ? bookingOccupancyService.getTodayBoard() : null;
        ManagerReportDay report = needReport ? reportService.manager(today.toString()).getToday() : null;

        Arrivals arrivals = settings.isSummaryArrivals()
                ? new Arrivals(board.getArrivingToday().size(), report.getAccounts().getArrivals())
                : null;
        Departures departures = null;
        if (settings.isSummaryDepartures()) {
            int overdue = (int) board.getDepartingToday().stream().filter(StaffSummaryService::overdue).count();
            departures = new Departures(board.getDepartingToday().size() - overdue, overdue, report.getAccounts().getDepartures());
        }
        InHouse inHouse = settings.isSummaryInHouse()
                ? new InHouse(board.getInHouse().size(), report.getGuests().getGuestsInHouse())
                : null;
        Occupancy occupancy = settings.isSummaryOccupancy()
                ? new Occupancy(report.getRooms().getOccupancyPercent().orElse(null), report.getRooms().getAvailableForSale() - report.getRooms().getOccupied())
                : null;
        Rooms rooms = null;
        if (settings.isSummaryRooms()) {
            int dirty = (int) propertyMapService.get().getUnits().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsActive()) && u.getHousekeepingStatus() == HousekeepingStatus.DIRTY)
                    .count();
            rooms = new Rooms(report.getRooms().getOutOfOrder(), dirty);
        }
        Staff staff = settings.isSummaryStaff() ? staff() : null;
        Integer pending = settings.isSummaryRequests() ? requestRepository.findByStatus("PENDING").size() : null;
        return new Data(today, arrivals, departures, inHouse, occupancy, rooms, staff, pending);
    }

    private Staff staff() {
        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();
        boolean terminalOk = terminalHealthService.current().healthy();
        int rostered = (int) board.stream().filter(s -> !Boolean.TRUE.equals(s.getUnscheduled())).count();
        int onShift = (int) board.stream().filter(s -> s.getState() == TodayShiftState.ON_SHIFT).count();
        int absent = (int) board.stream().filter(s -> s.getState() == TodayShiftState.MISSED).count();
        return new Staff(terminalOk, rostered, onShift, absent);
    }

    private static boolean overdue(TodayBoardEntry entry) {
        return entry.getOverdueDays() != null && entry.getOverdueDays() > 0;
    }

    /** The summary as one message in {@code language}: only the blocks that were collected, one line each. */
    public String render(Data data, NotificationLanguage language) {
        List<String> lines = new ArrayList<>();
        lines.add(NotificationMessages.render(Key.SUM_TITLE, language, Map.of("date", BotText.date(data.date(), language))));
        if (data.isEmpty()) {
            lines.add(NotificationMessages.render(Key.SUM_NOTHING, language, Map.of()));
            return String.join("\n", lines);
        }
        if (data.arrivals() != null) {
            lines.add(line(Key.SUM_ARRIVALS, language, "expected", data.arrivals().expected(), "total", data.arrivals().total()));
        }
        if (data.departures() != null) {
            lines.add(line(Key.SUM_DEPARTURES, language, "due", data.departures().due(), "total", data.departures().total()));
            if (data.departures().overdue() > 0) {
                lines.add(line(Key.SUM_OVERDUE, language, "overdue", data.departures().overdue()));
            }
        }
        if (data.inHouse() != null) {
            lines.add(line(Key.SUM_INHOUSE, language, "rooms", data.inHouse().rooms(), "guests", data.inHouse().guests()));
        }
        if (data.occupancy() != null) {
            String percent = data.occupancy().percent() == null ? "-" : data.occupancy().percent() + "%";
            lines.add(NotificationMessages.render(Key.SUM_OCCUPANCY, language, Map.of("percent", percent, "free", String.valueOf(data.occupancy().free()))));
        }
        if (data.rooms() != null) {
            lines.add(line(Key.SUM_ROOMS, language, "blocked", data.rooms().blocked(), "dirty", data.rooms().dirty()));
        }
        if (data.staff() != null) {
            Staff staff = data.staff();
            lines.add(staff.terminalOk()
                    ? line(Key.SUM_STAFF, language, "rostered", staff.rostered(), "onShift", staff.onShift(), "absent", staff.absent())
                    : line(Key.SUM_STAFF_SILENT, language, "rostered", staff.rostered()));
        }
        if (data.pendingRequests() != null) {
            lines.add(line(Key.SUM_REQUESTS, language, "count", data.pendingRequests()));
        }
        return String.join("\n", lines);
    }

    private static String line(Key key, NotificationLanguage language, Object... pairs) {
        Map<String, String> params = new java.util.HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            params.put((String) pairs[i], String.valueOf(pairs[i + 1]));
        }
        return NotificationMessages.render(key, language, params);
    }
}
