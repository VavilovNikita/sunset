package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.HousekeepingStatus;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One physical room on the Housekeeping page (&#x60;GET /housekeeping/rooms&#x60;). Deliberately carries nothing about who is in it: the page is read by people who see no guest data. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class HousekeepingRoom {

  private String roomUnitId;

  private String roomId;

  private String roomName;

  private String unitLabel;

  private Boolean isActive;

  private HousekeepingStatus housekeepingStatus;

  private Boolean occupied;

  private Boolean arrivingToday;

  private Boolean departingToday;

  private Integer overdueDays;

  private Boolean outOfOrder;

  public HousekeepingRoom() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public HousekeepingRoom(String roomUnitId, String roomId, String roomName, String unitLabel, Boolean isActive, HousekeepingStatus housekeepingStatus, Boolean occupied, Boolean arrivingToday, Boolean departingToday, Integer overdueDays, Boolean outOfOrder) {
    this.roomUnitId = roomUnitId;
    this.roomId = roomId;
    this.roomName = roomName;
    this.unitLabel = unitLabel;
    this.isActive = isActive;
    this.housekeepingStatus = housekeepingStatus;
    this.occupied = occupied;
    this.arrivingToday = arrivingToday;
    this.departingToday = departingToday;
    this.overdueDays = overdueDays;
    this.outOfOrder = outOfOrder;
  }

  public HousekeepingRoom roomUnitId(String roomUnitId) {
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

  public HousekeepingRoom roomId(String roomId) {
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

  public HousekeepingRoom roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  /**
   * The room type's name.
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

  public HousekeepingRoom unitLabel(String unitLabel) {
    this.unitLabel = unitLabel;
    return this;
  }

  /**
   * Get unitLabel
   * @return unitLabel
   */
  @NotNull 
  @JsonProperty("unitLabel")
  public String getUnitLabel() {
    return unitLabel;
  }

  public void setUnitLabel(String unitLabel) {
    this.unitLabel = unitLabel;
  }

  public HousekeepingRoom isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Get isActive
   * @return isActive
   */
  @NotNull 
  @JsonProperty("isActive")
  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public HousekeepingRoom housekeepingStatus(HousekeepingStatus housekeepingStatus) {
    this.housekeepingStatus = housekeepingStatus;
    return this;
  }

  /**
   * Get housekeepingStatus
   * @return housekeepingStatus
   */
  @NotNull @Valid 
  @JsonProperty("housekeepingStatus")
  public HousekeepingStatus getHousekeepingStatus() {
    return housekeepingStatus;
  }

  public void setHousekeepingStatus(HousekeepingStatus housekeepingStatus) {
    this.housekeepingStatus = housekeepingStatus;
  }

  public HousekeepingRoom occupied(Boolean occupied) {
    this.occupied = occupied;
    return this;
  }

  /**
   * A guest is checked into the room right now (an overstaying guest included).
   * @return occupied
   */
  @NotNull 
  @JsonProperty("occupied")
  public Boolean getOccupied() {
    return occupied;
  }

  public void setOccupied(Boolean occupied) {
    this.occupied = occupied;
  }

  public HousekeepingRoom arrivingToday(Boolean arrivingToday) {
    this.arrivingToday = arrivingToday;
    return this;
  }

  /**
   * A guest expected today has this room and has not checked in - whether or not someone is still in it.
   * @return arrivingToday
   */
  @NotNull 
  @JsonProperty("arrivingToday")
  public Boolean getArrivingToday() {
    return arrivingToday;
  }

  public void setArrivingToday(Boolean arrivingToday) {
    this.arrivingToday = arrivingToday;
  }

  public HousekeepingRoom departingToday(Boolean departingToday) {
    this.departingToday = departingToday;
    return this;
  }

  /**
   * A guest in the room is due out today (of two in it, either one).
   * @return departingToday
   */
  @NotNull 
  @JsonProperty("departingToday")
  public Boolean getDepartingToday() {
    return departingToday;
  }

  public void setDepartingToday(Boolean departingToday) {
    this.departingToday = departingToday;
  }

  public HousekeepingRoom overdueDays(Integer overdueDays) {
    this.overdueDays = overdueDays;
    return this;
  }

  /**
   * Days a guest in the room is past check-out (of two in it, the longest), `0` otherwise.
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

  public HousekeepingRoom outOfOrder(Boolean outOfOrder) {
    this.outOfOrder = outOfOrder;
    return this;
  }

  /**
   * The room is blocked (`RoomUnitBlock`) today.
   * @return outOfOrder
   */
  @NotNull 
  @JsonProperty("outOfOrder")
  public Boolean getOutOfOrder() {
    return outOfOrder;
  }

  public void setOutOfOrder(Boolean outOfOrder) {
    this.outOfOrder = outOfOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HousekeepingRoom housekeepingRoom = (HousekeepingRoom) o;
    return Objects.equals(this.roomUnitId, housekeepingRoom.roomUnitId) &&
        Objects.equals(this.roomId, housekeepingRoom.roomId) &&
        Objects.equals(this.roomName, housekeepingRoom.roomName) &&
        Objects.equals(this.unitLabel, housekeepingRoom.unitLabel) &&
        Objects.equals(this.isActive, housekeepingRoom.isActive) &&
        Objects.equals(this.housekeepingStatus, housekeepingRoom.housekeepingStatus) &&
        Objects.equals(this.occupied, housekeepingRoom.occupied) &&
        Objects.equals(this.arrivingToday, housekeepingRoom.arrivingToday) &&
        Objects.equals(this.departingToday, housekeepingRoom.departingToday) &&
        Objects.equals(this.overdueDays, housekeepingRoom.overdueDays) &&
        Objects.equals(this.outOfOrder, housekeepingRoom.outOfOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomUnitId, roomId, roomName, unitLabel, isActive, housekeepingStatus, occupied, arrivingToday, departingToday, overdueDays, outOfOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HousekeepingRoom {\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    unitLabel: ").append(toIndentedString(unitLabel)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    housekeepingStatus: ").append(toIndentedString(housekeepingStatus)).append("\n");
    sb.append("    occupied: ").append(toIndentedString(occupied)).append("\n");
    sb.append("    arrivingToday: ").append(toIndentedString(arrivingToday)).append("\n");
    sb.append("    departingToday: ").append(toIndentedString(departingToday)).append("\n");
    sb.append("    overdueDays: ").append(toIndentedString(overdueDays)).append("\n");
    sb.append("    outOfOrder: ").append(toIndentedString(outOfOrder)).append("\n");
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

