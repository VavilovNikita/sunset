package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderDateBasis;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): the {@code GET /orders} filters
 * that back the closed-order review list - the {@code from}/{@code to} period (bounded on both
 * ends inclusive, same convention as {@link ShiftListTests}), and {@code shiftId} (joining
 * through {@code Payment}, since {@code Order} itself carries no shiftId - see {@code
 * ShiftsApi}). {@code status} itself already had test coverage via {@link
 * OrderCloseAndShiftGuardTests} and friends. {@code zone}/{@code bookingId}/{@code staffId} were
 * removed as dead API surface - no real caller ever sent them.
 */
@SpringBootTest
@Transactional
class OrderListTests extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private UserEntity staffOne;

    @BeforeEach
    void setUp() {
        staffOne = persistUser();
    }

    private UserEntity persistUser() {
        UserEntity user = new UserEntity();
        user.setEmail("order-list-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(user);
    }

    private OrderEntity persistOrder(UserEntity openedBy, LocalDateTime createdAt) {
        OrderEntity order = new OrderEntity();
        order.setOpenedByUserId(openedBy.getId());
        OrderEntity saved = orderRepository.saveAndFlush(order);
        // createdAt is @CreationTimestamp (Hibernate-managed, no setter) - same workaround as
        // ShiftListTests.persistShift.
        entityManager
                .createNativeQuery("UPDATE \"Order\" SET \"createdAt\" = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", saved.getId())
                .executeUpdate();
        entityManager.clear();
        return orderRepository.findById(saved.getId()).orElseThrow();
    }

    @Test
    void list_filtersByDateRange_boundedOnBothEndsInclusive() {
        LocalDate from = LocalDate.now().minusDays(5);
        LocalDate to = LocalDate.now().minusDays(3);
        OrderEntity beforeRange = persistOrder(staffOne, from.minusDays(1).atTime(12, 0));
        OrderEntity onFromDate = persistOrder(staffOne, from.atTime(0, 30));
        OrderEntity onToDate = persistOrder(staffOne, to.atTime(23, 30));
        OrderEntity afterRange = persistOrder(staffOne, to.plusDays(1).atTime(12, 0));

        List<String> ids = orderService.list(null, null, from, to, null, null).stream()
                .map(Order::getId)
                .toList();

        assertThat(ids).contains(onFromDate.getId(), onToDate.getId());
        assertThat(ids).doesNotContain(beforeRange.getId(), afterRange.getId());
    }

    /**
     * {@code dateBasis=CLOSED}: an order opened a month before the period and paid inside it is in
     * that period's history - the bug was that the history only ever filtered on the opening date.
     * Fixed dates throughout; the payment's and the cancellation's timestamps are backdated the
     * same way as {@link #persistOrder}'s createdAt.
     */
    @Test
    void list_closedDateBasis_filtersOnWhenTheOrderWasPaidOrCancelled_notOpened() {
        LocalDate from = LocalDate.of(2026, 2, 9);
        LocalDate to = LocalDate.of(2026, 2, 11);
        ShiftEntity shift = new ShiftEntity();
        shift.setOpenedByUserId(staffOne.getId());
        shift.setOpeningCashFloat(BigDecimal.ZERO);
        shift = shiftRepository.saveAndFlush(shift);

        OrderEntity openedLongAgoPaidInRange = persistOrder(staffOne, LocalDateTime.of(2026, 1, 5, 19, 0));
        pay(openedLongAgoPaidInRange, shift, LocalDateTime.of(2026, 2, 10, 13, 0));
        OrderEntity openedInRangePaidAfter = persistOrder(staffOne, LocalDateTime.of(2026, 2, 10, 20, 0));
        pay(openedInRangePaidAfter, shift, LocalDateTime.of(2026, 2, 12, 1, 0));
        OrderEntity cancelledInRange = persistOrder(staffOne, LocalDateTime.of(2026, 1, 20, 12, 0));
        setStatusAndUpdatedAt(cancelledInRange, OrderStatus.CANCELLED, LocalDateTime.of(2026, 2, 9, 0, 30));
        OrderEntity stillOpen = persistOrder(staffOne, LocalDateTime.of(2026, 2, 10, 12, 0));

        List<String> byClose = orderService.list(null, null, from, to, OrderDateBasis.CLOSED, null).stream().map(Order::getId).toList();
        assertThat(byClose).contains(openedLongAgoPaidInRange.getId(), cancelledInRange.getId());
        assertThat(byClose).doesNotContain(openedInRangePaidAfter.getId(), stillOpen.getId());

        // The opening date stays available, and is still the default.
        List<String> byOpen = orderService.list(null, null, from, to, null, null).stream().map(Order::getId).toList();
        assertThat(byOpen).contains(openedInRangePaidAfter.getId(), stillOpen.getId());
        assertThat(byOpen).doesNotContain(openedLongAgoPaidInRange.getId(), cancelledInRange.getId());

        Order paid = orderService.getById(openedLongAgoPaidInRange.getId());
        assertThat(paid.getClosedAt().get().toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 2, 10, 13, 0));
        assertThat(orderService.getById(stillOpen.getId()).getClosedAt().get()).isNull();
    }

    private void pay(OrderEntity order, ShiftEntity shift, LocalDateTime paidAt) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod(PaymentMethod.CASH);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setRecordedByUserId(staffOne.getId());
        payment.setShiftId(shift.getId());
        PaymentEntity saved = paymentRepository.saveAndFlush(payment);
        entityManager
                .createNativeQuery("UPDATE \"Payment\" SET \"createdAt\" = :paidAt WHERE id = :id")
                .setParameter("paidAt", paidAt)
                .setParameter("id", saved.getId())
                .executeUpdate();
        setStatusAndUpdatedAt(order, OrderStatus.PAID, paidAt);
    }

    private void setStatusAndUpdatedAt(OrderEntity order, OrderStatus status, LocalDateTime updatedAt) {
        entityManager
                .createNativeQuery("UPDATE \"Order\" SET status = CAST(:status AS \"OrderStatus\"), \"updatedAt\" = :updatedAt WHERE id = :id")
                .setParameter("status", status.getValue())
                .setParameter("updatedAt", updatedAt)
                .setParameter("id", order.getId())
                .executeUpdate();
        entityManager.clear();
    }

    @Test
    void list_filtersByShiftId_joinsThroughPayment_excludesOrdersFromOtherShifts() {
        ShiftEntity shift = new ShiftEntity();
        shift.setOpenedByUserId(staffOne.getId());
        shift.setOpeningCashFloat(BigDecimal.ZERO);
        shift = shiftRepository.saveAndFlush(shift);

        OrderEntity paidInShift = persistOrder(staffOne, LocalDateTime.now());
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(paidInShift.getId());
        payment.setMethod(PaymentMethod.CASH);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setRecordedByUserId(staffOne.getId());
        payment.setShiftId(shift.getId());
        paymentRepository.saveAndFlush(payment);

        OrderEntity unrelatedOrder = persistOrder(staffOne, LocalDateTime.now()); // no Payment at all

        List<String> ids = orderService.list(null, null, null, null, null, shift.getId()).stream().map(Order::getId).toList();

        assertThat(ids).containsExactly(paidInShift.getId());
        assertThat(ids).doesNotContain(unrelatedOrder.getId());
    }

    @Test
    void list_shiftIdWithNoPayments_returnsEmptyNotEverything() {
        ShiftEntity shift = new ShiftEntity();
        shift.setOpenedByUserId(staffOne.getId());
        shift.setOpeningCashFloat(BigDecimal.ZERO);
        shift = shiftRepository.saveAndFlush(shift);
        persistOrder(staffOne, LocalDateTime.now());

        List<Order> results = orderService.list(null, null, null, null, null, shift.getId());

        assertThat(results).isEmpty();
    }

    @Test
    void getById_paidOrder_exposesPaymentMethodAndOpenedByEmail_openOrderExposesNullPaymentMethod() {
        OrderEntity open = persistOrder(staffOne, LocalDateTime.now());
        assertThat(orderService.getById(open.getId()).getPaymentMethod().get()).isNull();

        ShiftEntity shift = new ShiftEntity();
        shift.setOpenedByUserId(staffOne.getId());
        shift.setOpeningCashFloat(BigDecimal.ZERO);
        shift = shiftRepository.saveAndFlush(shift);

        OrderEntity paidOrder = persistOrder(staffOne, LocalDateTime.now());
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(paidOrder.getId());
        payment.setMethod(PaymentMethod.CARD);
        payment.setAmount(BigDecimal.ZERO);
        payment.setRecordedByUserId(staffOne.getId());
        payment.setShiftId(shift.getId());
        paymentRepository.saveAndFlush(payment);

        Order fetched = orderService.getById(paidOrder.getId());
        assertThat(fetched.getPaymentMethod().get()).isEqualTo(PaymentMethod.CARD);
        assertThat(fetched.getOpenedByEmail()).isEqualTo(staffOne.getEmail());
        // list() must carry the same values through its batched lookups, not just getById's single ones.
        List<Order> listed = orderService.list(null, null, null, null, null, null);
        Order listedPaid = listed.stream().filter(o -> o.getId().equals(paidOrder.getId())).findFirst().orElseThrow();
        assertThat(listedPaid.getPaymentMethod().get()).isEqualTo(PaymentMethod.CARD);
        assertThat(listedPaid.getOpenedByEmail()).isEqualTo(staffOne.getEmail());
    }

    /**
     * A room-service order has no staff opener (see {@code Order.openedByUserId}'s own
     * openapi.yaml description) - {@code getById}/{@code list} must resolve a human label instead
     * of calling {@code UserRepository} with a null id, which throws outright. Covers both the
     * single-order path ({@code resolveEmail}) and the batched one ({@code toDtos}) so they can
     * never silently diverge on this.
     */
    @Test
    void openedByUserIdNull_getByIdAndList_resolveAGuestLabel_neverThrow() {
        OrderEntity roomServiceOrder = new OrderEntity();
        roomServiceOrder.setOpenedByUserId(null);
        OrderEntity saved = orderRepository.saveAndFlush(roomServiceOrder);

        Order fetched = orderService.getById(saved.getId());
        assertThat(fetched.getOpenedByUserId().get()).isNull();
        assertThat(fetched.getOpenedByEmail()).isEqualTo("Guest (room service)");

        List<Order> listed = orderService.list(null, null, null, null, null, null);
        Order listedOne = listed.stream().filter(o -> o.getId().equals(saved.getId())).findFirst().orElseThrow();
        assertThat(listedOne.getOpenedByUserId().get()).isNull();
        assertThat(listedOne.getOpenedByEmail()).isEqualTo("Guest (room service)");
    }
}
