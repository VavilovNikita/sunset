package com.sunsetbeach.service;

import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shift codes are data, not an enum - each one carries its own department, its own interval(s),
 * and whether it counts as worked and/or is paid, matching exactly how the source spreadsheet's
 * legend behaves (the same code meaning different hours in a different area, and a code's own
 * hours drifting between one month's legend and the next). See V57__staff_area_and_shift_code.sql
 * for why a row here is never updated once any {@code RosterEntry} references it - "editing"
 * hours is always {@link #create} again, which retires the previous version for new use without
 * touching what already happened under it.
 */
@Service
public class ShiftCodeService {

    private final ShiftCodeRepository shiftCodeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ShiftCodeService(ShiftCodeRepository shiftCodeRepository, UserRepository userRepository, AuditLogService auditLogService) {
        this.shiftCodeRepository = shiftCodeRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * With {@code staffArea} given, this is a *resolved* view for that one area - every
     * area-scoped code for it, plus every shared code whose {@code code} string isn't also
     * defined for this area (the area-scoped version wins and the shared one is left out, never
     * both - see {@code ShiftCode}'s own openapi.yaml description). Without {@code staffArea},
     * every active row is returned exactly as stored, area-scoped and shared alike, for the
     * shift-code management screen itself, where seeing both rows of a collision is the point.
     */
    @Transactional(readOnly = true)
    public List<ShiftCode> list(StaffArea staffArea) {
        List<ShiftCodeEntity> entities = staffArea != null ? resolveForArea(staffArea) : shiftCodeRepository.findByActiveTrue();
        Map<String, String> emailsById = resolveCreatorEmails(entities);
        return entities.stream().map(e -> toDto(e, emailsById.get(e.getCreatedByUserId()))).toList();
    }

    private List<ShiftCodeEntity> resolveForArea(StaffArea staffArea) {
        List<ShiftCodeEntity> areaScoped = shiftCodeRepository.findByStaffAreaAndActiveTrue(staffArea);
        List<ShiftCodeEntity> shared = shiftCodeRepository.findByStaffAreaIsNullAndActiveTrue();
        Set<String> areaScopedCodes = areaScoped.stream().map(ShiftCodeEntity::getCode).collect(Collectors.toSet());
        return java.util.stream.Stream.concat(areaScoped.stream(), shared.stream().filter(s -> !areaScopedCodes.contains(s.getCode()))).toList();
    }

    @Transactional
    public ShiftCode create(ShiftCodeCreateInput input, String actorUserId) {
        LocalTime start1 = parseTime(input.getStartTime1().orElse(null));
        LocalTime end1 = parseTime(input.getEndTime1().orElse(null));
        LocalTime start2 = parseTime(input.getStartTime2().orElse(null));
        LocalTime end2 = parseTime(input.getEndTime2().orElse(null));
        validateIntervals(start1, end1, start2, end2);

        // Retires the current version of this exact (staffArea, code) pair, if one exists - the
        // new row takes over for anything created from here on, every RosterEntry already
        // pointing at the old one keeps meaning what it meant.
        shiftCodeRepository.findByStaffAreaAndCodeAndActiveTrue(input.getStaffArea(), input.getCode()).ifPresent(existing -> {
            existing.setActive(false);
            shiftCodeRepository.save(existing);
        });

        ShiftCodeEntity entity = new ShiftCodeEntity();
        entity.setStaffArea(input.getStaffArea());
        entity.setCode(input.getCode());
        entity.setStartTime1(start1);
        entity.setEndTime1(end1);
        entity.setStartTime2(start2);
        entity.setEndTime2(end2);
        entity.setCountsAsWorked(input.getCountsAsWorked());
        entity.setPaid(input.getIsPaid());
        entity.setEffectiveFrom(LocalDate.parse(input.getEffectiveFrom()));
        entity.setCreatedByUserId(actorUserId);

        ShiftCodeEntity saved = shiftCodeRepository.saveAndFlush(entity);

        String creatorEmail = userRepository.findById(actorUserId).map(UserEntity::getEmail).orElse(null);
        String areaDescription = saved.getStaffArea() != null ? saved.getStaffArea().getValue() : "shared (every area)";
        auditLogService.record(
                AuditAction.SHIFT_CODE_CREATED,
                AuditEntityType.SHIFT_CODE,
                saved.getId(),
                "Defined " + areaDescription + " code \"" + saved.getCode() + "\", effective " + saved.getEffectiveFrom());

        return toDto(saved, creatorEmail);
    }

    private static LocalTime parseTime(String value) {
        return value != null ? LocalTime.parse(value) : null;
    }

    private static void validateIntervals(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if ((start1 == null) != (end1 == null)) {
            throw new BadRequestException("startTime1 and endTime1 must both be set or both be null");
        }
        if ((start2 == null) != (end2 == null)) {
            throw new BadRequestException("startTime2 and endTime2 must both be set or both be null");
        }
        if (start2 != null && start1 == null) {
            throw new BadRequestException("A second interval needs a first one");
        }
        if (start1 != null && !end1.isAfter(start1)) {
            throw new BadRequestException("endTime1 must be after startTime1");
        }
        if (start2 != null && !end2.isAfter(start2)) {
            throw new BadRequestException("endTime2 must be after startTime2");
        }
    }

    private Map<String, String> resolveCreatorEmails(List<ShiftCodeEntity> entities) {
        List<String> ids = entities.stream().map(ShiftCodeEntity::getCreatedByUserId).distinct().toList();
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
    }

    /** Package-private so RosterService/EmployeePatternService can render a ShiftCode already loaded, without a second query. */
    static ShiftCode toDto(ShiftCodeEntity e, String createdByEmail) {
        ShiftCode dto = new ShiftCode(
                e.getId(), e.getCode(), e.isCountsAsWorked(), e.isPaid(), e.getEffectiveFrom().toString(), e.isActive(),
                createdByEmail, com.sunsetbeach.mapper.TimestampFormat.toUtc(e.getCreatedAt()));
        // Left undefined (not "present but null") when shared - same convention as the interval
        // fields below: JsonNullable.of(null) would mean "explicitly null" (isPresent() true),
        // which is a different JSON shape than omitting the key entirely.
        if (e.getStaffArea() != null) dto.staffArea(e.getStaffArea());
        if (e.getStartTime1() != null) dto.setStartTime1(org.openapitools.jackson.nullable.JsonNullable.of(e.getStartTime1().toString().substring(0, 5)));
        if (e.getEndTime1() != null) dto.setEndTime1(org.openapitools.jackson.nullable.JsonNullable.of(e.getEndTime1().toString().substring(0, 5)));
        if (e.getStartTime2() != null) dto.setStartTime2(org.openapitools.jackson.nullable.JsonNullable.of(e.getStartTime2().toString().substring(0, 5)));
        if (e.getEndTime2() != null) dto.setEndTime2(org.openapitools.jackson.nullable.JsonNullable.of(e.getEndTime2().toString().substring(0, 5)));
        return dto;
    }
}
