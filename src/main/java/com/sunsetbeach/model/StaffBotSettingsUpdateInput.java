package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PUT /settings/staff-bot&#x60; - every field of &#x60;StaffBotSettings&#x60; except &#x60;updatedAt&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class StaffBotSettingsUpdateInput {

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

  public StaffBotSettingsUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StaffBotSettingsUpdateInput(Boolean myShiftsEnabled, Boolean myDayEnabled, Boolean myMonthEnabled, Boolean myRequestsEnabled, Boolean botSettingsEnabled, Boolean requestsEnabled, Boolean moveRequestsEnabled, Boolean deleteRequestsEnabled, Integer requestMinHoursBefore, Boolean allowTodayRequests, Integer moveWindowDays, Boolean reasonRequired, Integer myShiftsDays, Boolean fallbackToAdmin, Boolean pendingReminderEnabled, Integer pendingReminderHours, Boolean eveReminderEnabled, String eveReminderTime, Boolean eveReminderDefaultOn, Boolean rosterChangeEnabled, Integer rosterChangeHorizonDays, Integer rosterChangeDelayMinutes) {
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
  }

  public StaffBotSettingsUpdateInput myShiftsEnabled(Boolean myShiftsEnabled) {
    this.myShiftsEnabled = myShiftsEnabled;
    return this;
  }

  /**
   * Get myShiftsEnabled
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

  public StaffBotSettingsUpdateInput myDayEnabled(Boolean myDayEnabled) {
    this.myDayEnabled = myDayEnabled;
    return this;
  }

  /**
   * Get myDayEnabled
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

  public StaffBotSettingsUpdateInput myMonthEnabled(Boolean myMonthEnabled) {
    this.myMonthEnabled = myMonthEnabled;
    return this;
  }

  /**
   * Get myMonthEnabled
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

  public StaffBotSettingsUpdateInput myRequestsEnabled(Boolean myRequestsEnabled) {
    this.myRequestsEnabled = myRequestsEnabled;
    return this;
  }

  /**
   * Get myRequestsEnabled
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

  public StaffBotSettingsUpdateInput botSettingsEnabled(Boolean botSettingsEnabled) {
    this.botSettingsEnabled = botSettingsEnabled;
    return this;
  }

  /**
   * Get botSettingsEnabled
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

  public StaffBotSettingsUpdateInput requestsEnabled(Boolean requestsEnabled) {
    this.requestsEnabled = requestsEnabled;
    return this;
  }

  /**
   * Get requestsEnabled
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

  public StaffBotSettingsUpdateInput moveRequestsEnabled(Boolean moveRequestsEnabled) {
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

  public StaffBotSettingsUpdateInput deleteRequestsEnabled(Boolean deleteRequestsEnabled) {
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

  public StaffBotSettingsUpdateInput requestMinHoursBefore(Integer requestMinHoursBefore) {
    this.requestMinHoursBefore = requestMinHoursBefore;
    return this;
  }

  /**
   * Get requestMinHoursBefore
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

  public StaffBotSettingsUpdateInput allowTodayRequests(Boolean allowTodayRequests) {
    this.allowTodayRequests = allowTodayRequests;
    return this;
  }

  /**
   * Get allowTodayRequests
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

  public StaffBotSettingsUpdateInput moveWindowDays(Integer moveWindowDays) {
    this.moveWindowDays = moveWindowDays;
    return this;
  }

  /**
   * Get moveWindowDays
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

  public StaffBotSettingsUpdateInput reasonRequired(Boolean reasonRequired) {
    this.reasonRequired = reasonRequired;
    return this;
  }

  /**
   * Get reasonRequired
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

  public StaffBotSettingsUpdateInput myShiftsDays(Integer myShiftsDays) {
    this.myShiftsDays = myShiftsDays;
    return this;
  }

  /**
   * Get myShiftsDays
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

  public StaffBotSettingsUpdateInput fallbackToAdmin(Boolean fallbackToAdmin) {
    this.fallbackToAdmin = fallbackToAdmin;
    return this;
  }

  /**
   * Get fallbackToAdmin
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

  public StaffBotSettingsUpdateInput pendingReminderEnabled(Boolean pendingReminderEnabled) {
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

  public StaffBotSettingsUpdateInput pendingReminderHours(Integer pendingReminderHours) {
    this.pendingReminderHours = pendingReminderHours;
    return this;
  }

  /**
   * Get pendingReminderHours
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

  public StaffBotSettingsUpdateInput eveReminderEnabled(Boolean eveReminderEnabled) {
    this.eveReminderEnabled = eveReminderEnabled;
    return this;
  }

  /**
   * Get eveReminderEnabled
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

  public StaffBotSettingsUpdateInput eveReminderTime(String eveReminderTime) {
    this.eveReminderTime = eveReminderTime;
    return this;
  }

  /**
   * Get eveReminderTime
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

  public StaffBotSettingsUpdateInput eveReminderDefaultOn(Boolean eveReminderDefaultOn) {
    this.eveReminderDefaultOn = eveReminderDefaultOn;
    return this;
  }

  /**
   * Get eveReminderDefaultOn
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

  public StaffBotSettingsUpdateInput rosterChangeEnabled(Boolean rosterChangeEnabled) {
    this.rosterChangeEnabled = rosterChangeEnabled;
    return this;
  }

  /**
   * Get rosterChangeEnabled
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

  public StaffBotSettingsUpdateInput rosterChangeHorizonDays(Integer rosterChangeHorizonDays) {
    this.rosterChangeHorizonDays = rosterChangeHorizonDays;
    return this;
  }

  /**
   * Get rosterChangeHorizonDays
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

  public StaffBotSettingsUpdateInput rosterChangeDelayMinutes(Integer rosterChangeDelayMinutes) {
    this.rosterChangeDelayMinutes = rosterChangeDelayMinutes;
    return this;
  }

  /**
   * Get rosterChangeDelayMinutes
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StaffBotSettingsUpdateInput staffBotSettingsUpdateInput = (StaffBotSettingsUpdateInput) o;
    return Objects.equals(this.myShiftsEnabled, staffBotSettingsUpdateInput.myShiftsEnabled) &&
        Objects.equals(this.myDayEnabled, staffBotSettingsUpdateInput.myDayEnabled) &&
        Objects.equals(this.myMonthEnabled, staffBotSettingsUpdateInput.myMonthEnabled) &&
        Objects.equals(this.myRequestsEnabled, staffBotSettingsUpdateInput.myRequestsEnabled) &&
        Objects.equals(this.botSettingsEnabled, staffBotSettingsUpdateInput.botSettingsEnabled) &&
        Objects.equals(this.requestsEnabled, staffBotSettingsUpdateInput.requestsEnabled) &&
        Objects.equals(this.moveRequestsEnabled, staffBotSettingsUpdateInput.moveRequestsEnabled) &&
        Objects.equals(this.deleteRequestsEnabled, staffBotSettingsUpdateInput.deleteRequestsEnabled) &&
        Objects.equals(this.requestMinHoursBefore, staffBotSettingsUpdateInput.requestMinHoursBefore) &&
        Objects.equals(this.allowTodayRequests, staffBotSettingsUpdateInput.allowTodayRequests) &&
        Objects.equals(this.moveWindowDays, staffBotSettingsUpdateInput.moveWindowDays) &&
        Objects.equals(this.reasonRequired, staffBotSettingsUpdateInput.reasonRequired) &&
        Objects.equals(this.myShiftsDays, staffBotSettingsUpdateInput.myShiftsDays) &&
        Objects.equals(this.fallbackToAdmin, staffBotSettingsUpdateInput.fallbackToAdmin) &&
        Objects.equals(this.pendingReminderEnabled, staffBotSettingsUpdateInput.pendingReminderEnabled) &&
        Objects.equals(this.pendingReminderHours, staffBotSettingsUpdateInput.pendingReminderHours) &&
        Objects.equals(this.eveReminderEnabled, staffBotSettingsUpdateInput.eveReminderEnabled) &&
        Objects.equals(this.eveReminderTime, staffBotSettingsUpdateInput.eveReminderTime) &&
        Objects.equals(this.eveReminderDefaultOn, staffBotSettingsUpdateInput.eveReminderDefaultOn) &&
        Objects.equals(this.rosterChangeEnabled, staffBotSettingsUpdateInput.rosterChangeEnabled) &&
        Objects.equals(this.rosterChangeHorizonDays, staffBotSettingsUpdateInput.rosterChangeHorizonDays) &&
        Objects.equals(this.rosterChangeDelayMinutes, staffBotSettingsUpdateInput.rosterChangeDelayMinutes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myShiftsEnabled, myDayEnabled, myMonthEnabled, myRequestsEnabled, botSettingsEnabled, requestsEnabled, moveRequestsEnabled, deleteRequestsEnabled, requestMinHoursBefore, allowTodayRequests, moveWindowDays, reasonRequired, myShiftsDays, fallbackToAdmin, pendingReminderEnabled, pendingReminderHours, eveReminderEnabled, eveReminderTime, eveReminderDefaultOn, rosterChangeEnabled, rosterChangeHorizonDays, rosterChangeDelayMinutes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffBotSettingsUpdateInput {\n");
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

