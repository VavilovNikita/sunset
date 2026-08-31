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
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.BookingPosOrder;
import com.sunsetbeach.model.BookingStatus;
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
import jakarta.persistence.EntityManager;
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
 * DB-backed (real dev Postgres, rolled back after each test via @Transactional): covers the
 * two POS follow-up items - the GET /bookings date-range overlap fix, and the new
 * GET /bookings/{id}/pos-orders endpoint built on top of BookingService.computeFolio.
 */
@SpringBootTest
@Transactional
class BookingRoomChargeTests {

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

    @Autowired
    private EntityManager entityManager;

    private RoomEntity room;
    private UserEntity staffUser;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Test Room");
        newRoom.setDescription("A room used only by tests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);

        UserEntity newUser = new UserEntity();
        newUser.setEmail("cashier-" + UUID.randomUUID() + "@example.com");
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.CASHIER);
        staffUser = userRepository.saveAndFlush(newUser);
    }

    private BookingEntity persistBooking(LocalDate checkIn, LocalDate checkOut) {
        return persistBooking(checkIn, checkOut, "Guest");
    }

    private BookingEntity persistBooking(LocalDate checkIn, LocalDate checkOut, String guestName) {
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName(guestName);
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        // BookingEntity.room is a read-only association mapped over the same "roomId" column
        // as the plain roomId field. Clearing the persistence context here forces
        // BookingService.list()'s fetch-join query to hydrate a fresh Room from the result set
        // instead of Hibernate trying (and, within a single test transaction, sometimes
        // failing) to reconcile it against the already-managed instance sitting in this
        // session's identity map from the saveAndFlush() calls above.
        entityManager.clear();
        return saved;
    }

    // --- GET /bookings?from&to date-range overlap ---

    @Test
    void list_bookingSpanningToday_isFoundByFromToToday() {
        LocalDate today = LocalDate.now();
        BookingEntity booking = persistBooking(today.minusDays(1), today.plusDays(1));

        List<Booking> results = bookingService.list(today.toString(), today.toString(), null, null);

        assertThat(results).extracting(Booking::getId).contains(booking.getId());
    }

    @Test
    void list_bookingCheckingInToday_isFoundByFromToToday() {
        // The boundary case the strict "checkIn < to" used to drop: a booking that starts
        // exactly on the query day must still match from=to=<today>.
        LocalDate today = LocalDate.now();
        BookingEntity booking = persistBooking(today, today.plusDays(1));

        List<Booking> results = bookingService.list(today.toString(), today.toString(), null, null);

        assertThat(results).extracting(Booking::getId).contains(booking.getId());
    }

    @Test
    void list_bookingAlreadyCheckedOut_isNotFoundByFromToToday() {
        // Real dev-DB bookings can independently overlap "today", so assert this specific
        // booking is absent rather than asserting the whole result set is empty.
        LocalDate today = LocalDate.now();
        BookingEntity booking = persistBooking(today.minusDays(5), today.minusDays(3));

        List<Booking> results = bookingService.list(today.toString(), today.toString(), null, null);

        assertThat(results).extracting(Booking::getId).doesNotContain(booking.getId());
    }

    // --- GET /bookings?guestName - the mobile "charge to room" search ---

    @Test
    void list_guestNameSubstring_isCaseInsensitiveAndMatchesPartial() {
        LocalDate today = LocalDate.now();
        BookingEntity booking = persistBooking(today, today.plusDays(1), "Somchai Testworth-" + UUID.randomUUID());

        List<Booking> results = bookingService.list(null, null, null, "testworth");

        assertThat(results).extracting(Booking::getId).contains(booking.getId());
    }

    @Test
    void list_guestNameSubstring_combinesWithDateRange() {
        LocalDate today = LocalDate.now();
        String uniqueName = "Napat Riverstone-" + UUID.randomUUID();
        BookingEntity inRange = persistBooking(today, today.plusDays(1), uniqueName);
        BookingEntity outOfRange = persistBooking(today.minusDays(10), today.minusDays(8), uniqueName);

        List<Booking> results = bookingService.list(today.toString(), today.toString(), null, "riverstone");

        assertThat(results).extracting(Booking::getId).contains(inRange.getId()).doesNotContain(outOfRange.getId());
    }

    @Test
    void list_guestNameWithNoMatch_isEmpty() {
        List<Booking> results = bookingService.list(null, null, null, "no-such-guest-" + UUID.randomUUID());

        assertThat(results).isEmpty();
    }

    // --- GET /bookings/{id}/pos-orders ---

    @Test
    void listPosOrders_returnsRoomChargeOrdersWithItemBreakdown() {
        BookingEntity booking = persistBooking(LocalDate.now(), LocalDate.now().plusDays(2));

        MenuItemEntity menuItem = new MenuItemEntity();
        menuItem.setName("Pad Thai");
        menuItem.setDescription("Stir-fried rice noodles");
        menuItem.setCategory("Mains");
        menuItem.setPrice(new BigDecimal("250.00"));
        menuItem.setAvailable(true);
        menuItem = menuItemRepository.saveAndFlush(menuItem);

        OrderEntity order = new OrderEntity();
        order.setBookingId(booking.getId());
        order.setOpenedByUserId(staffUser.getId());
        order = orderRepository.saveAndFlush(order);

        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(order.getId());
        item.setMenuItemId(menuItem.getId());
        item.setQuantity(2);
        item.setUnitPrice(menuItem.getPrice());
        orderItemRepository.saveAndFlush(item);

        ShiftEntity shift = new ShiftEntity();
        shift.setOpenedByUserId(staffUser.getId());
        shift = shiftRepository.saveAndFlush(shift);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod(PaymentMethod.ROOM_CHARGE);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setBookingId(booking.getId());
        payment.setRecordedByUserId(staffUser.getId());
        payment.setShiftId(shift.getId());
        paymentRepository.saveAndFlush(payment);

        List<BookingPosOrder> result = bookingService.listPosOrders(booking.getId());

        assertThat(result).hasSize(1);
        BookingPosOrder posOrder = result.get(0);
        assertThat(posOrder.getOrderId()).isEqualTo(order.getId());
        assertThat(posOrder.getAmount()).isEqualTo("500.00");
        assertThat(posOrder.getItems()).hasSize(1);
        assertThat(posOrder.getItems().get(0).getName()).isEqualTo("Pad Thai");
        assertThat(posOrder.getItems().get(0).getQuantity()).isEqualTo(2);

        // Same underlying query as computeFolio - the two must agree.
        BigDecimal folio = bookingService.computeFolio(booking.getId());
        assertThat(folio).isEqualByComparingTo(booking.getTotalPrice().add(payment.getAmount()));
    }

    @Test
    void listPosOrders_bookingWithNoRoomCharges_isEmpty() {
        BookingEntity booking = persistBooking(LocalDate.now(), LocalDate.now().plusDays(1));

        assertThat(bookingService.listPosOrders(booking.getId())).isEmpty();
    }

    @Test
    void listPosOrders_unknownBooking_throwsNotFound() {
        assertThatThrownBy(() -> bookingService.listPosOrders("does-not-exist")).isInstanceOf(NotFoundException.class);
    }

    // --- GET /bookings/{id}/folio ---

    @Test
    void getFolio_withRoomCharges_sumsBookingTotalAndPayments() {
        BookingEntity booking = persistBooking(LocalDate.now(), LocalDate.now().plusDays(2));
        persistRoomChargePayment(booking, new BigDecimal("500.00"));
        persistRoomChargePayment(booking, new BigDecimal("250.00"));

        BookingFolio folio = bookingService.getFolio(booking.getId());

        assertThat(folio.getRoomTotal()).isEqualTo("1000.00");
        assertThat(folio.getRoomChargesTotal()).isEqualTo("750.00");
        assertThat(folio.getFolioTotal()).isEqualTo("1750.00");
        assertThat(folio.getRoomChargeCount()).isEqualTo(2);

        // Must agree with computeFolio - same computeFolioBreakdown() underneath.
        assertThat(bookingService.computeFolio(booking.getId())).isEqualByComparingTo(folio.getFolioTotal());
    }

    @Test
    void getFolio_noRoomCharges_isJustTheRoomTotal() {
        BookingEntity booking = persistBooking(LocalDate.now(), LocalDate.now().plusDays(1));

        BookingFolio folio = bookingService.getFolio(booking.getId());

        assertThat(folio.getRoomTotal()).isEqualTo("1000.00");
        assertThat(folio.getRoomChargesTotal()).isEqualTo("0.00");
        assertThat(folio.getFolioTotal()).isEqualTo("1000.00");
        assertThat(folio.getRoomChargeCount()).isEqualTo(0);
    }

    @Test
    void getFolio_unknownBooking_throwsNotFound() {
        assertThatThrownBy(() -> bookingService.getFolio("does-not-exist")).isInstanceOf(NotFoundException.class);
    }

    /** Builds a MenuItem+Order+OrderItem+Shift+ROOM_CHARGE Payment against {@code booking}. */
    private PaymentEntity persistRoomChargePayment(BookingEntity booking, BigDecimal amount) {
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
        // staffUser, and only one OPEN shift per user is allowed (Shift_one_open_per_user).
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
        return paymentRepository.saveAndFlush(payment);
    }
}
