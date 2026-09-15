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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): the fingerprint terminal's own
 * numeric PIN for a person - see {@code User.enrollmentNumber}'s own openapi.yaml description for
 * why a device punch can't be attributed without one, and why it's unique and nullable.
 */
@SpringBootTest
@Transactional
class UserEnrollmentNumberTests extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity persistUser() {
        UserEntity entity = new UserEntity();
        entity.setEmail("enrollment-number-" + UUID.randomUUID() + "@example.com");
        entity.setName(entity.getEmail());
        entity.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        entity.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(entity);
    }

    @Test
    void create_withNoEnrollmentNumber_staysNull() {
        User created = userService.create(new UserCreateInput("Cook " + UUID.randomUUID()));

        assertThat(created.getEnrollmentNumber()).isNull();
    }

    @Test
    void create_withAnEnrollmentNumber_isHonored() {
        int number = uniqueNumber();
        User created = userService.create(new UserCreateInput("Housekeeper " + UUID.randomUUID()).enrollmentNumber(number));

        assertThat(created.getEnrollmentNumber()).isEqualTo(number);
    }

    @Test
    void create_duplicateEnrollmentNumber_isConflict() {
        int number = uniqueNumber();
        userService.create(new UserCreateInput("First " + UUID.randomUUID()).enrollmentNumber(number));

        assertThatThrownBy(() -> userService.create(new UserCreateInput("Second " + UUID.randomUUID()).enrollmentNumber(number)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("enrollment number");
    }

    @Test
    void updateEnrollmentNumber_setsItWithoutBumpingTokenVersion() {
        UserEntity user = persistUser();
        int number = uniqueNumber();

        User result = userService.updateEnrollmentNumber(user.getId(), JsonNullable.of(number));

        assertThat(result.getEnrollmentNumber()).isEqualTo(number);
        UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEnrollmentNumber()).isEqualTo(number);
        assertThat(reloaded.getTokenVersion()).isZero();
    }

    @Test
    void updateEnrollmentNumber_toNull_clearsIt() {
        UserEntity user = persistUser();
        userService.updateEnrollmentNumber(user.getId(), JsonNullable.of(uniqueNumber()));

        User result = userService.updateEnrollmentNumber(user.getId(), JsonNullable.of(null));

        assertThat(result.getEnrollmentNumber()).isNull();
    }

    /** Omitting the field entirely (an undefined JsonNullable) has no sensible no-op meaning for this endpoint - see the service's own comment. */
    @Test
    void updateEnrollmentNumber_omittedEntirely_isRejected() {
        UserEntity user = persistUser();

        assertThatThrownBy(() -> userService.updateEnrollmentNumber(user.getId(), JsonNullable.undefined()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateEnrollmentNumber_alreadyAssignedToSomeoneElse_isConflict() {
        UserEntity first = persistUser();
        UserEntity second = persistUser();
        int number = uniqueNumber();
        userService.updateEnrollmentNumber(first.getId(), JsonNullable.of(number));

        assertThatThrownBy(() -> userService.updateEnrollmentNumber(second.getId(), JsonNullable.of(number)))
                .isInstanceOf(ConflictException.class);
    }

    private static int uniqueNumber() {
        return Math.abs(UUID.randomUUID().hashCode()) % 1_000_000 + 1;
    }
}
