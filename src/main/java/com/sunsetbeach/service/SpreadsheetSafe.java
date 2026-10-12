package com.sunsetbeach.service;

import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Spreadsheet formula injection, closed in one place for every export (bookings and shift CSV,
 * revenue workbook, roster and attendance workbooks, anything added later). A cell whose text
 * starts with {@code =}, {@code +}, {@code -}, {@code @}, a tab or a carriage return is run as a
 * formula by Excel/LibreOffice/Sheets, and a guest's name or a menu item is text a stranger chose
 * (the public booking form takes any name). Such a cell gets a leading {@code '}, which the
 * spreadsheet shows as a plain-text marker and never evaluates.
 *
 * <p>One exemption: a value that is only a number ({@code -50.00}, {@code +66812345678}) is left
 * alone. It cannot be a formula, and the sign is data - a discrepancy or an amount that came out
 * negative would otherwise stop being a number in the export.
 *
 * <p>{@link #sanitize} is the workbook-wide form (call it once before {@code write}); {@link
 * #unescape} undoes it for this app's own re-import of its roster export, so a round trip returns
 * the text that was exported.
 */
public final class SpreadsheetSafe {

    private static final Pattern PLAIN_NUMBER = Pattern.compile("[-+]?[0-9]+([.,][0-9]+)?");

    private SpreadsheetSafe() {}

    /** {@code value} as a cell's text: unchanged unless a spreadsheet would read it as a formula. */
    public static String text(String value) {
        if (value == null || value.isEmpty() || !startsLikeFormula(value) || PLAIN_NUMBER.matcher(value).matches()) {
            return value;
        }
        return "'" + value;
    }

    /** What {@link #text} turned into {@code stored}, back to the original. A text that merely starts with {@code '} is left as it is. */
    public static String unescape(String stored) {
        if (stored != null && stored.length() > 1 && stored.charAt(0) == '\'' && startsLikeFormula(stored.substring(1))) {
            return stored.substring(1);
        }
        return stored;
    }

    /** Makes every text cell of every sheet safe, in place. Numbers, booleans and blanks are not text and are not touched. */
    public static void sanitize(Workbook workbook) {
        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        String value = cell.getStringCellValue();
                        String safe = text(value);
                        if (safe != value) {
                            cell.setCellValue(safe);
                        }
                    }
                }
            }
        }
    }

    private static boolean startsLikeFormula(String value) {
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r';
    }
}
