package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AttendanceDaySummary;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.AttendancePunchCreateInput;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.PunchSource;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private AttendanceDeviceRepository attendanceDeviceRepository;

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
    private final List<String> createdDeviceIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        attendancePunchRepository.deleteAll(attendancePunchRepository.findAll().stream().filter(p -> createdUserIds.contains(p.getEmployeeUserId())).toList());
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        createdUserIds.forEach(employeePatternRepository::deleteById);
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
        // ingestDevicePunch runs REQUIRES_NEW (see its own javadoc), so these punches and the
        // employees/devices behind them are never covered by a test transaction's own rollback -
        // deleted here explicitly, same as everything else in this class.
        attendanceDeviceRepository.deleteAllById(createdDeviceIds);
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

    /** An employee the (fake, not-yet-built) terminal knows how to attribute a punch to. */
    private UserEntity createEnrolledUser(int enrollmentNumber) {
        UserEntity user = createNoLoginUser();
        user.setEnrollmentNumber(enrollmentNumber);
        return userRepository.saveAndFlush(user);
    }

    private AttendanceDeviceEntity createDevice() {
        AttendanceDeviceEntity device = new AttendanceDeviceEntity();
        device.setName("Test Terminal " + UUID.randomUUID());
        device.setSerial("SN-" + UUID.randomUUID());
        device.setAddress("192.168.1.99");
        device.setTimezone("Asia/Bangkok");
        AttendanceDeviceEntity saved = attendanceDeviceRepository.saveAndFlush(device);
        createdDeviceIds.add(saved.getId());
        return saved;
    }

    private static int uniqueEnrollmentNumber() {
        return Math.abs(UUID.randomUUID().hashCode()) % 1_000_000 + 1;
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
                new ShiftCodeCreateInput("C" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.MORNING, true, true, "2020-01-01")
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
                new ShiftCodeCreateInput("C" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.MORNING, true, true, "2020-01-01")
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
                new ShiftCodeCreateInput("OP" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.OPEN_SCHEDULE, true, true, "2020-01-01").staffArea(StaffArea.MAINTENANCE), mgr.getId());
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

    @Test
    void ingestDevicePunch_sameRecordTwice_landsOnce() {
        AttendanceDeviceEntity device = createDevice();
        int enrollmentNumber = uniqueEnrollmentNumber();
        UserEntity employee = createEnrolledUser(enrollmentNumber);
        LocalDate date = LocalDate.of(2027, 7, 1);
        LocalDateTime deviceTimestamp = date.atTime(9, 0);

        DeviceIngestResult first = attendanceService.ingestDevicePunch(device, enrollmentNumber, deviceTimestamp, PunchDirection.IN);
        // Same device, same enrollment number, same timestamp - exactly what a re-send after a
        // network drop, a restart, or a re-read of the same window looks like.
        DeviceIngestResult second = attendanceService.ingestDevicePunch(device, enrollmentNumber, deviceTimestamp, PunchDirection.IN);

        assertThat(first).isEqualTo(DeviceIngestResult.INGESTED);
        assertThat(second).isEqualTo(DeviceIngestResult.DUPLICATE);
        assertThat(attendanceService.list(employee.getId(), date, date)).hasSize(1);
    }

    @Test
    void ingestDevicePunch_unknownEnrollmentNumber_isNotIngested() {
        AttendanceDeviceEntity device = createDevice();
        int unknownNumber = uniqueEnrollmentNumber();

        DeviceIngestResult result = attendanceService.ingestDevicePunch(device, unknownNumber, LocalDate.of(2027, 7, 2).atTime(9, 0), PunchDirection.IN);

        assertThat(result).isEqualTo(DeviceIngestResult.UNKNOWN_ENROLLMENT_NUMBER);
        assertThat(attendancePunchRepository.findAll().stream().noneMatch(p -> unknownNumber == Objects.requireNonNullElse(p.getEnrollmentNumber(), -1)))
                .isTrue();
    }

    @Test
    void ingestDevicePunch_writesScannerSourceAndDeviceAttribution_noRecorder() {
        AttendanceDeviceEntity device = createDevice();
        int enrollmentNumber = uniqueEnrollmentNumber();
        UserEntity employee = createEnrolledUser(enrollmentNumber);
        LocalDate date = LocalDate.of(2027, 7, 3);

        attendanceService.ingestDevicePunch(device, enrollmentNumber, date.atTime(9, 0), PunchDirection.IN);

        AttendancePunchEntity saved = attendancePunchRepository
                .findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employee.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .get(0);
        assertThat(saved.getSource()).isEqualTo(PunchSource.SCANNER);
        assertThat(saved.getRecordedByUserId()).isNull();
        assertThat(saved.getDeviceId()).isEqualTo(device.getId());
        assertThat(saved.getEnrollmentNumber()).isEqualTo(enrollmentNumber);
    }

    /**
     * A realistic month's worth of device-shaped data fed straight into the existing pairing
     * logic ({@link AttendanceService#summary}), unchanged by this round - split shifts really do
     * produce four punches a day, a missed clock-out really does happen, the device really does
     * resend, and a shift really does cross midnight. Each of those is checked against the
     * behaviour V64's own migration comment already commits to: pairing is same-day-only and
     * positional, an odd count is left honestly "incomplete" rather than guessed at, and nothing
     * here reconciles a pair across the midnight boundary - that boundary is two independently
     * incomplete days, by design, not a gap this test is discovering.
     */
    @Test
    void summary_aMonthOfRealisticDevicePunches_incompleteStateBehavesAsDesigned() {
        AttendanceDeviceEntity device = createDevice();
        int enrollmentNumber = uniqueEnrollmentNumber();
        UserEntity employee = createEnrolledUser(enrollmentNumber);

        // An ordinary single-shift day: complete, 540 worked minutes.
        LocalDate singleShiftDay = LocalDate.of(2027, 8, 2);
        attendanceService.ingestDevicePunch(device, enrollmentNumber, singleShiftDay.atTime(9, 0), PunchDirection.IN);
        attendanceService.ingestDevicePunch(device, enrollmentNumber, singleShiftDay.atTime(18, 0), PunchDirection.OUT);

        // A split shift: four punches, still complete, same 540 total (240 + 300).
        LocalDate splitShiftDay = LocalDate.of(2027, 8, 3);
        attendanceService.ingestDevicePunch(device, enrollmentNumber, splitShiftDay.atTime(9, 0), PunchDirection.IN);
        attendanceService.ingestDevicePunch(device, enrollmentNumber, splitShiftDay.atTime(13, 0), PunchDirection.OUT);
        attendanceService.ingestDevicePunch(device, enrollmentNumber, splitShiftDay.atTime(16, 0), PunchDirection.IN);
        attendanceService.ingestDevicePunch(device, enrollmentNumber, splitShiftDay.atTime(21, 0), PunchDirection.OUT);

        // A missed clock-out, re-sent by the device exactly as a real retry would - the duplicate
        // must not turn one missed punch into a falsely "complete" (even-count) day.
        LocalDate missedPunchDay = LocalDate.of(2027, 8, 4);
        LocalDateTime missedPunchTimestamp = missedPunchDay.atTime(9, 0);
        DeviceIngestResult firstIngest = attendanceService.ingestDevicePunch(device, enrollmentNumber, missedPunchTimestamp, PunchDirection.IN);
        DeviceIngestResult duplicateIngest = attendanceService.ingestDevicePunch(device, enrollmentNumber, missedPunchTimestamp, PunchDirection.IN);

        // A shift crossing midnight: IN late one night, OUT after midnight the next day.
        LocalDate midnightInDay = LocalDate.of(2027, 8, 5);
        LocalDate midnightOutDay = LocalDate.of(2027, 8, 6);
        attendanceService.ingestDevicePunch(device, enrollmentNumber, midnightInDay.atTime(23, 30), PunchDirection.IN);
        attendanceService.ingestDevicePunch(device, enrollmentNumber, midnightOutDay.atTime(0, 45), PunchDirection.OUT);

        assertThat(firstIngest).isEqualTo(DeviceIngestResult.INGESTED);
        assertThat(duplicateIngest).isEqualTo(DeviceIngestResult.DUPLICATE);

        List<AttendanceDaySummary> augustSummaries = attendanceService.summary(employee.getId(), 2027, 8);

        AttendanceDaySummary single = dayOf(augustSummaries, singleShiftDay);
        assertThat(single.getIncomplete()).isFalse();
        assertThat(single.getWorkedMinutes().get()).isEqualTo(540);

        AttendanceDaySummary split = dayOf(augustSummaries, splitShiftDay);
        assertThat(split.getPunches()).hasSize(4);
        assertThat(split.getIncomplete()).isFalse();
        assertThat(split.getWorkedMinutes().get()).isEqualTo(540);

        AttendanceDaySummary missed = dayOf(augustSummaries, missedPunchDay);
        assertThat(missed.getPunches()).hasSize(1);
        assertThat(missed.getIncomplete()).isTrue();
        assertThat(missed.getWorkedMinutes().isPresent()).isFalse();

        // The midnight crossing: two independently incomplete days, not one reconciled interval -
        // this is the existing, documented design (see V64's own comment), not a bug this test
        // exists to catch.
        AttendanceDaySummary midnightIn = dayOf(augustSummaries, midnightInDay);
        assertThat(midnightIn.getPunches()).hasSize(1);
        assertThat(midnightIn.getIncomplete()).isTrue();
        assertThat(midnightIn.getWorkedMinutes().isPresent()).isFalse();

        AttendanceDaySummary midnightOut = dayOf(augustSummaries, midnightOutDay);
        assertThat(midnightOut.getPunches()).hasSize(1);
        assertThat(midnightOut.getIncomplete()).isTrue();
        assertThat(midnightOut.getWorkedMinutes().isPresent()).isFalse();
    }

    private static AttendanceDaySummary dayOf(List<AttendanceDaySummary> summaries, LocalDate date) {
        return summaries.stream().filter(s -> s.getDate().equals(date.toString())).findFirst().orElseThrow();
    }
}
