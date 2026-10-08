package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Body of &#x60;PUT /attendance/day&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class AttendanceDayCorrectionInput {

  private String employeeUserId;

  private String date;

  @Valid
  private List<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")String> times = new ArrayList<>();

  private String reason;

  public AttendanceDayCorrectionInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AttendanceDayCorrectionInput(String employeeUserId, String date, List<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")String> times, String reason) {
    this.employeeUserId = employeeUserId;
    this.date = date;
    this.times = times;
    this.reason = reason;
  }

  public AttendanceDayCorrectionInput employeeUserId(String employeeUserId) {
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

  public AttendanceDayCorrectionInput date(String date) {
    this.date = date;
    return this;
  }

  /**
   * The hotel-local calendar day being corrected.
   * @return date
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("date")
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public AttendanceDayCorrectionInput times(List<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")String> times) {
    this.times = times;
    return this;
  }

  public AttendanceDayCorrectionInput addTimesItem(String timesItem) {
    if (this.times == null) {
      this.times = new ArrayList<>();
    }
    this.times.add(timesItem);
    return this;
  }

  /**
   * Every punch of that day, `HH:mm` hotel time, strictly increasing. Empty clears the day.
   * @return times
   */
  @NotNull @Size(max = 12) 
  @JsonProperty("times")
  public List<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")String> getTimes() {
    return times;
  }

  public void setTimes(List<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")String> times) {
    this.times = times;
  }

  public AttendanceDayCorrectionInput reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Get reason
   * @return reason
   */
  @NotNull @Size(min = 1, max = 500) 
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttendanceDayCorrectionInput attendanceDayCorrectionInput = (AttendanceDayCorrectionInput) o;
    return Objects.equals(this.employeeUserId, attendanceDayCorrectionInput.employeeUserId) &&
        Objects.equals(this.date, attendanceDayCorrectionInput.date) &&
        Objects.equals(this.times, attendanceDayCorrectionInput.times) &&
        Objects.equals(this.reason, attendanceDayCorrectionInput.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeUserId, date, times, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttendanceDayCorrectionInput {\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    times: ").append(toIndentedString(times)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

