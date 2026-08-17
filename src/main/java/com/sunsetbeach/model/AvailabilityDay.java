package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RoomUnitAvailability;
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
 * Per-day inventory breakdown for staff, at both the room-type level (&#x60;unitCount&#x60;, &#x60;blockedCount&#x60;, &#x60;bookedCount&#x60;, &#x60;availableCount&#x60;) and the physical-room level (&#x60;units[]&#x60;). &#x60;availableCount&#x60; is the derived remainder (&#x60;unitCount - blockedCount - bookedCount&#x60;) and can go negative - that&#39;s not clamped away, since a negative remainder (e.g. after room units are deactivated) is exactly the thing staff need to see, not hide. &#x60;blockedCount&#x60; counts *distinct* blocked active units for the day (an overlapping second block on the same unit doesn&#39;t double-count); &#x60;bookedCount&#x60; counts bookings covering the day regardless of whether they have an assigned unit - an unassigned booking still occupies one unit of the type. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T15:17:55.380996100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class AvailabilityDay {

  private String date;

  private Integer unitCount;

  private Integer blockedCount;

  private Integer bookedCount;

  private Integer availableCount;

  @Valid
  private List<@Valid RoomUnitAvailability> units = new ArrayList<>();

  public AvailabilityDay() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AvailabilityDay(String date, Integer unitCount, Integer blockedCount, Integer bookedCount, Integer availableCount, List<@Valid RoomUnitAvailability> units) {
    this.date = date;
    this.unitCount = unitCount;
    this.blockedCount = blockedCount;
    this.bookedCount = bookedCount;
    this.availableCount = availableCount;
    this.units = units;
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

  public AvailabilityDay unitCount(Integer unitCount) {
    this.unitCount = unitCount;
    return this;
  }

  /**
   * Number of active `RoomUnit`s of this type at query time.
   * @return unitCount
   */
  @NotNull 
  @JsonProperty("unitCount")
  public Integer getUnitCount() {
    return unitCount;
  }

  public void setUnitCount(Integer unitCount) {
    this.unitCount = unitCount;
  }

  public AvailabilityDay blockedCount(Integer blockedCount) {
    this.blockedCount = blockedCount;
    return this;
  }

  /**
   * Distinct active units with a `RoomUnitBlock` covering this day.
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
   * Units covered by a non-CANCELLED booking for this day (assigned or not).
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
   * unitCount - blockedCount - bookedCount. Not clamped at 0.
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

  public AvailabilityDay units(List<@Valid RoomUnitAvailability> units) {
    this.units = units;
    return this;
  }

  public AvailabilityDay addUnitsItem(RoomUnitAvailability unitsItem) {
    if (this.units == null) {
      this.units = new ArrayList<>();
    }
    this.units.add(unitsItem);
    return this;
  }

  /**
   * One entry per active `RoomUnit` of this type.
   * @return units
   */
  @NotNull @Valid 
  @JsonProperty("units")
  public List<@Valid RoomUnitAvailability> getUnits() {
    return units;
  }

  public void setUnits(List<@Valid RoomUnitAvailability> units) {
    this.units = units;
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
        Objects.equals(this.unitCount, availabilityDay.unitCount) &&
        Objects.equals(this.blockedCount, availabilityDay.blockedCount) &&
        Objects.equals(this.bookedCount, availabilityDay.bookedCount) &&
        Objects.equals(this.availableCount, availabilityDay.availableCount) &&
        Objects.equals(this.units, availabilityDay.units);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, unitCount, blockedCount, bookedCount, availableCount, units);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AvailabilityDay {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    unitCount: ").append(toIndentedString(unitCount)).append("\n");
    sb.append("    blockedCount: ").append(toIndentedString(blockedCount)).append("\n");
    sb.append("    bookedCount: ").append(toIndentedString(bookedCount)).append("\n");
    sb.append("    availableCount: ").append(toIndentedString(availableCount)).append("\n");
    sb.append("    units: ").append(toIndentedString(units)).append("\n");
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

