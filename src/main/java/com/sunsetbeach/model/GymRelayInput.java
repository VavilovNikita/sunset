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
 * GymRelayInput
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymRelayInput {

  private Boolean isOn;

  public GymRelayInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymRelayInput(Boolean isOn) {
    this.isOn = isOn;
  }

  public GymRelayInput isOn(Boolean isOn) {
    this.isOn = isOn;
    return this;
  }

  /**
   * Get isOn
   * @return isOn
   */
  @NotNull 
  @JsonProperty("isOn")
  public Boolean getIsOn() {
    return isOn;
  }

  public void setIsOn(Boolean isOn) {
    this.isOn = isOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymRelayInput gymRelayInput = (GymRelayInput) o;
    return Objects.equals(this.isOn, gymRelayInput.isOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymRelayInput {\n");
    sb.append("    isOn: ").append(toIndentedString(isOn)).append("\n");
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

