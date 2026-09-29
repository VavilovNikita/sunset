package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.InHouseRow;
import com.sunsetbeach.model.InHouseTotal;
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
 * &#x60;GET /reports/in-house&#x60; - see that operation for who is listed.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class InHouseReport {

  private String date;

  @Valid
  private List<@Valid InHouseRow> rooms = new ArrayList<>();

  private InHouseTotal total;

  public InHouseReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InHouseReport(String date, List<@Valid InHouseRow> rooms, InHouseTotal total) {
    this.date = date;
    this.rooms = rooms;
    this.total = total;
  }

  public InHouseReport date(String date) {
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

  public InHouseReport rooms(List<@Valid InHouseRow> rooms) {
    this.rooms = rooms;
    return this;
  }

  public InHouseReport addRoomsItem(InHouseRow roomsItem) {
    if (this.rooms == null) {
      this.rooms = new ArrayList<>();
    }
    this.rooms.add(roomsItem);
    return this;
  }

  /**
   * One row per occupied room, sorted by room label, unassigned last.
   * @return rooms
   */
  @NotNull @Valid 
  @JsonProperty("rooms")
  public List<@Valid InHouseRow> getRooms() {
    return rooms;
  }

  public void setRooms(List<@Valid InHouseRow> rooms) {
    this.rooms = rooms;
  }

  public InHouseReport total(InHouseTotal total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull @Valid 
  @JsonProperty("total")
  public InHouseTotal getTotal() {
    return total;
  }

  public void setTotal(InHouseTotal total) {
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
    InHouseReport inHouseReport = (InHouseReport) o;
    return Objects.equals(this.date, inHouseReport.date) &&
        Objects.equals(this.rooms, inHouseReport.rooms) &&
        Objects.equals(this.total, inHouseReport.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, rooms, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InHouseReport {\n");
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

