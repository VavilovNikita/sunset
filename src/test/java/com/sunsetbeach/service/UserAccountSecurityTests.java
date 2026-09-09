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
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserRoleUpdateInput;
import com.sunsetbeach.model.UserUpdateResult;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): covers the account-security
 * additions that make JWT revocation possible - a disabled account or a stale tokenVersion is
 * only meaningful if these entry points actually flip those columns, which JwtAuthFilterTests
 * assumes but doesn't itself exercise.
 */
@SpringBootTest
@Transactional
class UserAccountSecurityTests extends AbstractIntegrationTest {

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

    private UserEntity persistUser(String password) {
        UserEntity entity = new UserEntity();
        entity.setEmail("account-sec-" + UUID.randomUUID() + "@example.com");
        entity.setPasswordHash(passwordEncoder.encode(password));
        entity.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(entity);
    }

    /** A single future BOOKED appointment assigned to {@code therapist} - see the two warning tests below. */
    private void bookFutureAppointmentFor(UserEntity therapist, UserEntity actor) {
        RoomEntity room = new RoomEntity();
        room.setName("Account Sec Room " + UUID.randomUUID());
        room.setDescription("Used only by UserAccountSecurityTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Account Sec Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);
        LocalDate checkIn = LocalDate.now().plusDays(360);
        Booking booking = bookingService.createStaffBooking(new StaffBookingCreateInput(
                        savedRoom.getId(), "Account Sec Guest", checkIn.toString(), checkIn.plusDays(2).toString())
                .roomUnitId(savedUnit.getId()));

        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Account Sec Table " + UUID.randomUUID());
        table.setCapacity(1);
        table.setActive(true);
        TableEntity savedTable = tableRepository.saveAndFlush(table);

        MenuItemEntity treatment = new MenuItemEntity();
        treatment.setName("Account Sec Massage " + UUID.randomUUID());
        treatment.setDescription("Used only by UserAccountSecurityTests");
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
    void updateRole_bumpsTokenVersion() {
        UserEntity target = persistUser("irrelevant1");
        UserEntity caller = persistUser("irrelevant2");

        userService.updateRole(target.getId(), caller.getId(), new UserRoleUpdateInput(Role.MANAGER));

        UserEntity reloaded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.MANAGER);
        assertThat(reloaded.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void changeOwnPassword_correctCurrentPassword_updatesHashAndBumpsTokenVersion() {
        UserEntity user = persistUser("old-password1");
        String oldHash = user.getPasswordHash();

        UserEntity updated = userService.changeOwnPassword(user.getId(), "old-password1", "new-password1");

        assertThat(updated.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(passwordEncoder.matches("new-password1", updated.getPasswordHash())).isTrue();
        assertThat(updated.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void changeOwnPassword_wrongCurrentPassword_isRejectedWithoutChangingAnything() {
        UserEntity user = persistUser("old-password1");
        String oldHash = user.getPasswordHash();

        assertThatThrownBy(() -> userService.changeOwnPassword(user.getId(), "wrong-password", "new-password1"))
                .isInstanceOf(UnauthorizedException.class);

        UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getPasswordHash()).isEqualTo(oldHash);
        assertThat(reloaded.getTokenVersion()).isZero();
    }

    @Test
    void resetPassword_adminReset_updatesHashAndBumpsTokenVersionWithoutCurrentPassword() {
        UserEntity target = persistUser("old-password1");

        User result = userService.resetPassword(target.getId(), "admin-set-password1");

        UserEntity reloaded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(result.getId()).isEqualTo(target.getId());
        assertThat(passwordEncoder.matches("admin-set-password1", reloaded.getPasswordHash())).isTrue();
        assertThat(reloaded.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void setActive_disablingAnotherUser_flipsFlagAndBumpsTokenVersion() {
        UserEntity target = persistUser("irrelevant1");
        UserEntity caller = persistUser("irrelevant2");

        userService.setActive(target.getId(), caller.getId(), false);

        UserEntity reloaded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void setActive_disablingSelf_isRejected() {
        UserEntity self = persistUser("irrelevant1");

        assertThatThrownBy(() -> userService.setActive(self.getId(), self.getId(), false)).isInstanceOf(BadRequestException.class);

        UserEntity reloaded = userRepository.findById(self.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.getTokenVersion()).isZero();
    }

    /**
     * The "one thing your proposal doesn't cover" fix: disabling a therapist who holds a future
     * BOOKED appointment warns rather than silently leaving a dangling assignment - the account
     * is still disabled either way, nothing here blocks it (see CLAUDE.md's Failure handling
     * section, "front-desk operations warn rather than block").
     */
    @Test
    void setActive_disablingATherapistWithFutureAppointments_warns() {
        UserEntity therapist = persistUser("irrelevant1");
        therapist.setJobFunctions(new String[] {JobFunction.THERAPIST.getValue()});
        userRepository.saveAndFlush(therapist);
        UserEntity caller = persistUser("irrelevant2");
        bookFutureAppointmentFor(therapist, caller);

        UserUpdateResult result = userService.setActive(therapist.getId(), caller.getId(), false);

        assertThat(result.getWarning().get()).isNotNull();
        UserEntity reloaded = userRepository.findById(therapist.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
    }

    @Test
    void setActive_disablingANonTherapist_noWarning() {
        UserEntity target = persistUser("irrelevant1");
        UserEntity caller = persistUser("irrelevant2");

        UserUpdateResult result = userService.setActive(target.getId(), caller.getId(), false);

        assertThat(result.getWarning().get()).isNull();
    }
}
