package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.RoomStillHeldState;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Present on the guest a planned room move has put into a room that a guest who came before is still checked into although they should have left it (see &#x60;RoomStillHeldState&#x60;). The move takes effect by date, so nothing refuses it; this is the warning. The way out is to deal with that guest (check them out, extend their stay or pick their new room), or to move this one; it goes with either. Set on a &#x60;TodayBoard&#x60; entry, on &#x60;GET /bookings/{id}&#x60; and on the property map&#39;s current booking (a tile with both guests shows this one); absent otherwise. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RoomStillHeld {

  private String bookingId;

  private String guestName;

  private RoomStillHeldState state;

  private String reason;

  public RoomStillHeld() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomStillHeld(String bookingId, String guestName, RoomStillHeldState state, String reason) {
    this.bookingId = bookingId;
    this.guestName = guestName;
    this.state = state;
    this.reason = reason;
  }

  public RoomStillHeld bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * The booking of the guest still in the room.
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

  public RoomStillHeld guestName(String guestName) {
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

  public RoomStillHeld state(RoomStillHeldState state) {
    this.state = state;
    return this;
  }

  /**
   * Get state
   * @return state
   */
  @NotNull @Valid 
  @JsonProperty("state")
  public RoomStillHeldState getState() {
    return state;
  }

  public void setState(RoomStillHeldState state) {
    this.state = state;
  }

  public RoomStillHeld reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * The line to show, e.g. \"Room 101 is still held by Ann (past check-out)\".
   * @return reason
   */
  @NotNull 
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
    RoomStillHeld roomStillHeld = (RoomStillHeld) o;
    return Objects.equals(this.bookingId, roomStillHeld.bookingId) &&
        Objects.equals(this.guestName, roomStillHeld.guestName) &&
        Objects.equals(this.state, roomStillHeld.state) &&
        Objects.equals(this.reason, roomStillHeld.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, guestName, state, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomStillHeld {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append("[REDACTED]").append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    reason: ").append("[REDACTED]").append("\n");
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

