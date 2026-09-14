package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.EmployeePattern;
import com.sunsetbeach.model.EmployeePatternInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.Weekday;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class EmployeePatternServiceTests extends AbstractIntegrationTest {

    @Autowired
    private EmployeePatternService employeePatternService;

    @Autowired
    private ShiftCodeService shiftCodeService;

    @Autowired
    private EmployeePatternRepository employeePatternRepository;

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdShiftCodeIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdUserIds.forEach(employeePatternRepository::deleteById);
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("employee-pattern-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(role);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    @Test
    void set_createsThenReplaces() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput("P" + UUID.randomUUID().toString().substring(0, 4), true, true, "2026-01-01")
                        .staffArea(StaffArea.RESTAURANT)
                        .startTime1("09:00")
                        .endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(code.getId());

        EmployeePattern created = employeePatternService.set(
                employee.getId(), new EmployeePatternInput(StaffArea.RESTAURANT, 6, Weekday.SUNDAY).defaultShiftCodeId(code.getId()), manager.getId());
        assertThat(created.getWorkDaysPerWeek()).isEqualTo(6);
        assertThat(created.getWeeklyDayOff()).isEqualTo(Weekday.SUNDAY);
        assertThat(created.getDefaultShiftCodeId().get()).isEqualTo(code.getId());

        EmployeePattern replaced =
                employeePatternService.set(employee.getId(), new EmployeePatternInput(StaffArea.RESTAURANT, 5, Weekday.MONDAY), manager.getId());
        assertThat(replaced.getWorkDaysPerWeek()).isEqualTo(5);
        assertThat(replaced.getWeeklyDayOff()).isEqualTo(Weekday.MONDAY);
        // No default code this time - a real, nullable case (several rows in the source
        // spreadsheet had none), not an oversight.
        assertThat(replaced.getDefaultShiftCodeId().isPresent()).isFalse();
        assertThat(employeePatternService.list()).hasSize(1);
    }

    @Test
    void set_defaultShiftCodeFromWrongArea_isRejected() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode kitchenCode = shiftCodeService.create(
                new ShiftCodeCreateInput("K" + UUID.randomUUID().toString().substring(0, 4), true, true, "2026-01-01")
                        .staffArea(StaffArea.KITCHEN)
                        .startTime1("12:00")
                        .endTime1("21:00"),
                manager.getId());
        createdShiftCodeIds.add(kitchenCode.getId());

        assertThatThrownBy(() -> employeePatternService.set(
                        employee.getId(),
                        new EmployeePatternInput(StaffArea.RESTAURANT, 6, Weekday.SUNDAY).defaultShiftCodeId(kitchenCode.getId()),
                        manager.getId()))
                .isInstanceOf(BadRequestException.class);
    }
}
