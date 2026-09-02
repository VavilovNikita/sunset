package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One entry of the array body of &#x60;PATCH /room-units/positions&#x60;. &#x60;positionX&#x60;/&#x60;positionY&#x60; must both be null (clears the position, returning the unit to \&quot;not placed\&quot;) or both set within 0..1 - never one without the other; checked manually in &#x60;RoomUnitService&#x60;, not via &#x60;required&#x60; below. Deliberately NOT listing &#x60;positionX&#x60;/&#x60;positionY&#x60; under &#x60;required&#x60; despite them conceptually needing to be present: &#x60;org.openapitools:jackson-databind- nullable&#x60;&#39;s Bean Validation integration registers an &#x60;@UnwrapByDefault&#x60; &#x60;ValueExtractor&#x60; for &#x60;JsonNullable&lt;T&gt;&#x60;, so a generated &#x60;@NotNull&#x60; on a &#x60;nullable: true&#x60; + &#x60;required&#x60; property validates the *unwrapped* value, rejecting the exact &#x60;null&#x60; these fields exist to accept - same reasoning as &#x60;RoomUnitAssignmentInput.roomUnitId&#x60; above. 
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
  public RoomUnitPositionInput(String roomUnitId) {
    this.roomUnitId = roomUnitId;
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
  @Valid @DecimalMin("0") @DecimalMax("1") 
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
  @Valid @DecimalMin("0") @DecimalMax("1") 
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
        equalsNullable(this.positionX, roomUnitPositionInput.positionX) &&
        equalsNullable(this.positionY, roomUnitPositionInput.positionY);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomUnitId, hashCodeNullable(positionX), hashCodeNullable(positionY));
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

