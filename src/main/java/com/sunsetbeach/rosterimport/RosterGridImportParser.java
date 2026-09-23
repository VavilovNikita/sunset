package com.sunsetbeach.rosterimport;

import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Reads back a file previously produced by {@code RosterExportService#exportGrid} - this app's own
 * data, not the hotel's hand-built schedule ({@link ScheduleWorkbookParser} is that one, a
 * completely different file shape). See {@link RosterGridImportFormat}'s own javadoc for the exact
 * hidden layout this reads: per-row/per-cell facts as hidden companion columns on the visible sheet
 * itself (survives a hand-inserted or hand-deleted employee row, since Excel moves a row's cells
 * together), whole-file facts (export timestamp, the shift-code dictionary) on a separate hidden
 * sheet.
 *
 * <p>Deliberately does no resolution of its own - unlike {@link ScheduleWorkbookParser}, which
 * resolves names/codes as it goes, this class only ever reports what's literally on the two
 * sheets. Deciding whether a row/cell's metadata is still trustworthy (see {@link
 * ParsedGridImportCell}'s own javadoc on why a stale hidden id must never be trusted blindly) is
 * {@code RosterGridImportService}'s job, since it needs the current DB state to make that call.
 */
@Component
public class RosterGridImportParser {

    /** Column B text this class recognises as "end of the person grid" - see RosterExportService#writeTotalsRow's own labels; never a department or an employee. */
    private static final java.util.Set<String> TOTALS_ROW_LABELS = java.util.Set.of("Working", "Off", "Absent");

    private static final int FIRST_GRID_ROW = 2; // Excel row 3 - row 0 is the day-number header, row 1 the weekday row.

    public ParsedGridImport parse(InputStream xlsx, int year, int month) {
        String sheetName = RosterGridImportFormat.visibleSheetName(year, month);
        try (Workbook workbook = new XSSFWorkbook(xlsx)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new ScheduleParseException("No sheet named \"" + sheetName + "\" in this file - was it exported for a different month?");
            }
            Sheet metaSheet = workbook.getSheet(RosterGridImportFormat.METADATA_SHEET_NAME);
            if (metaSheet == null) {
                throw new ScheduleParseException(
                        "This file has no \"" + RosterGridImportFormat.METADATA_SHEET_NAME + "\" sheet - it wasn't produced by this app's own "
                                + "roster export, or predates this feature. Re-export the month and try again.");
            }

            int days = YearMonth.of(year, month).lengthOfMonth();
            Instant exportedAt = readExportedAt(metaSheet);
            Map<String, ShiftCodeSnapshot> snapshots = readShiftCodeSnapshots(metaSheet);
            return parseGrid(sheet, YearMonth.of(year, month), days, exportedAt, snapshots);
        } catch (IOException e) {
            throw new ScheduleParseException("Could not read this file as an Excel workbook (" + e.getMessage() + ")");
        }
    }

    private ParsedGridImport parseGrid(Sheet sheet, YearMonth ym, int days, Instant exportedAt, Map<String, ShiftCodeSnapshot> snapshots) {
        List<ParsedGridImportRow> rows = new ArrayList<>();
        List<ParsedGridImportCell> cells = new ArrayList<>();

        int employeeIdCol = RosterGridImportFormat.employeeIdColumn(days);
        java.util.Set<Integer> groupHeaderRows = new java.util.HashSet<>();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstColumn() == 0 && region.getLastColumn() >= days) {
                groupHeaderRows.add(region.getFirstRow());
            }
        }

        int lastRow = sheet.getLastRowNum();
        for (int r = FIRST_GRID_ROW; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String nameText = textOrNull(row.getCell(0));
            if (nameText == null) {
                continue; // blank separator row
            }
            if (TOTALS_ROW_LABELS.contains(nameText)) {
                break; // totals block reached - nothing after this is grid content
            }
            if (groupHeaderRows.contains(r)) {
                continue; // a department header, not a person
            }

            String metadataEmployeeUserId = textOrNull(row.getCell(employeeIdCol));
            rows.add(new ParsedGridImportRow(r, nameText, metadataEmployeeUserId));

            for (int day = 1; day <= days; day++) {
                String rawCode = textOrNull(row.getCell(day));
                if (rawCode == null) {
                    continue; // a day off - no cell to read, same convention as the hand-built importer
                }
                String metadataShiftCodeId = textOrNull(row.getCell(RosterGridImportFormat.shiftCodeIdColumn(days, day)));
                cells.add(new ParsedGridImportCell(r, day, ym.atDay(day), rawCode, metadataShiftCodeId));
            }
        }

        return new ParsedGridImport(exportedAt, snapshots, rows, cells);
    }

    private Instant readExportedAt(Sheet metaSheet) {
        Row row = metaSheet.getRow(0);
        String label = row != null ? textOrNull(row.getCell(0)) : null;
        String value = row != null ? textOrNull(row.getCell(1)) : null;
        if (!RosterGridImportFormat.EXPORTED_AT_LABEL.equals(label) || value == null) {
            throw new ScheduleParseException("This file's import-metadata sheet is malformed (missing its own export timestamp) - re-export the month and try again.");
        }
        try {
            return Instant.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ScheduleParseException("This file's export timestamp is unreadable - re-export the month and try again.");
        }
    }

    private Map<String, ShiftCodeSnapshot> readShiftCodeSnapshots(Sheet metaSheet) {
        int headerRow = -1;
        int lastRow = metaSheet.getLastRowNum();
        for (int r = 0; r <= lastRow; r++) {
            Row row = metaSheet.getRow(r);
            if (row != null && RosterGridImportFormat.SHIFT_CODES_BLOCK_LABEL.equals(textOrNull(row.getCell(0)))) {
                headerRow = r;
                break;
            }
        }
        if (headerRow == -1) {
            throw new ScheduleParseException("This file's import-metadata sheet is malformed (missing its shift-code dictionary) - re-export the month and try again.");
        }

        Map<String, ShiftCodeSnapshot> snapshots = new LinkedHashMap<>();
        for (int r = headerRow + 1; r <= lastRow; r++) {
            Row row = metaSheet.getRow(r);
            String id = row != null ? textOrNull(row.getCell(0)) : null;
            if (id == null) {
                break; // blank row - end of this block
            }
            String staffAreaText = textOrNull(row.getCell(2));
            String kindText = textOrNull(row.getCell(3));
            snapshots.put(id, new ShiftCodeSnapshot(
                    id, textOrNull(row.getCell(1)), staffAreaText != null ? StaffArea.fromValue(staffAreaText) : null,
                    kindText != null ? ShiftCodeKind.fromValue(kindText) : null, textOrNull(row.getCell(4)), textOrNull(row.getCell(5)),
                    textOrNull(row.getCell(6)), textOrNull(row.getCell(7)), row.getCell(8) != null && row.getCell(8).getBooleanCellValue(),
                    row.getCell(9) != null && row.getCell(9).getBooleanCellValue(), textOrNull(row.getCell(10)), textOrNull(row.getCell(11))));
        }
        return snapshots;
    }

    private static String textOrNull(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case BLANK -> null;
            case STRING -> {
                String s = cell.getStringCellValue();
                yield (s == null || s.isBlank()) ? null : s.trim();
            }
            case NUMERIC -> formatNumeric(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> throw new ScheduleParseException("Unexpected cell content at " + cell.getAddress() + " (" + cell.getCellType() + ")");
        };
    }

    /** Same round-trip-safe numeric formatting as ScheduleWorkbookParser's own formatNumeric - a code like "8.3" must read back exactly as typed. */
    private static String formatNumeric(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
