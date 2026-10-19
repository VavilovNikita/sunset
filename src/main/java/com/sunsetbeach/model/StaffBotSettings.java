package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * What the staff Telegram bot offers and how shift requests behave - the one row V150 seeds, separate from &#x60;NotificationSettings&#x60; (alerts) because it is a different feature with its own defaults. Every threshold, time and switch the bot uses is here; none is a constant in code. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class StaffBotSettings {

  private Boolean myShiftsEnabled;

  private Boolean myDayEnabled;

  private Boolean myMonthEnabled;

  private Boolean myRequestsEnabled;

  private Boolean botSettingsEnabled;

  private Boolean requestsEnabled;

  private Boolean moveRequestsEnabled;

  private Boolean deleteRequestsEnabled;

  private Integer requestMinHoursBefore;

  private Boolean allowTodayRequests;

  private Integer moveWindowDays;

  private Boolean reasonRequired;

  private Integer myShiftsDays;

  private Boolean fallbackToAdmin;

  private Boolean pendingReminderEnabled;

  private Integer pendingReminderHours;

  private Boolean eveReminderEnabled;

  private String eveReminderTime;

  private Boolean eveReminderDefaultOn;

  private Boolean rosterChangeEnabled;

  private Integer rosterChangeHorizonDays;

  private Integer rosterChangeDelayMinutes;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public StaffBotSettings() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StaffBotSettings(Boolean myShiftsEnabled, Boolean myDayEnabled, Boolean myMonthEnabled, Boolean myRequestsEnabled, Boolean botSettingsEnabled, Boolean requestsEnabled, Boolean moveRequestsEnabled, Boolean deleteRequestsEnabled, Integer requestMinHoursBefore, Boolean allowTodayRequests, Integer moveWindowDays, Boolean reasonRequired, Integer myShiftsDays, Boolean fallbackToAdmin, Boolean pendingReminderEnabled, Integer pendingReminderHours, Boolean eveReminderEnabled, String eveReminderTime, Boolean eveReminderDefaultOn, Boolean rosterChangeEnabled, Integer rosterChangeHorizonDays, Integer rosterChangeDelayMinutes, OffsetDateTime updatedAt) {
    this.myShiftsEnabled = myShiftsEnabled;
    this.myDayEnabled = myDayEnabled;
    this.myMonthEnabled = myMonthEnabled;
    this.myRequestsEnabled = myRequestsEnabled;
    this.botSettingsEnabled = botSettingsEnabled;
    this.requestsEnabled = requestsEnabled;
    this.moveRequestsEnabled = moveRequestsEnabled;
    this.deleteRequestsEnabled = deleteRequestsEnabled;
    this.requestMinHoursBefore = requestMinHoursBefore;
    this.allowTodayRequests = allowTodayRequests;
    this.moveWindowDays = moveWindowDays;
    this.reasonRequired = reasonRequired;
    this.myShiftsDays = myShiftsDays;
    this.fallbackToAdmin = fallbackToAdmin;
    this.pendingReminderEnabled = pendingReminderEnabled;
    this.pendingReminderHours = pendingReminderHours;
    this.eveReminderEnabled = eveReminderEnabled;
    this.eveReminderTime = eveReminderTime;
    this.eveReminderDefaultOn = eveReminderDefaultOn;
    this.rosterChangeEnabled = rosterChangeEnabled;
    this.rosterChangeHorizonDays = rosterChangeHorizonDays;
    this.rosterChangeDelayMinutes = rosterChangeDelayMinutes;
    this.updatedAt = updatedAt;
  }

  public StaffBotSettings myShiftsEnabled(Boolean myShiftsEnabled) {
    this.myShiftsEnabled = myShiftsEnabled;
    return this;
  }

  /**
   * The \"My shifts\" command.
   * @return myShiftsEnabled
   */
  @NotNull 
  @JsonProperty("myShiftsEnabled")
  public Boolean getMyShiftsEnabled() {
    return myShiftsEnabled;
  }

  public void setMyShiftsEnabled(Boolean myShiftsEnabled) {
    this.myShiftsEnabled = myShiftsEnabled;
  }

  public StaffBotSettings myDayEnabled(Boolean myDayEnabled) {
    this.myDayEnabled = myDayEnabled;
    return this;
  }

  /**
   * The \"My day\" command (today's shift and the employee's own punches).
   * @return myDayEnabled
   */
  @NotNull 
  @JsonProperty("myDayEnabled")
  public Boolean getMyDayEnabled() {
    return myDayEnabled;
  }

  public void setMyDayEnabled(Boolean myDayEnabled) {
    this.myDayEnabled = myDayEnabled;
  }

  public StaffBotSettings myMonthEnabled(Boolean myMonthEnabled) {
    this.myMonthEnabled = myMonthEnabled;
    return this;
  }

  /**
   * The \"My month\" command (planned and worked days and hours).
   * @return myMonthEnabled
   */
  @NotNull 
  @JsonProperty("myMonthEnabled")
  public Boolean getMyMonthEnabled() {
    return myMonthEnabled;
  }

  public void setMyMonthEnabled(Boolean myMonthEnabled) {
    this.myMonthEnabled = myMonthEnabled;
  }

  public StaffBotSettings myRequestsEnabled(Boolean myRequestsEnabled) {
    this.myRequestsEnabled = myRequestsEnabled;
    return this;
  }

  /**
   * The \"My requests\" command.
   * @return myRequestsEnabled
   */
  @NotNull 
  @JsonProperty("myRequestsEnabled")
  public Boolean getMyRequestsEnabled() {
    return myRequestsEnabled;
  }

  public void setMyRequestsEnabled(Boolean myRequestsEnabled) {
    this.myRequestsEnabled = myRequestsEnabled;
  }

  public StaffBotSettings botSettingsEnabled(Boolean botSettingsEnabled) {
    this.botSettingsEnabled = botSettingsEnabled;
    return this;
  }

  /**
   * The bot's own \"Settings\" command (language, evening-before reminder).
   * @return botSettingsEnabled
   */
  @NotNull 
  @JsonProperty("botSettingsEnabled")
  public Boolean getBotSettingsEnabled() {
    return botSettingsEnabled;
  }

  public void setBotSettingsEnabled(Boolean botSettingsEnabled) {
    this.botSettingsEnabled = botSettingsEnabled;
  }

  public StaffBotSettings requestsEnabled(Boolean requestsEnabled) {
    this.requestsEnabled = requestsEnabled;
    return this;
  }

  /**
   * Master switch for shift requests - filing, the deciders' list and the messages about them.
   * @return requestsEnabled
   */
  @NotNull 
  @JsonProperty("requestsEnabled")
  public Boolean getRequestsEnabled() {
    return requestsEnabled;
  }

  public void setRequestsEnabled(Boolean requestsEnabled) {
    this.requestsEnabled = requestsEnabled;
  }

  public StaffBotSettings moveRequestsEnabled(Boolean moveRequestsEnabled) {
    this.moveRequestsEnabled = moveRequestsEnabled;
    return this;
  }

  /**
   * Get moveRequestsEnabled
   * @return moveRequestsEnabled
   */
  @NotNull 
  @JsonProperty("moveRequestsEnabled")
  public Boolean getMoveRequestsEnabled() {
    return moveRequestsEnabled;
  }

  public void setMoveRequestsEnabled(Boolean moveRequestsEnabled) {
    this.moveRequestsEnabled = moveRequestsEnabled;
  }

  public StaffBotSettings deleteRequestsEnabled(Boolean deleteRequestsEnabled) {
    this.deleteRequestsEnabled = deleteRequestsEnabled;
    return this;
  }

  /**
   * Get deleteRequestsEnabled
   * @return deleteRequestsEnabled
   */
  @NotNull 
  @JsonProperty("deleteRequestsEnabled")
  public Boolean getDeleteRequestsEnabled() {
    return deleteRequestsEnabled;
  }

  public void setDeleteRequestsEnabled(Boolean deleteRequestsEnabled) {
    this.deleteRequestsEnabled = deleteRequestsEnabled;
  }

  public StaffBotSettings requestMinHoursBefore(Integer requestMinHoursBefore) {
    this.requestMinHoursBefore = requestMinHoursBefore;
    return this;
  }

  /**
   * A request must be filed at least this many hours before the shift starts (default 24).
   * minimum: 0
   * maximum: 720
   * @return requestMinHoursBefore
   */
  @NotNull @Min(0) @Max(720) 
  @JsonProperty("requestMinHoursBefore")
  public Integer getRequestMinHoursBefore() {
    return requestMinHoursBefore;
  }

  public void setRequestMinHoursBefore(Integer requestMinHoursBefore) {
    this.requestMinHoursBefore = requestMinHoursBefore;
  }

  public StaffBotSettings allowTodayRequests(Boolean allowTodayRequests) {
    this.allowTodayRequests = allowTodayRequests;
    return this;
  }

  /**
   * Allow a request for today's shift (still subject to `requestMinHoursBefore`). Default off.
   * @return allowTodayRequests
   */
  @NotNull 
  @JsonProperty("allowTodayRequests")
  public Boolean getAllowTodayRequests() {
    return allowTodayRequests;
  }

  public void setAllowTodayRequests(Boolean allowTodayRequests) {
    this.allowTodayRequests = allowTodayRequests;
  }

  public StaffBotSettings moveWindowDays(Integer moveWindowDays) {
    this.moveWindowDays = moveWindowDays;
    return this;
  }

  /**
   * A shift can be moved to a day at most this many days ahead (default 14).
   * minimum: 1
   * maximum: 90
   * @return moveWindowDays
   */
  @NotNull @Min(1) @Max(90) 
  @JsonProperty("moveWindowDays")
  public Integer getMoveWindowDays() {
    return moveWindowDays;
  }

  public void setMoveWindowDays(Integer moveWindowDays) {
    this.moveWindowDays = moveWindowDays;
  }

  public StaffBotSettings reasonRequired(Boolean reasonRequired) {
    this.reasonRequired = reasonRequired;
    return this;
  }

  /**
   * The employee must give a reason (up to 300 characters). Default on.
   * @return reasonRequired
   */
  @NotNull 
  @JsonProperty("reasonRequired")
  public Boolean getReasonRequired() {
    return reasonRequired;
  }

  public void setReasonRequired(Boolean reasonRequired) {
    this.reasonRequired = reasonRequired;
  }

  public StaffBotSettings myShiftsDays(Integer myShiftsDays) {
    this.myShiftsDays = myShiftsDays;
    return this;
  }

  /**
   * How many days ahead \"My shifts\" shows (default 14).
   * minimum: 1
   * maximum: 60
   * @return myShiftsDays
   */
  @NotNull @Min(1) @Max(60) 
  @JsonProperty("myShiftsDays")
  public Integer getMyShiftsDays() {
    return myShiftsDays;
  }

  public void setMyShiftsDays(Integer myShiftsDays) {
    this.myShiftsDays = myShiftsDays;
  }

  public StaffBotSettings fallbackToAdmin(Boolean fallbackToAdmin) {
    this.fallbackToAdmin = fallbackToAdmin;
    return this;
  }

  /**
   * Administrators decide while nobody holds `HR` or the holder is on leave (default on).
   * @return fallbackToAdmin
   */
  @NotNull 
  @JsonProperty("fallbackToAdmin")
  public Boolean getFallbackToAdmin() {
    return fallbackToAdmin;
  }

  public void setFallbackToAdmin(Boolean fallbackToAdmin) {
    this.fallbackToAdmin = fallbackToAdmin;
  }

  public StaffBotSettings pendingReminderEnabled(Boolean pendingReminderEnabled) {
    this.pendingReminderEnabled = pendingReminderEnabled;
    return this;
  }

  /**
   * Get pendingReminderEnabled
   * @return pendingReminderEnabled
   */
  @NotNull 
  @JsonProperty("pendingReminderEnabled")
  public Boolean getPendingReminderEnabled() {
    return pendingReminderEnabled;
  }

  public void setPendingReminderEnabled(Boolean pendingReminderEnabled) {
    this.pendingReminderEnabled = pendingReminderEnabled;
  }

  public StaffBotSettings pendingReminderHours(Integer pendingReminderHours) {
    this.pendingReminderHours = pendingReminderHours;
    return this;
  }

  /**
   * Remind the deciders of a request waiting this long, at most once a day per request (default 12).
   * minimum: 1
   * maximum: 168
   * @return pendingReminderHours
   */
  @NotNull @Min(1) @Max(168) 
  @JsonProperty("pendingReminderHours")
  public Integer getPendingReminderHours() {
    return pendingReminderHours;
  }

  public void setPendingReminderHours(Integer pendingReminderHours) {
    this.pendingReminderHours = pendingReminderHours;
  }

  public StaffBotSettings eveReminderEnabled(Boolean eveReminderEnabled) {
    this.eveReminderEnabled = eveReminderEnabled;
    return this;
  }

  /**
   * The evening-before reminder feature as a whole.
   * @return eveReminderEnabled
   */
  @NotNull 
  @JsonProperty("eveReminderEnabled")
  public Boolean getEveReminderEnabled() {
    return eveReminderEnabled;
  }

  public void setEveReminderEnabled(Boolean eveReminderEnabled) {
    this.eveReminderEnabled = eveReminderEnabled;
  }

  public StaffBotSettings eveReminderTime(String eveReminderTime) {
    this.eveReminderTime = eveReminderTime;
    return this;
  }

  /**
   * Hotel time, `HH:mm` (default 18:00).
   * @return eveReminderTime
   */
  @NotNull 
  @JsonProperty("eveReminderTime")
  public String getEveReminderTime() {
    return eveReminderTime;
  }

  public void setEveReminderTime(String eveReminderTime) {
    this.eveReminderTime = eveReminderTime;
  }

  public StaffBotSettings eveReminderDefaultOn(Boolean eveReminderDefaultOn) {
    this.eveReminderDefaultOn = eveReminderDefaultOn;
    return this;
  }

  /**
   * Whether an employee who never chose gets the reminder (default off). Each employee overrides it in the bot.
   * @return eveReminderDefaultOn
   */
  @NotNull 
  @JsonProperty("eveReminderDefaultOn")
  public Boolean getEveReminderDefaultOn() {
    return eveReminderDefaultOn;
  }

  public void setEveReminderDefaultOn(Boolean eveReminderDefaultOn) {
    this.eveReminderDefaultOn = eveReminderDefaultOn;
  }

  public StaffBotSettings rosterChangeEnabled(Boolean rosterChangeEnabled) {
    this.rosterChangeEnabled = rosterChangeEnabled;
    return this;
  }

  /**
   * Tell an employee when a manager changed their shift.
   * @return rosterChangeEnabled
   */
  @NotNull 
  @JsonProperty("rosterChangeEnabled")
  public Boolean getRosterChangeEnabled() {
    return rosterChangeEnabled;
  }

  public void setRosterChangeEnabled(Boolean rosterChangeEnabled) {
    this.rosterChangeEnabled = rosterChangeEnabled;
  }

  public StaffBotSettings rosterChangeHorizonDays(Integer rosterChangeHorizonDays) {
    this.rosterChangeHorizonDays = rosterChangeHorizonDays;
    return this;
  }

  /**
   * Only changes to shifts within this many days ahead are announced (default 14).
   * minimum: 1
   * maximum: 90
   * @return rosterChangeHorizonDays
   */
  @NotNull @Min(1) @Max(90) 
  @JsonProperty("rosterChangeHorizonDays")
  public Integer getRosterChangeHorizonDays() {
    return rosterChangeHorizonDays;
  }

  public void setRosterChangeHorizonDays(Integer rosterChangeHorizonDays) {
    this.rosterChangeHorizonDays = rosterChangeHorizonDays;
  }

  public StaffBotSettings rosterChangeDelayMinutes(Integer rosterChangeDelayMinutes) {
    this.rosterChangeDelayMinutes = rosterChangeDelayMinutes;
    return this;
  }

  /**
   * A change is announced this long after the last change to that employee and day, as one message with the final state (default 10).
   * minimum: 1
   * maximum: 240
   * @return rosterChangeDelayMinutes
   */
  @NotNull @Min(1) @Max(240) 
  @JsonProperty("rosterChangeDelayMinutes")
  public Integer getRosterChangeDelayMinutes() {
    return rosterChangeDelayMinutes;
  }

  public void setRosterChangeDelayMinutes(Integer rosterChangeDelayMinutes) {
    this.rosterChangeDelayMinutes = rosterChangeDelayMinutes;
  }

  public StaffBotSettings updatedAt(OffsetDateTime updatedAt) {
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
    StaffBotSettings staffBotSettings = (StaffBotSettings) o;
    return Objects.equals(this.myShiftsEnabled, staffBotSettings.myShiftsEnabled) &&
        Objects.equals(this.myDayEnabled, staffBotSettings.myDayEnabled) &&
        Objects.equals(this.myMonthEnabled, staffBotSettings.myMonthEnabled) &&
        Objects.equals(this.myRequestsEnabled, staffBotSettings.myRequestsEnabled) &&
        Objects.equals(this.botSettingsEnabled, staffBotSettings.botSettingsEnabled) &&
        Objects.equals(this.requestsEnabled, staffBotSettings.requestsEnabled) &&
        Objects.equals(this.moveRequestsEnabled, staffBotSettings.moveRequestsEnabled) &&
        Objects.equals(this.deleteRequestsEnabled, staffBotSettings.deleteRequestsEnabled) &&
        Objects.equals(this.requestMinHoursBefore, staffBotSettings.requestMinHoursBefore) &&
        Objects.equals(this.allowTodayRequests, staffBotSettings.allowTodayRequests) &&
        Objects.equals(this.moveWindowDays, staffBotSettings.moveWindowDays) &&
        Objects.equals(this.reasonRequired, staffBotSettings.reasonRequired) &&
        Objects.equals(this.myShiftsDays, staffBotSettings.myShiftsDays) &&
        Objects.equals(this.fallbackToAdmin, staffBotSettings.fallbackToAdmin) &&
        Objects.equals(this.pendingReminderEnabled, staffBotSettings.pendingReminderEnabled) &&
        Objects.equals(this.pendingReminderHours, staffBotSettings.pendingReminderHours) &&
        Objects.equals(this.eveReminderEnabled, staffBotSettings.eveReminderEnabled) &&
        Objects.equals(this.eveReminderTime, staffBotSettings.eveReminderTime) &&
        Objects.equals(this.eveReminderDefaultOn, staffBotSettings.eveReminderDefaultOn) &&
        Objects.equals(this.rosterChangeEnabled, staffBotSettings.rosterChangeEnabled) &&
        Objects.equals(this.rosterChangeHorizonDays, staffBotSettings.rosterChangeHorizonDays) &&
        Objects.equals(this.rosterChangeDelayMinutes, staffBotSettings.rosterChangeDelayMinutes) &&
        Objects.equals(this.updatedAt, staffBotSettings.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myShiftsEnabled, myDayEnabled, myMonthEnabled, myRequestsEnabled, botSettingsEnabled, requestsEnabled, moveRequestsEnabled, deleteRequestsEnabled, requestMinHoursBefore, allowTodayRequests, moveWindowDays, reasonRequired, myShiftsDays, fallbackToAdmin, pendingReminderEnabled, pendingReminderHours, eveReminderEnabled, eveReminderTime, eveReminderDefaultOn, rosterChangeEnabled, rosterChangeHorizonDays, rosterChangeDelayMinutes, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffBotSettings {\n");
    sb.append("    myShiftsEnabled: ").append(toIndentedString(myShiftsEnabled)).append("\n");
    sb.append("    myDayEnabled: ").append(toIndentedString(myDayEnabled)).append("\n");
    sb.append("    myMonthEnabled: ").append(toIndentedString(myMonthEnabled)).append("\n");
    sb.append("    myRequestsEnabled: ").append(toIndentedString(myRequestsEnabled)).append("\n");
    sb.append("    botSettingsEnabled: ").append(toIndentedString(botSettingsEnabled)).append("\n");
    sb.append("    requestsEnabled: ").append(toIndentedString(requestsEnabled)).append("\n");
    sb.append("    moveRequestsEnabled: ").append(toIndentedString(moveRequestsEnabled)).append("\n");
    sb.append("    deleteRequestsEnabled: ").append(toIndentedString(deleteRequestsEnabled)).append("\n");
    sb.append("    requestMinHoursBefore: ").append(toIndentedString(requestMinHoursBefore)).append("\n");
    sb.append("    allowTodayRequests: ").append(toIndentedString(allowTodayRequests)).append("\n");
    sb.append("    moveWindowDays: ").append(toIndentedString(moveWindowDays)).append("\n");
    sb.append("    reasonRequired: ").append(toIndentedString(reasonRequired)).append("\n");
    sb.append("    myShiftsDays: ").append(toIndentedString(myShiftsDays)).append("\n");
    sb.append("    fallbackToAdmin: ").append(toIndentedString(fallbackToAdmin)).append("\n");
    sb.append("    pendingReminderEnabled: ").append(toIndentedString(pendingReminderEnabled)).append("\n");
    sb.append("    pendingReminderHours: ").append(toIndentedString(pendingReminderHours)).append("\n");
    sb.append("    eveReminderEnabled: ").append(toIndentedString(eveReminderEnabled)).append("\n");
    sb.append("    eveReminderTime: ").append(toIndentedString(eveReminderTime)).append("\n");
    sb.append("    eveReminderDefaultOn: ").append(toIndentedString(eveReminderDefaultOn)).append("\n");
    sb.append("    rosterChangeEnabled: ").append(toIndentedString(rosterChangeEnabled)).append("\n");
    sb.append("    rosterChangeHorizonDays: ").append(toIndentedString(rosterChangeHorizonDays)).append("\n");
    sb.append("    rosterChangeDelayMinutes: ").append(toIndentedString(rosterChangeDelayMinutes)).append("\n");
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

