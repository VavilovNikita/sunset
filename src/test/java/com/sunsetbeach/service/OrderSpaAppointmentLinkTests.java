package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): how {@code SpaAppointment.orderId}
 * actually gets set. Both `/pos` and `/admin/pos` open an order the same way - {@code POST
 * /orders} with nothing but a {@code tableId} and/or {@code bookingId} - so the auto-link in
 * {@link OrderService#autoLinkSpaAppointment} is what makes the ordinary path set the link
 * without either screen having to know spa appointments exist; explicit {@code spaAppointmentId}
 * stays as the override for when auto-resolution can't or shouldn't guess.
 *
 * <p>Auto-resolution runs from {@link OrderService#addItems}, not {@link OrderService#create} -
 * an order has no items at creation, and resolution now requires one to be a {@code SPA}-
 * department item (see the class javadoc on {@code autoLinkSpaAppointment} for the correction
 * this closes). Every auto-resolution test here therefore creates the order, then calls
 * {@code addItems} with the treatment line before asserting on the link - a test that only calls
 * {@code create} would be testing behavior that no longer exists.
 *
 * <p>Appointments here are persisted directly via {@link SpaAppointmentRepository}, not through
 * {@link SpaAppointmentService#create}, specifically so their times can be set relative to
 * {@link LocalDateTime#now()} without also having to fall inside the spa's configured opening
 * hours (irrelevant to what's under test here - the resolution logic, not appointment creation
 * validation). The exclusion constraint still applies either way; it's a database constraint, not
 * a service-layer check.
 */
@SpringBootTest
@Transactional
class OrderSpaAppointmentLinkTests extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private BookingService bookingService;

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

    private Booking createBooking(LocalDate checkIn) {
        RoomEntity room = new RoomEntity();
        room.setName("Order Link Room " + UUID.randomUUID());
        room.setDescription("Used only by OrderSpaAppointmentLinkTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Order Link Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);
        return bookingService.createStaffBooking(
                new StaffBookingCreateInput(savedRoom.getId(), "Order Link Guest", checkIn.toString(), checkIn.plusDays(2).toString())
                        .roomUnitId(savedUnit.getId()));
    }

    private TableEntity createSpaTable() {
        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Order Link Table " + UUID.randomUUID());
        table.setCapacity(1);
        table.setActive(true);
        return tableRepository.saveAndFlush(table);
    }

    private TableEntity createRestaurantTable() {
        TableEntity table = new TableEntity();
        table.setZone(Zone.RESTAURANT);
        table.setLabel("Order Link Restaurant Table " + UUID.randomUUID());
        table.setCapacity(2);
        table.setActive(true);
        return tableRepository.saveAndFlush(table);
    }

    private MenuItemEntity createTreatment() {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Order Link Massage " + UUID.randomUUID());
        item.setDescription("Used only by OrderSpaAppointmentLinkTests");
        item.setCategory("Massage");
        item.setDepartment(MenuDepartment.SPA);
        item.setPrice(new BigDecimal("1500.00"));
        item.setAvailable(true);
        item.setDurationMinutes(60);
        return menuItemRepository.saveAndFlush(item);
    }

    /** A perfectly ordinary, non-treatment line - a bottled water, say - used to prove the content gate actually excludes on department. */
    private MenuItemEntity createKitchenItem() {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Order Link Water " + UUID.randomUUID());
        item.setDescription("Used only by OrderSpaAppointmentLinkTests");
        item.setCategory("Drinks");
        item.setDepartment(MenuDepartment.KITCHEN);
        item.setPrice(new BigDecimal("100.00"));
        item.setAvailable(true);
        return menuItemRepository.saveAndFlush(item);
    }

    private UserEntity createTherapist() {
        UserEntity user = new UserEntity();
        user.setEmail("order-link-therapist-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant");
        user.setRole(Role.WAITER);
        user.setActive(true);
        user.setJobFunctions(new String[] {JobFunction.THERAPIST.getValue()});
        return userRepository.saveAndFlush(user);
    }

    private UserEntity createReceptionist() {
        UserEntity user = new UserEntity();
        user.setEmail("order-link-reception-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant");
        user.setRole(Role.CASHIER);
        user.setActive(true);
        return userRepository.saveAndFlush(user);
    }

    /** Persisted directly (bypassing SpaAppointmentService's opening-hours validation) - see the class javadoc. */
    private SpaAppointmentEntity persistAppointment(
            TableEntity table, Booking booking, MenuItemEntity treatment, UserEntity therapist, UserEntity actor, LocalDateTime start, int durationMinutes) {
        SpaAppointmentEntity entity = new SpaAppointmentEntity();
        entity.setTableId(table.getId());
        entity.setBookingId(booking.getId());
        entity.setTreatmentMenuItemId(treatment.getId());
        entity.setTherapistUserId(therapist.getId());
        entity.setCreatedByUserId(actor.getId());
        entity.setDate(start.toLocalDate());
        entity.setStartTime(start.toLocalTime());
        entity.setDurationMinutes(durationMinutes);
        entity.setStatus(SpaAppointmentStatus.BOOKED);
        return spaAppointmentRepository.saveAndFlush(entity);
    }

    private String orderIdOf(SpaAppointmentEntity appointment) {
        return spaAppointmentRepository.findById(appointment.getId()).orElseThrow().getOrderId();
    }

    /** Adds one unit of the given item to the order - the moment auto-resolution actually runs. */
    private void billTreatment(String orderId, MenuItemEntity item) {
        orderService.addItems(orderId, List.of(new OrderItemInput(item.getId(), 1)));
    }

    /**
     * A fixed today+time, for tests on the booking axis (date-only, not time-of-day sensitive -
     * see {@link OrderService#resolveByBooking}) that must NOT be built as an offset from
     * {@code LocalDateTime.now()}: an offset of a couple of hours can cross midnight depending on
     * when the suite happens to run, silently landing the appointment on tomorrow's date and
     * making the test flaky. A fixed time-of-day on today's actual date has no such risk.
     */
    private static LocalDateTime todayAt(int hour, int minute) {
        return LocalDateTime.of(LocalDate.now(), java.time.LocalTime.of(hour, minute));
    }

    /**
     * The scenario the correction named directly: a table busy all day (three appointments, gaps
     * between them) - the order must link to whichever one is actually happening, not decline
     * because the day has more than one candidate.
     */
    @Test
    void addItems_busyTableDuringSecondAppointment_linksToTheSecond() {
        LocalDateTime now = LocalDateTime.now();
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking bookingA = createBooking(LocalDate.now().plusDays(300));
        Booking bookingB = createBooking(LocalDate.now().plusDays(301));
        Booking bookingC = createBooking(LocalDate.now().plusDays(302));

        SpaAppointmentEntity first = persistAppointment(table, bookingA, treatment, therapist, receptionist, now.minusMinutes(150), 60);
        SpaAppointmentEntity second = persistAppointment(table, bookingB, treatment, therapist, receptionist, now.minusMinutes(15), 60);
        SpaAppointmentEntity third = persistAppointment(table, bookingC, treatment, therapist, receptionist, now.plusMinutes(120), 60);

        Order order = orderService.create(new OrderCreateInput().tableId(table.getId()), receptionist.getId());
        billTreatment(order.getId(), treatment);

        assertThat(orderIdOf(second)).isEqualTo(order.getId());
        assertThat(orderIdOf(first)).isNull();
        assertThat(orderIdOf(third)).isNull();
    }

    /**
     * The common case named directly: the order is opened a few minutes after the treatment
     * actually finished, not during it. Within app.spa.order-link-grace-minutes (30 by default)
     * of the appointment's end, it still links.
     */
    @Test
    void addItems_shortlyAfterAppointmentEnded_stillLinksWithinGraceWindow() {
        LocalDateTime now = LocalDateTime.now();
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));

        // Ended 10 minutes ago (60-minute treatment starting 70 minutes ago) - well inside the 30-minute grace window.
        SpaAppointmentEntity appointment = persistAppointment(table, booking, treatment, therapist, receptionist, now.minusMinutes(70), 60);

        Order order = orderService.create(new OrderCreateInput().tableId(table.getId()), receptionist.getId());
        billTreatment(order.getId(), treatment);

        assertThat(orderIdOf(appointment)).isEqualTo(order.getId());
    }

    @Test
    void addItems_wellAfterAppointmentEnded_leavesOrderUnlinked() {
        LocalDateTime now = LocalDateTime.now();
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));

        // Ended 40 minutes ago - past the 30-minute grace window, and nothing else on the table today.
        SpaAppointmentEntity appointment = persistAppointment(table, booking, treatment, therapist, receptionist, now.minusMinutes(100), 60);

        Order order = orderService.create(new OrderCreateInput().tableId(table.getId()), receptionist.getId());
        billTreatment(order.getId(), treatment);

        assertThat(order.getId()).isNotNull();
        assertThat(orderIdOf(appointment)).isNull();
    }

    /** Two candidates both inside the grace window at once - genuinely ambiguous, must decline rather than guess. */
    @Test
    void addItems_twoAppointmentsBothRecentlyEnded_declinesToGuess() {
        LocalDateTime now = LocalDateTime.now();
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking bookingA = createBooking(LocalDate.now().plusDays(300));
        Booking bookingB = createBooking(LocalDate.now().plusDays(301));

        // A 10-minute treatment ending 5 minutes ago, and another (non-overlapping) ending 25 minutes ago - both within grace.
        SpaAppointmentEntity first = persistAppointment(table, bookingA, treatment, therapist, receptionist, now.minusMinutes(15), 10);
        SpaAppointmentEntity second = persistAppointment(table, bookingB, treatment, therapist, receptionist, now.minusMinutes(35), 10);

        Order order = orderService.create(new OrderCreateInput().tableId(table.getId()), receptionist.getId());
        billTreatment(order.getId(), treatment);

        assertThat(orderIdOf(first)).isNull();
        assertThat(orderIdOf(second)).isNull();
    }

    @Test
    void addItems_forNonSpaTable_neverAttemptsToLink() {
        TableEntity restaurantTable = createRestaurantTable();
        MenuItemEntity kitchenItem = createKitchenItem();

        Order order = orderService.create(new OrderCreateInput().tableId(restaurantTable.getId()), createReceptionist().getId());
        billTreatment(order.getId(), kitchenItem);

        assertThat(order.getId()).isNotNull();
    }

    /**
     * A room-charge order carries a bookingId and often no tableId at all - the table axis would
     * never fire for it, so the booking axis (tried first) is the only way this ever resolves.
     */
    @Test
    void addItems_roomChargeOrderWithNoTable_linksViaBookingWhenUnambiguous() {
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        // The booking axis is date-only, not time-of-day sensitive - see todayAt's own comment.
        SpaAppointmentEntity appointment = persistAppointment(table, booking, treatment, therapist, receptionist, todayAt(15, 0), 60);

        Order order = orderService.create(new OrderCreateInput().bookingId(booking.getId()), receptionist.getId());
        billTreatment(order.getId(), treatment);

        assertThat(orderIdOf(appointment)).isEqualTo(order.getId());
    }

    @Test
    void addItems_bookingWithTwoUnlinkedAppointments_declinesToGuess() {
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity first = persistAppointment(table, booking, treatment, therapist, receptionist, todayAt(10, 0), 60);
        SpaAppointmentEntity second = persistAppointment(table, booking, treatment, therapist, receptionist, todayAt(15, 0), 60);

        Order order = orderService.create(new OrderCreateInput().bookingId(booking.getId()), receptionist.getId());
        billTreatment(order.getId(), treatment);

        assertThat(order.getId()).isNotNull();
        assertThat(orderIdOf(first)).isNull();
        assertThat(orderIdOf(second)).isNull();
    }

    /**
     * Booking first, table second: the order names both a table (with a live appointment for an
     * unrelated booking) and a bookingId (with its own, unambiguous appointment elsewhere) - the
     * booking's own appointment wins, not whatever the table happens to be running.
     */
    @Test
    void addItems_withBothBookingAndTable_prefersTheBookingsOwnAppointment() {
        LocalDateTime now = LocalDateTime.now();
        TableEntity tableAtCounter = createSpaTable();
        TableEntity tableElsewhere = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        Booking unrelatedBooking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity liveAtCounterTable = persistAppointment(tableAtCounter, unrelatedBooking, treatment, therapist, receptionist, now.minusMinutes(10), 60);

        Booking guestBooking = createBooking(LocalDate.now().plusDays(301));
        // The booking axis is date-only, not time-of-day sensitive - see todayAt's own comment.
        SpaAppointmentEntity guestsOwnAppointment = persistAppointment(tableElsewhere, guestBooking, treatment, therapist, receptionist, todayAt(16, 0), 60);

        Order order = orderService.create(new OrderCreateInput().tableId(tableAtCounter.getId()).bookingId(guestBooking.getId()), receptionist.getId());
        billTreatment(order.getId(), treatment);

        assertThat(orderIdOf(guestsOwnAppointment)).isEqualTo(order.getId());
        assertThat(orderIdOf(liveAtCounterTable)).isNull();
    }

    /** The zone gate applies to the booking axis too - a restaurant order must never guess its way onto a spa appointment. */
    @Test
    void addItems_nonSpaTableWithBookingId_neverAttemptsToLink() {
        TableEntity restaurantTable = createRestaurantTable();
        TableEntity spaTable = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity appointment = persistAppointment(spaTable, booking, treatment, therapist, receptionist, todayAt(11, 0), 60);

        Order order = orderService.create(new OrderCreateInput().tableId(restaurantTable.getId()).bookingId(booking.getId()), receptionist.getId());
        // Even billing a treatment-department item doesn't matter here - the zone gate on the order's own table rules it out first.
        billTreatment(order.getId(), treatment);

        assertThat(order.getId()).isNotNull();
        assertThat(orderIdOf(appointment)).isNull();
    }

    /**
     * The residual hole this correction closes: a table-less order naming the guest's booking,
     * with nothing on it that actually bills a treatment - either no items at all, or items that
     * aren't SPA-department - must never link, no matter how unambiguous the booking axis would
     * otherwise be. Before this fix, resolution ran at order-open time keyed only on bookingId,
     * so this exact order would have linked and the appointment would read as billed against an
     * order that never charged for it - quieter, and worse, than the over-eager version replaced
     * two corrections ago.
     */
    @Test
    void noItemsAtAll_bookingOrderWithUnambiguousAppointment_neverLinks() {
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity appointment = persistAppointment(createSpaTable(), booking, treatment, therapist, receptionist, todayAt(14, 0), 60);

        // No tableId at all - a room-charge-style tab naming only the booking.
        Order order = orderService.create(new OrderCreateInput().bookingId(booking.getId()), receptionist.getId());
        // Never billed anything - addItems is never called at all.

        assertThat(order.getId()).isNotNull();
        assertThat(orderIdOf(appointment)).isNull();
    }

    @Test
    void addItems_bookingOrderWithNoTreatmentItem_neverLinks() {
        MenuItemEntity treatment = createTreatment();
        MenuItemEntity kitchenItem = createKitchenItem();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity appointment = persistAppointment(createSpaTable(), booking, treatment, therapist, receptionist, todayAt(14, 0), 60);

        // No tableId, and the one thing rung up isn't a treatment - a minibar item charged to the room, say.
        Order order = orderService.create(new OrderCreateInput().bookingId(booking.getId()), receptionist.getId());
        billTreatment(order.getId(), kitchenItem);

        assertThat(orderIdOf(appointment)).isNull();
    }

    @Test
    void create_withExplicitSpaAppointmentId_linksRegardlessOfTable() {
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity appointment = persistAppointment(table, booking, treatment, therapist, receptionist, todayAt(18, 0), 60);

        // No tableId/bookingId sent at all - auto-resolution has nothing to work with either way; proves the explicit id doesn't depend on it.
        Order order = orderService.create(new OrderCreateInput().spaAppointmentId(appointment.getId()), receptionist.getId());

        assertThat(orderIdOf(appointment)).isEqualTo(order.getId());
    }

    @Test
    void create_withUnknownExplicitSpaAppointmentId_isSilentlyIgnored() {
        Order order = orderService.create(new OrderCreateInput().spaAppointmentId("does-not-exist"), createReceptionist().getId());

        assertThat(order.getId()).isNotNull();
    }
}
