package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.MarketSegment;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One segment&#39;s figures, or the total (where &#x60;segment&#x60; is null). Money is a decimal string with two decimals.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class MarketSegmentRow {

  private MarketSegment segment;

  private Integer roomNights;

  private JsonNullable<String> roomNightsPercent = JsonNullable.<String>undefined();

  private Integer guests;

  private JsonNullable<String> guestsPercent = JsonNullable.<String>undefined();

  private String revenue;

  private JsonNullable<String> revenuePercent = JsonNullable.<String>undefined();

  private JsonNullable<String> averageRate = JsonNullable.<String>undefined();

  public MarketSegmentRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MarketSegmentRow(MarketSegment segment, Integer roomNights, String roomNightsPercent, Integer guests, String guestsPercent, String revenue, String revenuePercent, String averageRate) {
    this.segment = segment;
    this.roomNights = roomNights;
    this.roomNightsPercent = JsonNullable.of(roomNightsPercent);
    this.guests = guests;
    this.guestsPercent = JsonNullable.of(guestsPercent);
    this.revenue = revenue;
    this.revenuePercent = JsonNullable.of(revenuePercent);
    this.averageRate = JsonNullable.of(averageRate);
  }

  public MarketSegmentRow segment(MarketSegment segment) {
    this.segment = segment;
    return this;
  }

  /**
   * Get segment
   * @return segment
   */
  @NotNull @Valid 
  @JsonProperty("segment")
  public MarketSegment getSegment() {
    return segment;
  }

  public void setSegment(MarketSegment segment) {
    this.segment = segment;
  }

  public MarketSegmentRow roomNights(Integer roomNights) {
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

  public MarketSegmentRow roomNightsPercent(String roomNightsPercent) {
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

  public MarketSegmentRow guests(Integer guests) {
    this.guests = guests;
    return this;
  }

  /**
   * `adults + children` over the distinct bookings with a night in the range.
   * @return guests
   */
  @NotNull 
  @JsonProperty("guests")
  public Integer getGuests() {
    return guests;
  }

  public void setGuests(Integer guests) {
    this.guests = guests;
  }

  public MarketSegmentRow guestsPercent(String guestsPercent) {
    this.guestsPercent = JsonNullable.of(guestsPercent);
    return this;
  }

  /**
   * Share of the total's guests × 100, two decimals. Null when the total is zero.
   * @return guestsPercent
   */
  @NotNull 
  @JsonProperty("guestsPercent")
  public JsonNullable<String> getGuestsPercent() {
    return guestsPercent;
  }

  public void setGuestsPercent(JsonNullable<String> guestsPercent) {
    this.guestsPercent = guestsPercent;
  }

  public MarketSegmentRow revenue(String revenue) {
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

  public MarketSegmentRow revenuePercent(String revenuePercent) {
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

  public MarketSegmentRow averageRate(String averageRate) {
    this.averageRate = JsonNullable.of(averageRate);
    return this;
  }

  /**
   * Revenue / room-nights. Null when no room-night was sold.
   * @return averageRate
   */
  @NotNull 
  @JsonProperty("averageRate")
  public JsonNullable<String> getAverageRate() {
    return averageRate;
  }

  public void setAverageRate(JsonNullable<String> averageRate) {
    this.averageRate = averageRate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarketSegmentRow marketSegmentRow = (MarketSegmentRow) o;
    return Objects.equals(this.segment, marketSegmentRow.segment) &&
        Objects.equals(this.roomNights, marketSegmentRow.roomNights) &&
        Objects.equals(this.roomNightsPercent, marketSegmentRow.roomNightsPercent) &&
        Objects.equals(this.guests, marketSegmentRow.guests) &&
        Objects.equals(this.guestsPercent, marketSegmentRow.guestsPercent) &&
        Objects.equals(this.revenue, marketSegmentRow.revenue) &&
        Objects.equals(this.revenuePercent, marketSegmentRow.revenuePercent) &&
        Objects.equals(this.averageRate, marketSegmentRow.averageRate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(segment, roomNights, roomNightsPercent, guests, guestsPercent, revenue, revenuePercent, averageRate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarketSegmentRow {\n");
    sb.append("    segment: ").append(toIndentedString(segment)).append("\n");
    sb.append("    roomNights: ").append(toIndentedString(roomNights)).append("\n");
    sb.append("    roomNightsPercent: ").append(toIndentedString(roomNightsPercent)).append("\n");
    sb.append("    guests: ").append(toIndentedString(guests)).append("\n");
    sb.append("    guestsPercent: ").append(toIndentedString(guestsPercent)).append("\n");
    sb.append("    revenue: ").append(toIndentedString(revenue)).append("\n");
    sb.append("    revenuePercent: ").append(toIndentedString(revenuePercent)).append("\n");
    sb.append("    averageRate: ").append(toIndentedString(averageRate)).append("\n");
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

