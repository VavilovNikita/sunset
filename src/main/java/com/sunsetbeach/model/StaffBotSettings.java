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

  private Integer moveMaxDaysAhead;

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

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public StaffBotSettings() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StaffBotSettings(Boolean myShiftsEnabled, Boolean myDayEnabled, Boolean myMonthEnabled, Boolean myRequestsEnabled, Boolean botSettingsEnabled, Boolean requestsEnabled, Boolean moveRequestsEnabled, Boolean deleteRequestsEnabled, Integer requestMinHoursBefore, Boolean allowTodayRequests, Integer moveWindowDays, Integer moveMaxDaysAhead, Boolean reasonRequired, Integer myShiftsDays, Boolean fallbackToAdmin, Boolean pendingReminderEnabled, Integer pendingReminderHours, Boolean eveReminderEnabled, String eveReminderTime, Boolean eveReminderDefaultOn, Boolean rosterChangeEnabled, Integer rosterChangeHorizonDays, Integer rosterChangeDelayMinutes, Boolean engineerTasksEnabled, Boolean housekeeperBlocksEnabled, Integer housekeeperBlocksDays, Boolean therapistScheduleEnabled, Integer therapistScheduleDays, Boolean therapistMorningEnabled, String therapistMorningTime, Boolean therapistMorningDefaultOn, Boolean whoIsOnShiftEnabled, Boolean whoIsOnShiftForHr, Boolean summaryCommandEnabled, Boolean morningSummaryEnabled, String morningSummaryTime, Boolean summaryArrivals, Boolean summaryDepartures, Boolean summaryInHouse, Boolean summaryOccupancy, Boolean summaryRooms, Boolean summaryStaff, Boolean summaryRequests, OffsetDateTime updatedAt) {
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
    this.moveMaxDaysAhead = moveMaxDaysAhead;
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
   * How many of the nearest days the bot offers as buttons when a shift is moved (default 14). It does not limit the move - `moveMaxDaysAhead` does. 
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

  public StaffBotSettings moveMaxDaysAhead(Integer moveMaxDaysAhead) {
    this.moveMaxDaysAhead = moveMaxDaysAhead;
    return this;
  }

  /**
   * How far ahead a shift can be moved at all (default 60; never less than `moveWindowDays`, else 400 on this field). Days beyond the buttons are reached with the bot's \"Other date...\" calendar or by typing a date. 
   * minimum: 1
   * maximum: 180
   * @return moveMaxDaysAhead
   */
  @NotNull @Min(1) @Max(180) 
  @JsonProperty("moveMaxDaysAhead")
  public Integer getMoveMaxDaysAhead() {
    return moveMaxDaysAhead;
  }

  public void setMoveMaxDaysAhead(Integer moveMaxDaysAhead) {
    this.moveMaxDaysAhead = moveMaxDaysAhead;
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
   * Administrators decide while nobody holds `HR` or the holder is on leave (default on). With it off those requests wait - except a request filed by the HR director themself: it is never decided by them, so it always goes to the administrators, whatever this switch says (otherwise nobody could decide it). 
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
   * Tell an employee when a manager changed their shift. Only the manual gestures of the roster grid (create, move, delete, reassign, swap) are announced; the month generator, \"copy a month\" and the Excel imports are bulk set-up and never send this message. 
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

  public StaffBotSettings engineerTasksEnabled(Boolean engineerTasksEnabled) {
    this.engineerTasksEnabled = engineerTasksEnabled;
    return this;
  }

  /**
   * The engineer's \"Tasks\" screen: open maintenance tasks with Start / Done (ENGINEER, MANAGER+ as the fallback). Default on.
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

  public StaffBotSettings housekeeperBlocksEnabled(Boolean housekeeperBlocksEnabled) {
    this.housekeeperBlocksEnabled = housekeeperBlocksEnabled;
    return this;
  }

  /**
   * The housekeeper's \"Blocks\" screen: room blocks the housekeeping function is answerable for, read only. Default on.
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

  public StaffBotSettings housekeeperBlocksDays(Integer housekeeperBlocksDays) {
    this.housekeeperBlocksDays = housekeeperBlocksDays;
    return this;
  }

  /**
   * Blocks that are on now or start within this many days are listed (default 7).
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

  public StaffBotSettings therapistScheduleEnabled(Boolean therapistScheduleEnabled) {
    this.therapistScheduleEnabled = therapistScheduleEnabled;
    return this;
  }

  /**
   * The therapist's \"My procedures\" screen. Default on.
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

  public StaffBotSettings therapistScheduleDays(Integer therapistScheduleDays) {
    this.therapistScheduleDays = therapistScheduleDays;
    return this;
  }

  /**
   * How many days, today included, the My procedures screen shows (default 2).
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

  public StaffBotSettings therapistMorningEnabled(Boolean therapistMorningEnabled) {
    this.therapistMorningEnabled = therapistMorningEnabled;
    return this;
  }

  /**
   * The automatic morning message \"N procedures today, the first at 10:00\" to therapists who switched it on themselves. Default on (nothing is sent until a therapist opts in).
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

  public StaffBotSettings therapistMorningTime(String therapistMorningTime) {
    this.therapistMorningTime = therapistMorningTime;
    return this;
  }

  /**
   * Hotel time, `HH:mm`, from which the therapists' morning message goes (default 08:00).
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

  public StaffBotSettings therapistMorningDefaultOn(Boolean therapistMorningDefaultOn) {
    this.therapistMorningDefaultOn = therapistMorningDefaultOn;
    return this;
  }

  /**
   * Whether a therapist who never chose gets the morning message. Default off; each therapist overrides it in the bot's Settings.
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

  public StaffBotSettings whoIsOnShiftEnabled(Boolean whoIsOnShiftEnabled) {
    this.whoIsOnShiftEnabled = whoIsOnShiftEnabled;
    return this;
  }

  /**
   * The \"Who is on shift\" screen for managers. Default on.
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

  public StaffBotSettings whoIsOnShiftForHr(Boolean whoIsOnShiftForHr) {
    this.whoIsOnShiftForHr = whoIsOnShiftForHr;
    return this;
  }

  /**
   * Whether the HR director, who is not a manager, also sees \"Who is on shift\" (they can already read attendance). Default on.
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

  public StaffBotSettings summaryCommandEnabled(Boolean summaryCommandEnabled) {
    this.summaryCommandEnabled = summaryCommandEnabled;
    return this;
  }

  /**
   * The \"Summary\" command for MANAGER+. Default on.
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

  public StaffBotSettings morningSummaryEnabled(Boolean morningSummaryEnabled) {
    this.morningSummaryEnabled = morningSummaryEnabled;
    return this;
  }

  /**
   * The automatic morning summary to the director recipients of `NotificationSettings`. Default on.
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

  public StaffBotSettings morningSummaryTime(String morningSummaryTime) {
    this.morningSummaryTime = morningSummaryTime;
    return this;
  }

  /**
   * Hotel time, `HH:mm`, from which the morning summary goes (default 08:00).
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

  public StaffBotSettings summaryArrivals(Boolean summaryArrivals) {
    this.summaryArrivals = summaryArrivals;
    return this;
  }

  /**
   * Summary block: arrivals today. Default on.
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

  public StaffBotSettings summaryDepartures(Boolean summaryDepartures) {
    this.summaryDepartures = summaryDepartures;
    return this;
  }

  /**
   * Summary block: departures today and overdue departures. Default on.
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

  public StaffBotSettings summaryInHouse(Boolean summaryInHouse) {
    this.summaryInHouse = summaryInHouse;
    return this;
  }

  /**
   * Summary block: rooms and guests in the house. Default on.
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

  public StaffBotSettings summaryOccupancy(Boolean summaryOccupancy) {
    this.summaryOccupancy = summaryOccupancy;
    return this;
  }

  /**
   * Summary block: occupancy today and rooms still free tonight. Default on.
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

  public StaffBotSettings summaryRooms(Boolean summaryRooms) {
    this.summaryRooms = summaryRooms;
    return this;
  }

  /**
   * Summary block: rooms blocked and rooms waiting to be cleaned. Default on.
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

  public StaffBotSettings summaryStaff(Boolean summaryStaff) {
    this.summaryStaff = summaryStaff;
    return this;
  }

  /**
   * Summary block: staff on the roster, on shift now and absent. Default on.
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

  public StaffBotSettings summaryRequests(Boolean summaryRequests) {
    this.summaryRequests = summaryRequests;
    return this;
  }

  /**
   * Summary block: shift requests waiting for a decision. Default on.
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
        Objects.equals(this.moveMaxDaysAhead, staffBotSettings.moveMaxDaysAhead) &&
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
        Objects.equals(this.engineerTasksEnabled, staffBotSettings.engineerTasksEnabled) &&
        Objects.equals(this.housekeeperBlocksEnabled, staffBotSettings.housekeeperBlocksEnabled) &&
        Objects.equals(this.housekeeperBlocksDays, staffBotSettings.housekeeperBlocksDays) &&
        Objects.equals(this.therapistScheduleEnabled, staffBotSettings.therapistScheduleEnabled) &&
        Objects.equals(this.therapistScheduleDays, staffBotSettings.therapistScheduleDays) &&
        Objects.equals(this.therapistMorningEnabled, staffBotSettings.therapistMorningEnabled) &&
        Objects.equals(this.therapistMorningTime, staffBotSettings.therapistMorningTime) &&
        Objects.equals(this.therapistMorningDefaultOn, staffBotSettings.therapistMorningDefaultOn) &&
        Objects.equals(this.whoIsOnShiftEnabled, staffBotSettings.whoIsOnShiftEnabled) &&
        Objects.equals(this.whoIsOnShiftForHr, staffBotSettings.whoIsOnShiftForHr) &&
        Objects.equals(this.summaryCommandEnabled, staffBotSettings.summaryCommandEnabled) &&
        Objects.equals(this.morningSummaryEnabled, staffBotSettings.morningSummaryEnabled) &&
        Objects.equals(this.morningSummaryTime, staffBotSettings.morningSummaryTime) &&
        Objects.equals(this.summaryArrivals, staffBotSettings.summaryArrivals) &&
        Objects.equals(this.summaryDepartures, staffBotSettings.summaryDepartures) &&
        Objects.equals(this.summaryInHouse, staffBotSettings.summaryInHouse) &&
        Objects.equals(this.summaryOccupancy, staffBotSettings.summaryOccupancy) &&
        Objects.equals(this.summaryRooms, staffBotSettings.summaryRooms) &&
        Objects.equals(this.summaryStaff, staffBotSettings.summaryStaff) &&
        Objects.equals(this.summaryRequests, staffBotSettings.summaryRequests) &&
        Objects.equals(this.updatedAt, staffBotSettings.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myShiftsEnabled, myDayEnabled, myMonthEnabled, myRequestsEnabled, botSettingsEnabled, requestsEnabled, moveRequestsEnabled, deleteRequestsEnabled, requestMinHoursBefore, allowTodayRequests, moveWindowDays, moveMaxDaysAhead, reasonRequired, myShiftsDays, fallbackToAdmin, pendingReminderEnabled, pendingReminderHours, eveReminderEnabled, eveReminderTime, eveReminderDefaultOn, rosterChangeEnabled, rosterChangeHorizonDays, rosterChangeDelayMinutes, engineerTasksEnabled, housekeeperBlocksEnabled, housekeeperBlocksDays, therapistScheduleEnabled, therapistScheduleDays, therapistMorningEnabled, therapistMorningTime, therapistMorningDefaultOn, whoIsOnShiftEnabled, whoIsOnShiftForHr, summaryCommandEnabled, morningSummaryEnabled, morningSummaryTime, summaryArrivals, summaryDepartures, summaryInHouse, summaryOccupancy, summaryRooms, summaryStaff, summaryRequests, updatedAt);
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
    sb.append("    moveMaxDaysAhead: ").append(toIndentedString(moveMaxDaysAhead)).append("\n");
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

