package com.sunsetbeach.service;

import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.RosterEmployee;
import com.sunsetbeach.model.RosterEntry;
import com.sunsetbeach.model.RosterMonth;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * {@code GET /roster/export} - an .xlsx mirror of the on-screen grid ({@code RosterGrid.tsx}) for
 * one month, ADMIN only (same floor as the Excel *import*, not the CSV actuals export - see this
 * endpoint's own openapi.yaml description for why). Deliberately re-derives {@link RosterService
 * #getMonth} rather than reading a stored grid: there is no cached "the grid" anywhere, the React
 * component computes what's on screen straight from {@code RosterMonth} + a client-side reducer
 * every render, and this does the same in Java, row for row.
 *
 * <p>Grouping/order, per-day totals (Working/Off/Absent), and cell colouring (kind-shape cues
 * layered under a code's own {@code displayColor}, falling back to the grid's own
 * lightness-by-start-time neutral scheme when unset) are all reimplemented here rather than
 * shared with the frontend, because none of it exists as backend code today - {@code
 * RosterGrid.tsx}'s own totals block and colour scheme are the only place this logic lived before
 * this endpoint. Kept in careful lockstep with that file's own comments (see the constants below)
 * rather than factored into a shared module, since there is no shared module between a Next.js
 * frontend and this Java backend to put one in.
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

    private final RosterService rosterService;
    private final AuditLogService auditLogService;

    public RosterExportService(RosterService rosterService, AuditLogService auditLogService) {
        this.rosterService = rosterService;
        this.auditLogService = auditLogService;
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
