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
 * A booking as rendered on the booking calendar grid - a lighter projection than &#x60;Booking&#x60; (no nested &#x60;room&#x60;/&#x60;roomUnit&#x60;; cross-reference against &#x60;BookingCalendarResponse.roomTypes&#x60; instead), returned only for non-&#x60;CANCELLED&#x60; bookings. &#x60;roomUnitId: null&#x60; means the booking occupies a unit of its room type but has no specific physical room assigned yet - the grid renders it in an unassigned row for that type.  ⚠ &#x60;checkIn&#x60;/&#x60;checkOut&#x60; here are plain date-only strings (&#x60;YYYY-MM-DD&#x60;), **unlike** &#x60;Booking.checkIn&#x60;/&#x60;checkOut&#x60; which carry a legacy &#x60;T00:00:00.000Z&#x60; time component (a Prisma serialization artifact &#x60;Booking&#x60; inherited that this new schema deliberately does not repeat). Do not assume the two formats match when comparing values sourced from both endpoints - frontend code must go through &#x60;dateOnlyUTC()&#x60; (&#x60;lib/bookings.ts&#x60;) either way, never a manual string slice/parse. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T21:43:17.277610500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class CalendarBooking {

  private String id;

  private String roomId;

  private JsonNullable<String> roomUnitId = JsonNullable.<String>undefined();

  private String guestName;

  private String checkIn;

  private String checkOut;

  private BookingStatus status;

  private String totalPrice;

  public CalendarBooking() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CalendarBooking(String id, String roomId, String roomUnitId, String guestName, String checkIn, String checkOut, BookingStatus status, String totalPrice) {
    this.id = id;
    this.roomId = roomId;
    this.roomUnitId = JsonNullable.of(roomUnitId);
    this.guestName = guestName;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.status = status;
    this.totalPrice = totalPrice;
  }

  public CalendarBooking id(String id) {
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

  public CalendarBooking roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * Get roomId
   * @return roomId
   */
  @NotNull 
  @JsonProperty("roomId")
  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public CalendarBooking roomUnitId(String roomUnitId) {
    this.roomUnitId = JsonNullable.of(roomUnitId);
    return this;
  }

  /**
   * Get roomUnitId
   * @return roomUnitId
   */
  @NotNull 
  @JsonProperty("roomUnitId")
  public JsonNullable<String> getRoomUnitId() {
    return roomUnitId;
  }

  public void setRoomUnitId(JsonNullable<String> roomUnitId) {
    this.roomUnitId = roomUnitId;
  }

  public CalendarBooking guestName(String guestName) {
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

  public CalendarBooking checkIn(String checkIn) {
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

  public CalendarBooking checkOut(String checkOut) {
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

  public CalendarBooking status(BookingStatus status) {
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

  public CalendarBooking totalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return totalPrice
   */
  @NotNull 
  @JsonProperty("totalPrice")
  public String getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CalendarBooking calendarBooking = (CalendarBooking) o;
    return Objects.equals(this.id, calendarBooking.id) &&
        Objects.equals(this.roomId, calendarBooking.roomId) &&
        Objects.equals(this.roomUnitId, calendarBooking.roomUnitId) &&
        Objects.equals(this.guestName, calendarBooking.guestName) &&
        Objects.equals(this.checkIn, calendarBooking.checkIn) &&
        Objects.equals(this.checkOut, calendarBooking.checkOut) &&
        Objects.equals(this.status, calendarBooking.status) &&
        Objects.equals(this.totalPrice, calendarBooking.totalPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, roomId, roomUnitId, guestName, checkIn, checkOut, status, totalPrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CalendarBooking {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
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

