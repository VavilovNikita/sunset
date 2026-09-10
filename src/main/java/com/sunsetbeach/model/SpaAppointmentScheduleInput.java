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
 * Body of &#x60;PATCH /spa-appointments/{id}/schedule&#x60;. Full replacement of all four fields - same &#x60;tableId&#x60;/&#x60;therapistUserId&#x60;/&#x60;date&#x60;/&#x60;startTime&#x60; shape as &#x60;SpaAppointmentCreateInput&#x60;, minus &#x60;bookingId&#x60;/&#x60;treatmentMenuItemId&#x60; (neither changes here) and &#x60;durationMinutes&#x60; stays frozen from creation (see &#x60;SpaAppointment.durationMinutes&#x60;), so the appointment&#39;s own length is preserved regardless of where it moves. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaAppointmentScheduleInput {

  private String tableId;

  private String therapistUserId;

  private String date;

  private String startTime;

  public SpaAppointmentScheduleInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaAppointmentScheduleInput(String tableId, String therapistUserId, String date, String startTime) {
    this.tableId = tableId;
    this.therapistUserId = therapistUserId;
    this.date = date;
    this.startTime = startTime;
  }

  public SpaAppointmentScheduleInput tableId(String tableId) {
    this.tableId = tableId;
    return this;
  }

  /**
   * Get tableId
   * @return tableId
   */
  @NotNull 
  @JsonProperty("tableId")
  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public SpaAppointmentScheduleInput therapistUserId(String therapistUserId) {
    this.therapistUserId = therapistUserId;
    return this;
  }

  /**
   * Get therapistUserId
   * @return therapistUserId
   */
  @NotNull 
  @JsonProperty("therapistUserId")
  public String getTherapistUserId() {
    return therapistUserId;
  }

  public void setTherapistUserId(String therapistUserId) {
    this.therapistUserId = therapistUserId;
  }

  public SpaAppointmentScheduleInput date(String date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
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

  public SpaAppointmentScheduleInput startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Get startTime
   * @return startTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("startTime")
  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaAppointmentScheduleInput spaAppointmentScheduleInput = (SpaAppointmentScheduleInput) o;
    return Objects.equals(this.tableId, spaAppointmentScheduleInput.tableId) &&
        Objects.equals(this.therapistUserId, spaAppointmentScheduleInput.therapistUserId) &&
        Objects.equals(this.date, spaAppointmentScheduleInput.date) &&
        Objects.equals(this.startTime, spaAppointmentScheduleInput.startTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableId, therapistUserId, date, startTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaAppointmentScheduleInput {\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    therapistUserId: ").append(toIndentedString(therapistUserId)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
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

