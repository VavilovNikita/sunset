package com.sunsetbeach.service;

import com.sunsetbeach.entity.GymHallSettingsEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.GymHallSettings;
import com.sunsetbeach.repository.GymHallSettingsRepository;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The gym's opening hours and the automation's parameters (lead time, air-conditioner scene).
 * The slot rules the owner fixed - a 30-minute grid, 30 to 120 minutes a session, 120 minutes a
 * day per booking - are constants here, not settings.
 */
@Service
public class GymHallSettingsService {

    public static final int STEP_MINUTES = 30;
    public static final int MIN_DURATION_MINUTES = 30;
    public static final int MAX_DURATION_MINUTES = 120;
    public static final int MAX_DAILY_MINUTES = 120;

    static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final GymHallSettingsRepository repository;
    private final AuditLogService auditLogService;

    public GymHallSettingsService(GymHallSettingsRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    /** The settings row (seeded by V142; a missing one is a broken database, not a user error). */
    public GymHallSettingsEntity entity() {
        return repository.findById(GymHallSettingsEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("GymHallSettings row is missing - V142 seeds it"));
    }

    public GymHallSettings get() {
        return toDto(entity());
    }

    @Transactional
    public GymHallSettings update(GymHallSettings input) {
        LocalTime open = parse("openTime", input.getOpenTime());
        LocalTime close = parse("closeTime", input.getCloseTime());
        if (!open.isBefore(close)) {
            throw new BadRequestException("The gym must open before it closes");
        }
        GymHallSettingsEntity entity = entity();
        List<String> changes = new ArrayList<>();
        if (!entity.getOpenTime().equals(open) || !entity.getCloseTime().equals(close)) {
            changes.add("hours " + open.format(HHMM) + "-" + close.format(HHMM));
        }
        if (entity.getLeadMinutes() != input.getLeadMinutes()) {
            changes.add("lead time " + input.getLeadMinutes() + " min");
        }
        if (entity.getAcMode() != input.getAcMode() || entity.getAcTemp() != input.getAcTemp() || entity.getAcWind() != input.getAcWind()) {
            changes.add("air conditioner mode " + input.getAcMode() + ", " + input.getAcTemp() + "°, fan " + input.getAcWind());
        }
        entity.setOpenTime(open);
        entity.setCloseTime(close);
        entity.setLeadMinutes(input.getLeadMinutes());
        entity.setAcMode(input.getAcMode());
        entity.setAcTemp(input.getAcTemp());
        entity.setAcWind(input.getAcWind());
        GymHallSettingsEntity saved = repository.saveAndFlush(entity);
        if (!changes.isEmpty()) {
            auditLogService.record(
                    AuditAction.GYM_HALL_SETTINGS_UPDATED,
                    AuditEntityType.GYM_HALL_SETTINGS,
                    String.valueOf(GymHallSettingsEntity.SINGLETON_ID),
                    "Gym settings changed: " + String.join("; ", changes));
        }
        return toDto(saved);
    }

    static GymHallSettings toDto(GymHallSettingsEntity e) {
        return new GymHallSettings(
                e.getOpenTime().format(HHMM), e.getCloseTime().format(HHMM), e.getLeadMinutes(), e.getAcMode(), e.getAcTemp(), e.getAcWind());
    }

    private static LocalTime parse(String field, String value) {
        LocalTime time;
        try {
            time = LocalTime.parse(value, HHMM);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(field + " must be a time like 06:00");
        }
        if (time.getMinute() % STEP_MINUTES != 0) {
            throw new BadRequestException(field + " must be on the " + STEP_MINUTES + "-minute grid (:00 or :30)");
        }
        return time;
    }
}
