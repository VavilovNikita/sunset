package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): a credential-less {@code User}
 * (see {@code UserCreateInput}'s own javadoc) - creating one, the pairing rule that keeps
 * {@code email}/{@code passwordHash} arriving and leaving together, and {@code PATCH
 * /users/{id}/credentials} turning one into a login-capable account without recreating it.
 */
@SpringBootTest
@Transactional
class UserCredentialsServiceTests extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdUserIds.forEach(userRepository::deleteById);
    }

    private User createNoLoginEmployee(String name) {
        User created = userService.create(new UserCreateInput(name));
        createdUserIds.add(created.getId());
        return created;
    }

    @Test
    void create_withoutEmailOrPassword_succeedsWithNoCredentials() {
        User created = createNoLoginEmployee("Cook One");

        assertThat(created.getName()).isEqualTo("Cook One");
        assertThat(created.getEmail()).isNull();

        UserEntity entity = userRepository.findById(created.getId()).orElseThrow();
        assertThat(entity.getEmail()).isNull();
        assertThat(entity.getPasswordHash()).isNull();
    }

    @Test
    void create_multipleNoLoginAccounts_coexistDespiteBothHavingNoEmail() {
        // The whole point of a plain unique index: any number of NULLs are distinct from each
        // other, so two credential-less accounts never collide on "no email".
        User first = createNoLoginEmployee("Cook One");
        User second = createNoLoginEmployee("Cook Two");

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void create_emailWithoutPassword_isRejected() {
        UserCreateInput input = new UserCreateInput("Half Credentialed").email("half-" + UUID.randomUUID() + "@example.com");

        assertThatThrownBy(() -> userService.create(input)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_passwordWithoutEmail_isRejected() {
        UserCreateInput input = new UserCreateInput("Half Credentialed").password("password1234");

        assertThatThrownBy(() -> userService.create(input)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_withEmailAndPassword_stillWorksAsBefore() {
        String email = "login-employee-" + UUID.randomUUID() + "@example.com";
        User created = userService.create(new UserCreateInput("Receptionist One").email(email).password("password1234"));
        createdUserIds.add(created.getId());

        assertThat(created.getEmail()).isEqualTo(email);
        UserEntity entity = userRepository.findById(created.getId()).orElseThrow();
        assertThat(entity.getPasswordHash()).isNotNull();
    }

    @Test
    void grantCredentials_onNoLoginAccount_makesItLoginCapable() {
        User employee = createNoLoginEmployee("Dishwasher");
        String email = "promoted-" + UUID.randomUUID() + "@example.com";

        User updated = userService.grantCredentials(employee.getId(), email, "a-new-password1");

        assertThat(updated.getEmail()).isEqualTo(email);
        assertThat(updated.getName()).isEqualTo("Dishwasher");
        UserEntity entity = userRepository.findById(employee.getId()).orElseThrow();
        assertThat(entity.getEmail()).isEqualTo(email);
        assertThat(passwordEncoder.matches("a-new-password1", entity.getPasswordHash())).isTrue();
        // Same account, not a new one - the whole point of "without being recreated".
        assertThat(entity.getId()).isEqualTo(employee.getId());
    }

    @Test
    void grantCredentials_onAccountThatAlreadyHasCredentials_isRejected() {
        String email = "already-has-login-" + UUID.randomUUID() + "@example.com";
        User employee = userService.create(new UserCreateInput("Receptionist Two").email(email).password("password1234"));
        createdUserIds.add(employee.getId());

        assertThatThrownBy(() -> userService.grantCredentials(employee.getId(), "new-" + UUID.randomUUID() + "@example.com", "password1234"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void grantCredentials_withEmailAlreadyInUse_isRejected() {
        String takenEmail = "taken-" + UUID.randomUUID() + "@example.com";
        User existing = userService.create(new UserCreateInput("Existing Login User").email(takenEmail).password("password1234"));
        createdUserIds.add(existing.getId());
        User noLogin = createNoLoginEmployee("Aspiring Receptionist");

        assertThatThrownBy(() -> userService.grantCredentials(noLogin.getId(), takenEmail, "password1234")).isInstanceOf(ConflictException.class);
    }

    /**
     * The DB-level backstop behind {@link UserService#create}'s own pairing check (see V68's own
     * comment) - proven directly against the constraint, bypassing the service layer entirely,
     * the same way {@code SpaAppointmentTableSwapConstraintTimingTests} proves a constraint
     * rather than the code that's supposed to prevent reaching it.
     */
    @Test
    void halfCredentialedRow_violatesDbConstraint_evenBypassingTheService() {
        UserEntity entity = new UserEntity();
        entity.setName("Bypasses The Service");
        entity.setEmail("bypass-" + UUID.randomUUID() + "@example.com");
        entity.setPasswordHash(null); // email present, password absent - exactly what the CHECK constraint exists to reject.
        entity.setRole(Role.WAITER);

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(entity);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
