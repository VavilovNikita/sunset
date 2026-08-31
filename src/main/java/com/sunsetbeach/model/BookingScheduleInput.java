package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PATCH /bookings/{id}/schedule&#x60; and &#x60;POST /bookings/{id}/schedule/quote&#x60; - the booking&#39;s full desired schedule (dates and physical room together), not a partial patch. Used for extending/shortening a stay, moving a booking to a different physical room, or both at once (e.g. dragging a reservation to a different row and different dates on the booking calendar grid). The room *type* (&#x60;roomId&#x60;) cannot be changed this way - only &#x60;checkIn&#x60;/&#x60;checkOut&#x60;/&#x60;roomUnitId&#x60;. &#x60;roomUnitId: null&#x60; leaves (or makes) the booking unassigned; a non-null value must belong to the same room type as the booking. &#x60;roomUnitId&#x60; must be present in the payload (string or explicit &#x60;null&#x60;) but is deliberately not listed under &#x60;required&#x60; - see &#x60;RoomUnitAssignmentInput&#x60;&#39;s description for why a generated &#x60;@NotNull&#x60; on this field would incorrectly reject &#x60;null&#x60;; presence is checked manually in &#x60;BookingService&#x60; instead.  On a booking with more than one segment (see &#x60;Booking.segments&#x60;), only a change that moves exactly one outer edge of the stay is accepted - see the PATCH endpoint&#39;s own description for exactly which changes that covers and which are rejected. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-30T22:13:01.959152900+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class BookingScheduleInput {

  private String checkIn;

  private String checkOut;

  private JsonNullable<String> roomUnitId = JsonNullable.<String>undefined();

  public BookingScheduleInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingScheduleInput(String checkIn, String checkOut) {
    this.checkIn = checkIn;
    this.checkOut = checkOut;
  }

  public BookingScheduleInput checkIn(String checkIn) {
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

  public BookingScheduleInput checkOut(String checkOut) {
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

  public BookingScheduleInput roomUnitId(String roomUnitId) {
    this.roomUnitId = JsonNullable.of(roomUnitId);
    return this;
  }

  /**
   * The room unit to assign, or `null` to leave/make the booking unassigned.
   * @return roomUnitId
   */
  
  @JsonProperty("roomUnitId")
  public JsonNullable<String> getRoomUnitId() {
    return roomUnitId;
  }

  public void setRoomUnitId(JsonNullable<String> roomUnitId) {
    this.roomUnitId = roomUnitId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingScheduleInput bookingScheduleInput = (BookingScheduleInput) o;
    return Objects.equals(this.checkIn, bookingScheduleInput.checkIn) &&
        Objects.equals(this.checkOut, bookingScheduleInput.checkOut) &&
        equalsNullable(this.roomUnitId, bookingScheduleInput.roomUnitId);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(checkIn, checkOut, hashCodeNullable(roomUnitId));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingScheduleInput {\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
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

