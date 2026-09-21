package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AttendancePunchCreateInput;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.Role;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
 * {@link AttendanceService#getTodayShiftBoard} - "who's on shift right now", {@code
 * GET /attendance/today}. See {@code TodayShiftState}'s own openapi.yaml description for the
 * state rules this exercises.
 *
 * <p>Time is fixed, not real, same reasoning and mechanism as {@code SpaMapServiceTests}' own
 * {@code FixedClockConfig}: every state here (SCHEDULED vs. ARRIVING_SOON vs. LATE vs. MISSED) is
 * a comparison against "now", so a test built off {@code LocalTime.now()} would depend on what
 * wall-clock time the suite happens to run at. {@code FIXED_TIME} (10:00) is deliberately mid-day
 * so every interval used below, from 06:00 to 21:00, stays on {@code FIXED_DATE} with no midnight
 * wraparound to reason about.
 *
 * <p>Not {@code @Transactional}: {@code getTodayShiftBoard} reads across every employee's {@code
 * RosterEntry} for "today" in the shared Testcontainers database, so each test filters the
 * returned board down to its own {@code createdUserIds} rather than asserting on its size -
 * exactly the same defensive filtering {@code dayOf} in {@code AttendanceServiceTests} uses for a
 * different reason (multiple summary rows per call). Cleanup mirrors that class's own
 * {@code @AfterEach} exactly.
 */
@SpringBootTest
class AttendanceServiceTodayShiftBoardTests extends AbstractIntegrationTest {

    private static final LocalDate FIXED_DATE = LocalDate.of(2027, 9, 14);
    private static final LocalTime FIXED_TIME = LocalTime.of(10, 0);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            Instant fixedInstant = LocalDateTime.of(FIXED_DATE, FIXED_TIME).atZone(ZoneId.systemDefault()).toInstant();
            return Clock.fixed(fixedInstant, ZoneId.systemDefault());
        }
    }

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

    private UserEntity createUser() {
        UserEntity user = new UserEntity();
        user.setEmail("today-board-test-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.WAITER);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private ShiftCode createShiftCode(ShiftCodeCreateInput input, String actorId) {
        ShiftCode code = shiftCodeService.create(input, actorId);
        createdShiftCodeIds.add(code.getId());
        return code;
    }

    private void assignEntry(UserEntity employee, ShiftCode code, String actorId) {
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), FIXED_DATE.toString(), code.getId()), actorId);
    }

    private static OffsetDateTime at(int hour, int minute) {
        return FIXED_DATE.atTime(hour, minute).atOffset(ZoneOffset.UTC);
    }

    private void punch(UserEntity employee, int hour, int minute, PunchDirection direction, String actorId) {
        attendanceService.recordPunch(new AttendancePunchCreateInput(employee.getId(), at(hour, minute), direction), actorId);
    }

    /** Finds this employee's row on today's board - never absent, unless the test itself asserts it should be. */
    private static TodayShiftStatus statusOf(List<TodayShiftStatus> board, UserEntity employee) {
        return board.stream().filter(s -> s.getEmployeeUserId().equals(employee.getId())).findFirst()
                .orElseThrow(() -> new AssertionError(employee.getName() + " is missing from today's board"));
    }

    @Test
    void openSchedule_threeReachableStates() {
        UserEntity mgr = createUser();
        ShiftCode op = createShiftCode(
                new ShiftCodeCreateInput("OP" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.OPEN_SCHEDULE, true, true, "2020-01-01")
                        .staffArea(StaffArea.MAINTENANCE),
                mgr.getId());

        UserEntity notYetArrived = createUser();
        assignEntry(notYetArrived, op, mgr.getId());

        UserEntity onShift = createUser();
        assignEntry(onShift, op, mgr.getId());
        punch(onShift, 8, 0, PunchDirection.IN, mgr.getId());

        UserEntity finished = createUser();
        assignEntry(finished, op, mgr.getId());
        punch(finished, 7, 0, PunchDirection.IN, mgr.getId());
        punch(finished, 9, 0, PunchDirection.OUT, mgr.getId());

        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();

        TodayShiftStatus notYetArrivedStatus = statusOf(board, notYetArrived);
        assertThat(notYetArrivedStatus.getState()).isEqualTo(TodayShiftState.NOT_YET_ARRIVED);
        assertThat(notYetArrivedStatus.getReferenceTime().isPresent()).isFalse();

        TodayShiftStatus onShiftStatus = statusOf(board, onShift);
        assertThat(onShiftStatus.getState()).isEqualTo(TodayShiftState.ON_SHIFT);
        assertThat(onShiftStatus.getReferenceTime().get()).isEqualTo(at(8, 0));

        TodayShiftStatus finishedStatus = statusOf(board, finished);
        assertThat(finishedStatus.getState()).isEqualTo(TodayShiftState.FINISHED);
        assertThat(finishedStatus.getReferenceTime().get()).isEqualTo(at(9, 0));
    }

    /** A single-interval code ("9"-shaped), one employee per reachable state, none of them arrived yet except two. */
    @Test
    void singleInterval_everyReachableState() {
        UserEntity mgr = createUser();

        UserEntity scheduled = createUser();
        assignEntry(scheduled, createShiftCode(morningCode("12:00", "20:00"), mgr.getId()), mgr.getId());

        UserEntity arrivingSoon = createUser();
        assignEntry(arrivingSoon, createShiftCode(morningCode("10:30", "18:30"), mgr.getId()), mgr.getId());

        UserEntity late = createUser();
        assignEntry(late, createShiftCode(morningCode("09:30", "17:30"), mgr.getId()), mgr.getId());

        UserEntity missed = createUser();
        assignEntry(missed, createShiftCode(morningCode("07:00", "09:00"), mgr.getId()), mgr.getId());

        UserEntity onShift = createUser();
        assignEntry(onShift, createShiftCode(morningCode("09:00", "17:00"), mgr.getId()), mgr.getId());
        punch(onShift, 9, 5, PunchDirection.IN, mgr.getId());

        UserEntity finished = createUser();
        assignEntry(finished, createShiftCode(morningCode("07:00", "09:00"), mgr.getId()), mgr.getId());
        punch(finished, 7, 0, PunchDirection.IN, mgr.getId());
        punch(finished, 9, 0, PunchDirection.OUT, mgr.getId());

        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();

        TodayShiftStatus scheduledStatus = statusOf(board, scheduled);
        assertThat(scheduledStatus.getState()).isEqualTo(TodayShiftState.SCHEDULED);
        assertThat(scheduledStatus.getReferenceTime().get()).isEqualTo(at(12, 0));

        TodayShiftStatus arrivingSoonStatus = statusOf(board, arrivingSoon);
        assertThat(arrivingSoonStatus.getState()).isEqualTo(TodayShiftState.ARRIVING_SOON);
        assertThat(arrivingSoonStatus.getReferenceTime().get()).isEqualTo(at(10, 30));

        TodayShiftStatus lateStatus = statusOf(board, late);
        assertThat(lateStatus.getState()).isEqualTo(TodayShiftState.LATE);
        assertThat(lateStatus.getReferenceTime().get()).isEqualTo(at(9, 30));

        TodayShiftStatus missedStatus = statusOf(board, missed);
        assertThat(missedStatus.getState()).isEqualTo(TodayShiftState.MISSED);
        assertThat(missedStatus.getReferenceTime().get()).isEqualTo(at(7, 0));

        TodayShiftStatus onShiftStatus = statusOf(board, onShift);
        assertThat(onShiftStatus.getState()).isEqualTo(TodayShiftState.ON_SHIFT);
        assertThat(onShiftStatus.getReferenceTime().get()).isEqualTo(at(9, 5));

        TodayShiftStatus finishedStatus = statusOf(board, finished);
        assertThat(finishedStatus.getState()).isEqualTo(TodayShiftState.FINISHED);
        assertThat(finishedStatus.getReferenceTime().get()).isEqualTo(at(9, 0));
    }

    /** Interval 1 already worked, interval 2 well outside its own upcoming-window - not SCHEDULED, not FINISHED. */
    @Test
    void splitShift_betweenIntervals_isBetweenShiftsNotScheduled() {
        UserEntity mgr = createUser();
        ShiftCode split = createShiftCode(
                new ShiftCodeCreateInput("9S" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.SPLIT, true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT)
                        .startTime1("06:00")
                        .endTime1("09:00")
                        .startTime2("15:00")
                        .endTime2("21:00"),
                mgr.getId());

        UserEntity employee = createUser();
        assignEntry(employee, split, mgr.getId());
        punch(employee, 6, 0, PunchDirection.IN, mgr.getId());
        punch(employee, 9, 0, PunchDirection.OUT, mgr.getId());

        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();

        TodayShiftStatus status = statusOf(board, employee);
        assertThat(status.getState()).isEqualTo(TodayShiftState.BETWEEN_SHIFTS);
        assertThat(status.getReferenceTime().get()).isEqualTo(at(15, 0));
    }

    /** An ABSENCE-kind entry (PH) is not shift-relevant duty - not on this board at all. */
    @Test
    void absenceEntry_isNotOnTheBoard() {
        UserEntity mgr = createUser();
        ShiftCode ph = createShiftCode(
                new ShiftCodeCreateInput("PH" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.ABSENCE, false, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT),
                mgr.getId());
        UserEntity employee = createUser();
        assignEntry(employee, ph, mgr.getId());

        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();

        assertThat(board.stream().anyMatch(s -> s.getEmployeeUserId().equals(employee.getId()))).isFalse();
    }

    /** No RosterEntry today at all - same "absence of a row" convention, same result as ABSENCE. */
    @Test
    void noEntryToday_isNotOnTheBoard() {
        UserEntity employee = createUser();

        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();

        assertThat(board.stream().anyMatch(s -> s.getEmployeeUserId().equals(employee.getId()))).isFalse();
    }

    private static ShiftCodeCreateInput morningCode(String start, String end) {
        return new ShiftCodeCreateInput("C" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.MORNING, true, true, "2020-01-01")
                .staffArea(StaffArea.RESTAURANT)
                .startTime1(start)
                .endTime1(end);
    }
}
