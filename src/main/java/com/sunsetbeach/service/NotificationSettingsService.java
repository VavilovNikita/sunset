package com.sunsetbeach.service;

import com.sunsetbeach.entity.NotificationSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.NotificationSettings;
import com.sunsetbeach.model.NotificationSettingsUpdateInput;
import com.sunsetbeach.repository.NotificationSettingsRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET/PUT /settings/notifications} - the one row V148 seeds. ADMIN-only at the route level
 * ({@code /settings/**}). Everything the scanners and the sender decide by lives here, so no
 * threshold is a constant in code.
 */
@Service
public class NotificationSettingsService {

    private final NotificationSettingsRepository repository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final long terminalPollIntervalMs;

    public NotificationSettingsService(
            NotificationSettingsRepository repository,
            UserRepository userRepository,
            AuditLogService auditLogService,
            @Value("${app.attendance.device-poll-interval-ms:300000}") long terminalPollIntervalMs) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.terminalPollIntervalMs = terminalPollIntervalMs;
    }

    /** The terminal's poll interval rounded up to whole minutes: a punch cannot be seen sooner than this. */
    public int minLateGraceMinutes() {
        return (int) Math.max(1, (terminalPollIntervalMs + 59_999) / 60_000);
    }

    @Transactional(readOnly = true)
    public NotificationSettings get() {
        return toDto(getEntity());
    }

    /** The row always exists - V148 seeds it and nothing deletes it. */
    @Transactional(readOnly = true)
    public NotificationSettingsEntity getEntity() {
        return repository.findById(1).orElseThrow(() -> new IllegalStateException("NotificationSettings row is missing - V148 seeds it"));
    }

    @Transactional
    public NotificationSettings update(NotificationSettingsUpdateInput input) {
        int minGrace = minLateGraceMinutes();
        if (input.getLateGraceMinutes() < minGrace) {
            throw ValidationException.field(
                    "lateGraceMinutes", "Must be at least " + minGrace + " minutes - the attendance terminal is read every " + minGrace + " minutes");
        }
        if (input.getAbsenceMinutes() < input.getLateGraceMinutes()) {
            throw ValidationException.field("absenceMinutes", "Must not be shorter than the late grace (" + input.getLateGraceMinutes() + " minutes)");
        }
        LocalTime quietStart = parseTime(input.getQuietStart(), "quietStart");
        LocalTime quietEnd = parseTime(input.getQuietEnd(), "quietEnd");
        LocalTime morningTime = parseTime(input.getMorningTime(), "morningTime");
        if (input.getQuietHoursEnabled() && quietStart.equals(quietEnd)) {
            throw ValidationException.field("quietEnd", "Quiet hours can't start and end at the same time - turn them off instead");
        }
        String[] lateRecipients = validRecipients(input.getLateRecipientUserIds(), "lateRecipientUserIds");
        String[] directors = validRecipients(input.getDirectorUserIds(), "directorUserIds");

        NotificationSettingsEntity entity = getEntity();
        List<String> changes = new ArrayList<>();
        change(changes, "late alerts", entity.isLateEnabled(), input.getLateEnabled());
        change(changes, "late grace", entity.getLateGraceMinutes() + " min", input.getLateGraceMinutes() + " min");
        change(changes, "absence alerts", entity.isAbsenceEnabled(), input.getAbsenceEnabled());
        change(changes, "absence after", entity.getAbsenceMinutes() + " min", input.getAbsenceMinutes() + " min");
        change(changes, "block created alerts", entity.isBlockCreatedEnabled(), input.getBlockCreatedEnabled());
        change(changes, "block last day alerts", entity.isBlockLastDayEnabled(), input.getBlockLastDayEnabled());
        change(changes, "expired block alerts", entity.isBlockExpiredEnabled(), input.getBlockExpiredEnabled());
        change(changes, "terminal alerts", entity.isTerminalAlertsEnabled(), input.getTerminalAlertsEnabled());
        change(changes, "terminal silence", entity.getTerminalSilenceMinutes() + " min", input.getTerminalSilenceMinutes() + " min");
        change(changes, "quiet hours", entity.isQuietHoursEnabled(), input.getQuietHoursEnabled());
        change(changes, "quiet start", entity.getQuietStart(), quietStart);
        change(changes, "quiet end", entity.getQuietEnd(), quietEnd);
        change(changes, "morning time", entity.getMorningTime(), morningTime);
        change(changes, "default language", entity.getDefaultLanguage(), input.getDefaultLanguage().getValue());
        if (!Arrays.equals(entity.getLateRecipientUserIds(), lateRecipients)) {
            changes.add("late recipients " + entity.getLateRecipientUserIds().length + " -> " + lateRecipients.length);
        }
        if (!Arrays.equals(entity.getDirectorUserIds(), directors)) {
            changes.add("director recipients " + entity.getDirectorUserIds().length + " -> " + directors.length);
        }

        entity.setLateEnabled(input.getLateEnabled());
        entity.setLateGraceMinutes(input.getLateGraceMinutes());
        entity.setAbsenceEnabled(input.getAbsenceEnabled());
        entity.setAbsenceMinutes(input.getAbsenceMinutes());
        entity.setBlockCreatedEnabled(input.getBlockCreatedEnabled());
        entity.setBlockLastDayEnabled(input.getBlockLastDayEnabled());
        entity.setBlockExpiredEnabled(input.getBlockExpiredEnabled());
        entity.setTerminalAlertsEnabled(input.getTerminalAlertsEnabled());
        entity.setTerminalSilenceMinutes(input.getTerminalSilenceMinutes());
        entity.setQuietHoursEnabled(input.getQuietHoursEnabled());
        entity.setQuietStart(quietStart);
        entity.setQuietEnd(quietEnd);
        entity.setMorningTime(morningTime);
        entity.setDefaultLanguage(input.getDefaultLanguage().getValue());
        entity.setLateRecipientUserIds(lateRecipients);
        entity.setDirectorUserIds(directors);
        NotificationSettingsEntity saved = repository.saveAndFlush(entity);

        // An unchanged PUT writes nothing. The summary names what changed, never a person's details.
        if (!changes.isEmpty()) {
            auditLogService.record(
                    AuditAction.NOTIFICATION_SETTINGS_UPDATED, AuditEntityType.SETTINGS, null, "Notification settings: " + String.join("; ", changes));
        }
        return toDto(saved);
    }

    private static void change(List<String> changes, String what, Object before, Object after) {
        if (!java.util.Objects.equals(before, after)) {
            changes.add(what + " " + before + " -> " + after);
        }
    }

    private static LocalTime parseTime(String value, String field) {
        try {
            return LocalTime.parse(value);
        } catch (RuntimeException e) {
            throw ValidationException.field(field, "Must be a time like 09:00");
        }
    }

    /** Distinct, in the order given; every id must be an existing, active user. */
    private String[] validRecipients(List<String> ids, String field) {
        List<String> distinct = new ArrayList<>(new LinkedHashSet<>(ids));
        Map<String, UserEntity> users = userRepository.findAllById(distinct).stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        for (String id : distinct) {
            UserEntity user = users.get(id);
            if (user == null || !user.isActive()) {
                throw ValidationException.field(field, "Every recipient must be an existing, active user");
            }
        }
        return distinct.toArray(new String[0]);
    }

    private NotificationSettings toDto(NotificationSettingsEntity e) {
        return new NotificationSettings(
                e.isLateEnabled(),
                e.getLateGraceMinutes(),
                minLateGraceMinutes(),
                e.isAbsenceEnabled(),
                e.getAbsenceMinutes(),
                e.isBlockCreatedEnabled(),
                e.isBlockLastDayEnabled(),
                e.isBlockExpiredEnabled(),
                e.isTerminalAlertsEnabled(),
                e.getTerminalSilenceMinutes(),
                e.isQuietHoursEnabled(),
                e.getQuietStart().toString(),
                e.getQuietEnd().toString(),
                e.getMorningTime().toString(),
                NotificationLanguage.fromValue(e.getDefaultLanguage()),
                List.of(e.getLateRecipientUserIds()),
                List.of(e.getDirectorUserIds()),
                TimestampFormat.toUtc(e.getUpdatedAt()));
    }
}
