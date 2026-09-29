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
 * ManagerAccountCount
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ManagerAccountCount {

  private Integer arrivals;

  private Integer departures;

  private Integer cancellations;

  private Integer noShows;

  private Integer walkInRooms;

  public ManagerAccountCount() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ManagerAccountCount(Integer arrivals, Integer departures, Integer cancellations, Integer noShows, Integer walkInRooms) {
    this.arrivals = arrivals;
    this.departures = departures;
    this.cancellations = cancellations;
    this.noShows = noShows;
    this.walkInRooms = walkInRooms;
  }

  public ManagerAccountCount arrivals(Integer arrivals) {
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

  public ManagerAccountCount departures(Integer departures) {
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

  public ManagerAccountCount cancellations(Integer cancellations) {
    this.cancellations = cancellations;
    return this;
  }

  /**
   * Approximate - see the operation's note on `updatedAt`.
   * @return cancellations
   */
  @NotNull 
  @JsonProperty("cancellations")
  public Integer getCancellations() {
    return cancellations;
  }

  public void setCancellations(Integer cancellations) {
    this.cancellations = cancellations;
  }

  public ManagerAccountCount noShows(Integer noShows) {
    this.noShows = noShows;
    return this;
  }

  /**
   * Approximate - see the operation's note on `updatedAt`.
   * @return noShows
   */
  @NotNull 
  @JsonProperty("noShows")
  public Integer getNoShows() {
    return noShows;
  }

  public void setNoShows(Integer noShows) {
    this.noShows = noShows;
  }

  public ManagerAccountCount walkInRooms(Integer walkInRooms) {
    this.walkInRooms = walkInRooms;
    return this;
  }

  /**
   * Get walkInRooms
   * @return walkInRooms
   */
  @NotNull 
  @JsonProperty("walkInRooms")
  public Integer getWalkInRooms() {
    return walkInRooms;
  }

  public void setWalkInRooms(Integer walkInRooms) {
    this.walkInRooms = walkInRooms;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ManagerAccountCount managerAccountCount = (ManagerAccountCount) o;
    return Objects.equals(this.arrivals, managerAccountCount.arrivals) &&
        Objects.equals(this.departures, managerAccountCount.departures) &&
        Objects.equals(this.cancellations, managerAccountCount.cancellations) &&
        Objects.equals(this.noShows, managerAccountCount.noShows) &&
        Objects.equals(this.walkInRooms, managerAccountCount.walkInRooms);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arrivals, departures, cancellations, noShows, walkInRooms);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ManagerAccountCount {\n");
    sb.append("    arrivals: ").append(toIndentedString(arrivals)).append("\n");
    sb.append("    departures: ").append(toIndentedString(departures)).append("\n");
    sb.append("    cancellations: ").append(toIndentedString(cancellations)).append("\n");
    sb.append("    noShows: ").append(toIndentedString(noShows)).append("\n");
    sb.append("    walkInRooms: ").append(toIndentedString(walkInRooms)).append("\n");
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

