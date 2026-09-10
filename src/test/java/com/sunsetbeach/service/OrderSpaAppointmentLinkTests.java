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
 * DB-backed (real dev Postgres, rolled back after each test): the *table* axis of how {@code
 * SpaAppointment.orderId} gets set - {@link OrderService#autoLinkSpaAppointmentByTable}, run from
 * {@link OrderService#addItems}. The *booking* axis ({@link
 * OrderService#autoLinkSpaAppointmentByBooking}, run from {@link OrderService#close}) has its own
 * test class, {@code OrderCloseSpaAppointmentLinkTests} - a `ROOM_CHARGE` close needs a real
 * `Shift` and writes audit-log entries in their own committing transaction (see that class's own
 * javadoc for why it isn't `@Transactional`), machinery this table-axis-only file has no reason
 * to carry.
 *
 * <p>The two axes used to be one method, tried at order-open time keyed on whichever of table/
 * booking the order happened to name. It was split because the two facts each axis needs never
 * exist at the same call site: the table is known from order creation, but the SPA-content gate
 * needs items, which don't exist until {@code addItems}; the booking, meanwhile, is often not
 * known until a `ROOM_CHARGE` close names one - {@code Order.bookingId} (settable at creation) is
 * never populated by any real caller, so resolving on it before this split was dead code that
 * happened to never fire. See {@code OrderService#autoLinkSpaAppointmentByTable}'s own javadoc.
 *
 * <p>Every test here therefore creates the order, then calls {@code addItems} with the treatment
 * line before asserting on the link - a test that only calls {@code create} would be testing
 * behavior that no longer exists.
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

    /** The zone gate applies on the table axis regardless of what's actually billed - a restaurant order must never guess its way onto a spa appointment. */
    @Test
    void addItems_treatmentOnNonSpaTable_neverAttemptsToLink() {
        TableEntity restaurantTable = createRestaurantTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity appointment = persistAppointment(createSpaTable(), booking, treatment, therapist, receptionist, todayAt(11, 0), 60);

        Order order = orderService.create(new OrderCreateInput().tableId(restaurantTable.getId()), receptionist.getId());
        // Even billing a treatment-department item doesn't matter here - the zone gate on the order's own table rules it out first.
        billTreatment(order.getId(), treatment);

        assertThat(order.getId()).isNotNull();
        assertThat(orderIdOf(appointment)).isNull();
    }

    /**
     * A residual hole an earlier correction closed: a table-less order naming the guest's
     * booking, with nothing on it that actually bills a treatment - either no items at all, or
     * items that aren't SPA-department - must never link, no matter how unambiguous the booking
     * axis would otherwise be. This is now doubly true: {@code addItems}' table axis was never
     * going to touch a table-less order regardless (there's no table to resolve by), and the
     * booking axis doesn't run here at all any more - see {@code OrderCloseSpaAppointmentLinkTests}
     * for the same guarantee at the one call site ({@code close}) that axis actually runs from.
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

    /**
     * The table axis has nothing to resolve by on a table-less order, regardless of what's rung
     * in - even a genuine treatment item never links it through {@code addItems} alone. This
     * order would eventually link at close (see {@code OrderCloseSpaAppointmentLinkTests}'
     * "must link" scenario for the exact same setup carried through to {@code close}); this test
     * is scoped to proving {@code addItems} by itself is not that mechanism.
     */
    @Test
    void addItems_tableLessOrder_neverLinksThroughAddItemsAlone() {
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        Booking booking = createBooking(LocalDate.now().plusDays(300));
        SpaAppointmentEntity appointment = persistAppointment(createSpaTable(), booking, treatment, therapist, receptionist, todayAt(14, 0), 60);

        // No tableId at all.
        Order order = orderService.create(new OrderCreateInput().bookingId(booking.getId()), receptionist.getId());
        billTreatment(order.getId(), treatment);

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
