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
 * Summary of the maintenance task this block was raised for, if any - a block can exist with no task at all, and a task can be linked to an existing block after the fact (see &#x60;MaintenanceTask.blockId&#x60;), so this is resolved live at read time, not stored on the block itself. Deliberately not the same shape as &#x60;PropertyMapMaintenanceTask&#x60; - no &#x60;blockExpired&#x60; here, since that field answers \&quot;has this block&#39;s own date range already passed while the task is still open,\&quot; which is meaningless when the block being described is the very one already on screen. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RoomUnitBlockMaintenanceTask {

  private String taskId;

  private String description;

  private MaintenanceTaskStatus status;

  public RoomUnitBlockMaintenanceTask() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomUnitBlockMaintenanceTask(String taskId, String description, MaintenanceTaskStatus status) {
    this.taskId = taskId;
    this.description = description;
    this.status = status;
  }

  public RoomUnitBlockMaintenanceTask taskId(String taskId) {
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

  public RoomUnitBlockMaintenanceTask description(String description) {
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

  public RoomUnitBlockMaintenanceTask status(MaintenanceTaskStatus status) {
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
    RoomUnitBlockMaintenanceTask roomUnitBlockMaintenanceTask = (RoomUnitBlockMaintenanceTask) o;
    return Objects.equals(this.taskId, roomUnitBlockMaintenanceTask.taskId) &&
        Objects.equals(this.description, roomUnitBlockMaintenanceTask.description) &&
        Objects.equals(this.status, roomUnitBlockMaintenanceTask.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskId, description, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUnitBlockMaintenanceTask {\n");
    sb.append("    taskId: ").append(toIndentedString(taskId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

