package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AttendanceDaySummary;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.AttendancePunchCreateInput;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A raw punch stream, not paired sessions - see AttendancePunch's own description. Pairing into
 * worked minutes, and treating an odd count as "incomplete" rather than guessing, both happen at
 * read time in {@link AttendanceService#summary}.
 */
@SpringBootTest
class AttendanceServiceTests extends AbstractIntegrationTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private RosterService rosterService;

    @Autowired
    private ShiftCodeService shiftCodeService;

    @Autowired
    private AttendancePunchRepository attendancePunchRepository;

    @Autowired
    private RosterEntryRepository rosterEntryRepository;

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private EmployeePatternRepository employeePatternRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdShiftCodeIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        attendancePunchRepository.deleteAll(attendancePunchRepository.findAll().stream().filter(p -> createdUserIds.contains(p.getEmployeeUserId())).toList());
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        createdUserIds.forEach(employeePatternRepository::deleteById);
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("attendance-test-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(role);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    /** Attendance exists specifically for staff who punch in and out but never sign in. */
    private UserEntity createNoLoginUser() {
        UserEntity user = new UserEntity();
        user.setName("No Login Employee " + UUID.randomUUID());
        user.setRole(Role.WAITER);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private static OffsetDateTime at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atOffset(ZoneOffset.UTC);
    }

    /**
     * The population this module exists for. Every read path must show the same name, and no
     * email at all - including the punches nested inside a day summary, which used to pass a
     * hardcoded null for employeeEmail regardless of whether the employee actually had one.
     */
    @Test
    void recordPunch_forNoLoginEmployee_showsNameNotEmail() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createNoLoginUser();
        LocalDate date = LocalDate.of(2027, 6, 5);

        AttendancePunch recorded =
                attendanceService.recordPunch(new AttendancePunchCreateInput(employee.getId(), at(date, 9, 0), PunchDirection.IN), mgr.getId());
        assertThat(recorded.getEmployeeName()).isEqualTo(employee.getName());
        assertThat(recorded.getEmployeeEmail()).isNull();

        List<AttendancePunch> listed = attendanceService.list(employee.getId(), date, date);
        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).getEmployeeName()).isEqualTo(employee.getName());
        assertThat(listed.get(0).getEmployeeEmail()).isNull();

        AttendanceDaySummary day = attendanceService.summary(employee.getId(), 2027, 6).stream()
                .filter(s -> s.getDate().equals(date.toString()))
                .findFirst()
                .orElseThrow();
        assertThat(day.getPunches()).hasSize(1);
        assertThat(day.getPunches().get(0).getEmployeeName()).isEqualTo(employee.getName());
        assertThat(day.getPunches().get(0).getEmployeeEmail()).isNull();
    }

    @Test
    void recordPunch_aClockInThenOut_pairsIntoWorkedMinutes() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput("C" + UUID.randomUUID().toString().substring(0, 6), true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT)
                        .startTime1("09:00")
                        .endTime1("17:00"),
                mgr.getId());
        createdShiftCodeIds.add(code.getId());
        LocalDate date = LocalDate.of(2027, 6, 1);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), code.getId()), mgr.getId());

        attendanceService.recordPunch(new AttendancePunchCreateInput(employee.getId(), at(date, 9, 0), PunchDirection.IN), mgr.getId());
        attendanceService.recordPunch(new AttendancePunchCreateInput(employee.getId(), at(date, 17, 0), PunchDirection.OUT), mgr.getId());

        List<AttendanceDaySummary> summaries = attendanceService.summary(employee.getId(), 2027, 6);
        AttendanceDaySummary day = summaries.stream().filter(s -> s.getDate().equals(date.toString())).findFirst().orElseThrow();
        assertThat(day.getIncomplete()).isFalse();
        assertThat(day.getWorkedMinutes().get()).isEqualTo(480);
        assertThat(day.getPlannedIntervals()).hasSize(1);
    }

    /** The constant case: a missed clock-out. Left honestly incomplete, not guessed at. */
    @Test
    void summary_oddPunchCount_isIncompleteUntilAnotherPunchCloses() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput("C" + UUID.randomUUID().toString().substring(0, 6), true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT)
                        .startTime1("09:00")
                        .endTime1("17:00"),
                mgr.getId());
        createdShiftCodeIds.add(code.getId());
        LocalDate date = LocalDate.of(2027, 6, 2);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), code.getId()), mgr.getId());

        attendanceService.recordPunch(new AttendancePunchCreateInput(employee.getId(), at(date, 9, 0), PunchDirection.IN), mgr.getId());

        AttendanceDaySummary incomplete = attendanceService.summary(employee.getId(), 2027, 6).stream()
                .filter(s -> s.getDate().equals(date.toString()))
                .findFirst()
                .orElseThrow();
        assertThat(incomplete.getIncomplete()).isTrue();
        assertThat(incomplete.getWorkedMinutes().isPresent()).isFalse();

        // Closed only by recording another punch, through the same endpoint, with a note.
        AttendancePunch correction = attendanceService.recordPunch(
                new AttendancePunchCreateInput(employee.getId(), at(date, 17, 0), PunchDirection.OUT).note("forgot to clock out, closed by manager"),
                mgr.getId());
        assertThat(correction.getNote().get()).isNotBlank();

        AttendanceDaySummary closed = attendanceService.summary(employee.getId(), 2027, 6).stream()
                .filter(s -> s.getDate().equals(date.toString()))
                .findFirst()
                .orElseThrow();
        assertThat(closed.getIncomplete()).isFalse();
        assertThat(closed.getWorkedMinutes().get()).isEqualTo(480);
    }

    /** OP has a shift code but zero planned intervals - never a fabricated "0 minutes planned". */
    @Test
    void summary_opDay_hasNoPlannedIntervalsNotZero() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode op = shiftCodeService.create(
                new ShiftCodeCreateInput("OP" + UUID.randomUUID().toString().substring(0, 4), true, true, "2020-01-01").staffArea(StaffArea.MAINTENANCE), mgr.getId());
        createdShiftCodeIds.add(op.getId());
        LocalDate date = LocalDate.of(2027, 6, 3);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), op.getId()), mgr.getId());

        AttendanceDaySummary day = attendanceService.summary(employee.getId(), 2027, 6).stream()
                .filter(s -> s.getDate().equals(date.toString()))
                .findFirst()
                .orElseThrow();
        assertThat(day.getShiftCode().isPresent()).isTrue();
        assertThat(day.getShiftCode().get()).isNotNull();
        assertThat(day.getPlannedIntervals()).isEmpty();
    }

    /** A day off - no RosterEntry at all - has no shiftCode at all, distinct from OP's empty-but-present one. */
    @Test
    void summary_dayOff_hasNoShiftCode() {
        UserEntity employee = createUser(Role.WAITER);
        LocalDate date = LocalDate.of(2027, 6, 4);

        AttendanceDaySummary day = attendanceService.summary(employee.getId(), 2027, 6).stream()
                .filter(s -> s.getDate().equals(date.toString()))
                .findFirst()
                .orElseThrow();
        assertThat(day.getShiftCode().isPresent()).isFalse();
        assertThat(day.getPlannedIntervals()).isEmpty();
    }
}
