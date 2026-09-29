package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * SiteMinderRoomTypeMapping
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SiteMinderRoomTypeMapping {

  private String id;

  private String siteMinderRoomType;

  private String roomId;

  private String roomName;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public SiteMinderRoomTypeMapping() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SiteMinderRoomTypeMapping(String id, String siteMinderRoomType, String roomId, String roomName, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.siteMinderRoomType = siteMinderRoomType;
    this.roomId = roomId;
    this.roomName = roomName;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public SiteMinderRoomTypeMapping id(String id) {
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

  public SiteMinderRoomTypeMapping siteMinderRoomType(String siteMinderRoomType) {
    this.siteMinderRoomType = siteMinderRoomType;
    return this;
  }

  /**
   * Get siteMinderRoomType
   * @return siteMinderRoomType
   */
  @NotNull 
  @JsonProperty("siteMinderRoomType")
  public String getSiteMinderRoomType() {
    return siteMinderRoomType;
  }

  public void setSiteMinderRoomType(String siteMinderRoomType) {
    this.siteMinderRoomType = siteMinderRoomType;
  }

  public SiteMinderRoomTypeMapping roomId(String roomId) {
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

  public SiteMinderRoomTypeMapping roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  /**
   * The mapped room type's current name, for display.
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

  public SiteMinderRoomTypeMapping createdAt(OffsetDateTime createdAt) {
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

  public SiteMinderRoomTypeMapping updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SiteMinderRoomTypeMapping siteMinderRoomTypeMapping = (SiteMinderRoomTypeMapping) o;
    return Objects.equals(this.id, siteMinderRoomTypeMapping.id) &&
        Objects.equals(this.siteMinderRoomType, siteMinderRoomTypeMapping.siteMinderRoomType) &&
        Objects.equals(this.roomId, siteMinderRoomTypeMapping.roomId) &&
        Objects.equals(this.roomName, siteMinderRoomTypeMapping.roomName) &&
        Objects.equals(this.createdAt, siteMinderRoomTypeMapping.createdAt) &&
        Objects.equals(this.updatedAt, siteMinderRoomTypeMapping.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, siteMinderRoomType, roomId, roomName, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SiteMinderRoomTypeMapping {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    siteMinderRoomType: ").append(toIndentedString(siteMinderRoomType)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

