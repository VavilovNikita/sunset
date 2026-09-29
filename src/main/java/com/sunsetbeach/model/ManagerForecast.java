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
 * The next night&#39;s figures.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ManagerForecast {

  private String date;

  private Integer arrivals;

  private Integer departures;

  private Integer occupied;

  private Integer availableForSale;

  private JsonNullable<String> occupancyPercent = JsonNullable.<String>undefined();

  public ManagerForecast() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ManagerForecast(String date, Integer arrivals, Integer departures, Integer occupied, Integer availableForSale, String occupancyPercent) {
    this.date = date;
    this.arrivals = arrivals;
    this.departures = departures;
    this.occupied = occupied;
    this.availableForSale = availableForSale;
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
  }

  public ManagerForecast date(String date) {
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

  public ManagerForecast arrivals(Integer arrivals) {
    this.arrivals = arrivals;
    return this;
  }

  /**
   * Get arrivals
   * @return arrivals
   */
  @NotNull 
  @JsonProperty("arrivals")
  public Integer getArrivals() {
    return arrivals;
  }

  public void setArrivals(Integer arrivals) {
    this.arrivals = arrivals;
  }

  public ManagerForecast departures(Integer departures) {
    this.departures = departures;
    return this;
  }

  /**
   * Get departures
   * @return departures
   */
  @NotNull 
  @JsonProperty("departures")
  public Integer getDepartures() {
    return departures;
  }

  public void setDepartures(Integer departures) {
    this.departures = departures;
  }

  public ManagerForecast occupied(Integer occupied) {
    this.occupied = occupied;
    return this;
  }

  /**
   * Get occupied
   * @return occupied
   */
  @NotNull 
  @JsonProperty("occupied")
  public Integer getOccupied() {
    return occupied;
  }

  public void setOccupied(Integer occupied) {
    this.occupied = occupied;
  }

  public ManagerForecast availableForSale(Integer availableForSale) {
    this.availableForSale = availableForSale;
    return this;
  }

  /**
   * Get availableForSale
   * @return availableForSale
   */
  @NotNull 
  @JsonProperty("availableForSale")
  public Integer getAvailableForSale() {
    return availableForSale;
  }

  public void setAvailableForSale(Integer availableForSale) {
    this.availableForSale = availableForSale;
  }

  public ManagerForecast occupancyPercent(String occupancyPercent) {
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    return this;
  }

  /**
   * occupied / availableForSale × 100.
   * @return occupancyPercent
   */
  @NotNull 
  @JsonProperty("occupancyPercent")
  public JsonNullable<String> getOccupancyPercent() {
    return occupancyPercent;
  }

  public void setOccupancyPercent(JsonNullable<String> occupancyPercent) {
    this.occupancyPercent = occupancyPercent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ManagerForecast managerForecast = (ManagerForecast) o;
    return Objects.equals(this.date, managerForecast.date) &&
        Objects.equals(this.arrivals, managerForecast.arrivals) &&
        Objects.equals(this.departures, managerForecast.departures) &&
        Objects.equals(this.occupied, managerForecast.occupied) &&
        Objects.equals(this.availableForSale, managerForecast.availableForSale) &&
        Objects.equals(this.occupancyPercent, managerForecast.occupancyPercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, arrivals, departures, occupied, availableForSale, occupancyPercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ManagerForecast {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    arrivals: ").append(toIndentedString(arrivals)).append("\n");
    sb.append("    departures: ").append(toIndentedString(departures)).append("\n");
    sb.append("    occupied: ").append(toIndentedString(occupied)).append("\n");
    sb.append("    availableForSale: ").append(toIndentedString(availableForSale)).append("\n");
    sb.append("    occupancyPercent: ").append(toIndentedString(occupancyPercent)).append("\n");
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

