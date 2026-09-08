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
 * Summary of the most urgent open (&#x60;OPEN&#x60;/&#x60;IN_PROGRESS&#x60;) &#x60;MaintenanceTask&#x60; on a &#x60;PropertyMapUnit&#x60;, if any - see that schema&#39;s &#x60;openMaintenanceTask&#x60;. When a unit has more than one open task, the one with &#x60;blockExpired: true&#x60; is preferred (if any), else the oldest. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PropertyMapMaintenanceTask {

  private String taskId;

  private String description;

  private MaintenanceTaskStatus status;

  private Boolean blockExpired;

  public PropertyMapMaintenanceTask() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PropertyMapMaintenanceTask(String taskId, String description, MaintenanceTaskStatus status, Boolean blockExpired) {
    this.taskId = taskId;
    this.description = description;
    this.status = status;
    this.blockExpired = blockExpired;
  }

  public PropertyMapMaintenanceTask taskId(String taskId) {
    this.taskId = taskId;
    return this;
  }

  /**
   * Get taskId
   * @return taskId
   */
  @NotNull 
  @JsonProperty("taskId")
  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public PropertyMapMaintenanceTask description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public PropertyMapMaintenanceTask status(MaintenanceTaskStatus status) {
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

  public PropertyMapMaintenanceTask blockExpired(Boolean blockExpired) {
    this.blockExpired = blockExpired;
    return this;
  }

  /**
   * True when this task has a linked block whose `toDate` has already passed while the task is still open - the room has silently returned to sale while still broken. The one state this field exists to catch; everything else about the task is informational. 
   * @return blockExpired
   */
  @NotNull 
  @JsonProperty("blockExpired")
  public Boolean getBlockExpired() {
    return blockExpired;
  }

  public void setBlockExpired(Boolean blockExpired) {
    this.blockExpired = blockExpired;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertyMapMaintenanceTask propertyMapMaintenanceTask = (PropertyMapMaintenanceTask) o;
    return Objects.equals(this.taskId, propertyMapMaintenanceTask.taskId) &&
        Objects.equals(this.description, propertyMapMaintenanceTask.description) &&
        Objects.equals(this.status, propertyMapMaintenanceTask.status) &&
        Objects.equals(this.blockExpired, propertyMapMaintenanceTask.blockExpired);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskId, description, status, blockExpired);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertyMapMaintenanceTask {\n");
    sb.append("    taskId: ").append(toIndentedString(taskId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    blockExpired: ").append(toIndentedString(blockExpired)).append("\n");
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

