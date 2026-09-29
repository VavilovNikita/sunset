package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.ForecastRow;
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
 * One date of the forecast.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ForecastDay {

  private String date;

  @Valid
  private List<@Valid ForecastRow> rooms = new ArrayList<>();

  private ForecastRow total;

  public ForecastDay() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ForecastDay(String date, List<@Valid ForecastRow> rooms, ForecastRow total) {
    this.date = date;
    this.rooms = rooms;
    this.total = total;
  }

  public ForecastDay date(String date) {
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

  public ForecastDay rooms(List<@Valid ForecastRow> rooms) {
    this.rooms = rooms;
    return this;
  }

  public ForecastDay addRoomsItem(ForecastRow roomsItem) {
    if (this.rooms == null) {
      this.rooms = new ArrayList<>();
    }
    this.rooms.add(roomsItem);
    return this;
  }

  /**
   * One row per room type, sorted by name.
   * @return rooms
   */
  @NotNull @Valid 
  @JsonProperty("rooms")
  public List<@Valid ForecastRow> getRooms() {
    return rooms;
  }

  public void setRooms(List<@Valid ForecastRow> rooms) {
    this.rooms = rooms;
  }

  public ForecastDay total(ForecastRow total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull @Valid 
  @JsonProperty("total")
  public ForecastRow getTotal() {
    return total;
  }

  public void setTotal(ForecastRow total) {
    this.total = total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForecastDay forecastDay = (ForecastDay) o;
    return Objects.equals(this.date, forecastDay.date) &&
        Objects.equals(this.rooms, forecastDay.rooms) &&
        Objects.equals(this.total, forecastDay.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, rooms, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForecastDay {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    rooms: ").append(toIndentedString(rooms)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
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

