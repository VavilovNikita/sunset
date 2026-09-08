package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.BookingStatus;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One booking whose stay overlaps a block being created on this unit - enough for the UI to name the guest and link to the booking (&#x60;GET /bookings/{id}&#x60;), not the full &#x60;Booking&#x60; object. &#x60;checkIn&#x60;/&#x60;checkOut&#x60;/&#x60;status&#x60; are the booking&#39;s own derived-from-segments fields (see &#x60;Booking&#x60;&#39;s own description), not the specific segment that touches this unit - the overwhelmingly common case is one segment, and this stays a simple per-booking summary rather than a per-segment one. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RoomUnitBlockAffectedBooking {

  private String bookingId;

  private String guestName;

  private String checkIn;

  private String checkOut;

  private BookingStatus status;

  public RoomUnitBlockAffectedBooking() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomUnitBlockAffectedBooking(String bookingId, String guestName, String checkIn, String checkOut, BookingStatus status) {
    this.bookingId = bookingId;
    this.guestName = guestName;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.status = status;
  }

  public RoomUnitBlockAffectedBooking bookingId(String bookingId) {
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

  public RoomUnitBlockAffectedBooking guestName(String guestName) {
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

  public RoomUnitBlockAffectedBooking checkIn(String checkIn) {
    this.checkIn = checkIn;
    return this;
  }

  /**
   * Get checkIn
   * @return checkIn
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("checkIn")
  public String getCheckIn() {
    return checkIn;
  }

  public void setCheckIn(String checkIn) {
    this.checkIn = checkIn;
  }

  public RoomUnitBlockAffectedBooking checkOut(String checkOut) {
    this.checkOut = checkOut;
    return this;
  }

  /**
   * Get checkOut
   * @return checkOut
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("checkOut")
  public String getCheckOut() {
    return checkOut;
  }

  public void setCheckOut(String checkOut) {
    this.checkOut = checkOut;
  }

  public RoomUnitBlockAffectedBooking status(BookingStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public BookingStatus getStatus() {
    return status;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomUnitBlockAffectedBooking roomUnitBlockAffectedBooking = (RoomUnitBlockAffectedBooking) o;
    return Objects.equals(this.bookingId, roomUnitBlockAffectedBooking.bookingId) &&
        Objects.equals(this.guestName, roomUnitBlockAffectedBooking.guestName) &&
        Objects.equals(this.checkIn, roomUnitBlockAffectedBooking.checkIn) &&
        Objects.equals(this.checkOut, roomUnitBlockAffectedBooking.checkOut) &&
        Objects.equals(this.status, roomUnitBlockAffectedBooking.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, guestName, checkIn, checkOut, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUnitBlockAffectedBooking {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

