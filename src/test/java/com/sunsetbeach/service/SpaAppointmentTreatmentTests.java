package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentStatusUpdateInput;
import com.sunsetbeach.model.SpaAppointmentTreatment;
import com.sunsetbeach.model.SpaAppointmentTreatmentCreateInput;
import com.sunsetbeach.model.SpaSchedule;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.SpaAppointmentTreatmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DB-backed against the real dev Postgres (Testcontainers), same convention as
 * {@link SpaAppointmentServiceTests} (disposable rows tracked and deleted in {@link #cleanUp()},
 * not {@code @Transactional} - {@code AuditLogService.record} commits in its own transaction
 * regardless, see CLAUDE.md's Tests section, and this class doesn't chase those rows down either,
 * matching {@code SpaAppointmentServiceTests}'s own established precedent).
 *
 * <p>Covers the multi-treatment child-row model: adding/removing a treatment on an already-booked
 * appointment (the maintained {@code durationMinutes} sum, the BOOKED/COMPLETED lifecycle gate,
 * the minimum-one-treatment floor, and the exclusion-constraint collision a grow can hit), and the
 * completeness warning ({@code missingTreatmentNames}) including the CANCELLED-order case - see
 * CLAUDE.md's Spa billing section for why a cancelled order counts as carrying nothing. Money is
 * deliberately not under test here beyond {@code currentPrice} being a live menu read - see that
 * same CLAUDE.md section for why no price is ever frozen on a treatment row.
 */
@SpringBootTest
class SpaAppointmentTreatmentTests extends AbstractIntegrationTest {

    @Autowired
    private SpaAppointmentService spaAppointmentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

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
    private SpaAppointmentTreatmentRepository spaAppointmentTreatmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdRoomIds = new ArrayList<>();
    private final List<String> createdTableIds = new ArrayList<>();
    private final List<String> createdMenuItemIds = new ArrayList<>();
    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdOrderIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        // Spa appointments before orders - SpaAppointment.orderId FK-references Order, so an
        // appointment still linked to one of these orders would otherwise block its delete below.
        spaAppointmentRepository.findAll().stream()
                .filter(a -> createdTableIds.contains(a.getTableId()))
                .forEach(a -> {
                    spaAppointmentTreatmentRepository.deleteAll(spaAppointmentTreatmentRepository.findBySpaAppointmentId(a.getId()));
                    spaAppointmentRepository.deleteById(a.getId());
                });
        for (String orderId : createdOrderIds) {
            orderItemRepository.deleteAll(orderItemRepository.findByOrderId(orderId));
            orderRepository.deleteById(orderId);
        }
        createdTableIds.forEach(tableRepository::deleteById);
        createdMenuItemIds.forEach(menuItemRepository::deleteById);
        createdUserIds.forEach(userRepository::deleteById);
        bookingRepository.deleteAll(bookingRepository.findAll().stream().filter(b -> createdRoomIds.contains(b.getRoomId())).toList());
        for (String roomId : createdRoomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
    }

    private Booking createBooking(LocalDate checkIn) {
        RoomEntity room = new RoomEntity();
        room.setName("Treatment Test Room " + UUID.randomUUID());
        room.setDescription("Used only by SpaAppointmentTreatmentTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());

        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Treatment Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);

        return bookingService.createStaffBooking(
                new StaffBookingCreateInput(savedRoom.getId(), "Treatment Test Guest", checkIn.toString(), checkIn.plusDays(2).toString())
                        .roomUnitId(savedUnit.getId()));
    }

    private TableEntity createSpaTable() {
        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Treatment Test Table " + UUID.randomUUID());
        table.setCapacity(1);
        table.setActive(true);
        TableEntity saved = tableRepository.saveAndFlush(table);
        createdTableIds.add(saved.getId());
        return saved;
    }

    private MenuItemEntity createTreatment(int durationMinutes, String price) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Treatment " + UUID.randomUUID());
        item.setDescription("Used only by SpaAppointmentTreatmentTests");
        item.setCategory("Massage");
        item.setDepartment(MenuDepartment.SPA);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(true);
        item.setDurationMinutes(durationMinutes);
        MenuItemEntity saved = menuItemRepository.saveAndFlush(item);
        createdMenuItemIds.add(saved.getId());
        return saved;
    }

    private UserEntity createTherapist() {
        UserEntity user = new UserEntity();
        user.setEmail("treatment-therapist-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.WAITER);
        user.setActive(true);
        user.setJobFunctions(new String[] {JobFunction.THERAPIST.getValue()});
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private UserEntity createReceptionist() {
        UserEntity user = new UserEntity();
        user.setEmail("treatment-reception-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.CASHIER);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    /**
     * Links explicitly via {@code spaAppointmentId} (same mechanism the real billing door uses,
     * see {@code lib/spaOrderClient.ts}'s own comment) rather than the table/time-window auto-link
     * axes - those are keyed off the real clock ({@code OrderSpaAppointmentLinkTests} covers that
     * timing logic on its own), and these appointments live hundreds of days in the future so they
     * would never resolve by time regardless of what "now" happens to be when the suite runs.
     */
    private Order openOrderLinkedTo(String spaAppointmentId, String receptionistId) {
        Order order = orderService.create(new OrderCreateInput().spaAppointmentId(spaAppointmentId), receptionistId);
        createdOrderIds.add(order.getId());
        return order;
    }

    private void bill(String orderId, MenuItemEntity item, int quantity) {
        orderService.addItems(orderId, List.of(new OrderItemInput(item.getId(), quantity)));
    }

    // --- create() produces exactly one treatment row ------------------------------------------

    @Test
    void create_producesOneTreatmentRowWithLiveCurrentPrice() {
        LocalDate checkIn = LocalDate.now().plusDays(360);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, "1500.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();

        assertThat(created.getTreatments()).hasSize(1);
        SpaAppointmentTreatment row = created.getTreatments().get(0);
        assertThat(row.getTreatmentMenuItemId()).isEqualTo(treatment.getId());
        assertThat(row.getTreatmentName()).isEqualTo(treatment.getName());
        assertThat(row.getDurationMinutes()).isEqualTo(60);
        assertThat(row.getCurrentPrice()).isEqualTo("1500.00");
        assertThat(created.getMissingTreatmentNames()).isEmpty();
    }

    // --- addTreatment ---------------------------------------------------------------------------

    @Test
    void addTreatment_onBookedAppointment_growsDurationAndAddsRow() {
        LocalDate checkIn = LocalDate.now().plusDays(361);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity first = createTreatment(60, "1500.00");
        MenuItemEntity second = createTreatment(30, "800.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), first.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();

        SpaAppointment updated = spaAppointmentService.addTreatment(created.getId(), new SpaAppointmentTreatmentCreateInput(second.getId()), receptionist.getId());

        assertThat(updated.getDurationMinutes()).isEqualTo(90);
        assertThat(updated.getTreatments()).hasSize(2);
        assertThat(updated.getTreatments().stream().map(SpaAppointmentTreatment::getTreatmentMenuItemId)).containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void addTreatment_sameTreatmentTwice_isTwoRowsNotAQuantity() {
        LocalDate checkIn = LocalDate.now().plusDays(362);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(30, "800.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();

        SpaAppointment updated = spaAppointmentService.addTreatment(created.getId(), new SpaAppointmentTreatmentCreateInput(treatment.getId()), receptionist.getId());

        assertThat(updated.getTreatments()).hasSize(2);
        assertThat(updated.getTreatments()).allMatch(t -> t.getTreatmentMenuItemId().equals(treatment.getId()));
        assertThat(updated.getDurationMinutes()).isEqualTo(60);
    }

    @Test
    void addTreatment_onCompletedAppointment_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(363);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity first = createTreatment(60, "1500.00");
        MenuItemEntity second = createTreatment(30, "800.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), first.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();
        spaAppointmentService.updateStatus(created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        assertThatThrownBy(() -> spaAppointmentService.addTreatment(created.getId(), new SpaAppointmentTreatmentCreateInput(second.getId()), receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addTreatment_intoATableConflict_isRejectedAndDoesNotGrowDuration() {
        LocalDate checkIn = LocalDate.now().plusDays(364);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity first = createTreatment(30, "800.00");
        MenuItemEntity second = createTreatment(30, "800.00");
        UserEntity therapistA = createTherapist();
        UserEntity therapistB = createTherapist();
        UserEntity receptionist = createReceptionist();
        // Occupies the same table 10:30-11:00, which a 30->60 minute grow (10:00-11:00) would overlap.
        spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapistB.getId(), first.getId(), checkIn.toString(), "10:30"), receptionist.getId());
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapistA.getId(), first.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();

        assertThatThrownBy(() -> spaAppointmentService.addTreatment(created.getId(), new SpaAppointmentTreatmentCreateInput(second.getId()), receptionist.getId()))
                .isInstanceOf(ConflictException.class);

        // Rejected write must not have grown the appointment or added a row.
        SpaAppointment reloaded = spaAppointmentService.getSchedule(checkIn).getAppointments().stream()
                .filter(a -> a.getId().equals(created.getId())).findFirst().orElseThrow();
        assertThat(reloaded.getDurationMinutes()).isEqualTo(30);
        assertThat(reloaded.getTreatments()).hasSize(1);
    }

    @Test
    void addTreatment_wouldRunPastClosing_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(365);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity first = createTreatment(30, "800.00");
        // Default app.spa.closing-time is 20:00 - starting 19:30, a further 60 minutes would run past it.
        MenuItemEntity second = createTreatment(60, "1500.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), first.getId(), checkIn.toString(), "19:30"), receptionist.getId())
                .getAppointment();

        assertThatThrownBy(() -> spaAppointmentService.addTreatment(created.getId(), new SpaAppointmentTreatmentCreateInput(second.getId()), receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    // --- removeTreatment --------------------------------------------------------------------------

    @Test
    void removeTreatment_shrinksDurationAndNeverConflicts() {
        LocalDate checkIn = LocalDate.now().plusDays(366);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity first = createTreatment(60, "1500.00");
        MenuItemEntity second = createTreatment(30, "800.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), first.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();
        SpaAppointment grown = spaAppointmentService.addTreatment(created.getId(), new SpaAppointmentTreatmentCreateInput(second.getId()), receptionist.getId());
        String secondRowId = grown.getTreatments().stream().filter(t -> t.getTreatmentMenuItemId().equals(second.getId())).findFirst().orElseThrow().getId();

        SpaAppointment shrunk = spaAppointmentService.removeTreatment(created.getId(), secondRowId, receptionist.getId());

        assertThat(shrunk.getDurationMinutes()).isEqualTo(60);
        assertThat(shrunk.getTreatments()).hasSize(1);
    }

    @Test
    void removeTreatment_lastRemainingTreatment_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(367);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, "1500.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();
        String onlyRowId = created.getTreatments().get(0).getId();

        assertThatThrownBy(() -> spaAppointmentService.removeTreatment(created.getId(), onlyRowId, receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void removeTreatment_onCompletedAppointment_isAllowedToCorrectAnOverCount() {
        LocalDate checkIn = LocalDate.now().plusDays(368);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity first = createTreatment(60, "1500.00");
        MenuItemEntity second = createTreatment(30, "800.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), first.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();
        SpaAppointment grown = spaAppointmentService.addTreatment(created.getId(), new SpaAppointmentTreatmentCreateInput(second.getId()), receptionist.getId());
        spaAppointmentService.updateStatus(created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());
        String secondRowId = grown.getTreatments().stream().filter(t -> t.getTreatmentMenuItemId().equals(second.getId())).findFirst().orElseThrow().getId();

        SpaAppointment shrunk = spaAppointmentService.removeTreatment(created.getId(), secondRowId, receptionist.getId());

        assertThat(shrunk.getStatus()).isEqualTo(SpaAppointmentStatus.COMPLETED);
        assertThat(shrunk.getTreatments()).hasSize(1);
    }

    @Test
    void removeTreatment_onCancelledAppointment_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(369);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, "1500.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();
        spaAppointmentService.updateStatus(created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.CANCELLED), receptionist.getId());
        String onlyRowId = created.getTreatments().get(0).getId();

        assertThatThrownBy(() -> spaAppointmentService.removeTreatment(created.getId(), onlyRowId, receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void removeTreatment_unknownTreatmentId_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(370);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, "1500.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();

        assertThatThrownBy(() -> spaAppointmentService.removeTreatment(created.getId(), "does-not-exist", receptionist.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    // --- missingTreatmentNames (the completeness warning) ---------------------------------------

    @Test
    void missingTreatmentNames_bookedAppointment_isEmptyEvenWithNoOrder() {
        LocalDate checkIn = LocalDate.now().plusDays(371);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, "1500.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();

        assertThat(created.getMissingTreatmentNames()).isEmpty();
    }

    @Test
    void missingTreatmentNames_completedWithNoOrder_listsTheTreatment() {
        LocalDate checkIn = LocalDate.now().plusDays(372);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, "1500.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();

        SpaAppointment completed = spaAppointmentService.updateStatus(created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        assertThat(completed.getMissingTreatmentNames()).containsExactly(treatment.getName());
    }

    @Test
    void missingTreatmentNames_completedWithFullyBilledOrder_isEmpty() {
        LocalDate checkIn = LocalDate.now().plusDays(373);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity first = createTreatment(60, "1500.00");
        MenuItemEntity second = createTreatment(30, "800.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), first.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();
        spaAppointmentService.addTreatment(created.getId(), new SpaAppointmentTreatmentCreateInput(second.getId()), receptionist.getId());
        spaAppointmentService.updateStatus(created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        Order order = openOrderLinkedTo(created.getId(), receptionist.getId());
        bill(order.getId(), first, 1);
        bill(order.getId(), second, 1);

        SpaAppointment reloaded = spaAppointmentService.getSchedule(checkIn).getAppointments().stream()
                .filter(a -> a.getId().equals(created.getId())).findFirst().orElseThrow();
        assertThat(reloaded.getOrderId().get()).isEqualTo(order.getId());
        assertThat(reloaded.getMissingTreatmentNames()).isEmpty();
    }

    @Test
    void missingTreatmentNames_completedWithPartiallyBilledOrder_listsOnlyTheMissingOne() {
        LocalDate checkIn = LocalDate.now().plusDays(374);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity first = createTreatment(60, "1500.00");
        MenuItemEntity second = createTreatment(30, "800.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), first.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();
        spaAppointmentService.addTreatment(created.getId(), new SpaAppointmentTreatmentCreateInput(second.getId()), receptionist.getId());
        spaAppointmentService.updateStatus(created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        Order order = openOrderLinkedTo(created.getId(), receptionist.getId());
        bill(order.getId(), first, 1); // only the first treatment is rung up

        SpaAppointment reloaded = spaAppointmentService.getSchedule(checkIn).getAppointments().stream()
                .filter(a -> a.getId().equals(created.getId())).findFirst().orElseThrow();
        assertThat(reloaded.getMissingTreatmentNames()).containsExactly(second.getName());
    }

    /**
     * ADDITION 2: a cancelled order keeps its items and keeps its link (SpaAppointment.orderId is
     * presence-based, set once, never cleared by a cancel), so without this the appointment would
     * read as fully billed despite the order never actually charging anything. A cancelled order
     * must count as carrying none of this appointment's treatments - same as no order at all.
     */
    @Test
    void missingTreatmentNames_completedWithCancelledOrder_listsEveryTreatmentDespiteTheItemsStillBeingOnIt() {
        LocalDate checkIn = LocalDate.now().plusDays(375);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, "1500.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();
        spaAppointmentService.updateStatus(created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        Order order = openOrderLinkedTo(created.getId(), receptionist.getId());
        bill(order.getId(), treatment, 1);
        orderService.cancel(order.getId());

        SpaAppointment reloaded = spaAppointmentService.getSchedule(checkIn).getAppointments().stream()
                .filter(a -> a.getId().equals(created.getId())).findFirst().orElseThrow();
        // The link itself is untouched by the cancel - orderId is presence-based and never cleared.
        assertThat(reloaded.getOrderId().get()).isEqualTo(order.getId());
        assertThat(reloaded.getMissingTreatmentNames()).containsExactly(treatment.getName());
    }

    /**
     * The batched day-grid path (getSchedule) and the single-appointment path (toDto, reached via
     * addTreatment here) share one completeness computation - this guards against the two ever
     * silently diverging, the same duplicated-logic risk CLAUDE.md's Spa billing section calls out.
     */
    @Test
    void getSchedule_andSingleAppointmentReads_agreeOnMissingTreatmentNames() {
        LocalDate checkIn = LocalDate.now().plusDays(376);
        Booking booking = createBooking(checkIn);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, "1500.00");
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"), receptionist.getId())
                .getAppointment();
        SpaAppointment completed = spaAppointmentService.updateStatus(created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.COMPLETED), receptionist.getId());

        SpaSchedule schedule = spaAppointmentService.getSchedule(checkIn);
        SpaAppointment fromSchedule = schedule.getAppointments().stream().filter(a -> a.getId().equals(created.getId())).findFirst().orElseThrow();

        assertThat(fromSchedule.getMissingTreatmentNames()).isEqualTo(completed.getMissingTreatmentNames());
        assertThat(fromSchedule.getTreatments()).hasSize(completed.getTreatments().size());
    }
}
