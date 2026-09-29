package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.ManagerAccountCount;
import com.sunsetbeach.model.ManagerForecast;
import com.sunsetbeach.model.ManagerGuestStatistic;
import com.sunsetbeach.model.ManagerRevenue;
import com.sunsetbeach.model.ManagerRoomStatistic;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Every figure of the manager report for one night.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ManagerReportDay {

  private ManagerRoomStatistic rooms;

  private ManagerGuestStatistic guests;

  private ManagerAccountCount accounts;

  private ManagerRevenue revenue;

  private ManagerForecast tomorrow;

  public ManagerReportDay() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ManagerReportDay(ManagerRoomStatistic rooms, ManagerGuestStatistic guests, ManagerAccountCount accounts, ManagerRevenue revenue, ManagerForecast tomorrow) {
    this.rooms = rooms;
    this.guests = guests;
    this.accounts = accounts;
    this.revenue = revenue;
    this.tomorrow = tomorrow;
  }

  public ManagerReportDay rooms(ManagerRoomStatistic rooms) {
    this.rooms = rooms;
    return this;
  }

  /**
   * Get rooms
   * @return rooms
   */
  @NotNull @Valid 
  @JsonProperty("rooms")
  public ManagerRoomStatistic getRooms() {
    return rooms;
  }

  public void setRooms(ManagerRoomStatistic rooms) {
    this.rooms = rooms;
  }

  public ManagerReportDay guests(ManagerGuestStatistic guests) {
    this.guests = guests;
    return this;
  }

  /**
   * Get guests
   * @return guests
   */
  @NotNull @Valid 
  @JsonProperty("guests")
  public ManagerGuestStatistic getGuests() {
    return guests;
  }

  public void setGuests(ManagerGuestStatistic guests) {
    this.guests = guests;
  }

  public ManagerReportDay accounts(ManagerAccountCount accounts) {
    this.accounts = accounts;
    return this;
  }

  /**
   * Get accounts
   * @return accounts
   */
  @NotNull @Valid 
  @JsonProperty("accounts")
  public ManagerAccountCount getAccounts() {
    return accounts;
  }

  public void setAccounts(ManagerAccountCount accounts) {
    this.accounts = accounts;
  }

  public ManagerReportDay revenue(ManagerRevenue revenue) {
    this.revenue = revenue;
    return this;
  }

  /**
   * Get revenue
   * @return revenue
   */
  @NotNull @Valid 
  @JsonProperty("revenue")
  public ManagerRevenue getRevenue() {
    return revenue;
  }

  public void setRevenue(ManagerRevenue revenue) {
    this.revenue = revenue;
  }

  public ManagerReportDay tomorrow(ManagerForecast tomorrow) {
    this.tomorrow = tomorrow;
    return this;
  }

  /**
   * Get tomorrow
   * @return tomorrow
   */
  @NotNull @Valid 
  @JsonProperty("tomorrow")
  public ManagerForecast getTomorrow() {
    return tomorrow;
  }

  public void setTomorrow(ManagerForecast tomorrow) {
    this.tomorrow = tomorrow;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ManagerReportDay managerReportDay = (ManagerReportDay) o;
    return Objects.equals(this.rooms, managerReportDay.rooms) &&
        Objects.equals(this.guests, managerReportDay.guests) &&
        Objects.equals(this.accounts, managerReportDay.accounts) &&
        Objects.equals(this.revenue, managerReportDay.revenue) &&
        Objects.equals(this.tomorrow, managerReportDay.tomorrow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rooms, guests, accounts, revenue, tomorrow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ManagerReportDay {\n");
    sb.append("    rooms: ").append(toIndentedString(rooms)).append("\n");
    sb.append("    guests: ").append(toIndentedString(guests)).append("\n");
    sb.append("    accounts: ").append(toIndentedString(accounts)).append("\n");
    sb.append("    revenue: ").append(toIndentedString(revenue)).append("\n");
    sb.append("    tomorrow: ").append(toIndentedString(tomorrow)).append("\n");
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

