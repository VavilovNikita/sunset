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
 * Per-day status of one physical room - the data the booking calendar grid renders per cell.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T15:17:55.380996100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class RoomUnitAvailability {

  private String roomUnitId;

  private String label;

  private Boolean isBlocked;

  private Boolean isBooked;

  private Boolean isAvailable;

  private JsonNullable<String> bookingId = JsonNullable.<String>undefined();

  private JsonNullable<String> blockReason = JsonNullable.<String>undefined();

  public RoomUnitAvailability() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomUnitAvailability(String roomUnitId, String label, Boolean isBlocked, Boolean isBooked, Boolean isAvailable, String bookingId, String blockReason) {
    this.roomUnitId = roomUnitId;
    this.label = label;
    this.isBlocked = isBlocked;
    this.isBooked = isBooked;
    this.isAvailable = isAvailable;
    this.bookingId = JsonNullable.of(bookingId);
    this.blockReason = JsonNullable.of(blockReason);
  }

  public RoomUnitAvailability roomUnitId(String roomUnitId) {
    this.roomUnitId = roomUnitId;
    return this;
  }

  /**
   * Get roomUnitId
   * @return roomUnitId
   */
  @NotNull 
  @JsonProperty("roomUnitId")
  public String getRoomUnitId() {
    return roomUnitId;
  }

  public void setRoomUnitId(String roomUnitId) {
    this.roomUnitId = roomUnitId;
  }

  public RoomUnitAvailability label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  @NotNull 
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public RoomUnitAvailability isBlocked(Boolean isBlocked) {
    this.isBlocked = isBlocked;
    return this;
  }

  /**
   * True if a `RoomUnitBlock` covers this day.
   * @return isBlocked
   */
  @NotNull 
  @JsonProperty("isBlocked")
  public Boolean getIsBlocked() {
    return isBlocked;
  }

  public void setIsBlocked(Boolean isBlocked) {
    this.isBlocked = isBlocked;
  }

  public RoomUnitAvailability isBooked(Boolean isBooked) {
    this.isBooked = isBooked;
    return this;
  }

  /**
   * True if a non-CANCELLED booking has this unit assigned and covers this day.
   * @return isBooked
   */
  @NotNull 
  @JsonProperty("isBooked")
  public Boolean getIsBooked() {
    return isBooked;
  }

  public void setIsBooked(Boolean isBooked) {
    this.isBooked = isBooked;
  }

  public RoomUnitAvailability isAvailable(Boolean isAvailable) {
    this.isAvailable = isAvailable;
    return this;
  }

  /**
   * `!isBlocked && !isBooked`.
   * @return isAvailable
   */
  @NotNull 
  @JsonProperty("isAvailable")
  public Boolean getIsAvailable() {
    return isAvailable;
  }

  public void setIsAvailable(Boolean isAvailable) {
    this.isAvailable = isAvailable;
  }

  public RoomUnitAvailability bookingId(String bookingId) {
    this.bookingId = JsonNullable.of(bookingId);
    return this;
  }

  /**
   * Set when `isBooked` is true.
   * @return bookingId
   */
  @NotNull 
  @JsonProperty("bookingId")
  public JsonNullable<String> getBookingId() {
    return bookingId;
  }

  public void setBookingId(JsonNullable<String> bookingId) {
    this.bookingId = bookingId;
  }

  public RoomUnitAvailability blockReason(String blockReason) {
    this.blockReason = JsonNullable.of(blockReason);
    return this;
  }

  /**
   * Set when `isBlocked` is true (`RoomUnitBlock.reason`).
   * @return blockReason
   */
  @NotNull 
  @JsonProperty("blockReason")
  public JsonNullable<String> getBlockReason() {
    return blockReason;
  }

  public void setBlockReason(JsonNullable<String> blockReason) {
    this.blockReason = blockReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomUnitAvailability roomUnitAvailability = (RoomUnitAvailability) o;
    return Objects.equals(this.roomUnitId, roomUnitAvailability.roomUnitId) &&
        Objects.equals(this.label, roomUnitAvailability.label) &&
        Objects.equals(this.isBlocked, roomUnitAvailability.isBlocked) &&
        Objects.equals(this.isBooked, roomUnitAvailability.isBooked) &&
        Objects.equals(this.isAvailable, roomUnitAvailability.isAvailable) &&
        Objects.equals(this.bookingId, roomUnitAvailability.bookingId) &&
        Objects.equals(this.blockReason, roomUnitAvailability.blockReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomUnitId, label, isBlocked, isBooked, isAvailable, bookingId, blockReason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUnitAvailability {\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    isBlocked: ").append(toIndentedString(isBlocked)).append("\n");
    sb.append("    isBooked: ").append(toIndentedString(isBooked)).append("\n");
    sb.append("    isAvailable: ").append(toIndentedString(isAvailable)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    blockReason: ").append(toIndentedString(blockReason)).append("\n");
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

