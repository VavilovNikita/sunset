package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the mechanism {@link SpaAppointmentService#swapTables} depends on, BEFORE that method
 * is written to rely on it - see {@link SpaAppointmentTableSwapConstraintTimingTests} for why a
 * single UPDATE (and, by extension, two ordinary UPDATEs under the default constraint timing)
 * cannot do this swap at all.
 *
 * <p>{@code @Transactional} deliberately, unlike most DB-backed tests in this codebase: the whole
 * point under test is behaviour that only exists *within one transaction* (a
 * {@code SET CONSTRAINTS ... DEFERRED} that would mean nothing if each statement below ran in
 * its own auto-committed transaction), and Spring's test rollback afterward is exactly the
 * cleanup this needs - no manual {@code @AfterEach} bookkeeping.
 *
 * <p>Each test does, in order, exactly what {@code swapTables} itself will do: defer both
 * exclusion constraints for this transaction, apply two ordinary {@code saveAndFlush} entity
 * updates (flushed explicitly rather than left to Hibernate's own batching, so the UPDATE
 * statements are guaranteed to have already reached the database - in this exact order - before
 * the next step runs), then force the deferred checks to run immediately. The two tests below
 * are the two outcomes that matter: a state that's actually fine at that point passes silently,
 * and a state that still conflicts is still caught right there, as a normal, translatable
 * exception - not silently deferred all the way to an opaque failure at COMMIT.
 */
@SpringBootTest
@Transactional
class SpaAppointmentTableSwapDeferredConstraintTests extends AbstractIntegrationTest {

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Booking createBooking() {
        LocalDate checkIn = LocalDate.now().plusDays(365);
        RoomEntity room = new RoomEntity();
        room.setName("Deferred Swap Test Room");
        room.setDescription("Used only by SpaAppointmentTableSwapDeferredConstraintTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Deferred Swap Test Unit");
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);

        return bookingService.createStaffBooking(new StaffBookingCreateInput(
                        savedRoom.getId(), "Deferred Swap Test Guest", checkIn.toString(), checkIn.plusDays(2).toString())
                .roomUnitId(savedUnit.getId()));
    }

    private TableEntity createSpaTable(String label) {
        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel(label);
        table.setCapacity(1);
        table.setActive(true);
        return tableRepository.saveAndFlush(table);
    }

    private MenuItemEntity createTreatment(int durationMinutes) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Deferred Swap Test Massage " + durationMinutes);
        item.setDescription("Used only by SpaAppointmentTableSwapDeferredConstraintTests");
        item.setCategory("Massage");
        item.setDepartment(MenuDepartment.SPA);
        item.setPrice(new BigDecimal("1500.00"));
        item.setAvailable(true);
        item.setDurationMinutes(durationMinutes);
        return menuItemRepository.saveAndFlush(item);
    }

    private UserEntity createTherapist(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.WAITER);
        user.setActive(true);
        user.setJobFunctions(new String[] {JobFunction.THERAPIST.getValue()});
        return userRepository.saveAndFlush(user);
    }

    private UserEntity createReceptionist() {
        UserEntity user = new UserEntity();
        user.setEmail("deferred-swap-reception@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.CASHIER);
        user.setActive(true);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void deferThenForceImmediate_passesSilentlyWhenTheFinalStateIsConflictFree() {
        Booking booking = createBooking();
        TableEntity tableA = createSpaTable("Deferred Swap Test Table A1");
        TableEntity tableB = createSpaTable("Deferred Swap Test Table B1");
        MenuItemEntity treatment = createTreatment(60);
        UserEntity therapistA = createTherapist("deferred-swap-therapist-a1@example.com");
        UserEntity therapistB = createTherapist("deferred-swap-therapist-b1@example.com");
        UserEntity receptionist = createReceptionist();

        // Same time, different tables - each one's target table is the other's own, still
        // occupied at this exact instant. This is precisely the state a NOT DEFERRABLE, per-row
        // check refuses (see SpaAppointmentTableSwapConstraintTimingTests) - the state this test
        // exists to prove is fine once the checks are deferred and only forced after both moves.
        SpaAppointmentResult resultA = spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), tableA.getId(), therapistA.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                receptionist.getId());
        SpaAppointmentResult resultB = spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), tableB.getId(), therapistB.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                receptionist.getId());
        SpaAppointmentEntity a = spaAppointmentRepository.findById(resultA.getAppointment().getId()).orElseThrow();
        SpaAppointmentEntity b = spaAppointmentRepository.findById(resultB.getAppointment().getId()).orElseThrow();

        spaAppointmentRepository.deferOverlapConstraints();
        a.setTableId(tableB.getId());
        spaAppointmentRepository.saveAndFlush(a);
        b.setTableId(tableA.getId());
        spaAppointmentRepository.saveAndFlush(b);
        spaAppointmentRepository.restoreImmediateOverlapConstraints(); // must not throw

        assertThat(spaAppointmentRepository.findById(a.getId()).orElseThrow().getTableId()).isEqualTo(tableB.getId());
        assertThat(spaAppointmentRepository.findById(b.getId()).orElseThrow().getTableId()).isEqualTo(tableA.getId());
    }

    @Test
    void deferThenForceImmediate_stillCatchesAGenuineConflict() {
        Booking booking = createBooking();
        TableEntity tableA = createSpaTable("Deferred Swap Test Table A2");
        TableEntity tableB = createSpaTable("Deferred Swap Test Table B2");
        MenuItemEntity treatment = createTreatment(60);
        UserEntity therapistA = createTherapist("deferred-swap-therapist-a2@example.com");
        UserEntity therapistC = createTherapist("deferred-swap-therapist-c2@example.com");
        UserEntity receptionist = createReceptionist();

        SpaAppointmentResult resultA = spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), tableA.getId(), therapistA.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                receptionist.getId());
        // A third, unrelated appointment genuinely occupies tableB at this same time - not part
        // of the swap, and never moves. Deferring the check must not make this go away.
        spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), tableB.getId(), therapistC.getId(), treatment.getId(), booking.getCheckIn(), "10:00"),
                receptionist.getId());
        SpaAppointmentEntity a = spaAppointmentRepository.findById(resultA.getAppointment().getId()).orElseThrow();

        spaAppointmentRepository.deferOverlapConstraints();
        a.setTableId(tableB.getId());
        spaAppointmentRepository.saveAndFlush(a); // no exception yet - the check is deferred

        assertThatThrownBy(spaAppointmentRepository::restoreImmediateOverlapConstraints)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(rootCauseMessage(e)).contains("spa_appointment_no_table_overlap"));
    }

    private static String rootCauseMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
