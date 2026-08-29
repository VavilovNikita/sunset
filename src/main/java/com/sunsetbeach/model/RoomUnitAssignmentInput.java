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
 * Body of &#x60;PUT /bookings/{id}/room-unit&#x60;. &#x60;roomUnitId&#x60; must be present in the payload (either a string or explicit &#x60;null&#x60;) - deliberately NOT listed under &#x60;required&#x60; below despite that, because &#x60;org.openapitools:jackson-databind-nullable&#x60;&#39;s Jakarta Bean Validation integration registers an &#x60;@UnwrapByDefault&#x60; &#x60;ValueExtractor&#x60; for &#x60;JsonNullable&lt;T&gt;&#x60;: a generated &#x60;@NotNull&#x60; on a &#x60;nullable: true&#x60; + &#x60;required&#x60; property would validate the *unwrapped* value, rejecting the exact &#x60;null&#x60; this field exists to accept. Presence is checked manually in &#x60;BookingService&#x60;, the same way other rules Bean Validation can&#39;t correctly express are handled elsewhere in this API (see &#x60;ValidationException&#x60;). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T21:58:28.917463700+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class RoomUnitAssignmentInput {

  private JsonNullable<String> roomUnitId = JsonNullable.<String>undefined();

  public RoomUnitAssignmentInput roomUnitId(String roomUnitId) {
    this.roomUnitId = JsonNullable.of(roomUnitId);
    return this;
  }

  /**
   * The room unit to assign, or `null` to clear the current assignment.
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
    RoomUnitAssignmentInput roomUnitAssignmentInput = (RoomUnitAssignmentInput) o;
    return equalsNullable(this.roomUnitId, roomUnitAssignmentInput.roomUnitId);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(roomUnitId));
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
    sb.append("class RoomUnitAssignmentInput {\n");
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

