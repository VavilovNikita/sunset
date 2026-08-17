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
 * Body of &#x60;PUT /bookings/{id}/room-unit&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T15:17:55.380996100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class RoomUnitAssignmentInput {

  private JsonNullable<String> roomUnitId = JsonNullable.<String>undefined();

  public RoomUnitAssignmentInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomUnitAssignmentInput(String roomUnitId) {
    this.roomUnitId = JsonNullable.of(roomUnitId);
  }

  public RoomUnitAssignmentInput roomUnitId(String roomUnitId) {
    this.roomUnitId = JsonNullable.of(roomUnitId);
    return this;
  }

  /**
   * The room unit to assign, or `null` to clear the current assignment.
   * @return roomUnitId
   */
  @NotNull 
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
    return Objects.equals(this.roomUnitId, roomUnitAssignmentInput.roomUnitId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomUnitId);
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

