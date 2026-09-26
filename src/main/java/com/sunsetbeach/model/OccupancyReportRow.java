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
 * One room type&#39;s figures, or the property total (where &#x60;roomId&#x60;/&#x60;roomName&#x60; are null). Money is a decimal string with two decimals. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class OccupancyReportRow {

  private JsonNullable<String> roomId = JsonNullable.<String>undefined();

  private JsonNullable<String> roomName = JsonNullable.<String>undefined();

  private Integer activeUnits;

  private Integer roomNightsAvailable;

  private Integer roomNightsSold;

  private JsonNullable<String> occupancyPercent = JsonNullable.<String>undefined();

  private String roomRevenue;

  private JsonNullable<String> adr = JsonNullable.<String>undefined();

  private JsonNullable<String> revpar = JsonNullable.<String>undefined();

  public OccupancyReportRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OccupancyReportRow(String roomId, String roomName, Integer activeUnits, Integer roomNightsAvailable, Integer roomNightsSold, String occupancyPercent, String roomRevenue, String adr, String revpar) {
    this.roomId = JsonNullable.of(roomId);
    this.roomName = JsonNullable.of(roomName);
    this.activeUnits = activeUnits;
    this.roomNightsAvailable = roomNightsAvailable;
    this.roomNightsSold = roomNightsSold;
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    this.roomRevenue = roomRevenue;
    this.adr = JsonNullable.of(adr);
    this.revpar = JsonNullable.of(revpar);
  }

  public OccupancyReportRow roomId(String roomId) {
    this.roomId = JsonNullable.of(roomId);
    return this;
  }

  /**
   * Get roomId
   * @return roomId
   */
  @NotNull 
  @JsonProperty("roomId")
  public JsonNullable<String> getRoomId() {
    return roomId;
  }

  public void setRoomId(JsonNullable<String> roomId) {
    this.roomId = roomId;
  }

  public OccupancyReportRow roomName(String roomName) {
    this.roomName = JsonNullable.of(roomName);
    return this;
  }

  /**
   * Get roomName
   * @return roomName
   */
  @NotNull 
  @JsonProperty("roomName")
  public JsonNullable<String> getRoomName() {
    return roomName;
  }

  public void setRoomName(JsonNullable<String> roomName) {
    this.roomName = roomName;
  }

  public OccupancyReportRow activeUnits(Integer activeUnits) {
    this.activeUnits = activeUnits;
    return this;
  }

  /**
   * Physical units of this type active today - see the operation's note on why this is today's count.
   * @return activeUnits
   */
  @NotNull 
  @JsonProperty("activeUnits")
  public Integer getActiveUnits() {
    return activeUnits;
  }

  public void setActiveUnits(Integer activeUnits) {
    this.activeUnits = activeUnits;
  }

  public OccupancyReportRow roomNightsAvailable(Integer roomNightsAvailable) {
    this.roomNightsAvailable = roomNightsAvailable;
    return this;
  }

  /**
   * Get roomNightsAvailable
   * @return roomNightsAvailable
   */
  @NotNull 
  @JsonProperty("roomNightsAvailable")
  public Integer getRoomNightsAvailable() {
    return roomNightsAvailable;
  }

  public void setRoomNightsAvailable(Integer roomNightsAvailable) {
    this.roomNightsAvailable = roomNightsAvailable;
  }

  public OccupancyReportRow roomNightsSold(Integer roomNightsSold) {
    this.roomNightsSold = roomNightsSold;
    return this;
  }

  /**
   * Get roomNightsSold
   * @return roomNightsSold
   */
  @NotNull 
  @JsonProperty("roomNightsSold")
  public Integer getRoomNightsSold() {
    return roomNightsSold;
  }

  public void setRoomNightsSold(Integer roomNightsSold) {
    this.roomNightsSold = roomNightsSold;
  }

  public OccupancyReportRow occupancyPercent(String occupancyPercent) {
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    return this;
  }

  /**
   * sold / available × 100, two decimals (e.g. \"66.67\"). Null when nothing was available.
   * @return occupancyPercent
   */
  @NotNull 
  @JsonProperty("occupancyPercent")
  public JsonNullable<String> getOccupancyPercent() {
    return occupancyPercent;
  }

  public void setOccupancyPercent(JsonNullable<String> occupancyPercent) {
    this.occupancyPercent = occupancyPercent;
  }

  public OccupancyReportRow roomRevenue(String roomRevenue) {
    this.roomRevenue = roomRevenue;
    return this;
  }

  /**
   * Get roomRevenue
   * @return roomRevenue
   */
  @NotNull 
  @JsonProperty("roomRevenue")
  public String getRoomRevenue() {
    return roomRevenue;
  }

  public void setRoomRevenue(String roomRevenue) {
    this.roomRevenue = roomRevenue;
  }

  public OccupancyReportRow adr(String adr) {
    this.adr = JsonNullable.of(adr);
    return this;
  }

  /**
   * Room revenue / room-nights sold. Null when nothing was sold.
   * @return adr
   */
  @NotNull 
  @JsonProperty("adr")
  public JsonNullable<String> getAdr() {
    return adr;
  }

  public void setAdr(JsonNullable<String> adr) {
    this.adr = adr;
  }

  public OccupancyReportRow revpar(String revpar) {
    this.revpar = JsonNullable.of(revpar);
    return this;
  }

  /**
   * Room revenue / room-nights available. Null when nothing was available.
   * @return revpar
   */
  @NotNull 
  @JsonProperty("revpar")
  public JsonNullable<String> getRevpar() {
    return revpar;
  }

  public void setRevpar(JsonNullable<String> revpar) {
    this.revpar = revpar;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OccupancyReportRow occupancyReportRow = (OccupancyReportRow) o;
    return Objects.equals(this.roomId, occupancyReportRow.roomId) &&
        Objects.equals(this.roomName, occupancyReportRow.roomName) &&
        Objects.equals(this.activeUnits, occupancyReportRow.activeUnits) &&
        Objects.equals(this.roomNightsAvailable, occupancyReportRow.roomNightsAvailable) &&
        Objects.equals(this.roomNightsSold, occupancyReportRow.roomNightsSold) &&
        Objects.equals(this.occupancyPercent, occupancyReportRow.occupancyPercent) &&
        Objects.equals(this.roomRevenue, occupancyReportRow.roomRevenue) &&
        Objects.equals(this.adr, occupancyReportRow.adr) &&
        Objects.equals(this.revpar, occupancyReportRow.revpar);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, roomName, activeUnits, roomNightsAvailable, roomNightsSold, occupancyPercent, roomRevenue, adr, revpar);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OccupancyReportRow {\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    activeUnits: ").append(toIndentedString(activeUnits)).append("\n");
    sb.append("    roomNightsAvailable: ").append(toIndentedString(roomNightsAvailable)).append("\n");
    sb.append("    roomNightsSold: ").append(toIndentedString(roomNightsSold)).append("\n");
    sb.append("    occupancyPercent: ").append(toIndentedString(occupancyPercent)).append("\n");
    sb.append("    roomRevenue: ").append(toIndentedString(roomRevenue)).append("\n");
    sb.append("    adr: ").append(toIndentedString(adr)).append("\n");
    sb.append("    revpar: ").append(toIndentedString(revpar)).append("\n");
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

