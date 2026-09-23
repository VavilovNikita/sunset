package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.TodayShiftState;
import com.sunsetbeach.model.TodayShiftStatus;
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
 * Regression test for the "referenceTime is ~7h off" bug: pinning the app's {@link Clock} to
 * {@code Asia/Bangkok} (see {@code ClockConfig}) means {@code LocalDate.now(clock)}/{@code
 * LocalDateTime.now(clock)} inside {@link AttendanceService#toTodayShiftStatus} return
 * Bangkok-wall-clock numbers, not UTC ones. {@link com.sunsetbeach.mapper.TimestampFormat#toUtc}
 * only labels a {@code LocalDateTime} as UTC, it never converts a zone - so a {@code
 * referenceTime} built from {@code today.atTime(shiftStart)} has to be converted to a true UTC
 * instant before that call, unlike every other {@code referenceTime} assignment in that method
 * (sourced from {@code AttendancePunchEntity.punchAt}, which is already UTC-denominated).
 *
 * <p>Unlike {@link AttendanceServiceTodayShiftBoardTests}, this class pins the fixed clock to the
 * real production zone ({@code Asia/Bangkok}) instead of {@code ZoneId.systemDefault()}
 * specifically so the assertions below only pass if the Bangkok-to-UTC conversion actually
 * happens - a {@code systemDefault()} zone of UTC (typical in CI) would make this bug invisible.
 */
@SpringBootTest
class AttendanceServiceTodayShiftBoardTimezoneTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
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
        if (createdUserId != null) {
            userRepository.deleteById(createdUserId);
        }
    }

    @Test
    void lateReferenceTime_isConvertedFromBangkokWallClockToTrueUtcInstant() {
        UserEntity mgr = new UserEntity();
        mgr.setEmail("tz-bug-mgr-" + UUID.randomUUID() + "@example.com");
        mgr.setName(mgr.getEmail());
        mgr.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        mgr.setRole(com.sunsetbeach.model.Role.MANAGER);
        mgr.setActive(true);
        mgr = userRepository.saveAndFlush(mgr);

        UserEntity employee = new UserEntity();
        employee.setEmail("tz-bug-employee-" + UUID.randomUUID() + "@example.com");
        employee.setName(employee.getEmail());
        employee.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        employee.setRole(com.sunsetbeach.model.Role.WAITER);
        employee.setActive(true);
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

        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();
        String finalEmployeeId = employee.getId();
        TodayShiftStatus status = board.stream().filter(s -> s.getEmployeeUserId().equals(finalEmployeeId)).findFirst()
                .orElseThrow(() -> new AssertionError("employee missing from today's board"));

        assertThat(status.getState()).isEqualTo(TodayShiftState.LATE);
        assertThat(status.getReferenceTime().isPresent()).isTrue();

        // 07:00 Asia/Bangkok (UTC+7) is 00:00 UTC the same day - not 07:00 UTC, which is what the
        // pre-fix bug produced by mislabeling Bangkok wall-clock numbers as already being UTC.
        OffsetDateTime expectedReferenceTime = FIXED_DATE.atTime(0, 0).atOffset(ZoneOffset.UTC);
        assertThat(status.getReferenceTime().get()).isEqualTo(expectedReferenceTime);

        // Elapsed time from shift start to "now" should be ~7h33m, not the ~33m the bug produced.
        Duration elapsed = Duration.between(status.getReferenceTime().get().toInstant(), OffsetDateTime.now(clock).toInstant());
        assertThat(elapsed).isEqualTo(Duration.ofHours(7).plusMinutes(33));
    }
}
