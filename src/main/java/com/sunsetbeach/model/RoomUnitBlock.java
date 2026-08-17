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
 * A physical room pulled off sale for a date range, with a reason. Replaces the old Room-level &#x60;Availability.blockedCount&#x60;. &#x60;fromDate&#x60;/&#x60;toDate&#x60; are both inclusive. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T15:17:55.380996100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class RoomUnitBlock {

  private String id;

  private String roomUnitId;

  private String fromDate;

  private String toDate;

  private String reason;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public RoomUnitBlock() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomUnitBlock(String id, String roomUnitId, String fromDate, String toDate, String reason, OffsetDateTime createdAt) {
    this.id = id;
    this.roomUnitId = roomUnitId;
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.reason = reason;
    this.createdAt = createdAt;
  }

  public RoomUnitBlock id(String id) {
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

  public RoomUnitBlock roomUnitId(String roomUnitId) {
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

  public RoomUnitBlock fromDate(String fromDate) {
    this.fromDate = fromDate;
    return this;
  }

  /**
   * Get fromDate
   * @return fromDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("fromDate")
  public String getFromDate() {
    return fromDate;
  }

  public void setFromDate(String fromDate) {
    this.fromDate = fromDate;
  }

  public RoomUnitBlock toDate(String toDate) {
    this.toDate = toDate;
    return this;
  }

  /**
   * Get toDate
   * @return toDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("toDate")
  public String getToDate() {
    return toDate;
  }

  public void setToDate(String toDate) {
    this.toDate = toDate;
  }

  public RoomUnitBlock reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Get reason
   * @return reason
   */
  @NotNull 
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public RoomUnitBlock createdAt(OffsetDateTime createdAt) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomUnitBlock roomUnitBlock = (RoomUnitBlock) o;
    return Objects.equals(this.id, roomUnitBlock.id) &&
        Objects.equals(this.roomUnitId, roomUnitBlock.roomUnitId) &&
        Objects.equals(this.fromDate, roomUnitBlock.fromDate) &&
        Objects.equals(this.toDate, roomUnitBlock.toDate) &&
        Objects.equals(this.reason, roomUnitBlock.reason) &&
        Objects.equals(this.createdAt, roomUnitBlock.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, roomUnitId, fromDate, toDate, reason, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUnitBlock {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
    sb.append("    fromDate: ").append(toIndentedString(fromDate)).append("\n");
    sb.append("    toDate: ").append(toIndentedString(toDate)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

