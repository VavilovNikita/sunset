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
 * Body of &#x60;POST /bookings/{id}/reprice&#x60; and &#x60;POST /bookings/{id}/reprice/quote&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RepriceInput {

  private String segmentId;

  public RepriceInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RepriceInput(String segmentId) {
    this.segmentId = segmentId;
  }

  public RepriceInput segmentId(String segmentId) {
    this.segmentId = segmentId;
    return this;
  }

  /**
   * Which segment to reprice - required rather than inferred, since a relocated booking has more than one and there is no single unambiguous default.
   * @return segmentId
   */
  @NotNull 
  @JsonProperty("segmentId")
  public String getSegmentId() {
    return segmentId;
  }

  public void setSegmentId(String segmentId) {
    this.segmentId = segmentId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RepriceInput repriceInput = (RepriceInput) o;
    return Objects.equals(this.segmentId, repriceInput.segmentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(segmentId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RepriceInput {\n");
    sb.append("    segmentId: ").append(toIndentedString(segmentId)).append("\n");
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

