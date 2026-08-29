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
 * One day&#39;s remaining-unit count for a room type, from the same server-side formula as &#x60;AvailabilityDay.availableCount&#x60; (&#x60;activeUnitCount - blockedUnits - bookedUnits&#x60; for that day) - deliberately **not** clamped at zero. A negative value is a real signal (e.g. more bookings/blocks than currently-active units, after a unit was deactivated), not noise - clients must render it distinctly, not hide or floor it. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T21:43:17.277610500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class RoomTypeDailyAvailability {

  private String date;

  private Integer availableCount;

  public RoomTypeDailyAvailability() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomTypeDailyAvailability(String date, Integer availableCount) {
    this.date = date;
    this.availableCount = availableCount;
  }

  public RoomTypeDailyAvailability date(String date) {
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

  public RoomTypeDailyAvailability availableCount(Integer availableCount) {
    this.availableCount = availableCount;
    return this;
  }

  /**
   * Get availableCount
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
    RoomTypeDailyAvailability roomTypeDailyAvailability = (RoomTypeDailyAvailability) o;
    return Objects.equals(this.date, roomTypeDailyAvailability.date) &&
        Objects.equals(this.availableCount, roomTypeDailyAvailability.availableCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, availableCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomTypeDailyAvailability {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
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

