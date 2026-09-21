package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.TodayShiftState;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One employee&#39;s live status on &#x60;GET /attendance/today&#x60; - \&quot;who&#39;s on shift right now\&quot;, not \&quot;who&#39;s scheduled this month\&quot; (&#x60;GET /attendance/summary&#x60;) or \&quot;what actually happened last month\&quot; (&#x60;GET /roster/actuals-export&#x60;). Only employees with a &#x60;countsAsWorked&#x60; &#x60;RosterEntry&#x60; for today appear at all - an &#x60;ABSENCE&#x60;-kind entry (&#x60;PH&#x60; and the like) and an employee with no entry today are both simply absent from this list, the same \&quot;a day off is the absence of a row\&quot; convention &#x60;RosterEntry&#x60; itself already uses for the roster grid. &#x60;referenceTime&#x60; is deliberately one raw timestamp whose meaning switches with &#x60;state&#x60;, not a pre-formatted \&quot;in 23 minutes\&quot; string - the frontend derives (and re-derives, every poll tick) the relative time itself, so it never goes stale between polls on its own: &#x60;SCHEDULED&#x60;/&#x60;ARRIVING_SOON&#x60;/&#x60;LATE&#x60;/&#x60;BETWEEN_SHIFTS&#x60;/&#x60;MISSED&#x60; carry the current interval&#39;s own expected start time, today; &#x60;ON_SHIFT&#x60; carries the actual punch time of the trailing unmatched &#x60;IN&#x60;; &#x60;FINISHED&#x60; carries the punch time of the last completed pair&#39;s &#x60;OUT&#x60;; &#x60;NOT_YET_ARRIVED&#x60; carries nothing - there is genuinely nothing to report yet. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TodayShiftStatus {

  private String employeeUserId;

  private String employeeName;

  private JsonNullable<StaffArea> staffArea = JsonNullable.<StaffArea>undefined();

  private ShiftCode shiftCode;

  private TodayShiftState state;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> referenceTime = JsonNullable.<OffsetDateTime>undefined();

  public TodayShiftStatus() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TodayShiftStatus(String employeeUserId, String employeeName, ShiftCode shiftCode, TodayShiftState state) {
    this.employeeUserId = employeeUserId;
    this.employeeName = employeeName;
    this.shiftCode = shiftCode;
    this.state = state;
  }

  public TodayShiftStatus employeeUserId(String employeeUserId) {
    this.employeeUserId = employeeUserId;
    return this;
  }

  /**
   * Get employeeUserId
   * @return employeeUserId
   */
  @NotNull 
  @JsonProperty("employeeUserId")
  public String getEmployeeUserId() {
    return employeeUserId;
  }

  public void setEmployeeUserId(String employeeUserId) {
    this.employeeUserId = employeeUserId;
  }

  public TodayShiftStatus employeeName(String employeeName) {
    this.employeeName = employeeName;
    return this;
  }

  /**
   * Get employeeName
   * @return employeeName
   */
  @NotNull 
  @JsonProperty("employeeName")
  public String getEmployeeName() {
    return employeeName;
  }

  public void setEmployeeName(String employeeName) {
    this.employeeName = employeeName;
  }

  public TodayShiftStatus staffArea(StaffArea staffArea) {
    this.staffArea = JsonNullable.of(staffArea);
    return this;
  }

  /**
   * Get staffArea
   * @return staffArea
   */
  @Valid 
  @JsonProperty("staffArea")
  public JsonNullable<StaffArea> getStaffArea() {
    return staffArea;
  }

  public void setStaffArea(JsonNullable<StaffArea> staffArea) {
    this.staffArea = staffArea;
  }

  public TodayShiftStatus shiftCode(ShiftCode shiftCode) {
    this.shiftCode = shiftCode;
    return this;
  }

  /**
   * Get shiftCode
   * @return shiftCode
   */
  @NotNull @Valid 
  @JsonProperty("shiftCode")
  public ShiftCode getShiftCode() {
    return shiftCode;
  }

  public void setShiftCode(ShiftCode shiftCode) {
    this.shiftCode = shiftCode;
  }

  public TodayShiftStatus state(TodayShiftState state) {
    this.state = state;
    return this;
  }

  /**
   * Get state
   * @return state
   */
  @NotNull @Valid 
  @JsonProperty("state")
  public TodayShiftState getState() {
    return state;
  }

  public void setState(TodayShiftState state) {
    this.state = state;
  }

  public TodayShiftStatus referenceTime(OffsetDateTime referenceTime) {
    this.referenceTime = JsonNullable.of(referenceTime);
    return this;
  }

  /**
   * Get referenceTime
   * @return referenceTime
   */
  @Valid 
  @JsonProperty("referenceTime")
  public JsonNullable<OffsetDateTime> getReferenceTime() {
    return referenceTime;
  }

  public void setReferenceTime(JsonNullable<OffsetDateTime> referenceTime) {
    this.referenceTime = referenceTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TodayShiftStatus todayShiftStatus = (TodayShiftStatus) o;
    return Objects.equals(this.employeeUserId, todayShiftStatus.employeeUserId) &&
        Objects.equals(this.employeeName, todayShiftStatus.employeeName) &&
        equalsNullable(this.staffArea, todayShiftStatus.staffArea) &&
        Objects.equals(this.shiftCode, todayShiftStatus.shiftCode) &&
        Objects.equals(this.state, todayShiftStatus.state) &&
        equalsNullable(this.referenceTime, todayShiftStatus.referenceTime);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeUserId, employeeName, hashCodeNullable(staffArea), shiftCode, state, hashCodeNullable(referenceTime));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TodayShiftStatus {\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    employeeName: ").append(toIndentedString(employeeName)).append("\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
    sb.append("    shiftCode: ").append(toIndentedString(shiftCode)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    referenceTime: ").append(toIndentedString(referenceTime)).append("\n");
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

