package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.GuestOrderView;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderRepository;
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
 * DB-backed coverage of {@code OrderService#requireGuestAccess} - the single gate every
 * {@code PublicOrderingApi} operation calls first. The point of this gate is that a wrong token,
 * an unknown order, and an order that's left OPEN/SENT are all indistinguishable 404s - so these
 * tests focus on the gate itself, not on re-proving {@code addItems}'s merge/print-dispatch
 * behavior (already covered by {@code OrderItemMergingTests} and friends).
 */
@SpringBootTest
@Transactional
class GuestOrderAccessTests extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftService shiftService;

    private UserEntity waiter;
    private UserEntity cashier;
    private MenuItemEntity mojito;

    @BeforeEach
    void setUp() {
        waiter = saveUser(Role.WAITER);
        cashier = saveUser(Role.CASHIER);

        mojito = new MenuItemEntity();
        mojito.setName("Mojito");
        mojito.setDescription("test item");
        mojito.setCategory("Test");
        mojito.setDepartment(MenuDepartment.BAR);
        mojito.setPrice(new BigDecimal("150.00"));
        mojito.setAvailable(true);
        mojito = menuItemRepository.saveAndFlush(mojito);
    }

    private UserEntity saveUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void wrongToken_getView_isNotFound() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());

        assertThatThrownBy(() -> orderService.getGuestOrderView(order.getId(), "not-the-real-token"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void missingToken_getView_isNotFound() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());

        assertThatThrownBy(() -> orderService.getGuestOrderView(order.getId(), null)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> orderService.getGuestOrderView(order.getId(), "")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> orderService.getGuestOrderView(order.getId(), "   ")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownOrderId_isNotFound() {
        assertThatThrownBy(() -> orderService.getGuestOrderView("does-not-exist", "any-token")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void wrongToken_addItems_isNotFound() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());

        assertThatThrownBy(() -> orderService.addGuestItems(order.getId(), "wrong-token", List.of(new OrderItemInput(mojito.getId(), 1))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rightToken_orderPaid_getView_isNotFound() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());
        String token = order.getGuestAccessToken().get();
        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));
        openShiftFor(cashier);
        orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH), cashier.getId());

        assertThatThrownBy(() -> orderService.getGuestOrderView(order.getId(), token)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rightToken_orderCancelled_getView_isNotFound() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());
        String token = order.getGuestAccessToken().get();
        orderService.cancel(order.getId());

        assertThatThrownBy(() -> orderService.getGuestOrderView(order.getId(), token)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rightToken_orderPaid_addItems_isNotFound() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());
        String token = order.getGuestAccessToken().get();
        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));
        openShiftFor(cashier);
        orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH), cashier.getId());

        assertThatThrownBy(() -> orderService.addGuestItems(order.getId(), token, List.of(new OrderItemInput(mojito.getId(), 1))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rightToken_orderOpen_getView_succeeds() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());
        String token = order.getGuestAccessToken().get();
        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 2)));

        GuestOrderView view = orderService.getGuestOrderView(order.getId(), token);

        assertThat(view.getId()).isEqualTo(order.getId());
        assertThat(view.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(view.getItems()).hasSize(1);
        assertThat(view.getItems().get(0).getName()).isEqualTo("Mojito");
        assertThat(view.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(view.getTotal()).isEqualTo("300.00");
    }

    @Test
    void rightToken_orderSent_getView_succeeds() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());
        String token = order.getGuestAccessToken().get();
        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));
        orderService.update(order.getId(), new com.sunsetbeach.model.OrderUpdateInput().status(OrderStatus.SENT));

        GuestOrderView view = orderService.getGuestOrderView(order.getId(), token);

        assertThat(view.getStatus()).isEqualTo(OrderStatus.SENT);
    }

    @Test
    void rightToken_orderOpen_addItems_succeeds_andBehavesLikeStaffAddItems() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());
        String token = order.getGuestAccessToken().get();

        GuestOrderView view = orderService.addGuestItems(order.getId(), token, List.of(new OrderItemInput(mojito.getId(), 1)));
        // A second add of the same item merges into the existing unsent line, same as the staff
        // path (OrderItemMergingTests) - proves this delegates into the real addItems rather than
        // reimplementing it.
        view = orderService.addGuestItems(order.getId(), token, List.of(new OrderItemInput(mojito.getId(), 2)));

        assertThat(view.getItems()).hasSize(1);
        assertThat(view.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(view.getTotal()).isEqualTo("450.00");
    }

    @Test
    void everyNewOrder_getsAUniqueNonNullGuestAccessToken() {
        Order first = orderService.create(new OrderCreateInput(), waiter.getId());
        Order second = orderService.create(new OrderCreateInput(), waiter.getId());

        assertThat(first.getGuestAccessToken().get()).isNotBlank();
        assertThat(second.getGuestAccessToken().get()).isNotBlank();
        assertThat(first.getGuestAccessToken().get()).isNotEqualTo(second.getGuestAccessToken().get());

        OrderEntity persisted = orderRepository.findById(first.getId()).orElseThrow();
        assertThat(persisted.getGuestAccessToken()).isEqualTo(first.getGuestAccessToken().get());
    }

    private void openShiftFor(UserEntity user) {
        shiftService.open(user.getId(), new ShiftOpenInput());
    }
}
