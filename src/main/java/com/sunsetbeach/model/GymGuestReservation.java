package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.GymReservationStatus;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A reservation as its guest sees it - no automation details, no staff names.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymGuestReservation {

  private String id;

  private String bookingId;

  private String date;

  private String startTime;

  private String endTime;

  private Integer durationMinutes;

  private GymReservationStatus status;

  private Boolean canCancel;

  public GymGuestReservation() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymGuestReservation(String id, String bookingId, String date, String startTime, String endTime, Integer durationMinutes, GymReservationStatus status, Boolean canCancel) {
    this.id = id;
    this.bookingId = bookingId;
    this.date = date;
    this.startTime = startTime;
    this.endTime = endTime;
    this.durationMinutes = durationMinutes;
    this.status = status;
    this.canCancel = canCancel;
  }

  public GymGuestReservation id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public GymGuestReservation bookingId(String bookingId) {
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

  public GymGuestReservation date(String date) {
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

  public GymGuestReservation startTime(String startTime) {
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

  public GymGuestReservation endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Get endTime
   * @return endTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("endTime")
  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public GymGuestReservation durationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Get durationMinutes
   * @return durationMinutes
   */
  @NotNull 
  @JsonProperty("durationMinutes")
  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public GymGuestReservation status(GymReservationStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public GymReservationStatus getStatus() {
    return status;
  }

  public void setStatus(GymReservationStatus status) {
    this.status = status;
  }

  public GymGuestReservation canCancel(Boolean canCancel) {
    this.canCancel = canCancel;
    return this;
  }

  /**
   * True while the reservation is `BOOKED` and has not started.
   * @return canCancel
   */
  @NotNull 
  @JsonProperty("canCancel")
  public Boolean getCanCancel() {
    return canCancel;
  }

  public void setCanCancel(Boolean canCancel) {
    this.canCancel = canCancel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymGuestReservation gymGuestReservation = (GymGuestReservation) o;
    return Objects.equals(this.id, gymGuestReservation.id) &&
        Objects.equals(this.bookingId, gymGuestReservation.bookingId) &&
        Objects.equals(this.date, gymGuestReservation.date) &&
        Objects.equals(this.startTime, gymGuestReservation.startTime) &&
        Objects.equals(this.endTime, gymGuestReservation.endTime) &&
        Objects.equals(this.durationMinutes, gymGuestReservation.durationMinutes) &&
        Objects.equals(this.status, gymGuestReservation.status) &&
        Objects.equals(this.canCancel, gymGuestReservation.canCancel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, bookingId, date, startTime, endTime, durationMinutes, status, canCancel);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymGuestReservation {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    canCancel: ").append(toIndentedString(canCancel)).append("\n");
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

