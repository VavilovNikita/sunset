package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Do not delete this test as "obviously passing," fold it into SpaAppointmentServiceTests as one
 * case among many, or read a future green run of it as license to drop the explicit
 * {@code deferOverlapConstraints}/{@code restoreImmediateOverlapConstraints} calls from
 * {@link SpaAppointmentService#swapTables} in favour of two plain saves. It exists specifically
 * to keep failing the day someone makes that simplification, and to explain why it has to fail.
 *
 * <p><b>History - what was tried first, and why it failed then.</b> Before V56, both
 * {@code SpaAppointment} exclusion constraints were {@code NOT DEFERRABLE}. A single native
 * {@code UPDATE} moving both appointments' {@code tableId} at once, via a {@code CASE} keyed on
 * {@code id}, was tried on the theory that PostgreSQL checks a constraint like that once, at the
 * end of the statement - which would mean the transient mid-swap state (each appointment briefly
 * wanting the table the other hasn't vacated yet) is never actually checked, only the final,
 * conflict-free result. That theory was wrong for a {@code NOT DEFERRABLE} constraint: PostgreSQL
 * enforces it synchronously, per row, as each row's GiST index entry is written during statement
 * execution - not once against the statement's final result - so the single-statement swap still
 * failed with a real {@code spa_appointment_no_table_overlap} violation whenever the two
 * appointments' times actually overlapped (the only case a swap is ever worth doing).
 *
 * <p><b>Why that same single-statement attempt is not tested here any more.</b> V56 made both
 * constraints {@code DEFERRABLE INITIALLY IMMEDIATE} - and "checked once, at the end of the
 * statement" genuinely is what {@code INITIALLY IMMEDIATE} means for a constraint that actually
 * is deferrable. Empirically, the exact single native {@code UPDATE} described above now
 * succeeds against the current schema, with no explicit {@code SET CONSTRAINTS} at all - the
 * theory that was wrong before V56 happens to become true after it, incidentally. This codebase
 * does not rely on that: relying on "it happens to be one SQL statement" is exactly the kind of
 * heuristic-that-happens-to-work this feature was designed to avoid, and it would silently break
 * the moment anyone touched the appointment through the ordinary per-entity save path this
 * service uses everywhere else. What actually matters, and what stays true regardless of that
 * incidental fact, is proven below instead.
 *
 * <p><b>What this test proves instead, and keeps proving.</b> {@code INITIALLY IMMEDIATE} batches
 * a deferrable constraint's check to the end of the *current statement* - not the current
 * transaction. Two separate {@code UPDATE}s, even inside one transaction, are still two separate
 * statements, each still checked at the end of *itself* unless something explicitly defers that
 * checking for the transaction. So the "obvious simplification" once V56 exists - drop the
 * explicit defer/restore calls and just save both entities, since the constraint is deferrable
 * now anyway - still fails exactly the way the pre-V56 single-statement attempt did, for exactly
 * the same underlying reason (a check running before both rows have moved). This is the failure
 * mode {@code swapTables}'s explicit {@code deferOverlapConstraints()} call exists to prevent,
 * proven working (both the success and the still-genuinely-conflicting case) in
 * {@link SpaAppointmentTableSwapDeferredConstraintTests}. See also CLAUDE.md's Concurrency
 * section, which records this whole history for whoever reaches for a swap next.
 */
@SpringBootTest
class SpaAppointmentTableSwapConstraintTimingTests extends AbstractIntegrationTest {

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
        List<SpaAppointmentEntity> appointments =
                spaAppointmentRepository.findAll().stream().filter(a -> createdTableIds.contains(a.getTableId())).toList();
        spaAppointmentTreatmentRepository.deleteAll(
                spaAppointmentTreatmentRepository.findBySpaAppointmentIdIn(appointments.stream().map(SpaAppointmentEntity::getId).toList()));
        spaAppointmentRepository.deleteAll(appointments);
        createdTableIds.forEach(tableRepository::deleteById);
        createdMenuItemIds.forEach(menuItemRepository::deleteById);
        createdUserIds.forEach(userRepository::deleteById);
        bookingRepository.deleteAll(bookingRepository.findAll().stream().filter(b -> createdRoomIds.contains(b.getRoomId())).toList());
        for (String roomId : createdRoomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
    }

    private Booking createBooking() {
        LocalDate checkIn = LocalDate.now().plusDays(360);
        RoomEntity room = new RoomEntity();
        room.setName("Swap Timing Test Room " + UUID.randomUUID());
        room.setDescription("Used only by SpaAppointmentTableSwapConstraintTimingTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());

        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Swap Timing Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);

        return bookingService.createStaffBooking(new StaffBookingCreateInput(
                        savedRoom.getId(), "Swap Timing Test Guest", checkIn.toString(), checkIn.plusDays(2).toString())
                .roomUnitId(savedUnit.getId()));
    }

    private TableEntity createSpaTable() {
        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Swap Timing Test Table " + UUID.randomUUID());
        table.setCapacity(1);
        table.setActive(true);
        TableEntity saved = tableRepository.saveAndFlush(table);
        createdTableIds.add(saved.getId());
        return saved;
    }

    private MenuItemEntity createTreatment(int durationMinutes) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Swap Timing Test Massage " + UUID.randomUUID());
        item.setDescription("Used only by SpaAppointmentTableSwapConstraintTimingTests");
        item.setCategory("Massage");
        item.setDepartment(MenuDepartment.SPA);
        item.setPrice(new BigDecimal("1500.00"));
        item.setAvailable(true);
        item.setDurationMinutes(durationMinutes);
        MenuItemEntity saved = menuItemRepository.saveAndFlush(item);
        createdMenuItemIds.add(saved.getId());
        return saved;
    }

    private UserEntity createTherapist() {
        UserEntity user = new UserEntity();
        user.setEmail("swap-timing-therapist-" + UUID.randomUUID() + "@example.com");
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
        user.setEmail("swap-timing-reception-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.CASHIER);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    /**
     * Same time, different tables, different durations (to also show the failure has nothing to
     * do with length) - each one's *new* table is the table the other is, at this exact instant,
     * still sitting on. No {@code deferOverlapConstraints()} call here - this reproduces exactly
     * what {@code swapTables} would do if that call were ever removed as "unnecessary now that
     * the constraint is deferrable anyway." The very first {@code saveAndFlush} must already
     * fail: {@code INITIALLY IMMEDIATE} checks at the end of *that* statement, and the other
     * appointment hasn't been touched by anything yet.
     */
    @Test
    void twoSequentialSavesWithoutExplicitDeferral_stillFailTheSameWayTheOriginalAttemptDid() {
        Booking bookingA = createBooking();
        Booking bookingB = createBooking();
        TableEntity tableA = createSpaTable();
        TableEntity tableB = createSpaTable();
        MenuItemEntity shortTreatment = createTreatment(30);
        MenuItemEntity longTreatment = createTreatment(90);
        UserEntity therapistA = createTherapist();
        UserEntity therapistB = createTherapist();
        UserEntity receptionist = createReceptionist();

        SpaAppointmentResult resultA = spaAppointmentService.create(
                new SpaAppointmentCreateInput(bookingA.getId(), tableA.getId(), therapistA.getId(), shortTreatment.getId(), bookingA.getCheckIn(), "10:00"),
                receptionist.getId());
        // appointmentB is never referenced again by id - its mere presence on tableB at this
        // time is what appointmentA's own save must still be checked against.
        spaAppointmentService.create(
                new SpaAppointmentCreateInput(bookingB.getId(), tableB.getId(), therapistB.getId(), longTreatment.getId(), bookingB.getCheckIn(), "10:00"),
                receptionist.getId());
        SpaAppointmentEntity a = spaAppointmentRepository.findById(resultA.getAppointment().getId()).orElseThrow();

        a.setTableId(tableB.getId());
        assertThatThrownBy(() -> spaAppointmentRepository.saveAndFlush(a)).isInstanceOf(DataAccessException.class);
    }
}
