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
 * Body of &#x60;PATCH /room-units/{id}&#x60;. Full replacement of &#x60;label&#x60;/&#x60;isActive&#x60; — &#x60;roomId&#x60; is immutable after creation.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T15:17:55.380996100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class RoomUnitUpdateInput {

  private String label;

  private Boolean isActive;

  public RoomUnitUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomUnitUpdateInput(String label, Boolean isActive) {
    this.label = label;
    this.isActive = isActive;
  }

  public RoomUnitUpdateInput label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  @NotNull @Size(min = 1, max = 60) 
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public RoomUnitUpdateInput isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Get isActive
   * @return isActive
   */
  @NotNull 
  @JsonProperty("isActive")
  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomUnitUpdateInput roomUnitUpdateInput = (RoomUnitUpdateInput) o;
    return Objects.equals(this.label, roomUnitUpdateInput.label) &&
        Objects.equals(this.isActive, roomUnitUpdateInput.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUnitUpdateInput {\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

