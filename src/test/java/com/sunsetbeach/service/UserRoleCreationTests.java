package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): confirms POST /users accepts the
 * two roles the V2 migration added to the DB-side "Role" enum (CASHIER/WAITER) - the openapi.yaml
 * `Role` schema and the generated `Role.java` model already list all four values, so this is
 * purely a regression guard against that drifting, not a fix.
 */
@SpringBootTest
@Transactional
class UserRoleCreationTests {

    @Autowired
    private UserService userService;

    @Test
    void create_withWaiterRole_succeeds() {
        UserCreateInput input = new UserCreateInput(uniqueEmail(), "password1234");
        input.setRole(Role.WAITER);

        User created = userService.create(input);

        assertThat(created.getRole()).isEqualTo(Role.WAITER);
    }

    @Test
    void create_withCashierRole_succeeds() {
        UserCreateInput input = new UserCreateInput(uniqueEmail(), "password1234");
        input.setRole(Role.CASHIER);

        User created = userService.create(input);

        assertThat(created.getRole()).isEqualTo(Role.CASHIER);
    }

    private static String uniqueEmail() {
        return "staff-" + UUID.randomUUID() + "@example.com";
    }
}
