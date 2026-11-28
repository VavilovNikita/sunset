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
 * Present when a booking could be checked in today but the part of the stay holding tonight has no physical room (not cancelled, &#x60;EXPECTED&#x60; or &#x60;NO_SHOW&#x60;, the hotel date within &#x60;checkIn&#x60;..&#x60;checkOut&#x60;). Assign one with &#x60;PUT /bookings/{id}/segments/{segmentId}/room-unit&#x60;; &#x60;reason&#x60; is &#x60;POST /bookings/{id}/check-in&#x60;&#39;s 400. Rooms of later segments are not asked for. Absent otherwise. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class CheckInRoomNeeded {

  private String segmentId;

  private String roomId;

  private String roomName;

  private String night;

  private String checkIn;

  private String checkOut;

  private String reason;

  public CheckInRoomNeeded() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CheckInRoomNeeded(String segmentId, String roomId, String roomName, String night, String checkIn, String checkOut, String reason) {
    this.segmentId = segmentId;
    this.roomId = roomId;
    this.roomName = roomName;
    this.night = night;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.reason = reason;
  }

  public CheckInRoomNeeded segmentId(String segmentId) {
    this.segmentId = segmentId;
    return this;
  }

  /**
   * The segment that needs the room.
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

  public CheckInRoomNeeded roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * That segment's room type.
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

  public CheckInRoomNeeded roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  /**
   * Get roomName
   * @return roomName
   */
  @NotNull 
  @JsonProperty("roomName")
  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public CheckInRoomNeeded night(String night) {
    this.night = night;
    return this;
  }

  /**
   * The night a check-in today is for - today, or on the departure day the night before.
   * @return night
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("night")
  public String getNight() {
    return night;
  }

  public void setNight(String night) {
    this.night = night;
  }

  public CheckInRoomNeeded checkIn(String checkIn) {
    this.checkIn = checkIn;
    return this;
  }

  /**
   * The segment's first night.
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

  public CheckInRoomNeeded checkOut(String checkOut) {
    this.checkOut = checkOut;
    return this;
  }

  /**
   * The segment's departure day (not a night of it).
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

  public CheckInRoomNeeded reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Get reason
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
    CheckInRoomNeeded checkInRoomNeeded = (CheckInRoomNeeded) o;
    return Objects.equals(this.segmentId, checkInRoomNeeded.segmentId) &&
        Objects.equals(this.roomId, checkInRoomNeeded.roomId) &&
        Objects.equals(this.roomName, checkInRoomNeeded.roomName) &&
        Objects.equals(this.night, checkInRoomNeeded.night) &&
        Objects.equals(this.checkIn, checkInRoomNeeded.checkIn) &&
        Objects.equals(this.checkOut, checkInRoomNeeded.checkOut) &&
        Objects.equals(this.reason, checkInRoomNeeded.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(segmentId, roomId, roomName, night, checkIn, checkOut, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckInRoomNeeded {\n");
    sb.append("    segmentId: ").append(toIndentedString(segmentId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    night: ").append(toIndentedString(night)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

