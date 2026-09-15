package com.sunsetbeach.rosterimport;

import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.StaffArea;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Reads one month's sheet from the hotel's own hand-built schedule workbook - see this class's
 * own field constants for the exact shape the file is always in (one sheet per month, named like
 * {@code Sep26}; a title row, a weekday-name row, a day-number row, then department header rows
 * each followed by one row per person). Deliberately narrow: only the grid of codes (person row
 * against day column) is ever read. The trailing columns after the last day column, the legend,
 * the totals rows, and the notes block are never touched - their position shifts between months
 * (one month even has an extra trailing column the others don't), so any parser that locates them
 * by counting from either end of the sheet is wrong for at least one real month. This class
 * instead only ever reads forward from a known starting point (row 4, column D) until it
 * recognises a stopping label ({@link #TOTALS_STOP_LABELS}) - everything past that point simply
 * never gets visited, regardless of what it contains.
 *
 * <p><b>The one place a cell's fill matters.</b> Two different shifts are both written as the
 * bare text {@code "9"} in this file - a single 09:00-18:00 shift (filled yellow) and a split one
 * (filled light blue, the workbook's theme colour "Accent 4, Lighter 80%"). Every other code is
 * read by its text alone; fill is read for {@code "9"} and nothing else. A {@code "9"} whose fill
 * matches neither known colour becomes a {@link ParseIssue}, never a guess - see {@link
 * #detectNineFill}.
 *
 * <p>Cell comments ({@code "half day"}, {@code "do not move"}) are deliberately never read. {@code
 * "do not move"} looks, from {@code RosterEntry#locked}'s own openapi.yaml description, like it
 * should map onto that flag - but in this hotel's own real files, every occurrence of it sits on
 * a blank (day-off) cell, not a coded one, and a day off has no {@code RosterEntry} for a lock to
 * attach to at all (see that entity's own "absence of a row" convention). There is no reliable,
 * general mapping from a comment to a specific entry to be found here, so none is attempted -
 * comments are ignored entirely rather than read for the cells where it would happen to work and
 * silently dropped for the cells where it wouldn't.
 */
@Component
public class ScheduleWorkbookParser {

    // --- Fixed shape of this file - see this class's own javadoc ---
    private static final int DAY_NUMBER_ROW = 2; // Excel row 3
    private static final int FIRST_DEPARTMENT_ROW = 3; // Excel row 4
    private static final int DEPARTMENT_COLUMN = 1; // Excel column B
    private static final int NAME_COLUMN = 2; // Excel column C
    private static final int FIRST_DAY_COLUMN = 3; // Excel column D

    /** Column B text that ends the person grid for that sheet - everything at or after one of these rows is a total, never a department. */
    private static final java.util.Set<String> TOTALS_STOP_LABELS =
            java.util.Set.of("inhouse", "total staffs", "total off", "total ph/al", "total working");

    private static final Map<String, StaffArea> DEPARTMENT_LABELS = Map.of(
            "admin", StaffArea.ADMIN,
            "front office", StaffArea.FRONT_OFFICE,
            "maintenance", StaffArea.MAINTENANCE,
            "house keeping", StaffArea.HOUSEKEEPING,
            "restaurant", StaffArea.RESTAURANT,
            "kitchen", StaffArea.KITCHEN);

    /** Pure ARGB match - the workbook's own "Yellow" standard colour, fill applied with no tint. */
    private static final String YELLOW_ARGB = "FFFFFF00";
    /** Theme index for "Accent 4" in this workbook's theme (0-indexed: dk1,lt1,dk2,lt2,accent1..6) - see this class's own javadoc. */
    private static final int BLUE_THEME_INDEX = 7;
    /** A generous lower bound on "lightened", not an exact match on the observed ~0.8 - excludes an unlightened or darkened Accent 4, which would not read as light blue. */
    private static final double BLUE_MIN_TINT = 0.5;

    public ParsedSchedule parse(InputStream xlsx, int year, int month) {
        String sheetName = expectedSheetName(year, month);
        try (Workbook workbook = new XSSFWorkbook(xlsx)) {
            Sheet sheet = findSheet(workbook, sheetName);
            return parseSheet(sheet, YearMonth.of(year, month));
        } catch (IOException e) {
            throw new ScheduleParseException("Could not read this file as an Excel workbook (" + e.getMessage() + ")");
        }
    }

    private static String expectedSheetName(int year, int month) {
        String[] abbrev = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };
        return abbrev[month - 1] + String.format("%02d", year % 100);
    }

    private static Sheet findSheet(Workbook workbook, String expectedName) {
        Sheet exact = workbook.getSheet(expectedName);
        if (exact != null) {
            return exact;
        }
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getSheetName().equalsIgnoreCase(expectedName)) {
                return sheet;
            }
        }
        List<String> available = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            available.add(workbook.getSheetAt(i).getSheetName());
        }
        throw new ScheduleParseException("No sheet named \"" + expectedName + "\" in this file - sheets found: " + available);
    }

    private ParsedSchedule parseSheet(Sheet sheet, YearMonth yearMonth) {
        int lastDayColumn = findLastDayColumn(sheet);

        List<ParsedCell> cells = new ArrayList<>();
        List<ParseIssue> issues = new ArrayList<>();
        StaffArea currentArea = null;

        int lastRow = sheet.getLastRowNum();
        for (int r = FIRST_DEPARTMENT_ROW; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String deptText = textOrNull(row.getCell(DEPARTMENT_COLUMN));
            String nameText = textOrNull(row.getCell(NAME_COLUMN));

            if (deptText != null) {
                String normalized = normalizeLabel(deptText);
                if (TOTALS_STOP_LABELS.contains(normalized)) {
                    break; // end of the person grid for this sheet - legend/notes follow, never read
                }
                StaffArea area = DEPARTMENT_LABELS.get(normalized);
                if (area == null) {
                    issues.add(new ParseIssue(cellRef(r, DEPARTMENT_COLUMN), "Unrecognised department header: \"" + deptText.trim() + "\""));
                    currentArea = null;
                } else {
                    currentArea = area;
                }
                continue;
            }
            if (nameText == null) {
                continue; // a blank separator row
            }

            String rawName = normalizeName(nameText);
            for (int c = FIRST_DAY_COLUMN; c <= lastDayColumn; c++) {
                Cell codeCell = row.getCell(c);
                String rawCode = textOrNull(codeCell);
                if (rawCode == null) {
                    continue; // a day off - no cell to read
                }
                String ref = cellRef(r, c);
                if (currentArea == null) {
                    issues.add(new ParseIssue(ref, "\"" + rawName + "\" has no recognised department above this row"));
                    continue;
                }

                FillColor fillColor = null;
                if ("9".equals(rawCode)) {
                    fillColor = detectNineFill(codeCell);
                    if (fillColor == null) {
                        issues.add(new ParseIssue(
                                ref, "\"9\" at " + ref + " has neither the yellow nor the light-blue fill this file uses to tell its two 9 "
                                        + "shifts apart - refusing to guess which one this is"));
                        continue;
                    }
                }

                int dayNumber = c - FIRST_DAY_COLUMN + 1;
                cells.add(new ParsedCell(rawName, currentArea, yearMonth.atDay(dayNumber), rawCode, fillColor, ref));
            }
        }

        return new ParsedSchedule(cells, issues);
    }

    /**
     * Row 3 (0-indexed 2), starting at column D - the day-number header every month has, per this
     * class's own javadoc. Consecutive integers from 1; the first column that breaks the sequence
     * is the last real day column, and everything from there on is a trailing column this class
     * never reads (see the javadoc for why that boundary can't be found any other way).
     */
    private static int findLastDayColumn(Sheet sheet) {
        Row headerRow = sheet.getRow(DAY_NUMBER_ROW);
        if (headerRow == null) {
            throw new ScheduleParseException("Row " + (DAY_NUMBER_ROW + 1) + " (the day-number header) is empty");
        }
        Cell first = headerRow.getCell(FIRST_DAY_COLUMN);
        if (first == null || first.getCellType() != CellType.NUMERIC || first.getNumericCellValue() != 1) {
            throw new ScheduleParseException(
                    "Expected day 1 in " + cellRef(DAY_NUMBER_ROW, FIRST_DAY_COLUMN) + " - this file isn't shaped the way this importer expects");
        }
        int col = FIRST_DAY_COLUMN;
        int expected = 1;
        while (true) {
            Cell c = headerRow.getCell(col);
            if (c == null || c.getCellType() != CellType.NUMERIC || (int) c.getNumericCellValue() != expected) {
                break;
            }
            col++;
            expected++;
        }
        return col - 1;
    }

    /** Yellow (pure ARGB) or light blue (this workbook's own Accent 4 theme colour, lightened) - anything else is unrecognised. See this class's own javadoc. */
    private static FillColor detectNineFill(Cell cell) {
        if (cell.getCellStyle() == null) {
            return null;
        }
        Object fg = cell.getCellStyle().getFillForegroundColorColor();
        if (!(fg instanceof XSSFColor color)) {
            return null;
        }
        if (color.isThemed()) {
            if (color.getTheme() == BLUE_THEME_INDEX && color.getTint() > BLUE_MIN_TINT) {
                return FillColor.BLUE;
            }
            return null;
        }
        return YELLOW_ARGB.equals(color.getARGBHex()) ? FillColor.YELLOW : null;
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
            default -> throw new ScheduleParseException(
                    "Unexpected cell content at " + cell.getAddress() + " (" + cell.getCellType() + ") - expected a code or a blank cell");
        };
    }

    /** BigDecimal.valueOf(double) round-trips via Double.toString(), so 8.3 stays "8.3" - no float noise - and stripTrailingZeros + toPlainString turns 7.0 into "7", never scientific notation. */
    private static String formatNumeric(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String normalizeLabel(String text) {
        String trimmed = text.trim();
        if (trimmed.endsWith(".")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** Trim and collapse internal whitespace only - never a fuzzy match, see this class's own javadoc and RosterImportService's. */
    private static String normalizeName(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }

    private static String cellRef(int row, int col) {
        return new CellReference(row, col).formatAsString();
    }
}
