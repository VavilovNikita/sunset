package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.HousekeepingStatus;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PATCH /room-units/{id}/housekeeping&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class HousekeepingStatusInput {

  private HousekeepingStatus housekeepingStatus;

  public HousekeepingStatusInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public HousekeepingStatusInput(HousekeepingStatus housekeepingStatus) {
    this.housekeepingStatus = housekeepingStatus;
  }

  public HousekeepingStatusInput housekeepingStatus(HousekeepingStatus housekeepingStatus) {
    this.housekeepingStatus = housekeepingStatus;
    return this;
  }

  /**
   * Get housekeepingStatus
   * @return housekeepingStatus
   */
  @NotNull @Valid 
  @JsonProperty("housekeepingStatus")
  public HousekeepingStatus getHousekeepingStatus() {
    return housekeepingStatus;
  }

  public void setHousekeepingStatus(HousekeepingStatus housekeepingStatus) {
    this.housekeepingStatus = housekeepingStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HousekeepingStatusInput housekeepingStatusInput = (HousekeepingStatusInput) o;
    return Objects.equals(this.housekeepingStatus, housekeepingStatusInput.housekeepingStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(housekeepingStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HousekeepingStatusInput {\n");
    sb.append("    housekeepingStatus: ").append(toIndentedString(housekeepingStatus)).append("\n");
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

