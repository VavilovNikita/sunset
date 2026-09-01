package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.Zone;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Full replacement on PATCH — no partial update.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TableInput {

  private Zone zone;

  private String label;

  private Integer capacity;

  private Boolean isActive = true;

  public TableInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TableInput(Zone zone, String label, Integer capacity) {
    this.zone = zone;
    this.label = label;
    this.capacity = capacity;
  }

  public TableInput zone(Zone zone) {
    this.zone = zone;
    return this;
  }

  /**
   * Get zone
   * @return zone
   */
  @NotNull @Valid 
  @JsonProperty("zone")
  public Zone getZone() {
    return zone;
  }

  public void setZone(Zone zone) {
    this.zone = zone;
  }

  public TableInput label(String label) {
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

  public TableInput capacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * Get capacity
   * minimum: 1
   * maximum: 50
   * @return capacity
   */
  @NotNull @Min(1) @Max(50) 
  @JsonProperty("capacity")
  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public TableInput isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Get isActive
   * @return isActive
   */
  
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
    TableInput tableInput = (TableInput) o;
    return Objects.equals(this.zone, tableInput.zone) &&
        Objects.equals(this.label, tableInput.label) &&
        Objects.equals(this.capacity, tableInput.capacity) &&
        Objects.equals(this.isActive, tableInput.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(zone, label, capacity, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TableInput {\n");
    sb.append("    zone: ").append(toIndentedString(zone)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
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

