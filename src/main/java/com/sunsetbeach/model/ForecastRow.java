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
 * One room type&#39;s figures for a date, or the property total (where &#x60;roomId&#x60;/&#x60;roomName&#x60; are null). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ForecastRow {

  private JsonNullable<String> roomId = JsonNullable.<String>undefined();

  private JsonNullable<String> roomName = JsonNullable.<String>undefined();

  private Integer arrivals;

  private Integer departures;

  private Integer totalRooms;

  private Integer outOfOrder;

  private Integer occupied;

  private Integer vacant;

  private JsonNullable<String> occupancyPercent = JsonNullable.<String>undefined();

  public ForecastRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ForecastRow(String roomId, String roomName, Integer arrivals, Integer departures, Integer totalRooms, Integer outOfOrder, Integer occupied, Integer vacant, String occupancyPercent) {
    this.roomId = JsonNullable.of(roomId);
    this.roomName = JsonNullable.of(roomName);
    this.arrivals = arrivals;
    this.departures = departures;
    this.totalRooms = totalRooms;
    this.outOfOrder = outOfOrder;
    this.occupied = occupied;
    this.vacant = vacant;
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
  }

  public ForecastRow roomId(String roomId) {
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

  public ForecastRow roomName(String roomName) {
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

  public ForecastRow arrivals(Integer arrivals) {
    this.arrivals = arrivals;
    return this;
  }

  /**
   * Get arrivals
   * @return arrivals
   */
  @NotNull 
  @JsonProperty("arrivals")
  public Integer getArrivals() {
    return arrivals;
  }

  public void setArrivals(Integer arrivals) {
    this.arrivals = arrivals;
  }

  public ForecastRow departures(Integer departures) {
    this.departures = departures;
    return this;
  }

  /**
   * Get departures
   * @return departures
   */
  @NotNull 
  @JsonProperty("departures")
  public Integer getDepartures() {
    return departures;
  }

  public void setDepartures(Integer departures) {
    this.departures = departures;
  }

  public ForecastRow totalRooms(Integer totalRooms) {
    this.totalRooms = totalRooms;
    return this;
  }

  /**
   * Physical units active today - see the operation's note on why this is today's count.
   * @return totalRooms
   */
  @NotNull 
  @JsonProperty("totalRooms")
  public Integer getTotalRooms() {
    return totalRooms;
  }

  public void setTotalRooms(Integer totalRooms) {
    this.totalRooms = totalRooms;
  }

  public ForecastRow outOfOrder(Integer outOfOrder) {
    this.outOfOrder = outOfOrder;
    return this;
  }

  /**
   * Get outOfOrder
   * @return outOfOrder
   */
  @NotNull 
  @JsonProperty("outOfOrder")
  public Integer getOutOfOrder() {
    return outOfOrder;
  }

  public void setOutOfOrder(Integer outOfOrder) {
    this.outOfOrder = outOfOrder;
  }

  public ForecastRow occupied(Integer occupied) {
    this.occupied = occupied;
    return this;
  }

  /**
   * Get occupied
   * @return occupied
   */
  @NotNull 
  @JsonProperty("occupied")
  public Integer getOccupied() {
    return occupied;
  }

  public void setOccupied(Integer occupied) {
    this.occupied = occupied;
  }

  public ForecastRow vacant(Integer vacant) {
    this.vacant = vacant;
    return this;
  }

  /**
   * totalRooms - occupied - outOfOrder. Can be negative (overbooked).
   * @return vacant
   */
  @NotNull 
  @JsonProperty("vacant")
  public Integer getVacant() {
    return vacant;
  }

  public void setVacant(Integer vacant) {
    this.vacant = vacant;
  }

  public ForecastRow occupancyPercent(String occupancyPercent) {
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    return this;
  }

  /**
   * occupied / (totalRooms - outOfOrder) × 100. Null when nothing is sellable.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForecastRow forecastRow = (ForecastRow) o;
    return Objects.equals(this.roomId, forecastRow.roomId) &&
        Objects.equals(this.roomName, forecastRow.roomName) &&
        Objects.equals(this.arrivals, forecastRow.arrivals) &&
        Objects.equals(this.departures, forecastRow.departures) &&
        Objects.equals(this.totalRooms, forecastRow.totalRooms) &&
        Objects.equals(this.outOfOrder, forecastRow.outOfOrder) &&
        Objects.equals(this.occupied, forecastRow.occupied) &&
        Objects.equals(this.vacant, forecastRow.vacant) &&
        Objects.equals(this.occupancyPercent, forecastRow.occupancyPercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, roomName, arrivals, departures, totalRooms, outOfOrder, occupied, vacant, occupancyPercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForecastRow {\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    arrivals: ").append(toIndentedString(arrivals)).append("\n");
    sb.append("    departures: ").append(toIndentedString(departures)).append("\n");
    sb.append("    totalRooms: ").append(toIndentedString(totalRooms)).append("\n");
    sb.append("    outOfOrder: ").append(toIndentedString(outOfOrder)).append("\n");
    sb.append("    occupied: ").append(toIndentedString(occupied)).append("\n");
    sb.append("    vacant: ").append(toIndentedString(vacant)).append("\n");
    sb.append("    occupancyPercent: ").append(toIndentedString(occupancyPercent)).append("\n");
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

