package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserFunctionsUpdateInput;
import com.sunsetbeach.repository.UserRepository;
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

    private UserEntity persistUser() {
        UserEntity entity = new UserEntity();
        entity.setEmail("job-functions-" + UUID.randomUUID() + "@example.com");
        entity.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        entity.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(entity);
    }

    @Test
    void updateFunctions_setsFunctionsWithoutBumpingTokenVersion() {
        UserEntity user = persistUser();

        User result = userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER)));

        assertThat(result.getFunctions()).containsExactly(JobFunction.ENGINEER);
        UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getJobFunctions()).containsExactly("ENGINEER");
        assertThat(reloaded.getTokenVersion()).isZero();
    }

    @Test
    void updateFunctions_withMultipleFunctions_setsBoth() {
        UserEntity user = persistUser();

        User result = userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER, JobFunction.HOUSEKEEPER)));

        assertThat(result.getFunctions()).containsExactlyInAnyOrder(JobFunction.ENGINEER, JobFunction.HOUSEKEEPER);
    }

    @Test
    void updateFunctions_isAFullReplaceNotAnIncrementalAdd() {
        UserEntity user = persistUser();
        userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER)));

        User result = userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.HOUSEKEEPER)));

        assertThat(result.getFunctions()).containsExactly(JobFunction.HOUSEKEEPER);
    }

    @Test
    void updateFunctions_toEmptyList_clearsFunctions() {
        UserEntity user = persistUser();
        userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER)));

        User result = userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of()));

        assertThat(result.getFunctions()).isEmpty();
    }

    @Test
    void updateFunctions_deduplicatesRepeatedValues() {
        UserEntity user = persistUser();

        User result = userService.updateFunctions(user.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.ENGINEER, JobFunction.ENGINEER)));

        assertThat(result.getFunctions()).containsExactly(JobFunction.ENGINEER);
    }
}
