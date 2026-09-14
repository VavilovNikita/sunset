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
 * Body of &#x60;PATCH /users/{id}/overtime-eligibility&#x60;. See &#x60;User.overtimeEligible&#x60;&#39;s own description.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class UserOvertimeEligibilityUpdateInput {

  private Boolean overtimeEligible;

  public UserOvertimeEligibilityUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserOvertimeEligibilityUpdateInput(Boolean overtimeEligible) {
    this.overtimeEligible = overtimeEligible;
  }

  public UserOvertimeEligibilityUpdateInput overtimeEligible(Boolean overtimeEligible) {
    this.overtimeEligible = overtimeEligible;
    return this;
  }

  /**
   * Get overtimeEligible
   * @return overtimeEligible
   */
  @NotNull 
  @JsonProperty("overtimeEligible")
  public Boolean getOvertimeEligible() {
    return overtimeEligible;
  }

  public void setOvertimeEligible(Boolean overtimeEligible) {
    this.overtimeEligible = overtimeEligible;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserOvertimeEligibilityUpdateInput userOvertimeEligibilityUpdateInput = (UserOvertimeEligibilityUpdateInput) o;
    return Objects.equals(this.overtimeEligible, userOvertimeEligibilityUpdateInput.overtimeEligible);
  }

  @Override
  public int hashCode() {
    return Objects.hash(overtimeEligible);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserOvertimeEligibilityUpdateInput {\n");
    sb.append("    overtimeEligible: ").append(toIndentedString(overtimeEligible)).append("\n");
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

