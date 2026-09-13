package com.sunsetbeach.service;

import com.sunsetbeach.entity.EmployeePatternEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.EmployeePattern;
import com.sunsetbeach.model.EmployeePatternInput;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What {@link RosterService#generateMonth} reads to seed a month for each employee. */
@Service
public class EmployeePatternService {

    private final EmployeePatternRepository employeePatternRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public EmployeePatternService(
            EmployeePatternRepository employeePatternRepository,
            ShiftCodeRepository shiftCodeRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.employeePatternRepository = employeePatternRepository;
        this.shiftCodeRepository = shiftCodeRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<EmployeePattern> list() {
        List<EmployeePatternEntity> entities = employeePatternRepository.findAll();
        List<String> userIds =
                entities.stream().flatMap(e -> Stream.of(e.getEmployeeUserId(), e.getUpdatedByUserId())).distinct().toList();
        Map<String, String> emailsById = userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
        return entities.stream().map(e -> toDto(e, emailsById.get(e.getEmployeeUserId()), emailsById.get(e.getUpdatedByUserId()))).toList();
    }

    @Transactional
    public EmployeePattern set(String employeeUserId, EmployeePatternInput input, String actorUserId) {
        UserEntity employee = userRepository.findById(employeeUserId).orElseThrow(() -> new NotFoundException("Employee not found"));
        UserEntity actor = userRepository.findById(actorUserId).orElseThrow(() -> new NotFoundException("Actor not found"));
        String defaultShiftCodeId = input.getDefaultShiftCodeId().orElse(null);
        if (defaultShiftCodeId != null) {
            ShiftCodeEntity shiftCode = shiftCodeRepository.findById(defaultShiftCodeId).orElseThrow(() -> new NotFoundException("Shift code not found"));
            if (shiftCode.getStaffArea() != input.getStaffArea()) {
                throw new BadRequestException("defaultShiftCodeId belongs to a different staffArea");
            }
            if (!shiftCode.isActive()) {
                throw new BadRequestException("defaultShiftCodeId is not an active shift code");
            }
        }

        EmployeePatternEntity entity = employeePatternRepository.findById(employeeUserId).orElseGet(EmployeePatternEntity::new);
        entity.setEmployeeUserId(employeeUserId);
        entity.setStaffArea(input.getStaffArea());
        entity.setDefaultShiftCodeId(defaultShiftCodeId);
        entity.setWorkDaysPerWeek(input.getWorkDaysPerWeek());
        entity.setWeeklyDayOff(input.getWeeklyDayOff());
        entity.setUpdatedByUserId(actorUserId);

        EmployeePatternEntity saved = employeePatternRepository.saveAndFlush(entity);

        auditLogService.record(
                AuditAction.EMPLOYEE_PATTERN_CHANGED,
                AuditEntityType.USER,
                employeeUserId,
                "Roster pattern set for " + employee.getEmail() + ": " + saved.getStaffArea().getValue() + ", "
                        + saved.getWorkDaysPerWeek() + " days/week, " + saved.getWeeklyDayOff().getValue() + " off");

        return toDto(saved, employee.getEmail(), actor.getEmail());
    }

    private static EmployeePattern toDto(EmployeePatternEntity e, String employeeEmail, String updatedByEmail) {
        EmployeePattern dto = new EmployeePattern(
                e.getEmployeeUserId(), employeeEmail, e.getStaffArea(), e.getWorkDaysPerWeek(), e.getWeeklyDayOff(),
                updatedByEmail, com.sunsetbeach.mapper.TimestampFormat.toUtc(e.getUpdatedAt()));
        if (e.getDefaultShiftCodeId() != null) {
            dto.defaultShiftCodeId(e.getDefaultShiftCodeId());
        }
        return dto;
    }
}
