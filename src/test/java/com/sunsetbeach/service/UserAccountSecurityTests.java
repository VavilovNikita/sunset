package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserRoleUpdateInput;
import com.sunsetbeach.repository.UserRepository;
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

    private UserEntity persistUser(String password) {
        UserEntity entity = new UserEntity();
        entity.setEmail("account-sec-" + UUID.randomUUID() + "@example.com");
        entity.setPasswordHash(passwordEncoder.encode(password));
        entity.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(entity);
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
}
