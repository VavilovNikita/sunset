package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.NotificationLanguage;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * NotificationSettings
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NotificationSettings {

  private Boolean lateEnabled;

  private Integer lateGraceMinutes;

  private Integer minLateGraceMinutes;

  private Boolean absenceEnabled;

  private Integer absenceMinutes;

  private Boolean blockCreatedEnabled;

  private Boolean blockLastDayEnabled;

  private Boolean blockExpiredEnabled;

  private Boolean terminalAlertsEnabled;

  private Integer terminalSilenceMinutes;

  private Boolean quietHoursEnabled;

  private String quietStart;

  private String quietEnd;

  private String morningTime;

  private NotificationLanguage defaultLanguage;

  @Valid
  private List<String> lateRecipientUserIds = new ArrayList<>();

  @Valid
  private List<String> directorUserIds = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public NotificationSettings() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationSettings(Boolean lateEnabled, Integer lateGraceMinutes, Integer minLateGraceMinutes, Boolean absenceEnabled, Integer absenceMinutes, Boolean blockCreatedEnabled, Boolean blockLastDayEnabled, Boolean blockExpiredEnabled, Boolean terminalAlertsEnabled, Integer terminalSilenceMinutes, Boolean quietHoursEnabled, String quietStart, String quietEnd, String morningTime, NotificationLanguage defaultLanguage, List<String> lateRecipientUserIds, List<String> directorUserIds, OffsetDateTime updatedAt) {
    this.lateEnabled = lateEnabled;
    this.lateGraceMinutes = lateGraceMinutes;
    this.minLateGraceMinutes = minLateGraceMinutes;
    this.absenceEnabled = absenceEnabled;
    this.absenceMinutes = absenceMinutes;
    this.blockCreatedEnabled = blockCreatedEnabled;
    this.blockLastDayEnabled = blockLastDayEnabled;
    this.blockExpiredEnabled = blockExpiredEnabled;
    this.terminalAlertsEnabled = terminalAlertsEnabled;
    this.terminalSilenceMinutes = terminalSilenceMinutes;
    this.quietHoursEnabled = quietHoursEnabled;
    this.quietStart = quietStart;
    this.quietEnd = quietEnd;
    this.morningTime = morningTime;
    this.defaultLanguage = defaultLanguage;
    this.lateRecipientUserIds = lateRecipientUserIds;
    this.directorUserIds = directorUserIds;
    this.updatedAt = updatedAt;
  }

  public NotificationSettings lateEnabled(Boolean lateEnabled) {
    this.lateEnabled = lateEnabled;
    return this;
  }

  /**
   * Get lateEnabled
   * @return lateEnabled
   */
  @NotNull 
  @JsonProperty("lateEnabled")
  public Boolean getLateEnabled() {
    return lateEnabled;
  }

  public void setLateEnabled(Boolean lateEnabled) {
    this.lateEnabled = lateEnabled;
  }

  public NotificationSettings lateGraceMinutes(Integer lateGraceMinutes) {
    this.lateGraceMinutes = lateGraceMinutes;
    return this;
  }

  /**
   * Minutes after the planned start of an interval before \"late\" is sent. Not below `minLateGraceMinutes`.
   * minimum: 1
   * maximum: 240
   * @return lateGraceMinutes
   */
  @NotNull @Min(1) @Max(240) 
  @JsonProperty("lateGraceMinutes")
  public Integer getLateGraceMinutes() {
    return lateGraceMinutes;
  }

  public void setLateGraceMinutes(Integer lateGraceMinutes) {
    this.lateGraceMinutes = lateGraceMinutes;
  }

  public NotificationSettings minLateGraceMinutes(Integer minLateGraceMinutes) {
    this.minLateGraceMinutes = minLateGraceMinutes;
    return this;
  }

  /**
   * The attendance terminal's poll interval in minutes (currently 5) - the lowest grace the server accepts.
   * @return minLateGraceMinutes
   */
  
  @JsonProperty("minLateGraceMinutes")
  public Integer getMinLateGraceMinutes() {
    return minLateGraceMinutes;
  }

  public void setMinLateGraceMinutes(Integer minLateGraceMinutes) {
    this.minLateGraceMinutes = minLateGraceMinutes;
  }

  public NotificationSettings absenceEnabled(Boolean absenceEnabled) {
    this.absenceEnabled = absenceEnabled;
    return this;
  }

  /**
   * Get absenceEnabled
   * @return absenceEnabled
   */
  @NotNull 
  @JsonProperty("absenceEnabled")
  public Boolean getAbsenceEnabled() {
    return absenceEnabled;
  }

  public void setAbsenceEnabled(Boolean absenceEnabled) {
    this.absenceEnabled = absenceEnabled;
  }

  public NotificationSettings absenceMinutes(Integer absenceMinutes) {
    this.absenceMinutes = absenceMinutes;
    return this;
  }

  /**
   * Minutes after the planned start of an interval before the directors are told the employee is absent.
   * minimum: 1
   * maximum: 1440
   * @return absenceMinutes
   */
  @NotNull @Min(1) @Max(1440) 
  @JsonProperty("absenceMinutes")
  public Integer getAbsenceMinutes() {
    return absenceMinutes;
  }

  public void setAbsenceMinutes(Integer absenceMinutes) {
    this.absenceMinutes = absenceMinutes;
  }

  public NotificationSettings blockCreatedEnabled(Boolean blockCreatedEnabled) {
    this.blockCreatedEnabled = blockCreatedEnabled;
    return this;
  }

  /**
   * Get blockCreatedEnabled
   * @return blockCreatedEnabled
   */
  @NotNull 
  @JsonProperty("blockCreatedEnabled")
  public Boolean getBlockCreatedEnabled() {
    return blockCreatedEnabled;
  }

  public void setBlockCreatedEnabled(Boolean blockCreatedEnabled) {
    this.blockCreatedEnabled = blockCreatedEnabled;
  }

  public NotificationSettings blockLastDayEnabled(Boolean blockLastDayEnabled) {
    this.blockLastDayEnabled = blockLastDayEnabled;
    return this;
  }

  /**
   * Get blockLastDayEnabled
   * @return blockLastDayEnabled
   */
  @NotNull 
  @JsonProperty("blockLastDayEnabled")
  public Boolean getBlockLastDayEnabled() {
    return blockLastDayEnabled;
  }

  public void setBlockLastDayEnabled(Boolean blockLastDayEnabled) {
    this.blockLastDayEnabled = blockLastDayEnabled;
  }

  public NotificationSettings blockExpiredEnabled(Boolean blockExpiredEnabled) {
    this.blockExpiredEnabled = blockExpiredEnabled;
    return this;
  }

  /**
   * Get blockExpiredEnabled
   * @return blockExpiredEnabled
   */
  @NotNull 
  @JsonProperty("blockExpiredEnabled")
  public Boolean getBlockExpiredEnabled() {
    return blockExpiredEnabled;
  }

  public void setBlockExpiredEnabled(Boolean blockExpiredEnabled) {
    this.blockExpiredEnabled = blockExpiredEnabled;
  }

  public NotificationSettings terminalAlertsEnabled(Boolean terminalAlertsEnabled) {
    this.terminalAlertsEnabled = terminalAlertsEnabled;
    return this;
  }

  /**
   * Get terminalAlertsEnabled
   * @return terminalAlertsEnabled
   */
  @NotNull 
  @JsonProperty("terminalAlertsEnabled")
  public Boolean getTerminalAlertsEnabled() {
    return terminalAlertsEnabled;
  }

  public void setTerminalAlertsEnabled(Boolean terminalAlertsEnabled) {
    this.terminalAlertsEnabled = terminalAlertsEnabled;
  }

  public NotificationSettings terminalSilenceMinutes(Integer terminalSilenceMinutes) {
    this.terminalSilenceMinutes = terminalSilenceMinutes;
    return this;
  }

  /**
   * A terminal not reached for longer than this is \"silent\". While any active terminal is silent, no late or absence message is raised (its punches cannot be seen), and the directors are told once instead. 
   * minimum: 1
   * maximum: 1440
   * @return terminalSilenceMinutes
   */
  @NotNull @Min(1) @Max(1440) 
  @JsonProperty("terminalSilenceMinutes")
  public Integer getTerminalSilenceMinutes() {
    return terminalSilenceMinutes;
  }

  public void setTerminalSilenceMinutes(Integer terminalSilenceMinutes) {
    this.terminalSilenceMinutes = terminalSilenceMinutes;
  }

  public NotificationSettings quietHoursEnabled(Boolean quietHoursEnabled) {
    this.quietHoursEnabled = quietHoursEnabled;
    return this;
  }

  /**
   * Get quietHoursEnabled
   * @return quietHoursEnabled
   */
  @NotNull 
  @JsonProperty("quietHoursEnabled")
  public Boolean getQuietHoursEnabled() {
    return quietHoursEnabled;
  }

  public void setQuietHoursEnabled(Boolean quietHoursEnabled) {
    this.quietHoursEnabled = quietHoursEnabled;
  }

  public NotificationSettings quietStart(String quietStart) {
    this.quietStart = quietStart;
    return this;
  }

  /**
   * Hotel time (Asia/Bangkok), inclusive. The window may cross midnight.
   * @return quietStart
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("quietStart")
  public String getQuietStart() {
    return quietStart;
  }

  public void setQuietStart(String quietStart) {
    this.quietStart = quietStart;
  }

  public NotificationSettings quietEnd(String quietEnd) {
    this.quietEnd = quietEnd;
    return this;
  }

  /**
   * Hotel time, exclusive. Messages raised during quiet hours are sent at this time.
   * @return quietEnd
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("quietEnd")
  public String getQuietEnd() {
    return quietEnd;
  }

  public void setQuietEnd(String quietEnd) {
    this.quietEnd = quietEnd;
  }

  public NotificationSettings morningTime(String morningTime) {
    this.morningTime = morningTime;
    return this;
  }

  /**
   * Hotel time of the morning block reminders (last day, expired block with an open task).
   * @return morningTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("morningTime")
  public String getMorningTime() {
    return morningTime;
  }

  public void setMorningTime(String morningTime) {
    this.morningTime = morningTime;
  }

  public NotificationSettings defaultLanguage(NotificationLanguage defaultLanguage) {
    this.defaultLanguage = defaultLanguage;
    return this;
  }

  /**
   * Get defaultLanguage
   * @return defaultLanguage
   */
  @NotNull @Valid 
  @JsonProperty("defaultLanguage")
  public NotificationLanguage getDefaultLanguage() {
    return defaultLanguage;
  }

  public void setDefaultLanguage(NotificationLanguage defaultLanguage) {
    this.defaultLanguage = defaultLanguage;
  }

  public NotificationSettings lateRecipientUserIds(List<String> lateRecipientUserIds) {
    this.lateRecipientUserIds = lateRecipientUserIds;
    return this;
  }

  public NotificationSettings addLateRecipientUserIdsItem(String lateRecipientUserIdsItem) {
    if (this.lateRecipientUserIds == null) {
      this.lateRecipientUserIds = new ArrayList<>();
    }
    this.lateRecipientUserIds.add(lateRecipientUserIdsItem);
    return this;
  }

  /**
   * Besides the employee themself, who is told that an employee is late.
   * @return lateRecipientUserIds
   */
  @NotNull 
  @JsonProperty("lateRecipientUserIds")
  public List<String> getLateRecipientUserIds() {
    return lateRecipientUserIds;
  }

  public void setLateRecipientUserIds(List<String> lateRecipientUserIds) {
    this.lateRecipientUserIds = lateRecipientUserIds;
  }

  public NotificationSettings directorUserIds(List<String> directorUserIds) {
    this.directorUserIds = directorUserIds;
    return this;
  }

  public NotificationSettings addDirectorUserIdsItem(String directorUserIdsItem) {
    if (this.directorUserIds == null) {
      this.directorUserIds = new ArrayList<>();
    }
    this.directorUserIds.add(directorUserIdsItem);
    return this;
  }

  /**
   * Who is told about absences, terminal outages and blocks nobody on shift can take.
   * @return directorUserIds
   */
  @NotNull 
  @JsonProperty("directorUserIds")
  public List<String> getDirectorUserIds() {
    return directorUserIds;
  }

  public void setDirectorUserIds(List<String> directorUserIds) {
    this.directorUserIds = directorUserIds;
  }

  public NotificationSettings updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationSettings notificationSettings = (NotificationSettings) o;
    return Objects.equals(this.lateEnabled, notificationSettings.lateEnabled) &&
        Objects.equals(this.lateGraceMinutes, notificationSettings.lateGraceMinutes) &&
        Objects.equals(this.minLateGraceMinutes, notificationSettings.minLateGraceMinutes) &&
        Objects.equals(this.absenceEnabled, notificationSettings.absenceEnabled) &&
        Objects.equals(this.absenceMinutes, notificationSettings.absenceMinutes) &&
        Objects.equals(this.blockCreatedEnabled, notificationSettings.blockCreatedEnabled) &&
        Objects.equals(this.blockLastDayEnabled, notificationSettings.blockLastDayEnabled) &&
        Objects.equals(this.blockExpiredEnabled, notificationSettings.blockExpiredEnabled) &&
        Objects.equals(this.terminalAlertsEnabled, notificationSettings.terminalAlertsEnabled) &&
        Objects.equals(this.terminalSilenceMinutes, notificationSettings.terminalSilenceMinutes) &&
        Objects.equals(this.quietHoursEnabled, notificationSettings.quietHoursEnabled) &&
        Objects.equals(this.quietStart, notificationSettings.quietStart) &&
        Objects.equals(this.quietEnd, notificationSettings.quietEnd) &&
        Objects.equals(this.morningTime, notificationSettings.morningTime) &&
        Objects.equals(this.defaultLanguage, notificationSettings.defaultLanguage) &&
        Objects.equals(this.lateRecipientUserIds, notificationSettings.lateRecipientUserIds) &&
        Objects.equals(this.directorUserIds, notificationSettings.directorUserIds) &&
        Objects.equals(this.updatedAt, notificationSettings.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lateEnabled, lateGraceMinutes, minLateGraceMinutes, absenceEnabled, absenceMinutes, blockCreatedEnabled, blockLastDayEnabled, blockExpiredEnabled, terminalAlertsEnabled, terminalSilenceMinutes, quietHoursEnabled, quietStart, quietEnd, morningTime, defaultLanguage, lateRecipientUserIds, directorUserIds, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationSettings {\n");
    sb.append("    lateEnabled: ").append(toIndentedString(lateEnabled)).append("\n");
    sb.append("    lateGraceMinutes: ").append(toIndentedString(lateGraceMinutes)).append("\n");
    sb.append("    minLateGraceMinutes: ").append(toIndentedString(minLateGraceMinutes)).append("\n");
    sb.append("    absenceEnabled: ").append(toIndentedString(absenceEnabled)).append("\n");
    sb.append("    absenceMinutes: ").append(toIndentedString(absenceMinutes)).append("\n");
    sb.append("    blockCreatedEnabled: ").append(toIndentedString(blockCreatedEnabled)).append("\n");
    sb.append("    blockLastDayEnabled: ").append(toIndentedString(blockLastDayEnabled)).append("\n");
    sb.append("    blockExpiredEnabled: ").append(toIndentedString(blockExpiredEnabled)).append("\n");
    sb.append("    terminalAlertsEnabled: ").append(toIndentedString(terminalAlertsEnabled)).append("\n");
    sb.append("    terminalSilenceMinutes: ").append(toIndentedString(terminalSilenceMinutes)).append("\n");
    sb.append("    quietHoursEnabled: ").append(toIndentedString(quietHoursEnabled)).append("\n");
    sb.append("    quietStart: ").append(toIndentedString(quietStart)).append("\n");
    sb.append("    quietEnd: ").append(toIndentedString(quietEnd)).append("\n");
    sb.append("    morningTime: ").append(toIndentedString(morningTime)).append("\n");
    sb.append("    defaultLanguage: ").append(toIndentedString(defaultLanguage)).append("\n");
    sb.append("    lateRecipientUserIds: ").append(toIndentedString(lateRecipientUserIds)).append("\n");
    sb.append("    directorUserIds: ").append(toIndentedString(directorUserIds)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

