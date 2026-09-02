package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.PropertyMapActiveBlock;
import com.sunsetbeach.model.PropertyMapCurrentBooking;
import java.math.BigDecimal;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One physical room on the property map (&#x60;GET /property-map&#x60;). &#x60;isActive&#x60; (permanently deactivated) and &#x60;activeBlock&#x60; (temporarily pulled off sale today, &#x60;isActive&#x60; still true) are independent facts and both always present here - never collapse them into a single \&quot;unavailable\&quot; flag, they mean different things and the frontend&#39;s display rule treats them differently (a deactivated unit reads as gone from the picture; a blocked unit reads as \&quot;unavailable today, will return\&quot; with its reason shown). Which one wins visually when both happen to be true is a frontend decision, not encoded in this DTO. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PropertyMapUnit {

  private String roomUnitId;

  private String roomId;

  private String roomName;

  private String unitLabel;

  private Boolean isActive;

  private HousekeepingStatus housekeepingStatus;

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionX = JsonNullable.<BigDecimal>undefined();

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionY = JsonNullable.<BigDecimal>undefined();

  private PropertyMapCurrentBooking currentBooking;

  private PropertyMapActiveBlock activeBlock;

  public PropertyMapUnit() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PropertyMapUnit(String roomUnitId, String roomId, String roomName, String unitLabel, Boolean isActive, HousekeepingStatus housekeepingStatus, BigDecimal positionX, BigDecimal positionY, PropertyMapCurrentBooking currentBooking, PropertyMapActiveBlock activeBlock) {
    this.roomUnitId = roomUnitId;
    this.roomId = roomId;
    this.roomName = roomName;
    this.unitLabel = unitLabel;
    this.isActive = isActive;
    this.housekeepingStatus = housekeepingStatus;
    this.positionX = JsonNullable.of(positionX);
    this.positionY = JsonNullable.of(positionY);
    this.currentBooking = currentBooking;
    this.activeBlock = activeBlock;
  }

  public PropertyMapUnit roomUnitId(String roomUnitId) {
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

  public PropertyMapUnit roomId(String roomId) {
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

  public PropertyMapUnit roomName(String roomName) {
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

  public PropertyMapUnit unitLabel(String unitLabel) {
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

  public PropertyMapUnit isActive(Boolean isActive) {
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

  public PropertyMapUnit housekeepingStatus(HousekeepingStatus housekeepingStatus) {
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

  public PropertyMapUnit positionX(BigDecimal positionX) {
    this.positionX = JsonNullable.of(positionX);
    return this;
  }

  /**
   * Get positionX
   * minimum: 0
   * maximum: 1
   * @return positionX
   */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionX")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionX() {
    return positionX;
  }

  public void setPositionX(JsonNullable<BigDecimal> positionX) {
    this.positionX = positionX;
  }

  public PropertyMapUnit positionY(BigDecimal positionY) {
    this.positionY = JsonNullable.of(positionY);
    return this;
  }

  /**
   * Get positionY
   * minimum: 0
   * maximum: 1
   * @return positionY
   */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionY")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionY() {
    return positionY;
  }

  public void setPositionY(JsonNullable<BigDecimal> positionY) {
    this.positionY = positionY;
  }

  public PropertyMapUnit currentBooking(PropertyMapCurrentBooking currentBooking) {
    this.currentBooking = currentBooking;
    return this;
  }

  /**
   * Get currentBooking
   * @return currentBooking
   */
  @NotNull @Valid 
  @JsonProperty("currentBooking")
  public PropertyMapCurrentBooking getCurrentBooking() {
    return currentBooking;
  }

  public void setCurrentBooking(PropertyMapCurrentBooking currentBooking) {
    this.currentBooking = currentBooking;
  }

  public PropertyMapUnit activeBlock(PropertyMapActiveBlock activeBlock) {
    this.activeBlock = activeBlock;
    return this;
  }

  /**
   * Get activeBlock
   * @return activeBlock
   */
  @NotNull @Valid 
  @JsonProperty("activeBlock")
  public PropertyMapActiveBlock getActiveBlock() {
    return activeBlock;
  }

  public void setActiveBlock(PropertyMapActiveBlock activeBlock) {
    this.activeBlock = activeBlock;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertyMapUnit propertyMapUnit = (PropertyMapUnit) o;
    return Objects.equals(this.roomUnitId, propertyMapUnit.roomUnitId) &&
        Objects.equals(this.roomId, propertyMapUnit.roomId) &&
        Objects.equals(this.roomName, propertyMapUnit.roomName) &&
        Objects.equals(this.unitLabel, propertyMapUnit.unitLabel) &&
        Objects.equals(this.isActive, propertyMapUnit.isActive) &&
        Objects.equals(this.housekeepingStatus, propertyMapUnit.housekeepingStatus) &&
        Objects.equals(this.positionX, propertyMapUnit.positionX) &&
        Objects.equals(this.positionY, propertyMapUnit.positionY) &&
        Objects.equals(this.currentBooking, propertyMapUnit.currentBooking) &&
        Objects.equals(this.activeBlock, propertyMapUnit.activeBlock);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomUnitId, roomId, roomName, unitLabel, isActive, housekeepingStatus, positionX, positionY, currentBooking, activeBlock);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertyMapUnit {\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    unitLabel: ").append(toIndentedString(unitLabel)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    housekeepingStatus: ").append(toIndentedString(housekeepingStatus)).append("\n");
    sb.append("    positionX: ").append(toIndentedString(positionX)).append("\n");
    sb.append("    positionY: ").append(toIndentedString(positionY)).append("\n");
    sb.append("    currentBooking: ").append(toIndentedString(currentBooking)).append("\n");
    sb.append("    activeBlock: ").append(toIndentedString(activeBlock)).append("\n");
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

