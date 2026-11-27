package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.mapper.DateText;
import com.sunsetbeach.model.BookingTrackerBucket;
import com.sunsetbeach.model.BookingTrackerChannelRank;
import com.sunsetbeach.model.BookingTrackerChannelRow;
import com.sunsetbeach.model.BookingTrackerMonth;
import com.sunsetbeach.model.BookingTrackerReport;
import com.sunsetbeach.model.BookingTrackerSnapshot;
import com.sunsetbeach.model.BookingTrackerWeek;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /reports/booking-tracker/export}: the same computation as the screen ({@link BookingTrackerService
 * #computation} - nothing is calculated here), laid out like the hotel's own workbook: Results, Channel summary,
 * Booking trends, Weekly review and Bookings. Values, not formulas. The five charts are native Excel charts (POI
 * XDDF) reading their own sheet's cells.
 */
@Service
public class BookingTrackerExportService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final BookingTrackerService trackerService;

    public BookingTrackerExportService(BookingTrackerService trackerService) {
        this.trackerService = trackerService;
    }

    /** The file name, {@code booking-tracker_<fromMonth>_<toMonth>_<today>.xlsx}. */
    public record Export(String filename, byte[] workbook) {}

    @Transactional(readOnly = true)
    public Export export(String fromMonth, String toMonth, String stayMonth) {
        BookingTrackerService.Computation c = trackerService.computation(fromMonth, toMonth, stayMonth);
        BookingTrackerReport report = c.report();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            writeResults(workbook, styles, c);
            writeChannels(workbook, styles, c);
            writeTrends(workbook, styles, c);
            writeWeeklyReview(workbook, styles, report);
            writeBookings(workbook, styles, c);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            SpreadsheetSafe.sanitize(workbook);
            workbook.write(out);
            String name = "booking-tracker_" + report.getFromMonth() + "_" + report.getToMonth() + "_" + c.today().format(FILE_DATE) + ".xlsx";
            return new Export(name, out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // --- Results -------------------------------------------------------------------------------

    private void writeResults(XSSFWorkbook workbook, Styles s, BookingTrackerService.Computation c) {
        XSSFSheet sheet = workbook.createSheet("Results");
        BookingTrackerReport report = c.report();
        List<BookingTrackerMonth> months = report.getMonths();
        int r = 0;
        r = title(sheet, s, r, "Booking tracker " + report.getFromMonth() + " to " + report.getToMonth());
        r = note(sheet, s, r, "As at " + DateText.date(report.getPositionDate()) + ". Counted bookings are CONFIRMED and PAID (no enquiries, cancellations or no-shows). "
                + "Gross includes VAT " + report.getVatRate() + "% and service charge " + report.getServiceChargeRate() + "% (deducted for this analysis only; nothing is charged).");
        r = note(sheet, s, r, "An early-departure fee has no night and is left out, as in the revenue statistic (Z410).");
        r++;

        r = header(sheet, s, r, "Results", "Available room nights", "Booked room nights", "Occupancy", "Net room revenue", "Breakfast revenue",
                "Net ADR", "Target room nights", "Still needed", "Pickup vs baseline");
        for (BookingTrackerMonth m : concat(months, report.getTotal())) {
            Row row = sheet.createRow(r++);
            boolean total = m.getMonth().get() == null;
            text(row, 0, total ? "Total" : m.getMonth().get(), total ? s.boldLabel : s.label);
            integer(row, 1, m.getAvailableRoomNights(), s);
            integer(row, 2, m.getBookedRoomNights(), s);
            percent(row, 3, m.getOccupancyPercent().get(), s);
            money(row, 4, m.getNetRoomRevenue(), s);
            money(row, 5, m.getBreakfastRevenue(), s);
            money(row, 6, m.getNetAdr().get(), s);
            integer(row, 7, m.getTargetRoomNights().get(), s);
            integer(row, 8, m.getStillNeededRoomNights().get(), s);
            integer(row, 9, m.getPickupVsBaseline().get(), s);
        }
        r++;

        r = header(sheet, s, r, "Breakdown", "Inclusive package (gross)", "Service charge", "VAT", "Breakfast", "Net room revenue");
        for (BookingTrackerMonth m : concat(months, report.getTotal())) {
            Row row = sheet.createRow(r++);
            boolean total = m.getMonth().get() == null;
            text(row, 0, total ? "Total" : m.getMonth().get(), total ? s.boldLabel : s.label);
            money(row, 1, m.getGrossRevenue(), s);
            money(row, 2, m.getServiceCharge(), s);
            money(row, 3, m.getVat(), s);
            money(row, 4, m.getBreakfastRevenue(), s);
            money(row, 5, m.getNetRoomRevenue(), s);
        }
        r++;

        r = header(sheet, s, r, "Inputs", "Physical rooms", "Nights in month", "Blocked room nights", "Baseline room nights", "Baseline net revenue", "Target room nights");
        for (BookingTrackerMonth m : concat(months, report.getTotal())) {
            Row row = sheet.createRow(r++);
            boolean total = m.getMonth().get() == null;
            text(row, 0, total ? "Total" : m.getMonth().get(), total ? s.boldLabel : s.label);
            integer(row, 1, m.getPhysicalRooms().get(), s);
            integer(row, 2, m.getNightsInMonth(), s);
            integer(row, 3, m.getBlockedRoomNights(), s);
            integer(row, 4, m.getBaselineRoomNights().get(), s);
            money(row, 5, m.getBaselineNetRevenue().get(), s);
            integer(row, 6, m.getTargetRoomNights().get(), s);
        }
        widths(sheet, 24, 18, 18, 14, 18, 18, 14, 18, 14, 18);
    }

    // --- Channel summary -----------------------------------------------------------------------

    private void writeChannels(XSSFWorkbook workbook, Styles s, BookingTrackerService.Computation c) {
        XSSFSheet sheet = workbook.createSheet("Channel summary");
        BookingTrackerReport report = c.report();
        int r = 0;
        r = title(sheet, s, r, "Channel summary - " + filterLabel(report));
        r++;
        r = header(sheet, s, r, "Channel", "Room nights", "Net room revenue", "Net ADR", "Room-night share", "Revenue share");
        int channelFirst = r;
        for (BookingTrackerChannelRow ch : report.getChannels().getRows()) {
            Row row = sheet.createRow(r++);
            text(row, 0, ReportService.channelLabel(ch.getChannel().get()), s.label);
            channelCells(row, ch, s);
        }
        int channelLast = r - 1;
        Row totalRow = sheet.createRow(r++);
        text(totalRow, 0, "Total", s.boldLabel);
        channelCells(totalRow, report.getChannels().getTotal(), s);
        r++;

        r = header(sheet, s, r, "Rank", "Channel", "Net room revenue", "Room nights");
        int rankFirst = r;
        for (BookingTrackerChannelRank rank : report.getChannels().getRanking()) {
            Row row = sheet.createRow(r++);
            integer(row, 0, rank.getRank(), s);
            text(row, 1, ReportService.channelLabel(rank.getChannel()), s.label);
            money(row, 2, rank.getNetRoomRevenue(), s);
            integer(row, 3, rank.getRoomNights(), s);
        }
        int rankLast = r - 1;
        widths(sheet, 18, 16, 18, 14, 18, 16);

        if (rankLast >= rankFirst) {
            barChart(sheet, "Net room revenue by channel (ranked)", new CellRangeAddress(rankFirst, rankLast, 1, 1),
                    new CellRangeAddress(rankFirst, rankLast, 2, 2), "Net room revenue", 7, 1, 15, 18);
        }
        barChart(sheet, "Room nights by channel", new CellRangeAddress(channelFirst, channelLast, 0, 0),
                new CellRangeAddress(channelFirst, channelLast, 1, 1), "Room nights", 7, 20, 15, 37);
    }

    private void channelCells(Row row, BookingTrackerChannelRow ch, Styles s) {
        integer(row, 1, ch.getRoomNights(), s);
        money(row, 2, ch.getNetRoomRevenue(), s);
        money(row, 3, ch.getNetAdr().get(), s);
        percent(row, 4, ch.getRoomNightSharePercent().get(), s);
        percent(row, 5, ch.getRevenueSharePercent().get(), s);
    }

    // --- Booking trends ------------------------------------------------------------------------

    private void writeTrends(XSSFWorkbook workbook, Styles s, BookingTrackerService.Computation c) {
        XSSFSheet sheet = workbook.createSheet("Booking trends");
        BookingTrackerReport report = c.report();
        int r = 0;
        r = title(sheet, s, r, "Booking trends - " + filterLabel(report));
        Row avg = sheet.createRow(r++);
        text(avg, 0, "Average booking window (days, by room nights)", s.label);
        decimal(avg, 1, report.getTrends().getAverageBookingWindowDays().get(), s);
        r++;

        r = header(sheet, s, r, "Booking window (days)", "Room nights", "Net room revenue", "Room-night share");
        int bucketFirst = r;
        for (BookingTrackerBucket b : report.getTrends().getBuckets()) {
            Row row = sheet.createRow(r++);
            text(row, 0, b.getLabel(), s.label);
            integer(row, 1, b.getRoomNights(), s);
            money(row, 2, b.getNetRoomRevenue(), s);
            percent(row, 3, b.getRoomNightSharePercent().get(), s);
        }
        int bucketLast = r - 1;
        r++;

        r = header(sheet, s, r, "Week starting (Monday)", "Room nights booked", "Net room revenue booked");
        int weekFirst = r;
        for (BookingTrackerWeek w : report.getTrends().getWeeks()) {
            Row row = sheet.createRow(r++);
            date(row, 0, w.getWeekStart(), s);
            integer(row, 1, w.getRoomNights(), s);
            money(row, 2, w.getNetRoomRevenue(), s);
        }
        int weekLast = r - 1;
        widths(sheet, 30, 18, 22, 18);

        barChart(sheet, "Room nights by booking window", new CellRangeAddress(bucketFirst, bucketLast, 0, 0),
                new CellRangeAddress(bucketFirst, bucketLast, 1, 1), "Room nights", 6, 1, 14, 18);
        lineChart(sheet, "Room nights booked per week", new CellRangeAddress(weekFirst, weekLast, 0, 0),
                new CellRangeAddress(weekFirst, weekLast, 1, 1), "Room nights", 6, 20, 14, 36);
        lineChart(sheet, "Net room revenue booked per week", new CellRangeAddress(weekFirst, weekLast, 0, 0),
                new CellRangeAddress(weekFirst, weekLast, 2, 2), "Net room revenue", 6, 38, 14, 54);
    }

    // --- Weekly review -------------------------------------------------------------------------

    private void writeWeeklyReview(XSSFWorkbook workbook, Styles s, BookingTrackerReport report) {
        XSSFSheet sheet = workbook.createSheet("Weekly review");
        int r = 0;
        r = title(sheet, s, r, "Weekly review - Friday snapshots " + report.getFromMonth() + " to " + report.getToMonth());
        r = note(sheet, s, r, "Figures are frozen as at the snapshot; pickup is the change against the previous snapshot of the same month.");
        r++;
        r = header(sheet, s, r, "Snapshot", "Stay month", "Late", "Room nights", "Net room revenue", "Occupancy", "Pickup room nights",
                "Pickup net revenue", "Reason / issue", "Next action", "Owner", "Due date");
        for (BookingTrackerSnapshot snap : report.getWeeklyReview()) {
            Row row = sheet.createRow(r++);
            date(row, 0, snap.getSnapshotDate(), s);
            text(row, 1, snap.getStayMonth(), s.label);
            text(row, 2, snap.getLate() ? "late" : "", s.label);
            integer(row, 3, snap.getRoomNights(), s);
            money(row, 4, snap.getNetRoomRevenue(), s);
            percent(row, 5, snap.getOccupancyPercent().get(), s);
            integer(row, 6, snap.getPickupRoomNights().get(), s);
            money(row, 7, snap.getPickupNetRevenue().get(), s);
            text(row, 8, snap.getReason().get(), s.label);
            text(row, 9, snap.getNextAction().get(), s.label);
            text(row, 10, snap.getOwner().get(), s.label);
            date(row, 11, snap.getDueDate().get(), s);
        }
        widths(sheet, 14, 12, 8, 14, 18, 12, 18, 18, 40, 40, 20, 14);
    }

    // --- Bookings ------------------------------------------------------------------------------

    private void writeBookings(XSSFWorkbook workbook, Styles s, BookingTrackerService.Computation c) {
        XSSFSheet sheet = workbook.createSheet("Bookings");
        List<YearMonth> months = c.period().months();
        List<String> headers = new java.util.ArrayList<>(List.of("Reference", "Guest name", "Booking date", "Arrival", "Departure", "Rooms", "Channel",
                "Room category", "Status", "Gross", "Breakfast guests", "Breakfast rate", "Nights", "Room nights", "Package", "Service charge",
                "VAT", "Breakfast", "Net room revenue", "Net ADR", "Booking window (days)"));
        for (YearMonth m : months) {
            String label = m.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + m.getYear();
            headers.add(label + " nights");
            headers.add(label + " gross");
            headers.add(label + " net");
        }
        int r = header(sheet, s, 0, headers.toArray(new String[0]));
        for (BookingTrackerService.BookingRow br : c.rows()) {
            BookingEntity b = br.booking();
            BookingTrackerService.Figures f = br.total(months);
            Row row = sheet.createRow(r++);
            text(row, 0, b.getExternalReference() != null ? b.getExternalReference() : b.getId(), s.label);
            text(row, 1, b.getGuestName(), s.label);
            date(row, 2, br.bookingDate(), s);
            date(row, 3, b.getCheckIn(), s);
            date(row, 4, b.getCheckOut(), s);
            integer(row, 5, 1, s);
            text(row, 6, ReportService.channelLabel(b.getChannel()), s.label);
            text(row, 7, br.roomName(), s.label);
            text(row, 8, b.getStatus().getValue(), s.label);
            money(row, 9, f.gross(), s);
            integer(row, 10, b.getBreakfastGuests(), s);
            integer(row, 11, b.getBreakfastRate(), s);
            integer(row, 12, (int) f.nights(), s);
            integer(row, 13, (int) f.nights(), s);
            money(row, 14, f.packageRevenue(), s);
            money(row, 15, f.serviceCharge(), s);
            money(row, 16, f.vat(), s);
            money(row, 17, f.breakfast(), s);
            money(row, 18, f.net(), s);
            money(row, 19, f.nights() == 0 ? null : f.net().divide(BigDecimal.valueOf(f.nights()), 12, RoundingMode.HALF_UP), s);
            integer(row, 20, (int) br.windowDays(), s);
            int col = 21;
            for (YearMonth m : months) {
                BookingTrackerService.Figures mf = br.months().get(m);
                if (mf != null) {
                    integer(row, col, (int) mf.nights(), s);
                    money(row, col + 1, mf.gross(), s);
                    money(row, col + 2, mf.net(), s);
                }
                col += 3;
            }
        }
        sheet.createFreezePane(2, 1);
        widths(sheet, 16, 26, 14, 14, 14, 8, 14, 26, 12, 14, 12, 12, 8, 12, 14, 14, 14, 14, 16, 14, 12);
    }

    // --- Charts --------------------------------------------------------------------------------

    private void barChart(XSSFSheet sheet, String title, CellRangeAddress categories, CellRangeAddress values, String seriesTitle,
                          int col1, int row1, int col2, int row2) {
        XSSFChart chart = newChart(sheet, title, col1, row1, col2, row2);
        XDDFCategoryAxis bottom = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis left = chart.createValueAxis(AxisPosition.LEFT);
        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottom, left);
        data.setBarDirection(BarDirection.COL);
        addSeries(sheet, data, categories, values, seriesTitle);
        chart.plot(data);
    }

    private void lineChart(XSSFSheet sheet, String title, CellRangeAddress categories, CellRangeAddress values, String seriesTitle,
                           int col1, int row1, int col2, int row2) {
        XSSFChart chart = newChart(sheet, title, col1, row1, col2, row2);
        XDDFCategoryAxis bottom = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis left = chart.createValueAxis(AxisPosition.LEFT);
        XDDFChartData data = chart.createData(ChartTypes.LINE, bottom, left);
        addSeries(sheet, data, categories, values, seriesTitle);
        chart.plot(data);
    }

    private static XSSFChart newChart(XSSFSheet sheet, String title, int col1, int row1, int col2, int row2) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);
        return chart;
    }

    private static void addSeries(XSSFSheet sheet, XDDFChartData data, CellRangeAddress categories, CellRangeAddress values, String seriesTitle) {
        XDDFDataSource<String> cats = XDDFDataSourcesFactory.fromStringCellRange(sheet, categories);
        XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromNumericCellRange(sheet, values);
        XDDFChartData.Series series = data.addSeries(cats, vals);
        series.setTitle(seriesTitle, null);
    }

    // --- Cells ---------------------------------------------------------------------------------

    private static String filterLabel(BookingTrackerReport report) {
        return report.getStayMonth().get() != null ? "stay month " + report.getStayMonth().get() : "all stays " + report.getFromMonth() + " to " + report.getToMonth();
    }

    private static <T> List<T> concat(List<T> list, T last) {
        List<T> all = new java.util.ArrayList<>(list);
        all.add(last);
        return all;
    }

    private static int title(XSSFSheet sheet, Styles s, int r, String text) {
        text(sheet.createRow(r), 0, text, s.title);
        return r + 1;
    }

    private static int note(XSSFSheet sheet, Styles s, int r, String text) {
        text(sheet.createRow(r), 0, text, s.note);
        return r + 1;
    }

    private static int header(XSSFSheet sheet, Styles s, int r, String... labels) {
        Row row = sheet.createRow(r);
        for (int i = 0; i < labels.length; i++) {
            text(row, i, labels[i], s.header);
        }
        return r + 1;
    }

    private static void text(Row row, int col, String value, CellStyle style) {
        if (value == null) return;
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void integer(Row row, int col, Integer value, Styles s) {
        if (value == null) return;
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(s.integer);
    }

    private static void money(Row row, int col, String value, Styles s) {
        if (value == null) return;
        money(row, col, new BigDecimal(value), s);
    }

    private static void money(Row row, int col, BigDecimal value, Styles s) {
        if (value == null) return;
        Cell cell = row.createCell(col);
        cell.setCellValue(value.setScale(2, RoundingMode.HALF_UP).doubleValue());
        cell.setCellStyle(s.money);
    }

    private static void decimal(Row row, int col, String value, Styles s) {
        if (value == null) return;
        Cell cell = row.createCell(col);
        cell.setCellValue(new BigDecimal(value).doubleValue());
        cell.setCellStyle(s.decimal);
    }

    /** The report carries percent (75.00); the cell holds the fraction (0.75) so Excel's own percent format shows it. */
    private static void percent(Row row, int col, String value, Styles s) {
        if (value == null) return;
        Cell cell = row.createCell(col);
        cell.setCellValue(new BigDecimal(value).movePointLeft(2).doubleValue());
        cell.setCellStyle(s.percent);
    }

    private static void date(Row row, int col, LocalDate value, Styles s) {
        if (value == null) return;
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(s.date);
    }

    private static void widths(XSSFSheet sheet, int... chars) {
        for (int i = 0; i < chars.length; i++) {
            sheet.setColumnWidth(i, chars[i] * 256);
        }
    }

    private static final class Styles {
        final CellStyle title;
        final CellStyle header;
        final CellStyle label;
        final CellStyle boldLabel;
        final CellStyle note;
        final CellStyle money;
        final CellStyle decimal;
        final CellStyle integer;
        final CellStyle percent;
        final CellStyle date;

        Styles(Workbook workbook) {
            DataFormat format = workbook.createDataFormat();
            XSSFFont bold = (XSSFFont) workbook.createFont();
            bold.setBold(true);
            XSSFFont big = (XSSFFont) workbook.createFont();
            big.setBold(true);
            big.setFontHeightInPoints((short) 14);
            XSSFFont italic = (XSSFFont) workbook.createFont();
            italic.setItalic(true);

            title = workbook.createCellStyle();
            title.setFont(big);
            header = workbook.createCellStyle();
            header.setFont(bold);
            header.setWrapText(true);
            header.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            label = workbook.createCellStyle();
            boldLabel = workbook.createCellStyle();
            boldLabel.setFont(bold);
            note = workbook.createCellStyle();
            note.setFont(italic);
            money = workbook.createCellStyle();
            money.setDataFormat(format.getFormat("#,##0.00 \"THB\""));
            decimal = workbook.createCellStyle();
            decimal.setDataFormat(format.getFormat("0.00"));
            integer = workbook.createCellStyle();
            integer.setDataFormat(format.getFormat("#,##0"));
            percent = workbook.createCellStyle();
            percent.setDataFormat(format.getFormat("0.00%"));
            date = workbook.createCellStyle();
            date.setDataFormat(format.getFormat("yyyy-mm-dd"));
        }
    }
}
