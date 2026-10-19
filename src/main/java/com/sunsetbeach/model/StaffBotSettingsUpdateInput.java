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

  private Boolean engineerTasksEnabled;

  private Boolean housekeeperBlocksEnabled;

  private Integer housekeeperBlocksDays;

  private Boolean therapistScheduleEnabled;

  private Integer therapistScheduleDays;

  private Boolean therapistMorningEnabled;

  private String therapistMorningTime;

  private Boolean therapistMorningDefaultOn;

  private Boolean whoIsOnShiftEnabled;

  private Boolean whoIsOnShiftForHr;

  private Boolean summaryCommandEnabled;

  private Boolean morningSummaryEnabled;

  private String morningSummaryTime;

  private Boolean summaryArrivals;

  private Boolean summaryDepartures;

  private Boolean summaryInHouse;

  private Boolean summaryOccupancy;

  private Boolean summaryRooms;

  private Boolean summaryStaff;

  private Boolean summaryRequests;

  public StaffBotSettingsUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StaffBotSettingsUpdateInput(Boolean myShiftsEnabled, Boolean myDayEnabled, Boolean myMonthEnabled, Boolean myRequestsEnabled, Boolean botSettingsEnabled, Boolean requestsEnabled, Boolean moveRequestsEnabled, Boolean deleteRequestsEnabled, Integer requestMinHoursBefore, Boolean allowTodayRequests, Integer moveWindowDays, Boolean reasonRequired, Integer myShiftsDays, Boolean fallbackToAdmin, Boolean pendingReminderEnabled, Integer pendingReminderHours, Boolean eveReminderEnabled, String eveReminderTime, Boolean eveReminderDefaultOn, Boolean rosterChangeEnabled, Integer rosterChangeHorizonDays, Integer rosterChangeDelayMinutes, Boolean engineerTasksEnabled, Boolean housekeeperBlocksEnabled, Integer housekeeperBlocksDays, Boolean therapistScheduleEnabled, Integer therapistScheduleDays, Boolean therapistMorningEnabled, String therapistMorningTime, Boolean therapistMorningDefaultOn, Boolean whoIsOnShiftEnabled, Boolean whoIsOnShiftForHr, Boolean summaryCommandEnabled, Boolean morningSummaryEnabled, String morningSummaryTime, Boolean summaryArrivals, Boolean summaryDepartures, Boolean summaryInHouse, Boolean summaryOccupancy, Boolean summaryRooms, Boolean summaryStaff, Boolean summaryRequests) {
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
    this.engineerTasksEnabled = engineerTasksEnabled;
    this.housekeeperBlocksEnabled = housekeeperBlocksEnabled;
    this.housekeeperBlocksDays = housekeeperBlocksDays;
    this.therapistScheduleEnabled = therapistScheduleEnabled;
    this.therapistScheduleDays = therapistScheduleDays;
    this.therapistMorningEnabled = therapistMorningEnabled;
    this.therapistMorningTime = therapistMorningTime;
    this.therapistMorningDefaultOn = therapistMorningDefaultOn;
    this.whoIsOnShiftEnabled = whoIsOnShiftEnabled;
    this.whoIsOnShiftForHr = whoIsOnShiftForHr;
    this.summaryCommandEnabled = summaryCommandEnabled;
    this.morningSummaryEnabled = morningSummaryEnabled;
    this.morningSummaryTime = morningSummaryTime;
    this.summaryArrivals = summaryArrivals;
    this.summaryDepartures = summaryDepartures;
    this.summaryInHouse = summaryInHouse;
    this.summaryOccupancy = summaryOccupancy;
    this.summaryRooms = summaryRooms;
    this.summaryStaff = summaryStaff;
    this.summaryRequests = summaryRequests;
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

  public StaffBotSettingsUpdateInput engineerTasksEnabled(Boolean engineerTasksEnabled) {
    this.engineerTasksEnabled = engineerTasksEnabled;
    return this;
  }

  /**
   * Get engineerTasksEnabled
   * @return engineerTasksEnabled
   */
  @NotNull 
  @JsonProperty("engineerTasksEnabled")
  public Boolean getEngineerTasksEnabled() {
    return engineerTasksEnabled;
  }

  public void setEngineerTasksEnabled(Boolean engineerTasksEnabled) {
    this.engineerTasksEnabled = engineerTasksEnabled;
  }

  public StaffBotSettingsUpdateInput housekeeperBlocksEnabled(Boolean housekeeperBlocksEnabled) {
    this.housekeeperBlocksEnabled = housekeeperBlocksEnabled;
    return this;
  }

  /**
   * Get housekeeperBlocksEnabled
   * @return housekeeperBlocksEnabled
   */
  @NotNull 
  @JsonProperty("housekeeperBlocksEnabled")
  public Boolean getHousekeeperBlocksEnabled() {
    return housekeeperBlocksEnabled;
  }

  public void setHousekeeperBlocksEnabled(Boolean housekeeperBlocksEnabled) {
    this.housekeeperBlocksEnabled = housekeeperBlocksEnabled;
  }

  public StaffBotSettingsUpdateInput housekeeperBlocksDays(Integer housekeeperBlocksDays) {
    this.housekeeperBlocksDays = housekeeperBlocksDays;
    return this;
  }

  /**
   * Get housekeeperBlocksDays
   * minimum: 1
   * maximum: 60
   * @return housekeeperBlocksDays
   */
  @NotNull @Min(1) @Max(60) 
  @JsonProperty("housekeeperBlocksDays")
  public Integer getHousekeeperBlocksDays() {
    return housekeeperBlocksDays;
  }

  public void setHousekeeperBlocksDays(Integer housekeeperBlocksDays) {
    this.housekeeperBlocksDays = housekeeperBlocksDays;
  }

  public StaffBotSettingsUpdateInput therapistScheduleEnabled(Boolean therapistScheduleEnabled) {
    this.therapistScheduleEnabled = therapistScheduleEnabled;
    return this;
  }

  /**
   * Get therapistScheduleEnabled
   * @return therapistScheduleEnabled
   */
  @NotNull 
  @JsonProperty("therapistScheduleEnabled")
  public Boolean getTherapistScheduleEnabled() {
    return therapistScheduleEnabled;
  }

  public void setTherapistScheduleEnabled(Boolean therapistScheduleEnabled) {
    this.therapistScheduleEnabled = therapistScheduleEnabled;
  }

  public StaffBotSettingsUpdateInput therapistScheduleDays(Integer therapistScheduleDays) {
    this.therapistScheduleDays = therapistScheduleDays;
    return this;
  }

  /**
   * Get therapistScheduleDays
   * minimum: 1
   * maximum: 14
   * @return therapistScheduleDays
   */
  @NotNull @Min(1) @Max(14) 
  @JsonProperty("therapistScheduleDays")
  public Integer getTherapistScheduleDays() {
    return therapistScheduleDays;
  }

  public void setTherapistScheduleDays(Integer therapistScheduleDays) {
    this.therapistScheduleDays = therapistScheduleDays;
  }

  public StaffBotSettingsUpdateInput therapistMorningEnabled(Boolean therapistMorningEnabled) {
    this.therapistMorningEnabled = therapistMorningEnabled;
    return this;
  }

  /**
   * Get therapistMorningEnabled
   * @return therapistMorningEnabled
   */
  @NotNull 
  @JsonProperty("therapistMorningEnabled")
  public Boolean getTherapistMorningEnabled() {
    return therapistMorningEnabled;
  }

  public void setTherapistMorningEnabled(Boolean therapistMorningEnabled) {
    this.therapistMorningEnabled = therapistMorningEnabled;
  }

  public StaffBotSettingsUpdateInput therapistMorningTime(String therapistMorningTime) {
    this.therapistMorningTime = therapistMorningTime;
    return this;
  }

  /**
   * Get therapistMorningTime
   * @return therapistMorningTime
   */
  @NotNull 
  @JsonProperty("therapistMorningTime")
  public String getTherapistMorningTime() {
    return therapistMorningTime;
  }

  public void setTherapistMorningTime(String therapistMorningTime) {
    this.therapistMorningTime = therapistMorningTime;
  }

  public StaffBotSettingsUpdateInput therapistMorningDefaultOn(Boolean therapistMorningDefaultOn) {
    this.therapistMorningDefaultOn = therapistMorningDefaultOn;
    return this;
  }

  /**
   * Get therapistMorningDefaultOn
   * @return therapistMorningDefaultOn
   */
  @NotNull 
  @JsonProperty("therapistMorningDefaultOn")
  public Boolean getTherapistMorningDefaultOn() {
    return therapistMorningDefaultOn;
  }

  public void setTherapistMorningDefaultOn(Boolean therapistMorningDefaultOn) {
    this.therapistMorningDefaultOn = therapistMorningDefaultOn;
  }

  public StaffBotSettingsUpdateInput whoIsOnShiftEnabled(Boolean whoIsOnShiftEnabled) {
    this.whoIsOnShiftEnabled = whoIsOnShiftEnabled;
    return this;
  }

  /**
   * Get whoIsOnShiftEnabled
   * @return whoIsOnShiftEnabled
   */
  @NotNull 
  @JsonProperty("whoIsOnShiftEnabled")
  public Boolean getWhoIsOnShiftEnabled() {
    return whoIsOnShiftEnabled;
  }

  public void setWhoIsOnShiftEnabled(Boolean whoIsOnShiftEnabled) {
    this.whoIsOnShiftEnabled = whoIsOnShiftEnabled;
  }

  public StaffBotSettingsUpdateInput whoIsOnShiftForHr(Boolean whoIsOnShiftForHr) {
    this.whoIsOnShiftForHr = whoIsOnShiftForHr;
    return this;
  }

  /**
   * Get whoIsOnShiftForHr
   * @return whoIsOnShiftForHr
   */
  @NotNull 
  @JsonProperty("whoIsOnShiftForHr")
  public Boolean getWhoIsOnShiftForHr() {
    return whoIsOnShiftForHr;
  }

  public void setWhoIsOnShiftForHr(Boolean whoIsOnShiftForHr) {
    this.whoIsOnShiftForHr = whoIsOnShiftForHr;
  }

  public StaffBotSettingsUpdateInput summaryCommandEnabled(Boolean summaryCommandEnabled) {
    this.summaryCommandEnabled = summaryCommandEnabled;
    return this;
  }

  /**
   * Get summaryCommandEnabled
   * @return summaryCommandEnabled
   */
  @NotNull 
  @JsonProperty("summaryCommandEnabled")
  public Boolean getSummaryCommandEnabled() {
    return summaryCommandEnabled;
  }

  public void setSummaryCommandEnabled(Boolean summaryCommandEnabled) {
    this.summaryCommandEnabled = summaryCommandEnabled;
  }

  public StaffBotSettingsUpdateInput morningSummaryEnabled(Boolean morningSummaryEnabled) {
    this.morningSummaryEnabled = morningSummaryEnabled;
    return this;
  }

  /**
   * Get morningSummaryEnabled
   * @return morningSummaryEnabled
   */
  @NotNull 
  @JsonProperty("morningSummaryEnabled")
  public Boolean getMorningSummaryEnabled() {
    return morningSummaryEnabled;
  }

  public void setMorningSummaryEnabled(Boolean morningSummaryEnabled) {
    this.morningSummaryEnabled = morningSummaryEnabled;
  }

  public StaffBotSettingsUpdateInput morningSummaryTime(String morningSummaryTime) {
    this.morningSummaryTime = morningSummaryTime;
    return this;
  }

  /**
   * Get morningSummaryTime
   * @return morningSummaryTime
   */
  @NotNull 
  @JsonProperty("morningSummaryTime")
  public String getMorningSummaryTime() {
    return morningSummaryTime;
  }

  public void setMorningSummaryTime(String morningSummaryTime) {
    this.morningSummaryTime = morningSummaryTime;
  }

  public StaffBotSettingsUpdateInput summaryArrivals(Boolean summaryArrivals) {
    this.summaryArrivals = summaryArrivals;
    return this;
  }

  /**
   * Get summaryArrivals
   * @return summaryArrivals
   */
  @NotNull 
  @JsonProperty("summaryArrivals")
  public Boolean getSummaryArrivals() {
    return summaryArrivals;
  }

  public void setSummaryArrivals(Boolean summaryArrivals) {
    this.summaryArrivals = summaryArrivals;
  }

  public StaffBotSettingsUpdateInput summaryDepartures(Boolean summaryDepartures) {
    this.summaryDepartures = summaryDepartures;
    return this;
  }

  /**
   * Get summaryDepartures
   * @return summaryDepartures
   */
  @NotNull 
  @JsonProperty("summaryDepartures")
  public Boolean getSummaryDepartures() {
    return summaryDepartures;
  }

  public void setSummaryDepartures(Boolean summaryDepartures) {
    this.summaryDepartures = summaryDepartures;
  }

  public StaffBotSettingsUpdateInput summaryInHouse(Boolean summaryInHouse) {
    this.summaryInHouse = summaryInHouse;
    return this;
  }

  /**
   * Get summaryInHouse
   * @return summaryInHouse
   */
  @NotNull 
  @JsonProperty("summaryInHouse")
  public Boolean getSummaryInHouse() {
    return summaryInHouse;
  }

  public void setSummaryInHouse(Boolean summaryInHouse) {
    this.summaryInHouse = summaryInHouse;
  }

  public StaffBotSettingsUpdateInput summaryOccupancy(Boolean summaryOccupancy) {
    this.summaryOccupancy = summaryOccupancy;
    return this;
  }

  /**
   * Get summaryOccupancy
   * @return summaryOccupancy
   */
  @NotNull 
  @JsonProperty("summaryOccupancy")
  public Boolean getSummaryOccupancy() {
    return summaryOccupancy;
  }

  public void setSummaryOccupancy(Boolean summaryOccupancy) {
    this.summaryOccupancy = summaryOccupancy;
  }

  public StaffBotSettingsUpdateInput summaryRooms(Boolean summaryRooms) {
    this.summaryRooms = summaryRooms;
    return this;
  }

  /**
   * Get summaryRooms
   * @return summaryRooms
   */
  @NotNull 
  @JsonProperty("summaryRooms")
  public Boolean getSummaryRooms() {
    return summaryRooms;
  }

  public void setSummaryRooms(Boolean summaryRooms) {
    this.summaryRooms = summaryRooms;
  }

  public StaffBotSettingsUpdateInput summaryStaff(Boolean summaryStaff) {
    this.summaryStaff = summaryStaff;
    return this;
  }

  /**
   * Get summaryStaff
   * @return summaryStaff
   */
  @NotNull 
  @JsonProperty("summaryStaff")
  public Boolean getSummaryStaff() {
    return summaryStaff;
  }

  public void setSummaryStaff(Boolean summaryStaff) {
    this.summaryStaff = summaryStaff;
  }

  public StaffBotSettingsUpdateInput summaryRequests(Boolean summaryRequests) {
    this.summaryRequests = summaryRequests;
    return this;
  }

  /**
   * Get summaryRequests
   * @return summaryRequests
   */
  @NotNull 
  @JsonProperty("summaryRequests")
  public Boolean getSummaryRequests() {
    return summaryRequests;
  }

  public void setSummaryRequests(Boolean summaryRequests) {
    this.summaryRequests = summaryRequests;
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
        Objects.equals(this.rosterChangeDelayMinutes, staffBotSettingsUpdateInput.rosterChangeDelayMinutes) &&
        Objects.equals(this.engineerTasksEnabled, staffBotSettingsUpdateInput.engineerTasksEnabled) &&
        Objects.equals(this.housekeeperBlocksEnabled, staffBotSettingsUpdateInput.housekeeperBlocksEnabled) &&
        Objects.equals(this.housekeeperBlocksDays, staffBotSettingsUpdateInput.housekeeperBlocksDays) &&
        Objects.equals(this.therapistScheduleEnabled, staffBotSettingsUpdateInput.therapistScheduleEnabled) &&
        Objects.equals(this.therapistScheduleDays, staffBotSettingsUpdateInput.therapistScheduleDays) &&
        Objects.equals(this.therapistMorningEnabled, staffBotSettingsUpdateInput.therapistMorningEnabled) &&
        Objects.equals(this.therapistMorningTime, staffBotSettingsUpdateInput.therapistMorningTime) &&
        Objects.equals(this.therapistMorningDefaultOn, staffBotSettingsUpdateInput.therapistMorningDefaultOn) &&
        Objects.equals(this.whoIsOnShiftEnabled, staffBotSettingsUpdateInput.whoIsOnShiftEnabled) &&
        Objects.equals(this.whoIsOnShiftForHr, staffBotSettingsUpdateInput.whoIsOnShiftForHr) &&
        Objects.equals(this.summaryCommandEnabled, staffBotSettingsUpdateInput.summaryCommandEnabled) &&
        Objects.equals(this.morningSummaryEnabled, staffBotSettingsUpdateInput.morningSummaryEnabled) &&
        Objects.equals(this.morningSummaryTime, staffBotSettingsUpdateInput.morningSummaryTime) &&
        Objects.equals(this.summaryArrivals, staffBotSettingsUpdateInput.summaryArrivals) &&
        Objects.equals(this.summaryDepartures, staffBotSettingsUpdateInput.summaryDepartures) &&
        Objects.equals(this.summaryInHouse, staffBotSettingsUpdateInput.summaryInHouse) &&
        Objects.equals(this.summaryOccupancy, staffBotSettingsUpdateInput.summaryOccupancy) &&
        Objects.equals(this.summaryRooms, staffBotSettingsUpdateInput.summaryRooms) &&
        Objects.equals(this.summaryStaff, staffBotSettingsUpdateInput.summaryStaff) &&
        Objects.equals(this.summaryRequests, staffBotSettingsUpdateInput.summaryRequests);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myShiftsEnabled, myDayEnabled, myMonthEnabled, myRequestsEnabled, botSettingsEnabled, requestsEnabled, moveRequestsEnabled, deleteRequestsEnabled, requestMinHoursBefore, allowTodayRequests, moveWindowDays, reasonRequired, myShiftsDays, fallbackToAdmin, pendingReminderEnabled, pendingReminderHours, eveReminderEnabled, eveReminderTime, eveReminderDefaultOn, rosterChangeEnabled, rosterChangeHorizonDays, rosterChangeDelayMinutes, engineerTasksEnabled, housekeeperBlocksEnabled, housekeeperBlocksDays, therapistScheduleEnabled, therapistScheduleDays, therapistMorningEnabled, therapistMorningTime, therapistMorningDefaultOn, whoIsOnShiftEnabled, whoIsOnShiftForHr, summaryCommandEnabled, morningSummaryEnabled, morningSummaryTime, summaryArrivals, summaryDepartures, summaryInHouse, summaryOccupancy, summaryRooms, summaryStaff, summaryRequests);
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
    sb.append("    engineerTasksEnabled: ").append(toIndentedString(engineerTasksEnabled)).append("\n");
    sb.append("    housekeeperBlocksEnabled: ").append(toIndentedString(housekeeperBlocksEnabled)).append("\n");
    sb.append("    housekeeperBlocksDays: ").append(toIndentedString(housekeeperBlocksDays)).append("\n");
    sb.append("    therapistScheduleEnabled: ").append(toIndentedString(therapistScheduleEnabled)).append("\n");
    sb.append("    therapistScheduleDays: ").append(toIndentedString(therapistScheduleDays)).append("\n");
    sb.append("    therapistMorningEnabled: ").append(toIndentedString(therapistMorningEnabled)).append("\n");
    sb.append("    therapistMorningTime: ").append(toIndentedString(therapistMorningTime)).append("\n");
    sb.append("    therapistMorningDefaultOn: ").append(toIndentedString(therapistMorningDefaultOn)).append("\n");
    sb.append("    whoIsOnShiftEnabled: ").append(toIndentedString(whoIsOnShiftEnabled)).append("\n");
    sb.append("    whoIsOnShiftForHr: ").append(toIndentedString(whoIsOnShiftForHr)).append("\n");
    sb.append("    summaryCommandEnabled: ").append(toIndentedString(summaryCommandEnabled)).append("\n");
    sb.append("    morningSummaryEnabled: ").append(toIndentedString(morningSummaryEnabled)).append("\n");
    sb.append("    morningSummaryTime: ").append(toIndentedString(morningSummaryTime)).append("\n");
    sb.append("    summaryArrivals: ").append(toIndentedString(summaryArrivals)).append("\n");
    sb.append("    summaryDepartures: ").append(toIndentedString(summaryDepartures)).append("\n");
    sb.append("    summaryInHouse: ").append(toIndentedString(summaryInHouse)).append("\n");
    sb.append("    summaryOccupancy: ").append(toIndentedString(summaryOccupancy)).append("\n");
    sb.append("    summaryRooms: ").append(toIndentedString(summaryRooms)).append("\n");
    sb.append("    summaryStaff: ").append(toIndentedString(summaryStaff)).append("\n");
    sb.append("    summaryRequests: ").append(toIndentedString(summaryRequests)).append("\n");
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

