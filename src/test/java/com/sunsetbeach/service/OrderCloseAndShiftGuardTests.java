package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftCloseInput;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): covers three of the six POS
 * review findings -
 *   (1) ShiftService.close()'s order guard is genuinely system-wide, not scoped to the shift
 *       being closed - an unrelated order anywhere blocks it, and cancelling that order lifts
 *       the block;
 *   (2) OrderService.close() always charges Order.total, never a client-supplied amount
 *       (CloseOrderInput no longer even has an amount field to send);
 *   (3) POST /orders/{id}/items works on a SENT order (not just OPEN) and recomputes total,
 *       while PATCH/DELETE on an existing line still require OPEN.
 */
@SpringBootTest
@Transactional
class OrderCloseAndShiftGuardTests extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity cashier;
    private MenuItemEntity menuItem;

    @BeforeEach
    void setUp() {
        UserEntity newUser = new UserEntity();
        newUser.setEmail("cashier-" + UUID.randomUUID() + "@example.com");
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.CASHIER);
        cashier = userRepository.saveAndFlush(newUser);

        MenuItemEntity newMenuItem = new MenuItemEntity();
        newMenuItem.setName("Coke");
        newMenuItem.setDescription("Soft drink");
        newMenuItem.setCategory("Drinks");
        newMenuItem.setPrice(new BigDecimal("100.00"));
        newMenuItem.setAvailable(true);
        menuItem = menuItemRepository.saveAndFlush(newMenuItem);
    }

    private Order openOrderWithOneItem(int quantity) {
        Order order = orderService.create(new OrderCreateInput(), cashier.getId());
        return orderService.addItems(order.getId(), List.of(new OrderItemInput(menuItem.getId(), quantity)));
    }

    // --- Issue 1: shift-close guard is system-wide ---

    @Test
    void closeShift_blockedByUnrelatedOpenOrderAnywhereInTheSystem() {
        Shift shift = shiftService.open(cashier.getId(), new ShiftOpenInput());
        // Deliberately unrelated to this shift/cashier: a bare OPEN order opened by nobody in
        // particular, just to prove the guard isn't scoped to this shift.
        Order unrelatedOrder = orderService.create(new OrderCreateInput(), cashier.getId());
        assertThat(unrelatedOrder.getStatus()).isEqualTo(OrderStatus.OPEN);

        assertThatThrownBy(() -> shiftService.close(shift.getId(), new ShiftCloseInput())).isInstanceOf(ConflictException.class);

        orderService.cancel(unrelatedOrder.getId());

        Shift closed = shiftService.close(shift.getId(), new ShiftCloseInput());
        assertThat(closed.getId()).isEqualTo(shift.getId());
    }

    // --- Issue 2: Payment.amount is always Order.total, never client-supplied ---

    @Test
    void closeOrder_paymentAmountAlwaysEqualsOrderTotal() {
        Shift shift = shiftService.open(cashier.getId(), new ShiftOpenInput());
        Order order = openOrderWithOneItem(3); // 3 x 100.00 = 300.00
        assertThat(order.getTotal()).isEqualTo("300.00");

        Order closed = orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH), cashier.getId());

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.PAID);
        PaymentEntity payment = paymentRepository.findByShiftId(shift.getId()).stream()
                .filter(p -> p.getOrderId().equals(order.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    // --- Issue 6: adding items is allowed on SENT, editing/removing existing lines is not ---

    @Test
    void addItems_onSentOrder_succeedsAndRecomputesTotal() {
        Order order = openOrderWithOneItem(1); // 1 x 100.00 = 100.00
        markSent(order.getId());

        Order updated = orderService.addItems(order.getId(), List.of(new OrderItemInput(menuItem.getId(), 2)));

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(updated.getTotal()).isEqualTo("300.00"); // (1 + 2) x 100.00
        assertThat(updated.getItems()).hasSize(2);
    }

    @Test
    void updateItem_onSentOrder_isStillBlocked() {
        Order order = openOrderWithOneItem(1);
        markSent(order.getId());
        String itemId = order.getItems().get(0).getId();

        assertThatThrownBy(() -> orderService.updateItem(order.getId(), itemId, new OrderItemInput(menuItem.getId(), 5)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteItem_onSentOrder_isStillBlocked() {
        Order order = openOrderWithOneItem(1);
        markSent(order.getId());
        String itemId = order.getItems().get(0).getId();

        assertThatThrownBy(() -> orderService.deleteItem(order.getId(), itemId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void addItems_onPaidOrder_isBlocked() {
        Shift shift = shiftService.open(cashier.getId(), new ShiftOpenInput());
        Order order = openOrderWithOneItem(1);
        orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH), cashier.getId());

        assertThatThrownBy(() -> orderService.addItems(order.getId(), List.of(new OrderItemInput(menuItem.getId(), 1))))
                .isInstanceOf(ConflictException.class);
    }

    /**
     * A raw-repository shortcut standing in for {@code PATCH /orders/{id}} {@code status: SENT}
     * (which additionally dispatches tickets and is covered end to end in {@link PrintingTests}) -
     * marks every current line {@code sentAt} too, same as the real transition, so that a
     * subsequent {@link OrderService#addItems} call in these tests sees a genuinely already-
     * dispatched order (an unsent-line merge target would be a fixture bug, not the behavior
     * under test).
     */
    private void markSent(String orderId) {
        OrderEntity entity = orderRepository.findById(orderId).orElseThrow();
        entity.setStatus(OrderStatus.SENT);
        orderRepository.saveAndFlush(entity);

        LocalDateTime now = LocalDateTime.now();
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(orderId);
        items.forEach(item -> item.setSentAt(now));
        orderItemRepository.saveAll(items);
    }
}
