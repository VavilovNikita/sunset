package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.BookingTrackerBucket;
import com.sunsetbeach.model.BookingTrackerWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * BookingTrackerTrends
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerTrends {

  private JsonNullable<String> averageBookingWindowDays = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid BookingTrackerBucket> buckets = new ArrayList<>();

  @Valid
  private List<@Valid BookingTrackerWeek> weeks = new ArrayList<>();

  public BookingTrackerTrends() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerTrends(String averageBookingWindowDays, List<@Valid BookingTrackerBucket> buckets, List<@Valid BookingTrackerWeek> weeks) {
    this.averageBookingWindowDays = JsonNullable.of(averageBookingWindowDays);
    this.buckets = buckets;
    this.weeks = weeks;
  }

  public BookingTrackerTrends averageBookingWindowDays(String averageBookingWindowDays) {
    this.averageBookingWindowDays = JsonNullable.of(averageBookingWindowDays);
    return this;
  }

  /**
   * Two decimals, weighted by room nights; null without room nights.
   * @return averageBookingWindowDays
   */
  @NotNull 
  @JsonProperty("averageBookingWindowDays")
  public JsonNullable<String> getAverageBookingWindowDays() {
    return averageBookingWindowDays;
  }

  public void setAverageBookingWindowDays(JsonNullable<String> averageBookingWindowDays) {
    this.averageBookingWindowDays = averageBookingWindowDays;
  }

  public BookingTrackerTrends buckets(List<@Valid BookingTrackerBucket> buckets) {
    this.buckets = buckets;
    return this;
  }

  public BookingTrackerTrends addBucketsItem(BookingTrackerBucket bucketsItem) {
    if (this.buckets == null) {
      this.buckets = new ArrayList<>();
    }
    this.buckets.add(bucketsItem);
    return this;
  }

  /**
   * Get buckets
   * @return buckets
   */
  @NotNull @Valid 
  @JsonProperty("buckets")
  public List<@Valid BookingTrackerBucket> getBuckets() {
    return buckets;
  }

  public void setBuckets(List<@Valid BookingTrackerBucket> buckets) {
    this.buckets = buckets;
  }

  public BookingTrackerTrends weeks(List<@Valid BookingTrackerWeek> weeks) {
    this.weeks = weeks;
    return this;
  }

  public BookingTrackerTrends addWeeksItem(BookingTrackerWeek weeksItem) {
    if (this.weeks == null) {
      this.weeks = new ArrayList<>();
    }
    this.weeks.add(weeksItem);
    return this;
  }

  /**
   * The last 12 weeks, oldest first.
   * @return weeks
   */
  @NotNull @Valid 
  @JsonProperty("weeks")
  public List<@Valid BookingTrackerWeek> getWeeks() {
    return weeks;
  }

  public void setWeeks(List<@Valid BookingTrackerWeek> weeks) {
    this.weeks = weeks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerTrends bookingTrackerTrends = (BookingTrackerTrends) o;
    return Objects.equals(this.averageBookingWindowDays, bookingTrackerTrends.averageBookingWindowDays) &&
        Objects.equals(this.buckets, bookingTrackerTrends.buckets) &&
        Objects.equals(this.weeks, bookingTrackerTrends.weeks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(averageBookingWindowDays, buckets, weeks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerTrends {\n");
    sb.append("    averageBookingWindowDays: ").append(toIndentedString(averageBookingWindowDays)).append("\n");
    sb.append("    buckets: ").append(toIndentedString(buckets)).append("\n");
    sb.append("    weeks: ").append(toIndentedString(weeks)).append("\n");
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

