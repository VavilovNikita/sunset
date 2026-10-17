package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The single row of notification settings (id 1, seeded by V148); only ever updated.
 */
@Entity
@Table(name = "NotificationSettings")
public class NotificationSettingsEntity {

    @Id
    private Integer id;

    private boolean lateEnabled;

    private int lateGraceMinutes;

    private boolean absenceEnabled;

    private int absenceMinutes;

    private boolean blockCreatedEnabled;

    private boolean blockLastDayEnabled;

    private boolean blockExpiredEnabled;

    private boolean terminalAlertsEnabled;

    private int terminalSilenceMinutes;

    private boolean quietHoursEnabled;

    private LocalTime quietStart;

    private LocalTime quietEnd;

    private LocalTime morningTime;

    private String defaultLanguage;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] lateRecipientUserIds = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] directorUserIds = new String[0];

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isLateEnabled() {
        return lateEnabled;
    }

    public void setLateEnabled(boolean lateEnabled) {
        this.lateEnabled = lateEnabled;
    }

    public int getLateGraceMinutes() {
        return lateGraceMinutes;
    }

    public void setLateGraceMinutes(int lateGraceMinutes) {
        this.lateGraceMinutes = lateGraceMinutes;
    }

    public boolean isAbsenceEnabled() {
        return absenceEnabled;
    }

    public void setAbsenceEnabled(boolean absenceEnabled) {
        this.absenceEnabled = absenceEnabled;
    }

    public int getAbsenceMinutes() {
        return absenceMinutes;
    }

    public void setAbsenceMinutes(int absenceMinutes) {
        this.absenceMinutes = absenceMinutes;
    }

    public boolean isBlockCreatedEnabled() {
        return blockCreatedEnabled;
    }

    public void setBlockCreatedEnabled(boolean blockCreatedEnabled) {
        this.blockCreatedEnabled = blockCreatedEnabled;
    }

    public boolean isBlockLastDayEnabled() {
        return blockLastDayEnabled;
    }

    public void setBlockLastDayEnabled(boolean blockLastDayEnabled) {
        this.blockLastDayEnabled = blockLastDayEnabled;
    }

    public boolean isBlockExpiredEnabled() {
        return blockExpiredEnabled;
    }

    public void setBlockExpiredEnabled(boolean blockExpiredEnabled) {
        this.blockExpiredEnabled = blockExpiredEnabled;
    }

    public boolean isTerminalAlertsEnabled() {
        return terminalAlertsEnabled;
    }

    public void setTerminalAlertsEnabled(boolean terminalAlertsEnabled) {
        this.terminalAlertsEnabled = terminalAlertsEnabled;
    }

    public int getTerminalSilenceMinutes() {
        return terminalSilenceMinutes;
    }

    public void setTerminalSilenceMinutes(int terminalSilenceMinutes) {
        this.terminalSilenceMinutes = terminalSilenceMinutes;
    }

    public boolean isQuietHoursEnabled() {
        return quietHoursEnabled;
    }

    public void setQuietHoursEnabled(boolean quietHoursEnabled) {
        this.quietHoursEnabled = quietHoursEnabled;
    }

    public LocalTime getQuietStart() {
        return quietStart;
    }

    public void setQuietStart(LocalTime quietStart) {
        this.quietStart = quietStart;
    }

    public LocalTime getQuietEnd() {
        return quietEnd;
    }

    public void setQuietEnd(LocalTime quietEnd) {
        this.quietEnd = quietEnd;
    }

    public LocalTime getMorningTime() {
        return morningTime;
    }

    public void setMorningTime(LocalTime morningTime) {
        this.morningTime = morningTime;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public String[] getLateRecipientUserIds() {
        return lateRecipientUserIds;
    }

    public void setLateRecipientUserIds(String[] lateRecipientUserIds) {
        this.lateRecipientUserIds = lateRecipientUserIds;
    }

    public String[] getDirectorUserIds() {
        return directorUserIds;
    }

    public void setDirectorUserIds(String[] directorUserIds) {
        this.directorUserIds = directorUserIds;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
