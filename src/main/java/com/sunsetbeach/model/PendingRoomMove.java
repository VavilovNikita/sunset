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
 * Present for a guest in the house whose part of the stay holding today has no physical room - a move that took effect with none picked, or a room taken off by hand. They are still in the earlier room (&#x60;stayingInRoom&#x60;): occupied on every screen, closed to other check-ins, not marked dirty. Pick the room with &#x60;PUT /bookings/{id}/segments/{segmentId}/room-unit&#x60;. Absent otherwise. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PendingRoomMove {

  private String segmentId;

  private String roomId;

  private String roomName;

  private String checkIn;

  private String checkOut;

  private String reason;

  private String stayingInRoom;

  public PendingRoomMove() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PendingRoomMove(String segmentId, String roomId, String roomName, String checkIn, String checkOut, String reason) {
    this.segmentId = segmentId;
    this.roomId = roomId;
    this.roomName = roomName;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.reason = reason;
  }

  public PendingRoomMove segmentId(String segmentId) {
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

  public PendingRoomMove roomId(String roomId) {
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

  public PendingRoomMove roomName(String roomName) {
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

  public PendingRoomMove checkIn(String checkIn) {
    this.checkIn = checkIn;
    return this;
  }

  /**
   * The segment's first night - the day of the move.
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

  public PendingRoomMove checkOut(String checkOut) {
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

  public PendingRoomMove reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * The line to show, e.g. \"Moving today — choose the new room\".
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

  public PendingRoomMove stayingInRoom(String stayingInRoom) {
    this.stayingInRoom = stayingInRoom;
    return this;
  }

  /**
   * Label of the room the guest is still in; absent when there is none.
   * @return stayingInRoom
   */
  
  @JsonProperty("stayingInRoom")
  public String getStayingInRoom() {
    return stayingInRoom;
  }

  public void setStayingInRoom(String stayingInRoom) {
    this.stayingInRoom = stayingInRoom;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PendingRoomMove pendingRoomMove = (PendingRoomMove) o;
    return Objects.equals(this.segmentId, pendingRoomMove.segmentId) &&
        Objects.equals(this.roomId, pendingRoomMove.roomId) &&
        Objects.equals(this.roomName, pendingRoomMove.roomName) &&
        Objects.equals(this.checkIn, pendingRoomMove.checkIn) &&
        Objects.equals(this.checkOut, pendingRoomMove.checkOut) &&
        Objects.equals(this.reason, pendingRoomMove.reason) &&
        Objects.equals(this.stayingInRoom, pendingRoomMove.stayingInRoom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(segmentId, roomId, roomName, checkIn, checkOut, reason, stayingInRoom);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PendingRoomMove {\n");
    sb.append("    segmentId: ").append(toIndentedString(segmentId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    stayingInRoom: ").append(toIndentedString(stayingInRoom)).append("\n");
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

