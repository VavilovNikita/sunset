package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.ShiftMapper;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftCloseInput;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.ShiftSummary;
import com.sunsetbeach.model.ShiftTotals;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private static final Logger log = LoggerFactory.getLogger(ShiftService.class);

    private static final List<OrderStatus> UNSETTLED_STATUSES = List.of(OrderStatus.OPEN, OrderStatus.SENT);

    private final ShiftRepository shiftRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableRepository tableRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ShiftMapper shiftMapper;
    private final PrintService printService;
    private final AuditLogService auditLogService;

    public ShiftService(
            ShiftRepository shiftRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            TableRepository tableRepository,
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            UserRepository userRepository,
            ShiftMapper shiftMapper,
            PrintService printService,
            AuditLogService auditLogService) {
        this.shiftRepository = shiftRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.tableRepository = tableRepository;
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.shiftMapper = shiftMapper;
        this.printService = printService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Shift getCurrentOpenShift(String userId) {
        ShiftEntity shift = shiftRepository
                .findByOpenedByUserIdAndStatus(userId, ShiftStatus.OPEN)
                .orElseThrow(() -> new NotFoundException("No open shift"));
        return shiftMapper.toDto(shift);
    }

    @Transactional
    public Shift open(String userId, ShiftOpenInput input) {
        if (shiftRepository.findByOpenedByUserIdAndStatus(userId, ShiftStatus.OPEN).isPresent()) {
            throw new ConflictException("You already have an open shift");
        }

        ShiftEntity entity = new ShiftEntity();
        entity.setOpenedByUserId(userId);
        entity.setOpeningCashFloat(input.getOpeningCashFloat());
        ShiftEntity saved;
        try {
            saved = shiftRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            // Belt-and-suspenders: the unique partial index (Shift_one_open_per_user) is the
            // real guard against a race between two concurrent opens for the same user.
            throw new ConflictException("You already have an open shift");
        }
        auditLogService.record(
                AuditAction.SHIFT_OPENED,
                AuditEntityType.SHIFT,
                saved.getId(),
                "Shift opened with opening float "
                        + (saved.getOpeningCashFloat() != null ? PriceFormat.asDecimalString(saved.getOpeningCashFloat()) : "0.00"));
        return shiftMapper.toDto(saved);
    }

    @Transactional
    public Shift close(String id, ShiftCloseInput input) {
        ShiftEntity shift = shiftRepository.findById(id).orElseThrow(() -> new NotFoundException("Shift not found"));
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new ConflictException("Shift is already closed");
        }
        // Orders don't carry a shiftId (only Payments do), so there's no way to scope this
        // check to "orders opened during this shift" - it's a system-wide gate: don't let the
        // register close while there's still unpaid business anywhere in the POS.
        if (orderRepository.existsByStatusIn(UNSETTLED_STATUSES)) {
            throw new ConflictException("There are still OPEN/SENT orders that need to be closed or cancelled first");
        }

        shift.setClosedByUserId(shift.getOpenedByUserId());
        shift.setClosedAt(LocalDateTime.now());
        shift.setClosingCashCounted(input.getClosingCashCounted());
        shift.setNotes(input.getNotes().orElse(null));
        shift.setStatus(ShiftStatus.CLOSED);
        ShiftEntity saved = shiftRepository.saveAndFlush(shift);
        printZReport(saved);

        auditLogService.record(AuditAction.SHIFT_CLOSED, AuditEntityType.SHIFT, saved.getId(), describeShiftClose(saved));

        return shiftMapper.toDto(saved);
    }

    /**
     * Same reconciliation arithmetic as {@link #buildZReportPayload}/{@link #appendSummary} -
     * opening float + cash payments = expected cash, compared against what was physically
     * counted. Called out explicitly in the audit summary because a cash discrepancy at shift
     * close is exactly the kind of thing this audit trail exists to make traceable back to who
     * closed the drawer.
     */
    private String describeShiftClose(ShiftEntity shift) {
        PaymentAggregation.Totals totals = PaymentAggregation.aggregate(paymentRepository.findByShiftId(shift.getId()));
        BigDecimal openingFloat = shift.getOpeningCashFloat() != null ? shift.getOpeningCashFloat() : BigDecimal.ZERO;
        BigDecimal expectedCash = openingFloat.add(totals.cash());
        BigDecimal closingCounted = shift.getClosingCashCounted();

        String summary = "Shift closed, received " + PriceFormat.asDecimalString(totals.receivedTotal()) + " (cash "
                + PriceFormat.asDecimalString(totals.cash()) + ", card " + PriceFormat.asDecimalString(totals.card()) + ", other "
                + PriceFormat.asDecimalString(totals.other()) + ")";
        if (closingCounted != null) {
            BigDecimal discrepancy = closingCounted.subtract(expectedCash);
            summary += ", counted cash " + PriceFormat.asDecimalString(closingCounted);
            if (discrepancy.compareTo(BigDecimal.ZERO) != 0) {
                summary += " — discrepancy of " + PriceFormat.asDecimalString(discrepancy) + " vs. expected "
                        + PriceFormat.asDecimalString(expectedCash);
            }
        }
        return summary;
    }

    /**
     * Fire-and-forget, same fail-open contract as {@link OrderPrintingService}: an unreachable
     * CASHIER printer must never fail the shift close that triggered it. No-op if no active
     * CASHIER printer is configured.
     */
    private void printZReport(ShiftEntity shift) {
        try {
            printService.findActivePrinter(PrinterDepartment.CASHIER).ifPresent(printer -> {
                byte[] payload = buildZReportPayload(shift, printer.getCodepage());
                printService.queueAndAttempt(
                        printer, PrintDocumentType.Z_REPORT, "Z-report — Shift #" + shortId(shift.getId()), payload);
            });
        } catch (Exception e) {
            log.error("printZReport failed for shift {}", shift.getId(), e);
        }
    }

    /**
     * Same reconciliation numbers as {@link #appendSummary} (the CSV export's totals block):
     * per-method sums, opening float, expected vs. counted cash, discrepancy. ROOM_CHARGE is
     * called out on its own line and excluded from the received total, same reasoning as there.
     */
    private byte[] buildZReportPayload(ShiftEntity shift, PrinterCodepage codepage) {
        PaymentAggregation.Totals totals = PaymentAggregation.aggregate(paymentRepository.findByShiftId(shift.getId()));
        BigDecimal openingFloat = shift.getOpeningCashFloat() != null ? shift.getOpeningCashFloat() : BigDecimal.ZERO;
        BigDecimal expectedCash = openingFloat.add(totals.cash());
        BigDecimal closingCounted = shift.getClosingCashCounted();

        EscPosBuilder b = new EscPosBuilder(codepage);
        b.center(true).bold(true).line("Z-REPORT").bold(false).center(false);
        b.line("Shift #" + shortId(shift.getId()));
        b.line("Closed: " + TimestampFormat.readable(shift.getClosedAt()));
        b.divider();
        b.twoColumn("Cash", PriceFormat.asDecimalString(totals.cash()));
        b.twoColumn("Card", PriceFormat.asDecimalString(totals.card()));
        b.twoColumn("Other", PriceFormat.asDecimalString(totals.other()));
        b.twoColumn("Room charge (folio, not received)", PriceFormat.asDecimalString(totals.roomCharge()));
        b.divider();
        b.twoColumn("Received total", PriceFormat.asDecimalString(totals.receivedTotal()));
        b.twoColumn("Payments", String.valueOf(totals.paymentCount()));
        b.divider();
        b.twoColumn("Opening float", PriceFormat.asDecimalString(openingFloat));
        b.twoColumn("Expected cash", PriceFormat.asDecimalString(expectedCash));
        b.twoColumn("Counted cash", closingCounted != null ? PriceFormat.asDecimalString(closingCounted) : "—");
        b.twoColumn(
                "Discrepancy",
                closingCounted != null ? PriceFormat.asDecimalString(closingCounted.subtract(expectedCash)) : "—");
        return b.cutAndBuild();
    }

    @Transactional(readOnly = true)
    public ShiftSummary getSummary(String id, String callerId, Role callerRole) {
        ShiftEntity shift = shiftRepository.findById(id).orElseThrow(() -> new NotFoundException("Shift not found"));
        // A plain CASHIER may only look up their own shift - MANAGER/ADMIN can look up any.
        // 404 (not 403) so a cashier can't even confirm another cashier's shift id exists.
        if (callerRole == Role.CASHIER && !shift.getOpenedByUserId().equals(callerId)) {
            throw new NotFoundException("Shift not found");
        }
        return shiftMapper.toSummaryDto(shift, computeTotals(id));
    }

    /**
     * The Z-report: one row per payment (in plain-language terms a manager can actually
     * reconcile a cash drawer against - see {@link #describeOrder} / {@link #describeGuestRoom}),
     * followed by a totals block ending in the number that closing a shift is actually for: how
     * far the physically recounted cash is from what the drawer should hold.
     */
    @Transactional(readOnly = true)
    public String exportCsv(String id) {
        ShiftEntity shift = shiftRepository.findById(id).orElseThrow(() -> new NotFoundException("Shift not found"));
        List<PaymentEntity> payments = paymentRepository.findByShiftId(id);

        List<String> orderIds = payments.stream().map(PaymentEntity::getOrderId).distinct().toList();
        Map<String, OrderEntity> ordersById =
                orderRepository.findAllById(orderIds).stream().collect(Collectors.toMap(OrderEntity::getId, o -> o));

        Map<String, List<OrderItemEntity>> itemsByOrderId =
                orderItemRepository.findByOrderIdIn(orderIds).stream().collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
        List<String> menuItemIds =
                itemsByOrderId.values().stream().flatMap(List::stream).map(OrderItemEntity::getMenuItemId).distinct().toList();
        Map<String, String> menuItemNames = menuItemRepository.findAllById(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, MenuItemEntity::getName));

        List<String> tableIds =
                ordersById.values().stream().map(OrderEntity::getTableId).filter(Objects::nonNull).distinct().toList();
        Map<String, TableEntity> tablesById =
                tableRepository.findAllById(tableIds).stream().collect(Collectors.toMap(TableEntity::getId, t -> t));

        List<String> bookingIds = payments.stream().map(PaymentEntity::getBookingId).filter(Objects::nonNull).distinct().toList();
        Map<String, BookingEntity> bookingsById =
                bookingRepository.findAllById(bookingIds).stream().collect(Collectors.toMap(BookingEntity::getId, b -> b));
        List<String> roomIds = bookingsById.values().stream().map(BookingEntity::getRoomId).distinct().toList();
        Map<String, String> roomNamesById =
                roomRepository.findAllById(roomIds).stream().collect(Collectors.toMap(RoomEntity::getId, RoomEntity::getName));

        List<String> userIds = payments.stream().map(PaymentEntity::getRecordedByUserId).distinct().toList();
        Map<String, String> cashierEmailsById =
                userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));

        CsvBuilder csv = new CsvBuilder();
        csv.row("Time", "Method", "Amount", "Order", "Guest / Room", "Cashier", "Payment ID", "Order ID");
        for (PaymentEntity p : payments) {
            csv.row(
                    TimestampFormat.readable(p.getCreatedAt()),
                    p.getMethod().getValue(),
                    PriceFormat.asDecimalString(p.getAmount()),
                    describeOrder(ordersById.get(p.getOrderId()), itemsByOrderId, menuItemNames, tablesById),
                    describeGuestRoom(p.getBookingId(), bookingsById, roomNamesById),
                    cashierEmailsById.getOrDefault(p.getRecordedByUserId(), p.getRecordedByUserId()),
                    p.getId(),
                    p.getOrderId());
        }

        appendSummary(csv, shift, PaymentAggregation.aggregate(payments));

        auditLogService.record(
                AuditAction.SHIFT_EXPORTED,
                AuditEntityType.SHIFT,
                shift.getId(),
                "Exported shift report (" + payments.size() + " payment(s))");

        return csv.toString();
    }

    private ShiftTotals computeTotals(String shiftId) {
        return PaymentAggregation.toShiftTotals(PaymentAggregation.aggregate(paymentRepository.findByShiftId(shiftId)));
    }

    /** "Zone – table label: 2× Mojito; 1× Caesar salad", or a fallback when there's no table. */
    private static String describeOrder(
            OrderEntity order,
            Map<String, List<OrderItemEntity>> itemsByOrderId,
            Map<String, String> menuItemNames,
            Map<String, TableEntity> tablesById) {
        if (order == null) {
            return "";
        }
        String location = describeLocation(order, tablesById);
        String items = itemsByOrderId.getOrDefault(order.getId(), List.of()).stream()
                .map(item -> item.getQuantity() + "× " + menuItemNames.getOrDefault(item.getMenuItemId(), "item"))
                .collect(Collectors.joining("; "));
        return items.isEmpty() ? location : location + ": " + items;
    }

    private static String describeLocation(OrderEntity order, Map<String, TableEntity> tablesById) {
        TableEntity table = order.getTableId() != null ? tablesById.get(order.getTableId()) : null;
        if (table != null) {
            return zoneLabel(table.getZone()) + " – " + table.getLabel();
        }
        if (order.getGuestName() != null && !order.getGuestName().isBlank()) {
            return "Takeaway – " + order.getGuestName();
        }
        return "Order #" + shortId(order.getId());
    }

    private static String zoneLabel(Zone zone) {
        return switch (zone) {
            case RESTAURANT -> "Restaurant";
            case BAR -> "Bar";
            case SPA -> "Spa";
            case POOL -> "Pool";
            case ROOM_SERVICE -> "Room service";
        };
    }

    private static String shortId(String id) {
        return (id.length() > 8 ? id.substring(0, 8) : id).toUpperCase();
    }

    /** "Jane Doe – Ocean View Suite" for a ROOM_CHARGE payment, "—" for everything else. */
    private static String describeGuestRoom(
            String bookingId, Map<String, BookingEntity> bookingsById, Map<String, String> roomNamesById) {
        if (bookingId == null) {
            return "—";
        }
        BookingEntity booking = bookingsById.get(bookingId);
        if (booking == null) {
            return "—";
        }
        String room = roomNamesById.get(booking.getRoomId());
        return room == null || room.isBlank() ? booking.getGuestName() : booking.getGuestName() + " – " + room;
    }

    /**
     * The reconciliation block. `discrepancy = closingCashCounted - expectedCash`: positive
     * means the drawer has more cash than it should, negative means it's short.
     * `expectedCash` deliberately excludes ROOM_CHARGE, same as `PaymentAggregation.Totals#receivedTotal`
     * used by GET /payments/summary - it's a folio transfer, not cash that ever entered the
     * drawer.
     */
    private static void appendSummary(CsvBuilder csv, ShiftEntity shift, PaymentAggregation.Totals totals) {
        BigDecimal openingFloat = shift.getOpeningCashFloat() != null ? shift.getOpeningCashFloat() : BigDecimal.ZERO;
        BigDecimal expectedCash = openingFloat.add(totals.cash());
        BigDecimal closingCounted = shift.getClosingCashCounted();

        csv.row();
        csv.row("Summary");
        csv.row("Cash", PriceFormat.asDecimalString(totals.cash()));
        csv.row("Card", PriceFormat.asDecimalString(totals.card()));
        csv.row("Other", PriceFormat.asDecimalString(totals.other()));
        csv.row("Room charge (posted to room folio - not received cash/card)", PriceFormat.asDecimalString(totals.roomCharge()));
        csv.row("Received total (cash + card + other)", PriceFormat.asDecimalString(totals.receivedTotal()));
        csv.row("Payments", String.valueOf(totals.paymentCount()));
        csv.row("Opening cash float", PriceFormat.asDecimalString(openingFloat));
        csv.row("Expected cash (float + cash payments)", PriceFormat.asDecimalString(expectedCash));
        csv.row("Counted cash", closingCounted != null ? PriceFormat.asDecimalString(closingCounted) : "—");
        csv.row(
                "Discrepancy (counted - expected)",
                closingCounted != null ? PriceFormat.asDecimalString(closingCounted.subtract(expectedCash)) : "—");
    }
}
