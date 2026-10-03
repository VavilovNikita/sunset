package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * The POS money guards: an order is created together with its first line (so tapping a free table
 * never leaves an empty order holding it), an empty order can't be closed into a ฿0 payment, and
 * the cash dialog's tendered amount is checked but never charged.
 */
@SpringBootTest
@Transactional
class OrderEmptyAndCashTenderedTests extends AbstractIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private ShiftService shiftService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private UserRepository userRepository;

    private UserEntity cashier;
    private MenuItemEntity menuItem;

    @BeforeEach
    void setUp() {
        UserEntity newUser = new UserEntity();
        newUser.setEmail("cashier-" + UUID.randomUUID() + "@example.com");
        newUser.setName(newUser.getEmail());
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

        shiftService.open(cashier.getId(), new ShiftOpenInput());
    }

    @Test
    void create_withItems_addsThemInTheSameCall_atTheMenuPrice() {
        Order order = orderService.create(new OrderCreateInput().items(List.of(new OrderItemInput(menuItem.getId(), 2))), cashier.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(order.getItems()).hasSize(1);
        assertThat(new BigDecimal(order.getTotal())).isEqualByComparingTo("200.00");
    }

    @Test
    void close_anEmptyOrder_isRefused_andNoPaymentIsRecorded() {
        Order empty = orderService.create(new OrderCreateInput(), cashier.getId());

        assertThatThrownBy(() -> orderService.close(empty.getId(), new CloseOrderInput(PaymentMethod.CASH), cashier.getId()))
                .isInstanceOf(ConflictException.class);
        assertThat(paymentRepository.findByOrderId(empty.getId())).isEmpty();
        assertThat(orderService.getById(empty.getId()).getStatus()).isEqualTo(OrderStatus.OPEN);
    }

    @Test
    void close_cashWithEnoughTendered_chargesTheTotalNotTheTenderedAmount() {
        Order order = orderService.create(new OrderCreateInput().items(List.of(new OrderItemInput(menuItem.getId(), 1))), cashier.getId());

        Order closed = orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH).amountTendered("500"), cashier.getId());

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void close_cashWithLessThanTheTotalTendered_isRefused() {
        Order order = orderService.create(new OrderCreateInput().items(List.of(new OrderItemInput(menuItem.getId(), 1))), cashier.getId());

        assertThatThrownBy(() -> orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH).amountTendered("99.99"), cashier.getId()))
                .isInstanceOf(ValidationException.class);
        assertThat(paymentRepository.findByOrderId(order.getId())).isEmpty();
    }

    @Test
    void close_tenderedWithANonCashMethod_isRefused() {
        Order order = orderService.create(new OrderCreateInput().items(List.of(new OrderItemInput(menuItem.getId(), 1))), cashier.getId());

        assertThatThrownBy(() -> orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CARD).amountTendered("100"), cashier.getId()))
                .isInstanceOf(ValidationException.class);
    }
}
