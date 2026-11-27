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
 * Set only on an &#x60;arrivingToday&#x60; entry whose room still has another guest checked into it: &#x60;POST /bookings/{id}/check-in&#x60; answers 409 until that guest is checked out or the arrival is moved to another room. Absent or null otherwise. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TodayBoardRoomOccupant {

  private String bookingId;

  private String guestName;

  private Integer overdueDays;

  public TodayBoardRoomOccupant() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TodayBoardRoomOccupant(String bookingId, String guestName, Integer overdueDays) {
    this.bookingId = bookingId;
    this.guestName = guestName;
    this.overdueDays = overdueDays;
  }

  public TodayBoardRoomOccupant bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * The booking of the guest in the room.
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

  public TodayBoardRoomOccupant guestName(String guestName) {
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

  public TodayBoardRoomOccupant overdueDays(Integer overdueDays) {
    this.overdueDays = overdueDays;
    return this;
  }

  /**
   * How many days past their `checkOut` that guest is (see `OverstayRule`); `0` when they are simply not checked out yet (due out today). 
   * minimum: 0
   * @return overdueDays
   */
  @NotNull @Min(0) 
  @JsonProperty("overdueDays")
  public Integer getOverdueDays() {
    return overdueDays;
  }

  public void setOverdueDays(Integer overdueDays) {
    this.overdueDays = overdueDays;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TodayBoardRoomOccupant todayBoardRoomOccupant = (TodayBoardRoomOccupant) o;
    return Objects.equals(this.bookingId, todayBoardRoomOccupant.bookingId) &&
        Objects.equals(this.guestName, todayBoardRoomOccupant.guestName) &&
        Objects.equals(this.overdueDays, todayBoardRoomOccupant.overdueDays);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, guestName, overdueDays);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TodayBoardRoomOccupant {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    overdueDays: ").append(toIndentedString(overdueDays)).append("\n");
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

