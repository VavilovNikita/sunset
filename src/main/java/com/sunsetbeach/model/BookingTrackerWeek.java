package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * BookingTrackerWeek
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerWeek {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate weekStart;

  private Integer roomNights;

  private String netRoomRevenue;

  public BookingTrackerWeek() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerWeek(LocalDate weekStart, Integer roomNights, String netRoomRevenue) {
    this.weekStart = weekStart;
    this.roomNights = roomNights;
    this.netRoomRevenue = netRoomRevenue;
  }

  public BookingTrackerWeek weekStart(LocalDate weekStart) {
    this.weekStart = weekStart;
    return this;
  }

  /**
   * The Monday of the week.
   * @return weekStart
   */
  @NotNull @Valid 
  @JsonProperty("weekStart")
  public LocalDate getWeekStart() {
    return weekStart;
  }

  public void setWeekStart(LocalDate weekStart) {
    this.weekStart = weekStart;
  }

  public BookingTrackerWeek roomNights(Integer roomNights) {
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

  public BookingTrackerWeek netRoomRevenue(String netRoomRevenue) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerWeek bookingTrackerWeek = (BookingTrackerWeek) o;
    return Objects.equals(this.weekStart, bookingTrackerWeek.weekStart) &&
        Objects.equals(this.roomNights, bookingTrackerWeek.roomNights) &&
        Objects.equals(this.netRoomRevenue, bookingTrackerWeek.netRoomRevenue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(weekStart, roomNights, netRoomRevenue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerWeek {\n");
    sb.append("    weekStart: ").append(toIndentedString(weekStart)).append("\n");
    sb.append("    roomNights: ").append(toIndentedString(roomNights)).append("\n");
    sb.append("    netRoomRevenue: ").append(toIndentedString(netRoomRevenue)).append("\n");
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

