package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One entry of the array body of &#x60;PATCH /room-units/positions&#x60;. &#x60;positionX&#x60;/&#x60;positionY&#x60; must both be null (clears the position, returning the unit to \&quot;not placed\&quot;) or both set within 0..1 - never one without the other. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RoomUnitPositionInput {

  private String roomUnitId;

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionX = JsonNullable.<BigDecimal>undefined();

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionY = JsonNullable.<BigDecimal>undefined();

  public RoomUnitPositionInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomUnitPositionInput(String roomUnitId, BigDecimal positionX, BigDecimal positionY) {
    this.roomUnitId = roomUnitId;
    this.positionX = JsonNullable.of(positionX);
    this.positionY = JsonNullable.of(positionY);
  }

  public RoomUnitPositionInput roomUnitId(String roomUnitId) {
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

  public RoomUnitPositionInput positionX(BigDecimal positionX) {
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

  public RoomUnitPositionInput positionY(BigDecimal positionY) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomUnitPositionInput roomUnitPositionInput = (RoomUnitPositionInput) o;
    return Objects.equals(this.roomUnitId, roomUnitPositionInput.roomUnitId) &&
        Objects.equals(this.positionX, roomUnitPositionInput.positionX) &&
        Objects.equals(this.positionY, roomUnitPositionInput.positionY);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomUnitId, positionX, positionY);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUnitPositionInput {\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    positionX: ").append(toIndentedString(positionX)).append("\n");
    sb.append("    positionY: ").append(toIndentedString(positionY)).append("\n");
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

