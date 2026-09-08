package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A reported problem in a physical room. &#x60;roomId&#x60;/&#x60;roomName&#x60;/&#x60;unitLabel&#x60; are denormalized display fields (same pattern as &#x60;PropertyMapUnit&#x60;) so the UI doesn&#39;t need a second round trip. &#x60;blockId&#x60; is null until a manager links one via &#x60;POST /maintenance-tasks/{id}/block&#x60; - a block is optional, not every task needs the room pulled off sale. &#x60;closedAt&#x60; is null until &#x60;status&#x60; becomes &#x60;DONE&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class MaintenanceTask {

  private String id;

  private String roomUnitId;

  private String roomId;

  private String roomName;

  private String unitLabel;

  private String description;

  private MaintenanceTaskStatus status;

  private JsonNullable<String> blockId = JsonNullable.<String>undefined();

  private String reportedByUserId;

  private String reportedByEmail;

  @Valid
  private List<String> photos = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> closedAt = JsonNullable.<OffsetDateTime>undefined();

  public MaintenanceTask() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MaintenanceTask(String id, String roomUnitId, String roomId, String roomName, String unitLabel, String description, MaintenanceTaskStatus status, String blockId, String reportedByUserId, String reportedByEmail, List<String> photos, OffsetDateTime createdAt, OffsetDateTime closedAt) {
    this.id = id;
    this.roomUnitId = roomUnitId;
    this.roomId = roomId;
    this.roomName = roomName;
    this.unitLabel = unitLabel;
    this.description = description;
    this.status = status;
    this.blockId = JsonNullable.of(blockId);
    this.reportedByUserId = reportedByUserId;
    this.reportedByEmail = reportedByEmail;
    this.photos = photos;
    this.createdAt = createdAt;
    this.closedAt = JsonNullable.of(closedAt);
  }

  public MaintenanceTask id(String id) {
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

  public MaintenanceTask roomUnitId(String roomUnitId) {
    this.roomUnitId = roomUnitId;
    return this;
  }

  /**
   * Get roomUnitId
   * @return roomUnitId
   */
  @NotNull 
  @JsonProperty("roomUnitId")
  public String getRoomUnitId() {
    return roomUnitId;
  }

  public void setRoomUnitId(String roomUnitId) {
    this.roomUnitId = roomUnitId;
  }

  public MaintenanceTask roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * Get roomId
   * @return roomId
   */
  @NotNull 
  @JsonProperty("roomId")
  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public MaintenanceTask roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  /**
   * Get roomName
   * @return roomName
   */
  @NotNull 
  @JsonProperty("roomName")
  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public MaintenanceTask unitLabel(String unitLabel) {
    this.unitLabel = unitLabel;
    return this;
  }

  /**
   * Get unitLabel
   * @return unitLabel
   */
  @NotNull 
  @JsonProperty("unitLabel")
  public String getUnitLabel() {
    return unitLabel;
  }

  public void setUnitLabel(String unitLabel) {
    this.unitLabel = unitLabel;
  }

  public MaintenanceTask description(String description) {
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

  public MaintenanceTask status(MaintenanceTaskStatus status) {
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

  public MaintenanceTask blockId(String blockId) {
    this.blockId = JsonNullable.of(blockId);
    return this;
  }

  /**
   * Get blockId
   * @return blockId
   */
  @NotNull 
  @JsonProperty("blockId")
  public JsonNullable<String> getBlockId() {
    return blockId;
  }

  public void setBlockId(JsonNullable<String> blockId) {
    this.blockId = blockId;
  }

  public MaintenanceTask reportedByUserId(String reportedByUserId) {
    this.reportedByUserId = reportedByUserId;
    return this;
  }

  /**
   * Get reportedByUserId
   * @return reportedByUserId
   */
  @NotNull 
  @JsonProperty("reportedByUserId")
  public String getReportedByUserId() {
    return reportedByUserId;
  }

  public void setReportedByUserId(String reportedByUserId) {
    this.reportedByUserId = reportedByUserId;
  }

  public MaintenanceTask reportedByEmail(String reportedByEmail) {
    this.reportedByEmail = reportedByEmail;
    return this;
  }

  /**
   * Get reportedByEmail
   * @return reportedByEmail
   */
  @NotNull 
  @JsonProperty("reportedByEmail")
  public String getReportedByEmail() {
    return reportedByEmail;
  }

  public void setReportedByEmail(String reportedByEmail) {
    this.reportedByEmail = reportedByEmail;
  }

  public MaintenanceTask photos(List<String> photos) {
    this.photos = photos;
    return this;
  }

  public MaintenanceTask addPhotosItem(String photosItem) {
    if (this.photos == null) {
      this.photos = new ArrayList<>();
    }
    this.photos.add(photosItem);
    return this;
  }

  /**
   * Served paths under `/maintenance-tasks/{id}/photos/{filename}` - staff-only, never `/uploads/_**`.
   * @return photos
   */
  @NotNull 
  @JsonProperty("photos")
  public List<String> getPhotos() {
    return photos;
  }

  public void setPhotos(List<String> photos) {
    this.photos = photos;
  }

  public MaintenanceTask createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public MaintenanceTask closedAt(OffsetDateTime closedAt) {
    this.closedAt = JsonNullable.of(closedAt);
    return this;
  }

  /**
   * Get closedAt
   * @return closedAt
   */
  @NotNull @Valid 
  @JsonProperty("closedAt")
  public JsonNullable<OffsetDateTime> getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(JsonNullable<OffsetDateTime> closedAt) {
    this.closedAt = closedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MaintenanceTask maintenanceTask = (MaintenanceTask) o;
    return Objects.equals(this.id, maintenanceTask.id) &&
        Objects.equals(this.roomUnitId, maintenanceTask.roomUnitId) &&
        Objects.equals(this.roomId, maintenanceTask.roomId) &&
        Objects.equals(this.roomName, maintenanceTask.roomName) &&
        Objects.equals(this.unitLabel, maintenanceTask.unitLabel) &&
        Objects.equals(this.description, maintenanceTask.description) &&
        Objects.equals(this.status, maintenanceTask.status) &&
        Objects.equals(this.blockId, maintenanceTask.blockId) &&
        Objects.equals(this.reportedByUserId, maintenanceTask.reportedByUserId) &&
        Objects.equals(this.reportedByEmail, maintenanceTask.reportedByEmail) &&
        Objects.equals(this.photos, maintenanceTask.photos) &&
        Objects.equals(this.createdAt, maintenanceTask.createdAt) &&
        Objects.equals(this.closedAt, maintenanceTask.closedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, roomUnitId, roomId, roomName, unitLabel, description, status, blockId, reportedByUserId, reportedByEmail, photos, createdAt, closedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MaintenanceTask {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    unitLabel: ").append(toIndentedString(unitLabel)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    blockId: ").append(toIndentedString(blockId)).append("\n");
    sb.append("    reportedByUserId: ").append(toIndentedString(reportedByUserId)).append("\n");
    sb.append("    reportedByEmail: ").append(toIndentedString(reportedByEmail)).append("\n");
    sb.append("    photos: ").append(toIndentedString(photos)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    closedAt: ").append(toIndentedString(closedAt)).append("\n");
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

