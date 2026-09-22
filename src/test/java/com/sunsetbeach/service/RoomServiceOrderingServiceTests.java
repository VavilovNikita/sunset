package com.sunsetbeach.service;

import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.GuestOrderView;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.RoomRepository;
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
 * DB-backed coverage of {@link RoomServiceOrderingService} - the gate ("this booking is this
 * guest's own, and currently CHECKED_IN") in front of the ordinary {@link OrderService} machinery.
 * Mirrors {@code GuestOrderAccessTests}' focus on the gate itself, not a re-proof of {@code
 * addItems}'s merge/print-dispatch behavior (already covered by {@code OrderItemMergingTests}).
 */
@SpringBootTest
@Transactional
class RoomServiceOrderingServiceTests extends AbstractIntegrationTest {

    private static final String GUEST_EMAIL = "guest@example.com";

    @Autowired
    private RoomServiceOrderingService roomServiceOrderingService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftService shiftService;

    private String roomId;
    private MenuItemEntity mojito;

    @BeforeEach
    void setUp() {
        RoomEntity room = new RoomEntity();
        room.setName("Room Service Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by RoomServiceOrderingServiceTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        roomId = roomRepository.saveAndFlush(room).getId();

        mojito = new MenuItemEntity();
        mojito.setName("Mojito");
        mojito.setDescription("test item");
        mojito.setCategory("Test");
        mojito.setDepartment(MenuDepartment.BAR);
        mojito.setPrice(new BigDecimal("150.00"));
        mojito.setAvailable(true);
        mojito = menuItemRepository.saveAndFlush(mojito);
    }

    private String checkedInBooking(String guestEmail) {
        return persistBooking(guestEmail, OccupancyStatus.CHECKED_IN);
    }

    private String persistBooking(String guestEmail, OccupancyStatus occupancyStatus) {
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(roomId);
        booking.setGuestName("Somchai");
        booking.setGuestEmail(guestEmail);
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.now().minusDays(1));
        booking.setCheckOut(LocalDate.now().plusDays(2));
        booking.setTotalPrice(new BigDecimal("3000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setOccupancyStatus(occupancyStatus);
        return bookingRepository.saveAndFlush(booking).getId();
    }

    private List<OrderItemInput> oneMojito() {
        return List.of(new OrderItemInput(mojito.getId(), 1));
    }

    @Test
    void checkedInGuest_submit_orderLandsSent_andItemsCarryThrough() {
        String bookingId = checkedInBooking(GUEST_EMAIL);

        GuestOrderView view = roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito());

        assertThat(view.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(view.getItems()).hasSize(1);
        assertThat(view.getItems().get(0).getName()).isEqualTo("Mojito");
        assertThat(view.getTotal()).isEqualTo("150.00");

        OrderEntity order = orderRepository.findById(view.getId()).orElseThrow();
        assertThat(order.getBookingId()).isEqualTo(bookingId);
        assertThat(order.getTableId()).isNull();
        assertThat(order.getOpenedByUserId()).isNull();
    }

    @Test
    void expected_submit_isRejected() {
        String bookingId = persistBooking(GUEST_EMAIL, OccupancyStatus.EXPECTED);
        assertThatThrownBy(() -> roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void checkedOut_submit_isRejected() {
        String bookingId = persistBooking(GUEST_EMAIL, OccupancyStatus.CHECKED_OUT);
        assertThatThrownBy(() -> roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void noShow_submit_isRejected() {
        String bookingId = persistBooking(GUEST_EMAIL, OccupancyStatus.NO_SHOW);
        assertThatThrownBy(() -> roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void wrongOwner_submit_isRejected() {
        String bookingId = checkedInBooking("someone-else@example.com");
        assertThatThrownBy(() -> roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownBooking_submit_isRejected() {
        assertThatThrownBy(() -> roomServiceOrderingService.submit(GUEST_EMAIL, "does-not-exist", oneMojito()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void ownerMatch_isCaseInsensitive() {
        String bookingId = checkedInBooking("Guest@Example.com");
        GuestOrderView view = roomServiceOrderingService.submit("guest@example.com", bookingId, oneMojito());
        assertThat(view.getStatus()).isEqualTo(OrderStatus.SENT);
    }

    @Test
    void secondRound_whileStillCheckedIn_succeeds_andMergesWithExistingLine() {
        String bookingId = checkedInBooking(GUEST_EMAIL);
        GuestOrderView first = roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito());

        GuestOrderView second = roomServiceOrderingService.addItems(GUEST_EMAIL, first.getId(), oneMojito());

        assertThat(second.getItems()).hasSize(1);
        assertThat(second.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(second.getTotal()).isEqualTo("300.00");
    }

    @Test
    void secondRound_afterCheckedOutBetweenCalls_isRejected() {
        String bookingId = checkedInBooking(GUEST_EMAIL);
        GuestOrderView first = roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito());

        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setOccupancyStatus(OccupancyStatus.CHECKED_OUT);
        bookingRepository.saveAndFlush(booking);

        assertThatThrownBy(() -> roomServiceOrderingService.addItems(GUEST_EMAIL, first.getId(), oneMojito()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addItems_wrongOwner_isRejected() {
        String bookingId = checkedInBooking(GUEST_EMAIL);
        GuestOrderView order = roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito());

        assertThatThrownBy(() -> roomServiceOrderingService.addItems("someone-else@example.com", order.getId(), oneMojito()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_ownOrder_succeedsEvenAfterCheckOut() {
        String bookingId = checkedInBooking(GUEST_EMAIL);
        GuestOrderView order = roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito());

        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setOccupancyStatus(OccupancyStatus.CHECKED_OUT);
        bookingRepository.saveAndFlush(booking);

        GuestOrderView view = roomServiceOrderingService.getById(GUEST_EMAIL, order.getId());
        assertThat(view.getId()).isEqualTo(order.getId());
    }

    @Test
    void getById_wrongOwner_isRejected() {
        String bookingId = checkedInBooking(GUEST_EMAIL);
        GuestOrderView order = roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito());

        assertThatThrownBy(() -> roomServiceOrderingService.getById("someone-else@example.com", order.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_unknownOrder_isRejected() {
        assertThatThrownBy(() -> roomServiceOrderingService.getById(GUEST_EMAIL, "does-not-exist"))
                .isInstanceOf(NotFoundException.class);
    }

    /** A dine-in order that merely carries this guest's own bookingId (staff pre-fill) is never reachable through this guest-facing door - see the service's own class javadoc. */
    @Test
    void tableOrderCarryingThisGuestsBookingId_isNeverVisibleThroughThisDoor() {
        String bookingId = checkedInBooking(GUEST_EMAIL);
        TableEntity table = new TableEntity();
        table.setZone(Zone.RESTAURANT);
        table.setLabel("T1-" + UUID.randomUUID());
        table.setCapacity(4);
        table.setActive(true);
        table = tableRepository.saveAndFlush(table);
        Order tableOrder = orderService.create(new OrderCreateInput().bookingId(bookingId).tableId(table.getId()), null);

        assertThatThrownBy(() -> roomServiceOrderingService.getById(GUEST_EMAIL, tableOrder.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> roomServiceOrderingService.addItems(GUEST_EMAIL, tableOrder.getId(), oneMojito()))
                .isInstanceOf(NotFoundException.class);
    }

    /**
     * No code change was expected for this - {@code OrderService#close} already treats a
     * room-service order like any other {@code Order}; this only confirms that stays true now
     * that {@code openedByUserId} can be null on the order being closed.
     */
    @Test
    void closeWithRoomCharge_stillWorksUnchangedForARoomServiceOrder() {
        String bookingId = checkedInBooking(GUEST_EMAIL);
        GuestOrderView placed = roomServiceOrderingService.submit(GUEST_EMAIL, bookingId, oneMojito());

        UserEntity cashier = new UserEntity();
        cashier.setEmail("cashier-" + UUID.randomUUID() + "@example.com");
        cashier.setName(cashier.getEmail());
        cashier.setPasswordHash("irrelevant-for-this-test");
        cashier.setRole(Role.CASHIER);
        cashier = userRepository.saveAndFlush(cashier);
        shiftService.open(cashier.getId(), new ShiftOpenInput());

        Order closed = orderService.close(placed.getId(), new CloseOrderInput(PaymentMethod.ROOM_CHARGE).bookingId(bookingId), cashier.getId());

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(closed.getPaymentMethod().get()).isEqualTo(PaymentMethod.ROOM_CHARGE);
    }
}
