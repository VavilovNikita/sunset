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
 * PropertyMapWaitingArrival
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PropertyMapWaitingArrival {

  private String bookingId;

  private String guestName;

  public PropertyMapWaitingArrival() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PropertyMapWaitingArrival(String bookingId, String guestName) {
    this.bookingId = bookingId;
    this.guestName = guestName;
  }

  public PropertyMapWaitingArrival bookingId(String bookingId) {
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

  public PropertyMapWaitingArrival guestName(String guestName) {
    this.guestName = guestName;
    return this;
  }

  /**
   * Get guestName
   * @return guestName
   */
  @NotNull 
  @JsonProperty("guestName")
  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertyMapWaitingArrival propertyMapWaitingArrival = (PropertyMapWaitingArrival) o;
    return Objects.equals(this.bookingId, propertyMapWaitingArrival.bookingId) &&
        Objects.equals(this.guestName, propertyMapWaitingArrival.guestName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, guestName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertyMapWaitingArrival {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
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

