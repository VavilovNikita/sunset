package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AttendancePunchCreateInput;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@code GET /roster/export} builds its own grouping, totals and cell colouring straight from
 * {@code RosterService#getMonth} rather than sharing code with the frontend (see {@code
 * RosterExportService}'s own javadoc for why) - these tests exist because that duplication is
 * exactly the kind of thing that silently drifts from what the grid actually shows. Assertions
 * are deliberately relative (a delta, or "row A before row B"), never an absolute headcount or row
 * index: {@code RosterService#listEmployees} reads every active {@code User} in the whole
 * database, which in this shared Testcontainers instance includes rows other test classes create
 * and may not always clean up - an exact count here would be flaky for a reason that has nothing
 * to do with this feature.
 */
@SpringBootTest
class RosterExportServiceTests extends AbstractIntegrationTest {

    @Autowired
    private RosterExportService rosterExportService;

    @Autowired
    private RosterService rosterService;

    @Autowired
    private ShiftCodeService shiftCodeService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private RosterEntryRepository rosterEntryRepository;

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private AttendancePunchRepository attendancePunchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdShiftCodeIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        attendancePunchRepository.deleteAll(attendancePunchRepository.findAll().stream().filter(p -> createdUserIds.contains(p.getEmployeeUserId())).toList());
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
    }

    private static OffsetDateTime at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atOffset(ZoneOffset.UTC);
    }

    private void punch(UserEntity employee, UserEntity actor, LocalDate date, int hour, int minute, PunchDirection direction) {
        attendanceService.recordPunch(new AttendancePunchCreateInput(employee.getId(), at(date, hour, minute), direction), actor.getId());
    }

    private UserEntity createEmployee(StaffArea area, String namePrefix) {
        UserEntity user = new UserEntity();
        user.setEmail(namePrefix.toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setName(namePrefix + " " + UUID.randomUUID().toString().substring(0, 8));
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.WAITER);
        user.setActive(true);
        user.setStaffArea(area);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private ShiftCode createCode(ShiftCodeKind kind, boolean countsAsWorked, String start, String end) {
        UserEntity actor = createEmployee(null, "Manager");
        ShiftCodeCreateInput input = new ShiftCodeCreateInput("X" + UUID.randomUUID().toString().substring(0, 6), kind, countsAsWorked, true, "2020-01-01");
        if (start != null) input.startTime1(start).endTime1(end);
        ShiftCode code = shiftCodeService.create(input, actor.getId());
        createdShiftCodeIds.add(code.getId());
        return code;
    }

    // Deliberately doesn't close the returned sheet's own workbook - every assertion below only
    // reads plain cached cell/style values, which stay valid after the try-with-resources above
    // would otherwise have released the underlying package; letting POI's finalizer reclaim it is
    // simpler than threading a Closeable through every test method for a value never mutated.
    @SuppressWarnings("resource")
    private XSSFSheet exportSheet(int year, int month) {
        byte[] bytes = rosterExportService.exportGrid(year, month, "irrelevant-actor-id");
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
            return workbook.getSheetAt(0);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private String cellText(Row row, int col) {
        Cell cell = row.getCell(col);
        return cell == null ? null : cell.getStringCellValue();
    }

    private int rowIndexOfEmployee(XSSFSheet sheet, String employeeName) {
        for (Row row : sheet) {
            Cell first = row.getCell(0);
            if (first != null && first.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && employeeName.equals(first.getStringCellValue())) {
                return row.getRowNum();
            }
        }
        throw new AssertionError("Employee row not found: " + employeeName);
    }

    // Same "don't close the workbook" reasoning as exportSheet above - only cached cell values are
    // ever read back out of these.
    @SuppressWarnings("resource")
    private XSSFWorkbook exportActualsWorkbook(int year, int month) {
        byte[] bytes = rosterExportService.exportActuals(year, month, "irrelevant-actor-id");
        try {
            return new XSSFWorkbook(new ByteArrayInputStream(bytes));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** The punch sheet has one row per punch, not one per employee - unlike rowIndexOfEmployee, this collects every match. */
    private List<Row> punchRowsForEmployee(XSSFSheet sheet, String employeeName) {
        List<Row> rows = new ArrayList<>();
        for (Row row : sheet) {
            Cell first = row.getCell(0);
            if (first != null && first.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && employeeName.equals(first.getStringCellValue())) {
                rows.add(row);
            }
        }
        return rows;
    }

    private double numeric(Row row, int col) {
        return row.getCell(col).getNumericCellValue();
    }

    private int totalsRowIndex(XSSFSheet sheet, String label) {
        for (Row row : sheet) {
            Cell first = row.getCell(0);
            if (first != null && first.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && label.equals(first.getStringCellValue())) {
                return row.getRowNum();
            }
        }
        throw new AssertionError("Totals row not found: " + label);
    }

    @Test
    void exportGrid_populatedCell_showsTheCodeAndItsOwnDisplayColor() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "Chip");
        ShiftCode createdCode = createCode(ShiftCodeKind.MORNING, true, "09:00", "17:00");
        shiftCodeService.updateDisplayColor(createdCode.getId(), "#3B82F6", mgr.getId());
        String codeId = createdCode.getId();
        // Re-read so the entry embeds the coloured version, not the pre-color-update snapshot.
        ShiftCode code = shiftCodeService.list(null).stream().filter(c -> c.getId().equals(codeId)).findFirst().orElseThrow();

        LocalDate date = LocalDate.of(2027, 5, 12);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), code.getId()), mgr.getId());

        XSSFSheet sheet = exportSheet(2027, 5);
        Row row = sheet.getRow(rowIndexOfEmployee(sheet, employee.getName()));
        assertThat(cellText(row, date.getDayOfMonth())).isEqualTo(code.getCode());

        Cell dayCell = row.getCell(date.getDayOfMonth());
        assertThat(dayCell.getCellStyle().getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
        byte[] rgb = ((org.apache.poi.xssf.usermodel.XSSFColor) dayCell.getCellStyle().getFillForegroundColorColor()).getRGB();
        // #3B82F6
        assertThat(rgb).containsExactly((byte) 0x3B, (byte) 0x82, (byte) 0xF6);
    }

    @Test
    void exportGrid_grouping_ordersRestaurantBeforeKitchenBeforeNoAreaSet() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity restaurantEmployee = createEmployee(StaffArea.RESTAURANT, "GroupR");
        UserEntity kitchenEmployee = createEmployee(StaffArea.KITCHEN, "GroupK");
        UserEntity noAreaEmployee = createEmployee(null, "GroupN");
        ShiftCode code = createCode(ShiftCodeKind.MORNING, true, "09:00", "17:00");
        LocalDate date = LocalDate.of(2027, 6, 8);
        rosterService.createEntry(new RosterEntryCreateInput(restaurantEmployee.getId(), date.toString(), code.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(kitchenEmployee.getId(), date.toString(), code.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(noAreaEmployee.getId(), date.toString(), code.getId()), mgr.getId());

        XSSFSheet sheet = exportSheet(2027, 6);
        int restaurantRow = rowIndexOfEmployee(sheet, restaurantEmployee.getName());
        int kitchenRow = rowIndexOfEmployee(sheet, kitchenEmployee.getName());
        int noAreaRow = rowIndexOfEmployee(sheet, noAreaEmployee.getName());

        // Fixed order (STAFF_AREA_ORDER, "No area set" last) - same as RosterGrid.tsx's own
        // allGroups, not alphabetical and not insertion order.
        assertThat(restaurantRow).isLessThan(kitchenRow);
        assertThat(kitchenRow).isLessThan(noAreaRow);
    }

    @Test
    void exportGrid_dailyTotals_matchTheGridsOwnDefinitions() {
        UserEntity mgr = createEmployee(null, "Manager");
        LocalDate date = LocalDate.of(2027, 7, 20);

        XSSFSheet before = exportSheet(2027, 7);
        int workingRowIdx = totalsRowIndex(before, "Working");
        int absentRowIdx = totalsRowIndex(before, "Absent");
        double workingBefore = before.getRow(workingRowIdx).getCell(date.getDayOfMonth()).getNumericCellValue();
        double absentBefore = before.getRow(absentRowIdx).getCell(date.getDayOfMonth()).getNumericCellValue();

        // One more countsAsWorked entry and one more ABSENCE entry, both on the same day - the
        // "Working"/"Absent" totals for that day must move by exactly one each, regardless of
        // whatever else the shared test database already had on that day.
        UserEntity workingEmployee = createEmployee(StaffArea.MAINTENANCE, "TotalsWorking");
        ShiftCode workingCode = createCode(ShiftCodeKind.MORNING, true, "09:00", "17:00");
        rosterService.createEntry(new RosterEntryCreateInput(workingEmployee.getId(), date.toString(), workingCode.getId()), mgr.getId());

        UserEntity absentEmployee = createEmployee(StaffArea.MAINTENANCE, "TotalsAbsent");
        ShiftCode absenceCode = createCode(ShiftCodeKind.ABSENCE, false, null, null);
        rosterService.createEntry(new RosterEntryCreateInput(absentEmployee.getId(), date.toString(), absenceCode.getId()), mgr.getId());

        XSSFSheet after = exportSheet(2027, 7);
        double workingAfter = after.getRow(totalsRowIndex(after, "Working")).getCell(date.getDayOfMonth()).getNumericCellValue();
        double absentAfter = after.getRow(totalsRowIndex(after, "Absent")).getCell(date.getDayOfMonth()).getNumericCellValue();

        assertThat(workingAfter).isEqualTo(workingBefore + 1);
        assertThat(absentAfter).isEqualTo(absentBefore + 1);
    }

    /**
     * Migrated from RosterService's own {@code exportActualsCsv} tests (that method moved here as
     * part of the CSV-to-Excel/actuals upgrade). Days/hours assigned is unchanged logic - a split
     * shift's two intervals must both be summed, and OP counts as a day with zero hours - only
     * relabeled and read from the Summary sheet's own columns instead of parsed CSV text.
     */
    @Test
    void exportActuals_daysAssignedHoursAssigned_sumsSplitShiftIntervalsAndTreatsOpenScheduleAsZeroHours() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "Assigned");
        ShiftCode ordinary = createCode(ShiftCodeKind.MORNING, true, "09:00", "17:00"); // 8 hours
        ShiftCode split = shiftCodeService.create(
                new ShiftCodeCreateInput("SPLIT" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.SPLIT, true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT)
                        .startTime1("09:00").endTime1("13:00")
                        .startTime2("16:00").endTime2("21:00"),
                mgr.getId());
        createdShiftCodeIds.add(split.getId());
        ShiftCode op = shiftCodeService.create(
                new ShiftCodeCreateInput("OP" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.OPEN_SCHEDULE, true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT),
                mgr.getId());
        createdShiftCodeIds.add(op.getId());

        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-10-01", ordinary.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-10-02", split.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-10-03", op.getId()), mgr.getId());

        XSSFSheet summary = exportActualsWorkbook(2027, 10).getSheetAt(0);
        Row row = summary.getRow(rowIndexOfEmployee(summary, employee.getName()));

        assertThat(numeric(row, 1)).isEqualTo(3); // Days assigned: ordinary + split + OP
        // 8.00 (ordinary) + 9.00 (4h + 5h split) + 0.00 (OP) = 17.00
        assertThat(numeric(row, 2)).isEqualTo(17.0);
    }

    @Test
    void exportActuals_scheduledEmployeeWhoPunchedEveryDayMatchingSchedule_assignedAndActualAgree() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "Matches");
        ShiftCode code = createCode(ShiftCodeKind.MORNING, true, "09:00", "17:00");
        LocalDate day1 = LocalDate.of(2027, 9, 6);
        LocalDate day2 = LocalDate.of(2027, 9, 7);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), day1.toString(), code.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), day2.toString(), code.getId()), mgr.getId());
        punch(employee, mgr, day1, 9, 0, PunchDirection.IN);
        punch(employee, mgr, day1, 17, 0, PunchDirection.OUT);
        punch(employee, mgr, day2, 9, 0, PunchDirection.IN);
        punch(employee, mgr, day2, 17, 0, PunchDirection.OUT);

        XSSFSheet summary = exportActualsWorkbook(2027, 9).getSheetAt(0);
        Row row = summary.getRow(rowIndexOfEmployee(summary, employee.getName()));

        assertThat(numeric(row, 1)).isEqualTo(2); // Days assigned
        assertThat(numeric(row, 2)).isEqualTo(16); // Hours assigned (2 x 8h)
        assertThat(numeric(row, 3)).isEqualTo(2); // Days present
        assertThat(numeric(row, 4)).isEqualTo(16); // Hours worked
        assertThat(numeric(row, 5)).isEqualTo(0); // Incomplete days
    }

    @Test
    void exportActuals_scheduledEmployeeWhoNeverPunched_zeroActualsButStillListed() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "NoPunch");
        ShiftCode code = createCode(ShiftCodeKind.MORNING, true, "09:00", "17:00");
        LocalDate date = LocalDate.of(2027, 9, 8);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), code.getId()), mgr.getId());

        XSSFWorkbook workbook = exportActualsWorkbook(2027, 9);
        XSSFSheet summary = workbook.getSheetAt(0);
        Row row = summary.getRow(rowIndexOfEmployee(summary, employee.getName()));

        assertThat(numeric(row, 1)).isEqualTo(1); // Days assigned
        assertThat(numeric(row, 2)).isEqualTo(8); // Hours assigned
        assertThat(numeric(row, 3)).isEqualTo(0); // Days present - not dropped, genuinely zero
        assertThat(numeric(row, 4)).isEqualTo(0); // Hours worked
        assertThat(numeric(row, 5)).isEqualTo(0); // Incomplete days

        XSSFSheet punches = workbook.getSheetAt(1);
        assertThat(punchRowsForEmployee(punches, employee.getName())).isEmpty();
    }

    @Test
    void exportActuals_employeePunchedWithNoRosterEntry_daysPresentExceedsDaysAssignedAndPunchesListed() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "Unscheduled");
        LocalDate date = LocalDate.of(2027, 9, 9);
        // No RosterEntry at all this month - the employee just showed up and punched.
        punch(employee, mgr, date, 9, 0, PunchDirection.IN);
        punch(employee, mgr, date, 17, 0, PunchDirection.OUT);

        XSSFWorkbook workbook = exportActualsWorkbook(2027, 9);
        XSSFSheet summary = workbook.getSheetAt(0);
        Row row = summary.getRow(rowIndexOfEmployee(summary, employee.getName()));

        assertThat(numeric(row, 1)).isEqualTo(0); // Days assigned
        assertThat(numeric(row, 3)).isEqualTo(1); // Days present - present, though never assigned
        assertThat(numeric(row, 3)).isGreaterThan(numeric(row, 1));
        assertThat(numeric(row, 4)).isEqualTo(8); // Hours worked

        XSSFSheet punches = workbook.getSheetAt(1);
        List<Row> rows = punchRowsForEmployee(punches, employee.getName());
        assertThat(rows).hasSize(2);
        assertThat(cellText(rows.get(0), 2)).isEqualTo("IN");
        assertThat(cellText(rows.get(1), 2)).isEqualTo("OUT");
        assertThat(cellText(rows.get(0), 4)).isEqualTo("MANUAL");

        // Same fact ("punches with no counts-as-worked roster entry") surfaces as an anomaly row too.
        XSSFSheet anomalies = workbook.getSheetAt(2);
        List<Row> anomalyRows = punchRowsForEmployee(anomalies, employee.getName());
        assertThat(anomalyRows).hasSize(1);
        assertThat(cellText(anomalyRows.get(0), 1)).isEqualTo(date.toString());
        assertThat(cellText(anomalyRows.get(0), 2)).isEqualTo("UNSCHEDULED");
    }

    /** An odd punch count - the day contributes nothing to worked minutes and the lone punch never pairs with a neighboring day's punch. */
    @Test
    void exportActuals_incompleteDay_countedCorrectlyAndLonePunchStillListedAlone() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "Incomplete");
        LocalDate incompleteDay = LocalDate.of(2027, 9, 10);
        LocalDate completeDay = LocalDate.of(2027, 9, 11);
        // A missed clock-out on one day...
        punch(employee, mgr, incompleteDay, 9, 0, PunchDirection.IN);
        // ...followed by an ordinary complete day - the lone punch above must not silently pair
        // with either of these.
        punch(employee, mgr, completeDay, 9, 0, PunchDirection.IN);
        punch(employee, mgr, completeDay, 17, 0, PunchDirection.OUT);

        XSSFWorkbook workbook = exportActualsWorkbook(2027, 9);
        XSSFSheet summary = workbook.getSheetAt(0);
        Row row = summary.getRow(rowIndexOfEmployee(summary, employee.getName()));

        assertThat(numeric(row, 3)).isEqualTo(2); // Days present - both days count, incomplete or not
        assertThat(numeric(row, 4)).isEqualTo(8); // Hours worked - only the complete day's pair
        assertThat(numeric(row, 5)).isEqualTo(1); // Incomplete days

        XSSFSheet punches = workbook.getSheetAt(1);
        List<Row> rows = punchRowsForEmployee(punches, employee.getName());
        assertThat(rows).hasSize(3);
        assertThat(cellText(rows.get(0), 1)).isEqualTo(incompleteDay.toString());
        assertThat(cellText(rows.get(0), 2)).isEqualTo("IN");

        // Neither day has a roster entry (none was created above), so both are UNSCHEDULED too -
        // the incomplete day carries both an UNSCHEDULED row and its own INCOMPLETE row.
        XSSFSheet anomalies = workbook.getSheetAt(2);
        List<Row> anomalyRows = punchRowsForEmployee(anomalies, employee.getName());
        assertThat(anomalyRows).hasSize(3);
        assertThat(anomalyRows.stream().filter(r -> incompleteDay.toString().equals(cellText(r, 1))).map(r -> cellText(r, 2)))
                .containsExactlyInAnyOrder("UNSCHEDULED", "INCOMPLETE");
        assertThat(anomalyRows.stream().filter(r -> completeDay.toString().equals(cellText(r, 1))).map(r -> cellText(r, 2)))
                .containsExactly("UNSCHEDULED");
    }

    @Test
    void exportActuals_lateAndAnomalies_splitShiftOnlyLateHalfFlagged() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "SplitLate");
        ShiftCode split = shiftCodeService.create(
                new ShiftCodeCreateInput("SPLITLATE" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.SPLIT, true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT)
                        .startTime1("09:00").endTime1("13:00")
                        .startTime2("16:00").endTime2("21:00"),
                mgr.getId());
        createdShiftCodeIds.add(split.getId());
        LocalDate date = LocalDate.of(2027, 11, 1);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), split.getId()), mgr.getId());

        // Morning half on time...
        punch(employee, mgr, date, 9, 0, PunchDirection.IN);
        punch(employee, mgr, date, 13, 0, PunchDirection.OUT);
        // ...evening half 12 minutes late.
        punch(employee, mgr, date, 16, 12, PunchDirection.IN);
        punch(employee, mgr, date, 21, 0, PunchDirection.OUT);

        XSSFSheet anomalies = exportActualsWorkbook(2027, 11).getSheetAt(2);
        List<Row> rows = punchRowsForEmployee(anomalies, employee.getName());

        assertThat(rows).hasSize(1);
        assertThat(cellText(rows.get(0), 2)).isEqualTo("LATE");
        assertThat(cellText(rows.get(0), 3)).contains("12 min late");
    }

    @Test
    void exportActuals_lateAndAnomalies_extraPunchPairBeyondScheduledIntervalsNotFlagged() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "ExtraPair");
        ShiftCode code = createCode(ShiftCodeKind.MORNING, true, "09:00", "17:00");
        LocalDate date = LocalDate.of(2027, 11, 2);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), code.getId()), mgr.getId());

        // The scheduled interval, matched exactly...
        punch(employee, mgr, date, 9, 0, PunchDirection.IN);
        punch(employee, mgr, date, 17, 0, PunchDirection.OUT);
        // ...plus an extra, unofficial in/out with nothing scheduled to compare it against.
        punch(employee, mgr, date, 18, 0, PunchDirection.IN);
        punch(employee, mgr, date, 19, 0, PunchDirection.OUT);

        XSSFSheet anomalies = exportActualsWorkbook(2027, 11).getSheetAt(2);
        List<Row> rows = punchRowsForEmployee(anomalies, employee.getName());

        assertThat(rows).isEmpty();
    }

    @Test
    void exportActuals_lateAndAnomalies_openScheduleDayWithNoPunches_missedNotLate() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "OpMissed");
        ShiftCode op = shiftCodeService.create(
                new ShiftCodeCreateInput("OPMISS" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.OPEN_SCHEDULE, true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT),
                mgr.getId());
        createdShiftCodeIds.add(op.getId());
        LocalDate date = LocalDate.of(2027, 11, 3);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), op.getId()), mgr.getId());

        XSSFSheet anomalies = exportActualsWorkbook(2027, 11).getSheetAt(2);
        List<Row> rows = punchRowsForEmployee(anomalies, employee.getName());

        assertThat(rows).hasSize(1);
        assertThat(cellText(rows.get(0), 2)).isEqualTo("MISSED");
    }

    @Test
    void exportActuals_lateAndAnomalies_cleanDayProducesNoRow() {
        UserEntity mgr = createEmployee(null, "Manager");
        UserEntity employee = createEmployee(StaffArea.RESTAURANT, "Clean");
        ShiftCode code = createCode(ShiftCodeKind.MORNING, true, "09:00", "17:00");
        LocalDate date = LocalDate.of(2027, 11, 4);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), code.getId()), mgr.getId());
        punch(employee, mgr, date, 9, 0, PunchDirection.IN);
        punch(employee, mgr, date, 17, 0, PunchDirection.OUT);

        XSSFSheet anomalies = exportActualsWorkbook(2027, 11).getSheetAt(2);
        List<Row> rows = punchRowsForEmployee(anomalies, employee.getName());

        assertThat(rows).isEmpty();
    }
}
