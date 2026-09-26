package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.OccupancyReportRow;
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
 * &#x60;GET /reports/occupancy&#x60; - see that operation for how each figure is computed.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class OccupancyReport {

  private String from;

  private String to;

  private Integer nights;

  @Valid
  private List<@Valid OccupancyReportRow> rooms = new ArrayList<>();

  private OccupancyReportRow total;

  public OccupancyReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OccupancyReport(String from, String to, Integer nights, List<@Valid OccupancyReportRow> rooms, OccupancyReportRow total) {
    this.from = from;
    this.to = to;
    this.nights = nights;
    this.rooms = rooms;
    this.total = total;
  }

  public OccupancyReport from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public OccupancyReport to(String to) {
    this.to = to;
    return this;
  }

  /**
   * Get to
   * @return to
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public OccupancyReport nights(Integer nights) {
    this.nights = nights;
    return this;
  }

  /**
   * Number of nights in the range (`to` - `from` + 1).
   * @return nights
   */
  @NotNull 
  @JsonProperty("nights")
  public Integer getNights() {
    return nights;
  }

  public void setNights(Integer nights) {
    this.nights = nights;
  }

  public OccupancyReport rooms(List<@Valid OccupancyReportRow> rooms) {
    this.rooms = rooms;
    return this;
  }

  public OccupancyReport addRoomsItem(OccupancyReportRow roomsItem) {
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
  public List<@Valid OccupancyReportRow> getRooms() {
    return rooms;
  }

  public void setRooms(List<@Valid OccupancyReportRow> rooms) {
    this.rooms = rooms;
  }

  public OccupancyReport total(OccupancyReportRow total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull @Valid 
  @JsonProperty("total")
  public OccupancyReportRow getTotal() {
    return total;
  }

  public void setTotal(OccupancyReportRow total) {
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
    OccupancyReport occupancyReport = (OccupancyReport) o;
    return Objects.equals(this.from, occupancyReport.from) &&
        Objects.equals(this.to, occupancyReport.to) &&
        Objects.equals(this.nights, occupancyReport.nights) &&
        Objects.equals(this.rooms, occupancyReport.rooms) &&
        Objects.equals(this.total, occupancyReport.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, nights, rooms, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OccupancyReport {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    nights: ").append(toIndentedString(nights)).append("\n");
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

