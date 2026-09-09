package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserFunctionsUpdateInput;
import com.sunsetbeach.model.UserUpdateResult;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): PATCH /users/{id}/functions -
 * job functions are a second, independent authorization axis alongside Role (see JobFunction),
 * so unlike UserAccountSecurityTests's role/active/password coverage, the point of these tests
 * is specifically that tokenVersion does NOT get bumped - see UserService#updateFunctions and
 * JwtAuthFilter for why that's safe (functions are read fresh from the DB every request, not
 * from the JWT's own claims the way role is).
 */
@SpringBootTest
@Transactional
class UserFunctionsTests extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private UserEntity persistUser() {
        UserEntity entity = new UserEntity();
        entity.setEmail("job-functions-" + UUID.randomUUID() + "@example.com");
        entity.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        entity.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(entity);
    }

    /** A single future BOOKED appointment assigned to {@code therapist} - see the warning tests below. */
    private void bookFutureAppointmentFor(UserEntity therapist, UserEntity actor) {
        RoomEntity room = new RoomEntity();
        room.setName("Job Functions Room " + UUID.randomUUID());
        room.setDescription("Used only by UserFunctionsTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Job Functions Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);
        LocalDate checkIn = LocalDate.now().plusDays(365);
        Booking booking = bookingService.createStaffBooking(new StaffBookingCreateInput(
                        savedRoom.getId(), "Job Functions Guest", checkIn.toString(), checkIn.plusDays(2).toString())
                .roomUnitId(savedUnit.getId()));

        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Job Functions Table " + UUID.randomUUID());
        table.setCapacity(1);
        table.setActive(true);
        TableEntity savedTable = tableRepository.saveAndFlush(table);

        MenuItemEntity treatment = new MenuItemEntity();
        treatment.setName("Job Functions Massage " + UUID.randomUUID());
        treatment.setDescription("Used only by UserFunctionsTests");
        treatment.setCategory("Massage");
        treatment.setDepartment(MenuDepartment.SPA);
        treatment.setPrice(new BigDecimal("1500.00"));
        treatment.setAvailable(true);
        treatment.setDurationMinutes(60);
        MenuItemEntity savedTreatment = menuItemRepository.saveAndFlush(treatment);

        spaAppointmentService.create(
                new SpaAppointmentCreateInput(booking.getId(), savedTable.getId(), therapist.getId(), savedTreatment.getId(), checkIn.toString(), "10:00"),
                actor.getId());
    }

    @Test
    void updateFunctions_setsFunctionsWithoutBumpingTokenVersion() {
        UserEntity user = persistUser();

        User result = userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER))).getUser();

        assertThat(result.getFunctions()).containsExactly(JobFunction.ENGINEER);
        UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getJobFunctions()).containsExactly("ENGINEER");
        assertThat(reloaded.getTokenVersion()).isZero();
    }

    @Test
    void updateFunctions_withMultipleFunctions_setsBoth() {
        UserEntity user = persistUser();

        User result =
                userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER, JobFunction.HOUSEKEEPER))).getUser();

        assertThat(result.getFunctions()).containsExactlyInAnyOrder(JobFunction.ENGINEER, JobFunction.HOUSEKEEPER);
    }

    @Test
    void updateFunctions_isAFullReplaceNotAnIncrementalAdd() {
        UserEntity user = persistUser();
        userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER)));

        User result = userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.HOUSEKEEPER))).getUser();

        assertThat(result.getFunctions()).containsExactly(JobFunction.HOUSEKEEPER);
    }

    @Test
    void updateFunctions_toEmptyList_clearsFunctions() {
        UserEntity user = persistUser();
        userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER)));

        User result = userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of())).getUser();

        assertThat(result.getFunctions()).isEmpty();
    }

    @Test
    void updateFunctions_deduplicatesRepeatedValues() {
        UserEntity user = persistUser();

        User result =
                userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER, JobFunction.ENGINEER))).getUser();

        assertThat(result.getFunctions()).containsExactly(JobFunction.ENGINEER);
    }

    /**
     * The "one thing your proposal doesn't cover" fix: removing THERAPIST from a user who holds
     * a future BOOKED appointment warns rather than silently leaving a dangling assignment - the
     * function is still removed either way, nothing here blocks it.
     */
    @Test
    void updateFunctions_removingTherapistWithFutureAppointments_warns() {
        UserEntity therapist = persistUser();
        UserEntity actor = persistUser();
        userService.updateFunctions(therapist.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.THERAPIST)));
        bookFutureAppointmentFor(therapist, actor);

        UserUpdateResult result = userService.updateFunctions(therapist.getId(), new UserFunctionsUpdateInput(List.of()));

        assertThat(result.getWarning().get()).isNotNull();
        assertThat(result.getUser().getFunctions()).isEmpty();
    }

    @Test
    void updateFunctions_addingTherapist_noWarning() {
        UserEntity user = persistUser();

        UserUpdateResult result = userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.THERAPIST)));

        assertThat(result.getWarning().get()).isNull();
    }
}
