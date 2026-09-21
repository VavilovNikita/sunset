package com.sunsetbeach.service;

import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.RosterEmployee;
import com.sunsetbeach.model.RosterEntry;
import com.sunsetbeach.model.RosterMonth;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two of this module's Excel exports, sharing one class for the POI plumbing (cell styles, byte[]
 * plumbing) neither is big enough to justify factoring out on its own.
 *
 * <p>{@link #exportGrid} ({@code GET /roster/export}) is an .xlsx mirror of the on-screen grid
 * ({@code RosterGrid.tsx}) for one month, ADMIN only (same floor as the Excel *import*, not the
 * actuals export - see this endpoint's own openapi.yaml description for why). Deliberately
 * re-derives {@link RosterService#getMonth} rather than reading a stored grid: there is no cached
 * "the grid" anywhere, the React component computes what's on screen straight from {@code
 * RosterMonth} + a client-side reducer every render, and this does the same in Java, row for row.
 *
 * <p>Grouping/order, per-day totals (Working/Off/Absent), and cell colouring (kind-shape cues
 * layered under a code's own {@code displayColor}, falling back to the grid's own
 * lightness-by-start-time neutral scheme when unset) are all reimplemented here rather than
 * shared with the frontend, because none of it exists as backend code today - {@code
 * RosterGrid.tsx}'s own totals block and colour scheme are the only place this logic lived before
 * this endpoint. Kept in careful lockstep with that file's own comments (see the constants below)
 * rather than factored into a shared module, since there is no shared module between a Next.js
 * frontend and this Java backend to put one in.
 *
 * <p>{@link #exportActuals} ({@code GET /roster/actuals-export}) reports what the roster planned
 * against what {@code AttendancePunch} actually recorded, MANAGER floor. Unlike {@code
 * exportGrid}, it queries {@code RosterEntryRepository}/{@code AttendancePunchRepository} directly
 * rather than going through {@code RosterService#getMonth}: that method's employee universe is
 * every *active* {@code User}, but this export's is the union of "had a counts-as-worked entry" and
 * "punched at least once" - narrower in the ordinary case (an active employee with neither is
 * simply absent from the report, not a zero-filled row) and occasionally wider (a punch with no
 * roster entry at all is exactly the anomaly this export exists to surface). Per-day IN/OUT
 * pairing is {@link AttendancePunchPairing#sumWorkedMinutes}/{@link AttendancePunchPairing#pairs},
 * the same rule {@link AttendanceService#summary} uses - see that class's own javadoc for why
 * there is exactly one copy of it. Three sheets, all built from the one set of entries/punches
 * queried at the top of the method - Summary, Arrivals &amp; departures, and Late &amp; anomalies
 * (per-issue rows: late arrivals, early departures, missed/unscheduled/incomplete days - see
 * {@link #writeLateAndAnomaliesSheet} for the per-interval matching rule).
 */
@Service
public class RosterExportService {

    private static final List<StaffArea> STAFF_AREA_ORDER =
            List.of(StaffArea.ADMIN, StaffArea.FRONT_OFFICE, StaffArea.MAINTENANCE, StaffArea.HOUSEKEEPING, StaffArea.RESTAURANT, StaffArea.KITCHEN);

    private static final Map<StaffArea, String> STAFF_AREA_LABELS = Map.of(
            StaffArea.ADMIN, "Admin",
            StaffArea.FRONT_OFFICE, "Front office",
            StaffArea.MAINTENANCE, "Maintenance",
            StaffArea.HOUSEKEEPING, "Housekeeping",
            StaffArea.RESTAURANT, "Restaurant",
            StaffArea.KITCHEN, "Kitchen");

    // Same neutral lightness-by-start-time scale as RosterGrid.tsx's own chipRgbFor - see that
    // file's own comment block for why these exact anchors/mix cap were chosen.
    private static final int[] CHIP_DARK_RGB = {0x15, 0x31, 0x38};
    private static final int[] CHIP_LIGHT_RGB = {0xFB, 0xF6, 0xEC};
    private static final int EARLIEST_MINUTES = 6 * 60;
    private static final int LATEST_MINUTES = 23 * 60;
    private static final double MAX_LIGHT_MIX = 0.55;
    private static final int[] DARK_TEXT_RGB = {0x0F, 0x26, 0x2B};
    private static final int[] LIGHT_TEXT_RGB = {0xFB, 0xF6, 0xEC};
    private static final int[] NEUTRAL_BORDER_RGB = {0x99, 0x99, 0x99};
    private static final int[] UNCONFIRMED_FILL_RGB = {0xE0, 0xE0, 0xE0};
    private static final int[] WEEKEND_FILL_RGB = {0xF0, 0xF0, 0xF0};

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final RosterService rosterService;
    private final AuditLogService auditLogService;
    private final RosterEntryRepository rosterEntryRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final AttendancePunchRepository attendancePunchRepository;
    private final UserRepository userRepository;

    public RosterExportService(
            RosterService rosterService,
            AuditLogService auditLogService,
            RosterEntryRepository rosterEntryRepository,
            ShiftCodeRepository shiftCodeRepository,
            AttendancePunchRepository attendancePunchRepository,
            UserRepository userRepository) {
        this.rosterService = rosterService;
        this.auditLogService = auditLogService;
        this.rosterEntryRepository = rosterEntryRepository;
        this.shiftCodeRepository = shiftCodeRepository;
        this.attendancePunchRepository = attendancePunchRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportGrid(int year, int month, String actorUserId) {
        RosterMonth data = rosterService.getMonth(year, month);
        YearMonth ym = YearMonth.of(year, month);
        int days = ym.lengthOfMonth();
        LocalDate today = LocalDate.now();
        int todayDay = (today.getYear() == year && today.getMonthValue() == month) ? today.getDayOfMonth() : -1;

        Map<String, RosterEntry> entriesByKey = new HashMap<>();
        for (RosterEntry e : data.getEntries()) {
            entriesByKey.put(e.getEmployeeUserId() + "|" + e.getDate(), e);
        }

        java.util.Set<String> employeeIds = data.getEmployees().stream().map(RosterEmployee::getId).collect(java.util.stream.Collectors.toSet());
        Map<Integer, Integer> workingByDay = new HashMap<>();
        Map<Integer, Integer> absentByDay = new HashMap<>();
        Map<Integer, Integer> presentByDay = new HashMap<>();
        for (RosterEntry e : data.getEntries()) {
            if (!employeeIds.contains(e.getEmployeeUserId())) continue;
            int day = LocalDate.parse(e.getDate()).getDayOfMonth();
            presentByDay.merge(day, 1, Integer::sum);
            ShiftCode sc = e.getShiftCode();
            if (Boolean.TRUE.equals(sc.getCountsAsWorked())) workingByDay.merge(day, 1, Integer::sum);
            if (kindOf(sc) == ShiftCodeKind.ABSENCE) absentByDay.merge(day, 1, Integer::sum);
        }

        Map<StaffArea, List<RosterEmployee>> byArea = new LinkedHashMap<>();
        for (RosterEmployee emp : data.getEmployees()) {
            byArea.computeIfAbsent(resolveArea(emp), k -> new java.util.ArrayList<>()).add(emp);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Roster " + year + "-" + String.format("%02d", month));

            XSSFCellStyle headerStyle = boldStyle(workbook);
            XSSFCellStyle groupStyle = groupHeaderStyle(workbook);
            XSSFCellStyle nameStyle = workbook.createCellStyle();
            XSSFCellStyle totalsLabelStyle = boldStyle(workbook);

            int rowIndex = 0;
            Row dayRow = sheet.createRow(rowIndex++);
            Row weekdayRow = sheet.createRow(rowIndex++);
            Cell empHeader = dayRow.createCell(0);
            empHeader.setCellValue("Employee");
            empHeader.setCellStyle(headerStyle);
            weekdayRow.createCell(0);

            for (int day = 1; day <= days; day++) {
                LocalDate date = ym.atDay(day);
                boolean isToday = day == todayDay;
                boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;

                Cell dayCell = dayRow.createCell(day);
                dayCell.setCellValue(day);
                dayCell.setCellStyle(headerCellStyle(workbook, isWeekend, isToday));

                Cell weekdayCell = weekdayRow.createCell(day);
                weekdayCell.setCellValue(date.getDayOfWeek().toString().substring(0, 3));
                weekdayCell.setCellStyle(headerCellStyle(workbook, isWeekend, isToday));
            }

            for (Map.Entry<StaffArea, List<RosterEmployee>> group : orderedGroups(byArea)) {
                List<RosterEmployee> employees = group.getValue();
                if (employees.isEmpty()) continue;

                Row groupRow = sheet.createRow(rowIndex++);
                Cell groupCell = groupRow.createCell(0);
                groupCell.setCellValue(group.getKey() != null ? STAFF_AREA_LABELS.get(group.getKey()) : "No area set");
                groupCell.setCellStyle(groupStyle);
                sheet.addMergedRegion(new CellRangeAddress(groupRow.getRowNum(), groupRow.getRowNum(), 0, days));

                for (RosterEmployee emp : employees) {
                    Row row = sheet.createRow(rowIndex++);
                    Cell nameCell = row.createCell(0);
                    nameCell.setCellValue(emp.getName());
                    nameCell.setCellStyle(nameStyle);

                    for (int day = 1; day <= days; day++) {
                        String date = ym.atDay(day).toString();
                        boolean isWeekend = ym.atDay(day).getDayOfWeek() == DayOfWeek.SATURDAY || ym.atDay(day).getDayOfWeek() == DayOfWeek.SUNDAY;
                        RosterEntry entry = entriesByKey.get(emp.getId() + "|" + date);
                        Cell cell = row.createCell(day);
                        if (entry == null) {
                            cell.setCellStyle(emptyCellStyle(workbook, isWeekend));
                        } else {
                            cell.setCellValue(entry.getShiftCode().getCode());
                            cell.setCellStyle(chipCellStyle(workbook, entry.getShiftCode()));
                        }
                    }
                }
            }

            rowIndex += 1;
            rowIndex = writeTotalsRow(sheet, rowIndex, days, "Working", day -> workingByDay.getOrDefault(day, 0), totalsLabelStyle, workbook);
            rowIndex = writeTotalsRow(sheet, rowIndex, days, "Off", day -> employeeIds.size() - presentByDay.getOrDefault(day, 0), totalsLabelStyle, workbook);
            writeTotalsRow(sheet, rowIndex, days, "Absent", day -> absentByDay.getOrDefault(day, 0), totalsLabelStyle, workbook);

            sheet.setColumnWidth(0, 20 * 256);
            for (int day = 1; day <= days; day++) {
                sheet.setColumnWidth(day, 6 * 256);
            }
            sheet.createFreezePane(1, 2);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            auditLogService.record(
                    AuditAction.ROSTER_GRID_EXPORTED, AuditEntityType.ROSTER_ENTRY, null,
                    "Exported the " + ym + " roster grid as Excel");

            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Plan vs. reality for one month, two sheets. No money: the hotel's accountant keeps pay
     * calculation off-system - everyone is currently on a monthly salary held in a sheet this
     * system has never seen, and there are no part-timers - so this export reports only the
     * underlying facts and leaves the arithmetic where it already lives. See {@link
     * EmployeePayRateService}'s own javadoc for why the versioned rate model stays in the codebase
     * even though nothing here calls it.
     */
    @Transactional(readOnly = true)
    public byte[] exportActuals(int year, int month, String actorUserId) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<RosterEntryEntity> entries = rosterEntryRepository.findByDateBetween(from, to);
        Map<String, ShiftCodeEntity> shiftCodesById = shiftCodeRepository
                .findAllById(entries.stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ShiftCodeEntity::getId, s -> s));
        Map<String, List<RosterEntryEntity>> assignedByEmployee = entries.stream()
                .filter(e -> {
                    ShiftCodeEntity sc = shiftCodesById.get(e.getShiftCodeId());
                    return sc != null && sc.isCountsAsWorked();
                })
                .collect(Collectors.groupingBy(RosterEntryEntity::getEmployeeUserId));

        // Ordered by employeeUserId then punchAt (see the repository method's own javadoc) - each
        // group's own list below stays punchAt-sorted for free, exactly what same-day pairing needs.
        List<AttendancePunchEntity> punches =
                attendancePunchRepository.findByPunchAtBetweenOrderByEmployeeUserIdAscPunchAtAsc(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<String, List<AttendancePunchEntity>> punchesByEmployee = punches.stream().collect(Collectors.groupingBy(AttendancePunchEntity::getEmployeeUserId));

        // Union, not just who's scheduled - a punch with no roster entry is exactly the anomaly
        // this export exists to surface, not drop. See this class's own javadoc for why this
        // can't just reuse RosterService#getMonth's "every active employee" universe.
        Set<String> employeeIds = new HashSet<>(assignedByEmployee.keySet());
        employeeIds.addAll(punchesByEmployee.keySet());
        Map<String, UserEntity> employees = userRepository.findAllById(employeeIds).stream().collect(Collectors.toMap(UserEntity::getId, u -> u));
        List<String> orderedEmployeeIds = employeeIds.stream().sorted((a, b) -> employees.get(a).getName().compareTo(employees.get(b).getName())).toList();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            writeActualsSummarySheet(workbook, orderedEmployeeIds, employees, assignedByEmployee, shiftCodesById, punchesByEmployee);
            writeActualsPunchSheet(workbook, orderedEmployeeIds, employees, punchesByEmployee);
            writeLateAndAnomaliesSheet(workbook, orderedEmployeeIds, employees, assignedByEmployee, shiftCodesById, punchesByEmployee);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            auditLogService.record(
                    AuditAction.ROSTER_ACTUALS_EXPORTED, AuditEntityType.ROSTER_ENTRY, null,
                    "Exported " + ym + " roster actuals for " + orderedEmployeeIds.size() + " employee(s)");

            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeActualsSummarySheet(
            XSSFWorkbook workbook, List<String> employeeIds, Map<String, UserEntity> employees,
            Map<String, List<RosterEntryEntity>> assignedByEmployee, Map<String, ShiftCodeEntity> shiftCodesById,
            Map<String, List<AttendancePunchEntity>> punchesByEmployee) {
        XSSFSheet sheet = workbook.createSheet("Summary");
        XSSFCellStyle headerStyle = boldStyle(workbook);

        String[] columns = {"Employee", "Days assigned", "Hours assigned", "Days present", "Hours worked", "Incomplete days"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;
        for (String employeeId : employeeIds) {
            ActualsTotals totals = actualsTotalsFor(assignedByEmployee.get(employeeId), shiftCodesById, punchesByEmployee.get(employeeId));
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(employees.get(employeeId).getName());
            row.createCell(1).setCellValue(totals.daysAssigned());
            row.createCell(2).setCellValue(totals.hoursAssigned().doubleValue());
            row.createCell(3).setCellValue(totals.daysPresent());
            row.createCell(4).setCellValue(totals.hoursWorked().doubleValue());
            row.createCell(5).setCellValue(totals.incompleteDays());
        }

        sheet.setColumnWidth(0, 24 * 256);
        for (int i = 1; i < columns.length; i++) {
            sheet.setColumnWidth(i, 14 * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private void writeActualsPunchSheet(
            XSSFWorkbook workbook, List<String> employeeIds, Map<String, UserEntity> employees,
            Map<String, List<AttendancePunchEntity>> punchesByEmployee) {
        XSSFSheet sheet = workbook.createSheet("Arrivals & departures");
        XSSFCellStyle headerStyle = boldStyle(workbook);

        String[] columns = {"Employee", "Date", "Direction", "Time", "Source", "Note"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;
        for (String employeeId : employeeIds) {
            String name = employees.get(employeeId).getName();
            for (AttendancePunchEntity punch : punchesByEmployee.getOrDefault(employeeId, List.of())) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(name);
                row.createCell(1).setCellValue(punch.getPunchAt().toLocalDate().toString());
                row.createCell(2).setCellValue(punch.getDirection().getValue());
                row.createCell(3).setCellValue(punch.getPunchAt().toLocalTime().format(TIME_FORMAT));
                row.createCell(4).setCellValue(punch.getSource().getValue());
                row.createCell(5).setCellValue(punch.getNote() != null ? punch.getNote() : "");
            }
        }

        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 12 * 256);
        sheet.setColumnWidth(3, 10 * 256);
        sheet.setColumnWidth(4, 12 * 256);
        sheet.setColumnWidth(5, 30 * 256);
        sheet.createFreezePane(0, 1);
    }

    /**
     * One row per issue found on one employee's one day, reusing exactly the entries/punches the
     * other two sheets already loaded - no new queries. Per {@code employeeId}, walks the union of
     * "has a counts-as-worked entry" and "has a punch" dates, in order (the outer loop is already
     * {@code employeeIds} in name order, so employee-then-date falls out for free):
     *
     * <ul>
     *   <li>entry, no punches at all -&gt; {@code MISSED}.
     *   <li>no entry, punches exist -&gt; {@code UNSCHEDULED}.
     *   <li>entry and punches both exist -&gt; that day's punches are paired ({@link
     *       AttendancePunchPairing#pairs}, the same positional pairing {@link
     *       #actualsTotalsFor} sums minutes from) and matched, in order, to the shift code's own
     *       intervals (1 for {@code MORNING}/{@code EVENING}, 2 for {@code SPLIT}, 0 for {@code
     *       OPEN_SCHEDULE} - an {@code OP} day is therefore never eligible here, only for {@code
     *       MISSED}). Only matched pairs are compared - a pair beyond the interval count, or an
     *       interval beyond the pair count, has nothing to compare against and is silently not
     *       flagged (the former is already visible as an extra row on the Arrivals &amp; departures
     *       sheet). A late/early minute count is reported only when positive - arriving early or
     *       leaving late is not an anomaly, and there is no grace-period threshold to round it away.
     * </ul>
     *
     * <p>{@code INCOMPLETE} (an odd punch count that day) is checked independently of the three
     * cases above, off the same day's punches, whether or not there's an entry - the identical rule
     * {@link #actualsTotalsFor} already uses for the Summary sheet's "Incomplete days" column, so a
     * day can carry both an {@code UNSCHEDULED} row and an {@code INCOMPLETE} row.
     */
    private void writeLateAndAnomaliesSheet(
            XSSFWorkbook workbook, List<String> employeeIds, Map<String, UserEntity> employees,
            Map<String, List<RosterEntryEntity>> assignedByEmployee, Map<String, ShiftCodeEntity> shiftCodesById,
            Map<String, List<AttendancePunchEntity>> punchesByEmployee) {
        XSSFSheet sheet = workbook.createSheet("Late & anomalies");
        XSSFCellStyle headerStyle = boldStyle(workbook);

        String[] columns = {"Employee", "Date", "Type", "Detail"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;
        for (String employeeId : employeeIds) {
            String name = employees.get(employeeId).getName();
            Map<LocalDate, RosterEntryEntity> entryByDate = assignedByEmployee.getOrDefault(employeeId, List.of()).stream()
                    .collect(Collectors.toMap(RosterEntryEntity::getDate, e -> e));
            Map<LocalDate, List<AttendancePunchEntity>> punchesByDate = punchesByEmployee.getOrDefault(employeeId, List.of()).stream()
                    .collect(Collectors.groupingBy(p -> p.getPunchAt().toLocalDate()));

            Set<LocalDate> dates = new TreeSet<>(entryByDate.keySet());
            dates.addAll(punchesByDate.keySet());

            for (LocalDate date : dates) {
                RosterEntryEntity entry = entryByDate.get(date);
                List<AttendancePunchEntity> dayPunches = punchesByDate.getOrDefault(date, List.of());

                if (entry == null) {
                    // Union membership guarantees dayPunches is non-empty here - a date with
                    // neither an entry nor a punch was never added to `dates` at all.
                    rowIndex = writeAnomalyRow(sheet, rowIndex, name, date, "UNSCHEDULED",
                            dayPunches.size() + " punch(es) recorded with no counts-as-worked roster entry that day");
                } else if (dayPunches.isEmpty()) {
                    rowIndex = writeAnomalyRow(sheet, rowIndex, name, date, "MISSED",
                            "Scheduled " + shiftCodesById.get(entry.getShiftCodeId()).getCode() + " - no punches recorded");
                    continue;
                } else {
                    rowIndex = writeLateAndLeftEarlyRows(sheet, rowIndex, name, date, shiftCodesById.get(entry.getShiftCodeId()), dayPunches);
                }

                if (dayPunches.size() % 2 != 0) {
                    rowIndex = writeAnomalyRow(sheet, rowIndex, name, date, "INCOMPLETE", dayPunches.size() + " punch(es) recorded - an odd count");
                }
            }
        }

        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 14 * 256);
        sheet.setColumnWidth(3, 46 * 256);
        sheet.createFreezePane(0, 1);
    }

    /** Only reached when both an entry and at least one punch exist that day - see {@link #writeLateAndAnomaliesSheet}. */
    private int writeLateAndLeftEarlyRows(
            XSSFSheet sheet, int rowIndex, String employeeName, LocalDate date, ShiftCodeEntity shiftCode, List<AttendancePunchEntity> dayPunches) {
        List<LocalTime[]> intervals = new java.util.ArrayList<>();
        if (shiftCode.getStartTime1() != null) intervals.add(new LocalTime[] {shiftCode.getStartTime1(), shiftCode.getEndTime1()});
        if (shiftCode.getStartTime2() != null) intervals.add(new LocalTime[] {shiftCode.getStartTime2(), shiftCode.getEndTime2()});

        List<AttendancePunchPairing.PunchPair> pairs = AttendancePunchPairing.pairs(dayPunches);
        int matched = Math.min(pairs.size(), intervals.size());
        for (int i = 0; i < matched; i++) {
            AttendancePunchPairing.PunchPair pair = pairs.get(i);
            LocalTime start = intervals.get(i)[0];
            LocalTime end = intervals.get(i)[1];
            LocalTime inTime = pair.in().getPunchAt().toLocalTime();
            LocalTime outTime = pair.out().getPunchAt().toLocalTime();

            long lateMinutes = Duration.between(start, inTime).toMinutes();
            if (lateMinutes > 0) {
                rowIndex = writeAnomalyRow(sheet, rowIndex, employeeName, date, "LATE",
                        lateMinutes + " min late for the " + start.format(TIME_FORMAT) + " shift (clocked in " + inTime.format(TIME_FORMAT) + ")");
            }
            long earlyMinutes = Duration.between(outTime, end).toMinutes();
            if (earlyMinutes > 0) {
                rowIndex = writeAnomalyRow(sheet, rowIndex, employeeName, date, "LEFT_EARLY",
                        earlyMinutes + " min left early from the " + end.format(TIME_FORMAT) + " shift (clocked out " + outTime.format(TIME_FORMAT) + ")");
            }
        }
        return rowIndex;
    }

    private int writeAnomalyRow(XSSFSheet sheet, int rowIndex, String employeeName, LocalDate date, String type, String detail) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(employeeName);
        row.createCell(1).setCellValue(date.toString());
        row.createCell(2).setCellValue(type);
        row.createCell(3).setCellValue(detail);
        return rowIndex + 1;
    }

    private record ActualsTotals(int daysAssigned, BigDecimal hoursAssigned, int daysPresent, BigDecimal hoursWorked, int incompleteDays) {
    }

    /**
     * {@code assignedEntries}/{@code employeePunches} are null, not an empty list, when this
     * employee has none of either - {@code Map#get} on an absent key, same as everywhere else this
     * class reads {@code punchesByEmployee}/{@code assignedByEmployee}.
     */
    private ActualsTotals actualsTotalsFor(
            List<RosterEntryEntity> assignedEntries, Map<String, ShiftCodeEntity> shiftCodesById, List<AttendancePunchEntity> employeePunches) {
        List<RosterEntryEntity> assigned = assignedEntries == null ? List.of() : assignedEntries;
        BigDecimal hoursAssigned = BigDecimal.ZERO;
        for (RosterEntryEntity entry : assigned) {
            hoursAssigned = hoursAssigned.add(hoursWorked(shiftCodesById.get(entry.getShiftCodeId())));
        }

        List<AttendancePunchEntity> punches = employeePunches == null ? List.of() : employeePunches;
        Map<LocalDate, List<AttendancePunchEntity>> byDay = punches.stream().collect(Collectors.groupingBy(p -> p.getPunchAt().toLocalDate(), LinkedHashMap::new, Collectors.toList()));

        int incompleteDays = 0;
        int workedMinutes = 0;
        for (List<AttendancePunchEntity> dayPunches : byDay.values()) {
            if (dayPunches.size() % 2 != 0) {
                incompleteDays++;
            }
            workedMinutes += AttendancePunchPairing.sumWorkedMinutes(dayPunches);
        }

        return new ActualsTotals(
                assigned.size(), hoursAssigned.setScale(2, RoundingMode.HALF_UP), byDay.size(),
                BigDecimal.valueOf(workedMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP), incompleteDays);
    }

    /** {@code OP} has no interval at all and contributes zero here - it still counts as a day assigned, just not as hours. */
    private static BigDecimal hoursWorked(ShiftCodeEntity shiftCode) {
        long minutes = 0;
        if (shiftCode.getStartTime1() != null) {
            minutes += Duration.between(shiftCode.getStartTime1(), shiftCode.getEndTime1()).toMinutes();
        }
        if (shiftCode.getStartTime2() != null) {
            minutes += Duration.between(shiftCode.getStartTime2(), shiftCode.getEndTime2()).toMinutes();
        }
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private interface DayValue {
        int get(int day);
    }

    private int writeTotalsRow(XSSFSheet sheet, int rowIndex, int days, String label, DayValue values, XSSFCellStyle labelStyle, XSSFWorkbook workbook) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        XSSFCellStyle valueStyle = boldStyle(workbook);
        valueStyle.setAlignment(HorizontalAlignment.CENTER);
        for (int day = 1; day <= days; day++) {
            Cell cell = row.createCell(day);
            cell.setCellValue(values.get(day));
            cell.setCellStyle(valueStyle);
        }
        return rowIndex + 1;
    }

    /**
     * Fixed group order (see {@link #STAFF_AREA_ORDER}), the no-area group last, same as {@code
     * RosterGrid.tsx}'s own {@code allGroups} - a group nobody belongs to this month is simply
     * never added here, mirroring that file's own "filter to non-empty groups" step.
     */
    private List<Map.Entry<StaffArea, List<RosterEmployee>>> orderedGroups(Map<StaffArea, List<RosterEmployee>> byArea) {
        List<Map.Entry<StaffArea, List<RosterEmployee>>> ordered = new java.util.ArrayList<>();
        for (StaffArea area : STAFF_AREA_ORDER) {
            if (byArea.containsKey(area)) ordered.add(Map.entry(area, byArea.get(area)));
        }
        if (byArea.containsKey(null)) {
            List<RosterEmployee> noArea = byArea.get(null);
            ordered.add(new java.util.AbstractMap.SimpleEntry<>(null, noArea));
        }
        return ordered;
    }

    private static StaffArea resolveArea(RosterEmployee emp) {
        return emp.getStaffArea() != null && emp.getStaffArea().isPresent() ? emp.getStaffArea().get() : null;
    }

    private static ShiftCodeKind kindOf(ShiftCode sc) {
        return sc.getKind() != null && sc.getKind().isPresent() ? sc.getKind().get() : null;
    }

    private static String displayColorOf(ShiftCode sc) {
        return sc.getDisplayColor() != null && sc.getDisplayColor().isPresent() ? sc.getDisplayColor().get() : null;
    }

    private static String startTime1Of(ShiftCode sc) {
        return sc.getStartTime1() != null && sc.getStartTime1().isPresent() ? sc.getStartTime1().get() : null;
    }

    private static String startTime2Of(ShiftCode sc) {
        return sc.getStartTime2() != null && sc.getStartTime2().isPresent() ? sc.getStartTime2().get() : null;
    }

    // --- Cell appearance, mirroring RosterGrid.tsx's chipAppearanceFor ------------------------

    private XSSFCellStyle chipCellStyle(XSSFWorkbook workbook, ShiftCode shiftCode) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        ShiftCodeKind kind = kindOf(shiftCode);
        String displayColor = displayColorOf(shiftCode);

        if (kind == null) {
            // Predates ShiftCode.kind and not yet confirmed - the grid's own plain, unstyled chip.
            setSolidFill(style, UNCONFIRMED_FILL_RGB);
            return style;
        }

        if (kind == ShiftCodeKind.MORNING || kind == ShiftCodeKind.EVENING) {
            int[] rgb = displayColor != null ? hexToRgb(displayColor) : chipRgbFor(startTime1Of(shiftCode));
            setSolidFill(style, rgb);
            setFontColor(workbook, style, contrastTextFor(rgb));
            return style;
        }
        if (kind == ShiftCodeKind.SPLIT) {
            int[] rgb1 = displayColor != null ? hexToRgb(displayColor) : chipRgbFor(startTime1Of(shiftCode));
            int[] rgb2 = displayColor != null ? hexToRgb(displayColor) : chipRgbFor(startTime2Of(shiftCode));
            style.setFillForegroundColor(rgbColor(rgb1));
            style.setFillBackgroundColor(rgbColor(rgb2));
            style.setFillPattern(FillPatternType.THIN_FORWARD_DIAG);
            setFontColor(workbook, style, contrastTextFor(rgb1));
            return style;
        }
        if (kind == ShiftCodeKind.OPEN_SCHEDULE) {
            int[] borderRgb = displayColor != null ? hexToRgb(displayColor) : NEUTRAL_BORDER_RGB;
            setBorder(workbook, style, borderRgb, BorderStyle.THIN);
            return style;
        }
        // ABSENCE - hollow, dashed border; a colour never turns this into a filled chip.
        int[] borderRgb = displayColor != null ? hexToRgb(displayColor) : NEUTRAL_BORDER_RGB;
        setBorder(workbook, style, borderRgb, BorderStyle.DASHED);
        XSSFFont font = workbook.createFont();
        font.setItalic(true);
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle emptyCellStyle(XSSFWorkbook workbook, boolean isWeekend) {
        XSSFCellStyle style = workbook.createCellStyle();
        if (isWeekend) setSolidFill(style, WEEKEND_FILL_RGB);
        return style;
    }

    private XSSFCellStyle headerCellStyle(XSSFWorkbook workbook, boolean isWeekend, boolean isToday) {
        XSSFCellStyle style = boldStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        if (isWeekend) setSolidFill(style, WEEKEND_FILL_RGB);
        if (isToday) style.setBorderLeft(BorderStyle.MEDIUM);
        return style;
    }

    private XSSFCellStyle boldStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle groupHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = boldStyle(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setSolidFill(XSSFCellStyle style, int[] rgb) {
        style.setFillForegroundColor(rgbColor(rgb));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private static XSSFColor rgbColor(int[] rgb) {
        return new XSSFColor(new byte[] {(byte) rgb[0], (byte) rgb[1], (byte) rgb[2]}, null);
    }

    private void setBorder(XSSFWorkbook workbook, XSSFCellStyle style, int[] rgb, BorderStyle borderStyle) {
        XSSFColor c = rgbColor(rgb);
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
        style.setTopBorderColor(c);
        style.setBottomBorderColor(c);
        style.setLeftBorderColor(c);
        style.setRightBorderColor(c);
    }

    private void setFontColor(XSSFWorkbook workbook, XSSFCellStyle style, int[] rgb) {
        XSSFFont font = workbook.createFont();
        font.setColor(rgbColor(rgb));
        style.setFont(font);
    }

    // --- Ported 1:1 from RosterGrid.tsx's own colour helpers -----------------------------------

    private static int[] hexToRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        int n = Integer.parseInt(h, 16);
        return new int[] {(n >> 16) & 255, (n >> 8) & 255, n & 255};
    }

    private static int[] mixRgb(int[] from, int[] to, double amount) {
        return new int[] {
            (int) Math.round(from[0] + (to[0] - from[0]) * amount),
            (int) Math.round(from[1] + (to[1] - from[1]) * amount),
            (int) Math.round(from[2] + (to[2] - from[2]) * amount)
        };
    }

    private static double earlinessFor(String time) {
        LocalTime t = LocalTime.parse(time);
        int minutes = t.getHour() * 60 + t.getMinute();
        double v = (LATEST_MINUTES - minutes) / (double) (LATEST_MINUTES - EARLIEST_MINUTES);
        return Math.max(0, Math.min(1, v));
    }

    private static int[] chipRgbFor(String time) {
        return mixRgb(CHIP_DARK_RGB, CHIP_LIGHT_RGB, earlinessFor(time) * MAX_LIGHT_MIX);
    }

    private static int[] contrastTextFor(int[] rgb) {
        double luminance = 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
        return luminance > 150 ? DARK_TEXT_RGB : LIGHT_TEXT_RGB;
    }
}
