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
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentResult;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentStatusUpdateInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
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
 * {@code BookingRelocationTests} - disposable rows tracked and deleted in {@link #cleanUp()}.
 * Covers the ground rules from the SPA module: department/duration gating on the treatment,
 * opening-hours/grid validation, and the CORRECTION-1 inclusive-both-ends stay window (the guest
 * is still in the hotel on the departure day, unlike room occupancy's own half-open convention).
 * The double-booking guard itself (the exclusion constraint) is covered separately, under real
 * concurrency, by {@link SpaAppointmentOverlapRaceTests} - a check-then-write test here would
 * prove nothing about the actual race.
 */
@SpringBootTest
class SpaAppointmentServiceTests extends AbstractIntegrationTest {

    @Autowired
    private SpaAppointmentService spaAppointmentService;

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
    private PasswordEncoder passwordEncoder;

    private final List<String> createdRoomIds = new ArrayList<>();
    private final List<String> createdTableIds = new ArrayList<>();
    private final List<String> createdMenuItemIds = new ArrayList<>();
    private final List<String> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        spaAppointmentRepository.deleteAll(spaAppointmentRepository.findAll().stream()
                .filter(a -> createdTableIds.contains(a.getTableId()))
                .toList());
        createdTableIds.forEach(tableRepository::deleteById);
        createdMenuItemIds.forEach(menuItemRepository::deleteById);
        createdUserIds.forEach(userRepository::deleteById);
        // Bookings before room units - BookingSegment.roomUnitId's FK would otherwise reject the
        // room unit delete below (deleting a Booking cascades to its own segments - see
        // V18__booking_segments.sql - but nothing cascades the other way).
        bookingRepository.deleteAll(bookingRepository.findAll().stream().filter(b -> createdRoomIds.contains(b.getRoomId())).toList());
        for (String roomId : createdRoomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
    }

    private Booking createBooking(LocalDate checkIn, LocalDate checkOut) {
        RoomEntity room = new RoomEntity();
        room.setName("Spa Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by SpaAppointmentServiceTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());

        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Spa Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);

        return bookingService.createStaffBooking(
                new StaffBookingCreateInput(savedRoom.getId(), "Spa Test Guest", checkIn.toString(), checkOut.toString())
                        .roomUnitId(savedUnit.getId()));
    }

    private TableEntity createSpaTable() {
        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Spa Table " + UUID.randomUUID());
        table.setCapacity(1);
        table.setActive(true);
        TableEntity saved = tableRepository.saveAndFlush(table);
        createdTableIds.add(saved.getId());
        return saved;
    }

    private MenuItemEntity createTreatment(Integer durationMinutes, MenuDepartment department) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Massage " + UUID.randomUUID());
        item.setDescription("Used only by SpaAppointmentServiceTests");
        item.setCategory("Massage");
        item.setDepartment(department);
        item.setPrice(new BigDecimal("1500.00"));
        item.setAvailable(true);
        item.setDurationMinutes(durationMinutes);
        MenuItemEntity saved = menuItemRepository.saveAndFlush(item);
        createdMenuItemIds.add(saved.getId());
        return saved;
    }

    private UserEntity createTherapist() {
        UserEntity user = new UserEntity();
        user.setEmail("spa-therapist-" + UUID.randomUUID() + "@example.com");
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
        user.setEmail("spa-reception-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.CASHIER);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    @Test
    void create_withinStay_noWarning() {
        LocalDate checkIn = LocalDate.now().plusDays(320);
        LocalDate checkOut = checkIn.plusDays(3);
        Booking booking = createBooking(checkIn, checkOut);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        SpaAppointmentResult result = spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.plusDays(1).toString(), "10:00"),
                receptionist.getId());

        assertThat(result.getWarning().get()).isNull();
        SpaAppointment appointment = result.getAppointment();
        assertThat(appointment.getStatus()).isEqualTo(SpaAppointmentStatus.BOOKED);
        assertThat(appointment.getDurationMinutes()).isEqualTo(60);
        assertThat(appointment.getGuestName()).isEqualTo("Spa Test Guest");
        assertThat(appointment.getTableLabel()).isEqualTo(table.getLabel());
        assertThat(appointment.getTherapistEmail()).isEqualTo(therapist.getEmail());
        assertThat(appointment.getOrderId().get()).isNull();
    }

    /** CORRECTION 1: the stay window is inclusive of the departure day - the guest is still in the hotel that morning. */
    @Test
    void create_onDepartureDay_noWarning() {
        LocalDate checkIn = LocalDate.now().plusDays(325);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking booking = createBooking(checkIn, checkOut);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        SpaAppointmentResult result = spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkOut.toString(), "10:00"),
                receptionist.getId());

        assertThat(result.getWarning().get()).isNull();
    }

    @Test
    void create_dateAfterCheckOut_warnsButSucceeds() {
        LocalDate checkIn = LocalDate.now().plusDays(330);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking booking = createBooking(checkIn, checkOut);
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        SpaAppointmentResult result = spaAppointmentService.create(
                new SpaAppointmentCreateInput(
                        booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkOut.plusDays(1).toString(), "10:00"),
                receptionist.getId());

        assertThat(result.getWarning().get()).isNotNull();
        assertThat(result.getAppointment().getStatus()).isEqualTo(SpaAppointmentStatus.BOOKED);
    }

    @Test
    void create_nonSpaTreatment_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(335);
        Booking booking = createBooking(checkIn, checkIn.plusDays(2));
        TableEntity table = createSpaTable();
        MenuItemEntity kitchenItem = createTreatment(null, MenuDepartment.KITCHEN);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        assertThatThrownBy(() -> spaAppointmentService.create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), kitchenItem.getId(), checkIn.toString(), "10:00"),
                        receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_spaItemWithNoDuration_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(336);
        Booking booking = createBooking(checkIn, checkIn.plusDays(2));
        TableEntity table = createSpaTable();
        MenuItemEntity noDuration = createTreatment(null, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        assertThatThrownBy(() -> spaAppointmentService.create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), noDuration.getId(), checkIn.toString(), "10:00"),
                        receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_beforeOpeningHours_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(337);
        Booking booking = createBooking(checkIn, checkIn.plusDays(2));
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        // Default app.spa.opening-time is 09:00 - see application.properties.
        assertThatThrownBy(() -> spaAppointmentService.create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "08:00"),
                        receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_pastClosingTime_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(338);
        Booking booking = createBooking(checkIn, checkIn.plusDays(2));
        TableEntity table = createSpaTable();
        // Default app.spa.closing-time is 20:00 - a 60-minute treatment starting 19:30 would run past it.
        MenuItemEntity treatment = createTreatment(60, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        assertThatThrownBy(() -> spaAppointmentService.create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "19:30"),
                        receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_offGridStartTime_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(339);
        Booking booking = createBooking(checkIn, checkIn.plusDays(2));
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        // Default app.spa.slot-minutes is 30 - 10:15 isn't a slot boundary from a 09:00 opening.
        assertThatThrownBy(() -> spaAppointmentService.create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:15"),
                        receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_toCancelled_recordsActorAndReason() {
        LocalDate checkIn = LocalDate.now().plusDays(340);
        Booking booking = createBooking(checkIn, checkIn.plusDays(2));
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"),
                        receptionist.getId())
                .getAppointment();

        SpaAppointment cancelled = spaAppointmentService.updateStatus(
                created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.CANCELLED).cancelReason("Guest rescheduled"), receptionist.getId());

        assertThat(cancelled.getStatus()).isEqualTo(SpaAppointmentStatus.CANCELLED);
        assertThat(cancelled.getCancelledByUserId().get()).isEqualTo(receptionist.getId());
        assertThat(cancelled.getCancelReason().get()).isEqualTo("Guest rescheduled");
    }

    @Test
    void updateStatus_toBooked_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(341);
        Booking booking = createBooking(checkIn, checkIn.plusDays(2));
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"),
                        receptionist.getId())
                .getAppointment();

        assertThatThrownBy(() -> spaAppointmentService.updateStatus(
                        created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.BOOKED), receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_alreadyInEndState_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(342);
        Booking booking = createBooking(checkIn, checkIn.plusDays(2));
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment(60, MenuDepartment.SPA);
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();
        SpaAppointment created = spaAppointmentService
                .create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapist.getId(), treatment.getId(), checkIn.toString(), "10:00"),
                        receptionist.getId())
                .getAppointment();
        spaAppointmentService.updateStatus(created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.NO_SHOW), receptionist.getId());

        assertThatThrownBy(() -> spaAppointmentService.updateStatus(
                        created.getId(), new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.CANCELLED), receptionist.getId()))
                .isInstanceOf(BadRequestException.class);
    }
}
