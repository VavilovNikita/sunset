package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItem;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderUpdateInput;
import com.sunsetbeach.model.Role;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): {@code POST /orders/{id}/items}
 * merges a repeated add into the existing unsent line instead of appending a new row (covers
 * "tap the same item three times" leaving one line with quantity 3, not three lines of 1) -
 * except when the notes differ, or when the matching line already went out on a printed ticket
 * ({@code OrderItem.sentAt} set), where merging would silently desync the screen from what the
 * kitchen is already holding.
 */
@SpringBootTest
@Transactional
class OrderItemMergingTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private com.sunsetbeach.repository.MenuItemRepository menuItemRepository;

    @Autowired
    private com.sunsetbeach.repository.UserRepository userRepository;

    private UserEntity waiter;
    private MenuItemEntity mojito;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setEmail("waiter-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.WAITER);
        waiter = userRepository.saveAndFlush(user);

        mojito = new MenuItemEntity();
        mojito.setName("Mojito");
        mojito.setDescription("test item");
        mojito.setCategory("Test");
        mojito.setDepartment(MenuDepartment.BAR);
        mojito.setPrice(new BigDecimal("150.00"));
        mojito.setAvailable(true);
        mojito = menuItemRepository.saveAndFlush(mojito);
    }

    @Test
    void addingSameItemThreeTimesOneByOne_mergesIntoASingleLineWithQuantityThree() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());

        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));
        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));
        Order updated = orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));

        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(updated.getTotal()).isEqualTo("450.00");
    }

    @Test
    void addingSameItemInOneBatchCall_alsoMerges() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());

        Order updated = orderService.addItems(
                order.getId(),
                List.of(new OrderItemInput(mojito.getId(), 1), new OrderItemInput(mojito.getId(), 2)));

        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void differentNotes_neverMerge_evenWithSameMenuItem() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());

        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1).note("no ice")));
        Order updated = orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));

        assertThat(updated.getItems()).hasSize(2);
        assertThat(updated.getItems()).extracting(OrderItem::getQuantity).containsExactlyInAnyOrder(1, 1);
    }

    @Test
    void sameNote_stillMerges() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());

        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1).note("no ice")));
        Order updated = orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 2).note("no ice")));

        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void blankNoteAndOmittedNote_treatedAsTheSameNoNoteLine_merge() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());

        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1).note("   ")));
        Order updated = orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));

        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(updated.getItems().get(0).getNote().get()).isNull();
    }

    @Test
    void lineAlreadySent_neverMergedInto_reorderCreatesItsOwnUnsentLine() {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());
        orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));
        Order sent = orderService.update(order.getId(), new OrderUpdateInput().status(com.sunsetbeach.model.OrderStatus.SENT));

        assertThat(sent.getItems()).hasSize(1);
        assertThat(sent.getItems().get(0).getSentAt().get()).isNotNull();
        String sentLineId = sent.getItems().get(0).getId();

        Order reordered = orderService.addItems(order.getId(), List.of(new OrderItemInput(mojito.getId(), 1)));

        assertThat(reordered.getItems()).hasSize(2);
        OrderItem original = reordered.getItems().stream().filter(i -> i.getId().equals(sentLineId)).findFirst().orElseThrow();
        OrderItem added = reordered.getItems().stream().filter(i -> !i.getId().equals(sentLineId)).findFirst().orElseThrow();
        assertThat(original.getQuantity()).isEqualTo(1); // untouched by the reorder
        assertThat(added.getQuantity()).isEqualTo(1);
        // The reorder line is dispatched immediately too (order is already SENT - see
        // PrintingTests for the ticket-content coverage), so it also ends up with sentAt set.
        assertThat(added.getSentAt().get()).isNotNull();
    }
}
