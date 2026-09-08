package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.MaintenanceTask;
import com.sunsetbeach.model.RoomUnitBlockResult;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;POST /maintenance-tasks/{id}/block&#x60;. &#x60;blockResult&#x60; is exactly what &#x60;POST /room-units/{id}/blocks&#x60; itself returns (same overlap warning, same two affected- bookings populations) - linking a block to a task doesn&#39;t change that behavior, it reuses it. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class MaintenanceTaskBlockResult {

  private MaintenanceTask task;

  private RoomUnitBlockResult blockResult;

  public MaintenanceTaskBlockResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MaintenanceTaskBlockResult(MaintenanceTask task, RoomUnitBlockResult blockResult) {
    this.task = task;
    this.blockResult = blockResult;
  }

  public MaintenanceTaskBlockResult task(MaintenanceTask task) {
    this.task = task;
    return this;
  }

  /**
   * Get task
   * @return task
   */
  @NotNull @Valid 
  @JsonProperty("task")
  public MaintenanceTask getTask() {
    return task;
  }

  public void setTask(MaintenanceTask task) {
    this.task = task;
  }

  public MaintenanceTaskBlockResult blockResult(RoomUnitBlockResult blockResult) {
    this.blockResult = blockResult;
    return this;
  }

  /**
   * Get blockResult
   * @return blockResult
   */
  @NotNull @Valid 
  @JsonProperty("blockResult")
  public RoomUnitBlockResult getBlockResult() {
    return blockResult;
  }

  public void setBlockResult(RoomUnitBlockResult blockResult) {
    this.blockResult = blockResult;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MaintenanceTaskBlockResult maintenanceTaskBlockResult = (MaintenanceTaskBlockResult) o;
    return Objects.equals(this.task, maintenanceTaskBlockResult.task) &&
        Objects.equals(this.blockResult, maintenanceTaskBlockResult.blockResult);
  }

  @Override
  public int hashCode() {
    return Objects.hash(task, blockResult);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MaintenanceTaskBlockResult {\n");
    sb.append("    task: ").append(toIndentedString(task)).append("\n");
    sb.append("    blockResult: ").append(toIndentedString(blockResult)).append("\n");
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

