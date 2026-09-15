package com.sunsetbeach.rosterimport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Builds a small workbook shaped exactly the way {@link ScheduleWorkbookParser} expects (title
 * row, weekday row, day-number row, department/person rows), for tests that need to control
 * exactly which people/codes/fills appear - as opposed to {@link ScheduleWorkbookParserTests},
 * which deliberately tests against the hotel's own real file instead.
 */
public class ScheduleWorkbookBuilder {

    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final XSSFSheet sheet;
    private int nextRow = 3; // Excel row 4 - first department row
    private final List<Integer> dayColumns = new ArrayList<>();

    public ScheduleWorkbookBuilder(String sheetName, int... dayNumbers) {
        this.sheet = workbook.createSheet(sheetName);
        XSSFRow dayRow = sheet.createRow(2); // Excel row 3
        int col = 3; // column D
        for (int day : dayNumbers) {
            dayRow.createCell(col).setCellValue(day);
            dayColumns.add(col);
            col++;
        }
    }

    public ScheduleWorkbookBuilder department(String label) {
        XSSFRow row = sheet.createRow(nextRow++);
        row.createCell(1).setCellValue(label); // column B
        return this;
    }

    /** codes.length must match the day-number count this builder was constructed with; null means a day off. */
    public ScheduleWorkbookBuilder person(String name, String... codes) {
        XSSFRow row = sheet.createRow(nextRow++);
        row.createCell(2).setCellValue(name); // column C
        for (int i = 0; i < codes.length; i++) {
            if (codes[i] == null) {
                continue;
            }
            XSSFCell cell = row.createCell(dayColumns.get(i));
            setNumericOrString(cell, codes[i]);
        }
        return this;
    }

    /** The one place a "9" cell's colour needs setting explicitly - everything else in this file is read by text alone. */
    public ScheduleWorkbookBuilder nineFill(String name, int dayIndex, boolean yellow) {
        XSSFRow row = findPersonRow(name);
        XSSFCell cell = row.getCell(dayColumns.get(dayIndex));
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        if (yellow) {
            style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        } else {
            XSSFColor themeColor = new XSSFColor();
            themeColor.setTheme(7);
            themeColor.setTint(0.8);
            style.setFillForegroundColor(themeColor);
        }
        cell.setCellStyle(style);
        return this;
    }

    /** Sets a fill that matches neither known colour - for the "stop and report" test. */
    public ScheduleWorkbookBuilder unrecognisedFill(String name, int dayIndex) {
        XSSFRow row = findPersonRow(name);
        XSSFCell cell = row.getCell(dayColumns.get(dayIndex));
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        cell.setCellStyle(style);
        return this;
    }

    public ScheduleWorkbookBuilder stopMarker() {
        XSSFRow row = sheet.createRow(nextRow++);
        row.createCell(1).setCellValue("TOTAL STAFFS");
        return this;
    }

    public InputStream build() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private XSSFRow findPersonRow(String name) {
        for (int r = 3; r < nextRow; r++) {
            XSSFRow row = sheet.getRow(r);
            if (row != null) {
                XSSFCell c = row.getCell(2);
                if (c != null && name.equals(c.getStringCellValue())) {
                    return row;
                }
            }
        }
        throw new IllegalStateException("No such person row: " + name);
    }

    private static void setNumericOrString(XSSFCell cell, String code) {
        try {
            cell.setCellValue(Double.parseDouble(code));
        } catch (NumberFormatException e) {
            cell.setCellValue(code);
        }
    }

}
