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
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

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
            ShiftMapper shiftMapper) {
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
        try {
            return shiftMapper.toDto(shiftRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            // Belt-and-suspenders: the unique partial index (Shift_one_open_per_user) is the
            // real guard against a race between two concurrent opens for the same user.
            throw new ConflictException("You already have an open shift");
        }
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
        return shiftMapper.toDto(shiftRepository.saveAndFlush(shift));
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
