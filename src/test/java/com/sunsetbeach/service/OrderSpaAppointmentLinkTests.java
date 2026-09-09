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
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): how {@code SpaAppointment.orderId}
 * actually gets set. Both `/pos` and `/admin/pos` open an order the same way - {@code POST
 * /orders} with nothing but a {@code tableId} (see {@code PosTableBoard}/{@code OrderBoard} on
 * the frontend) - so the auto-link in {@link OrderService#create} is what makes the ordinary
 * path set the link without either screen having to know spa appointments exist; explicit
 * {@code spaAppointmentId} stays as the override for when auto-resolution can't or shouldn't
 * guess.
 */
@SpringBootTest
@Transactional
class OrderSpaAppointmentLinkTests extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SpaAppointmentService spaAppointmentService;

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

    private SpaAppointmentEntity bookAppointment(TableEntity table, MenuItemEntity treatment, LocalDate date, String startTime) {
        Booking booking = createBooking(date);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        String appointmentId = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), date.toString(), startTime), receptionist.getId())
                .getAppointment()
                .getId();
        return spaAppointmentRepository.findById(appointmentId).orElseThrow();
    }

    @Test
    void create_forSpaTableWithOneCandidate_autoLinksTheAppointment() {
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        SpaAppointmentEntity appointment = bookAppointment(table, treatment, LocalDate.now(), "10:00");

        Order order = orderService.create(new OrderCreateInput().tableId(table.getId()), createReceptionist().getId());

        SpaAppointmentEntity reloaded = spaAppointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(reloaded.getOrderId()).isEqualTo(order.getId());
    }

    @Test
    void create_forSpaTableWithNoAppointmentToday_leavesOrderUnlinked() {
        TableEntity table = createSpaTable();

        Order order = orderService.create(new OrderCreateInput().tableId(table.getId()), createReceptionist().getId());

        // No exception, no crash - just nothing to link. Confirmed by the order existing at all.
        assertThat(order.getId()).isNotNull();
        assertThat(order.getTableId().get()).isEqualTo(table.getId());
    }

    @Test
    void create_forSpaTableWithTwoUnlinkedCandidates_declinesToGuess() {
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        LocalDate date = LocalDate.now();
        SpaAppointmentEntity first = bookAppointment(table, treatment, date, "09:00");
        SpaAppointmentEntity second = bookAppointment(table, treatment, date, "10:00");

        orderService.create(new OrderCreateInput().tableId(table.getId()), createReceptionist().getId());

        assertThat(spaAppointmentRepository.findById(first.getId()).orElseThrow().getOrderId()).isNull();
        assertThat(spaAppointmentRepository.findById(second.getId()).orElseThrow().getOrderId()).isNull();
    }

    @Test
    void create_forNonSpaTable_neverAttemptsToLink() {
        TableEntity restaurantTable = new TableEntity();
        restaurantTable.setZone(Zone.RESTAURANT);
        restaurantTable.setLabel("Order Link Restaurant Table " + UUID.randomUUID());
        restaurantTable.setCapacity(2);
        restaurantTable.setActive(true);
        TableEntity saved = tableRepository.saveAndFlush(restaurantTable);

        Order order = orderService.create(new OrderCreateInput().tableId(saved.getId()), createReceptionist().getId());

        assertThat(order.getId()).isNotNull();
    }

    @Test
    void create_withExplicitSpaAppointmentId_linksRegardlessOfTable() {
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        SpaAppointmentEntity appointment = bookAppointment(table, treatment, LocalDate.now(), "11:00");

        // Opened with no tableId at all (a walk-in ticket, say) but the explicit id still applies -
        // the override doesn't depend on the auto-resolution path being reachable.
        Order order = orderService.create(new OrderCreateInput().spaAppointmentId(appointment.getId()), createReceptionist().getId());

        SpaAppointmentEntity reloaded = spaAppointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(reloaded.getOrderId()).isEqualTo(order.getId());
    }

    @Test
    void create_withUnknownExplicitSpaAppointmentId_isSilentlyIgnored() {
        Order order = orderService.create(new OrderCreateInput().spaAppointmentId("does-not-exist"), createReceptionist().getId());

        assertThat(order.getId()).isNotNull();
    }
}
