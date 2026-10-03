package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.OrderItemVoidEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.GuestOrderMapper;
import com.sunsetbeach.mapper.OrderMapper;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.GuestOrderView;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderDateBasis;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderItemVoidInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.OrderUpdateInput;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderItemVoidRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Set<OrderStatus> CLOSED_STATUSES = Set.of(OrderStatus.PAID, OrderStatus.CANCELLED);
    private static final Set<OrderStatus> ADDABLE_STATUSES = Set.of(OrderStatus.OPEN, OrderStatus.SENT);
    /** {@link #resolveEmail}'s label for a room-service order - see {@code Order.openedByUserId}'s own openapi.yaml description for why this id is null in the first place. */
    private static final String GUEST_ROOM_SERVICE_LABEL = "Guest (room service)";
    private static final List<SpaAppointmentStatus> LINKABLE_SPA_APPOINTMENT_STATUSES =
            List.of(SpaAppointmentStatus.BOOKED, SpaAppointmentStatus.COMPLETED);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemVoidRepository orderItemVoidRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableRepository tableRepository;
    private final BookingRepository bookingRepository;
    private final ShiftRepository shiftRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SpaAppointmentRepository spaAppointmentRepository;
    private final OrderMapper orderMapper;
    private final GuestOrderMapper guestOrderMapper;
    private final OrderPrintingService orderPrintingService;
    private final AuditLogService auditLogService;
    private final LedgerService ledgerService;
    private final long spaOrderLinkGraceMinutes;
    private final SecureRandom guestAccessTokenRandom = new SecureRandom();

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderItemVoidRepository orderItemVoidRepository,
            MenuItemRepository menuItemRepository,
            TableRepository tableRepository,
            BookingRepository bookingRepository,
            ShiftRepository shiftRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            SpaAppointmentRepository spaAppointmentRepository,
            OrderMapper orderMapper,
            GuestOrderMapper guestOrderMapper,
            OrderPrintingService orderPrintingService,
            AuditLogService auditLogService,
            LedgerService ledgerService,
            @Value("${app.spa.order-link-grace-minutes}") long spaOrderLinkGraceMinutes) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderItemVoidRepository = orderItemVoidRepository;
        this.menuItemRepository = menuItemRepository;
        this.tableRepository = tableRepository;
        this.bookingRepository = bookingRepository;
        this.shiftRepository = shiftRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.spaAppointmentRepository = spaAppointmentRepository;
        this.spaOrderLinkGraceMinutes = spaOrderLinkGraceMinutes;
        this.orderMapper = orderMapper;
        this.guestOrderMapper = guestOrderMapper;
        this.orderPrintingService = orderPrintingService;
        this.auditLogService = auditLogService;
        this.ledgerService = ledgerService;
    }

    @Transactional(readOnly = true)
    public List<Order> list(OrderStatus status, String tableId, LocalDate from, LocalDate to, OrderDateBasis dateBasis, String shiftId) {
        // Order carries no shiftId (only Payment does, and only once the order is closed - see
        // ShiftsApi) - resolve the membership the same way ShiftService's own reconciliation
        // does, then filter on Order.id, rather than a Criteria subquery.
        List<String> shiftOrderIds = null;
        if (shiftId != null) {
            shiftOrderIds = paymentRepository.findByShiftId(shiftId).stream().map(PaymentEntity::getOrderId).distinct().toList();
            if (shiftOrderIds.isEmpty()) {
                return List.of();
            }
        }

        List<OrderEntity> orders = orderRepository.findAll(buildSpecification(status, tableId, from, to, dateBasis, shiftOrderIds));
        return toDtos(orders);
    }

    @Transactional(readOnly = true)
    public Order getById(String id) {
        OrderEntity order = findOrThrow(id);
        return toDto(order, orderItemRepository.findByOrderId(id));
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
        entity.setGuestAccessToken(generateGuestAccessToken());
        OrderEntity saved = orderRepository.saveAndFlush(entity);

        // Explicit override only, here - auto-resolution can never fire at creation, since an
        // order has no items yet and, since the correction below, auto-resolution requires one.
        // See #autoLinkSpaAppointment, run from #addItems instead.
        String explicitSpaAppointmentId = input.getSpaAppointmentId().orElse(null);
        if (explicitSpaAppointmentId != null) {
            linkExplicitSpaAppointment(explicitSpaAppointmentId, saved.getId());
        }

        // The POS creates a table's order together with its first line (see OrderCreateInput.items),
        // so tapping a free table and leaving never leaves an empty order holding the table.
        // Same transaction and same path as POST /orders/{id}/items, so pricing, merging and the
        // spa auto-link behave identically either way.
        if (input.getItems() != null && !input.getItems().isEmpty()) {
            return addItems(saved.getId(), input.getItems());
        }

        return toDto(saved, List.of());
    }

    /**
     * The unconditional override for {@code SpaAppointment.orderId} - see that field's own
     * openapi.yaml description. An id that doesn't resolve to a real appointment is silently
     * ignored, same "don't let a side link fail the write it rides on" spirit as printing/audit;
     * order creation is never blocked by this. Deliberately has none of
     * {@link #autoLinkSpaAppointment}'s gates (zone, item content, time/count matching) - staff
     * naming a specific appointment is the case those gates exist to be overridden by.
     */
    private void linkExplicitSpaAppointment(String spaAppointmentId, String orderId) {
        spaAppointmentRepository.findById(spaAppointmentId).ifPresent(appointment -> {
            appointment.setOrderId(orderId);
            spaAppointmentRepository.save(appointment);
        });
    }

    /**
     * The automatic path for {@code SpaAppointment.orderId} - see that field's own openapi.yaml
     * description, and {@link #linkExplicitSpaAppointment} for the staff-named override this
     * complements. Tried from two different moments, on two different axes, because the two
     * facts each axis needs never exist at the same call site:
     *
     * <ul>
     * <li>{@link #autoLinkSpaAppointmentByTable}, run from {@link #addItems} - the table is
     * known from the moment the order is opened, but the order's items (needed for the
     * SPA-content gate below) only exist once something has been rung in.
     * <li>{@link #autoLinkSpaAppointmentByBooking}, run from {@link #close} - a {@code
     * ROOM_CHARGE} close is the one place a booking is actually known. {@code Order.bookingId}
     * (set only from an optional field on {@code POST /orders} - see that field's own
     * description) is never populated by any real caller today, so resolving on the booking axis
     * anywhere before this was dead code that happened to never fire; this uses {@code
     * CloseOrderInput.bookingId} instead, the value already resolved for {@code Payment.bookingId}
     * two lines above its own call site.
     * </ul>
     *
     * <p>Both share {@link #canAutoLink}'s gates - see that method's own javadoc for what they
     * are and why. Neither ever resolves a *second* time once either has linked the order -
     * {@code canAutoLink}'s existsByOrderId check covers both orderings (table linked first,
     * booking attempted later at close; or vice versa, though the booking axis existing at all is
     * new as of this method split).
     */
    private void autoLinkSpaAppointmentByTable(OrderEntity order, List<OrderItemEntity> items) {
        if (!canAutoLink(order, items)) {
            return;
        }
        String tableId = order.getTableId();
        if (tableId == null) {
            return;
        }
        applyLink(resolveByTable(tableId, order.getCreatedAt()), order.getId());
    }

    /**
     * See {@link #autoLinkSpaAppointmentByTable}'s own javadoc for why this runs from {@link
     * #close} rather than earlier. {@code bookingId} is the id already resolved there for the
     * {@code ROOM_CHARGE} payment being recorded, not read off the order itself. Uses the same
     * "moment" convention as the table axis - the order's own {@code createdAt} date, not the
     * close instant - for the same reason: this asks which appointment the *order's* items
     * correspond to, not when the till happened to close it.
     */
    private void autoLinkSpaAppointmentByBooking(OrderEntity order, List<OrderItemEntity> items, String bookingId) {
        if (!canAutoLink(order, items)) {
            return;
        }
        applyLink(resolveByBooking(bookingId, order.getCreatedAt().toLocalDate()), order.getId());
    }

    /**
     * Shared by both auto-link axes: already linked (explicitly, by an earlier {@code addItems}
     * call, or - now - by a booking match at an earlier close attempt on this same order, though
     * an order only ever closes once) never re-resolves onto a second appointment; an order with
     * no SPA-department item is never a treatment regardless of table/booking, the correction
     * {@code OrderSpaAppointmentLinkTests} exists to hold in place; and a table outside the SPA
     * zone rules out both axes, not just its own - a restaurant dinner charged to the room is
     * never a treatment no matter what the booking axis would otherwise resolve, the same masking
     * risk a spa-table order for a bottle of water would carry on the table axis alone.
     */
    private boolean canAutoLink(OrderEntity order, List<OrderItemEntity> items) {
        if (spaAppointmentRepository.existsByOrderId(order.getId())) {
            return false;
        }
        if (items.stream().map(this::departmentOf).noneMatch(MenuDepartment.SPA::equals)) {
            return false;
        }
        String tableId = order.getTableId();
        TableEntity table = tableId != null ? tableRepository.findById(tableId).orElse(null) : null;
        return table == null || table.getZone() == Zone.SPA;
    }

    private void applyLink(String spaAppointmentId, String orderId) {
        if (spaAppointmentId == null) {
            return;
        }
        spaAppointmentRepository.findById(spaAppointmentId).ifPresent(appointment -> {
            appointment.setOrderId(orderId);
            spaAppointmentRepository.save(appointment);
        });
    }

    private MenuDepartment departmentOf(OrderItemEntity item) {
        return menuItemRepository.findById(item.getMenuItemId()).map(MenuItemEntity::getDepartment).orElse(null);
    }

    private String resolveByBooking(String bookingId, LocalDate today) {
        List<SpaAppointmentEntity> candidates =
                spaAppointmentRepository.findByBookingIdAndDateAndOrderIdIsNullAndStatusIn(bookingId, today, LINKABLE_SPA_APPOINTMENT_STATUSES);
        return candidates.size() == 1 ? candidates.get(0).getId() : null;
    }

    private String resolveByTable(String tableId, LocalDateTime openedAt) {
        List<SpaAppointmentEntity> candidates = spaAppointmentRepository.findByTableIdAndDateAndOrderIdIsNullAndStatusIn(
                tableId, openedAt.toLocalDate(), LINKABLE_SPA_APPOINTMENT_STATUSES);

        List<SpaAppointmentEntity> live = candidates.stream().filter(a -> isDuring(a, openedAt)).toList();
        if (live.size() == 1) {
            return live.get(0).getId();
        }
        if (!live.isEmpty()) {
            return null; // more than one live candidate should be impossible under the exclusion constraint; decline rather than guess
        }

        List<SpaAppointmentEntity> recentlyEnded = candidates.stream().filter(a -> isWithinGraceOfEnding(a, openedAt)).toList();
        return recentlyEnded.size() == 1 ? recentlyEnded.get(0).getId() : null;
    }

    private static LocalDateTime startOf(SpaAppointmentEntity appointment) {
        return LocalDateTime.of(appointment.getDate(), appointment.getStartTime());
    }

    private static LocalDateTime endOf(SpaAppointmentEntity appointment) {
        return startOf(appointment).plusMinutes(appointment.getDurationMinutes());
    }

    private static boolean isDuring(SpaAppointmentEntity appointment, LocalDateTime moment) {
        return !moment.isBefore(startOf(appointment)) && moment.isBefore(endOf(appointment));
    }

    private boolean isWithinGraceOfEnding(SpaAppointmentEntity appointment, LocalDateTime moment) {
        LocalDateTime end = endOf(appointment);
        return !moment.isBefore(end) && moment.isBefore(end.plusMinutes(spaOrderLinkGraceMinutes));
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
            dispatchUnsentTickets(saved);
            items = orderItemRepository.findByOrderId(id);
        }
        return toDto(saved, items);
    }

    @Transactional
    public Order addItems(String id, List<OrderItemInput> inputs) {
        OrderEntity order = findOrThrow(id);
        // Unlike updateItem/deleteItem, adding new lines is allowed on a SENT order too - a
        // guest ordering another round after the ticket went to the kitchen is normal, and it's
        // purely additive (nothing already sent is touched). See openapi.yaml for the merge and
        // re-order-printing rules that make this safe.
        if (!ADDABLE_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Order is not open or sent");
        }

        List<OrderItemEntity> existing = orderItemRepository.findByOrderId(id);
        for (OrderItemInput input : inputs) {
            MenuItemEntity menuItem = menuItemRepository
                    .findById(input.getMenuItemId())
                    .orElseThrow(() -> new NotFoundException("Menu item not found: " + input.getMenuItemId()));
            String note = normalizeNote(input.getNote().orElse(null));

            // Only merge into a line that hasn't gone out yet (sentAt == null) - a line already
            // on a printed ticket must never have its quantity silently bumped on-screen with no
            // matching change on the paper the kitchen is holding.
            OrderItemEntity mergeTarget = existing.stream()
                    .filter(i -> i.getSentAt() == null)
                    .filter(i -> i.getMenuItemId().equals(menuItem.getId()))
                    .filter(i -> Objects.equals(normalizeNote(i.getNote()), note))
                    .findFirst()
                    .orElse(null);

            if (mergeTarget != null) {
                mergeTarget.setQuantity(mergeTarget.getQuantity() + input.getQuantity());
                orderItemRepository.save(mergeTarget);
            } else {
                OrderItemEntity item = new OrderItemEntity();
                item.setOrderId(order.getId());
                item.setMenuItemId(menuItem.getId());
                item.setQuantity(input.getQuantity());
                item.setUnitPrice(menuItem.getPrice());
                item.setNote(note);
                existing.add(orderItemRepository.save(item));
            }
        }

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(id);
        order.setTotal(sumTotal(items));
        OrderEntity saved = orderRepository.saveAndFlush(order);

        // The earliest point an order can actually be a candidate for the spa auto-link's table
        // axis - see that method's own javadoc for why creation is structurally too early, and
        // why the booking axis runs from #close instead, not here.
        autoLinkSpaAppointmentByTable(saved, items);

        // The order was already dispatched at least once - there's no "send" button left for
        // staff to press for this round, so the ticket for whatever just became unsent goes out
        // now, not never. See openapi.yaml's addOrderItems description.
        if (saved.getStatus() == OrderStatus.SENT) {
            dispatchUnsentTickets(saved);
            items = orderItemRepository.findByOrderId(id);
        }

        return toDto(saved, items);
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
        return toDto(saved, items);
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
        return toDto(saved, items);
    }

    /**
     * {@code POST /orders/{id}/items/{itemId}/void} - see its openapi.yaml description. The voided
     * quantity leaves {@code OrderItem} (so the total, the ledger posting at close, Z410 and the
     * spa completeness check never see it) and is kept as an {@link OrderItemVoidEntity} with who,
     * when and why. MANAGER+ is enforced in {@code SecurityConfig}.
     */
    @Transactional
    public Order voidItem(String id, String itemId, OrderItemVoidInput input, String userId) {
        OrderEntity order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Order not found"));
        if (!ADDABLE_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Order is not open or sent");
        }
        OrderItemEntity item = orderItemRepository
                .findByIdAndOrderId(itemId, id)
                .orElseThrow(() -> new NotFoundException("Line item not found"));
        if (item.getSentAt() == null) {
            throw new ConflictException("This line hasn't been sent yet - change or remove it instead of voiding it.");
        }
        String reason = input.getReason() != null ? input.getReason().trim() : "";
        if (reason.isEmpty()) {
            throw ValidationException.field("reason", "A reason is required to void a sent item.");
        }
        Integer requested = input.getQuantity().orElse(null);
        int quantity = requested != null ? requested : item.getQuantity();
        if (quantity < 1 || quantity > item.getQuantity()) {
            throw ValidationException.field("quantity", "must be between 1 and the line's quantity (" + item.getQuantity() + ")");
        }

        OrderItemVoidEntity voided = new OrderItemVoidEntity();
        voided.setOrderId(id);
        voided.setMenuItemId(item.getMenuItemId());
        voided.setQuantity(quantity);
        voided.setUnitPrice(item.getUnitPrice());
        voided.setNote(item.getNote());
        voided.setSentAt(item.getSentAt());
        voided.setReason(reason);
        voided.setVoidedByUserId(userId);
        orderItemVoidRepository.save(voided);

        if (quantity == item.getQuantity()) {
            orderItemRepository.delete(item);
        } else {
            item.setQuantity(item.getQuantity() - quantity);
            orderItemRepository.save(item);
        }
        orderItemRepository.flush();

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(id);
        order.setTotal(sumTotal(items));
        OrderEntity saved = orderRepository.saveAndFlush(order);

        MenuItemEntity menuItem = menuItemRepository.findById(voided.getMenuItemId()).orElse(null);
        String itemName = menuItem != null ? menuItem.getName() : voided.getMenuItemId();
        orderPrintingService.printVoidTicket(saved, voided, menuItem);
        auditLogService.record(
                AuditAction.ORDER_ITEM_VOIDED,
                AuditEntityType.ORDER,
                saved.getId(),
                "Voided " + quantity + "x " + itemName + " ("
                        + voided.getUnitPrice().multiply(BigDecimal.valueOf(quantity)) + ") from order #" + saved.getNumber()
                        + " after it was sent. Reason: " + reason);
        return toDto(saved, items);
    }

    @Transactional
    public Order close(String id, CloseOrderInput input, String cashierUserId) {
        // Locked so a concurrent void (see #voidItem) can't change the total between reading it
        // here and charging it below.
        OrderEntity order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Order not found"));
        if (CLOSED_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Order is already " + order.getStatus().getValue().toLowerCase());
        }

        // Nothing to charge: a ฿0 PAID order is noise in the history and the cash report, and it
        // is how an opened-and-abandoned table used to end up "paid". Cancel it instead.
        if (orderItemRepository.findByOrderId(id).isEmpty()) {
            throw new ConflictException("This order has no items - add something or cancel the order.");
        }

        ShiftEntity shift = shiftRepository
                .findByOpenedByUserIdAndStatus(cashierUserId, ShiftStatus.OPEN)
                .orElseThrow(() -> new ConflictException("You don't have an open shift"));

        BigDecimal tendered = parseTendered(input, order.getTotal());

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
        // Same transaction, not best-effort like the printing/audit around it - see LedgerService.
        ledgerService.postPosOrderClose(saved, items, input.getMethod(), cashierUserId);
        orderPrintingService.printGuestReceipt(saved, items, payment, booking);

        // Best-effort, same fail-open contract as printing/audit above and below - closing an
        // order is a money operation, this is inference sitting next to it, and a resolution
        // failure must never affect the close itself. See #autoLinkSpaAppointmentByBooking's own
        // javadoc for why this is the one moment this axis can run at all.
        if (booking != null) {
            try {
                autoLinkSpaAppointmentByBooking(saved, items, booking.getId());
            } catch (Exception e) {
                log.error("autoLinkSpaAppointmentByBooking failed for order {}", saved.getId(), e);
            }
        }

        auditLogService.record(
                AuditAction.ORDER_CLOSED,
                AuditEntityType.ORDER,
                saved.getId(),
                "Order closed with " + input.getMethod().getValue() + " payment of " + saved.getTotal()
                        + (tendered != null ? " (tendered " + tendered + ", change " + tendered.subtract(saved.getTotal()) + ")" : "")
                        + (booking != null ? " (charged to " + booking.getGuestName() + "'s room)" : ""));
        if (booking != null) {
            auditLogService.record(
                    AuditAction.ROOM_CHARGE_POSTED,
                    AuditEntityType.BOOKING,
                    booking.getId(),
                    "Room charge of " + saved.getTotal() + " posted to " + booking.getGuestName() + "'s folio from order " + saved.getId());
        }

        return toDto(saved, items);
    }

    /**
     * {@code CloseOrderInput.amountTendered} - what the guest handed over, as counted in the cash
     * dialog. Only ever checked and recorded, never charged: the payment is {@code Order.total}.
     */
    private static BigDecimal parseTendered(CloseOrderInput input, BigDecimal total) {
        String raw = input.getAmountTendered();
        if (raw == null) {
            return null;
        }
        if (input.getMethod() != PaymentMethod.CASH) {
            throw ValidationException.field("amountTendered", "only applies to a CASH payment");
        }
        BigDecimal tendered = new BigDecimal(raw);
        if (tendered.compareTo(total) < 0) {
            throw ValidationException.field("amountTendered", "is less than the order total (" + total + ")");
        }
        return tendered;
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
        return toDto(saved, orderItemRepository.findByOrderId(id));
    }

    /**
     * Prints a kitchen/bar ticket for every currently-unsent line on the order (department-split,
     * same as the original send) and marks them {@code sentAt}, regardless of whether the print
     * itself reached the printer - same fail-open contract as everywhere else in
     * {@link OrderPrintingService}. Called both by the original {@code OPEN -> SENT} transition
     * and by {@link #addItems} when items are added to an order that's already {@code SENT}, so a
     * re-order ticket only ever carries the delta, never a repeat of lines already printed.
     */
    private void dispatchUnsentTickets(OrderEntity order) {
        List<OrderItemEntity> unsent =
                orderItemRepository.findByOrderId(order.getId()).stream().filter(i -> i.getSentAt() == null).toList();
        if (unsent.isEmpty()) {
            return;
        }
        orderPrintingService.printTickets(order, unsent);
        LocalDateTime now = LocalDateTime.now();
        for (OrderItemEntity item : unsent) {
            item.setSentAt(now);
        }
        orderItemRepository.saveAll(unsent);
    }

    /**
     * 24 random bytes, URL-safe Base64, no padding - short enough for a QR code and a URL query
     * parameter, long enough that guessing one is not a practical attack. Generated once, here,
     * for every order (not just table orders - see {@code Order.guestAccessToken}'s own
     * openapi.yaml description for why); never regenerated afterward.
     */
    private String generateGuestAccessToken() {
        byte[] bytes = new byte[24];
        guestAccessTokenRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * The one gate every {@code PublicOrderingApi} operation calls first - a plain 404 for all
     * three failure reasons (order doesn't exist, token null/blank/mismatched,
     * or {@link #ADDABLE_STATUSES} no longer contains the order's status), never distinguished
     * from each other, so a guessing attempt learns nothing about which one applies. Note this
     * uses {@code ADDABLE_STATUSES}, not {@code CLOSED_STATUSES} negated - the same set that
     * gates {@link #addItems} itself, so a guest can never reach a state {@code addItems} would
     * reject anyway.
     */
    @Transactional(readOnly = true)
    public OrderEntity requireGuestAccess(String orderId, String token) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order == null
                || token == null
                || token.isBlank()
                || !token.equals(order.getGuestAccessToken())
                || !ADDABLE_STATUSES.contains(order.getStatus())) {
            throw new NotFoundException("Order not found");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public GuestOrderView getGuestOrderView(String orderId, String token) {
        OrderEntity order = requireGuestAccess(orderId, token);
        return guestOrderMapper.toDto(order, orderItemRepository.findByOrderId(orderId), orderPrintingService.describeLocation(order));
    }

    /** Delegates straight into {@link #addItems} - never a second implementation of its merge/print-dispatch logic. */
    @Transactional
    public GuestOrderView addGuestItems(String orderId, String token, List<OrderItemInput> inputs) {
        requireGuestAccess(orderId, token);
        addItems(orderId, inputs);
        return getGuestOrderView(orderId, token);
    }

    /** Blank and {@code null} are the same "no note" for merge-matching purposes. */
    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
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
        // One batched lookup each, not one query per row - same pattern as itemsByOrderId above.
        Map<String, PaymentEntity> paymentByOrderId = paymentRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(PaymentEntity::getOrderId, p -> p));
        Map<String, List<OrderItemVoidEntity>> voidsByOrderId = orderItemVoidRepository.findByOrderIdInOrderByVoidedAtAsc(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemVoidEntity::getOrderId));
        Map<String, String> spaAppointmentIdByOrderId = spaAppointmentIdsByOrderId(orderIds);
        // Room-service orders (Order.openedByUserId == null - see that field's own openapi.yaml
        // description) are filtered out in emailsFor: JpaRepository#findAllById rejects a null id
        // in the list outright, and there's no row to look up for one anyway.
        Map<String, String> emailsById = emailsFor(Stream.concat(
                orders.stream().map(OrderEntity::getOpenedByUserId),
                voidsByOrderId.values().stream().flatMap(List::stream).map(OrderItemVoidEntity::getVoidedByUserId)));
        return orders.stream()
                .map(o -> orderMapper.toDto(
                        o,
                        itemsByOrderId.getOrDefault(o.getId(), List.of()),
                        new OrderMapper.Extras(
                                o.getOpenedByUserId() == null
                                        ? GUEST_ROOM_SERVICE_LABEL
                                        : emailsById.getOrDefault(o.getOpenedByUserId(), o.getOpenedByUserId()),
                                paymentByOrderId.get(o.getId()),
                                voidsByOrderId.getOrDefault(o.getId(), List.of()),
                                emailsById,
                                spaAppointmentIdByOrderId.get(o.getId()))))
                .toList();
    }

    /** The single-order counterpart of {@link #toDtos} - every write path returns through this. */
    private Order toDto(OrderEntity order, List<OrderItemEntity> items) {
        PaymentEntity payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        List<OrderItemVoidEntity> voids = orderItemVoidRepository.findByOrderIdOrderByVoidedAtAsc(order.getId());
        return orderMapper.toDto(
                order,
                items,
                new OrderMapper.Extras(
                        resolveEmail(order.getOpenedByUserId()),
                        payment,
                        voids,
                        emailsFor(voids.stream().map(OrderItemVoidEntity::getVoidedByUserId)),
                        spaAppointmentIdsByOrderId(List.of(order.getId())).get(order.getId())));
    }

    private Map<String, String> emailsFor(Stream<String> userIds) {
        List<String> ids = userIds.filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
    }

    /** {@code Order.spaAppointmentId}. Should more than one appointment ever point at one order, the first wins. */
    private Map<String, String> spaAppointmentIdsByOrderId(Collection<String> orderIds) {
        Map<String, String> result = new HashMap<>();
        for (SpaAppointmentEntity appointment : spaAppointmentRepository.findByOrderIdIn(orderIds)) {
            result.putIfAbsent(appointment.getOrderId(), appointment.getId());
        }
        return result;
    }

    /**
     * Falls back to the raw id if the user was since deleted - same convention as {@code
     * OrderPrintingService.resolveWaiterLabel}. {@code null} exactly for a room-service order
     * (see {@code Order.openedByUserId}'s own openapi.yaml description) - a guest has no
     * {@code User} row to look up, so this returns a fixed human label instead of calling the
     * repository with a null id (which {@code JpaRepository#findById} rejects outright).
     */
    private String resolveEmail(String userId) {
        if (userId == null) {
            return GUEST_ROOM_SERVICE_LABEL;
        }
        return userRepository.findById(userId).map(UserEntity::getEmail).orElse(userId);
    }

    /** Shared by {@link #list}. */
    private static Specification<OrderEntity> buildSpecification(
            OrderStatus status, String tableId, LocalDate from, LocalDate to, OrderDateBasis dateBasis, List<String> shiftOrderIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (tableId != null) {
                predicates.add(cb.equal(root.get("tableId"), tableId));
            }
            if ((from != null || to != null) && dateBasis == OrderDateBasis.CLOSED) {
                // Order.closedAt isn't a column: it's the Payment's time for PAID and the last
                // update (the cancellation itself) for CANCELLED. OPEN/SENT have no close date.
                Subquery<String> paidInRange = query.subquery(String.class);
                Root<PaymentEntity> payment = paidInRange.from(PaymentEntity.class);
                List<Predicate> paymentPredicates = new ArrayList<>();
                paymentPredicates.add(cb.equal(payment.get("orderId"), root.get("id")));
                List<Predicate> cancelledPredicates = new ArrayList<>();
                cancelledPredicates.add(cb.equal(root.get("status"), OrderStatus.CANCELLED));
                if (from != null) {
                    paymentPredicates.add(cb.greaterThanOrEqualTo(payment.get("createdAt"), from.atStartOfDay()));
                    cancelledPredicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), from.atStartOfDay()));
                }
                if (to != null) {
                    paymentPredicates.add(cb.lessThan(payment.get("createdAt"), to.plusDays(1).atStartOfDay()));
                    cancelledPredicates.add(cb.lessThan(root.get("updatedAt"), to.plusDays(1).atStartOfDay()));
                }
                paidInRange.select(payment.get("orderId")).where(paymentPredicates.toArray(new Predicate[0]));
                predicates.add(cb.or(
                        cb.and(cb.equal(root.get("status"), OrderStatus.PAID), cb.exists(paidInRange)),
                        cb.and(cancelledPredicates.toArray(new Predicate[0]))));
            } else {
                if (from != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
                }
                if (to != null) {
                    predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
                }
            }
            if (shiftOrderIds != null) {
                predicates.add(root.get("id").in(shiftOrderIds));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
