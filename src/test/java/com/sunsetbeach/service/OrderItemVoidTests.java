package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCancelInput;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderItemVoid;
import com.sunsetbeach.model.OrderItemVoidInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.OrderUpdateInput;
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
 * Voiding a line that already went to the kitchen/bar: the voided quantity leaves the order's
 * lines and total (so the close charges, and the ledger posts, only what's left) and is kept on
 * {@code Order.voids} with the reason and who did it. A wrong number here is silent - the guest is
 * charged for something they sent back, or the void quietly hides a line that was paid for.
 */
@SpringBootTest
@Transactional
class OrderItemVoidTests extends AbstractIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private ShiftService shiftService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private UserRepository userRepository;

    private UserEntity waiter;
    private UserEntity manager;
    private MenuItemEntity beer;
    private MenuItemEntity burger;

    @BeforeEach
    void setUp() {
        waiter = persistUser(Role.CASHIER);
        manager = persistUser(Role.MANAGER);
        beer = persistMenuItem("Beer", "120.00");
        burger = persistMenuItem("Burger", "250.00");
        shiftService.open(waiter.getId(), new ShiftOpenInput());
    }

    private UserEntity persistUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("void-" + role.getValue().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private MenuItemEntity persistMenuItem(String name, String price) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName(name);
        item.setDescription("");
        item.setCategory("Void tests");
        item.setPrice(new BigDecimal(price));
        item.setAvailable(true);
        return menuItemRepository.saveAndFlush(item);
    }

    /** 3 beers + 1 burger, sent to the kitchen/bar - a total of 610.00. */
    private Order sentOrder() {
        Order order = orderService.create(
                new OrderCreateInput().items(List.of(new OrderItemInput(beer.getId(), 3), new OrderItemInput(burger.getId(), 1))),
                waiter.getId());
        return orderService.update(order.getId(), new OrderUpdateInput().status(OrderStatus.SENT));
    }

    private static String lineId(Order order, MenuItemEntity menuItem) {
        return order.getItems().stream().filter(i -> i.getMenuItemId().equals(menuItem.getId())).findFirst().orElseThrow().getId();
    }

    @Test
    void voidingAWholeSentLine_removesItFromTheTotal_andRecordsWhoAndWhy() {
        Order order = sentOrder();

        Order after = orderService.voidItem(
                order.getId(), lineId(order, burger), new OrderItemVoidInput("Guest changed their mind"), manager.getId());

        assertThat(new BigDecimal(after.getTotal())).isEqualByComparingTo("360.00");
        assertThat(after.getItems()).extracting(i -> i.getMenuItemId()).containsExactly(beer.getId());
        assertThat(after.getVoids()).hasSize(1);
        OrderItemVoid voided = after.getVoids().get(0);
        assertThat(voided.getMenuItemId()).isEqualTo(burger.getId());
        assertThat(voided.getQuantity()).isEqualTo(1);
        assertThat(new BigDecimal(voided.getUnitPrice())).isEqualByComparingTo("250.00");
        assertThat(voided.getReason()).isEqualTo("Guest changed their mind");
        assertThat(voided.getVoidedByUserId()).isEqualTo(manager.getId());
        assertThat(voided.getVoidedByEmail()).isEqualTo(manager.getEmail());
        // Read back through the list path too, which batches the same data differently.
        Order listed = orderService.list(null, null, null, null, null, null).stream()
                .filter(o -> o.getId().equals(order.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(listed.getVoids()).extracting(OrderItemVoid::getReason).containsExactly("Guest changed their mind");
        assertThat(new BigDecimal(listed.getTotal())).isEqualByComparingTo("360.00");
    }

    @Test
    void voidingPartOfASentLine_keepsTheRest() {
        Order order = sentOrder();

        Order after = orderService.voidItem(
                order.getId(), lineId(order, beer), new OrderItemVoidInput("Spilled").quantity(1), manager.getId());

        assertThat(after.getItems().stream().filter(i -> i.getMenuItemId().equals(beer.getId())).findFirst().orElseThrow().getQuantity())
                .isEqualTo(2);
        assertThat(new BigDecimal(after.getTotal())).isEqualByComparingTo("490.00");
        assertThat(after.getVoids()).extracting(OrderItemVoid::getQuantity).containsExactly(1);
    }

    @Test
    void closingAfterAVoid_chargesOnlyWhatIsLeft() {
        Order order = sentOrder();
        orderService.voidItem(order.getId(), lineId(order, burger), new OrderItemVoidInput("Wrong table"), manager.getId());

        orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CARD), waiter.getId());

        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getAmount()).isEqualByComparingTo("360.00");
    }

    @Test
    void anUnsentLine_isNotVoided_itIsEditedInstead() {
        Order order = orderService.create(new OrderCreateInput().items(List.of(new OrderItemInput(beer.getId(), 1))), waiter.getId());

        assertThatThrownBy(() -> orderService.voidItem(
                        order.getId(), lineId(order, beer), new OrderItemVoidInput("Not needed"), manager.getId()))
                .isInstanceOf(ConflictException.class);
        assertThat(orderService.getById(order.getId()).getVoids()).isEmpty();
    }

    @Test
    void aBlankReason_isRefused_andNothingChanges() {
        Order order = sentOrder();

        assertThatThrownBy(() -> orderService.voidItem(order.getId(), lineId(order, beer), new OrderItemVoidInput("   "), manager.getId()))
                .isInstanceOf(ValidationException.class);
        Order unchanged = orderService.getById(order.getId());
        assertThat(new BigDecimal(unchanged.getTotal())).isEqualByComparingTo("610.00");
        assertThat(unchanged.getVoids()).isEmpty();
    }

    @Test
    void voidingMoreThanTheLineHolds_isRefused() {
        Order order = sentOrder();

        assertThatThrownBy(() -> orderService.voidItem(
                        order.getId(), lineId(order, beer), new OrderItemVoidInput("Too many").quantity(4), manager.getId()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void aPaidOrder_isNotVoided() {
        Order order = sentOrder();
        orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH), waiter.getId());

        assertThatThrownBy(() -> orderService.voidItem(
                        order.getId(), lineId(order, beer), new OrderItemVoidInput("After the fact"), manager.getId()))
                .isInstanceOf(ConflictException.class);
    }

    private static OrderCancelInput because(String reason) {
        return new OrderCancelInput().reason(reason);
    }

    @Test
    void cancellingASentOrder_isAManagersVoid_notAWaitersTap() {
        Order order = sentOrder();

        // Waiter and cashier are refused whatever they write as the reason; nothing changes.
        assertThatThrownBy(() -> orderService.cancel(order.getId(), because("Guest left"), waiter.getId(), Role.WAITER))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> orderService.cancel(order.getId(), because("Guest left"), waiter.getId(), Role.CASHIER))
                .isInstanceOf(ForbiddenException.class);
        assertThat(orderService.getById(order.getId()).getStatus()).isEqualTo(OrderStatus.SENT);

        // A manager still needs a reason - none, or only spaces, is a 400.
        assertThatThrownBy(() -> orderService.cancel(order.getId(), null, manager.getId(), Role.MANAGER)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> orderService.cancel(order.getId(), because("   "), manager.getId(), Role.MANAGER))
                .isInstanceOf(ValidationException.class);
        assertThat(orderService.getById(order.getId()).getStatus()).isEqualTo(OrderStatus.SENT);

        Order after = orderService.cancel(order.getId(), because("  Table walked out "), manager.getId(), Role.MANAGER);

        assertThat(after.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(after.getItems()).isEmpty();
        assertThat(new BigDecimal(after.getTotal())).isEqualByComparingTo("0.00");
        assertThat(after.getVoids()).extracting(OrderItemVoid::getMenuItemId).containsExactlyInAnyOrder(beer.getId(), burger.getId());
        assertThat(after.getVoids()).extracting(OrderItemVoid::getQuantity).containsExactlyInAnyOrder(3, 1);
        assertThat(after.getVoids()).allSatisfy(v -> {
            assertThat(v.getReason()).isEqualTo("Table walked out");
            assertThat(v.getVoidedByUserId()).isEqualTo(manager.getId());
        });
    }

    @Test
    void cancellingASentOrder_voidsALaterRoundToo() {
        Order order = sentOrder();
        // A round added to an already-sent order goes to the kitchen at once, so it is on paper too.
        orderService.addItems(order.getId(), List.of(new OrderItemInput(beer.getId(), 2)));

        Order after = orderService.cancel(order.getId(), because("Guest left"), manager.getId(), Role.ADMIN);

        assertThat(after.getItems()).isEmpty();
        assertThat(after.getVoids()).extracting(OrderItemVoid::getQuantity).containsExactlyInAnyOrder(3, 1, 2);
    }

    @Test
    void cancellingAnOrderNothingWasSentFrom_staysAWaitersCall_andNeedsNoReason() {
        Order order = orderService.create(new OrderCreateInput().items(List.of(new OrderItemInput(beer.getId(), 1))), waiter.getId());

        Order after = orderService.cancel(order.getId(), null, waiter.getId(), Role.WAITER);

        assertThat(after.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(after.getVoids()).isEmpty();
        // Unchanged behaviour: the lines stay on the cancelled order.
        assertThat(after.getItems()).hasSize(1);
    }

    @Test
    void aCancelledOrder_cannotBeCancelledAgain() {
        Order order = sentOrder();
        orderService.cancel(order.getId(), because("Guest left"), manager.getId(), Role.MANAGER);

        assertThatThrownBy(() -> orderService.cancel(order.getId(), because("Again"), manager.getId(), Role.MANAGER))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void ordersGetIncreasingReceiptNumbers() {
        Order first = orderService.create(new OrderCreateInput().items(List.of(new OrderItemInput(beer.getId(), 1))), waiter.getId());
        Order second = orderService.create(new OrderCreateInput().items(List.of(new OrderItemInput(beer.getId(), 1))), waiter.getId());

        assertThat(first.getNumber()).isNotNull();
        assertThat(second.getNumber()).isGreaterThan(first.getNumber());
        assertThat(orderService.getById(first.getId()).getNumber()).isEqualTo(first.getNumber());
    }
}
