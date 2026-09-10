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
 * Body of &#x60;POST /bookings/{id}/segments/{segmentId}/swap-room-unit&#x60;. The dragged segment is named by the URL (&#x60;{id}&#x60;/&#x60;{segmentId}&#x60;); this names the other side - the gesture has a direction (someone drags A onto B), so the operation does too. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SwapSegmentRoomUnitInput {

  private String withSegmentId;

  public SwapSegmentRoomUnitInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SwapSegmentRoomUnitInput(String withSegmentId) {
    this.withSegmentId = withSegmentId;
  }

  public SwapSegmentRoomUnitInput withSegmentId(String withSegmentId) {
    this.withSegmentId = withSegmentId;
    return this;
  }

  /**
   * The other segment to swap room units with. Must currently be assigned a room unit of the same room type as the URL's own segment.
   * @return withSegmentId
   */
  @NotNull 
  @JsonProperty("withSegmentId")
  public String getWithSegmentId() {
    return withSegmentId;
  }

  public void setWithSegmentId(String withSegmentId) {
    this.withSegmentId = withSegmentId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SwapSegmentRoomUnitInput swapSegmentRoomUnitInput = (SwapSegmentRoomUnitInput) o;
    return Objects.equals(this.withSegmentId, swapSegmentRoomUnitInput.withSegmentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(withSegmentId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SwapSegmentRoomUnitInput {\n");
    sb.append("    withSegmentId: ").append(toIndentedString(withSegmentId)).append("\n");
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

