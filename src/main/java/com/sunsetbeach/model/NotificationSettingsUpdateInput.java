package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.NotificationLanguage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PUT /settings/notifications&#x60; - every field of &#x60;NotificationSettings&#x60; except &#x60;minLateGraceMinutes&#x60; and &#x60;updatedAt&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NotificationSettingsUpdateInput {

  private Boolean lateEnabled;

  private Integer lateGraceMinutes;

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

  public NotificationSettingsUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationSettingsUpdateInput(Boolean lateEnabled, Integer lateGraceMinutes, Boolean absenceEnabled, Integer absenceMinutes, Boolean blockCreatedEnabled, Boolean blockLastDayEnabled, Boolean blockExpiredEnabled, Boolean terminalAlertsEnabled, Integer terminalSilenceMinutes, Boolean quietHoursEnabled, String quietStart, String quietEnd, String morningTime, NotificationLanguage defaultLanguage, List<String> lateRecipientUserIds, List<String> directorUserIds) {
    this.lateEnabled = lateEnabled;
    this.lateGraceMinutes = lateGraceMinutes;
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
  }

  public NotificationSettingsUpdateInput lateEnabled(Boolean lateEnabled) {
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

  public NotificationSettingsUpdateInput lateGraceMinutes(Integer lateGraceMinutes) {
    this.lateGraceMinutes = lateGraceMinutes;
    return this;
  }

  /**
   * Get lateGraceMinutes
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

  public NotificationSettingsUpdateInput absenceEnabled(Boolean absenceEnabled) {
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

  public NotificationSettingsUpdateInput absenceMinutes(Integer absenceMinutes) {
    this.absenceMinutes = absenceMinutes;
    return this;
  }

  /**
   * Get absenceMinutes
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

  public NotificationSettingsUpdateInput blockCreatedEnabled(Boolean blockCreatedEnabled) {
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

  public NotificationSettingsUpdateInput blockLastDayEnabled(Boolean blockLastDayEnabled) {
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

  public NotificationSettingsUpdateInput blockExpiredEnabled(Boolean blockExpiredEnabled) {
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

  public NotificationSettingsUpdateInput terminalAlertsEnabled(Boolean terminalAlertsEnabled) {
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

  public NotificationSettingsUpdateInput terminalSilenceMinutes(Integer terminalSilenceMinutes) {
    this.terminalSilenceMinutes = terminalSilenceMinutes;
    return this;
  }

  /**
   * Get terminalSilenceMinutes
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

  public NotificationSettingsUpdateInput quietHoursEnabled(Boolean quietHoursEnabled) {
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

  public NotificationSettingsUpdateInput quietStart(String quietStart) {
    this.quietStart = quietStart;
    return this;
  }

  /**
   * Get quietStart
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

  public NotificationSettingsUpdateInput quietEnd(String quietEnd) {
    this.quietEnd = quietEnd;
    return this;
  }

  /**
   * Get quietEnd
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

  public NotificationSettingsUpdateInput morningTime(String morningTime) {
    this.morningTime = morningTime;
    return this;
  }

  /**
   * Get morningTime
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

  public NotificationSettingsUpdateInput defaultLanguage(NotificationLanguage defaultLanguage) {
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

  public NotificationSettingsUpdateInput lateRecipientUserIds(List<String> lateRecipientUserIds) {
    this.lateRecipientUserIds = lateRecipientUserIds;
    return this;
  }

  public NotificationSettingsUpdateInput addLateRecipientUserIdsItem(String lateRecipientUserIdsItem) {
    if (this.lateRecipientUserIds == null) {
      this.lateRecipientUserIds = new ArrayList<>();
    }
    this.lateRecipientUserIds.add(lateRecipientUserIdsItem);
    return this;
  }

  /**
   * Get lateRecipientUserIds
   * @return lateRecipientUserIds
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("lateRecipientUserIds")
  public List<String> getLateRecipientUserIds() {
    return lateRecipientUserIds;
  }

  public void setLateRecipientUserIds(List<String> lateRecipientUserIds) {
    this.lateRecipientUserIds = lateRecipientUserIds;
  }

  public NotificationSettingsUpdateInput directorUserIds(List<String> directorUserIds) {
    this.directorUserIds = directorUserIds;
    return this;
  }

  public NotificationSettingsUpdateInput addDirectorUserIdsItem(String directorUserIdsItem) {
    if (this.directorUserIds == null) {
      this.directorUserIds = new ArrayList<>();
    }
    this.directorUserIds.add(directorUserIdsItem);
    return this;
  }

  /**
   * Get directorUserIds
   * @return directorUserIds
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("directorUserIds")
  public List<String> getDirectorUserIds() {
    return directorUserIds;
  }

  public void setDirectorUserIds(List<String> directorUserIds) {
    this.directorUserIds = directorUserIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationSettingsUpdateInput notificationSettingsUpdateInput = (NotificationSettingsUpdateInput) o;
    return Objects.equals(this.lateEnabled, notificationSettingsUpdateInput.lateEnabled) &&
        Objects.equals(this.lateGraceMinutes, notificationSettingsUpdateInput.lateGraceMinutes) &&
        Objects.equals(this.absenceEnabled, notificationSettingsUpdateInput.absenceEnabled) &&
        Objects.equals(this.absenceMinutes, notificationSettingsUpdateInput.absenceMinutes) &&
        Objects.equals(this.blockCreatedEnabled, notificationSettingsUpdateInput.blockCreatedEnabled) &&
        Objects.equals(this.blockLastDayEnabled, notificationSettingsUpdateInput.blockLastDayEnabled) &&
        Objects.equals(this.blockExpiredEnabled, notificationSettingsUpdateInput.blockExpiredEnabled) &&
        Objects.equals(this.terminalAlertsEnabled, notificationSettingsUpdateInput.terminalAlertsEnabled) &&
        Objects.equals(this.terminalSilenceMinutes, notificationSettingsUpdateInput.terminalSilenceMinutes) &&
        Objects.equals(this.quietHoursEnabled, notificationSettingsUpdateInput.quietHoursEnabled) &&
        Objects.equals(this.quietStart, notificationSettingsUpdateInput.quietStart) &&
        Objects.equals(this.quietEnd, notificationSettingsUpdateInput.quietEnd) &&
        Objects.equals(this.morningTime, notificationSettingsUpdateInput.morningTime) &&
        Objects.equals(this.defaultLanguage, notificationSettingsUpdateInput.defaultLanguage) &&
        Objects.equals(this.lateRecipientUserIds, notificationSettingsUpdateInput.lateRecipientUserIds) &&
        Objects.equals(this.directorUserIds, notificationSettingsUpdateInput.directorUserIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lateEnabled, lateGraceMinutes, absenceEnabled, absenceMinutes, blockCreatedEnabled, blockLastDayEnabled, blockExpiredEnabled, terminalAlertsEnabled, terminalSilenceMinutes, quietHoursEnabled, quietStart, quietEnd, morningTime, defaultLanguage, lateRecipientUserIds, directorUserIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationSettingsUpdateInput {\n");
    sb.append("    lateEnabled: ").append(toIndentedString(lateEnabled)).append("\n");
    sb.append("    lateGraceMinutes: ").append(toIndentedString(lateGraceMinutes)).append("\n");
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

