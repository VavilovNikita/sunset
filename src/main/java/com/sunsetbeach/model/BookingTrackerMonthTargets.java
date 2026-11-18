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
 * BookingTrackerMonthTargets
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerMonthTargets {

  private String month;

  private JsonNullable<Integer> targetRoomNights = JsonNullable.<Integer>undefined();

  private JsonNullable<Integer> baselineRoomNights = JsonNullable.<Integer>undefined();

  private JsonNullable<String> baselineNetRevenue = JsonNullable.<String>undefined();

  public BookingTrackerMonthTargets() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerMonthTargets(String month, Integer targetRoomNights, Integer baselineRoomNights, String baselineNetRevenue) {
    this.month = month;
    this.targetRoomNights = JsonNullable.of(targetRoomNights);
    this.baselineRoomNights = JsonNullable.of(baselineRoomNights);
    this.baselineNetRevenue = JsonNullable.of(baselineNetRevenue);
  }

  public BookingTrackerMonthTargets month(String month) {
    this.month = month;
    return this;
  }

  /**
   * Get month
   * @return month
   */
  @NotNull 
  @JsonProperty("month")
  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  public BookingTrackerMonthTargets targetRoomNights(Integer targetRoomNights) {
    this.targetRoomNights = JsonNullable.of(targetRoomNights);
    return this;
  }

  /**
   * Get targetRoomNights
   * @return targetRoomNights
   */
  @NotNull 
  @JsonProperty("targetRoomNights")
  public JsonNullable<Integer> getTargetRoomNights() {
    return targetRoomNights;
  }

  public void setTargetRoomNights(JsonNullable<Integer> targetRoomNights) {
    this.targetRoomNights = targetRoomNights;
  }

  public BookingTrackerMonthTargets baselineRoomNights(Integer baselineRoomNights) {
    this.baselineRoomNights = JsonNullable.of(baselineRoomNights);
    return this;
  }

  /**
   * Get baselineRoomNights
   * @return baselineRoomNights
   */
  @NotNull 
  @JsonProperty("baselineRoomNights")
  public JsonNullable<Integer> getBaselineRoomNights() {
    return baselineRoomNights;
  }

  public void setBaselineRoomNights(JsonNullable<Integer> baselineRoomNights) {
    this.baselineRoomNights = baselineRoomNights;
  }

  public BookingTrackerMonthTargets baselineNetRevenue(String baselineNetRevenue) {
    this.baselineNetRevenue = JsonNullable.of(baselineNetRevenue);
    return this;
  }

  /**
   * Get baselineNetRevenue
   * @return baselineNetRevenue
   */
  @NotNull 
  @JsonProperty("baselineNetRevenue")
  public JsonNullable<String> getBaselineNetRevenue() {
    return baselineNetRevenue;
  }

  public void setBaselineNetRevenue(JsonNullable<String> baselineNetRevenue) {
    this.baselineNetRevenue = baselineNetRevenue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerMonthTargets bookingTrackerMonthTargets = (BookingTrackerMonthTargets) o;
    return Objects.equals(this.month, bookingTrackerMonthTargets.month) &&
        Objects.equals(this.targetRoomNights, bookingTrackerMonthTargets.targetRoomNights) &&
        Objects.equals(this.baselineRoomNights, bookingTrackerMonthTargets.baselineRoomNights) &&
        Objects.equals(this.baselineNetRevenue, bookingTrackerMonthTargets.baselineNetRevenue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(month, targetRoomNights, baselineRoomNights, baselineNetRevenue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerMonthTargets {\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    targetRoomNights: ").append(toIndentedString(targetRoomNights)).append("\n");
    sb.append("    baselineRoomNights: ").append(toIndentedString(baselineRoomNights)).append("\n");
    sb.append("    baselineNetRevenue: ").append(toIndentedString(baselineNetRevenue)).append("\n");
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

