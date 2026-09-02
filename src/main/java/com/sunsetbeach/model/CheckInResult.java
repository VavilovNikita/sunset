package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.Booking;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;POST /bookings/{id}/check-in&#x60;. &#x60;warning&#x60; is set (but the check-in still succeeds) when the room being checked into is &#x60;DIRTY&#x60; - staff are told, not blocked; a room sometimes gets finished while the guest waits, and the desk must not stall on it. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class CheckInResult {

  private Booking booking;

  private JsonNullable<String> warning = JsonNullable.<String>undefined();

  public CheckInResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CheckInResult(Booking booking, String warning) {
    this.booking = booking;
    this.warning = JsonNullable.of(warning);
  }

  public CheckInResult booking(Booking booking) {
    this.booking = booking;
    return this;
  }

  /**
   * Get booking
   * @return booking
   */
  @NotNull @Valid 
  @JsonProperty("booking")
  public Booking getBooking() {
    return booking;
  }

  public void setBooking(Booking booking) {
    this.booking = booking;
  }

  public CheckInResult warning(String warning) {
    this.warning = JsonNullable.of(warning);
    return this;
  }

  /**
   * Get warning
   * @return warning
   */
  @NotNull 
  @JsonProperty("warning")
  public JsonNullable<String> getWarning() {
    return warning;
  }

  public void setWarning(JsonNullable<String> warning) {
    this.warning = warning;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckInResult checkInResult = (CheckInResult) o;
    return Objects.equals(this.booking, checkInResult.booking) &&
        Objects.equals(this.warning, checkInResult.warning);
  }

  @Override
  public int hashCode() {
    return Objects.hash(booking, warning);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckInResult {\n");
    sb.append("    booking: ").append(toIndentedString(booking)).append("\n");
    sb.append("    warning: ").append(toIndentedString(warning)).append("\n");
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

