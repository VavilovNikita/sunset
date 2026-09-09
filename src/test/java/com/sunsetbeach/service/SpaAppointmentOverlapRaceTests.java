package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentResult;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DB-backed against the real dev Postgres, deliberately NOT {@code @Transactional} - same
 * reasoning as {@code OrderDoubleCloseRaceTests}/{@code BookingAvailabilityEngineTests}: the race
 * under test needs two genuinely separate top-level transactions/connections, which a wrapping
 * test transaction would prevent from actually contending. Cleans up what it wrote in
 * {@link #cleanUp()} instead of relying on rollback.
 *
 * <p>Covers the actual mechanism behind the SPA module's double-booking guard: two
 * {@code EXCLUDE USING gist} constraints on {@code SpaAppointment} (see
 * V41__spa_appointment.sql), not a check-then-write in {@link SpaAppointmentService}. A test that
 * calls {@code create} twice in sequence would prove nothing about the race - both would simply
 * see the first one already committed. This test drives two real concurrent transactions through
 * a {@link CyclicBarrier} so they attempt to insert overlapping appointments at (as close to)
 * the same instant as the database can arrange, on both axes: the same table with two different
 * therapists, and the same therapist across two different tables.
 */
@SpringBootTest
class SpaAppointmentOverlapRaceTests extends AbstractIntegrationTest {

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
        // Bookings before room units - see SpaAppointmentServiceTests.cleanUp's own comment.
        bookingRepository.deleteAll(bookingRepository.findAll().stream().filter(b -> createdRoomIds.contains(b.getRoomId())).toList());
        for (String roomId : createdRoomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
    }

    private Booking createBooking() {
        LocalDate checkIn = LocalDate.now().plusDays(350);
        RoomEntity room = new RoomEntity();
        room.setName("Race Test Room " + UUID.randomUUID());
        room.setDescription("Used only by SpaAppointmentOverlapRaceTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());

        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Race Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);

        return bookingService.createStaffBooking(new StaffBookingCreateInput(
                        savedRoom.getId(), "Race Test Guest", checkIn.toString(), checkIn.plusDays(2).toString())
                .roomUnitId(savedUnit.getId()));
    }

    private TableEntity createSpaTable() {
        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Race Test Table " + UUID.randomUUID());
        table.setCapacity(1);
        table.setActive(true);
        TableEntity saved = tableRepository.saveAndFlush(table);
        createdTableIds.add(saved.getId());
        return saved;
    }

    private MenuItemEntity createTreatment() {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Race Test Massage " + UUID.randomUUID());
        item.setDescription("Used only by SpaAppointmentOverlapRaceTests");
        item.setCategory("Massage");
        item.setDepartment(MenuDepartment.SPA);
        item.setPrice(new BigDecimal("1500.00"));
        item.setAvailable(true);
        item.setDurationMinutes(60);
        MenuItemEntity saved = menuItemRepository.saveAndFlush(item);
        createdMenuItemIds.add(saved.getId());
        return saved;
    }

    private UserEntity createTherapist() {
        UserEntity user = new UserEntity();
        user.setEmail("race-therapist-" + UUID.randomUUID() + "@example.com");
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
        user.setEmail("race-reception-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.CASHIER);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private record RaceOutcome(int succeeded, int conflicted) {}

    private RaceOutcome runConcurrently(Callable<SpaAppointmentResult> first, Callable<SpaAppointmentResult> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<SpaAppointmentResult> firstTask = () -> {
            barrier.await();
            return first.call();
        };
        Callable<SpaAppointmentResult> secondTask = () -> {
            barrier.await();
            return second.call();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<SpaAppointmentResult> a = executor.submit(firstTask);
            Future<SpaAppointmentResult> b = executor.submit(secondTask);

            int succeeded = 0;
            int conflicted = 0;
            for (Future<SpaAppointmentResult> f : List.of(a, b)) {
                try {
                    f.get();
                    succeeded++;
                } catch (Exception e) {
                    if (e.getCause() instanceof ConflictException) {
                        conflicted++;
                    } else {
                        throw e;
                    }
                }
            }
            return new RaceOutcome(succeeded, conflicted);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void concurrentCreate_sameTableDifferentTherapist_exactlyOneSucceeds() throws Exception {
        Booking booking = createBooking();
        TableEntity table = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapistA = createTherapist();
        UserEntity therapistB = createTherapist();
        UserEntity receptionist = createReceptionist();

        RaceOutcome outcome = runConcurrently(
                () -> spaAppointmentService.create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapistA.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                        receptionist.getId()),
                () -> spaAppointmentService.create(
                        new SpaAppointmentCreateInput(booking.getId(), table.getId(), therapistB.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                        receptionist.getId()));

        assertThat(outcome.succeeded()).isEqualTo(1);
        assertThat(outcome.conflicted()).isEqualTo(1);
        assertThat(spaAppointmentRepository.findByDate(java.time.LocalDate.parse(booking.getCheckIn())).stream()
                        .filter(a -> a.getTableId().equals(table.getId()))
                        .count())
                .isEqualTo(1);
    }

    @Test
    void concurrentCreate_sameTherapistDifferentTable_exactlyOneSucceeds() throws Exception {
        Booking booking = createBooking();
        TableEntity tableA = createSpaTable();
        TableEntity tableB = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapist = createTherapist();
        UserEntity receptionist = createReceptionist();

        RaceOutcome outcome = runConcurrently(
                () -> spaAppointmentService.create(
                        new SpaAppointmentCreateInput(booking.getId(), tableA.getId(), therapist.getId(), treatment.getId(), booking.getCheckIn(), "11:00"),
                        receptionist.getId()),
                () -> spaAppointmentService.create(
                        new SpaAppointmentCreateInput(booking.getId(), tableB.getId(), therapist.getId(), treatment.getId(), booking.getCheckIn(), "11:00"),
                        receptionist.getId()));

        assertThat(outcome.succeeded()).isEqualTo(1);
        assertThat(outcome.conflicted()).isEqualTo(1);
        assertThat(spaAppointmentRepository.findByDate(java.time.LocalDate.parse(booking.getCheckIn())).stream()
                        .filter(a -> a.getTherapistUserId().equals(therapist.getId()))
                        .count())
                .isEqualTo(1);
    }
}
