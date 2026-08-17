package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomUnit;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Raw Prisma &#x60;Booking&#x60; row with &#x60;room&#x60; included. &#x60;totalPrice&#x60; serializes as a **string** (Prisma &#x60;Decimal&#x60;). &#x60;checkIn&#x60;, &#x60;checkOut&#x60;, &#x60;createdAt&#x60;, &#x60;updatedAt&#x60; serialize as ISO-8601 datetime strings (&#x60;checkIn&#x60;/&#x60;checkOut&#x60; are date-only columns but still render with a &#x60;T00:00:00.000Z&#x60; time component since Prisma returns &#x60;Date&#x60; objects). &#x60;roomUnitId&#x60;/&#x60;roomUnit&#x60; are null until a physical room is assigned via &#x60;PUT /bookings/{id}/room-unit&#x60; - &#x60;roomId&#x60;/&#x60;room&#x60; (the room *type*) stay the source of truth for what was booked either way. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T15:17:55.380996100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class Booking {

  private String id;

  private String roomId;

  private Room room;

  private JsonNullable<String> roomUnitId = JsonNullable.<String>undefined();

  private RoomUnit roomUnit;

  private String guestName;

  private String guestEmail;

  private String guestPhone;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime checkIn;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime checkOut;

  private String totalPrice;

  private BookingStatus status;

  private JsonNullable<String> paymentNote = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public Booking() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Booking(String id, String roomId, Room room, String roomUnitId, RoomUnit roomUnit, String guestName, String guestEmail, String guestPhone, OffsetDateTime checkIn, OffsetDateTime checkOut, String totalPrice, BookingStatus status, String paymentNote, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.roomId = roomId;
    this.room = room;
    this.roomUnitId = JsonNullable.of(roomUnitId);
    this.roomUnit = roomUnit;
    this.guestName = guestName;
    this.guestEmail = guestEmail;
    this.guestPhone = guestPhone;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.totalPrice = totalPrice;
    this.status = status;
    this.paymentNote = JsonNullable.of(paymentNote);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Booking id(String id) {
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

  public Booking roomId(String roomId) {
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

  public Booking room(Room room) {
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

  public Booking roomUnitId(String roomUnitId) {
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

  public Booking roomUnit(RoomUnit roomUnit) {
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

  public Booking guestName(String guestName) {
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

  public Booking guestEmail(String guestEmail) {
    this.guestEmail = guestEmail;
    return this;
  }

  /**
   * Get guestEmail
   * @return guestEmail
   */
  @NotNull 
  @JsonProperty("guestEmail")
  public String getGuestEmail() {
    return guestEmail;
  }

  public void setGuestEmail(String guestEmail) {
    this.guestEmail = guestEmail;
  }

  public Booking guestPhone(String guestPhone) {
    this.guestPhone = guestPhone;
    return this;
  }

  /**
   * Get guestPhone
   * @return guestPhone
   */
  @NotNull 
  @JsonProperty("guestPhone")
  public String getGuestPhone() {
    return guestPhone;
  }

  public void setGuestPhone(String guestPhone) {
    this.guestPhone = guestPhone;
  }

  public Booking checkIn(OffsetDateTime checkIn) {
    this.checkIn = checkIn;
    return this;
  }

  /**
   * Get checkIn
   * @return checkIn
   */
  @NotNull @Valid 
  @JsonProperty("checkIn")
  public OffsetDateTime getCheckIn() {
    return checkIn;
  }

  public void setCheckIn(OffsetDateTime checkIn) {
    this.checkIn = checkIn;
  }

  public Booking checkOut(OffsetDateTime checkOut) {
    this.checkOut = checkOut;
    return this;
  }

  /**
   * Get checkOut
   * @return checkOut
   */
  @NotNull @Valid 
  @JsonProperty("checkOut")
  public OffsetDateTime getCheckOut() {
    return checkOut;
  }

  public void setCheckOut(OffsetDateTime checkOut) {
    this.checkOut = checkOut;
  }

  public Booking totalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string, e.g. `\"4500.00\"`. Always server-computed, never taken from the request.
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

  public Booking status(BookingStatus status) {
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

  public Booking paymentNote(String paymentNote) {
    this.paymentNote = JsonNullable.of(paymentNote);
    return this;
  }

  /**
   * Get paymentNote
   * @return paymentNote
   */
  @NotNull 
  @JsonProperty("paymentNote")
  public JsonNullable<String> getPaymentNote() {
    return paymentNote;
  }

  public void setPaymentNote(JsonNullable<String> paymentNote) {
    this.paymentNote = paymentNote;
  }

  public Booking createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Booking updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Booking booking = (Booking) o;
    return Objects.equals(this.id, booking.id) &&
        Objects.equals(this.roomId, booking.roomId) &&
        Objects.equals(this.room, booking.room) &&
        Objects.equals(this.roomUnitId, booking.roomUnitId) &&
        Objects.equals(this.roomUnit, booking.roomUnit) &&
        Objects.equals(this.guestName, booking.guestName) &&
        Objects.equals(this.guestEmail, booking.guestEmail) &&
        Objects.equals(this.guestPhone, booking.guestPhone) &&
        Objects.equals(this.checkIn, booking.checkIn) &&
        Objects.equals(this.checkOut, booking.checkOut) &&
        Objects.equals(this.totalPrice, booking.totalPrice) &&
        Objects.equals(this.status, booking.status) &&
        Objects.equals(this.paymentNote, booking.paymentNote) &&
        Objects.equals(this.createdAt, booking.createdAt) &&
        Objects.equals(this.updatedAt, booking.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, roomId, room, roomUnitId, roomUnit, guestName, guestEmail, guestPhone, checkIn, checkOut, totalPrice, status, paymentNote, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Booking {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    room: ").append(toIndentedString(room)).append("\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    roomUnit: ").append(toIndentedString(roomUnit)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    guestEmail: ").append(toIndentedString(guestEmail)).append("\n");
    sb.append("    guestPhone: ").append(toIndentedString(guestPhone)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    paymentNote: ").append(toIndentedString(paymentNote)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

