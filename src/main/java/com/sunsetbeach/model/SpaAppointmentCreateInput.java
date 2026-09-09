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
 * Body of &#x60;POST /spa-appointments&#x60;. &#x60;treatmentMenuItemId&#x60; must reference a &#x60;SPA&#x60;-department item with &#x60;durationMinutes&#x60; set (400 otherwise). &#x60;date&#x60; outside the named booking&#39;s &#x60;[checkIn, checkOut]&#x60; (inclusive both ends - the guest is still in the hotel on the departure day) is a warning, not a rejection - see &#x60;SpaAppointmentResult&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaAppointmentCreateInput {

  private String bookingId;

  private String tableId;

  private String therapistUserId;

  private String treatmentMenuItemId;

  private String date;

  private String startTime;

  public SpaAppointmentCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaAppointmentCreateInput(String bookingId, String tableId, String therapistUserId, String treatmentMenuItemId, String date, String startTime) {
    this.bookingId = bookingId;
    this.tableId = tableId;
    this.therapistUserId = therapistUserId;
    this.treatmentMenuItemId = treatmentMenuItemId;
    this.date = date;
    this.startTime = startTime;
  }

  public SpaAppointmentCreateInput bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * Get bookingId
   * @return bookingId
   */
  @NotNull 
  @JsonProperty("bookingId")
  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public SpaAppointmentCreateInput tableId(String tableId) {
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

  public SpaAppointmentCreateInput therapistUserId(String therapistUserId) {
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

  public SpaAppointmentCreateInput treatmentMenuItemId(String treatmentMenuItemId) {
    this.treatmentMenuItemId = treatmentMenuItemId;
    return this;
  }

  /**
   * Get treatmentMenuItemId
   * @return treatmentMenuItemId
   */
  @NotNull 
  @JsonProperty("treatmentMenuItemId")
  public String getTreatmentMenuItemId() {
    return treatmentMenuItemId;
  }

  public void setTreatmentMenuItemId(String treatmentMenuItemId) {
    this.treatmentMenuItemId = treatmentMenuItemId;
  }

  public SpaAppointmentCreateInput date(String date) {
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

  public SpaAppointmentCreateInput startTime(String startTime) {
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
    SpaAppointmentCreateInput spaAppointmentCreateInput = (SpaAppointmentCreateInput) o;
    return Objects.equals(this.bookingId, spaAppointmentCreateInput.bookingId) &&
        Objects.equals(this.tableId, spaAppointmentCreateInput.tableId) &&
        Objects.equals(this.therapistUserId, spaAppointmentCreateInput.therapistUserId) &&
        Objects.equals(this.treatmentMenuItemId, spaAppointmentCreateInput.treatmentMenuItemId) &&
        Objects.equals(this.date, spaAppointmentCreateInput.date) &&
        Objects.equals(this.startTime, spaAppointmentCreateInput.startTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, tableId, therapistUserId, treatmentMenuItemId, date, startTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaAppointmentCreateInput {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    therapistUserId: ").append(toIndentedString(therapistUserId)).append("\n");
    sb.append("    treatmentMenuItemId: ").append(toIndentedString(treatmentMenuItemId)).append("\n");
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

