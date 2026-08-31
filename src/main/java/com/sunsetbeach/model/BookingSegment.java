package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomUnit;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One \&quot;room X from date A to date B\&quot; leg of a booking&#39;s stay. A booking&#39;s segments always cover &#x60;[Booking.checkIn, Booking.checkOut)&#x60; with no gap and no overlap - a booking that has never been relocated has exactly one segment identical to the booking&#39;s own roomId/roomUnitId/checkIn/checkOut/totalPrice; that is the common case, not a special one. See &#x60;POST /bookings/{id}/relocate&#x60;&#39;s description for how a second segment appears. &#x60;checkIn&#x60;/&#x60;checkOut&#x60; are plain date-only strings (&#x60;YYYY-MM-DD&#x60;), the same format &#x60;Booking.checkIn&#x60;/&#x60;checkOut&#x60; now use too. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-30T22:52:49.532858600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class BookingSegment {

  private String id;

  private String roomId;

  private Room room;

  private JsonNullable<String> roomUnitId = JsonNullable.<String>undefined();

  private RoomUnit roomUnit;

  private String checkIn;

  private String checkOut;

  private String totalPrice;

  public BookingSegment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingSegment(String id, String roomId, Room room, String roomUnitId, RoomUnit roomUnit, String checkIn, String checkOut, String totalPrice) {
    this.id = id;
    this.roomId = roomId;
    this.room = room;
    this.roomUnitId = JsonNullable.of(roomUnitId);
    this.roomUnit = roomUnit;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.totalPrice = totalPrice;
  }

  public BookingSegment id(String id) {
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

  public BookingSegment roomId(String roomId) {
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

  public BookingSegment room(Room room) {
    this.room = room;
    return this;
  }

  /**
   * Get room
   * @return room
   */
  @NotNull @Valid 
  @JsonProperty("room")
  public Room getRoom() {
    return room;
  }

  public void setRoom(Room room) {
    this.room = room;
  }

  public BookingSegment roomUnitId(String roomUnitId) {
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

  public BookingSegment roomUnit(RoomUnit roomUnit) {
    this.roomUnit = roomUnit;
    return this;
  }

  /**
   * Get roomUnit
   * @return roomUnit
   */
  @NotNull @Valid 
  @JsonProperty("roomUnit")
  public RoomUnit getRoomUnit() {
    return roomUnit;
  }

  public void setRoomUnit(RoomUnit roomUnit) {
    this.roomUnit = roomUnit;
  }

  public BookingSegment checkIn(String checkIn) {
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

  public BookingSegment checkOut(String checkOut) {
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

  public BookingSegment totalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string - this segment's own share of the booking's total, server-computed from `Room.basePrice`/`RatePlan` for its own dates/room.
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
    BookingSegment bookingSegment = (BookingSegment) o;
    return Objects.equals(this.id, bookingSegment.id) &&
        Objects.equals(this.roomId, bookingSegment.roomId) &&
        Objects.equals(this.room, bookingSegment.room) &&
        Objects.equals(this.roomUnitId, bookingSegment.roomUnitId) &&
        Objects.equals(this.roomUnit, bookingSegment.roomUnit) &&
        Objects.equals(this.checkIn, bookingSegment.checkIn) &&
        Objects.equals(this.checkOut, bookingSegment.checkOut) &&
        Objects.equals(this.totalPrice, bookingSegment.totalPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, roomId, room, roomUnitId, roomUnit, checkIn, checkOut, totalPrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingSegment {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    room: ").append(toIndentedString(room)).append("\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    roomUnit: ").append(toIndentedString(roomUnit)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
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

