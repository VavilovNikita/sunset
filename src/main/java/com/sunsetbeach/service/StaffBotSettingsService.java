package com.sunsetbeach.service;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.StaffBotSettings;
import com.sunsetbeach.model.StaffBotSettingsUpdateInput;
import com.sunsetbeach.repository.StaffBotSettingsRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET/PUT /settings/staff-bot} - the one row V150 seeds, ADMIN-only through {@code /settings/**}.
 * Every threshold, time and switch of the staff bot and of shift requests lives here; none is a constant
 * in code. It is its own row rather than more columns of {@code NotificationSettings}: that one is the
 * alert scanners' (and its PUT requires every field), this is a different feature with its own defaults.
 */
@Service
public class StaffBotSettingsService {

    private final StaffBotSettingsRepository repository;
    private final AuditLogService auditLogService;

    public StaffBotSettingsService(StaffBotSettingsRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public StaffBotSettings get() {
        return toDto(getEntity());
    }

    /** The row always exists - V150 seeds it and nothing deletes it. */
    @Transactional(readOnly = true)
    public StaffBotSettingsEntity getEntity() {
        return repository.findById(1).orElseThrow(() -> new IllegalStateException("StaffBotSettings row is missing - V150 seeds it"));
    }

    @Transactional
    public StaffBotSettings update(StaffBotSettingsUpdateInput in) {
        // The spec's ranges are enforced by bean validation (400 on the field); only what a schema cannot say is here.
        LocalTime eveTime = time("eveReminderTime", in.getEveReminderTime());
        LocalTime therapistMorningTime = time("therapistMorningTime", in.getTherapistMorningTime());
        LocalTime morningSummaryTime = time("morningSummaryTime", in.getMorningSummaryTime());

        StaffBotSettingsEntity e = getEntity();
        List<String> changes = new ArrayList<>();
        change(changes, "My shifts", e.isMyShiftsEnabled(), in.getMyShiftsEnabled());
        change(changes, "My day", e.isMyDayEnabled(), in.getMyDayEnabled());
        change(changes, "My month", e.isMyMonthEnabled(), in.getMyMonthEnabled());
        change(changes, "My requests", e.isMyRequestsEnabled(), in.getMyRequestsEnabled());
        change(changes, "bot settings command", e.isBotSettingsEnabled(), in.getBotSettingsEnabled());
        change(changes, "shift requests", e.isRequestsEnabled(), in.getRequestsEnabled());
        change(changes, "move requests", e.isMoveRequestsEnabled(), in.getMoveRequestsEnabled());
        change(changes, "delete requests", e.isDeleteRequestsEnabled(), in.getDeleteRequestsEnabled());
        change(changes, "request notice hours", e.getRequestMinHoursBefore(), in.getRequestMinHoursBefore());
        change(changes, "requests for today", e.isAllowTodayRequests(), in.getAllowTodayRequests());
        change(changes, "move window days", e.getMoveWindowDays(), in.getMoveWindowDays());
        change(changes, "reason required", e.isReasonRequired(), in.getReasonRequired());
        change(changes, "My shifts days", e.getMyShiftsDays(), in.getMyShiftsDays());
        change(changes, "administrator fallback", e.isFallbackToAdmin(), in.getFallbackToAdmin());
        change(changes, "pending reminder", e.isPendingReminderEnabled(), in.getPendingReminderEnabled());
        change(changes, "pending reminder hours", e.getPendingReminderHours(), in.getPendingReminderHours());
        change(changes, "evening reminder", e.isEveReminderEnabled(), in.getEveReminderEnabled());
        change(changes, "evening reminder time", e.getEveReminderTime(), eveTime);
        change(changes, "evening reminder default", e.isEveReminderDefaultOn(), in.getEveReminderDefaultOn());
        change(changes, "roster change messages", e.isRosterChangeEnabled(), in.getRosterChangeEnabled());
        change(changes, "roster change horizon days", e.getRosterChangeHorizonDays(), in.getRosterChangeHorizonDays());
        change(changes, "roster change delay minutes", e.getRosterChangeDelayMinutes(), in.getRosterChangeDelayMinutes());
        change(changes, "engineer tasks screen", e.isEngineerTasksEnabled(), in.getEngineerTasksEnabled());
        change(changes, "housekeeper blocks screen", e.isHousekeeperBlocksEnabled(), in.getHousekeeperBlocksEnabled());
        change(changes, "therapist procedures screen", e.isTherapistScheduleEnabled(), in.getTherapistScheduleEnabled());
        change(changes, "therapist morning message", e.isTherapistMorningEnabled(), in.getTherapistMorningEnabled());
        change(changes, "therapist morning default", e.isTherapistMorningDefaultOn(), in.getTherapistMorningDefaultOn());
        change(changes, "who is on shift screen", e.isWhoIsOnShiftEnabled(), in.getWhoIsOnShiftEnabled());
        change(changes, "who is on shift for HR", e.isWhoIsOnShiftForHr(), in.getWhoIsOnShiftForHr());
        change(changes, "summary command", e.isSummaryCommandEnabled(), in.getSummaryCommandEnabled());
        change(changes, "morning summary", e.isMorningSummaryEnabled(), in.getMorningSummaryEnabled());
        change(changes, "summary block arrivals", e.isSummaryArrivals(), in.getSummaryArrivals());
        change(changes, "summary block departures", e.isSummaryDepartures(), in.getSummaryDepartures());
        change(changes, "summary block in house", e.isSummaryInHouse(), in.getSummaryInHouse());
        change(changes, "summary block occupancy", e.isSummaryOccupancy(), in.getSummaryOccupancy());
        change(changes, "summary block rooms", e.isSummaryRooms(), in.getSummaryRooms());
        change(changes, "summary block staff", e.isSummaryStaff(), in.getSummaryStaff());
        change(changes, "summary block requests", e.isSummaryRequests(), in.getSummaryRequests());
        change(changes, "housekeeper blocks days", e.getHousekeeperBlocksDays(), in.getHousekeeperBlocksDays());
        change(changes, "therapist procedures days", e.getTherapistScheduleDays(), in.getTherapistScheduleDays());
        change(changes, "therapist morning time", e.getTherapistMorningTime(), therapistMorningTime);
        change(changes, "morning summary time", e.getMorningSummaryTime(), morningSummaryTime);

        // An unchanged PUT writes nothing - not the row, not an audit entry.
        if (changes.isEmpty()) {
            return toDto(e);
        }
        e.setMyShiftsEnabled(in.getMyShiftsEnabled());
        e.setMyDayEnabled(in.getMyDayEnabled());
        e.setMyMonthEnabled(in.getMyMonthEnabled());
        e.setMyRequestsEnabled(in.getMyRequestsEnabled());
        e.setBotSettingsEnabled(in.getBotSettingsEnabled());
        e.setRequestsEnabled(in.getRequestsEnabled());
        e.setMoveRequestsEnabled(in.getMoveRequestsEnabled());
        e.setDeleteRequestsEnabled(in.getDeleteRequestsEnabled());
        e.setRequestMinHoursBefore(in.getRequestMinHoursBefore());
        e.setAllowTodayRequests(in.getAllowTodayRequests());
        e.setMoveWindowDays(in.getMoveWindowDays());
        e.setReasonRequired(in.getReasonRequired());
        e.setMyShiftsDays(in.getMyShiftsDays());
        e.setFallbackToAdmin(in.getFallbackToAdmin());
        e.setPendingReminderEnabled(in.getPendingReminderEnabled());
        e.setPendingReminderHours(in.getPendingReminderHours());
        e.setEveReminderEnabled(in.getEveReminderEnabled());
        e.setEveReminderTime(eveTime);
        e.setEveReminderDefaultOn(in.getEveReminderDefaultOn());
        e.setRosterChangeEnabled(in.getRosterChangeEnabled());
        e.setRosterChangeHorizonDays(in.getRosterChangeHorizonDays());
        e.setRosterChangeDelayMinutes(in.getRosterChangeDelayMinutes());
        e.setEngineerTasksEnabled(in.getEngineerTasksEnabled());
        e.setHousekeeperBlocksEnabled(in.getHousekeeperBlocksEnabled());
        e.setTherapistScheduleEnabled(in.getTherapistScheduleEnabled());
        e.setTherapistMorningEnabled(in.getTherapistMorningEnabled());
        e.setTherapistMorningDefaultOn(in.getTherapistMorningDefaultOn());
        e.setWhoIsOnShiftEnabled(in.getWhoIsOnShiftEnabled());
        e.setWhoIsOnShiftForHr(in.getWhoIsOnShiftForHr());
        e.setSummaryCommandEnabled(in.getSummaryCommandEnabled());
        e.setMorningSummaryEnabled(in.getMorningSummaryEnabled());
        e.setSummaryArrivals(in.getSummaryArrivals());
        e.setSummaryDepartures(in.getSummaryDepartures());
        e.setSummaryInHouse(in.getSummaryInHouse());
        e.setSummaryOccupancy(in.getSummaryOccupancy());
        e.setSummaryRooms(in.getSummaryRooms());
        e.setSummaryStaff(in.getSummaryStaff());
        e.setSummaryRequests(in.getSummaryRequests());
        e.setHousekeeperBlocksDays(in.getHousekeeperBlocksDays());
        e.setTherapistScheduleDays(in.getTherapistScheduleDays());
        e.setTherapistMorningTime(therapistMorningTime);
        e.setMorningSummaryTime(morningSummaryTime);
        StaffBotSettingsEntity saved = repository.saveAndFlush(e);
        auditLogService.record(
                AuditAction.STAFF_BOT_SETTINGS_UPDATED, AuditEntityType.SETTINGS, null, "Staff bot settings: " + String.join("; ", changes));
        return toDto(saved);
    }

    private static LocalTime time(String field, String value) {
        try {
            return LocalTime.parse(value);
        } catch (RuntimeException e) {
            throw ValidationException.field(field, "Must be a time like 08:00");
        }
    }

    private static void change(List<String> changes, String what, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(what + " " + before + " -> " + after);
        }
    }

    private static StaffBotSettings toDto(StaffBotSettingsEntity e) {
        return new StaffBotSettings(
                e.isMyShiftsEnabled(), e.isMyDayEnabled(), e.isMyMonthEnabled(), e.isMyRequestsEnabled(), e.isBotSettingsEnabled(),
                e.isRequestsEnabled(), e.isMoveRequestsEnabled(), e.isDeleteRequestsEnabled(),
                e.getRequestMinHoursBefore(), e.isAllowTodayRequests(), e.getMoveWindowDays(), e.isReasonRequired(), e.getMyShiftsDays(),
                e.isFallbackToAdmin(), e.isPendingReminderEnabled(), e.getPendingReminderHours(),
                e.isEveReminderEnabled(), e.getEveReminderTime().toString(), e.isEveReminderDefaultOn(),
                e.isRosterChangeEnabled(), e.getRosterChangeHorizonDays(), e.getRosterChangeDelayMinutes(),
                e.isEngineerTasksEnabled(), e.isHousekeeperBlocksEnabled(), e.getHousekeeperBlocksDays(),
                e.isTherapistScheduleEnabled(), e.getTherapistScheduleDays(),
                e.isTherapistMorningEnabled(), e.getTherapistMorningTime().toString(), e.isTherapistMorningDefaultOn(),
                e.isWhoIsOnShiftEnabled(), e.isWhoIsOnShiftForHr(), e.isSummaryCommandEnabled(),
                e.isMorningSummaryEnabled(), e.getMorningSummaryTime().toString(),
                e.isSummaryArrivals(), e.isSummaryDepartures(), e.isSummaryInHouse(), e.isSummaryOccupancy(),
                e.isSummaryRooms(), e.isSummaryStaff(), e.isSummaryRequests(),
                TimestampFormat.toUtc(e.getUpdatedAt()));
    }
}
