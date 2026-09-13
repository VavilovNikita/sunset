package com.sunsetbeach.service;

import com.sunsetbeach.entity.StaffAreaCoverageRuleEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.StaffAreaCoverageRule;
import com.sunsetbeach.model.StaffAreaCoverageRuleInput;
import com.sunsetbeach.repository.StaffAreaCoverageRuleRepository;
import com.sunsetbeach.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A minimum headcount per {@link StaffArea}. The source spreadsheet's own daily totals are
 * hotel-wide, not per area, so nothing today actually warns "Sunday has nobody in the kitchen" -
 * this is the rule {@link RosterService#getMonth} checks every {@code (staffArea, date)}
 * against. Warn, never block: a real staffing gap is a fact about the world, not something the
 * software gets to veto by refusing to save the schedule, same reasoning
 * {@code RoomUnitService#createBlock} already documents.
 */
@Service
public class StaffAreaCoverageRuleService {

    private final StaffAreaCoverageRuleRepository staffAreaCoverageRuleRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public StaffAreaCoverageRuleService(
            StaffAreaCoverageRuleRepository staffAreaCoverageRuleRepository, UserRepository userRepository, AuditLogService auditLogService) {
        this.staffAreaCoverageRuleRepository = staffAreaCoverageRuleRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<StaffAreaCoverageRule> list() {
        List<StaffAreaCoverageRuleEntity> entities = staffAreaCoverageRuleRepository.findAll();
        Map<String, String> emails = userRepository
                .findAllById(entities.stream().map(StaffAreaCoverageRuleEntity::getUpdatedByUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
        return entities.stream().map(e -> toDto(e, emails.get(e.getUpdatedByUserId()))).toList();
    }

    @Transactional
    public StaffAreaCoverageRule set(StaffArea staffArea, StaffAreaCoverageRuleInput input, String actorUserId) {
        UserEntity actor = userRepository.findById(actorUserId).orElseThrow();
        StaffAreaCoverageRuleEntity entity = staffAreaCoverageRuleRepository.findById(staffArea).orElseGet(StaffAreaCoverageRuleEntity::new);
        entity.setStaffArea(staffArea);
        entity.setMinimumWorking(input.getMinimumWorking());
        entity.setUpdatedByUserId(actorUserId);
        StaffAreaCoverageRuleEntity saved = staffAreaCoverageRuleRepository.saveAndFlush(entity);

        auditLogService.record(
                AuditAction.STAFF_AREA_COVERAGE_RULE_CHANGED,
                AuditEntityType.STAFF_AREA_COVERAGE_RULE,
                staffArea.name(),
                "Minimum working for " + staffArea.getValue() + " set to " + saved.getMinimumWorking());

        return toDto(saved, actor.getEmail());
    }

    private static StaffAreaCoverageRule toDto(StaffAreaCoverageRuleEntity e, String updatedByEmail) {
        return new StaffAreaCoverageRule(
                e.getStaffArea(), e.getMinimumWorking(), updatedByEmail, com.sunsetbeach.mapper.TimestampFormat.toUtc(e.getUpdatedAt()));
    }
}
