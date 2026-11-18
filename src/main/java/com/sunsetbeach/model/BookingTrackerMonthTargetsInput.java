package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PUT /reports/booking-tracker/months/{month}&#x60;. Every field is optional and nullable - an omitted or &#x60;null&#x60; field is cleared - so none is in &#x60;required&#x60; (a nullable required property generates a &#x60;@NotNull&#x60; that rejects the null this endpoint accepts). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerMonthTargetsInput {

  private JsonNullable<@Min(0) @Max(100000) Integer> targetRoomNights = JsonNullable.<Integer>undefined();

  private JsonNullable<@Min(0) @Max(100000) Integer> baselineRoomNights = JsonNullable.<Integer>undefined();

  private JsonNullable<@Pattern(regexp = "^\\d{1,12}(\\.\\d{1,2})?$") String> baselineNetRevenue = JsonNullable.<String>undefined();

  public BookingTrackerMonthTargetsInput targetRoomNights(Integer targetRoomNights) {
    this.targetRoomNights = JsonNullable.of(targetRoomNights);
    return this;
  }

  /**
   * Get targetRoomNights
   * minimum: 0
   * maximum: 100000
   * @return targetRoomNights
   */
  @Min(0) @Max(100000) 
  @JsonProperty("targetRoomNights")
  public JsonNullable<@Min(0) @Max(100000) Integer> getTargetRoomNights() {
    return targetRoomNights;
  }

  public void setTargetRoomNights(JsonNullable<Integer> targetRoomNights) {
    this.targetRoomNights = targetRoomNights;
  }

  public BookingTrackerMonthTargetsInput baselineRoomNights(Integer baselineRoomNights) {
    this.baselineRoomNights = JsonNullable.of(baselineRoomNights);
    return this;
  }

  /**
   * Get baselineRoomNights
   * minimum: 0
   * maximum: 100000
   * @return baselineRoomNights
   */
  @Min(0) @Max(100000) 
  @JsonProperty("baselineRoomNights")
  public JsonNullable<@Min(0) @Max(100000) Integer> getBaselineRoomNights() {
    return baselineRoomNights;
  }

  public void setBaselineRoomNights(JsonNullable<Integer> baselineRoomNights) {
    this.baselineRoomNights = baselineRoomNights;
  }

  public BookingTrackerMonthTargetsInput baselineNetRevenue(String baselineNetRevenue) {
    this.baselineNetRevenue = JsonNullable.of(baselineNetRevenue);
    return this;
  }

  /**
   * Baht, a decimal string with at most two decimals.
   * @return baselineNetRevenue
   */
  @Pattern(regexp = "^\\d{1,12}(\\.\\d{1,2})?$") 
  @JsonProperty("baselineNetRevenue")
  public JsonNullable<@Pattern(regexp = "^\\d{1,12}(\\.\\d{1,2})?$") String> getBaselineNetRevenue() {
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
    BookingTrackerMonthTargetsInput bookingTrackerMonthTargetsInput = (BookingTrackerMonthTargetsInput) o;
    return equalsNullable(this.targetRoomNights, bookingTrackerMonthTargetsInput.targetRoomNights) &&
        equalsNullable(this.baselineRoomNights, bookingTrackerMonthTargetsInput.baselineRoomNights) &&
        equalsNullable(this.baselineNetRevenue, bookingTrackerMonthTargetsInput.baselineNetRevenue);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(targetRoomNights), hashCodeNullable(baselineRoomNights), hashCodeNullable(baselineNetRevenue));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerMonthTargetsInput {\n");
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

