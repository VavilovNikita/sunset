package com.sunsetbeach.service;

import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.ServerTime;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.TodayShiftStatus;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * {@link AttendanceService#getServerTime()} - {@code GET /attendance/server-time} - and the
 * timezone fix in {@code ClockConfig} that it exists to make visible: with the clock fixed at
 * 02:00 Bangkok time (a moment that a UTC-zoned clock would still place on the previous calendar
 * day - see {@code ClockConfigTests}), "today" throughout {@code AttendanceService} must resolve
 * to the Bangkok date, not the UTC-shifted one.
 *
 * <p>Same fixed-{@code Clock} mechanism as {@code AttendanceServiceTodayShiftBoardTests}, pinned
 * to {@code Asia/Bangkok} specifically (rather than {@code ZoneId.systemDefault()}) since that
 * zone choice is exactly what's under test here.
 */
@SpringBootTest
class AttendanceServiceServerTimeTests extends AbstractIntegrationTest {

    private static final LocalDate BANGKOK_DATE = LocalDate.of(2027, 11, 3);
    private static final Instant FIXED_INSTANT =
            BANGKOK_DATE.atTime(2, 0).atZone(ZoneId.of("Asia/Bangkok")).toInstant();

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneId.of("Asia/Bangkok"));
        }
    }

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private RosterService rosterService;

    @Autowired
    private ShiftCodeService shiftCodeService;

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

    private final List<String> createdUserIds = new java.util.ArrayList<>();
    private final List<String> createdShiftCodeIds = new java.util.ArrayList<>();

    @AfterEach
    void cleanUp() {
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        createdUserIds.forEach(employeePatternRepository::deleteById);
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createUser() {
        UserEntity user = new UserEntity();
        user.setEmail("server-time-test-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.WAITER);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    @Test
    void getServerTime_reflectsTheInjectedClockExactly() {
        ServerTime serverTime = attendanceService.getServerTime();

        assertThat(serverTime.getZone()).isEqualTo("Asia/Bangkok");
        assertThat(serverTime.getNow().toInstant()).isEqualTo(FIXED_INSTANT);
        assertThat(serverTime.getNow().toLocalDate()).isEqualTo(BANGKOK_DATE);
    }

    @Test
    void todayShiftBoard_at2amBangkok_resolvesTodayAsTheBangkokDate_notTheUtcShiftedPreviousDay() {
        UserEntity mgr = createUser();
        ShiftCode op = createShiftCode(
                new ShiftCodeCreateInput("OP" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.OPEN_SCHEDULE, true, true, "2020-01-01")
                        .staffArea(StaffArea.MAINTENANCE),
                mgr.getId());

        UserEntity employee = createUser();
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), BANGKOK_DATE.toString(), op.getId()), mgr.getId());

        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();

        assertThat(board.stream().map(TodayShiftStatus::getEmployeeUserId)).contains(employee.getId());
    }

    private ShiftCode createShiftCode(ShiftCodeCreateInput input, String actorId) {
        ShiftCode code = shiftCodeService.create(input, actorId);
        createdShiftCodeIds.add(code.getId());
        return code;
    }
}
