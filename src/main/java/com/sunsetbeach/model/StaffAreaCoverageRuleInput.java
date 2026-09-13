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
 * Body of &#x60;PUT /staff-area-coverage-rules/{staffArea}&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class StaffAreaCoverageRuleInput {

  private Integer minimumWorking;

  public StaffAreaCoverageRuleInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StaffAreaCoverageRuleInput(Integer minimumWorking) {
    this.minimumWorking = minimumWorking;
  }

  public StaffAreaCoverageRuleInput minimumWorking(Integer minimumWorking) {
    this.minimumWorking = minimumWorking;
    return this;
  }

  /**
   * Get minimumWorking
   * minimum: 0
   * @return minimumWorking
   */
  @NotNull @Min(0) 
  @JsonProperty("minimumWorking")
  public Integer getMinimumWorking() {
    return minimumWorking;
  }

  public void setMinimumWorking(Integer minimumWorking) {
    this.minimumWorking = minimumWorking;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StaffAreaCoverageRuleInput staffAreaCoverageRuleInput = (StaffAreaCoverageRuleInput) o;
    return Objects.equals(this.minimumWorking, staffAreaCoverageRuleInput.minimumWorking);
  }

  @Override
  public int hashCode() {
    return Objects.hash(minimumWorking);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffAreaCoverageRuleInput {\n");
    sb.append("    minimumWorking: ").append(toIndentedString(minimumWorking)).append("\n");
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

