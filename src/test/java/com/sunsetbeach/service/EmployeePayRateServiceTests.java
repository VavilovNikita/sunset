package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.EmployeePayRateEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.EmployeePayRate;
import com.sunsetbeach.model.EmployeePayRateCreateInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.EmployeePayRateRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Never edited, only superseded - see EmployeePayRateService's own javadoc, including why this
 * versioned rate history stays in the codebase even though nothing calls {@link
 * EmployeePayRateService#rateAsOf} today.
 */
@SpringBootTest
class EmployeePayRateServiceTests extends AbstractIntegrationTest {

    @Autowired
    private EmployeePayRateService employeePayRateService;

    @Autowired
    private EmployeePayRateRepository employeePayRateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        employeePayRateRepository.deleteAll(employeePayRateRepository.findAll().stream().filter(r -> createdUserIds.contains(r.getEmployeeUserId())).toList());
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("pay-rate-test-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(role);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    @Test
    void create_secondVersion_bothKeptOrderedByEffectiveFrom() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);

        employeePayRateService.create(new EmployeePayRateCreateInput(employee.getId(), "700.00", "2027-07-01"), mgr.getId());
        employeePayRateService.create(new EmployeePayRateCreateInput(employee.getId(), "800.00", "2027-08-01"), mgr.getId());

        List<EmployeePayRate> history = employeePayRateService.list(employee.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getDailyRate()).isEqualTo("700.00");
        assertThat(history.get(1).getDailyRate()).isEqualTo("800.00");
    }

    /** A raise mid-range must not retroactively reprice days before its effective date. */
    @Test
    void rateAsOf_aRaiseMidRange_eachDatePricedAtItsOwnRate() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        employeePayRateService.create(new EmployeePayRateCreateInput(employee.getId(), "700.00", "2020-01-01"), mgr.getId());
        employeePayRateService.create(new EmployeePayRateCreateInput(employee.getId(), "900.00", "2027-08-15"), mgr.getId());

        List<EmployeePayRateEntity> history = employeePayRateRepository.findByEmployeeUserIdOrderByEffectiveFrom(employee.getId());

        assertThat(EmployeePayRateService.rateAsOf(history, LocalDate.of(2027, 8, 14))).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(EmployeePayRateService.rateAsOf(history, LocalDate.of(2027, 8, 15))).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(EmployeePayRateService.rateAsOf(history, LocalDate.of(2027, 8, 16))).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    void rateAsOf_beforeAnyVersion_isZero() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        employeePayRateService.create(new EmployeePayRateCreateInput(employee.getId(), "700.00", "2027-08-01"), mgr.getId());

        List<EmployeePayRateEntity> history = employeePayRateRepository.findByEmployeeUserIdOrderByEffectiveFrom(employee.getId());

        assertThat(EmployeePayRateService.rateAsOf(history, LocalDate.of(2027, 7, 31))).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
