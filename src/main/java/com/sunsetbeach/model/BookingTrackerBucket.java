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
 * BookingTrackerBucket
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerBucket {

  private String label;

  private Integer fromDays;

  private JsonNullable<Integer> toDays = JsonNullable.<Integer>undefined();

  private Integer roomNights;

  private String netRoomRevenue;

  private JsonNullable<String> roomNightSharePercent = JsonNullable.<String>undefined();

  public BookingTrackerBucket() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerBucket(String label, Integer fromDays, Integer toDays, Integer roomNights, String netRoomRevenue, String roomNightSharePercent) {
    this.label = label;
    this.fromDays = fromDays;
    this.toDays = JsonNullable.of(toDays);
    this.roomNights = roomNights;
    this.netRoomRevenue = netRoomRevenue;
    this.roomNightSharePercent = JsonNullable.of(roomNightSharePercent);
  }

  public BookingTrackerBucket label(String label) {
    this.label = label;
    return this;
  }

  /**
   * e.g. `0-3`, `91+`.
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

  public BookingTrackerBucket fromDays(Integer fromDays) {
    this.fromDays = fromDays;
    return this;
  }

  /**
   * Get fromDays
   * @return fromDays
   */
  @NotNull 
  @JsonProperty("fromDays")
  public Integer getFromDays() {
    return fromDays;
  }

  public void setFromDays(Integer fromDays) {
    this.fromDays = fromDays;
  }

  public BookingTrackerBucket toDays(Integer toDays) {
    this.toDays = JsonNullable.of(toDays);
    return this;
  }

  /**
   * Null for the open-ended last bucket.
   * @return toDays
   */
  @NotNull 
  @JsonProperty("toDays")
  public JsonNullable<Integer> getToDays() {
    return toDays;
  }

  public void setToDays(JsonNullable<Integer> toDays) {
    this.toDays = toDays;
  }

  public BookingTrackerBucket roomNights(Integer roomNights) {
    this.roomNights = roomNights;
    return this;
  }

  /**
   * Get roomNights
   * @return roomNights
   */
  @NotNull 
  @JsonProperty("roomNights")
  public Integer getRoomNights() {
    return roomNights;
  }

  public void setRoomNights(Integer roomNights) {
    this.roomNights = roomNights;
  }

  public BookingTrackerBucket netRoomRevenue(String netRoomRevenue) {
    this.netRoomRevenue = netRoomRevenue;
    return this;
  }

  /**
   * Get netRoomRevenue
   * @return netRoomRevenue
   */
  @NotNull 
  @JsonProperty("netRoomRevenue")
  public String getNetRoomRevenue() {
    return netRoomRevenue;
  }

  public void setNetRoomRevenue(String netRoomRevenue) {
    this.netRoomRevenue = netRoomRevenue;
  }

  public BookingTrackerBucket roomNightSharePercent(String roomNightSharePercent) {
    this.roomNightSharePercent = JsonNullable.of(roomNightSharePercent);
    return this;
  }

  /**
   * Get roomNightSharePercent
   * @return roomNightSharePercent
   */
  @NotNull 
  @JsonProperty("roomNightSharePercent")
  public JsonNullable<String> getRoomNightSharePercent() {
    return roomNightSharePercent;
  }

  public void setRoomNightSharePercent(JsonNullable<String> roomNightSharePercent) {
    this.roomNightSharePercent = roomNightSharePercent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerBucket bookingTrackerBucket = (BookingTrackerBucket) o;
    return Objects.equals(this.label, bookingTrackerBucket.label) &&
        Objects.equals(this.fromDays, bookingTrackerBucket.fromDays) &&
        Objects.equals(this.toDays, bookingTrackerBucket.toDays) &&
        Objects.equals(this.roomNights, bookingTrackerBucket.roomNights) &&
        Objects.equals(this.netRoomRevenue, bookingTrackerBucket.netRoomRevenue) &&
        Objects.equals(this.roomNightSharePercent, bookingTrackerBucket.roomNightSharePercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, fromDays, toDays, roomNights, netRoomRevenue, roomNightSharePercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerBucket {\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    fromDays: ").append(toIndentedString(fromDays)).append("\n");
    sb.append("    toDays: ").append(toIndentedString(toDays)).append("\n");
    sb.append("    roomNights: ").append(toIndentedString(roomNights)).append("\n");
    sb.append("    netRoomRevenue: ").append(toIndentedString(netRoomRevenue)).append("\n");
    sb.append("    roomNightSharePercent: ").append(toIndentedString(roomNightSharePercent)).append("\n");
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

