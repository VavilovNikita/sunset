package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.PublicAvailabilityDay;
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
 * PublicAvailabilityResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PublicAvailabilityResponse {

  @Valid
  private List<@Valid PublicAvailabilityDay> days = new ArrayList<>();

  public PublicAvailabilityResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PublicAvailabilityResponse(List<@Valid PublicAvailabilityDay> days) {
    this.days = days;
  }

  public PublicAvailabilityResponse days(List<@Valid PublicAvailabilityDay> days) {
    this.days = days;
    return this;
  }

  public PublicAvailabilityResponse addDaysItem(PublicAvailabilityDay daysItem) {
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
  public List<@Valid PublicAvailabilityDay> getDays() {
    return days;
  }

  public void setDays(List<@Valid PublicAvailabilityDay> days) {
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
    PublicAvailabilityResponse publicAvailabilityResponse = (PublicAvailabilityResponse) o;
    return Objects.equals(this.days, publicAvailabilityResponse.days);
  }

  @Override
  public int hashCode() {
    return Objects.hash(days);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublicAvailabilityResponse {\n");
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

