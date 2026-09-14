package com.sunsetbeach.service;

import com.sunsetbeach.entity.EmployeePayRateEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.EmployeePayRate;
import com.sunsetbeach.model.EmployeePayRateCreateInput;
import com.sunsetbeach.repository.EmployeePayRateRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Never edited, only superseded - the same "agreed terms are frozen" shape as {@code ShiftCode}
 * and {@code BookingSegmentNightlyRate}, so a raise partway through a month prices each day
 * against whichever rate was actually in effect that day, not whatever the field says today.
 *
 * <p><b>Deliberately unused by the roster actuals export today.</b> The hotel's accountant keeps
 * pay calculation off-system - everyone is currently on a monthly salary held in a sheet this
 * system has never seen, and there are no part-timers - so {@link RosterService#exportActualsCsv}
 * reports only days and hours now, not money (see that method's own javadoc). This service,
 * {@link #rateAsOf}, and {@code EmployeePayRateEntity} are kept exactly as built anyway: the same
 * versioned-rate machinery becomes load-bearing again the day a part-timer is hired, and rebuilding
 * it then would cost more than carrying it unused now. Do not delete this as dead code, and do not
 * wire {@link #rateAsOf} back into the export without checking first - it was pulled out
 * deliberately, not left behind by accident.
 */
@Service
public class EmployeePayRateService {

    private final EmployeePayRateRepository employeePayRateRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public EmployeePayRateService(EmployeePayRateRepository employeePayRateRepository, UserRepository userRepository, AuditLogService auditLogService) {
        this.employeePayRateRepository = employeePayRateRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<EmployeePayRate> list(String employeeUserId) {
        UserEntity employee = userRepository.findById(employeeUserId).orElseThrow(() -> new NotFoundException("Employee not found"));
        List<EmployeePayRateEntity> entities = employeePayRateRepository.findByEmployeeUserIdOrderByEffectiveFrom(employeeUserId);
        Map<String, String> emails = resolveEmails(entities.stream().map(EmployeePayRateEntity::getCreatedByUserId).distinct().toList());
        return entities.stream().map(e -> toDto(e, employee, emails.get(e.getCreatedByUserId()))).toList();
    }

    @Transactional
    public EmployeePayRate create(EmployeePayRateCreateInput input, String actorUserId) {
        UserEntity employee = userRepository.findById(input.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        UserEntity actor = userRepository.findById(actorUserId).orElseThrow(() -> new NotFoundException("Actor not found"));

        EmployeePayRateEntity entity = new EmployeePayRateEntity();
        entity.setEmployeeUserId(employee.getId());
        entity.setDailyRate(new BigDecimal(input.getDailyRate()));
        entity.setEffectiveFrom(LocalDate.parse(input.getEffectiveFrom()));
        entity.setCreatedByUserId(actorUserId);
        EmployeePayRateEntity saved = employeePayRateRepository.saveAndFlush(entity);

        auditLogService.record(
                AuditAction.EMPLOYEE_PAY_RATE_CHANGED,
                AuditEntityType.EMPLOYEE_PAY_RATE,
                saved.getId(),
                "Daily rate for " + employee.getName() + " set to " + saved.getDailyRate() + ", effective " + saved.getEffectiveFrom());

        return toDto(saved, employee, actor.getEmail());
    }

    /**
     * Resolves each employee's rate as of a given date from an already-fetched history -
     * batched by {@link RosterService}'s export, one query for every employee involved rather
     * than one per employee per day. The latest version with {@code effectiveFrom <= date} wins;
     * an employee with no rate on or before that date resolves to zero rather than throwing, so
     * a new hire's first, not-yet-priced days show honestly instead of failing the whole export.
     */
    static BigDecimal rateAsOf(List<EmployeePayRateEntity> historySortedByEffectiveFrom, LocalDate date) {
        BigDecimal rate = BigDecimal.ZERO;
        for (EmployeePayRateEntity version : historySortedByEffectiveFrom) {
            if (!version.getEffectiveFrom().isAfter(date)) {
                rate = version.getDailyRate();
            } else {
                break;
            }
        }
        return rate;
    }

    List<EmployeePayRateEntity> historyFor(Collection<String> employeeUserIds) {
        return employeePayRateRepository.findByEmployeeUserIdInOrderByEffectiveFrom(employeeUserIds);
    }

    private Map<String, String> resolveEmails(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
    }

    private static EmployeePayRate toDto(EmployeePayRateEntity e, UserEntity employee, String createdByEmail) {
        EmployeePayRate dto = new EmployeePayRate(
                e.getId(), e.getEmployeeUserId(), employee.getName(), e.getDailyRate().toString(), e.getEffectiveFrom().toString(), createdByEmail,
                TimestampFormat.toUtc(e.getCreatedAt()));
        dto.setEmployeeEmail(employee.getEmail());
        return dto;
    }
}
