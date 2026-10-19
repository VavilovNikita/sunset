package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * The single row of staff-bot and shift-request settings (id 1, seeded by V150); only ever updated.
 */
@Entity
@Table(name = "StaffBotSettings")
public class StaffBotSettingsEntity {

    @Id
    private Integer id;

    private boolean myShiftsEnabled;

    private boolean myDayEnabled;

    private boolean myMonthEnabled;

    private boolean myRequestsEnabled;

    private boolean botSettingsEnabled;

    private boolean requestsEnabled;

    private boolean moveRequestsEnabled;

    private boolean deleteRequestsEnabled;

    private int requestMinHoursBefore;

    private boolean allowTodayRequests;

    private int moveWindowDays;

    private int moveMaxDaysAhead;

    private boolean reasonRequired;

    private int myShiftsDays;

    private boolean fallbackToAdmin;

    private boolean pendingReminderEnabled;

    private int pendingReminderHours;

    private boolean eveReminderEnabled;

    private LocalTime eveReminderTime;

    private boolean eveReminderDefaultOn;

    private boolean rosterChangeEnabled;

    private int rosterChangeHorizonDays;

    private int rosterChangeDelayMinutes;

    private boolean engineerTasksEnabled;

    private boolean housekeeperBlocksEnabled;

    private int housekeeperBlocksDays;

    private boolean therapistScheduleEnabled;

    private int therapistScheduleDays;

    private boolean therapistMorningEnabled;

    private LocalTime therapistMorningTime;

    private boolean therapistMorningDefaultOn;

    private boolean whoIsOnShiftEnabled;

    private boolean whoIsOnShiftForHr;

    private boolean summaryCommandEnabled;

    private boolean morningSummaryEnabled;

    private LocalTime morningSummaryTime;

    private boolean summaryArrivals;

    private boolean summaryDepartures;

    private boolean summaryInHouse;

    private boolean summaryOccupancy;

    private boolean summaryRooms;

    private boolean summaryStaff;

    private boolean summaryRequests;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isMyShiftsEnabled() {
        return myShiftsEnabled;
    }

    public void setMyShiftsEnabled(boolean myShiftsEnabled) {
        this.myShiftsEnabled = myShiftsEnabled;
    }

    public boolean isMyDayEnabled() {
        return myDayEnabled;
    }

    public void setMyDayEnabled(boolean myDayEnabled) {
        this.myDayEnabled = myDayEnabled;
    }

    public boolean isMyMonthEnabled() {
        return myMonthEnabled;
    }

    public void setMyMonthEnabled(boolean myMonthEnabled) {
        this.myMonthEnabled = myMonthEnabled;
    }

    public boolean isMyRequestsEnabled() {
        return myRequestsEnabled;
    }

    public void setMyRequestsEnabled(boolean myRequestsEnabled) {
        this.myRequestsEnabled = myRequestsEnabled;
    }

    public boolean isBotSettingsEnabled() {
        return botSettingsEnabled;
    }

    public void setBotSettingsEnabled(boolean botSettingsEnabled) {
        this.botSettingsEnabled = botSettingsEnabled;
    }

    public boolean isRequestsEnabled() {
        return requestsEnabled;
    }

    public void setRequestsEnabled(boolean requestsEnabled) {
        this.requestsEnabled = requestsEnabled;
    }

    public boolean isMoveRequestsEnabled() {
        return moveRequestsEnabled;
    }

    public void setMoveRequestsEnabled(boolean moveRequestsEnabled) {
        this.moveRequestsEnabled = moveRequestsEnabled;
    }

    public boolean isDeleteRequestsEnabled() {
        return deleteRequestsEnabled;
    }

    public void setDeleteRequestsEnabled(boolean deleteRequestsEnabled) {
        this.deleteRequestsEnabled = deleteRequestsEnabled;
    }

    public int getRequestMinHoursBefore() {
        return requestMinHoursBefore;
    }

    public void setRequestMinHoursBefore(int requestMinHoursBefore) {
        this.requestMinHoursBefore = requestMinHoursBefore;
    }

    public boolean isAllowTodayRequests() {
        return allowTodayRequests;
    }

    public void setAllowTodayRequests(boolean allowTodayRequests) {
        this.allowTodayRequests = allowTodayRequests;
    }

    public int getMoveWindowDays() {
        return moveWindowDays;
    }

    public void setMoveWindowDays(int moveWindowDays) {
        this.moveWindowDays = moveWindowDays;
    }

    public int getMoveMaxDaysAhead() {
        return moveMaxDaysAhead;
    }

    public void setMoveMaxDaysAhead(int moveMaxDaysAhead) {
        this.moveMaxDaysAhead = moveMaxDaysAhead;
    }

    public boolean isReasonRequired() {
        return reasonRequired;
    }

    public void setReasonRequired(boolean reasonRequired) {
        this.reasonRequired = reasonRequired;
    }

    public int getMyShiftsDays() {
        return myShiftsDays;
    }

    public void setMyShiftsDays(int myShiftsDays) {
        this.myShiftsDays = myShiftsDays;
    }

    public boolean isFallbackToAdmin() {
        return fallbackToAdmin;
    }

    public void setFallbackToAdmin(boolean fallbackToAdmin) {
        this.fallbackToAdmin = fallbackToAdmin;
    }

    public boolean isPendingReminderEnabled() {
        return pendingReminderEnabled;
    }

    public void setPendingReminderEnabled(boolean pendingReminderEnabled) {
        this.pendingReminderEnabled = pendingReminderEnabled;
    }

    public int getPendingReminderHours() {
        return pendingReminderHours;
    }

    public void setPendingReminderHours(int pendingReminderHours) {
        this.pendingReminderHours = pendingReminderHours;
    }

    public boolean isEveReminderEnabled() {
        return eveReminderEnabled;
    }

    public void setEveReminderEnabled(boolean eveReminderEnabled) {
        this.eveReminderEnabled = eveReminderEnabled;
    }

    public LocalTime getEveReminderTime() {
        return eveReminderTime;
    }

    public void setEveReminderTime(LocalTime eveReminderTime) {
        this.eveReminderTime = eveReminderTime;
    }

    public boolean isEveReminderDefaultOn() {
        return eveReminderDefaultOn;
    }

    public void setEveReminderDefaultOn(boolean eveReminderDefaultOn) {
        this.eveReminderDefaultOn = eveReminderDefaultOn;
    }

    public boolean isRosterChangeEnabled() {
        return rosterChangeEnabled;
    }

    public void setRosterChangeEnabled(boolean rosterChangeEnabled) {
        this.rosterChangeEnabled = rosterChangeEnabled;
    }

    public int getRosterChangeHorizonDays() {
        return rosterChangeHorizonDays;
    }

    public void setRosterChangeHorizonDays(int rosterChangeHorizonDays) {
        this.rosterChangeHorizonDays = rosterChangeHorizonDays;
    }

    public int getRosterChangeDelayMinutes() {
        return rosterChangeDelayMinutes;
    }

    public void setRosterChangeDelayMinutes(int rosterChangeDelayMinutes) {
        this.rosterChangeDelayMinutes = rosterChangeDelayMinutes;
    }

    public boolean isEngineerTasksEnabled() {
        return engineerTasksEnabled;
    }

    public void setEngineerTasksEnabled(boolean engineerTasksEnabled) {
        this.engineerTasksEnabled = engineerTasksEnabled;
    }

    public boolean isHousekeeperBlocksEnabled() {
        return housekeeperBlocksEnabled;
    }

    public void setHousekeeperBlocksEnabled(boolean housekeeperBlocksEnabled) {
        this.housekeeperBlocksEnabled = housekeeperBlocksEnabled;
    }

    public int getHousekeeperBlocksDays() {
        return housekeeperBlocksDays;
    }

    public void setHousekeeperBlocksDays(int housekeeperBlocksDays) {
        this.housekeeperBlocksDays = housekeeperBlocksDays;
    }

    public boolean isTherapistScheduleEnabled() {
        return therapistScheduleEnabled;
    }

    public void setTherapistScheduleEnabled(boolean therapistScheduleEnabled) {
        this.therapistScheduleEnabled = therapistScheduleEnabled;
    }

    public int getTherapistScheduleDays() {
        return therapistScheduleDays;
    }

    public void setTherapistScheduleDays(int therapistScheduleDays) {
        this.therapistScheduleDays = therapistScheduleDays;
    }

    public boolean isTherapistMorningEnabled() {
        return therapistMorningEnabled;
    }

    public void setTherapistMorningEnabled(boolean therapistMorningEnabled) {
        this.therapistMorningEnabled = therapistMorningEnabled;
    }

    public LocalTime getTherapistMorningTime() {
        return therapistMorningTime;
    }

    public void setTherapistMorningTime(LocalTime therapistMorningTime) {
        this.therapistMorningTime = therapistMorningTime;
    }

    public boolean isTherapistMorningDefaultOn() {
        return therapistMorningDefaultOn;
    }

    public void setTherapistMorningDefaultOn(boolean therapistMorningDefaultOn) {
        this.therapistMorningDefaultOn = therapistMorningDefaultOn;
    }

    public boolean isWhoIsOnShiftEnabled() {
        return whoIsOnShiftEnabled;
    }

    public void setWhoIsOnShiftEnabled(boolean whoIsOnShiftEnabled) {
        this.whoIsOnShiftEnabled = whoIsOnShiftEnabled;
    }

    public boolean isWhoIsOnShiftForHr() {
        return whoIsOnShiftForHr;
    }

    public void setWhoIsOnShiftForHr(boolean whoIsOnShiftForHr) {
        this.whoIsOnShiftForHr = whoIsOnShiftForHr;
    }

    public boolean isSummaryCommandEnabled() {
        return summaryCommandEnabled;
    }

    public void setSummaryCommandEnabled(boolean summaryCommandEnabled) {
        this.summaryCommandEnabled = summaryCommandEnabled;
    }

    public boolean isMorningSummaryEnabled() {
        return morningSummaryEnabled;
    }

    public void setMorningSummaryEnabled(boolean morningSummaryEnabled) {
        this.morningSummaryEnabled = morningSummaryEnabled;
    }

    public LocalTime getMorningSummaryTime() {
        return morningSummaryTime;
    }

    public void setMorningSummaryTime(LocalTime morningSummaryTime) {
        this.morningSummaryTime = morningSummaryTime;
    }

    public boolean isSummaryArrivals() {
        return summaryArrivals;
    }

    public void setSummaryArrivals(boolean summaryArrivals) {
        this.summaryArrivals = summaryArrivals;
    }

    public boolean isSummaryDepartures() {
        return summaryDepartures;
    }

    public void setSummaryDepartures(boolean summaryDepartures) {
        this.summaryDepartures = summaryDepartures;
    }

    public boolean isSummaryInHouse() {
        return summaryInHouse;
    }

    public void setSummaryInHouse(boolean summaryInHouse) {
        this.summaryInHouse = summaryInHouse;
    }

    public boolean isSummaryOccupancy() {
        return summaryOccupancy;
    }

    public void setSummaryOccupancy(boolean summaryOccupancy) {
        this.summaryOccupancy = summaryOccupancy;
    }

    public boolean isSummaryRooms() {
        return summaryRooms;
    }

    public void setSummaryRooms(boolean summaryRooms) {
        this.summaryRooms = summaryRooms;
    }

    public boolean isSummaryStaff() {
        return summaryStaff;
    }

    public void setSummaryStaff(boolean summaryStaff) {
        this.summaryStaff = summaryStaff;
    }

    public boolean isSummaryRequests() {
        return summaryRequests;
    }

    public void setSummaryRequests(boolean summaryRequests) {
        this.summaryRequests = summaryRequests;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
