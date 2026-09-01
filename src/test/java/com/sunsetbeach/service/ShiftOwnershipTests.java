package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.ShiftSummary;
import com.sunsetbeach.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): GET /shifts/{id} must be
 * self-scoped for a plain CASHIER (own shift only, 404 on anyone else's) while MANAGER/ADMIN can
 * look up any shift - the gap the review flagged (the HTTP-layer matcher alone can't express
 * "your own resource only", that's ShiftService.getSummary's job).
 */
@SpringBootTest
@Transactional
class ShiftOwnershipTests extends AbstractIntegrationTest {

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private UserRepository userRepository;

    private UserEntity cashierA;
    private UserEntity cashierB;
    private UserEntity manager;
    private UserEntity admin;
    private Shift shiftA;

    @BeforeEach
    void setUp() {
        cashierA = persistUser(Role.CASHIER);
        cashierB = persistUser(Role.CASHIER);
        manager = persistUser(Role.MANAGER);
        admin = persistUser(Role.ADMIN);
        shiftA = shiftService.open(cashierA.getId(), new ShiftOpenInput());
    }

    private UserEntity persistUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("shift-owner-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void getSummary_ownShift_asCashier_succeeds() {
        ShiftSummary summary = shiftService.getSummary(shiftA.getId(), cashierA.getId(), Role.CASHIER);

        assertThat(summary.getId()).isEqualTo(shiftA.getId());
    }

    @Test
    void getSummary_anotherCashiersShift_asCashier_throwsNotFound() {
        assertThatThrownBy(() -> shiftService.getSummary(shiftA.getId(), cashierB.getId(), Role.CASHIER))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getSummary_anyShift_asManager_succeeds() {
        ShiftSummary summary = shiftService.getSummary(shiftA.getId(), manager.getId(), Role.MANAGER);

        assertThat(summary.getId()).isEqualTo(shiftA.getId());
    }

    @Test
    void getSummary_anyShift_asAdmin_succeeds() {
        ShiftSummary summary = shiftService.getSummary(shiftA.getId(), admin.getId(), Role.ADMIN);

        assertThat(summary.getId()).isEqualTo(shiftA.getId());
    }
}
