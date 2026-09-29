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
 * Room revenue only - see the operation for what is left out.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ManagerRevenue {

  private String roomRevenue;

  private JsonNullable<String> averageRevenuePerInHouseGuest = JsonNullable.<String>undefined();

  public ManagerRevenue() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ManagerRevenue(String roomRevenue, String averageRevenuePerInHouseGuest) {
    this.roomRevenue = roomRevenue;
    this.averageRevenuePerInHouseGuest = JsonNullable.of(averageRevenuePerInHouseGuest);
  }

  public ManagerRevenue roomRevenue(String roomRevenue) {
    this.roomRevenue = roomRevenue;
    return this;
  }

  /**
   * Get roomRevenue
   * @return roomRevenue
   */
  @NotNull 
  @JsonProperty("roomRevenue")
  public String getRoomRevenue() {
    return roomRevenue;
  }

  public void setRoomRevenue(String roomRevenue) {
    this.roomRevenue = roomRevenue;
  }

  public ManagerRevenue averageRevenuePerInHouseGuest(String averageRevenuePerInHouseGuest) {
    this.averageRevenuePerInHouseGuest = JsonNullable.of(averageRevenuePerInHouseGuest);
    return this;
  }

  /**
   * roomRevenue / in-house guests.
   * @return averageRevenuePerInHouseGuest
   */
  @NotNull 
  @JsonProperty("averageRevenuePerInHouseGuest")
  public JsonNullable<String> getAverageRevenuePerInHouseGuest() {
    return averageRevenuePerInHouseGuest;
  }

  public void setAverageRevenuePerInHouseGuest(JsonNullable<String> averageRevenuePerInHouseGuest) {
    this.averageRevenuePerInHouseGuest = averageRevenuePerInHouseGuest;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ManagerRevenue managerRevenue = (ManagerRevenue) o;
    return Objects.equals(this.roomRevenue, managerRevenue.roomRevenue) &&
        Objects.equals(this.averageRevenuePerInHouseGuest, managerRevenue.averageRevenuePerInHouseGuest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomRevenue, averageRevenuePerInHouseGuest);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ManagerRevenue {\n");
    sb.append("    roomRevenue: ").append(toIndentedString(roomRevenue)).append("\n");
    sb.append("    averageRevenuePerInHouseGuest: ").append(toIndentedString(averageRevenuePerInHouseGuest)).append("\n");
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

