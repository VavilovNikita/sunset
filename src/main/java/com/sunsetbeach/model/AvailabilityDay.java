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
 * Per-day inventory breakdown for staff. &#x60;availableCount&#x60; is the derived remainder (&#x60;quantity - blockedCount - bookedCount&#x60;) and can go negative - that&#39;s not clamped away, since a negative remainder (e.g. after a &#x60;quantity&#x60; reduction) is exactly the thing staff need to see, not hide. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-16T18:39:34.635637100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class AvailabilityDay {

  private String date;

  private Integer quantity;

  private Integer blockedCount;

  private Integer bookedCount;

  private Integer availableCount;

  public AvailabilityDay() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AvailabilityDay(String date, Integer quantity, Integer blockedCount, Integer bookedCount, Integer availableCount) {
    this.date = date;
    this.quantity = quantity;
    this.blockedCount = blockedCount;
    this.bookedCount = bookedCount;
    this.availableCount = availableCount;
  }

  public AvailabilityDay date(String date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("date")
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public AvailabilityDay quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Room.quantity at query time.
   * @return quantity
   */
  @NotNull 
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public AvailabilityDay blockedCount(Integer blockedCount) {
    this.blockedCount = blockedCount;
    return this;
  }

  /**
   * Units manually pulled from sale for this day (`Availability.blockedCount`).
   * @return blockedCount
   */
  @NotNull 
  @JsonProperty("blockedCount")
  public Integer getBlockedCount() {
    return blockedCount;
  }

  public void setBlockedCount(Integer blockedCount) {
    this.blockedCount = blockedCount;
  }

  public AvailabilityDay bookedCount(Integer bookedCount) {
    this.bookedCount = bookedCount;
    return this;
  }

  /**
   * Units covered by a non-CANCELLED booking for this day.
   * @return bookedCount
   */
  @NotNull 
  @JsonProperty("bookedCount")
  public Integer getBookedCount() {
    return bookedCount;
  }

  public void setBookedCount(Integer bookedCount) {
    this.bookedCount = bookedCount;
  }

  public AvailabilityDay availableCount(Integer availableCount) {
    this.availableCount = availableCount;
    return this;
  }

  /**
   * quantity - blockedCount - bookedCount. Not clamped at 0.
   * @return availableCount
   */
  @NotNull 
  @JsonProperty("availableCount")
  public Integer getAvailableCount() {
    return availableCount;
  }

  public void setAvailableCount(Integer availableCount) {
    this.availableCount = availableCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AvailabilityDay availabilityDay = (AvailabilityDay) o;
    return Objects.equals(this.date, availabilityDay.date) &&
        Objects.equals(this.quantity, availabilityDay.quantity) &&
        Objects.equals(this.blockedCount, availabilityDay.blockedCount) &&
        Objects.equals(this.bookedCount, availabilityDay.bookedCount) &&
        Objects.equals(this.availableCount, availabilityDay.availableCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, quantity, blockedCount, bookedCount, availableCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AvailabilityDay {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    blockedCount: ").append(toIndentedString(blockedCount)).append("\n");
    sb.append("    bookedCount: ").append(toIndentedString(bookedCount)).append("\n");
    sb.append("    availableCount: ").append(toIndentedString(availableCount)).append("\n");
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

