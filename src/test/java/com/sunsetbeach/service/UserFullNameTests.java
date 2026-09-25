package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
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
 * {@code User.fullName} is optional and independent of {@code name} - setting or clearing it must
 * never touch {@code name}, which every other screen keeps displaying. Blank normalizes to unset,
 * and an omitted field on {@code PATCH /users/{id}/full-name} is a 400, not a silent no-op.
 */
@SpringBootTest
@Transactional
class UserFullNameTests extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity persistUser() {
        UserEntity entity = new UserEntity();
        entity.setEmail("full-name-" + UUID.randomUUID() + "@example.com");
        entity.setName("Short " + UUID.randomUUID());
        entity.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        entity.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(entity);
    }

    @Test
    void create_withoutFullName_leavesItUnset() {
        User created = userService.create(new UserCreateInput("Cook " + UUID.randomUUID()));

        assertThat(created.getFullName().orElse(null)).isNull();
    }

    @Test
    void create_withFullName_trimsAndKeepsNameSeparate() {
        String name = "Nok " + UUID.randomUUID();
        User created = userService.create(new UserCreateInput(name).fullName("  Siriporn Chaiyaphum  "));

        assertThat(created.getName()).isEqualTo(name);
        assertThat(created.getFullName().get()).isEqualTo("Siriporn Chaiyaphum");
    }

    @Test
    void updateFullName_setsWithoutTouchingNameOrTokenVersion() {
        UserEntity user = persistUser();

        User result = userService.updateFullName(user.getId(), JsonNullable.of("Full Legal Name"));

        assertThat(result.getFullName().get()).isEqualTo("Full Legal Name");
        UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("Full Legal Name");
        assertThat(reloaded.getName()).isEqualTo(user.getName());
        assertThat(reloaded.getTokenVersion()).isZero();
    }

    @Test
    void updateFullName_nullOrBlank_clearsIt() {
        UserEntity user = persistUser();
        userService.updateFullName(user.getId(), JsonNullable.of("Someone"));

        userService.updateFullName(user.getId(), JsonNullable.of("   "));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getFullName()).isNull();

        userService.updateFullName(user.getId(), JsonNullable.of("Someone"));
        userService.updateFullName(user.getId(), JsonNullable.of(null));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getFullName()).isNull();
    }

    @Test
    void updateFullName_omitted_isRejected() {
        UserEntity user = persistUser();

        assertThatThrownBy(() -> userService.updateFullName(user.getId(), JsonNullable.undefined()))
                .isInstanceOf(BadRequestException.class);
    }
}
