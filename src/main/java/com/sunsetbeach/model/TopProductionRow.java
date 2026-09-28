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
 * One producer&#39;s figures, or the total (where &#x60;producer&#x60;/&#x60;label&#x60; are null). Money is a decimal string with two decimals. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TopProductionRow {

  private JsonNullable<String> producer = JsonNullable.<String>undefined();

  private JsonNullable<String> label = JsonNullable.<String>undefined();

  private Integer roomNights;

  private JsonNullable<String> roomNightsPercent = JsonNullable.<String>undefined();

  private String revenue;

  private JsonNullable<String> revenuePercent = JsonNullable.<String>undefined();

  private JsonNullable<String> adr = JsonNullable.<String>undefined();

  public TopProductionRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TopProductionRow(String producer, String label, Integer roomNights, String roomNightsPercent, String revenue, String revenuePercent, String adr) {
    this.producer = JsonNullable.of(producer);
    this.label = JsonNullable.of(label);
    this.roomNights = roomNights;
    this.roomNightsPercent = JsonNullable.of(roomNightsPercent);
    this.revenue = revenue;
    this.revenuePercent = JsonNullable.of(revenuePercent);
    this.adr = JsonNullable.of(adr);
  }

  public TopProductionRow producer(String producer) {
    this.producer = JsonNullable.of(producer);
    return this;
  }

  /**
   * A `BookingChannel` value, or `COMPLIMENTARY` / `HOUSE_USE` for a booking with that `purpose` (whatever its channel). Null on the total row. 
   * @return producer
   */
  @NotNull 
  @JsonProperty("producer")
  public JsonNullable<String> getProducer() {
    return producer;
  }

  public void setProducer(JsonNullable<String> producer) {
    this.producer = producer;
  }

  public TopProductionRow label(String label) {
    this.label = JsonNullable.of(label);
    return this;
  }

  /**
   * Display name, e.g. \"Booking.com\", \"Complimentary\". Null on the total row.
   * @return label
   */
  @NotNull 
  @JsonProperty("label")
  public JsonNullable<String> getLabel() {
    return label;
  }

  public void setLabel(JsonNullable<String> label) {
    this.label = label;
  }

  public TopProductionRow roomNights(Integer roomNights) {
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

  public TopProductionRow roomNightsPercent(String roomNightsPercent) {
    this.roomNightsPercent = JsonNullable.of(roomNightsPercent);
    return this;
  }

  /**
   * Share of the total's room-nights × 100, two decimals. Null when the total is zero.
   * @return roomNightsPercent
   */
  @NotNull 
  @JsonProperty("roomNightsPercent")
  public JsonNullable<String> getRoomNightsPercent() {
    return roomNightsPercent;
  }

  public void setRoomNightsPercent(JsonNullable<String> roomNightsPercent) {
    this.roomNightsPercent = roomNightsPercent;
  }

  public TopProductionRow revenue(String revenue) {
    this.revenue = revenue;
    return this;
  }

  /**
   * Get revenue
   * @return revenue
   */
  @NotNull 
  @JsonProperty("revenue")
  public String getRevenue() {
    return revenue;
  }

  public void setRevenue(String revenue) {
    this.revenue = revenue;
  }

  public TopProductionRow revenuePercent(String revenuePercent) {
    this.revenuePercent = JsonNullable.of(revenuePercent);
    return this;
  }

  /**
   * Share of the total's revenue × 100, two decimals. Null when the total is zero.
   * @return revenuePercent
   */
  @NotNull 
  @JsonProperty("revenuePercent")
  public JsonNullable<String> getRevenuePercent() {
    return revenuePercent;
  }

  public void setRevenuePercent(JsonNullable<String> revenuePercent) {
    this.revenuePercent = revenuePercent;
  }

  public TopProductionRow adr(String adr) {
    this.adr = JsonNullable.of(adr);
    return this;
  }

  /**
   * Revenue / room-nights. Null when no room-night was sold.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TopProductionRow topProductionRow = (TopProductionRow) o;
    return Objects.equals(this.producer, topProductionRow.producer) &&
        Objects.equals(this.label, topProductionRow.label) &&
        Objects.equals(this.roomNights, topProductionRow.roomNights) &&
        Objects.equals(this.roomNightsPercent, topProductionRow.roomNightsPercent) &&
        Objects.equals(this.revenue, topProductionRow.revenue) &&
        Objects.equals(this.revenuePercent, topProductionRow.revenuePercent) &&
        Objects.equals(this.adr, topProductionRow.adr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(producer, label, roomNights, roomNightsPercent, revenue, revenuePercent, adr);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TopProductionRow {\n");
    sb.append("    producer: ").append(toIndentedString(producer)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    roomNights: ").append(toIndentedString(roomNights)).append("\n");
    sb.append("    roomNightsPercent: ").append(toIndentedString(roomNightsPercent)).append("\n");
    sb.append("    revenue: ").append(toIndentedString(revenue)).append("\n");
    sb.append("    revenuePercent: ").append(toIndentedString(revenuePercent)).append("\n");
    sb.append("    adr: ").append(toIndentedString(adr)).append("\n");
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

