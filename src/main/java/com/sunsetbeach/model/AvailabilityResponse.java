package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.AvailabilityDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * AvailabilityResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-17T16:01:20.967720600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class AvailabilityResponse {

  @Valid
  private List<@Valid AvailabilityDay> days = new ArrayList<>();

  public AvailabilityResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AvailabilityResponse(List<@Valid AvailabilityDay> days) {
    this.days = days;
  }

  public AvailabilityResponse days(List<@Valid AvailabilityDay> days) {
    this.days = days;
    return this;
  }

  public AvailabilityResponse addDaysItem(AvailabilityDay daysItem) {
    if (this.days == null) {
      this.days = new ArrayList<>();
    }
    this.days.add(daysItem);
    return this;
  }

  /**
   * Get days
   * @return days
   */
  @NotNull @Valid 
  @JsonProperty("days")
  public List<@Valid AvailabilityDay> getDays() {
    return days;
  }

  public void setDays(List<@Valid AvailabilityDay> days) {
    this.days = days;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AvailabilityResponse availabilityResponse = (AvailabilityResponse) o;
    return Objects.equals(this.days, availabilityResponse.days);
  }

  @Override
  public int hashCode() {
    return Objects.hash(days);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AvailabilityResponse {\n");
    sb.append("    days: ").append(toIndentedString(days)).append("\n");
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

