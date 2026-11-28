package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.CheckInRoomNeeded;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A guest due to arrive today whom no room tile can show, because tonight has no room assigned.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PropertyMapArrivalWithoutRoom {

  private String bookingId;

  private String guestName;

  private CheckInRoomNeeded roomNeeded;

  public PropertyMapArrivalWithoutRoom() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PropertyMapArrivalWithoutRoom(String bookingId, String guestName, CheckInRoomNeeded roomNeeded) {
    this.bookingId = bookingId;
    this.guestName = guestName;
    this.roomNeeded = roomNeeded;
  }

  public PropertyMapArrivalWithoutRoom bookingId(String bookingId) {
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

  public PropertyMapArrivalWithoutRoom guestName(String guestName) {
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

  public PropertyMapArrivalWithoutRoom roomNeeded(CheckInRoomNeeded roomNeeded) {
    this.roomNeeded = roomNeeded;
    return this;
  }

  /**
   * Get roomNeeded
   * @return roomNeeded
   */
  @NotNull @Valid 
  @JsonProperty("roomNeeded")
  public CheckInRoomNeeded getRoomNeeded() {
    return roomNeeded;
  }

  public void setRoomNeeded(CheckInRoomNeeded roomNeeded) {
    this.roomNeeded = roomNeeded;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertyMapArrivalWithoutRoom propertyMapArrivalWithoutRoom = (PropertyMapArrivalWithoutRoom) o;
    return Objects.equals(this.bookingId, propertyMapArrivalWithoutRoom.bookingId) &&
        Objects.equals(this.guestName, propertyMapArrivalWithoutRoom.guestName) &&
        Objects.equals(this.roomNeeded, propertyMapArrivalWithoutRoom.roomNeeded);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, guestName, roomNeeded);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertyMapArrivalWithoutRoom {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append("[REDACTED]").append("\n");
    sb.append("    roomNeeded: ").append(toIndentedString(roomNeeded)).append("\n");
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

