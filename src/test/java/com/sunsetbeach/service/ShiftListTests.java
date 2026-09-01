package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftListItem;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): {@code GET /shifts}, the manager
 * overview {@link ShiftExportTests} doesn't cover - filtering by opener/period, and that
 * {@code expectedCash}/{@code discrepancy} are computed with the same arithmetic as the CSV
 * export's summary block, exposed as real fields instead of free text this time.
 */
@SpringBootTest
@Transactional
class ShiftListTests extends AbstractIntegrationTest {

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private UserEntity cashierOne;
    private UserEntity cashierTwo;

    @BeforeEach
    void setUp() {
        cashierOne = persistUser();
        cashierTwo = persistUser();
    }

    private UserEntity persistUser() {
        UserEntity user = new UserEntity();
        user.setEmail("shift-list-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(user);
    }

    private ShiftEntity persistShift(UserEntity opener, LocalDateTime openedAt, BigDecimal openingFloat, BigDecimal closingCounted) {
        ShiftEntity shift = new ShiftEntity();
        shift.setOpenedByUserId(opener.getId());
        shift.setOpeningCashFloat(openingFloat);
        shift.setClosingCashCounted(closingCounted);
        if (closingCounted != null) {
            shift.setStatus(ShiftStatus.CLOSED);
            shift.setClosedByUserId(opener.getId());
            shift.setClosedAt(openedAt.plusHours(8));
        }
        ShiftEntity saved = shiftRepository.saveAndFlush(shift);
        // openedAt is @CreationTimestamp (Hibernate-managed, no setter) - backdating it for the
        // date-range tests below has to go around the entity, straight to the row, then clear the
        // persistence context so ShiftService#list's own fresh query sees the update instead of
        // the entity manager's now-stale first-level cache copy.
        entityManager
                .createNativeQuery("UPDATE \"Shift\" SET \"openedAt\" = :openedAt WHERE id = :id")
                .setParameter("openedAt", openedAt)
                .setParameter("id", saved.getId())
                .executeUpdate();
        entityManager.clear();
        return shiftRepository.findById(saved.getId()).orElseThrow();
    }

    private void persistCashPayment(ShiftEntity shift, UserEntity recordedBy, String amount) {
        OrderEntity order = new OrderEntity();
        order.setOpenedByUserId(recordedBy.getId());
        order = orderRepository.saveAndFlush(order);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod(PaymentMethod.CASH);
        payment.setAmount(new BigDecimal(amount));
        payment.setRecordedByUserId(recordedBy.getId());
        payment.setShiftId(shift.getId());
        paymentRepository.saveAndFlush(payment);
    }

    @Test
    void list_noFilters_returnsAllMatchingShiftsNewestFirst() {
        // Both closed - "Shift_one_open_per_user" allows only one OPEN shift per user at a time,
        // and this test only cares about ordering, not open/closed state.
        ShiftEntity older =
                persistShift(cashierOne, LocalDateTime.now().minusDays(2), new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        ShiftEntity newer =
                persistShift(cashierOne, LocalDateTime.now().minusHours(1), new BigDecimal("1000.00"), new BigDecimal("1000.00"));

        List<ShiftListItem> results = shiftService.list(null, null, null);

        List<String> ids = results.stream().map(ShiftListItem::getId).toList();
        assertThat(ids).contains(newer.getId(), older.getId());
        assertThat(ids.indexOf(newer.getId())).isLessThan(ids.indexOf(older.getId()));
    }

    @Test
    void list_filtersByStaffId_excludesOtherStaffsShifts() {
        ShiftEntity ownShift = persistShift(cashierOne, LocalDateTime.now(), new BigDecimal("500.00"), null);
        ShiftEntity otherShift = persistShift(cashierTwo, LocalDateTime.now(), new BigDecimal("500.00"), null);

        List<ShiftListItem> results = shiftService.list(null, null, cashierOne.getId());

        List<String> ids = results.stream().map(ShiftListItem::getId).toList();
        assertThat(ids).contains(ownShift.getId());
        assertThat(ids).doesNotContain(otherShift.getId());
    }

    @Test
    void list_filtersByDateRange_boundedOnBothEndsInclusive() {
        // All closed - see list_noFilters_returnsAllMatchingShiftsNewestFirst's comment; this
        // test only cares about openedAt filtering.
        LocalDate from = LocalDate.now().minusDays(5);
        LocalDate to = LocalDate.now().minusDays(3);
        ShiftEntity beforeRange =
                persistShift(cashierOne, from.minusDays(1).atTime(12, 0), new BigDecimal("0.00"), new BigDecimal("0.00"));
        ShiftEntity onFromDate = persistShift(cashierOne, from.atTime(0, 30), new BigDecimal("0.00"), new BigDecimal("0.00"));
        ShiftEntity onToDate = persistShift(cashierOne, to.atTime(23, 30), new BigDecimal("0.00"), new BigDecimal("0.00"));
        ShiftEntity afterRange =
                persistShift(cashierOne, to.plusDays(1).atTime(12, 0), new BigDecimal("0.00"), new BigDecimal("0.00"));

        List<String> ids = shiftService.list(from.toString(), to.toString(), null).stream().map(ShiftListItem::getId).toList();

        assertThat(ids).contains(onFromDate.getId(), onToDate.getId());
        assertThat(ids).doesNotContain(beforeRange.getId(), afterRange.getId());
    }

    @Test
    void list_openedByEmail_matchesTheOpeningStaffMember() {
        ShiftEntity shift = persistShift(cashierOne, LocalDateTime.now(), new BigDecimal("0.00"), null);

        ShiftListItem item = onlyItem(shiftService.list(null, null, cashierOne.getId()), shift.getId());

        assertThat(item.getOpenedByEmail()).isEqualTo(cashierOne.getEmail());
        // JsonNullable.of(null) (a required-but-nullable field with no value) is still "present" -
        // isPresent() means "this key appears in the JSON", not "the value is non-null". The
        // value itself is what's null for a still-open shift.
        assertThat(item.getClosedByEmail().get()).isNull();
    }

    @Test
    void list_closedShift_closedByEmailPresent() {
        ShiftEntity shift = persistShift(cashierOne, LocalDateTime.now(), new BigDecimal("0.00"), new BigDecimal("0.00"));

        ShiftListItem item = onlyItem(shiftService.list(null, null, cashierOne.getId()), shift.getId());

        assertThat(item.getClosedByEmail().get()).isEqualTo(cashierOne.getEmail());
    }

    // --- Reconciliation numbers: same arithmetic as the CSV export's summary block ---

    @Test
    void list_expectedCash_isOpeningFloatPlusCashPayments() {
        ShiftEntity shift = persistShift(cashierOne, LocalDateTime.now(), new BigDecimal("1000.00"), null);
        persistCashPayment(shift, cashierOne, "200.00");
        persistCashPayment(shift, cashierOne, "50.00");

        ShiftListItem item = onlyItem(shiftService.list(null, null, cashierOne.getId()), shift.getId());

        assertThat(new BigDecimal(item.getExpectedCash())).isEqualByComparingTo("1250.00");
    }

    @Test
    void list_openShift_discrepancyIsNull() {
        ShiftEntity shift = persistShift(cashierOne, LocalDateTime.now(), new BigDecimal("1000.00"), null);

        ShiftListItem item = onlyItem(shiftService.list(null, null, cashierOne.getId()), shift.getId());

        // See list_openedByEmail_matchesTheOpeningStaffMember's comment on JsonNullable semantics.
        assertThat(item.getDiscrepancy().get()).isNull();
    }

    @Test
    void list_drawerShort_discrepancyIsNegative() {
        // Expected = 1000.00 float + 200.00 cash = 1200.00; counted 1150.00 -> short by 50.
        ShiftEntity shift = persistShift(cashierOne, LocalDateTime.now(), new BigDecimal("1000.00"), new BigDecimal("1150.00"));
        persistCashPayment(shift, cashierOne, "200.00");

        ShiftListItem item = onlyItem(shiftService.list(null, null, cashierOne.getId()), shift.getId());

        assertThat(new BigDecimal(item.getDiscrepancy().get())).isEqualByComparingTo("-50.00");
    }

    @Test
    void list_drawerOver_discrepancyIsPositive() {
        ShiftEntity shift = persistShift(cashierOne, LocalDateTime.now(), new BigDecimal("1000.00"), new BigDecimal("1250.00"));
        persistCashPayment(shift, cashierOne, "200.00");

        ShiftListItem item = onlyItem(shiftService.list(null, null, cashierOne.getId()), shift.getId());

        assertThat(new BigDecimal(item.getDiscrepancy().get())).isEqualByComparingTo("50.00");
    }

    @Test
    void list_drawerMatches_discrepancyIsZero() {
        ShiftEntity shift = persistShift(cashierOne, LocalDateTime.now(), new BigDecimal("1000.00"), new BigDecimal("1200.00"));
        persistCashPayment(shift, cashierOne, "200.00");

        ShiftListItem item = onlyItem(shiftService.list(null, null, cashierOne.getId()), shift.getId());

        assertThat(new BigDecimal(item.getDiscrepancy().get())).isEqualByComparingTo("0.00");
    }

    private static ShiftListItem onlyItem(List<ShiftListItem> items, String shiftId) {
        return items.stream().filter(i -> i.getId().equals(shiftId)).findFirst().orElseThrow();
    }
}
