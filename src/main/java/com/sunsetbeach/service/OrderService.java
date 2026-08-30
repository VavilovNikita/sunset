package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.OrderMapper;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.OrderUpdateInput;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.TableRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Set<OrderStatus> CLOSED_STATUSES = Set.of(OrderStatus.PAID, OrderStatus.CANCELLED);
    private static final Set<OrderStatus> ADDABLE_STATUSES = Set.of(OrderStatus.OPEN, OrderStatus.SENT);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableRepository tableRepository;
    private final BookingRepository bookingRepository;
    private final ShiftRepository shiftRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final OrderPrintingService orderPrintingService;
    private final AuditLogService auditLogService;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            TableRepository tableRepository,
            BookingRepository bookingRepository,
            ShiftRepository shiftRepository,
            PaymentRepository paymentRepository,
            OrderMapper orderMapper,
            OrderPrintingService orderPrintingService,
            AuditLogService auditLogService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.tableRepository = tableRepository;
        this.bookingRepository = bookingRepository;
        this.shiftRepository = shiftRepository;
        this.paymentRepository = paymentRepository;
        this.orderMapper = orderMapper;
        this.orderPrintingService = orderPrintingService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Order> list(OrderStatus status, Zone zone, String tableId, String bookingId) {
        List<String> zoneTableIds = null;
        if (zone != null) {
            zoneTableIds = tableRepository.findByZone(zone).stream().map(TableEntity::getId).toList();
            if (zoneTableIds.isEmpty()) {
                return List.of();
            }
        }

        List<OrderEntity> orders = orderRepository.findAll(buildSpecification(status, zoneTableIds, tableId, bookingId));
        return toDtos(orders);
    }

    @Transactional(readOnly = true)
    public Order getById(String id) {
        OrderEntity order = findOrThrow(id);
        return orderMapper.toDto(order, orderItemRepository.findByOrderId(id));
    }

    @Transactional
    public Order create(OrderCreateInput input, String openedByUserId) {
        String tableId = input.getTableId().orElse(null);
        if (tableId != null && !tableRepository.existsById(tableId)) {
            throw new NotFoundException("Table not found");
        }
        String bookingId = input.getBookingId().orElse(null);
        if (bookingId != null && !bookingRepository.existsById(bookingId)) {
            throw new NotFoundException("Booking not found");
        }

        OrderEntity entity = new OrderEntity();
        entity.setTableId(tableId);
        entity.setBookingId(bookingId);
        entity.setGuestName(input.getGuestName().orElse(null));
        entity.setOpenedByUserId(openedByUserId);
        OrderEntity saved = orderRepository.saveAndFlush(entity);
        return orderMapper.toDto(saved, List.of());
    }

    @Transactional
    public Order update(String id, OrderUpdateInput input) {
        OrderEntity order = findOrThrow(id);

        if (input.getTableId().isPresent()) {
            String tableId = input.getTableId().get();
            if (tableId != null && !tableRepository.existsById(tableId)) {
                throw new NotFoundException("Table not found");
            }
            order.setTableId(tableId);
        }
        if (input.getNote().isPresent()) {
            String note = input.getNote().get();
            order.setNote(note != null ? note.trim() : null);
        }
        boolean transitionedToSent = false;
        if (input.getStatus() != null) {
            if (order.getStatus() != OrderStatus.OPEN || input.getStatus() != OrderStatus.SENT) {
                throw ValidationException.field("status", "Only the OPEN -> SENT transition is allowed here");
            }
            order.setStatus(OrderStatus.SENT);
            transitionedToSent = true;
        }

        OrderEntity saved = orderRepository.saveAndFlush(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(id);
        if (transitionedToSent) {
            orderPrintingService.printTickets(saved, items);
        }
        return orderMapper.toDto(saved, items);
    }

    @Transactional
    public Order addItems(String id, List<OrderItemInput> inputs) {
        OrderEntity order = findOrThrow(id);
        // Unlike updateItem/deleteItem, adding new lines is allowed on a SENT order too - a
        // guest ordering another round after the ticket went to the kitchen is normal, and it's
        // purely additive (nothing already sent is touched). See openapi.yaml for why this
        // can't be told apart from the original ticket after the fact.
        if (!ADDABLE_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Order is not open or sent");
        }

        for (OrderItemInput input : inputs) {
            MenuItemEntity menuItem = menuItemRepository
                    .findById(input.getMenuItemId())
                    .orElseThrow(() -> new NotFoundException("Menu item not found: " + input.getMenuItemId()));
            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(order.getId());
            item.setMenuItemId(menuItem.getId());
            item.setQuantity(input.getQuantity());
            item.setUnitPrice(menuItem.getPrice());
            item.setNote(input.getNote().orElse(null));
            orderItemRepository.save(item);
        }

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(id);
        order.setTotal(sumTotal(items));
        OrderEntity saved = orderRepository.saveAndFlush(order);
        return orderMapper.toDto(saved, items);
    }

    @Transactional
    public Order updateItem(String id, String itemId, OrderItemInput input) {
        OrderEntity order = findOrThrow(id);
        requireOpen(order);

        OrderItemEntity item = orderItemRepository
                .findByIdAndOrderId(itemId, id)
                .orElseThrow(() -> new NotFoundException("Line item not found"));
        item.setQuantity(input.getQuantity());
        item.setNote(input.getNote().orElse(null));
        orderItemRepository.save(item);

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(id);
        order.setTotal(sumTotal(items));
        OrderEntity saved = orderRepository.saveAndFlush(order);
        return orderMapper.toDto(saved, items);
    }

    @Transactional
    public Order deleteItem(String id, String itemId) {
        OrderEntity order = findOrThrow(id);
        requireOpen(order);

        OrderItemEntity item = orderItemRepository
                .findByIdAndOrderId(itemId, id)
                .orElseThrow(() -> new NotFoundException("Line item not found"));
        orderItemRepository.delete(item);

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(id);
        order.setTotal(sumTotal(items));
        OrderEntity saved = orderRepository.saveAndFlush(order);
        return orderMapper.toDto(saved, items);
    }

    @Transactional
    public Order close(String id, CloseOrderInput input, String cashierUserId) {
        OrderEntity order = findOrThrow(id);
        if (CLOSED_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Order is already " + order.getStatus().getValue().toLowerCase());
        }

        ShiftEntity shift = shiftRepository
                .findByOpenedByUserIdAndStatus(cashierUserId, ShiftStatus.OPEN)
                .orElseThrow(() -> new ConflictException("You don't have an open shift"));

        String bookingId = null;
        BookingEntity booking = null;
        if (input.getMethod() == PaymentMethod.ROOM_CHARGE) {
            bookingId = input.getBookingId();
            if (bookingId == null || bookingId.isBlank()) {
                throw ValidationException.field("bookingId", "required when method is ROOM_CHARGE");
            }
            booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                throw new ConflictException("Booking is cancelled");
            }
        }

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod(input.getMethod());
        // Always Order.total, never a client-supplied amount - there's no partial-payment
        // model (an order goes straight to the terminal PAID status on the first close), so
        // trusting a client "amount" here would let a cashier or a form bug post the wrong
        // figure into the cash report and the guest's room folio.
        payment.setAmount(order.getTotal());
        payment.setBookingId(bookingId);
        payment.setRecordedByUserId(cashierUserId);
        payment.setShiftId(shift.getId());
        try {
            // Flushed immediately (not left for the save-and-flush below) so the unique
            // constraint on Payment.orderId - the real guard against two concurrent closes of
            // the same order both passing the CLOSED_STATUSES check above before either commits -
            // is checked right here, in this method's own try/catch, rather than surfacing later
            // as an unrelated-looking failure.
            paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This order was already closed by another request.");
        }

        order.setStatus(OrderStatus.PAID);
        OrderEntity saved = orderRepository.saveAndFlush(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(id);
        orderPrintingService.printGuestReceipt(saved, items, payment, booking);

        auditLogService.record(
                AuditAction.ORDER_CLOSED,
                AuditEntityType.ORDER,
                saved.getId(),
                "Order closed with " + input.getMethod().getValue() + " payment of " + saved.getTotal()
                        + (booking != null ? " (charged to " + booking.getGuestName() + "'s room)" : ""));
        if (booking != null) {
            auditLogService.record(
                    AuditAction.ROOM_CHARGE_POSTED,
                    AuditEntityType.BOOKING,
                    booking.getId(),
                    "Room charge of " + saved.getTotal() + " posted to " + booking.getGuestName() + "'s folio from order " + saved.getId());
        }

        return orderMapper.toDto(saved, items);
    }

    @Transactional
    public PrintAttemptResult printPrebill(String id) {
        OrderEntity order = findOrThrow(id);
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(id);
        return orderPrintingService.printPrebill(order, items);
    }

    @Transactional
    public Order cancel(String id) {
        OrderEntity order = findOrThrow(id);
        if (CLOSED_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Order is already " + order.getStatus().getValue().toLowerCase());
        }
        order.setStatus(OrderStatus.CANCELLED);
        OrderEntity saved = orderRepository.saveAndFlush(order);
        auditLogService.record(
                AuditAction.ORDER_CANCELLED, AuditEntityType.ORDER, saved.getId(), "Order cancelled (total was " + saved.getTotal() + ")");
        return orderMapper.toDto(saved, orderItemRepository.findByOrderId(id));
    }

    private void requireOpen(OrderEntity order) {
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new ConflictException("Order is not open");
        }
    }

    private static BigDecimal sumTotal(List<OrderItemEntity> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemEntity item : items) {
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

    private OrderEntity findOrThrow(String id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private List<Order> toDtos(List<OrderEntity> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<String> orderIds = orders.stream().map(OrderEntity::getId).toList();
        Map<String, List<OrderItemEntity>> itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
        return orders.stream().map(o -> orderMapper.toDto(o, itemsByOrderId.getOrDefault(o.getId(), List.of()))).toList();
    }

    /** Shared by {@link #list}. */
    private static Specification<OrderEntity> buildSpecification(
            OrderStatus status, List<String> zoneTableIds, String tableId, String bookingId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (tableId != null) {
                predicates.add(cb.equal(root.get("tableId"), tableId));
            }
            if (bookingId != null) {
                predicates.add(cb.equal(root.get("bookingId"), bookingId));
            }
            if (zoneTableIds != null) {
                predicates.add(root.get("tableId").in(zoneTableIds));
            }
            query.orderBy(cb.asc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
