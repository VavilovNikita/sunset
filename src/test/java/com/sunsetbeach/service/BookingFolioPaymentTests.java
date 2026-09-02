package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.FolioPayment;
import com.sunsetbeach.model.FolioPaymentInput;
import com.sunsetbeach.model.FolioPaymentMethod;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.sunsetbeach.security.StaffPrincipal;

/**
 * Covers the fix for a real bug: a ROOM_CHARGE Payment had no way to ever be marked collected,
 * so any booking that ever had one showed as owing it forever - the check-out warning fired on
 * every check-out and RoomChargeDebtBadge never went dark, regardless of what was actually
 * paid. See BookingService#recordFolioPayment's javadoc for the fix and FolioPaymentEntity's
 * for what it deliberately does and doesn't do (no shift linkage, doesn't touch PAID).
 *
 * <p>DB-backed against the ephemeral Testcontainers Postgres, {@code @Transactional} (auto
 * rollback).
 */
@SpringBootTest
@Transactional
class BookingFolioPaymentTests extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private RoomEntity room;
    private UserEntity staffUser;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Folio Payment Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by BookingFolioPaymentTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);

        UserEntity newUser = new UserEntity();
        newUser.setEmail("cashier-" + UUID.randomUUID() + "@example.com");
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.CASHIER);
        staffUser = userRepository.saveAndFlush(newUser);

        // recordFolioPayment reads the acting user off the security context (same pattern
        // AuditLogService uses) - there's no MockMvc/JWT layer in this service-level test, so
        // it's stubbed directly.
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new StaffPrincipal(staffUser.getId(), staffUser.getEmail(), Role.CASHIER), null, List.of()));
    }

    private BookingEntity persistBooking(BigDecimal totalPrice) {
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName("Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.now());
        booking.setCheckOut(LocalDate.now().plusDays(2));
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.saveAndFlush(booking);
    }

    /** Builds a MenuItem+Order+OrderItem+Shift+ROOM_CHARGE Payment against {@code booking} - mirrors BookingRoomChargeTests's own helper. */
    private void persistRoomChargePayment(BookingEntity booking, BigDecimal amount) {
        MenuItemEntity menuItem = new MenuItemEntity();
        menuItem.setName("Pad Thai");
        menuItem.setDescription("Stir-fried rice noodles");
        menuItem.setCategory("Mains");
        menuItem.setPrice(amount);
        menuItem.setAvailable(true);
        menuItem = menuItemRepository.saveAndFlush(menuItem);

        OrderEntity order = new OrderEntity();
        order.setBookingId(booking.getId());
        order.setOpenedByUserId(staffUser.getId());
        order = orderRepository.saveAndFlush(order);

        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(order.getId());
        item.setMenuItemId(menuItem.getId());
        item.setQuantity(1);
        item.setUnitPrice(amount);
        orderItemRepository.saveAndFlush(item);

        // CLOSED, not OPEN: this helper may be called more than once per test for the same
        // staffUser, and only one OPEN shift per user is allowed.
        ShiftEntity shift = new ShiftEntity();
        shift.setOpenedByUserId(staffUser.getId());
        shift.setStatus(ShiftStatus.CLOSED);
        shift = shiftRepository.saveAndFlush(shift);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod(PaymentMethod.ROOM_CHARGE);
        payment.setAmount(amount);
        payment.setBookingId(booking.getId());
        payment.setRecordedByUserId(staffUser.getId());
        payment.setShiftId(shift.getId());
        paymentRepository.saveAndFlush(payment);
    }

    // --- The actual bug: an unsettled room charge must not haunt the folio forever -------------

    @Test
    void roomCharge_withNoSettlement_showsAsOutstanding() {
        BookingEntity booking = persistBooking(new BigDecimal("1000.00"));
        persistRoomChargePayment(booking, new BigDecimal("500.00"));

        BookingFolio folio = bookingService.getFolio(booking.getId());
        assertThat(new BigDecimal(folio.getRoomChargesTotal())).isEqualByComparingTo("500.00");

        BigDecimal outstanding = bookingService.computeOutstandingBalance(booking.getId());
        assertThat(outstanding).isEqualByComparingTo("1500.00"); // room (not PAID) + charge
    }

    @Test
    void recordFolioPayment_fullAmount_zeroesOutTheRoomChargesBalance() {
        BookingEntity booking = persistBooking(new BigDecimal("1000.00"));
        persistRoomChargePayment(booking, new BigDecimal("500.00"));

        FolioPayment recorded = bookingService.recordFolioPayment(
                booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "500.00"));
        assertThat(recorded.getAmount()).isEqualTo("500.00");
        assertThat(recorded.getMethod()).isEqualTo(FolioPaymentMethod.CASH);
        assertThat(recorded.getRecordedByUserId()).isEqualTo(staffUser.getId());

        BookingFolio folio = bookingService.getFolio(booking.getId());
        assertThat(new BigDecimal(folio.getRoomChargesTotal())).isEqualByComparingTo("0.00");

        // The room portion is untouched by this - settling charges is not settling the room.
        BigDecimal outstanding = bookingService.computeOutstandingBalance(booking.getId());
        assertThat(outstanding).isEqualByComparingTo("1000.00"); // room only, still not PAID
    }

    @Test
    void recordFolioPayment_partialAmount_reducesButDoesNotZeroTheBalance() {
        BookingEntity booking = persistBooking(new BigDecimal("1000.00"));
        persistRoomChargePayment(booking, new BigDecimal("500.00"));

        bookingService.recordFolioPayment(booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "200.00"));

        BookingFolio folio = bookingService.getFolio(booking.getId());
        assertThat(new BigDecimal(folio.getRoomChargesTotal())).isEqualByComparingTo("300.00");
    }

    @Test
    void recordFolioPayment_multiplePartialPayments_accumulate() {
        BookingEntity booking = persistBooking(new BigDecimal("1000.00"));
        persistRoomChargePayment(booking, new BigDecimal("900.00"));

        bookingService.recordFolioPayment(booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "300.00"));
        bookingService.recordFolioPayment(booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CARD, "300.00"));
        bookingService.recordFolioPayment(booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "300.00"));

        BookingFolio folio = bookingService.getFolio(booking.getId());
        assertThat(new BigDecimal(folio.getRoomChargesTotal())).isEqualByComparingTo("0.00");

        List<FolioPayment> history = bookingService.listFolioPayments(booking.getId());
        assertThat(history).hasSize(3);
        assertThat(history).extracting(FolioPayment::getMethod).containsExactly(
                FolioPaymentMethod.CASH, FolioPaymentMethod.CARD, FolioPaymentMethod.CASH);
    }

    // --- Settlement fully clears both the checkout warning and the debt-badge signal -----------

    @Test
    void bookingAlreadyPaid_withRoomChargeFullySettled_hasNoOutstandingBalanceAtAll() {
        BookingEntity booking = persistBooking(new BigDecimal("1000.00"));
        persistRoomChargePayment(booking, new BigDecimal("500.00"));
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.PAID));
        bookingService.recordFolioPayment(booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "500.00"));

        BigDecimal outstanding = bookingService.computeOutstandingBalance(booking.getId());
        assertThat(outstanding).isEqualByComparingTo("0.00");

        BookingFolio folio = bookingService.getFolio(booking.getId());
        assertThat(new BigDecimal(folio.getRoomChargesTotal())).isEqualByComparingTo("0.00");
        // roomChargeCount stays a raw historical count - settling a charge doesn't erase that
        // this stay ever generated one.
        assertThat(folio.getRoomChargeCount()).isEqualTo(1);
    }

    // --- Guardrails ------------------------------------------------------------------------------

    @Test
    void recordFolioPayment_exceedingWhatIsOutstanding_isRejected() {
        BookingEntity booking = persistBooking(new BigDecimal("1000.00"));
        persistRoomChargePayment(booking, new BigDecimal("500.00"));

        assertThatCode(() -> bookingService.recordFolioPayment(
                        booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "500.01")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overpay");

        // Rejected write must not have recorded anything.
        assertThat(bookingService.listFolioPayments(booking.getId())).isEmpty();
    }

    @Test
    void recordFolioPayment_whenNothingIsOutstanding_isRejected() {
        BookingEntity booking = persistBooking(new BigDecimal("1000.00")); // no room charge at all

        assertThatCode(() -> bookingService.recordFolioPayment(
                        booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "0.01")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overpay");
    }

    @Test
    void recordFolioPayment_zeroOrNegativeAmount_isRejected() {
        BookingEntity booking = persistBooking(new BigDecimal("1000.00"));
        persistRoomChargePayment(booking, new BigDecimal("500.00"));

        assertThatCode(() -> bookingService.recordFolioPayment(
                        booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "0.00")))
                .isInstanceOf(ValidationException.class);
        assertThatCode(() -> bookingService.recordFolioPayment(
                        booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "-50.00")))
                .isInstanceOf(ValidationException.class);
    }
}
