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
import com.sunsetbeach.model.SwapSpaAppointmentTableInput;
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
    private com.sunsetbeach.repository.SpaAppointmentTreatmentRepository spaAppointmentTreatmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdRoomIds = new ArrayList<>();
    private final List<String> createdTableIds = new ArrayList<>();
    private final List<String> createdMenuItemIds = new ArrayList<>();
    private final List<String> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        // Treatment rows before their parent appointment - SpaAppointmentTreatment.spaAppointmentId
        // FK-references SpaAppointment, with no cascade (see V50's own comment).
        List<com.sunsetbeach.entity.SpaAppointmentEntity> appointments = spaAppointmentRepository.findAll().stream()
                .filter(a -> createdTableIds.contains(a.getTableId()))
                .toList();
        spaAppointmentTreatmentRepository.deleteAll(
                spaAppointmentTreatmentRepository.findBySpaAppointmentIdIn(appointments.stream().map(a -> a.getId()).toList()));
        spaAppointmentRepository.deleteAll(appointments);
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
        user.setName(user.getEmail());
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
        user.setName(user.getEmail());
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

    /**
     * The swap side of the same guarantee: {@code swapTables} defers its own two saves and only
     * checks them at the very end (see {@code SpaAppointmentTableSwapDeferredConstraintTests}),
     * so it never sees a genuine third-party conflict early - it has to be caught by the forced
     * immediate check right before commit. A concurrent, ordinary (non-deferred) create racing
     * for the exact table one side of the swap is moving into must still resolve to exactly one
     * winner: PostgreSQL's exclusion-constraint machinery makes an immediate-checked writer wait
     * on a possibly-conflicting row from a still-open, deferred transaction rather than skip past
     * it, so this isn't a gap deferring the swap's own checks could open up.
     *
     * <p>This exact race can resolve two different ways depending on timing, and both are
     * legitimate: either PostgreSQL rejects the losing write outright as a genuine exclusion
     * violation (the deferred check or the immediate one finds a real overlap), or - the case that
     * used to escape as a raw, untranslated exception - it kills one side to break a deadlock
     * between the two constraint checks (see {@code SpaAppointmentService#translateOverlap}'s own
     * javadoc). Either way the loser must get one of the two known sentences, never something
     * else - an untranslated failure passing a bare "was it thrown" assertion is exactly the
     * regression this test guards against.
     */
    @Test
    void concurrentSwapAndCreate_racingForTheSameTable_exactlyOneSucceeds() throws Exception {
        Booking booking = createBooking();
        TableEntity tableA = createSpaTable();
        TableEntity tableB = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapistA = createTherapist();
        UserEntity therapistB = createTherapist();
        UserEntity therapistC = createTherapist();
        UserEntity receptionist = createReceptionist();

        SpaAppointmentResult resultA = spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), tableA.getId(), therapistA.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                receptionist.getId());
        SpaAppointmentResult resultB = spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), tableB.getId(), therapistB.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                receptionist.getId());
        String appointmentAId = resultA.getAppointment().getId();
        String appointmentBId = resultB.getAppointment().getId();

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> swap = () -> {
            barrier.await();
            return spaAppointmentService.swapTables(appointmentAId, new SwapSpaAppointmentTableInput(appointmentBId), receptionist.getId());
        };
        // Races for tableB - exactly what appointmentA is trying to move into.
        Callable<Object> thirdPartyCreate = () -> {
            barrier.await();
            return spaAppointmentService.create(
                    new SpaAppointmentCreateInput(booking.getId(), tableB.getId(), therapistC.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                    receptionist.getId());
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        int succeeded = 0;
        List<String> conflictMessages = new ArrayList<>();
        try {
            Future<Object> swapFuture = executor.submit(swap);
            Future<Object> createFuture = executor.submit(thirdPartyCreate);
            for (Future<Object> f : List.of(swapFuture, createFuture)) {
                try {
                    f.get();
                    succeeded++;
                } catch (Exception e) {
                    if (e.getCause() instanceof ConflictException conflict) {
                        conflictMessages.add(conflict.getMessage());
                    } else {
                        throw e;
                    }
                }
            }
        } finally {
            executor.shutdown();
        }

        assertThat(succeeded).isEqualTo(1);
        assertThat(conflictMessages).hasSize(1);
        assertThat(conflictMessages.get(0))
                .isIn(
                        "This table already has an appointment overlapping this time.",
                        "Someone else was changing one of these appointments at the same time — please try again.");
        // Whichever won, tableB has exactly one BOOKED occupant at this time - not zero (both
        // somehow failed) and not two (both somehow "succeeded" against the same slot).
        assertThat(spaAppointmentRepository.findByDate(java.time.LocalDate.parse(booking.getCheckIn())).stream()
                        .filter(a -> a.getTableId().equals(tableB.getId())
                                && a.getStatus() == com.sunsetbeach.model.SpaAppointmentStatus.BOOKED)
                        .count())
                .isEqualTo(1);
    }

    /**
     * Regression test for the deadlock {@code swapTables} used to be exposed to whenever two
     * swaps named the same pair of appointments in opposite order (two receptionists, each
     * clicking "swap" from a different one of the two appointment cards) - see that method's own
     * Concurrency section. Both calls describe the identical target state (A takes B's table, B
     * takes A's), so a deterministic (lowest-id-first) lock order lets them serialize cleanly
     * instead of each locking one row and waiting on the other. If the deadlock were still
     * reachable, one side would surface a raw, uncaught exception here rather than either
     * completing or a translated {@link ConflictException} - this test fails loudly either way,
     * not silently.
     */
    @Test
    void concurrentSwapSwap_sameTwoAppointmentsNamedInOpposingOrder_neitherDeadlocks() throws Exception {
        Booking booking = createBooking();
        TableEntity tableA = createSpaTable();
        TableEntity tableB = createSpaTable();
        MenuItemEntity treatment = createTreatment();
        UserEntity therapistA = createTherapist();
        UserEntity therapistB = createTherapist();
        UserEntity receptionist = createReceptionist();

        SpaAppointmentResult resultA = spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), tableA.getId(), therapistA.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                receptionist.getId());
        SpaAppointmentResult resultB = spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), tableB.getId(), therapistB.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                receptionist.getId());
        String appointmentAId = resultA.getAppointment().getId();
        String appointmentBId = resultB.getAppointment().getId();

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> swapNamedAThenB = () -> {
            barrier.await();
            return spaAppointmentService.swapTables(appointmentAId, new SwapSpaAppointmentTableInput(appointmentBId), receptionist.getId());
        };
        Callable<Object> swapNamedBThenA = () -> {
            barrier.await();
            return spaAppointmentService.swapTables(appointmentBId, new SwapSpaAppointmentTableInput(appointmentAId), receptionist.getId());
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        int completed = 0;
        List<String> conflictMessages = new ArrayList<>();
        try {
            Future<Object> first = executor.submit(swapNamedAThenB);
            Future<Object> second = executor.submit(swapNamedBThenA);
            for (Future<Object> f : List.of(first, second)) {
                try {
                    f.get();
                    completed++;
                } catch (Exception e) {
                    if (e.getCause() instanceof ConflictException conflict) {
                        conflictMessages.add(conflict.getMessage());
                    } else {
                        throw e;
                    }
                }
            }
        } finally {
            executor.shutdown();
        }

        assertThat(completed + conflictMessages.size()).isEqualTo(2);
        assertThat(conflictMessages)
                .allMatch(message -> message.equals("Someone else was changing one of these appointments at the same time — please try again."));
        // Both calls describe the same swap - the end state must be that swap having happened
        // exactly once, regardless of which call "won".
        assertThat(spaAppointmentRepository.findById(appointmentAId).orElseThrow().getTableId()).isEqualTo(tableB.getId());
        assertThat(spaAppointmentRepository.findById(appointmentBId).orElseThrow().getTableId()).isEqualTo(tableA.getId());
    }
}
