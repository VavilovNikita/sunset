package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.FolioPaymentEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.FolioPaymentMethod;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.FolioPaymentRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.TableRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /reports/revenue-export} - money actually collected in a date range, for the hotel's
 * accountant. See that endpoint's own openapi.yaml description for the full contract; the two
 * decisions that shape everything here:
 *
 * <p><b>Two channels, never three.</b> POS payments received ({@link PaymentAggregation.Totals#receivedTotal()},
 * which already leaves {@code ROOM_CHARGE} out) plus {@link FolioPaymentEntity} settlements. A
 * room-charged order therefore counts exactly once, when its folio payment is collected - never
 * when the order is closed to the room. The room rate itself is in neither: it has no payment
 * record anywhere in this system (see {@link BookingService#computeOutstandingBalance}'s javadoc),
 * only {@code Booking.status = PAID}. The "Paid bookings (info)" sheet shows those bookings for
 * reference and is deliberately kept out of every total, since it says nothing about when, how, or
 * how much was actually collected.
 *
 * <p><b>Days are the hotel's own.</b> {@code Payment}/{@code FolioPayment.createdAt} are
 * {@code @CreationTimestamp} values - UTC wall-clock, the same reading {@link TimestampFormat#toUtc}
 * gives them everywhere else in this API. Both the query window and each row's day are converted
 * through the injected {@link Clock}'s zone, so a bar tab paid at 01:30 Bangkok lands on that
 * Bangkok date. {@link PaymentService#getSummary} does not convert, which is why the two can differ
 * at the edges of a range - documented on the endpoint rather than silently changed there.
 */
@Service
public class RevenueExportService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final PaymentRepository paymentRepository;
    private final FolioPaymentRepository folioPaymentRepository;
    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public RevenueExportService(
            PaymentRepository paymentRepository,
            FolioPaymentRepository folioPaymentRepository,
            OrderRepository orderRepository,
            TableRepository tableRepository,
            BookingRepository bookingRepository,
            AuditLogService auditLogService,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.folioPaymentRepository = folioPaymentRepository;
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.bookingRepository = bookingRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public byte[] export(String from, String to) {
        LocalDate fromDate = parseDate("from", from);
        LocalDate toDate = parseDate("to", to);
        if (fromDate.isAfter(toDate)) {
            throw ValidationException.field("to", "must be on or after from");
        }

        ZoneId zone = clock.getZone();
        LocalDateTime startUtc = fromDate.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endUtc = toDate.plusDays(1).atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        List<PaymentEntity> payments = paymentRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startUtc, endUtc).stream()
                .sorted(Comparator.comparing(PaymentEntity::getCreatedAt))
                .toList();
        List<FolioPaymentEntity> folioPayments = folioPaymentRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startUtc, endUtc).stream()
                .sorted(Comparator.comparing(FolioPaymentEntity::getCreatedAt))
                .toList();
        List<BookingEntity> paidBookings = bookingRepository.findByStatusAndCheckOutBetween(BookingStatus.PAID, fromDate, toDate).stream()
                .sorted(Comparator.comparing(BookingEntity::getCheckOut).thenComparing(BookingEntity::getGuestName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<String, OrderEntity> orders = byId(orderRepository.findAllById(distinct(payments, PaymentEntity::getOrderId)), OrderEntity::getId);
        Map<String, TableEntity> tables = byId(
                tableRepository.findAllById(orders.values().stream().map(OrderEntity::getTableId).filter(Objects::nonNull).distinct().toList()),
                TableEntity::getId);
        Map<String, BookingEntity> folioBookings = byId(bookingRepository.findAllById(distinct(folioPayments, FolioPaymentEntity::getBookingId)), BookingEntity::getId);

        Map<LocalDate, DayTotals> days = new TreeMap<>();
        for (LocalDate d = fromDate; !d.isAfter(toDate); d = d.plusDays(1)) {
            days.put(d, new DayTotals());
        }
        payments.forEach(p -> days.get(localDate(p.getCreatedAt())).pos.add(p));
        folioPayments.forEach(f -> days.get(localDate(f.getCreatedAt())).folio.add(f));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            BigDecimal grandTotal = writeSummarySheet(workbook, styles, days);
            writePosPaymentsSheet(workbook, styles, payments, orders, tables);
            writeFolioPaymentsSheet(workbook, styles, folioPayments, folioBookings);
            writePaidBookingsSheet(workbook, styles, paidBookings);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            auditLogService.record(
                    AuditAction.REVENUE_EXPORTED, AuditEntityType.PAYMENT, null,
                    "Exported revenue for " + fromDate + " to " + toDate + ": ฿" + PriceFormat.asDecimalString(grandTotal) + " collected ("
                            + payments.size() + " POS payment(s), " + folioPayments.size() + " room charge settlement(s))");

            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The openapi {@code pattern} only guarantees the shape - {@code 2026-02-30} passes it, and
     * an unhandled {@link DateTimeParseException} would surface as a 500, not the documented 400.
     */
    private static LocalDate parseDate(String field, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ValidationException.field(field, "must be a valid date (YYYY-MM-DD)");
        }
    }

    private LocalDateTime localDateTime(LocalDateTime createdAtUtc) {
        return TimestampFormat.toUtc(createdAtUtc).atZoneSameInstant(clock.getZone()).toLocalDateTime();
    }

    private LocalDate localDate(LocalDateTime createdAtUtc) {
        return localDateTime(createdAtUtc).toLocalDate();
    }

    private static <T> List<String> distinct(List<T> rows, Function<T, String> key) {
        return rows.stream().map(key).filter(Objects::nonNull).distinct().toList();
    }

    private static <T> Map<String, T> byId(List<T> rows, Function<T, String> id) {
        return rows.stream().collect(Collectors.toMap(id, r -> r));
    }

    // --- Sheet 1: Summary ---------------------------------------------------------------------

    private static final String[] SUMMARY_COLUMNS = {
        "Date",
        "POS cash", "POS card", "POS other", "POS received",
        "Room charges settled - cash", "Room charges settled - card", "Room charges settled - other", "Room charges settled",
        "Total collected",
        "POS charged to rooms (not collected)"
    };

    /** Returns the period's grand total (POS received + room charges settled), for the audit entry. */
    private BigDecimal writeSummarySheet(XSSFWorkbook workbook, Styles styles, Map<LocalDate, DayTotals> days) {
        XSSFSheet sheet = workbook.createSheet("Summary");
        writeHeader(sheet, 0, SUMMARY_COLUMNS, styles.header);

        int rowIndex = 1;
        DayTotals period = new DayTotals();
        for (Map.Entry<LocalDate, DayTotals> day : days.entrySet()) {
            writeSummaryRow(sheet.createRow(rowIndex++), day.getKey().toString(), day.getValue(), styles.amount, null);
            period.pos.addAll(day.getValue().pos);
            period.folio.addAll(day.getValue().folio);
        }
        writeSummaryRow(sheet.createRow(rowIndex++), "Total", period, styles.boldAmount, styles.header);

        rowIndex++;
        String[] notes = {
            "Days are Asia/Bangkok calendar days.",
            "POS received = POS payments by cash, card or other. POS charged to rooms is money moved onto a guest's room folio when an order was closed to the room - nothing was collected, so it is in no total.",
            "Room charges settled = money collected at the desk against room-charged POS orders (folio payments). This is where a room-charged order's money is counted, once, when it is collected.",
            "Room-rate revenue is NOT included anywhere in this workbook's totals: the system keeps no payment record for the room itself, only a booking's PAID status. See the \"Paid bookings (info)\" sheet.",
            "No tax breakdown - all amounts are gross, as received.",
        };
        for (String note : notes) {
            Cell cell = sheet.createRow(rowIndex++).createCell(0);
            cell.setCellValue(note);
            cell.setCellStyle(styles.note);
        }

        sheet.setColumnWidth(0, 12 * 256);
        for (int i = 1; i < SUMMARY_COLUMNS.length; i++) {
            sheet.setColumnWidth(i, 16 * 256);
        }
        sheet.createFreezePane(1, 1);
        return period.totalCollected();
    }

    private void writeSummaryRow(Row row, String label, DayTotals totals, XSSFCellStyle amountStyle, XSSFCellStyle labelStyle) {
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) labelCell.setCellStyle(labelStyle);

        PaymentAggregation.Totals pos = totals.posTotals();
        BigDecimal[] values = {
            pos.cash(), pos.card(), pos.other(), pos.receivedTotal(),
            totals.folio(FolioPaymentMethod.CASH), totals.folio(FolioPaymentMethod.CARD), totals.folio(FolioPaymentMethod.OTHER), totals.folioTotal(),
            totals.totalCollected(),
            pos.roomCharge()
        };
        for (int i = 0; i < values.length; i++) {
            amountCell(row, i + 1, values[i], amountStyle);
        }
    }

    // --- Sheet 2: POS payments ----------------------------------------------------------------

    private void writePosPaymentsSheet(
            XSSFWorkbook workbook, Styles styles, List<PaymentEntity> payments, Map<String, OrderEntity> orders, Map<String, TableEntity> tables) {
        XSSFSheet sheet = workbook.createSheet("POS payments");
        String[] columns = {"Date", "Time", "Method", "Amount", "Counted in POS received", "Order", "Table", "Charged to booking"};
        writeHeader(sheet, 0, columns, styles.header);

        int rowIndex = 1;
        for (PaymentEntity payment : payments) {
            LocalDateTime local = localDateTime(payment.getCreatedAt());
            OrderEntity order = orders.get(payment.getOrderId());
            TableEntity table = order != null && order.getTableId() != null ? tables.get(order.getTableId()) : null;

            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(local.toLocalDate().toString());
            row.createCell(1).setCellValue(local.format(TIME_FORMAT));
            row.createCell(2).setCellValue(payment.getMethod().getValue());
            amountCell(row, 3, payment.getAmount(), styles.amount);
            row.createCell(4).setCellValue(payment.getMethod() == PaymentMethod.ROOM_CHARGE ? "No - charged to room" : "Yes");
            row.createCell(5).setCellValue(payment.getOrderId());
            row.createCell(6).setCellValue(table != null ? table.getLabel() : "");
            // Payment.bookingId, not Order.bookingId: the former is what this payment was actually
            // charged to (set only for ROOM_CHARGE) - see Order.bookingId's own openapi description.
            row.createCell(7).setCellValue(payment.getBookingId() != null ? payment.getBookingId() : "");
        }

        int[] widths = {12, 8, 14, 12, 24, 38, 12, 38};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    // --- Sheet 3: Room charge settlements -----------------------------------------------------

    private void writeFolioPaymentsSheet(XSSFWorkbook workbook, Styles styles, List<FolioPaymentEntity> folioPayments, Map<String, BookingEntity> bookings) {
        XSSFSheet sheet = workbook.createSheet("Room charge settlements");
        String[] columns = {"Date", "Time", "Method", "Amount", "Booking", "Guest"};
        writeHeader(sheet, 0, columns, styles.header);

        int rowIndex = 1;
        for (FolioPaymentEntity folioPayment : folioPayments) {
            LocalDateTime local = localDateTime(folioPayment.getCreatedAt());
            BookingEntity booking = bookings.get(folioPayment.getBookingId());

            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(local.toLocalDate().toString());
            row.createCell(1).setCellValue(local.format(TIME_FORMAT));
            row.createCell(2).setCellValue(folioPayment.getMethod().getValue());
            amountCell(row, 3, folioPayment.getAmount(), styles.amount);
            row.createCell(4).setCellValue(folioPayment.getBookingId());
            row.createCell(5).setCellValue(booking != null && booking.getGuestName() != null ? booking.getGuestName() : "");
        }

        int[] widths = {12, 8, 10, 12, 38, 28};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    // --- Sheet 4: Paid bookings (info) --------------------------------------------------------

    private void writePaidBookingsSheet(XSSFWorkbook workbook, Styles styles, List<BookingEntity> bookings) {
        XSSFSheet sheet = workbook.createSheet("Paid bookings (info)");
        Cell note = sheet.createRow(0).createCell(0);
        note.setCellValue("For reference only - not a record of money collected, and not in any total. Bookings whose status is PAID now and whose "
                + "check-out falls in this period. The system does not record when, how, or how much was actually paid for a room.");
        note.setCellStyle(styles.note);

        String[] columns = {"Booking", "Guest", "Check-in", "Check-out", "Total price"};
        writeHeader(sheet, 2, columns, styles.header);

        int rowIndex = 3;
        for (BookingEntity booking : bookings) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(booking.getId());
            row.createCell(1).setCellValue(booking.getGuestName() != null ? booking.getGuestName() : "");
            row.createCell(2).setCellValue(booking.getCheckIn().toString());
            row.createCell(3).setCellValue(booking.getCheckOut().toString());
            amountCell(row, 4, booking.getTotalPrice(), styles.amount);
        }

        int[] widths = {38, 28, 12, 12, 14};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 3);
    }

    // --- Shared -------------------------------------------------------------------------------

    private static void writeHeader(XSSFSheet sheet, int rowIndex, String[] columns, XSSFCellStyle style) {
        Row header = sheet.createRow(rowIndex);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(style);
        }
    }

    /**
     * A numeric cell, not text, so the accountant can sum it in Excel. Routed through
     * {@link PriceFormat#asDecimalString} so an amount with more than two decimals fails loudly
     * here (its {@code RoundingMode.UNNECESSARY}) instead of being silently rounded.
     */
    private static void amountCell(Row row, int column, BigDecimal amount, XSSFCellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(Double.parseDouble(PriceFormat.asDecimalString(amount)));
        cell.setCellStyle(style);
    }

    private static final class Styles {
        final XSSFCellStyle header;
        final XSSFCellStyle amount;
        final XSSFCellStyle boldAmount;
        final XSSFCellStyle note;

        Styles(XSSFWorkbook workbook) {
            XSSFFont bold = workbook.createFont();
            bold.setBold(true);
            short amountFormat = workbook.createDataFormat().getFormat("#,##0.00");

            header = workbook.createCellStyle();
            header.setFont(bold);

            amount = workbook.createCellStyle();
            amount.setDataFormat(amountFormat);

            boldAmount = workbook.createCellStyle();
            boldAmount.setDataFormat(amountFormat);
            boldAmount.setFont(bold);

            XSSFFont italic = workbook.createFont();
            italic.setItalic(true);
            note = workbook.createCellStyle();
            note.setFont(italic);
        }
    }

    /** One day's (or the whole period's) rows, summed by the same rules on both levels. */
    private static final class DayTotals {
        final List<PaymentEntity> pos = new java.util.ArrayList<>();
        final List<FolioPaymentEntity> folio = new java.util.ArrayList<>();

        PaymentAggregation.Totals posTotals() {
            return PaymentAggregation.aggregate(pos);
        }

        BigDecimal folio(FolioPaymentMethod method) {
            return folio.stream().filter(f -> f.getMethod() == method).map(FolioPaymentEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal folioTotal() {
            return folio.stream().map(FolioPaymentEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal totalCollected() {
            return posTotals().receivedTotal().add(folioTotal());
        }
    }
}
