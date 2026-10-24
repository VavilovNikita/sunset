package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.FolioPaymentEntity;
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
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.ShiftMapper;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintLayoutDocument;
import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftCloseInput;
import com.sunsetbeach.model.ShiftListItem;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.ShiftSummary;
import com.sunsetbeach.model.ShiftTotals;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.FolioPaymentRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private static final Logger log = LoggerFactory.getLogger(ShiftService.class);

    private static final List<OrderStatus> UNSETTLED_STATUSES = List.of(OrderStatus.OPEN, OrderStatus.SENT);

    private final ShiftRepository shiftRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final FolioPaymentRepository folioPaymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableRepository tableRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ShiftMapper shiftMapper;
    private final PrintService printService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    private final PrintLayoutService printLayoutService;

    public ShiftService(
            ShiftRepository shiftRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            FolioPaymentRepository folioPaymentRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            TableRepository tableRepository,
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            UserRepository userRepository,
            ShiftMapper shiftMapper,
            PrintService printService,
            AuditLogService auditLogService,
            PrintLayoutService printLayoutService,
            Clock clock) {
        this.clock = clock;
        this.printLayoutService = printLayoutService;
        this.shiftRepository = shiftRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.folioPaymentRepository = folioPaymentRepository;
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

    /**
     * Closes a cash shift. A plain CASHIER closes only their own (404 for anyone else's, as in
     * {@link #getSummary}); MANAGER+ may close any, e.g. one left open overnight. Whoever closes it
     * is recorded as the closer - it used to be the opener whatever happened, which pinned a
     * counted figure and its discrepancy on someone who never counted the drawer (audit finding H7).
     */
    @Transactional
    public Shift close(String id, ShiftCloseInput input, String callerId, Role callerRole) {
        // Locked: waits for payments being recorded into this shift right now (they share-lock it,
        // see ShiftRepository#findByOpenerAndStatusForPayment), so the totals below include them, and
        // makes two concurrent closes of one shift run one after the other.
        ShiftEntity shift = shiftRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Shift not found"));
        if (callerRole == Role.CASHIER && !shift.getOpenedByUserId().equals(callerId)) {
            throw new NotFoundException("Shift not found");
        }
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new ConflictException("Shift is already closed");
        }
        // An OPEN order with no lines is an abandoned tap, not unpaid business (a table's order is
        // created with its first line, so these only come from the spa/room-service doors or from
        // a last line being deleted). Cancelling them here, on the record, is what keeps one of them
        // from blocking the close with the 409 below until somebody finds and cancels it by hand.
        for (OrderEntity open : orderRepository.findByStatus(OrderStatus.OPEN)) {
            if (!orderItemRepository.existsByOrderId(open.getId())) {
                open.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(open);
                auditLogService.record(
                        AuditAction.ORDER_CANCELLED, AuditEntityType.ORDER, open.getId(), "Empty order cancelled automatically when the shift was closed");
            }
        }
        orderRepository.flush();

        // Orders don't carry a shiftId (only Payments do), so "this shift's" orders are the ones its
        // cashier opened, plus any that already carry a payment recorded in this shift. Those block
        // the close. Everyone else's open orders are somebody else's business: the close goes ahead
        // and the response names them in a warning, so one waiter's open table can't keep another
        // cashier from counting their drawer.
        Set<String> paidInThisShift = paymentRepository.findByShiftId(shift.getId()).stream()
                .map(PaymentEntity::getOrderId)
                .collect(Collectors.toSet());
        List<OrderEntity> blocking = new ArrayList<>();
        List<OrderEntity> belongingToOthers = new ArrayList<>();
        for (OrderEntity unsettled : orderRepository.findByStatusIn(UNSETTLED_STATUSES)) {
            boolean thisShifts = Objects.equals(unsettled.getOpenedByUserId(), shift.getOpenedByUserId())
                    || paidInThisShift.contains(unsettled.getId());
            (thisShifts ? blocking : belongingToOthers).add(unsettled);
        }
        if (!blocking.isEmpty()) {
            throw new ConflictException("This shift still has " + blocking.size() + " OPEN/SENT order(s) that need to be closed or cancelled first: "
                    + describeOrders(blocking));
        }

        // A drawer that doesn't match the books is explained, not just recorded: counted cash that
        // differs from the expected figure (float + cash payments + folio cash) needs a note saying why.
        String notes = input.getNotes().orElse(null);
        if (input.getClosingCashCounted() != null && (notes == null || com.sunsetbeach.util.NameText.strip(notes).isEmpty())) {
            PaymentAggregation.Totals totals = PaymentAggregation.aggregate(paymentRepository.findByShiftId(shift.getId()));
            BigDecimal expected = expectedCash(shift, totals.cash(), folioTotals(shift.getId()));
            BigDecimal difference = input.getClosingCashCounted().subtract(expected);
            if (difference.signum() != 0) {
                throw ValidationException.field("notes", "The counted cash differs from the expected " + PriceFormat.asDecimalString(expected)
                        + " by " + PriceFormat.asDecimalString(difference) + " - write a note saying why before closing the shift.");
            }
        }
        shift.setClosedByUserId(callerId);
        shift.setClosedAt(TimestampFormat.nowUtc(clock));
        shift.setClosingCashCounted(input.getClosingCashCounted());
        shift.setNotes(notes == null ? null : com.sunsetbeach.util.NameText.strip(notes));
        shift.setStatus(ShiftStatus.CLOSED);
        ShiftEntity saved = shiftRepository.saveAndFlush(shift);
        printZReport(saved);

        auditLogService.record(AuditAction.SHIFT_CLOSED, AuditEntityType.SHIFT, saved.getId(), describeShiftClose(saved));

        Shift dto = shiftMapper.toDto(saved);
        if (!belongingToOthers.isEmpty()) {
            dto.warning(belongingToOthers.size() + " open order(s) opened by other staff were left open: " + describeOrders(belongingToOthers));
        }
        return dto;
    }

    /** "#1204, #1207 and 2 more" - enough to find them, short enough for a banner. */
    private static String describeOrders(List<OrderEntity> orders) {
        int shown = Math.min(orders.size(), 5);
        String list = orders.stream()
                .limit(shown)
                .map(o -> o.getNumber() != null ? "#" + o.getNumber() : o.getId().substring(0, 8))
                .collect(Collectors.joining(", "));
        return orders.size() > shown ? list + " and " + (orders.size() - shown) + " more" : list;
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
        FolioTotals folio = folioTotals(shift.getId());
        BigDecimal expectedCash = expectedCash(shift, totals.cash(), folio);
        BigDecimal closingCounted = shift.getClosingCashCounted();

        String summary = "Shift closed, received " + PriceFormat.asDecimalString(totals.receivedTotal()) + " (cash "
                + PriceFormat.asDecimalString(totals.cash()) + ", card " + PriceFormat.asDecimalString(totals.card()) + ", other "
                + PriceFormat.asDecimalString(totals.other()) + ")"
                + (folio.isEmpty() ? "" : ", plus folio payments " + PriceFormat.asDecimalString(folio.total()) + " (cash "
                        + PriceFormat.asDecimalString(folio.cash()) + ")");
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
                printService.queue(
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
        FolioTotals folio = folioTotals(shift.getId());
        BigDecimal openingFloat = shift.getOpeningCashFloat() != null ? shift.getOpeningCashFloat() : BigDecimal.ZERO;
        BigDecimal expectedCash = expectedCash(shift, totals.cash(), folio);
        BigDecimal closingCounted = shift.getClosingCashCounted();

        PrintRenderer.Rendered rendered = PrintRenderer.report(
                printLayoutService.style(PrintLayoutDocument.Z_REPORT),
                ZReportContent.build(
                        shortId(shift.getId()),
                        TimestampFormat.readableHotel(shift.getClosedAt(), clock),
                        PriceFormat.asDecimalString(totals.cash()),
                        PriceFormat.asDecimalString(totals.card()),
                        PriceFormat.asDecimalString(totals.other()),
                        PriceFormat.asDecimalString(totals.roomCharge()),
                        PriceFormat.asDecimalString(totals.receivedTotal()),
                        String.valueOf(totals.paymentCount()),
                        folio.isEmpty()
                                ? null
                                : new ZReportContent.Folio(
                                        PriceFormat.asDecimalString(folio.cash()),
                                        PriceFormat.asDecimalString(folio.card()),
                                        PriceFormat.asDecimalString(folio.other())),
                        PriceFormat.asDecimalString(openingFloat),
                        PriceFormat.asDecimalString(expectedCash),
                        closingCounted != null ? PriceFormat.asDecimalString(closingCounted) : "—",
                        closingCounted != null ? PriceFormat.asDecimalString(closingCounted.subtract(expectedCash)) : "—",
                        null));
        return PrintRenderer.toEscPos(rendered, codepage);
    }

    @Transactional(readOnly = true)
    public ShiftSummary getSummary(String id, String callerId, Role callerRole) {
        ShiftEntity shift = shiftRepository.findById(id).orElseThrow(() -> new NotFoundException("Shift not found"));
        // A plain CASHIER may only look up their own shift - MANAGER/ADMIN can look up any.
        // 404 (not 403) so a cashier can't even confirm another cashier's shift id exists.
        if (callerRole == Role.CASHIER && !shift.getOpenedByUserId().equals(callerId)) {
            throw new NotFoundException("Shift not found");
        }
        ShiftSummary summary = shiftMapper.toSummaryDto(shift, computeTotals(id));
        // Who opened/closed the drawer, same lookup as list() - a CASHIER can't call GET /users
        // to resolve the ids themselves.
        userRepository.findById(shift.getOpenedByUserId()).map(UserEntity::getEmail).ifPresent(summary::setOpenedByEmail);
        if (shift.getClosedByUserId() != null) {
            userRepository.findById(shift.getClosedByUserId()).map(UserEntity::getEmail).ifPresent(summary::setClosedByEmail);
        }
        return summary;
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

        appendSummary(csv, shift, PaymentAggregation.aggregate(payments), folioTotals(shift.getId()));

        auditLogService.record(
                AuditAction.SHIFT_EXPORTED,
                AuditEntityType.SHIFT,
                shift.getId(),
                "Exported shift report (" + payments.size() + " payment(s))");

        return csv.toString();
    }

    private ShiftTotals computeTotals(String shiftId) {
        FolioTotals folio = folioTotals(shiftId);
        return PaymentAggregation.toShiftTotals(PaymentAggregation.aggregate(paymentRepository.findByShiftId(shiftId)))
                .folioCash(PriceFormat.asDecimalString(folio.cash()))
                .folioCard(PriceFormat.asDecimalString(folio.card()))
                .folioOther(PriceFormat.asDecimalString(folio.other()));
    }

    /**
     * Folio payments recorded during this shift ({@code FolioPayment.shiftId}, set by {@code
     * BookingService#recordFolioPayment} from the recorder's open shift) - money taken at reception
     * against a booking. Kept apart from POS {@code Payment} totals, but its cash is in the drawer,
     * so it counts in {@link #expectedCash}: leaving it out made every shift that took room money
     * at the desk look over by exactly that amount.
     */
    private FolioTotals folioTotals(String shiftId) {
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        for (FolioPaymentEntity p : folioPaymentRepository.findByShiftIdOrderByCreatedAtAsc(shiftId)) {
            switch (p.getMethod()) {
                case CASH -> cash = cash.add(p.getAmount());
                case CARD -> card = card.add(p.getAmount());
                case OTHER -> other = other.add(p.getAmount());
            }
        }
        return new FolioTotals(cash, card, other);
    }

    private record FolioTotals(BigDecimal cash, BigDecimal card, BigDecimal other) {
        BigDecimal total() {
            return cash.add(card).add(other);
        }

        boolean isEmpty() {
            return total().signum() == 0;
        }
    }

    /** Opening float + POS cash + folio cash - the one expected-cash formula every shift read uses. */
    private static BigDecimal expectedCash(ShiftEntity shift, BigDecimal posCash, FolioTotals folio) {
        BigDecimal openingFloat = shift.getOpeningCashFloat() != null ? shift.getOpeningCashFloat() : BigDecimal.ZERO;
        return openingFloat.add(posCash).add(folio.cash());
    }

    /**
     * {@code GET /shifts} - the till-reconciliation overview {@link #exportCsv} lacks across more
     * than one shift at a time. Same reconciliation arithmetic as {@link #describeShiftClose}/
     * {@link #buildZReportPayload}/{@link #appendSummary}, computed per shift (shift volume here
     * is front-desk-staff-count-per-day, not order/booking scale, so one totals query per shift
     * is the same tradeoff several other manager-facing reads in this service already make, not
     * a new one). {@code from}/{@code to} are inclusive on both ends against {@code openedAt};
     * {@code staffId} narrows to one opener. Newest first, so the shift a manager most likely
     * wants (today's, or the one just closed) is always at the top.
     */
    @Transactional(readOnly = true)
    public List<ShiftListItem> list(String from, String to, String staffId) {
        List<ShiftEntity> shifts =
                shiftRepository.findAll(buildListSpecification(from, to, staffId, clock.getZone()), Sort.by(Sort.Direction.DESC, "openedAt"));

        List<String> userIds = shifts.stream()
                .flatMap(s -> Stream.of(s.getOpenedByUserId(), s.getClosedByUserId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> emailsById =
                userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));

        return shifts.stream()
                .map(shift -> {
                    ShiftTotals totals = computeTotals(shift.getId());
                    BigDecimal expectedCash = expectedCash(shift, new BigDecimal(totals.getCash()), folioTotals(shift.getId()));
                    BigDecimal discrepancy =
                            shift.getClosingCashCounted() != null ? shift.getClosingCashCounted().subtract(expectedCash) : null;
                    return shiftMapper.toListItemDto(
                            shift,
                            emailsById.get(shift.getOpenedByUserId()),
                            shift.getClosedByUserId() != null ? emailsById.get(shift.getClosedByUserId()) : null,
                            totals,
                            expectedCash,
                            discrepancy);
                })
                .toList();
    }

    private static Specification<ShiftEntity> buildListSpecification(String from, String to, String staffId, java.time.ZoneId zone) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null ? LocalDate.parse(to) : null;

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("openedAt"), HotelDay.startUtc(fromDate, zone)));
            }
            if (toDate != null) {
                // Hotel (Bangkok) days, converted to the UTC openedAt holds - see HotelDay. Exclusive upper bound one day past `to`, so a shift opened any time on the `to`
                // date itself is still included - the closed-on-both-ends date range every other
                // filter in this API promises is about calendar days, not a literal <= on a
                // datetime column, which would silently drop same-day matches after midnight.
                predicates.add(cb.lessThan(root.get("openedAt"), HotelDay.startUtc(toDate.plusDays(1), zone)));
            }
            if (staffId != null && !staffId.isBlank()) {
                predicates.add(cb.equal(root.get("openedByUserId"), staffId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
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
    private static void appendSummary(CsvBuilder csv, ShiftEntity shift, PaymentAggregation.Totals totals, FolioTotals folio) {
        BigDecimal openingFloat = shift.getOpeningCashFloat() != null ? shift.getOpeningCashFloat() : BigDecimal.ZERO;
        BigDecimal expectedCash = expectedCash(shift, totals.cash(), folio);
        BigDecimal closingCounted = shift.getClosingCashCounted();

        csv.row();
        csv.row("Summary");
        csv.row("Cash", PriceFormat.asDecimalString(totals.cash()));
        csv.row("Card", PriceFormat.asDecimalString(totals.card()));
        csv.row("Other", PriceFormat.asDecimalString(totals.other()));
        csv.row("Room charge (posted to room folio - not received cash/card)", PriceFormat.asDecimalString(totals.roomCharge()));
        csv.row("Received total (cash + card + other)", PriceFormat.asDecimalString(totals.receivedTotal()));
        csv.row("Payments", String.valueOf(totals.paymentCount()));
        csv.row("Folio payments at reception - cash", PriceFormat.asDecimalString(folio.cash()));
        csv.row("Folio payments at reception - card", PriceFormat.asDecimalString(folio.card()));
        csv.row("Folio payments at reception - other", PriceFormat.asDecimalString(folio.other()));
        csv.row("Opening cash float", PriceFormat.asDecimalString(openingFloat));
        csv.row("Expected cash (float + cash payments + folio cash)", PriceFormat.asDecimalString(expectedCash));
        csv.row("Counted cash", closingCounted != null ? PriceFormat.asDecimalString(closingCounted) : "—");
        csv.row(
                "Discrepancy (counted - expected)",
                closingCounted != null ? PriceFormat.asDecimalString(closingCounted.subtract(expectedCash)) : "—");
    }
}
