package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Body of &#x60;POST /bookings/{id}/relocate&#x60; and &#x60;POST /bookings/{id}/relocate/quote&#x60;. Unlike &#x60;BookingScheduleInput&#x60;, &#x60;roomId&#x60; (the room *type*) can change here - a relocation is exactly the operation that lets a guest move to a different type of room mid-stay, at that type&#39;s own price. &#x60;roomUnitId: null&#x60; is legal (relocate to \&quot;this type, unit not yet chosen\&quot;). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-30T18:13:29.409061300+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class RelocationInput {

  private String effectiveDate;

  private String roomId;

  private JsonNullable<String> roomUnitId = JsonNullable.<String>undefined();

  public RelocationInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RelocationInput(String effectiveDate, String roomId) {
    this.effectiveDate = effectiveDate;
    this.roomId = roomId;
  }

  public RelocationInput effectiveDate(String effectiveDate) {
    this.effectiveDate = effectiveDate;
    return this;
  }

  /**
   * The first night of the new segment. Must fall strictly after the start of the segment currently covering it.
   * @return effectiveDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("effectiveDate")
  public String getEffectiveDate() {
    return effectiveDate;
  }

  public void setEffectiveDate(String effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  public RelocationInput roomId(String roomId) {
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

  public RelocationInput roomUnitId(String roomUnitId) {
    this.roomUnitId = JsonNullable.of(roomUnitId);
    return this;
  }

  /**
   * Get roomUnitId
   * @return roomUnitId
   */
  
  @JsonProperty("roomUnitId")
  public JsonNullable<String> getRoomUnitId() {
    return roomUnitId;
  }

  public void setRoomUnitId(JsonNullable<String> roomUnitId) {
    this.roomUnitId = roomUnitId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RelocationInput relocationInput = (RelocationInput) o;
    return Objects.equals(this.effectiveDate, relocationInput.effectiveDate) &&
        Objects.equals(this.roomId, relocationInput.roomId) &&
        equalsNullable(this.roomUnitId, relocationInput.roomUnitId);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(effectiveDate, roomId, hashCodeNullable(roomUnitId));
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
    sb.append("class RelocationInput {\n");
    sb.append("    effectiveDate: ").append(toIndentedString(effectiveDate)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
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

