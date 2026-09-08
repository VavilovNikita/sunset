package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PATCH /maintenance-tasks/{id}/status&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class MaintenanceTaskStatusUpdateInput {

  private MaintenanceTaskStatus status;

  public MaintenanceTaskStatusUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MaintenanceTaskStatusUpdateInput(MaintenanceTaskStatus status) {
    this.status = status;
  }

  public MaintenanceTaskStatusUpdateInput status(MaintenanceTaskStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public MaintenanceTaskStatus getStatus() {
    return status;
  }

  public void setStatus(MaintenanceTaskStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MaintenanceTaskStatusUpdateInput maintenanceTaskStatusUpdateInput = (MaintenanceTaskStatusUpdateInput) o;
    return Objects.equals(this.status, maintenanceTaskStatusUpdateInput.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MaintenanceTaskStatusUpdateInput {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

