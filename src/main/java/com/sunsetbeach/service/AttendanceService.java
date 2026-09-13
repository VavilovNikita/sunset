package com.sunsetbeach.service;

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
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
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

    public AttendanceService(
            AttendancePunchRepository attendancePunchRepository,
            RosterEntryRepository rosterEntryRepository,
            ShiftCodeRepository shiftCodeRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.attendancePunchRepository = attendancePunchRepository;
        this.rosterEntryRepository = rosterEntryRepository;
        this.shiftCodeRepository = shiftCodeRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AttendancePunch> list(String employeeUserId, LocalDate from, LocalDate to) {
        UserEntity employee = userRepository.findById(employeeUserId).orElseThrow(() -> new NotFoundException("Employee not found"));
        List<AttendancePunchEntity> entities =
                attendancePunchRepository.findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employeeUserId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<String, String> recorderEmails = resolveEmails(entities.stream().map(AttendancePunchEntity::getRecordedByUserId).filter(java.util.Objects::nonNull).distinct().toList());
        return entities.stream().map(e -> toDto(e, employee.getEmail(), recorderEmails.get(e.getRecordedByUserId()))).toList();
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
                (closesAnIncompleteDay ? "Corrected " : "Recorded ") + employee.getEmail() + "'s " + saved.getDirection().getValue() + " punch at "
                        + punchAt.format(TIME_FORMAT) + " on " + day);

        return toDto(saved, employee.getEmail(), actor.getEmail());
    }

    /**
     * Planned vs. actual, one day per row. {@code shiftCode} is null on a day off (no
     * {@code RosterEntry}) - distinct from {@code OP}, which has a shift code but an empty
     * {@code plannedIntervals}, so a genuinely open day never shows a fabricated zero planned.
     */
    @Transactional(readOnly = true)
    public List<AttendanceDaySummary> summary(String employeeUserId, int year, int month) {
        userRepository.findById(employeeUserId).orElseThrow(() -> new NotFoundException("Employee not found"));
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
                    .map(p -> toDto(p, null, recorderEmails.get(p.getRecordedByUserId())))
                    .toList();

            boolean incomplete = dayPunches.size() % 2 != 0;
            // Null (not present), not zero, when there is no complete IN/OUT pair yet - a lone
            // opening punch has nothing to total, not a total that happens to be 0. A day with a
            // trailing unpaired punch (3, 5, ...) still totals whatever pairs did complete,
            // alongside incomplete=true - the two facts aren't mutually exclusive.
            Integer workedMinutes = dayPunches.size() < 2 ? null : sumWorkedMinutes(dayPunches);

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

    /** Pairs consecutive IN/OUT punches; an unpaired trailing IN (an incomplete day) contributes nothing rather than a guess. */
    private static int sumWorkedMinutes(List<AttendancePunchEntity> dayPunches) {
        int minutes = 0;
        for (int i = 0; i + 1 < dayPunches.size(); i += 2) {
            AttendancePunchEntity in = dayPunches.get(i);
            AttendancePunchEntity out = dayPunches.get(i + 1);
            if (in.getDirection() == PunchDirection.IN && out.getDirection() == PunchDirection.OUT) {
                minutes += (int) java.time.Duration.between(in.getPunchAt(), out.getPunchAt()).toMinutes();
            }
        }
        return minutes;
    }

    private Map<String, String> resolveEmails(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
    }

    private static AttendancePunch toDto(AttendancePunchEntity e, String employeeEmail, String recordedByEmail) {
        AttendancePunch dto = new AttendancePunch(
                e.getId(), e.getEmployeeUserId(), employeeEmail, TimestampFormat.toUtc(e.getPunchAt()), e.getDirection(), e.getSource(),
                TimestampFormat.toUtc(e.getCreatedAt()));
        if (recordedByEmail != null) {
            dto.recordedByEmail(recordedByEmail);
        }
        if (e.getNote() != null) {
            dto.note(e.getNote());
        }
        return dto;
    }
}
