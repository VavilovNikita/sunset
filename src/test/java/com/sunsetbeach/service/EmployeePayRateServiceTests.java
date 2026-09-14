package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.EmployeePayRate;
import com.sunsetbeach.model.EmployeePayRateCreateInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.EmployeePayRateRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
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

/**
 * Never edited, only superseded - see EmployeePayRateService's own javadoc. The export test
 * (below, using this same versioning) is what proves a rate change mid-month prices each day
 * against whichever rate was actually in effect that day, not today's rate applied backward.
 */
@SpringBootTest
class EmployeePayRateServiceTests extends AbstractIntegrationTest {

    @Autowired
    private EmployeePayRateService employeePayRateService;

    @Autowired
    private RosterService rosterService;

    @Autowired
    private ShiftCodeService shiftCodeService;

    @Autowired
    private EmployeePayRateRepository employeePayRateRepository;

    @Autowired
    private RosterEntryRepository rosterEntryRepository;

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
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        employeePayRateRepository.deleteAll(employeePayRateRepository.findAll().stream().filter(r -> createdUserIds.contains(r.getEmployeeUserId())).toList());
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
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

    /** A raise in August priced against September must not retroactively reprice August. */
    @Test
    void exportActualsCsv_aRaiseMidRange_pricesEachDayAtItsOwnRate() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput(StaffArea.RESTAURANT, "C" + UUID.randomUUID().toString().substring(0, 6), true, true, "2020-01-01")
                        .startTime1("09:00")
                        .endTime1("17:00"),
                mgr.getId());
        createdShiftCodeIds.add(code.getId());
        employeePayRateService.create(new EmployeePayRateCreateInput(employee.getId(), "700.00", "2020-01-01"), mgr.getId());
        // One day in July at the old rate, one day in August at the new rate - same month's
        // export call must never see, since the export is scoped to one calendar month; instead
        // prove the boundary directly: two entries either side of a rate change within ONE month.
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-08-14", code.getId()), mgr.getId());
        employeePayRateService.create(new EmployeePayRateCreateInput(employee.getId(), "900.00", "2027-08-15"), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-08-15", code.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-08-16", code.getId()), mgr.getId());

        String csv = rosterService.exportActualsCsv(2027, 8, mgr.getId());

        assertThat(csv).contains("Gross pay (before advances and deductions)");
        // 1 day at 700 (the 14th, before the raise) + 2 days at 900 (the 15th and 16th, from the
        // raise's own effective date onward) = 2500.00, not 3 x 900 or 3 x 700.
        assertThat(csv).contains(employee.getEmail());
        assertThat(csv).contains("2500.00");
    }
}
