package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RoomTypeDailyAvailability;
import com.sunsetbeach.model.RoomUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One room type&#39;s rows on the booking calendar grid - its physical units, and its per-day remaining-availability count.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RoomTypeCalendar {

  private String roomId;

  private String roomName;

  @Valid
  private List<@Valid RoomUnit> roomUnits = new ArrayList<>();

  @Valid
  private List<@Valid RoomTypeDailyAvailability> dailyAvailable = new ArrayList<>();

  public RoomTypeCalendar() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomTypeCalendar(String roomId, String roomName, List<@Valid RoomUnit> roomUnits, List<@Valid RoomTypeDailyAvailability> dailyAvailable) {
    this.roomId = roomId;
    this.roomName = roomName;
    this.roomUnits = roomUnits;
    this.dailyAvailable = dailyAvailable;
  }

  public RoomTypeCalendar roomId(String roomId) {
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

  public RoomTypeCalendar roomName(String roomName) {
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

  public RoomTypeCalendar roomUnits(List<@Valid RoomUnit> roomUnits) {
    this.roomUnits = roomUnits;
    return this;
  }

  public RoomTypeCalendar addRoomUnitsItem(RoomUnit roomUnitsItem) {
    if (this.roomUnits == null) {
      this.roomUnits = new ArrayList<>();
    }
    this.roomUnits.add(roomUnitsItem);
    return this;
  }

  /**
   * Get roomUnits
   * @return roomUnits
   */
  @NotNull @Valid 
  @JsonProperty("roomUnits")
  public List<@Valid RoomUnit> getRoomUnits() {
    return roomUnits;
  }

  public void setRoomUnits(List<@Valid RoomUnit> roomUnits) {
    this.roomUnits = roomUnits;
  }

  public RoomTypeCalendar dailyAvailable(List<@Valid RoomTypeDailyAvailability> dailyAvailable) {
    this.dailyAvailable = dailyAvailable;
    return this;
  }

  public RoomTypeCalendar addDailyAvailableItem(RoomTypeDailyAvailability dailyAvailableItem) {
    if (this.dailyAvailable == null) {
      this.dailyAvailable = new ArrayList<>();
    }
    this.dailyAvailable.add(dailyAvailableItem);
    return this;
  }

  /**
   * Get dailyAvailable
   * @return dailyAvailable
   */
  @NotNull @Valid 
  @JsonProperty("dailyAvailable")
  public List<@Valid RoomTypeDailyAvailability> getDailyAvailable() {
    return dailyAvailable;
  }

  public void setDailyAvailable(List<@Valid RoomTypeDailyAvailability> dailyAvailable) {
    this.dailyAvailable = dailyAvailable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomTypeCalendar roomTypeCalendar = (RoomTypeCalendar) o;
    return Objects.equals(this.roomId, roomTypeCalendar.roomId) &&
        Objects.equals(this.roomName, roomTypeCalendar.roomName) &&
        Objects.equals(this.roomUnits, roomTypeCalendar.roomUnits) &&
        Objects.equals(this.dailyAvailable, roomTypeCalendar.dailyAvailable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, roomName, roomUnits, dailyAvailable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomTypeCalendar {\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    roomUnits: ").append(toIndentedString(roomUnits)).append("\n");
    sb.append("    dailyAvailable: ").append(toIndentedString(dailyAvailable)).append("\n");
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

