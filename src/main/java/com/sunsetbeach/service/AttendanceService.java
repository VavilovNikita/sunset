package com.sunsetbeach.service;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AttendanceDaySummary;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.AttendancePunchCreateInput;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.PunchSource;
import com.sunsetbeach.model.ShiftInterval;
import com.sunsetbeach.model.TodayShiftState;
import com.sunsetbeach.model.TodayShiftStatus;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A raw punch stream, not paired sessions - see {@code AttendancePunch}'s own description for
 * why a split shift's four punches need no special handling on the write side, and why a missed
 * punch is left as an honestly "incomplete" day rather than guessed at, closed only by recording
 * another punch through {@link #recordPunch} with a note.
 */
@Service
public class AttendanceService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendancePunchRepository attendancePunchRepository;
    private final RosterEntryRepository rosterEntryRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final Duration upcomingWindow;

    public AttendanceService(
            AttendancePunchRepository attendancePunchRepository,
            RosterEntryRepository rosterEntryRepository,
            ShiftCodeRepository shiftCodeRepository,
            UserRepository userRepository,
            AuditLogService auditLogService,
            Clock clock,
            @Value("${app.attendance.upcoming-window-minutes:60}") long upcomingWindowMinutes) {
        this.attendancePunchRepository = attendancePunchRepository;
        this.rosterEntryRepository = rosterEntryRepository;
        this.shiftCodeRepository = shiftCodeRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.upcomingWindow = Duration.ofMinutes(upcomingWindowMinutes);
    }

    @Transactional(readOnly = true)
    public List<AttendancePunch> list(String employeeUserId, LocalDate from, LocalDate to) {
        UserEntity employee = userRepository.findById(employeeUserId).orElseThrow(() -> new NotFoundException("Employee not found"));
        List<AttendancePunchEntity> entities =
                attendancePunchRepository.findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employeeUserId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<String, String> recorderEmails = resolveEmails(entities.stream().map(AttendancePunchEntity::getRecordedByUserId).filter(java.util.Objects::nonNull).distinct().toList());
        return entities.stream().map(e -> toDto(e, employee, recorderEmails.get(e.getRecordedByUserId()))).toList();
    }

    /**
     * Records a punch by hand (MANAGER+ only in v1 - see this endpoint's own openapi
     * description for why employee self-service is a separate decision). Also the mechanism
     * behind closing an incomplete day: a correction is just another punch, with a note, using
     * {@code ATTENDANCE_PUNCH_CORRECTED} instead of {@code ATTENDANCE_PUNCH_RECORDED} for its
     * own audit entry whenever this punch resolves a day that was previously incomplete (an odd
     * count before this one is added).
     */
    @Transactional
    public AttendancePunch recordPunch(AttendancePunchCreateInput input, String actorUserId) {
        UserEntity employee = userRepository.findById(input.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        UserEntity actor = userRepository.findById(actorUserId).orElseThrow(() -> new NotFoundException("Actor not found"));

        LocalDateTime punchAt = input.getPunchAt().toLocalDateTime();
        LocalDate day = punchAt.toLocalDate();
        long priorCountToday = attendancePunchRepository
                .findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employee.getId(), day.atStartOfDay(), day.plusDays(1).atStartOfDay())
                .size();
        boolean closesAnIncompleteDay = priorCountToday % 2 == 1;

        AttendancePunchEntity entity = new AttendancePunchEntity();
        entity.setEmployeeUserId(employee.getId());
        entity.setPunchAt(punchAt);
        entity.setDirection(input.getDirection());
        entity.setSource(PunchSource.MANUAL);
        entity.setRecordedByUserId(actorUserId);
        entity.setNote(input.getNote().orElse(null));
        AttendancePunchEntity saved = attendancePunchRepository.saveAndFlush(entity);

        auditLogService.record(
                closesAnIncompleteDay ? AuditAction.ATTENDANCE_PUNCH_CORRECTED : AuditAction.ATTENDANCE_PUNCH_RECORDED,
                AuditEntityType.ATTENDANCE_PUNCH,
                saved.getId(),
                (closesAnIncompleteDay ? "Corrected " : "Recorded ") + employee.getName() + "'s " + saved.getDirection().getValue() + " punch at "
                        + punchAt.format(TIME_FORMAT) + " on " + day);

        return toDto(saved, employee, actor.getEmail());
    }

    /**
     * Writes one {@code SCANNER}-sourced punch - the write path a device poll (not yet built)
     * calls once per raw record read off a terminal. Idempotent by construction, not by
     * convention: {@code (deviceId, enrollmentNumber, punchAt)} is a unique triple (see V75), and
     * this method checks for that triple before writing rather than writing and catching the
     * violation - a caught {@code DataIntegrityViolationException} still leaves the surrounding
     * Spring transaction marked rollback-only (JPA's own flush failure poisons it before a
     * {@code catch} block ever runs, regardless of {@code REQUIRES_NEW}; the transaction, not just
     * the Java exception, has to be dealt with), so for the *expected* case - a device resending a
     * record after a network drop, a restart, or a re-read of the same window - checking first is
     * what actually lets this method return normally instead of throwing
     * {@code UnexpectedRollbackException} on every re-send. The unique index stays as a real,
     * database-enforced backstop for whatever this check can't see (two overlapping polls, say);
     * that genuinely-unexpected case is allowed to throw and fail the one record, same as any
     * other DB hiccup.
     *
     * <p>{@code REQUIRES_NEW}, so one record's own transaction can never be the reason another
     * record's write doesn't happen - a poll ingests many records in one pass, and every one of
     * them has to stand alone for that to hold, the same isolation reason
     * {@code AuditLogService.record} already documents for itself.
     *
     * <p>An enrollment number the device reports that matches no {@code User} is not ingested at
     * all - see {@code User.enrollmentNumber}'s own description: without one, nothing here can
     * say who this punch belongs to, and a punch attributed to nobody is worse than a punch not
     * recorded yet, which can still be backfilled once the number is assigned.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeviceIngestResult ingestDevicePunch(AttendanceDeviceEntity device, int enrollmentNumber, LocalDateTime deviceTimestamp, PunchDirection direction) {
        UserEntity employee = userRepository.findByEnrollmentNumber(enrollmentNumber).orElse(null);
        if (employee == null) {
            return DeviceIngestResult.UNKNOWN_ENROLLMENT_NUMBER;
        }
        if (attendancePunchRepository.existsByDeviceIdAndEnrollmentNumberAndPunchAt(device.getId(), enrollmentNumber, deviceTimestamp)) {
            return DeviceIngestResult.DUPLICATE;
        }

        AttendancePunchEntity entity = new AttendancePunchEntity();
        entity.setEmployeeUserId(employee.getId());
        entity.setPunchAt(deviceTimestamp);
        entity.setDirection(direction);
        entity.setSource(PunchSource.SCANNER);
        entity.setDeviceId(device.getId());
        entity.setEnrollmentNumber(enrollmentNumber);
        attendancePunchRepository.saveAndFlush(entity);
        return DeviceIngestResult.INGESTED;
    }

    /**
     * Planned vs. actual, one day per row. {@code shiftCode} is null on a day off (no
     * {@code RosterEntry}) - distinct from {@code OP}, which has a shift code but an empty
     * {@code plannedIntervals}, so a genuinely open day never shows a fabricated zero planned.
     */
    @Transactional(readOnly = true)
    public List<AttendanceDaySummary> summary(String employeeUserId, int year, int month) {
        UserEntity employee = userRepository.findById(employeeUserId).orElseThrow(() -> new NotFoundException("Employee not found"));
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<RosterEntryEntity> entries = rosterEntryRepository.findByEmployeeUserIdAndDateBetween(employeeUserId, from, to);
        Map<LocalDate, RosterEntryEntity> entryByDate = entries.stream().collect(Collectors.toMap(RosterEntryEntity::getDate, e -> e));
        Map<String, ShiftCodeEntity> shiftCodesById =
                shiftCodeRepository.findAllById(entries.stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList()).stream()
                        .collect(Collectors.toMap(ShiftCodeEntity::getId, s -> s));

        List<AttendancePunchEntity> punches =
                attendancePunchRepository.findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employeeUserId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<LocalDate, List<AttendancePunchEntity>> punchesByDate = punches.stream().collect(Collectors.groupingBy(p -> p.getPunchAt().toLocalDate()));
        Map<String, String> recorderEmails = resolveEmails(punches.stream().map(AttendancePunchEntity::getRecordedByUserId).filter(java.util.Objects::nonNull).distinct().toList());

        List<AttendanceDaySummary> summaries = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            RosterEntryEntity entry = entryByDate.get(date);
            ShiftCodeEntity shiftCode = entry != null ? shiftCodesById.get(entry.getShiftCodeId()) : null;

            List<ShiftInterval> plannedIntervals = new ArrayList<>();
            if (shiftCode != null) {
                if (shiftCode.getStartTime1() != null) {
                    plannedIntervals.add(new ShiftInterval(shiftCode.getStartTime1().format(TIME_FORMAT), shiftCode.getEndTime1().format(TIME_FORMAT)));
                }
                if (shiftCode.getStartTime2() != null) {
                    plannedIntervals.add(new ShiftInterval(shiftCode.getStartTime2().format(TIME_FORMAT), shiftCode.getEndTime2().format(TIME_FORMAT)));
                }
            }

            List<AttendancePunchEntity> dayPunches = punchesByDate.getOrDefault(date, List.of());
            List<AttendancePunch> punchDtos = dayPunches.stream()
                    .map(p -> toDto(p, employee, recorderEmails.get(p.getRecordedByUserId())))
                    .toList();

            boolean incomplete = dayPunches.size() % 2 != 0;
            // Null (not present), not zero, when there is no complete IN/OUT pair yet - a lone
            // opening punch has nothing to total, not a total that happens to be 0. A day with a
            // trailing unpaired punch (3, 5, ...) still totals whatever pairs did complete,
            // alongside incomplete=true - the two facts aren't mutually exclusive.
            Integer workedMinutes = dayPunches.size() < 2 ? null : AttendancePunchPairing.sumWorkedMinutes(dayPunches);

            AttendanceDaySummary daySummary = new AttendanceDaySummary(date.toString(), plannedIntervals, punchDtos, incomplete);
            if (shiftCode != null) {
                daySummary.shiftCode(ShiftCodeService.toDto(shiftCode, null));
            }
            if (workedMinutes != null) {
                daySummary.workedMinutes(workedMinutes);
            }
            summaries.add(daySummary);
        }
        return summaries;
    }

    /**
     * "Who's on shift right now" - {@code GET /attendance/today}. One row per employee with a
     * {@code countsAsWorked} {@code RosterEntry} today; an {@code ABSENCE}-kind entry ({@code PH})
     * and an employee with no entry today are both simply absent, the same "a day off is the
     * absence of a row" convention {@code RosterEntry} itself already uses. See {@code
     * TodayShiftState}'s own openapi.yaml description for exactly how each state is derived - this
     * method (and {@link #toTodayShiftStatus}) is the one place that logic lives, matching today's
     * punches to the shift code's own interval(s) positionally, the same rule {@code
     * RosterExportService#writeLateAndLeftEarlyRows} already uses for its "Late & anomalies" sheet.
     */
    @Transactional(readOnly = true)
    public List<TodayShiftStatus> getTodayShiftBoard() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        List<RosterEntryEntity> entries = rosterEntryRepository.findByDateBetween(today, today);
        Map<String, ShiftCodeEntity> shiftCodesById =
                shiftCodeRepository.findAllById(entries.stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList()).stream()
                        .collect(Collectors.toMap(ShiftCodeEntity::getId, s -> s));

        List<RosterEntryEntity> workingEntries = entries.stream().filter(e -> shiftCodesById.get(e.getShiftCodeId()).isCountsAsWorked()).toList();
        if (workingEntries.isEmpty()) {
            return List.of();
        }

        Map<String, UserEntity> employees = userRepository
                .findAllById(workingEntries.stream().map(RosterEntryEntity::getEmployeeUserId).toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<AttendancePunchEntity> punches = attendancePunchRepository
                .findByPunchAtBetweenOrderByEmployeeUserIdAscPunchAtAsc(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        Map<String, List<AttendancePunchEntity>> punchesByEmployee = punches.stream().collect(Collectors.groupingBy(AttendancePunchEntity::getEmployeeUserId));

        List<TodayShiftStatus> board = new ArrayList<>();
        for (RosterEntryEntity entry : workingEntries) {
            UserEntity employee = employees.get(entry.getEmployeeUserId());
            ShiftCodeEntity shiftCode = shiftCodesById.get(entry.getShiftCodeId());
            List<AttendancePunchEntity> dayPunches = punchesByEmployee.getOrDefault(employee.getId(), List.of());
            board.add(toTodayShiftStatus(employee, shiftCode, dayPunches, today, now));
        }
        return board;
    }

    /**
     * One employee's live state - see {@code TodayShiftState}'s own openapi.yaml description for
     * the rules this implements. {@code OPEN_SCHEDULE} (no intervals) only ever reaches three of
     * the eight states; a fixed-interval code (1 or 2) walks to the first interval not yet
     * satisfied by a complete pair, matched positionally exactly like {@code
     * RosterExportService#writeLateAndLeftEarlyRows}.
     */
    private TodayShiftStatus toTodayShiftStatus(
            UserEntity employee, ShiftCodeEntity shiftCode, List<AttendancePunchEntity> dayPunches, LocalDate today, LocalDateTime now) {
        List<LocalTime[]> intervals = new ArrayList<>();
        if (shiftCode.getStartTime1() != null) intervals.add(new LocalTime[] {shiftCode.getStartTime1(), shiftCode.getEndTime1()});
        if (shiftCode.getStartTime2() != null) intervals.add(new LocalTime[] {shiftCode.getStartTime2(), shiftCode.getEndTime2()});

        boolean hasTrailingUnmatchedIn =
                dayPunches.size() % 2 == 1 && dayPunches.get(dayPunches.size() - 1).getDirection() == PunchDirection.IN;
        int matched = Math.min(AttendancePunchPairing.pairs(dayPunches).size(), intervals.size());

        TodayShiftState state;
        LocalDateTime referenceTime;

        if (intervals.isEmpty()) {
            // OPEN_SCHEDULE (OP) - no fixed interval, so only these three states are reachable.
            if (dayPunches.isEmpty()) {
                state = TodayShiftState.NOT_YET_ARRIVED;
                referenceTime = null;
            } else if (hasTrailingUnmatchedIn) {
                state = TodayShiftState.ON_SHIFT;
                referenceTime = dayPunches.get(dayPunches.size() - 1).getPunchAt();
            } else {
                state = TodayShiftState.FINISHED;
                referenceTime = dayPunches.get(dayPunches.size() - 1).getPunchAt();
            }
        } else if (matched == intervals.size() && !hasTrailingUnmatchedIn) {
            state = TodayShiftState.FINISHED;
            referenceTime = dayPunches.get(dayPunches.size() - 1).getPunchAt();
        } else if (hasTrailingUnmatchedIn) {
            state = TodayShiftState.ON_SHIFT;
            referenceTime = dayPunches.get(dayPunches.size() - 1).getPunchAt();
        } else {
            LocalTime[] currentInterval = intervals.get(matched);
            LocalDateTime intervalStart = today.atTime(currentInterval[0]);
            LocalDateTime intervalEnd = today.atTime(currentInterval[1]);
            LocalDateTime upcomingFrom = intervalStart.minus(upcomingWindow);

            if (matched == 1 && now.isBefore(upcomingFrom)) {
                // Split shift only: interval 1 is already done, interval 2 isn't due for a while -
                // distinct from SCHEDULED so a returning employee never reads as not-yet-arrived.
                state = TodayShiftState.BETWEEN_SHIFTS;
            } else if (now.isBefore(upcomingFrom)) {
                state = TodayShiftState.SCHEDULED;
            } else if (now.isBefore(intervalStart)) {
                state = TodayShiftState.ARRIVING_SOON;
            } else if (!now.isAfter(intervalEnd)) {
                state = TodayShiftState.LATE;
            } else {
                state = TodayShiftState.MISSED;
            }
            referenceTime = intervalStart;
        }

        TodayShiftStatus dto = new TodayShiftStatus(employee.getId(), employee.getName(), ShiftCodeService.toDto(shiftCode, null), state);
        if (employee.getStaffArea() != null) dto.staffArea(employee.getStaffArea());
        if (referenceTime != null) dto.referenceTime(TimestampFormat.toUtc(referenceTime));
        return dto;
    }

    private Map<String, String> resolveEmails(List<String> userIds) {
        if (userIds.isEmpty()) {
            // Not Map.of(): every caller looks this map up by a SCANNER punch's own
            // recordedByUserId, which is null by design (see AttendancePunch's own description) -
            // Map.of()'s get(null) throws NPE (it validates its key argument like every other
            // Map.of() method), where Collections.emptyMap() - like the populated HashMap the
            // branch below already returns - just answers null for a key that isn't there.
            return java.util.Collections.emptyMap();
        }
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
    }

    private static AttendancePunch toDto(AttendancePunchEntity e, UserEntity employee, String recordedByEmail) {
        AttendancePunch dto = new AttendancePunch(
                e.getId(), e.getEmployeeUserId(), employee.getName(), TimestampFormat.toUtc(e.getPunchAt()), e.getDirection(), e.getSource(),
                TimestampFormat.toUtc(e.getCreatedAt()));
        dto.setEmployeeEmail(employee.getEmail());
        if (recordedByEmail != null) {
            dto.recordedByEmail(recordedByEmail);
        }
        if (e.getNote() != null) {
            dto.note(e.getNote());
        }
        return dto;
    }
}
