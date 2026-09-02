package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.HousekeepingStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A single physical room (\&quot;203\&quot;, \&quot;Garden Bungalow 4\&quot;) - one sellable unit of a Room type.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RoomUnit {

  private String id;

  private String roomId;

  private String label;

  private Boolean isActive;

  private HousekeepingStatus housekeepingStatus;

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionX = JsonNullable.<BigDecimal>undefined();

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionY = JsonNullable.<BigDecimal>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public RoomUnit() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomUnit(String id, String roomId, String label, Boolean isActive, HousekeepingStatus housekeepingStatus, OffsetDateTime createdAt) {
    this.id = id;
    this.roomId = roomId;
    this.label = label;
    this.isActive = isActive;
    this.housekeepingStatus = housekeepingStatus;
    this.createdAt = createdAt;
  }

  public RoomUnit id(String id) {
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

  public RoomUnit roomId(String roomId) {
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

  public RoomUnit label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Unique hotel-wide, not just within the room type.
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

  public RoomUnit isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Inactive units are excluded from `Room.activeUnitCount` and cannot be newly assigned to a booking.
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

  public RoomUnit housekeepingStatus(HousekeepingStatus housekeepingStatus) {
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

  public RoomUnit positionX(BigDecimal positionX) {
    this.positionX = JsonNullable.of(positionX);
    return this;
  }

  /**
   * Normalized (0..1) horizontal position on the property map image, set via `PATCH /room-units/positions`. Null (always paired with a null `positionY`) means this unit hasn't been placed on the map yet - a normal state, especially right after the map ships. 
   * minimum: 0
   * maximum: 1
   * @return positionX
   */
  @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionX")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionX() {
    return positionX;
  }

  public void setPositionX(JsonNullable<BigDecimal> positionX) {
    this.positionX = positionX;
  }

  public RoomUnit positionY(BigDecimal positionY) {
    this.positionY = JsonNullable.of(positionY);
    return this;
  }

  /**
   * Normalized (0..1) vertical position - see `positionX`.
   * minimum: 0
   * maximum: 1
   * @return positionY
   */
  @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionY")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionY() {
    return positionY;
  }

  public void setPositionY(JsonNullable<BigDecimal> positionY) {
    this.positionY = positionY;
  }

  public RoomUnit createdAt(OffsetDateTime createdAt) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomUnit roomUnit = (RoomUnit) o;
    return Objects.equals(this.id, roomUnit.id) &&
        Objects.equals(this.roomId, roomUnit.roomId) &&
        Objects.equals(this.label, roomUnit.label) &&
        Objects.equals(this.isActive, roomUnit.isActive) &&
        Objects.equals(this.housekeepingStatus, roomUnit.housekeepingStatus) &&
        equalsNullable(this.positionX, roomUnit.positionX) &&
        equalsNullable(this.positionY, roomUnit.positionY) &&
        Objects.equals(this.createdAt, roomUnit.createdAt);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, roomId, label, isActive, housekeepingStatus, hashCodeNullable(positionX), hashCodeNullable(positionY), createdAt);
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
    sb.append("class RoomUnit {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    housekeepingStatus: ").append(toIndentedString(housekeepingStatus)).append("\n");
    sb.append("    positionX: ").append(toIndentedString(positionX)).append("\n");
    sb.append("    positionY: ").append(toIndentedString(positionY)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

