package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentScheduleInput;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentStatusUpdateInput;
import com.sunsetbeach.model.SpaAppointmentTreatmentCreateInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import com.sunsetbeach.entity.BookingSource;
import jakarta.persistence.EntityManager;
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
 * The spa rules the server enforces itself (owner's decisions after the October 2026 QA report):
 * who can be booked (an active {@code THERAPIST}, a {@code SPA}-zone table, a guest the spa can
 * actually serve on that date), what cancelling a booking does to its spa, the top-up of a partly
 * paid order, and that a paid treatment can't be removed. Rolled back after each test; dates are
 * fixed (a stay in 2034), never read off the wall clock.
 */
@SpringBootTest
@Transactional
class SpaServerRulesTests extends AbstractIntegrationTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2034, 3, 10);
    private static final LocalDate CHECK_OUT = LocalDate.of(2034, 3, 13);
    private static final LocalDate IN_STAY = LocalDate.of(2034, 3, 11);

    @Autowired private SpaAppointmentService spaAppointmentService;
    @Autowired private BookingService bookingService;
    @Autowired private OrderService orderService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private TableRepository tableRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SpaAppointmentRepository spaAppointmentRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private BookingExpiryService bookingExpiryService;
    @Autowired private EntityManager entityManager;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private RoomEntity room;
    private Booking booking;
    private TableEntity spaTable;
    private UserEntity therapist;
    private UserEntity receptionist;
    private MenuItemEntity massage;
    private MenuItemEntity facial;
    private MenuItemEntity smoothie;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Spa Rules Room " + UUID.randomUUID());
        newRoom.setDescription("Used only by SpaServerRulesTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);
        booking = createBooking("Spa Rules Guest");
        spaTable = table(Zone.SPA);
        therapist = user(Role.WAITER, true, JobFunction.THERAPIST.getValue());
        receptionist = user(Role.CASHIER, true);
        massage = menuItem("Massage", MenuDepartment.SPA, 60, "1500.00");
        facial = menuItem("Facial", MenuDepartment.SPA, 60, "2000.00");
        smoothie = menuItem("Smoothie", MenuDepartment.BAR, null, "120.00");
    }

    // --- Fixtures -----------------------------------------------------------------------------------

    private Booking createBooking(String guestName) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Spa Rules Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);
        return bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), guestName, CHECK_IN.toString(), CHECK_OUT.toString(), BookingChannel.WALK_IN, 1)
                        .roomUnitId(savedUnit.getId()));
    }

    private TableEntity table(Zone zone) {
        TableEntity table = new TableEntity();
        table.setZone(zone);
        table.setLabel("Spa Rules Table " + UUID.randomUUID());
        table.setCapacity(2);
        table.setActive(true);
        return tableRepository.saveAndFlush(table);
    }

    private UserEntity user(Role role, boolean active, String... jobFunctions) {
        UserEntity user = new UserEntity();
        user.setEmail("spa-rules-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("unused");
        user.setRole(role);
        user.setActive(active);
        user.setJobFunctions(jobFunctions);
        return userRepository.saveAndFlush(user);
    }

    private MenuItemEntity menuItem(String name, MenuDepartment department, Integer durationMinutes, String price) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName(name + " " + UUID.randomUUID());
        item.setDescription("Used only by SpaServerRulesTests");
        item.setCategory(name);
        item.setDepartment(department);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(true);
        item.setDurationMinutes(durationMinutes);
        return menuItemRepository.saveAndFlush(item);
    }

    private BookingEntity bookingEntity(Booking b) {
        return bookingRepository.findById(b.getId()).orElseThrow();
    }

    private void setOccupancy(Booking b, OccupancyStatus occupancyStatus) {
        BookingEntity entity = bookingEntity(b);
        entity.setOccupancyStatus(occupancyStatus);
        bookingRepository.saveAndFlush(entity);
    }

    private SpaAppointmentCreateInput input(Booking b, TableEntity table, UserEntity who, LocalDate date, String startTime) {
        return new SpaAppointmentCreateInput(b.getId(), table.getId(), who.getId(), massage.getId(), date.toString(), startTime);
    }

    private SpaAppointment book(String startTime) {
        return spaAppointmentService.create(input(booking, spaTable, therapist, IN_STAY, startTime), receptionist.getId()).getAppointment();
    }

    /** Opens an order linked to the appointment (the billing door's explicit link) with the given lines. */
    private OrderEntity orderFor(SpaAppointment appointment, OrderItemInput... lines) {
        Order created = orderService.create(
                new OrderCreateInput().bookingId(appointment.getBookingId()).spaAppointmentId(appointment.getId()).items(List.of(lines)), receptionist.getId());
        return orderRepository.findById(created.getId()).orElseThrow();
    }

    private void markPaid(OrderEntity order) {
        order.setStatus(OrderStatus.PAID);
        orderRepository.saveAndFlush(order);
    }

    private SpaAppointmentEntity reload(SpaAppointment appointment) {
        return spaAppointmentRepository.findById(appointment.getId()).orElseThrow();
    }

    private void cancel(Booking b) {
        bookingService.updateStatus(b.getId(), new BookingStatusInput(BookingStatus.CANCELLED));
    }

    // --- 1. Who can be booked -----------------------------------------------------------------------

    @Test
    void create_withAnAccountThatIsNotATherapist_is400() {
        UserEntity notATherapist = user(Role.WAITER, true);

        assertThatThrownBy(() -> spaAppointmentService.create(input(booking, spaTable, notATherapist, IN_STAY, "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a therapist");
    }

    @Test
    void create_withADeactivatedTherapist_is400() {
        UserEntity gone = user(Role.WAITER, false, JobFunction.THERAPIST.getValue());

        assertThatThrownBy(() -> spaAppointmentService.create(input(booking, spaTable, gone, IN_STAY, "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void create_onARestaurantTable_is400() {
        TableEntity restaurantTable = table(Zone.RESTAURANT);

        assertThatThrownBy(() -> spaAppointmentService.create(input(booking, restaurantTable, therapist, IN_STAY, "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a spa table");
    }

    @Test
    void create_forAGuestWhoHasCheckedOut_is409() {
        setOccupancy(booking, OccupancyStatus.CHECKED_OUT);

        assertThatThrownBy(() -> spaAppointmentService.create(input(booking, spaTable, therapist, IN_STAY, "10:00"), receptionist.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("checked out");
    }

    @Test
    void create_forACancelledBooking_is409() {
        cancel(booking);

        assertThatThrownBy(() -> spaAppointmentService.create(input(booking, spaTable, therapist, IN_STAY, "10:00"), receptionist.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void create_forAGuestWhoHasNotArrived_onADateOutsideTheStay_is400_onBothSides() {
        assertThatThrownBy(() -> spaAppointmentService.create(input(booking, spaTable, therapist, CHECK_IN.minusDays(1), "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("doesn't include");
        assertThatThrownBy(() -> spaAppointmentService.create(input(booking, spaTable, therapist, CHECK_OUT.plusDays(1), "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("doesn't include");
    }

    @Test
    void create_forAGuestWhoHasNotArrived_onAnyDayOfTheStayIncludingBothEnds_works() {
        assertThat(spaAppointmentService.create(input(booking, spaTable, therapist, CHECK_IN, "10:00"), receptionist.getId()).getAppointment().getStatus())
                .isEqualTo(SpaAppointmentStatus.BOOKED);
        assertThat(spaAppointmentService.create(input(booking, spaTable, therapist, CHECK_OUT, "10:00"), receptionist.getId()).getAppointment().getStatus())
                .isEqualTo(SpaAppointmentStatus.BOOKED);
    }

    @Test
    void create_forAPaidButNotYetArrivedBooking_withinTheStay_works() {
        BookingEntity entity = bookingEntity(booking);
        entity.setStatus(BookingStatus.PAID);
        bookingRepository.saveAndFlush(entity);

        assertThat(spaAppointmentService.create(input(booking, spaTable, therapist, IN_STAY, "10:00"), receptionist.getId()).getAppointment().getStatus())
                .isEqualTo(SpaAppointmentStatus.BOOKED);
    }

    @Test
    void create_forAGuestInTheHouse_outsideTheStay_is400_likeEveryoneElse() {
        setOccupancy(booking, OccupancyStatus.CHECKED_IN);

        assertThatThrownBy(() -> spaAppointmentService.create(input(booking, spaTable, therapist, CHECK_OUT.plusDays(1), "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("doesn't include");
        assertThatThrownBy(() -> spaAppointmentService.create(input(booking, spaTable, therapist, CHECK_IN.minusDays(1), "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
        assertThat(spaAppointmentService.create(input(booking, spaTable, therapist, CHECK_OUT, "10:00"), receptionist.getId()).getAppointment().getStatus())
                .isEqualTo(SpaAppointmentStatus.BOOKED);
    }

    // --- 1b. The same checks when an appointment is moved -------------------------------------------

    @Test
    void updateSchedule_toARestaurantTable_orANonTherapist_orADayOutsideTheStay_isRefused() {
        SpaAppointment appointment = book("10:00");

        assertThatThrownBy(() -> spaAppointmentService.updateSchedule(
                        appointment.getId(), new SpaAppointmentScheduleInput(table(Zone.BAR).getId(), therapist.getId(), IN_STAY.toString(), "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a spa table");
        assertThatThrownBy(() -> spaAppointmentService.updateSchedule(
                        appointment.getId(), new SpaAppointmentScheduleInput(spaTable.getId(), user(Role.WAITER, true).getId(), IN_STAY.toString(), "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a therapist");
        assertThatThrownBy(() -> spaAppointmentService.updateSchedule(
                        appointment.getId(), new SpaAppointmentScheduleInput(spaTable.getId(), therapist.getId(), CHECK_OUT.plusDays(2).toString(), "10:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("doesn't include");
    }

    @Test
    void updateSchedule_withinTheRules_stillWorks() {
        SpaAppointment appointment = book("10:00");

        SpaAppointment moved = spaAppointmentService.updateSchedule(
                appointment.getId(), new SpaAppointmentScheduleInput(spaTable.getId(), therapist.getId(), CHECK_OUT.toString(), "14:00"), receptionist.getId());

        assertThat(moved.getStartTime()).isEqualTo("14:00");
    }

    // --- 2. Cancelling a booking cleans up its spa --------------------------------------------------

    @Test
    void cancellingTheBooking_cancelsItsBookedAppointments_andTakesTheUnpaidSpaLineOffTheOrder_keepingOtherLines() {
        SpaAppointment appointment = book("10:00");
        OrderEntity order = orderFor(appointment, new OrderItemInput(massage.getId(), 1), new OrderItemInput(smoothie.getId(), 2));
        assertThat(order.getTotal()).isEqualByComparingTo("1740.00");

        cancel(booking);

        SpaAppointmentEntity after = reload(appointment);
        assertThat(after.getStatus()).isEqualTo(SpaAppointmentStatus.CANCELLED);
        assertThat(after.getCancelReason()).isEqualTo("Booking cancelled");
        List<OrderItemEntity> lines = orderItemRepository.findByOrderId(order.getId());
        assertThat(lines).extracting(OrderItemEntity::getMenuItemId).containsExactly(smoothie.getId());
        OrderEntity orderAfter = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(orderAfter.getTotal()).isEqualByComparingTo("240.00");
        assertThat(orderAfter.getStatus()).isEqualTo(OrderStatus.OPEN);
    }

    @Test
    void cancellingTheBooking_cancelsAnOrderThatWasOnlyTheTreatment() {
        SpaAppointment appointment = book("10:00");
        OrderEntity order = orderFor(appointment, new OrderItemInput(massage.getId(), 1));

        cancel(booking);

        assertThat(orderItemRepository.findByOrderId(order.getId())).isEmpty();
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(reload(appointment).getStatus()).isEqualTo(SpaAppointmentStatus.CANCELLED);
    }

    @Test
    void cancellingTheBooking_cancelsAnAppointmentThatHasNoOrder() {
        SpaAppointment appointment = book("10:00");

        Booking result = bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CANCELLED));

        assertThat(reload(appointment).getStatus()).isEqualTo(SpaAppointmentStatus.CANCELLED);
        assertThat(result.getWarning()).isNull();
    }

    @Test
    void cancellingTheBooking_withAPaidSpaOrder_touchesNothingOfIt_andWarnsOnTheBooking() {
        SpaAppointment paid = book("10:00");
        OrderEntity paidOrder = orderFor(paid, new OrderItemInput(massage.getId(), 1));
        markPaid(paidOrder);
        SpaAppointment unpaid = book("12:00");
        OrderEntity openOrder = orderFor(unpaid, new OrderItemInput(facial.getId(), 1));

        Booking result = bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CANCELLED));

        // The paid one: appointment still booked, order and its lines exactly as they were.
        assertThat(reload(paid).getStatus()).isEqualTo(SpaAppointmentStatus.BOOKED);
        assertThat(orderRepository.findById(paidOrder.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(orderItemRepository.findByOrderId(paidOrder.getId())).hasSize(1);
        assertThat(result.getWarning()).contains("already paid").contains("#" + paidOrder.getNumber());
        // The unpaid one is cleaned up as usual.
        assertThat(reload(unpaid).getStatus()).isEqualTo(SpaAppointmentStatus.CANCELLED);
        assertThat(orderRepository.findById(openOrder.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // The warning is read back from the booking until the paid appointment is dealt with.
        assertThat(bookingService.getById(booking.getId()).getWarning()).contains("already paid");
        spaAppointmentService.updateStatus(
                paid.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.CANCELLED), receptionist.getId());
        assertThat(bookingService.getById(booking.getId()).getWarning()).isNull();
    }

    @Test
    void cancellingTheBooking_leavesACompletedTreatmentAndItsOrderAlone() {
        SpaAppointment done = book("10:00");
        OrderEntity order = orderFor(done, new OrderItemInput(massage.getId(), 1));
        spaAppointmentService.updateStatus(done.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        Booking result = bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CANCELLED));

        assertThat(reload(done).getStatus()).isEqualTo(SpaAppointmentStatus.COMPLETED);
        assertThat(orderItemRepository.findByOrderId(order.getId())).hasSize(1);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(result.getWarning()).isNull();
    }

    @Test
    void cancellingOneBooking_doesNotTouchAnotherGuestsSpa() {
        Booking other = createBooking("Somebody Else");
        SpaAppointment mine = book("10:00");
        SpaAppointment theirs =
                spaAppointmentService.create(input(other, spaTable, therapist, IN_STAY, "12:00"), receptionist.getId()).getAppointment();

        cancel(booking);

        assertThat(reload(mine).getStatus()).isEqualTo(SpaAppointmentStatus.CANCELLED);
        assertThat(reload(theirs).getStatus()).isEqualTo(SpaAppointmentStatus.BOOKED);
    }

    /** The auto-cancellation of a stale unconfirmed public booking is a cancellation too: the guest is not coming, so the spa is freed. */
    @Test
    void theExpirySweep_cancellingAStaleUnconfirmedBooking_alsoFreesItsSpa() {
        SpaAppointment appointment = book("10:00");
        BookingEntity entity = bookingEntity(booking);
        entity.setSource(BookingSource.PUBLIC);
        bookingRepository.saveAndFlush(entity);
        // Created years ago: far past the expiry threshold whatever day this runs.
        entityManager
                .createNativeQuery("UPDATE \"Booking\" SET \"createdAt\" = ?1 WHERE id = ?2")
                .setParameter(1, LocalDateTime.of(2020, 1, 6, 10, 0))
                .setParameter(2, booking.getId())
                .executeUpdate();
        entityManager.clear();

        bookingExpiryService.sweepUnconfirmedPublicBookings();

        assertThat(bookingEntity(booking).getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(reload(appointment).getStatus()).isEqualTo(SpaAppointmentStatus.CANCELLED);
    }

    // --- 3. Partial payment, and a paid treatment can't be removed ---------------------------------------

    @Test
    void billMissing_addsExactlyWhatTheOrderLacks_toTheSameOrder() {
        SpaAppointment appointment = book("10:00");
        spaAppointmentService.addTreatment(appointment.getId(), new SpaAppointmentTreatmentCreateInput(facial.getId()), receptionist.getId());
        // The order carries only the massage; the facial was never rung up.
        OrderEntity order = orderFor(appointment, new OrderItemInput(massage.getId(), 1));
        spaAppointmentService.updateStatus(appointment.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        SpaAppointment topped = spaAppointmentService.billMissingTreatments(appointment.getId(), receptionist.getId());

        assertThat(topped.getOrderId().get()).isEqualTo(order.getId());
        assertThat(topped.getMissingTreatmentNames()).isEmpty();
        List<OrderItemEntity> lines = orderItemRepository.findByOrderId(order.getId());
        assertThat(lines).hasSize(2);
        assertThat(lines).allSatisfy(l -> assertThat(l.getQuantity()).isEqualTo(1));
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getTotal()).isEqualByComparingTo("3500.00");
    }

    @Test
    void billMissing_whenTheSameTreatmentWasBookedTwiceAndBilledOnce_addsOneMore() {
        SpaAppointment appointment = book("10:00");
        spaAppointmentService.addTreatment(appointment.getId(), new SpaAppointmentTreatmentCreateInput(massage.getId()), receptionist.getId());
        OrderEntity order = orderFor(appointment, new OrderItemInput(massage.getId(), 1));
        spaAppointmentService.updateStatus(appointment.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        spaAppointmentService.billMissingTreatments(appointment.getId(), receptionist.getId());

        assertThat(orderItemRepository.findByOrderId(order.getId())).singleElement().satisfies(l -> assertThat(l.getQuantity()).isEqualTo(2));
    }

    @Test
    void billMissing_isRefusedWhenTheAppointmentIsNotCompleted_thereIsNoOrder_orNothingIsMissing() {
        SpaAppointment appointment = book("10:00");
        spaAppointmentService.addTreatment(appointment.getId(), new SpaAppointmentTreatmentCreateInput(facial.getId()), receptionist.getId());
        // Not completed yet.
        assertThatThrownBy(() -> spaAppointmentService.billMissingTreatments(appointment.getId(), receptionist.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("completed");

        spaAppointmentService.updateStatus(appointment.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());
        // Completed, but no order at all.
        assertThatThrownBy(() -> spaAppointmentService.billMissingTreatments(appointment.getId(), receptionist.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no order");

        // An order that already carries everything has nothing to add.
        SpaAppointment complete = book("12:00");
        orderFor(complete, new OrderItemInput(massage.getId(), 1));
        spaAppointmentService.updateStatus(complete.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());
        assertThatThrownBy(() -> spaAppointmentService.billMissingTreatments(complete.getId(), receptionist.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already on its orders");
    }

    @Test
    void billMissing_whenTheOrderIsAlreadyPaid_opensASecondOrderForTheMissingTreatments_linkedToTheSameAppointment() {
        SpaAppointment appointment = book("10:00");
        spaAppointmentService.addTreatment(appointment.getId(), new SpaAppointmentTreatmentCreateInput(facial.getId()), receptionist.getId());
        OrderEntity paidOrder = orderFor(appointment, new OrderItemInput(massage.getId(), 1));
        markPaid(paidOrder);
        spaAppointmentService.updateStatus(appointment.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());
        // Only the massage is paid for, so the facial is flagged.
        assertThat(scheduledAppointment(appointment).getMissingTreatmentNames()).hasSize(1);

        SpaAppointment topped = spaAppointmentService.billMissingTreatments(appointment.getId(), receptionist.getId());

        // The paid order is untouched; the appointment still points at it as its own order.
        assertThat(orderItemRepository.findByOrderId(paidOrder.getId())).hasSize(1);
        assertThat(orderRepository.findById(paidOrder.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(topped.getOrderId().get()).isEqualTo(paidOrder.getId());
        // A second order exists, linked to the same appointment, carrying just the facial.
        List<String> linked = jdbc.queryForList("select \"orderId\" from \"SpaAppointmentOrder\" where \"spaAppointmentId\" = ?", String.class, appointment.getId());
        assertThat(linked).hasSize(1);
        OrderEntity second = orderRepository.findById(linked.get(0)).orElseThrow();
        assertThat(second.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(orderItemRepository.findByOrderId(second.getId())).singleElement().satisfies(l -> {
            assertThat(l.getMenuItemId()).isEqualTo(facial.getId());
            assertThat(l.getQuantity()).isEqualTo(1);
        });
        assertThat(second.getTotal()).isEqualByComparingTo("2000.00");
        // Completeness is read over both orders: nothing is missing now, in the single read and in the day grid.
        assertThat(topped.getMissingTreatmentNames()).isEmpty();
        assertThat(scheduledAppointment(appointment).getMissingTreatmentNames()).isEmpty();
        // The order DTO names the appointment too.
        assertThat(orderService.getById(second.getId()).getSpaAppointmentId().orElse(null)).isEqualTo(appointment.getId());
        // And a third press has nothing to add.
        assertThatThrownBy(() -> spaAppointmentService.billMissingTreatments(appointment.getId(), receptionist.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already on its orders");
    }

    @Test
    void billMissing_whileTheSecondOrderIsStillOpen_addsToThatOne_notToAThird() {
        SpaAppointment appointment = book("10:00");
        spaAppointmentService.addTreatment(appointment.getId(), new SpaAppointmentTreatmentCreateInput(facial.getId()), receptionist.getId());
        OrderEntity paidOrder = orderFor(appointment, new OrderItemInput(massage.getId(), 1));
        markPaid(paidOrder);
        spaAppointmentService.updateStatus(appointment.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());
        spaAppointmentService.billMissingTreatments(appointment.getId(), receptionist.getId());
        List<String> linked = jdbc.queryForList("select \"orderId\" from \"SpaAppointmentOrder\" where \"spaAppointmentId\" = ?", String.class, appointment.getId());
        // The facial line is taken off the still-open top-up order, so it is missing again.
        orderItemRepository.deleteAll(orderItemRepository.findByOrderId(linked.get(0)));
        orderItemRepository.flush();

        spaAppointmentService.billMissingTreatments(appointment.getId(), receptionist.getId());

        // The open top-up order takes it again - no third order is opened.
        assertThat(jdbc.queryForList("select \"orderId\" from \"SpaAppointmentOrder\" where \"spaAppointmentId\" = ?", String.class, appointment.getId()))
                .containsExactly(linked.get(0));
        assertThat(orderItemRepository.findByOrderId(linked.get(0))).singleElement().satisfies(l -> assertThat(l.getMenuItemId()).isEqualTo(facial.getId()));
    }

    @Test
    void theWarning_isNotRaisedForATreatmentBilledOnAnotherOfTheAppointmentsOrders() {
        SpaAppointment appointment = book("10:00");
        spaAppointmentService.addTreatment(appointment.getId(), new SpaAppointmentTreatmentCreateInput(facial.getId()), receptionist.getId());
        OrderEntity first = orderFor(appointment, new OrderItemInput(massage.getId(), 1));
        markPaid(first);
        // A second order for the same appointment, opened by hand and linked as the top-up is.
        Order other = orderService.create(new OrderCreateInput().tableId(spaTable.getId()), receptionist.getId());
        jdbc.update("insert into \"SpaAppointmentOrder\" (\"orderId\", \"spaAppointmentId\") values (?, ?)", other.getId(), appointment.getId());
        orderService.addItems(other.getId(), List.of(new OrderItemInput(facial.getId(), 1)));
        spaAppointmentService.updateStatus(appointment.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        assertThat(scheduledAppointment(appointment).getMissingTreatmentNames()).isEmpty();
        // Cancel the second order: its facial no longer counts, so it is missing again.
        orderService.cancel(other.getId(), new com.sunsetbeach.model.OrderCancelInput(), receptionist.getId(), Role.CASHIER);
        assertThat(scheduledAppointment(appointment).getMissingTreatmentNames()).hasSize(1);
    }

    @Test
    void cancellingTheBooking_withAPaidTopUpOrder_leavesTheAppointmentBooked_andWarns() {
        SpaAppointment appointment = book("10:00");
        orderFor(appointment, new OrderItemInput(massage.getId(), 1));
        Order topUp = orderService.create(new OrderCreateInput().tableId(spaTable.getId()), receptionist.getId());
        jdbc.update("insert into \"SpaAppointmentOrder\" (\"orderId\", \"spaAppointmentId\") values (?, ?)", topUp.getId(), appointment.getId());
        orderService.addItems(topUp.getId(), List.of(new OrderItemInput(facial.getId(), 1)));
        markPaid(orderRepository.findById(topUp.getId()).orElseThrow());

        Booking result = bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CANCELLED));

        assertThat(reload(appointment).getStatus()).isEqualTo(SpaAppointmentStatus.BOOKED);
        assertThat(result.getWarning()).contains("already paid");
    }

    private SpaAppointment scheduledAppointment(SpaAppointment appointment) {
        return spaAppointmentService.getSchedule(IN_STAY).getAppointments().stream()
                .filter(a -> a.getId().equals(appointment.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void removeTreatment_isRefusedOnceTheLinkedOrderIsPaid_andStillWorksBefore() {
        SpaAppointment appointment = book("10:00");
        SpaAppointment withTwo = spaAppointmentService.addTreatment(
                appointment.getId(), new SpaAppointmentTreatmentCreateInput(facial.getId()), receptionist.getId());
        OrderEntity order = orderFor(appointment, new OrderItemInput(massage.getId(), 1), new OrderItemInput(facial.getId(), 1));
        String facialRowId = withTwo.getTreatments().stream().filter(t -> t.getTreatmentMenuItemId().equals(facial.getId())).findFirst().orElseThrow().getId();
        String massageRowId = withTwo.getTreatments().stream().filter(t -> t.getTreatmentMenuItemId().equals(massage.getId())).findFirst().orElseThrow().getId();

        // Unpaid order: removing is fine.
        SpaAppointment afterRemove = spaAppointmentService.removeTreatment(appointment.getId(), facialRowId, receptionist.getId());
        assertThat(afterRemove.getTreatments()).hasSize(1);

        spaAppointmentService.addTreatment(appointment.getId(), new SpaAppointmentTreatmentCreateInput(facial.getId()), receptionist.getId());
        markPaid(order);

        assertThatThrownBy(() -> spaAppointmentService.removeTreatment(appointment.getId(), massageRowId, receptionist.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already paid");
        assertThat(spaAppointmentService.getSchedule(IN_STAY).getAppointments().stream().filter(a -> a.getId().equals(appointment.getId())).findFirst().orElseThrow()
                        .getTreatments())
                .hasSize(2);
    }
}
