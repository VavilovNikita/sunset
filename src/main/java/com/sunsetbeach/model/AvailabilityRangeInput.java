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
 * Body of &#x60;availabilityRangeSchema&#x60;. Upserts one &#x60;Availability&#x60; row per day in &#x60;[from, to]&#x60; with &#x60;blockedCount&#x60; units of the room type pulled from sale. &#x60;blockedCount: 0&#x60; deletes the row for that day instead (zero is equivalent to no row existing). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-16T18:39:34.635637100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class AvailabilityRangeInput {

  private String from;

  private String to;

  private Integer blockedCount;

  public AvailabilityRangeInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AvailabilityRangeInput(String from, String to, Integer blockedCount) {
    this.from = from;
    this.to = to;
    this.blockedCount = blockedCount;
  }

  public AvailabilityRangeInput from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public AvailabilityRangeInput to(String to) {
    this.to = to;
    return this;
  }

  /**
   * Get to
   * @return to
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public AvailabilityRangeInput blockedCount(Integer blockedCount) {
    this.blockedCount = blockedCount;
    return this;
  }

  /**
   * Must not exceed the room's `quantity` - rejected with 400 otherwise.
   * minimum: 0
   * @return blockedCount
   */
  @NotNull @Min(0) 
  @JsonProperty("blockedCount")
  public Integer getBlockedCount() {
    return blockedCount;
  }

  public void setBlockedCount(Integer blockedCount) {
    this.blockedCount = blockedCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AvailabilityRangeInput availabilityRangeInput = (AvailabilityRangeInput) o;
    return Objects.equals(this.from, availabilityRangeInput.from) &&
        Objects.equals(this.to, availabilityRangeInput.to) &&
        Objects.equals(this.blockedCount, availabilityRangeInput.blockedCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, blockedCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AvailabilityRangeInput {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    blockedCount: ").append(toIndentedString(blockedCount)).append("\n");
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

