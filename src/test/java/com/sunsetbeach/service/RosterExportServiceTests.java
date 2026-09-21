package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
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
    private RosterEntryRepository rosterEntryRepository;

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdShiftCodeIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
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
}
