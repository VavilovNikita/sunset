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
 * A physical POS table/spot a guest tab can be opened against. Mapped to a &#x60;PosTable&#x60; database table (not &#x60;Table&#x60;) to sidestep the SQL keyword collision — the API schema name is unaffected. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T15:38:53.433655500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class Table {

  private String id;

  private Zone zone;

  private String label;

  private Integer capacity;

  private Boolean isActive;

  public Table() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Table(String id, Zone zone, String label, Integer capacity, Boolean isActive) {
    this.id = id;
    this.zone = zone;
    this.label = label;
    this.capacity = capacity;
    this.isActive = isActive;
  }

  public Table id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Table zone(Zone zone) {
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

  public Table label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  @NotNull 
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Table capacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * Get capacity
   * @return capacity
   */
  @NotNull 
  @JsonProperty("capacity")
  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public Table isActive(Boolean isActive) {
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
    Table table = (Table) o;
    return Objects.equals(this.id, table.id) &&
        Objects.equals(this.zone, table.zone) &&
        Objects.equals(this.label, table.label) &&
        Objects.equals(this.capacity, table.capacity) &&
        Objects.equals(this.isActive, table.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, zone, label, capacity, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Table {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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

