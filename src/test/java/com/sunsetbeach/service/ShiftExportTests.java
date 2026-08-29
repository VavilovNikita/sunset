package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): {@code GET /shifts/{id}/export}'s
 * CSV format (BOM/sep hint shared with bookings export) and the reconciliation totals block -
 * in particular that ROOM_CHARGE never leaks into the cash reconciliation, mirroring
 * PaymentsSummaryTests' core assertion for {@code GET /payments/summary}, and that the
 * counted-vs-expected cash discrepancy is computed and signed correctly.
 */
@SpringBootTest
@Transactional
class ShiftExportTests {

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity cashier;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setEmail("cashier-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.CASHIER);
        cashier = userRepository.saveAndFlush(user);
    }

    private ShiftEntity persistShift(BigDecimal openingFloat, BigDecimal closingCounted) {
        ShiftEntity shift = new ShiftEntity();
        shift.setOpenedByUserId(cashier.getId());
        shift.setOpeningCashFloat(openingFloat);
        shift.setClosingCashCounted(closingCounted);
        if (closingCounted != null) {
            shift.setStatus(ShiftStatus.CLOSED);
        }
        return shiftRepository.saveAndFlush(shift);
    }

    private OrderEntity persistOrder(String tableId) {
        OrderEntity order = new OrderEntity();
        order.setTableId(tableId);
        order.setOpenedByUserId(cashier.getId());
        return orderRepository.saveAndFlush(order);
    }

    private void persistPayment(ShiftEntity shift, PaymentMethod method, String amount, OrderEntity order, String bookingId) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod(method);
        payment.setAmount(new BigDecimal(amount));
        payment.setBookingId(bookingId);
        payment.setRecordedByUserId(cashier.getId());
        payment.setShiftId(shift.getId());
        paymentRepository.saveAndFlush(payment);
    }

    // --- CSV format: BOM + sep hint, shared with bookings export via CsvBuilder ---

    @Test
    void exportCsv_startsWithBomAndSepHint() {
        ShiftEntity shift = persistShift(null, null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).startsWith("﻿sep=,\r\n");
    }

    @Test
    void exportCsv_unknownShift_throwsNotFound() {
        assertThatThrownBy(() -> shiftService.exportCsv("does-not-exist")).isInstanceOf(NotFoundException.class);
    }

    // --- Row content: readable fields instead of raw order/booking/cashier UUIDs ---

    @Test
    void exportCsv_dineInPayment_showsTableZoneAndItemsInsteadOfRawOrderId() {
        ShiftEntity shift = persistShift(new BigDecimal("0.00"), null);

        TableEntity table = new TableEntity();
        table.setZone(Zone.BAR);
        table.setLabel("Bar Seat 3");
        table.setCapacity(2);
        table = tableRepository.saveAndFlush(table);

        MenuItemEntity menuItem = new MenuItemEntity();
        menuItem.setName("Mojito");
        menuItem.setDescription("Rum, mint, lime");
        menuItem.setCategory("Drinks");
        menuItem.setPrice(new BigDecimal("10.00"));
        menuItem = menuItemRepository.saveAndFlush(menuItem);

        OrderEntity order = persistOrder(table.getId());
        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(order.getId());
        item.setMenuItemId(menuItem.getId());
        item.setQuantity(2);
        item.setUnitPrice(menuItem.getPrice());
        orderItemRepository.saveAndFlush(item);

        persistPayment(shift, PaymentMethod.CARD, "20.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Bar – Bar Seat 3: 2× Mojito");
        List<String> lines = List.of(csv.split("\r\n"));
        String dataLine = lines.stream().filter(l -> l.contains("Bar Seat 3")).findFirst().orElseThrow();
        // The order's raw id is present only as the last (support-facing) column.
        assertThat(dataLine).doesNotStartWith(order.getId());
        assertThat(dataLine).endsWith(order.getId());
    }

    @Test
    void exportCsv_orderWithNoTable_fallsBackToShortIdentifier() {
        ShiftEntity shift = persistShift(new BigDecimal("0.00"), null);
        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.CASH, "15.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Order #" + order.getId().substring(0, 8).toUpperCase());
    }

    @Test
    void exportCsv_roomChargePayment_showsGuestNameAndRoomInsteadOfBookingId() {
        ShiftEntity shift = persistShift(new BigDecimal("0.00"), null);

        RoomEntity room = new RoomEntity();
        room.setName("Ocean View Suite");
        room.setDescription("A room used only by tests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(room);

        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName("Jane Doe");
        booking.setGuestEmail("jane@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.now());
        booking.setCheckOut(LocalDate.now().plusDays(1));
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.saveAndFlush(booking);

        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.ROOM_CHARGE, "250.00", order, booking.getId());

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Jane Doe – Ocean View Suite");
        assertThat(csv).doesNotContain(booking.getId());
    }

    @Test
    void exportCsv_nonRoomChargePayment_guestRoomColumnIsDash() {
        ShiftEntity shift = persistShift(new BigDecimal("0.00"), null);
        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.CASH, "15.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        // split("\r\n") lines: [0] = "sep=," preamble, [1] = header row, [2] = this payment's row.
        List<String> lines = List.of(csv.split("\r\n"));
        String dataLine = lines.get(2);
        assertThat(dataLine.split(",")).contains("—");
    }

    @Test
    void exportCsv_cashierColumn_showsEmailNotUserId() {
        ShiftEntity shift = persistShift(new BigDecimal("0.00"), null);
        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.CASH, "50.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains(cashier.getEmail());
        assertThat(csv).doesNotContain(cashier.getId());
    }

    @Test
    void exportCsv_time_isReadableWithoutMillisecondsOrRawIsoInstant() {
        ShiftEntity shift = persistShift(new BigDecimal("0.00"), null);
        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.CASH, "10.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        // "yyyy-MM-dd HH:mm:ss UTC" - a space between date and time, not the millisecond ISO
        // instant ("yyyy-MM-ddTHH:mm:ss.SSSZ") used elsewhere in the API.
        assertThat(csv).containsPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} UTC");
        assertThat(csv).doesNotContainPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
    }

    // --- Summary block: ROOM_CHARGE excluded from received/expected cash, like /payments/summary ---

    @Test
    void exportCsv_summary_roomChargeExcludedFromReceivedAndExpectedCash() {
        // A separate order per payment - Payment.orderId is now unique (one payment closes one
        // order, no partial-payment model), so three payments against a shift means three orders.
        ShiftEntity shift = persistShift(new BigDecimal("1000.00"), null);
        persistPayment(shift, PaymentMethod.CASH, "200.00", persistOrder(null), null);
        persistPayment(shift, PaymentMethod.CARD, "300.00", persistOrder(null), null);
        persistPayment(shift, PaymentMethod.ROOM_CHARGE, "5000.00", persistOrder(null), null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Cash,200.00");
        assertThat(csv).contains("Card,300.00");
        assertThat(csv).contains("Room charge (posted to room folio - not received cash/card),5000.00");
        assertThat(csv).contains("Received total (cash + card + other),500.00");
        // Expected cash = opening float (1000.00) + cash payments (200.00) only - not the room charge.
        assertThat(csv).contains("Expected cash (float + cash payments),1200.00");
    }

    @Test
    void exportCsv_summary_countedCashMatchesExpected_discrepancyIsZero() {
        ShiftEntity shift = persistShift(new BigDecimal("1000.00"), new BigDecimal("1200.00"));
        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.CASH, "200.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Counted cash,1200.00");
        assertThat(csv).contains("Discrepancy (counted - expected),0.00");
    }

    @Test
    void exportCsv_summary_drawerShort_discrepancyIsNegative() {
        // Expected = 1000.00 float + 200.00 cash = 1200.00; counted 1150.00 -> short by 50.
        ShiftEntity shift = persistShift(new BigDecimal("1000.00"), new BigDecimal("1150.00"));
        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.CASH, "200.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Discrepancy (counted - expected),-50.00");
    }

    @Test
    void exportCsv_summary_drawerOver_discrepancyIsPositive() {
        // Expected = 1000.00 float + 200.00 cash = 1200.00; counted 1250.00 -> over by 50.
        ShiftEntity shift = persistShift(new BigDecimal("1000.00"), new BigDecimal("1250.00"));
        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.CASH, "200.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Discrepancy (counted - expected),50.00").doesNotContain("Discrepancy (counted - expected),-50.00");
    }

    @Test
    void exportCsv_shiftNotYetClosed_countedCashAndDiscrepancyAreDashes() {
        ShiftEntity shift = persistShift(new BigDecimal("1000.00"), null);
        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.CASH, "200.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Counted cash,—");
        assertThat(csv).contains("Discrepancy (counted - expected),—");
    }

    @Test
    void exportCsv_noOpeningFloat_treatedAsZero() {
        ShiftEntity shift = persistShift(null, null);
        OrderEntity order = persistOrder(null);
        persistPayment(shift, PaymentMethod.CASH, "75.00", order, null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Opening cash float,0.00");
        assertThat(csv).contains("Expected cash (float + cash payments),75.00");
    }

    @Test
    void exportCsv_paymentCount_matchesNumberOfPayments() {
        // Separate order per payment - see exportCsv_summary_roomChargeExcludedFromReceivedAndExpectedCash.
        ShiftEntity shift = persistShift(new BigDecimal("0.00"), null);
        persistPayment(shift, PaymentMethod.CASH, "10.00", persistOrder(null), null);
        persistPayment(shift, PaymentMethod.CARD, "20.00", persistOrder(null), null);
        persistPayment(shift, PaymentMethod.OTHER, "5.00", persistOrder(null), null);

        String csv = shiftService.exportCsv(shift.getId());

        assertThat(csv).contains("Payments,3");
    }
}
