package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DB-backed (real dev Postgres), NOT {@code @Transactional} - {@code POST /orders/{id}/close}
 * needs a real open {@code Shift} and writes {@code AuditLogService.record} entries in their own
 * {@code REQUIRES_NEW} transaction (see that class's own javadoc), which would survive this
 * test's rollback anyway if it had one; cleanup is manual, by id, in {@link #cleanUp()}, same
 * pattern as {@code OrderShiftAuditLogTests}.
 *
 * <p>The *booking* axis of {@code SpaAppointment.orderId} auto-resolution - {@link
 * OrderService#autoLinkSpaAppointmentByBooking}, run from {@link OrderService#close} only when
 * the close is {@code ROOM_CHARGE}, using the booking that close names, not {@code
 * Order.bookingId} (never populated by any real caller - see that field's own openapi.yaml
 * description). The *table* axis ({@link OrderService#autoLinkSpaAppointmentByTable}, run from
 * {@link OrderService#addItems}) has its own test class, {@code OrderSpaAppointmentLinkTests}.
 */
@SpringBootTest
class OrderCloseSpaAppointmentLinkTests extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpaAppointmentRepository spaAppointmentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdSpaAppointmentIds = new ArrayList<>();
    private final List<String> createdOrderIds = new ArrayList<>();
    private final List<String> createdShiftIds = new ArrayList<>();
    private final List<String> createdBookingIds = new ArrayList<>();
    private final List<String> createdRoomIds = new ArrayList<>();
    private final List<String> createdTableIds = new ArrayList<>();
    private final List<String> createdMenuItemIds = new ArrayList<>();
    private final List<String> createdUserIds = new ArrayList<>();

    private String cashierId;

    @BeforeEach
    void setUp() {
        UserEntity cashier = new UserEntity();
        cashier.setEmail("order-close-link-cashier-" + UUID.randomUUID() + "@example.com");
        cashier.setPasswordHash("irrelevant-for-this-test");
        cashier.setRole(Role.CASHIER);
        cashierId = userRepository.saveAndFlush(cashier).getId();
        createdUserIds.add(cashierId);

        StaffPrincipal principal = new StaffPrincipal(cashierId, cashier.getEmail(), Role.CASHIER);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_CASHIER"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String appointmentId : createdSpaAppointmentIds) {
            spaAppointmentRepository.deleteById(appointmentId);
        }
        for (String orderId : createdOrderIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.ORDER, orderId));
            paymentRepository.deleteAll(paymentRepository.findAll().stream().filter(p -> orderId.equals(p.getOrderId())).toList());
            orderItemRepository.deleteAll(orderItemRepository.findByOrderId(orderId));
            orderRepository.deleteById(orderId);
        }
        for (String shiftId : createdShiftIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.SHIFT, shiftId));
            shiftRepository.deleteById(shiftId);
        }
        for (String bookingId : createdBookingIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.BOOKING, bookingId));
            bookingRepository.deleteById(bookingId);
        }
        for (String roomId : createdRoomIds) {
            for (var unit : roomUnitRepository.findByRoomId(roomId)) {
                roomUnitRepository.deleteById(unit.getId());
            }
            roomRepository.deleteById(roomId);
        }
        for (String tableId : createdTableIds) {
            tableRepository.deleteById(tableId);
        }
        for (String menuItemId : createdMenuItemIds) {
            menuItemRepository.deleteById(menuItemId);
        }
        for (String userId : createdUserIds) {
            userRepository.deleteById(userId);
        }
    }

    private List<AuditLogEntity> entriesFor(AuditEntityType entityType, String entityId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == entityType && entityId.equals(e.getEntityId()))
                .toList();
    }

    private Booking createBooking(LocalDate checkIn) {
        RoomEntity room = new RoomEntity();
        room.setName("Order Close Link Room " + UUID.randomUUID());
        room.setDescription("Used only by OrderCloseSpaAppointmentLinkTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());
        var unit = new com.sunsetbeach.entity.RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Order Close Link Unit " + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);
        Booking booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(savedRoom.getId(), "Order Close Link Guest", checkIn.toString(), checkIn.plusDays(2).toString())
                        .roomUnitId(unit.getId()));
        createdBookingIds.add(booking.getId());
        return booking;
    }

    private TableEntity createSpaTable() {
        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Order Close Link Table " + UUID.randomUUID());
        table.setCapacity(1);
        table.setActive(true);
        TableEntity saved = tableRepository.saveAndFlush(table);
        createdTableIds.add(saved.getId());
        return saved;
    }

    private TableEntity createRestaurantTable() {
        TableEntity table = new TableEntity();
        table.setZone(Zone.RESTAURANT);
        table.setLabel("Order Close Link Restaurant Table " + UUID.randomUUID());
        table.setCapacity(2);
        table.setActive(true);
        TableEntity saved = tableRepository.saveAndFlush(table);
        createdTableIds.add(saved.getId());
        return saved;
    }

    private MenuItemEntity createTreatment() {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Order Close Link Massage " + UUID.randomUUID());
        item.setDescription("Used only by OrderCloseSpaAppointmentLinkTests");
        item.setCategory("Massage");
        item.setDepartment(MenuDepartment.SPA);
        item.setPrice(new BigDecimal("1500.00"));
        item.setAvailable(true);
        item.setDurationMinutes(60);
        MenuItemEntity saved = menuItemRepository.saveAndFlush(item);
        createdMenuItemIds.add(saved.getId());
        return saved;
    }

    /** A perfectly ordinary, non-treatment line - a bottled water, say - used to prove the SPA-content gate applies at close time too. */
    private MenuItemEntity createKitchenItem() {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Order Close Link Water " + UUID.randomUUID());
        item.setDescription("Used only by OrderCloseSpaAppointmentLinkTests");
        item.setCategory("Drinks");
        item.setDepartment(MenuDepartment.KITCHEN);
        item.setPrice(new BigDecimal("100.00"));
        item.setAvailable(true);
        MenuItemEntity saved = menuItemRepository.saveAndFlush(item);
        createdMenuItemIds.add(saved.getId());
        return saved;
    }

    private UserEntity createTherapist() {
        UserEntity user = new UserEntity();
        user.setEmail("order-close-link-therapist-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant");
        user.setRole(Role.WAITER);
        user.setActive(true);
        user.setJobFunctions(new String[] {JobFunction.THERAPIST.getValue()});
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    /** Persisted directly (bypassing SpaAppointmentService's opening-hours validation), same reasoning as OrderSpaAppointmentLinkTests. */
    private SpaAppointmentEntity persistAppointment(TableEntity table, Booking booking, MenuItemEntity treatment, UserEntity therapist, LocalTime startTime, int durationMinutes) {
        SpaAppointmentEntity entity = new SpaAppointmentEntity();
        entity.setTableId(table.getId());
        entity.setBookingId(booking.getId());
        entity.setTherapistUserId(therapist.getId());
        entity.setCreatedByUserId(cashierId);
        entity.setDate(LocalDate.now());
        entity.setStartTime(startTime);
        entity.setDurationMinutes(durationMinutes);
        entity.setStatus(SpaAppointmentStatus.BOOKED);
        SpaAppointmentEntity saved = spaAppointmentRepository.saveAndFlush(entity);
        createdSpaAppointmentIds.add(saved.getId());
        return saved;
    }

    private String orderIdOf(SpaAppointmentEntity appointment) {
        return spaAppointmentRepository.findById(appointment.getId()).orElseThrow().getOrderId();
    }

    private Order openOrder(String tableId) {
        OrderCreateInput input = new OrderCreateInput();
        if (tableId != null) {
            input.tableId(tableId);
        }
        Order order = orderService.create(input, cashierId);
        createdOrderIds.add(order.getId());
        return order;
    }

    private void bill(String orderId, MenuItemEntity item) {
        orderService.addItems(orderId, List.of(new OrderItemInput(item.getId(), 1)));
    }

    private void openShift() {
        var shift = shiftService.open(cashierId, new ShiftOpenInput());
        createdShiftIds.add(shift.getId());
    }

    private Order closeRoomCharge(String orderId, String bookingId) {
        CloseOrderInput input = new CloseOrderInput(PaymentMethod.ROOM_CHARGE);
        input.setBookingId(bookingId);
        return orderService.close(orderId, input, cashierId);
    }

    /**
     * The exact scenario a table-less treatment order most likely takes in practice: a guest with
     * an appointment today, a table-less ticket carrying the treatment item, closed as a room
     * charge against that guest's booking. Must link.
     */
    @Test
    void closeRoomCharge_tableLessTicketWithTreatmentItem_linksToTodaysAppointment() {
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity appointment = persistAppointment(createSpaTable(), booking, treatment, therapist, LocalTime.of(14, 0), 60);
        openShift();

        Order order = openOrder(null);
        bill(order.getId(), treatment);
        Order closed = closeRoomCharge(order.getId(), booking.getId());

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(orderIdOf(appointment)).isEqualTo(order.getId());
    }

    /** The same ticket, but with only food on it - never a treatment, so it must not link even though the booking's appointment is unambiguous. */
    @Test
    void closeRoomCharge_tableLessTicketWithOnlyFoodItem_neverLinks() {
        MenuItemEntity treatment = createTreatment();
        MenuItemEntity kitchenItem = createKitchenItem();
        UserEntity therapist = createTherapist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity appointment = persistAppointment(createSpaTable(), booking, treatment, therapist, LocalTime.of(14, 0), 60);
        openShift();

        Order order = openOrder(null);
        bill(order.getId(), kitchenItem);
        Order closed = closeRoomCharge(order.getId(), booking.getId());

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(orderIdOf(appointment)).isNull();
    }

    @Test
    void closeRoomCharge_bookingWithTwoUnlinkedAppointments_declinesToGuess() {
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity first = persistAppointment(createSpaTable(), booking, treatment, therapist, LocalTime.of(10, 0), 60);
        SpaAppointmentEntity second = persistAppointment(createSpaTable(), booking, treatment, therapist, LocalTime.of(15, 0), 60);
        openShift();

        Order order = openOrder(null);
        bill(order.getId(), treatment);
        Order closed = closeRoomCharge(order.getId(), booking.getId());

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(orderIdOf(first)).isNull();
        assertThat(orderIdOf(second)).isNull();
    }

    /** The zone gate applies to the booking axis too - a restaurant order charged to the room must never guess its way onto a spa appointment. */
    @Test
    void closeRoomCharge_nonSpaTableWithBooking_neverAttemptsToLink() {
        TableEntity restaurantTable = createRestaurantTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity appointment = persistAppointment(createSpaTable(), booking, treatment, therapist, LocalTime.of(11, 0), 60);
        openShift();

        Order order = openOrder(restaurantTable.getId());
        // Even billing a treatment-department item doesn't matter here - the zone gate on the order's own table rules it out first.
        bill(order.getId(), treatment);
        Order closed = closeRoomCharge(order.getId(), booking.getId());

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(orderIdOf(appointment)).isNull();
    }

    /**
     * An order already linked by the table axis (at addItems time, before this booking ever
     * closed against it) must never be re-resolved at close onto a different appointment, even
     * one that names the exact booking the close itself is charging - the existsByOrderId guard
     * both axes share must hold across the addItems/close boundary, not just within one call.
     */
    @Test
    void closeRoomCharge_orderAlreadyLinkedByTableAxis_neverReResolvesAtClose() {
        TableEntity liveTable = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        // A second, distinct therapist for the room-charge booking's own appointment - the
        // exclusion constraint is scoped per therapist (see V41's own comment), so a fixed
        // clock-time appointment on the SAME therapist as the "live" one below (LocalTime.now(),
        // a 120-minute window) can spuriously collide depending on what time the suite happens to
        // run at. Nothing about this test cares whether the two appointments share a therapist.
        UserEntity secondTherapist = createTherapist();

        Booking tableBooking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity liveAppointment = persistAppointment(liveTable, tableBooking, treatment, therapist, LocalTime.now(), 120);

        Booking roomChargeBooking = createBooking(LocalDate.now().plusDays(301));
        SpaAppointmentEntity roomChargeBookingsOwnAppointment =
                persistAppointment(createSpaTable(), roomChargeBooking, treatment, secondTherapist, LocalTime.of(20, 0), 60);
        openShift();

        Order order = openOrder(liveTable.getId());
        bill(order.getId(), treatment); // links via the table axis, to liveAppointment, right here
        assertThat(orderIdOf(liveAppointment)).isEqualTo(order.getId());

        Order closed = closeRoomCharge(order.getId(), roomChargeBooking.getId());

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(orderIdOf(liveAppointment)).isEqualTo(order.getId());
        assertThat(orderIdOf(roomChargeBookingsOwnAppointment)).isNull();
    }

    /** No booking at all on a plain cash close - the booking axis must never run, and the close itself must succeed normally regardless. */
    @Test
    void closeCash_neverAttemptsBookingLinkAndStillClosesNormally() {
        MenuItemEntity treatment = createTreatment();
        openShift();

        Order order = openOrder(null);
        bill(order.getId(), treatment);
        Order closed = orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH), cashierId);

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
