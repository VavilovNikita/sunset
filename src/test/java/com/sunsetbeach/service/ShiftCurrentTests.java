package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): GET /shifts/current must find
 * the calling user's own open shift, 404 when they have none, and never leak another user's
 * open shift even though the underlying table only has one row per open shift globally.
 */
@SpringBootTest
@Transactional
class ShiftCurrentTests extends AbstractIntegrationTest {

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity cashierA;
    private UserEntity cashierB;

    @BeforeEach
    void setUp() {
        cashierA = persistCashier();
        cashierB = persistCashier();
    }

    private UserEntity persistCashier() {
        UserEntity user = new UserEntity();
        user.setEmail("cashier-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.CASHIER);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void getCurrentOpenShift_withOpenShift_returnsIt() {
        Shift opened = shiftService.open(cashierA.getId(), new ShiftOpenInput());

        Shift current = shiftService.getCurrentOpenShift(cashierA.getId());

        assertThat(current.getId()).isEqualTo(opened.getId());
        assertThat(current.getOpenedByUserId()).isEqualTo(cashierA.getId());
        assertThat(current.getStatus()).isEqualTo(ShiftStatus.OPEN);
    }

    @Test
    void getCurrentOpenShift_withNoOpenShift_throwsNotFound() {
        assertThatThrownBy(() -> shiftService.getCurrentOpenShift(cashierA.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getCurrentOpenShift_onlyClosedShiftExists_throwsNotFound() {
        ShiftEntity closed = new ShiftEntity();
        closed.setOpenedByUserId(cashierA.getId());
        closed.setStatus(ShiftStatus.CLOSED);
        shiftRepository.saveAndFlush(closed);

        assertThatThrownBy(() -> shiftService.getCurrentOpenShift(cashierA.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getCurrentOpenShift_anotherUsersOpenShift_isNotVisible() {
        shiftService.open(cashierA.getId(), new ShiftOpenInput());

        // cashierB has no shift of their own - cashierA's open shift must not leak through.
        assertThatThrownBy(() -> shiftService.getCurrentOpenShift(cashierB.getId())).isInstanceOf(NotFoundException.class);
    }
}
