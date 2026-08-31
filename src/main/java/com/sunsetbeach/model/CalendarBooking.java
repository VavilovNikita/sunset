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
 * One booking *segment* as rendered on the booking calendar grid - a lighter projection than &#x60;BookingSegment&#x60; (no nested &#x60;room&#x60;/&#x60;roomUnit&#x60;; cross-reference against &#x60;BookingCalendarResponse.roomTypes&#x60; instead), returned only for non-&#x60;CANCELLED&#x60; bookings. &#x60;segmentId&#x60; is the *segment&#39;s* id, not the booking&#39;s - a relocated booking produces more than one &#x60;CalendarBooking&#x60; entry, sharing the same &#x60;bookingId&#x60;, rendered as separate bars (different rows and/or date ranges) that all open the same booking&#39;s card on click. Any call that needs to act on the booking itself (open the card panel, quote or apply a schedule change, relocate) must use &#x60;bookingId&#x60;, never &#x60;segmentId&#x60; - the field is named for exactly what it identifies specifically because a same-named-but-different- meaning &#x60;id&#x60; here once caused every drag-to-edit handler to send the wrong id and get a silent 404. &#x60;roomUnitId: null&#x60; means this segment occupies a unit of its room type but has no specific physical room assigned yet - the grid renders it in an unassigned row for that type. &#x60;checkIn&#x60;/&#x60;checkOut&#x60; are plain date-only strings (&#x60;YYYY-MM-DD&#x60;), the same format &#x60;Booking.checkIn&#x60;/&#x60;checkOut&#x60; now use too. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-30T22:52:49.532858600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class CalendarBooking {

  private String segmentId;

  private String bookingId;

  private String roomId;

  private JsonNullable<String> roomUnitId = JsonNullable.<String>undefined();

  private String guestName;

  private String checkIn;

  private String checkOut;

  private BookingStatus status;

  private String totalPrice;

  private Integer segmentCount;

  public CalendarBooking() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CalendarBooking(String segmentId, String bookingId, String roomId, String roomUnitId, String guestName, String checkIn, String checkOut, BookingStatus status, String totalPrice, Integer segmentCount) {
    this.segmentId = segmentId;
    this.bookingId = bookingId;
    this.roomId = roomId;
    this.roomUnitId = JsonNullable.of(roomUnitId);
    this.guestName = guestName;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.status = status;
    this.totalPrice = totalPrice;
    this.segmentCount = segmentCount;
  }

  public CalendarBooking segmentId(String segmentId) {
    this.segmentId = segmentId;
    return this;
  }

  /**
   * This segment's own id (`BookingSegment.id`). Not the booking's id - use `bookingId` for anything that acts on the booking.
   * @return segmentId
   */
  @NotNull 
  @JsonProperty("segmentId")
  public String getSegmentId() {
    return segmentId;
  }

  public void setSegmentId(String segmentId) {
    this.segmentId = segmentId;
  }

  public CalendarBooking bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * Groups segments belonging to the same booking, and is what the card panel opens by, and what every schedule/relocate call must be addressed to.
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
   * Decimal(10,2) rendered as a string - this segment's own price, not the whole booking's total.
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

  public CalendarBooking segmentCount(Integer segmentCount) {
    this.segmentCount = segmentCount;
    return this;
  }

  /**
   * How many segments the parent booking has in total - `1` for the overwhelmingly common never-relocated case. The grid uses this to decide whether drag-resize/drag-move is offered on this bar at all (see `PATCH /bookings/{id}/schedule`'s description) without a second round trip to fetch the full booking.
   * @return segmentCount
   */
  @NotNull 
  @JsonProperty("segmentCount")
  public Integer getSegmentCount() {
    return segmentCount;
  }

  public void setSegmentCount(Integer segmentCount) {
    this.segmentCount = segmentCount;
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
    return Objects.equals(this.segmentId, calendarBooking.segmentId) &&
        Objects.equals(this.bookingId, calendarBooking.bookingId) &&
        Objects.equals(this.roomId, calendarBooking.roomId) &&
        Objects.equals(this.roomUnitId, calendarBooking.roomUnitId) &&
        Objects.equals(this.guestName, calendarBooking.guestName) &&
        Objects.equals(this.checkIn, calendarBooking.checkIn) &&
        Objects.equals(this.checkOut, calendarBooking.checkOut) &&
        Objects.equals(this.status, calendarBooking.status) &&
        Objects.equals(this.totalPrice, calendarBooking.totalPrice) &&
        Objects.equals(this.segmentCount, calendarBooking.segmentCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(segmentId, bookingId, roomId, roomUnitId, guestName, checkIn, checkOut, status, totalPrice, segmentCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CalendarBooking {\n");
    sb.append("    segmentId: ").append(toIndentedString(segmentId)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
    sb.append("    segmentCount: ").append(toIndentedString(segmentCount)).append("\n");
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

