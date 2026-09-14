package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): overtime eligibility is a flag on
 * the person, defaulting to eligible, settable per person via {@code PATCH
 * /users/{id}/overtime-eligibility} - see {@code User.overtimeEligible}'s own openapi.yaml
 * description for why this can't be derived from a shift code. Like {@link UserFunctionsTests},
 * the point here is specifically that {@code tokenVersion} does NOT get bumped and there is no
 * self-change restriction - nothing here touches authentication.
 */
@SpringBootTest
@Transactional
class UserOvertimeEligibilityTests extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity persistUser() {
        UserEntity entity = new UserEntity();
        entity.setEmail("overtime-eligibility-" + UUID.randomUUID() + "@example.com");
        entity.setName(entity.getEmail());
        entity.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        entity.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(entity);
    }

    @Test
    void create_defaultsToOvertimeEligible() {
        User created = userService.create(new UserCreateInput("No Login Cook " + UUID.randomUUID()));

        assertThat(created.getOvertimeEligible()).isTrue();
    }

    @Test
    void create_explicitlyIneligible_isHonored() {
        User created = userService.create(new UserCreateInput("OP Driver " + UUID.randomUUID()).overtimeEligible(false));

        assertThat(created.getOvertimeEligible()).isFalse();
    }

    @Test
    void updateOvertimeEligible_setsFlagWithoutBumpingTokenVersion() {
        UserEntity user = persistUser();

        User result = userService.updateOvertimeEligible(user.getId(), false);

        assertThat(result.getOvertimeEligible()).isFalse();
        UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.isOvertimeEligible()).isFalse();
        assertThat(reloaded.getTokenVersion()).isZero();
    }

    @Test
    void updateOvertimeEligible_backToTrue_isHonored() {
        UserEntity user = persistUser();
        userService.updateOvertimeEligible(user.getId(), false);

        User result = userService.updateOvertimeEligible(user.getId(), true);

        assertThat(result.getOvertimeEligible()).isTrue();
    }
}
