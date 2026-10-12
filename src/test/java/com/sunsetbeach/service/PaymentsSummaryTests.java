package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.FolioPaymentEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.FolioPaymentMethod;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.PaymentsSummary;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.repository.FolioPaymentRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): GET /payments/summary date-range
 * inclusivity/exclusivity at the day boundary, empty-period behavior, and - the core design
 * question this endpoint exists to answer - that ROOM_CHARGE never leaks into grandTotal while
 * still being visible in totals.roomCharge.
 *
 * A day is a hotel day (Asia/Bangkok, UTC+7) while {@code createdAt} holds UTC wall-clock, so every
 * boundary below is written as a Bangkok time and converted ({@link #bangkok}) - a payment at 02:00
 * Bangkok is 19:00 UTC the day before, and landing it on the previous day was the bug.
 *
 * All fixture payments use a fixed, far-past window (2030 is deliberately NOT used - picked a
 * window nothing else in the suite or real dev data could plausibly collide with) with
 * createdAt backdated via a native UPDATE, since @CreationTimestamp only fires on INSERT and
 * has no setter to override directly.
 */
@SpringBootTest
@Transactional
class PaymentsSummaryTests extends AbstractIntegrationTest {

    private static final LocalDate FROM = LocalDate.of(2031, 3, 10);
    private static final LocalDate TO = LocalDate.of(2031, 3, 12);
    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    /** A Bangkok wall-clock moment as the UTC wall-clock the timestamp columns store. */
    private static LocalDateTime bangkok(LocalDate day, LocalTime time) {
        return day.atTime(time).atZone(BANGKOK).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FolioPaymentRepository folioPaymentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private BookingService bookingService;

    private UserEntity user;
    private ShiftEntity shift;

    @BeforeEach
    void setUp() {
        UserEntity newUser = new UserEntity();
        newUser.setEmail("payments-summary-" + UUID.randomUUID() + "@example.com");
        newUser.setName(newUser.getEmail());
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.CASHIER);
        user = userRepository.saveAndFlush(newUser);

        ShiftEntity newShift = new ShiftEntity();
        newShift.setOpenedByUserId(user.getId());
        newShift.setStatus(ShiftStatus.CLOSED);
        shift = shiftRepository.saveAndFlush(newShift);
    }

    private void persistPayment(PaymentMethod method, String amount, LocalDateTime createdAt) {
        OrderEntity order = new OrderEntity();
        order.setOpenedByUserId(user.getId());
        order = orderRepository.saveAndFlush(order);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod(method);
        payment.setAmount(new BigDecimal(amount));
        payment.setRecordedByUserId(user.getId());
        payment.setShiftId(shift.getId());
        payment = paymentRepository.saveAndFlush(payment);

        entityManager
                .createNativeQuery("UPDATE \"Payment\" SET \"createdAt\" = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, payment.getId())
                .executeUpdate();
        entityManager.clear();
    }

    @Test
    void getSummary_emptyPeriod_isAllZeros() {
        PaymentsSummary summary = paymentService.getSummary(FROM.toString(), TO.toString());

        assertThat(summary.getFrom()).isEqualTo(FROM.toString());
        assertThat(summary.getTo()).isEqualTo(TO.toString());
        assertThat(summary.getTotals().getCash()).isEqualTo("0.00");
        assertThat(summary.getTotals().getCard()).isEqualTo("0.00");
        assertThat(summary.getTotals().getRoomCharge()).isEqualTo("0.00");
        assertThat(summary.getTotals().getOther()).isEqualTo("0.00");
        assertThat(summary.getTotals().getPaymentCount()).isZero();
        assertThat(summary.getGrandTotal()).isEqualTo("0.00");
    }

    @Test
    void getSummary_paymentAtStartOfFromDay_isIncluded() {
        persistPayment(PaymentMethod.CASH, "100.00", bangkok(FROM, LocalTime.MIDNIGHT));

        PaymentsSummary summary = paymentService.getSummary(FROM.toString(), TO.toString());

        assertThat(summary.getTotals().getCash()).isEqualTo("100.00");
        assertThat(summary.getGrandTotal()).isEqualTo("100.00");
    }

    @Test
    void getSummary_paymentAtEndOfToDay_isIncluded() {
        persistPayment(PaymentMethod.CASH, "50.00", bangkok(TO, LocalTime.of(23, 59, 59)));

        PaymentsSummary summary = paymentService.getSummary(FROM.toString(), TO.toString());

        assertThat(summary.getTotals().getCash()).isEqualTo("50.00");
    }

    @Test
    void getSummary_paymentOneDayBeforeFrom_isExcluded() {
        persistPayment(PaymentMethod.CASH, "999.00", bangkok(FROM.minusDays(1), LocalTime.of(23, 59, 59)));

        PaymentsSummary summary = paymentService.getSummary(FROM.toString(), TO.toString());

        assertThat(summary.getTotals().getCash()).isEqualTo("0.00");
        assertThat(summary.getTotals().getPaymentCount()).isZero();
    }

    @Test
    void getSummary_paymentOneDayAfterTo_isExcluded() {
        persistPayment(PaymentMethod.CASH, "999.00", bangkok(TO.plusDays(1), LocalTime.MIDNIGHT));

        PaymentsSummary summary = paymentService.getSummary(FROM.toString(), TO.toString());

        assertThat(summary.getTotals().getCash()).isEqualTo("0.00");
        assertThat(summary.getTotals().getPaymentCount()).isZero();
    }

    /** The reported case: 02:00 in Bangkok is still the previous UTC day, and belongs to the Bangkok day it was taken on. */
    @Test
    void getSummary_paymentAt0200Bangkok_countsOnThatBangkokDay_notTheDayBefore() {
        LocalDate day = FROM.plusDays(1);
        persistPayment(PaymentMethod.CASH, "75.00", bangkok(day, LocalTime.of(2, 0)));

        PaymentsSummary onThatDay = paymentService.getSummary(day.toString(), day.toString());
        PaymentsSummary dayBefore = paymentService.getSummary(day.minusDays(1).toString(), day.minusDays(1).toString());

        assertThat(onThatDay.getTotals().getCash()).isEqualTo("75.00");
        assertThat(dayBefore.getTotals().getCash()).isEqualTo("0.00");
    }

    /** The other end: 23:30 UTC is already 06:30 the next morning in Bangkok. */
    @Test
    void getSummary_paymentAt2330Utc_countsOnTheNextBangkokDay() {
        LocalDate utcDay = FROM.plusDays(1);
        persistPayment(PaymentMethod.CARD, "40.00", utcDay.atTime(23, 30));

        assertThat(paymentService.getSummary(utcDay.toString(), utcDay.toString()).getTotals().getCard()).isEqualTo("0.00");
        assertThat(paymentService.getSummary(utcDay.plusDays(1).toString(), utcDay.plusDays(1).toString()).getTotals().getCard()).isEqualTo("40.00");
    }

    /** "Room payments collected" on the dashboard is folioPaymentsTotal - the same Bangkok-day window as the POS figures. */
    @Test
    void getSummary_folioPaymentAt0200Bangkok_countsOnThatBangkokDay_notTheDayBefore() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Summary Folio Room " + UUID.randomUUID());
        newRoom.setDescription("Used only by PaymentsSummaryTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(newRoom);
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Summary Unit " + UUID.randomUUID());
        unit.setActive(true);
        unit = roomUnitRepository.saveAndFlush(unit);
        Booking booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(savedRoom.getId(), "Summary Guest", "2033-06-01", "2033-06-03", BookingChannel.WALK_IN, 1)
                        .roomUnitId(unit.getId()));

        FolioPaymentEntity payment = new FolioPaymentEntity();
        payment.setBookingId(booking.getId());
        payment.setMethod(FolioPaymentMethod.CASH);
        payment.setAmount(new BigDecimal("640.00"));
        payment.setRecordedByUserId(user.getId());
        payment = folioPaymentRepository.saveAndFlush(payment);
        LocalDate day = FROM.plusDays(1);
        entityManager
                .createNativeQuery("UPDATE \"FolioPayment\" SET \"createdAt\" = ?1 WHERE id = ?2")
                .setParameter(1, bangkok(day, LocalTime.of(2, 0)))
                .setParameter(2, payment.getId())
                .executeUpdate();
        entityManager.clear();

        assertThat(paymentService.getSummary(day.toString(), day.toString()).getFolioPaymentsTotal()).isEqualTo("640.00");
        assertThat(paymentService.getSummary(day.minusDays(1).toString(), day.minusDays(1).toString()).getFolioPaymentsTotal()).isEqualTo("0.00");
    }

    @Test
    void getSummary_fromAfterTo_throwsValidationException() {
        assertThatThrownBy(() -> paymentService.getSummary(TO.toString(), FROM.toString())).isInstanceOf(ValidationException.class);
    }

    // --- The core design question: ROOM_CHARGE must never leak into grandTotal ---

    @Test
    void getSummary_roomChargeIsReportedButExcludedFromGrandTotal() {
        LocalDateTime mid = FROM.plusDays(1).atTime(12, 0);
        persistPayment(PaymentMethod.CASH, "100.00", mid);
        persistPayment(PaymentMethod.CARD, "200.00", mid);
        persistPayment(PaymentMethod.OTHER, "10.00", mid);
        persistPayment(PaymentMethod.ROOM_CHARGE, "500.00", mid);

        PaymentsSummary summary = paymentService.getSummary(FROM.toString(), TO.toString());

        assertThat(summary.getTotals().getCash()).isEqualTo("100.00");
        assertThat(summary.getTotals().getCard()).isEqualTo("200.00");
        assertThat(summary.getTotals().getOther()).isEqualTo("10.00");
        // Still visible, in full, in the breakdown:
        assertThat(summary.getTotals().getRoomCharge()).isEqualTo("500.00");
        assertThat(summary.getTotals().getPaymentCount()).isEqualTo(4);
        // But NOT folded into grandTotal - only cash+card+other:
        assertThat(summary.getGrandTotal()).isEqualTo("310.00");
    }

    @Test
    void getSummary_onlyRoomCharges_grandTotalIsZeroDespiteNonZeroRoomCharge() {
        persistPayment(PaymentMethod.ROOM_CHARGE, "500.00", FROM.plusDays(1).atTime(12, 0));

        PaymentsSummary summary = paymentService.getSummary(FROM.toString(), TO.toString());

        assertThat(summary.getTotals().getRoomCharge()).isEqualTo("500.00");
        assertThat(summary.getGrandTotal()).isEqualTo("0.00");
    }
}
