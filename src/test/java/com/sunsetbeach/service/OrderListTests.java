package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Order;
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
 * DB-backed (real dev Postgres, rolled back after each test): the new {@code GET /orders}
 * filters that back the closed-order review list - {@code staffId}, the {@code from}/{@code to}
 * period (bounded on both ends inclusive, same convention as {@link ShiftListTests}), and
 * {@code shiftId} (joining through {@code Payment}, since {@code Order} itself carries no
 * shiftId - see {@code ShiftsApi}). {@code status} itself already had test coverage via
 * {@link OrderCloseAndShiftGuardTests} and friends.
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
    private UserEntity staffTwo;

    @BeforeEach
    void setUp() {
        staffOne = persistUser();
        staffTwo = persistUser();
    }

    private UserEntity persistUser() {
        UserEntity user = new UserEntity();
        user.setEmail("order-list-" + UUID.randomUUID() + "@example.com");
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
    void list_filtersByStaffId_excludesOtherStaffsOrders() {
        OrderEntity own = persistOrder(staffOne, LocalDateTime.now());
        OrderEntity other = persistOrder(staffTwo, LocalDateTime.now());

        List<String> ids = orderService.list(null, null, null, null, staffOne.getId(), null, null, null).stream()
                .map(Order::getId)
                .toList();

        assertThat(ids).contains(own.getId());
        assertThat(ids).doesNotContain(other.getId());
    }

    @Test
    void list_filtersByDateRange_boundedOnBothEndsInclusive() {
        LocalDate from = LocalDate.now().minusDays(5);
        LocalDate to = LocalDate.now().minusDays(3);
        OrderEntity beforeRange = persistOrder(staffOne, from.minusDays(1).atTime(12, 0));
        OrderEntity onFromDate = persistOrder(staffOne, from.atTime(0, 30));
        OrderEntity onToDate = persistOrder(staffOne, to.atTime(23, 30));
        OrderEntity afterRange = persistOrder(staffOne, to.plusDays(1).atTime(12, 0));

        List<String> ids = orderService.list(null, null, null, null, staffOne.getId(), from, to, null).stream()
                .map(Order::getId)
                .toList();

        assertThat(ids).contains(onFromDate.getId(), onToDate.getId());
        assertThat(ids).doesNotContain(beforeRange.getId(), afterRange.getId());
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

        List<String> ids =
                orderService.list(null, null, null, null, null, null, null, shift.getId()).stream().map(Order::getId).toList();

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

        List<Order> results = orderService.list(null, null, null, null, null, null, null, shift.getId());

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
        List<Order> listed = orderService.list(null, null, null, null, staffOne.getId(), null, null, null);
        Order listedPaid = listed.stream().filter(o -> o.getId().equals(paidOrder.getId())).findFirst().orElseThrow();
        assertThat(listedPaid.getPaymentMethod().get()).isEqualTo(PaymentMethod.CARD);
        assertThat(listedPaid.getOpenedByEmail()).isEqualTo(staffOne.getEmail());
    }

    @Test
    void list_filtersCombineWithAnd_statusAndStaffId() {
        OrderEntity open = persistOrder(staffOne, LocalDateTime.now());
        OrderEntity cancelled = persistOrder(staffOne, LocalDateTime.now());
        orderService.cancel(cancelled.getId());
        persistOrder(staffTwo, LocalDateTime.now()); // wrong staff, must never match

        List<String> ids = orderService
                .list(OrderStatus.CANCELLED, null, null, null, staffOne.getId(), null, null, null)
                .stream()
                .map(Order::getId)
                .toList();

        assertThat(ids).containsExactly(cancelled.getId());
        assertThat(ids).doesNotContain(open.getId());
    }
}
