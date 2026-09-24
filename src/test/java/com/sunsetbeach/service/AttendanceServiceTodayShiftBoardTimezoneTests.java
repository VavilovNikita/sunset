package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.AttendancePunchCreateInput;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.TodayShiftState;
import com.sunsetbeach.model.TodayShiftStatus;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
 * Regression tests for the "referenceTime / punchAt is ~7h off" bug. {@code
 * AttendancePunchEntity.punchAt} is Asia/Bangkok wall-clock (the terminal's own clock, and every
 * day-boundary and shift-interval comparison in this module assumes so), as is anything built from
 * {@code today.atTime(...)} off the Bangkok-pinned {@link Clock}. {@link
 * com.sunsetbeach.mapper.TimestampFormat#toUtc} only labels a {@code LocalDateTime} as UTC, it never
 * converts a zone - so every such value must leave the API zoned through the clock ({@code +07:00}),
 * never stamped {@code Z}.
 *
 * <p>An earlier fix (21b7656) converted only the interval-derived branches, on the assumption that
 * punchAt was already UTC; the punch-derived ones (ON_SHIFT, FINISHED) stayed 7h in the future,
 * which is what froze the admin board's live "on shift (0m)" duration.
 *
 * <p>Unlike {@link AttendanceServiceTodayShiftBoardTests}, this class pins the fixed clock to the
 * real production zone ({@code Asia/Bangkok}) instead of UTC specifically so the assertions below
 * only pass if the zone is actually applied - a UTC clock would make this bug invisible.
 */
@SpringBootTest
class AttendanceServiceTodayShiftBoardTimezoneTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final ZoneOffset BANGKOK_OFFSET = ZoneOffset.ofHours(7);
    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 9, 23);
    private static final LocalTime FIXED_TIME = LocalTime.of(14, 33);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            Instant fixedInstant = LocalDateTime.of(FIXED_DATE, FIXED_TIME).atZone(BANGKOK).toInstant();
            return Clock.fixed(fixedInstant, BANGKOK);
        }
    }

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private RosterService rosterService;

    @Autowired
    private ShiftCodeService shiftCodeService;

    @Autowired
    private Clock clock;

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

    private String createdUserId;
    private String createdShiftCodeId;
    private String createdDeviceId;

    @AfterEach
    void cleanUp() {
        if (createdUserId != null) {
            attendancePunchRepository.deleteAll(attendancePunchRepository.findAll().stream().filter(p -> p.getEmployeeUserId().equals(createdUserId)).toList());
            rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> e.getEmployeeUserId().equals(createdUserId)).toList());
            employeePatternRepository.deleteById(createdUserId);
        }
        if (createdShiftCodeId != null) {
            shiftCodeRepository.deleteById(createdShiftCodeId);
        }
        if (createdDeviceId != null) {
            attendanceDeviceRepository.deleteById(createdDeviceId);
        }
        if (createdUserId != null) {
            userRepository.deleteById(createdUserId);
        }
    }

    @Test
    void lateReferenceTime_isTheRealInstantOfTheBangkokShiftStart() {
        UserEntity mgr = createManager();
        UserEntity employee = createRosteredEmployee(mgr, null);

        TodayShiftStatus status = boardStatusOf(employee);

        assertThat(status.getState()).isEqualTo(TodayShiftState.LATE);
        assertThat(status.getReferenceTime().isPresent()).isTrue();

        // 07:00 Asia/Bangkok is 00:00 UTC the same day - not 07:00 UTC, which is what mislabeling
        // Bangkok wall-clock numbers as already being UTC produced.
        OffsetDateTime referenceTime = status.getReferenceTime().get();
        assertThat(referenceTime.toInstant()).isEqualTo(FIXED_DATE.atTime(0, 0).toInstant(ZoneOffset.UTC));
        assertThat(referenceTime.getOffset()).isEqualTo(BANGKOK_OFFSET);

        // Elapsed time from shift start to "now" should be ~7h33m, not the ~33m the bug produced.
        Duration elapsed = Duration.between(referenceTime.toInstant(), OffsetDateTime.now(clock).toInstant());
        assertThat(elapsed).isEqualTo(Duration.ofHours(7).plusMinutes(33));
    }

    @Test
    void onShiftReferenceTime_fromAScannerPunch_isTheRealInstant_soTheLiveDurationCountsUp() {
        UserEntity mgr = createManager();
        int enrollmentNumber = Math.abs(UUID.randomUUID().hashCode()) % 1_000_000 + 1;
        UserEntity employee = createRosteredEmployee(mgr, enrollmentNumber);

        // The terminal reports Bangkok wall-clock digits - 14:20 on its own display, 13 minutes
        // before the fixed "now" of 14:33 Bangkok.
        attendanceService.ingestDevicePunch(createDevice(), enrollmentNumber, FIXED_DATE.atTime(14, 20), PunchDirection.IN);

        TodayShiftStatus status = boardStatusOf(employee);
        assertThat(status.getState()).isEqualTo(TodayShiftState.ON_SHIFT);
        OffsetDateTime referenceTime = status.getReferenceTime().get();
        assertThat(referenceTime).isEqualTo(FIXED_DATE.atTime(14, 20).atOffset(BANGKOK_OFFSET));
        // The board shows "since HH:mm" by slicing the string and its duration as now minus this
        // instant - both only work when the offset is real, not a Z on Bangkok digits.
        assertThat(referenceTime.toString()).startsWith("2026-09-23T14:20").endsWith("+07:00");
        assertThat(Duration.between(referenceTime.toInstant(), clock.instant())).isEqualTo(Duration.ofMinutes(13));

        AttendancePunch listed = attendanceService.list(employee.getId(), FIXED_DATE, FIXED_DATE).get(0);
        assertThat(listed.getPunchAt()).isEqualTo(FIXED_DATE.atTime(14, 20).atOffset(BANGKOK_OFFSET));
    }

    @Test
    void manualPunch_sentAsAUtcInstant_isStoredAsBangkokWallClock_andReadBackAsTheSameInstant() {
        UserEntity mgr = createManager();
        UserEntity employee = createRosteredEmployee(mgr, null);

        // What the frontend sends for 14:20 entered at the hotel: new Date(local).toISOString().
        OffsetDateTime sent = OffsetDateTime.parse("2026-09-23T07:20:00Z");
        AttendancePunch recorded =
                attendanceService.recordPunch(new AttendancePunchCreateInput(employee.getId(), sent, PunchDirection.IN), mgr.getId());

        assertThat(recorded.getPunchAt().toInstant()).isEqualTo(sent.toInstant());
        assertThat(recorded.getPunchAt().getOffset()).isEqualTo(BANGKOK_OFFSET);
        AttendancePunchEntity stored = attendancePunchRepository.findById(recorded.getId()).orElseThrow();
        assertThat(stored.getPunchAt()).isEqualTo(FIXED_DATE.atTime(14, 20));

        TodayShiftStatus status = boardStatusOf(employee);
        assertThat(status.getState()).isEqualTo(TodayShiftState.ON_SHIFT);
        assertThat(status.getReferenceTime().get().toInstant()).isEqualTo(sent.toInstant());
    }

    private UserEntity createManager() {
        UserEntity mgr = new UserEntity();
        mgr.setEmail("tz-bug-mgr-" + UUID.randomUUID() + "@example.com");
        mgr.setName(mgr.getEmail());
        mgr.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        mgr.setRole(com.sunsetbeach.model.Role.MANAGER);
        mgr.setActive(true);
        return userRepository.saveAndFlush(mgr);
    }

    /** Rostered 07:00-18:00 today - so with no punch at 14:33 they're LATE, after a 14:20 IN they're ON_SHIFT. */
    private UserEntity createRosteredEmployee(UserEntity mgr, Integer enrollmentNumber) {
        UserEntity employee = new UserEntity();
        employee.setEmail("tz-bug-employee-" + UUID.randomUUID() + "@example.com");
        employee.setName(employee.getEmail());
        employee.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        employee.setRole(com.sunsetbeach.model.Role.WAITER);
        employee.setActive(true);
        employee.setEnrollmentNumber(enrollmentNumber);
        employee = userRepository.saveAndFlush(employee);
        createdUserId = employee.getId();

        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput("TZ" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.MORNING, true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT)
                        .startTime1("07:00")
                        .endTime1("18:00"),
                mgr.getId());
        createdShiftCodeId = code.getId();

        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), FIXED_DATE.toString(), code.getId()), mgr.getId());
        return employee;
    }

    private AttendanceDeviceEntity createDevice() {
        AttendanceDeviceEntity device = new AttendanceDeviceEntity();
        device.setName("TZ Test Terminal " + UUID.randomUUID());
        device.setSerial("SN-" + UUID.randomUUID());
        device.setAddress("192.168.1.99");
        device.setTimezone("Asia/Bangkok");
        device = attendanceDeviceRepository.saveAndFlush(device);
        createdDeviceId = device.getId();
        return device;
    }

    private TodayShiftStatus boardStatusOf(UserEntity employee) {
        String employeeId = employee.getId();
        return attendanceService.getTodayShiftBoard().stream().filter(s -> s.getEmployeeUserId().equals(employeeId)).findFirst()
                .orElseThrow(() -> new AssertionError("employee missing from today's board"));
    }
}
