package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.EmployeePatternInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntry;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.RosterMonth;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.ShiftCodeVersionInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.StaffAreaCoverageRuleInput;
import com.sunsetbeach.model.Weekday;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.StaffAreaCoverageRuleRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@code POST /shift-codes/{id}/versions} - editing a code as a new version effective today. The
 * case that prompted it: PH was set up with countsAsWorked=false, so coverage never counted it;
 * after editing it, days from today on read the new terms and earlier days keep the old ones.
 * "Today" is a fixed {@link Clock}, never the wall clock.
 */
@SpringBootTest
class ShiftCodeVersionTests extends AbstractIntegrationTest {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");
    private static final String TODAY = "2031-06-15";

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(LocalDateTime.parse(TODAY + "T10:00:00").atZone(HOTEL_ZONE).toInstant(), HOTEL_ZONE);
        }
    }

    @Autowired private ShiftCodeService shiftCodeService;
    @Autowired private RosterService rosterService;
    @Autowired private EmployeePatternService employeePatternService;
    @Autowired private StaffAreaCoverageRuleService staffAreaCoverageRuleService;
    @Autowired private RosterEntryRepository rosterEntryRepository;
    @Autowired private ShiftCodeRepository shiftCodeRepository;
    @Autowired private EmployeePatternRepository employeePatternRepository;
    @Autowired private StaffAreaCoverageRuleRepository staffAreaCoverageRuleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdCodes = new ArrayList<>();
    private UserEntity manager;

    @AfterEach
    void cleanUp() {
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        createdUserIds.forEach(id -> employeePatternRepository.findById(id).ifPresent(employeePatternRepository::delete));
        staffAreaCoverageRuleRepository.deleteAll();
        // Every version of every code this test defined, including the ones createVersion added.
        shiftCodeRepository.deleteAll(shiftCodeRepository.findAll().stream().filter(c -> createdCodes.contains(c.getCode())).toList());
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createUser(Role role, StaffArea area) {
        UserEntity user = new UserEntity();
        user.setEmail("shift-code-version-test-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(role);
        user.setActive(true);
        user.setStaffArea(area);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private UserEntity manager() {
        if (manager == null) manager = createUser(Role.MANAGER, null);
        return manager;
    }

    private ShiftCode createPhLikeCode() {
        String code = "P" + UUID.randomUUID().toString().substring(0, 6);
        createdCodes.add(code);
        return shiftCodeService.create(new ShiftCodeCreateInput(code, ShiftCodeKind.ABSENCE, false, true, "2020-01-01"), manager().getId());
    }

    private static ShiftCodeVersionInput openScheduleWorked() {
        return new ShiftCodeVersionInput(ShiftCodeKind.OPEN_SCHEDULE, true, true);
    }

    private String entryCodeId(String employeeUserId, String date) {
        return rosterEntryRepository.findByEmployeeUserIdAndDate(employeeUserId, java.time.LocalDate.parse(date)).orElseThrow().getShiftCodeId();
    }

    @Test
    void editingPhToCountAsWorked_coversDaysFromToday_earlierDaysKeepOldTerms() {
        ShiftCode ph = createPhLikeCode();
        UserEntity joy = createUser(Role.WAITER, StaffArea.FRONT_OFFICE);
        staffAreaCoverageRuleService.set(StaffArea.FRONT_OFFICE, new StaffAreaCoverageRuleInput(1), manager().getId());
        rosterService.createEntry(new RosterEntryCreateInput(joy.getId(), "2031-06-14", ph.getId()), manager().getId());
        rosterService.createEntry(new RosterEntryCreateInput(joy.getId(), TODAY, ph.getId()), manager().getId());
        rosterService.createEntry(new RosterEntryCreateInput(joy.getId(), "2031-06-20", ph.getId()), manager().getId());

        ShiftCode edited = shiftCodeService.createVersion(ph.getId(), openScheduleWorked().displayColor("#3e4200"), manager().getId());

        assertThat(edited.getId()).isNotEqualTo(ph.getId());
        assertThat(edited.getCode()).isEqualTo(ph.getCode());
        assertThat(edited.getEffectiveFrom()).isEqualTo(TODAY);
        assertThat(edited.getCountsAsWorked()).isTrue();
        assertThat(edited.getKind().get()).isEqualTo(ShiftCodeKind.OPEN_SCHEDULE);
        assertThat(edited.getDisplayColor().get()).isEqualTo("#3e4200");
        assertThat(shiftCodeRepository.findById(ph.getId()).orElseThrow().isActive()).isFalse();

        assertThat(entryCodeId(joy.getId(), "2031-06-14")).isEqualTo(ph.getId());
        assertThat(entryCodeId(joy.getId(), TODAY)).isEqualTo(edited.getId());
        assertThat(entryCodeId(joy.getId(), "2031-06-20")).isEqualTo(edited.getId());

        RosterEntry added = rosterService.createEntry(new RosterEntryCreateInput(joy.getId(), "2031-06-22", edited.getId()), manager().getId());
        assertThat(added.getShiftCode().getCountsAsWorked()).isTrue();

        RosterMonth june = rosterService.getMonth(2031, 6);
        assertThat(june.getCoverageWarnings())
                .anyMatch(w -> w.getStaffArea() == StaffArea.FRONT_OFFICE && w.getDate().equals("2031-06-14") && w.getWorkingCount() == 0)
                .noneMatch(w -> w.getStaffArea() == StaffArea.FRONT_OFFICE && w.getDate().equals(TODAY))
                .noneMatch(w -> w.getStaffArea() == StaffArea.FRONT_OFFICE && w.getDate().equals("2031-06-20"))
                .noneMatch(w -> w.getStaffArea() == StaffArea.FRONT_OFFICE && w.getDate().equals("2031-06-22"));
    }

    @Test
    void editingAgainTheSameDay_amendsTodaysVersion() {
        ShiftCode ph = createPhLikeCode();
        ShiftCode first = shiftCodeService.createVersion(ph.getId(), openScheduleWorked(), manager().getId());

        ShiftCode second = shiftCodeService.createVersion(first.getId(), new ShiftCodeVersionInput(ShiftCodeKind.OPEN_SCHEDULE, true, false), manager().getId());

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getIsPaid()).isFalse();
        assertThat(shiftCodeRepository.findAll().stream().filter(c -> c.getCode().equals(ph.getCode()))).hasSize(2);
    }

    @Test
    void editingAgainTheSameDay_whenTodaysVersionIsUsedOnAnEarlierDay_isRefused() {
        ShiftCode ph = createPhLikeCode();
        UserEntity joy = createUser(Role.WAITER, StaffArea.FRONT_OFFICE);
        ShiftCode first = shiftCodeService.createVersion(ph.getId(), openScheduleWorked(), manager().getId());
        rosterService.createEntry(new RosterEntryCreateInput(joy.getId(), "2031-06-01", first.getId()), manager().getId());

        assertThatThrownBy(() -> shiftCodeService.createVersion(first.getId(), openScheduleWorked(), manager().getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void editingARetiredVersion_isRefused() {
        ShiftCode ph = createPhLikeCode();
        shiftCodeService.createVersion(ph.getId(), openScheduleWorked(), manager().getId());

        assertThatThrownBy(() -> shiftCodeService.createVersion(ph.getId(), openScheduleWorked(), manager().getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void absenceThatCountsAsWorked_isRefused_andNothingChanges() {
        ShiftCode ph = createPhLikeCode();

        assertThatThrownBy(() -> shiftCodeService.createVersion(ph.getId(), new ShiftCodeVersionInput(ShiftCodeKind.ABSENCE, true, true), manager().getId()))
                .isInstanceOf(BadRequestException.class);
        assertThat(shiftCodeRepository.findById(ph.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void employeePatternsFollowTheNewVersion() {
        String code = "K" + UUID.randomUUID().toString().substring(0, 6);
        createdCodes.add(code);
        ShiftCode kitchen = shiftCodeService.create(
                new ShiftCodeCreateInput(code, ShiftCodeKind.MORNING, true, true, "2020-01-01").staffArea(StaffArea.KITCHEN).startTime1("07:00").endTime1("16:00"),
                manager().getId());
        UserEntity cook = createUser(Role.WAITER, StaffArea.KITCHEN);
        employeePatternService.set(cook.getId(), new EmployeePatternInput(StaffArea.KITCHEN, 6, Weekday.MONDAY).defaultShiftCodeId(kitchen.getId()), manager().getId());

        ShiftCode edited = shiftCodeService.createVersion(
                kitchen.getId(), new ShiftCodeVersionInput(ShiftCodeKind.MORNING, true, true).startTime1("08:00").endTime1("17:00"), manager().getId());

        assertThat(edited.getStaffArea().get()).isEqualTo(StaffArea.KITCHEN);
        assertThat(edited.getStartTime1().get()).isEqualTo("08:00");
        assertThat(employeePatternRepository.findById(cook.getId()).orElseThrow().getDefaultShiftCodeId()).isEqualTo(edited.getId());
    }
}
