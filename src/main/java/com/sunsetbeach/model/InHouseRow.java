package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.MarketSegment;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One occupied room on the in-house list.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class InHouseRow {

  private String bookingId;

  private String roomName;

  private JsonNullable<String> roomUnitLabel = JsonNullable.<String>undefined();

  private Integer adults;

  private Integer children;

  private String guestName;

  private MarketSegment marketSegment;

  private String arrival;

  private String departure;

  public InHouseRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InHouseRow(String bookingId, String roomName, String roomUnitLabel, Integer adults, Integer children, String guestName, MarketSegment marketSegment, String arrival, String departure) {
    this.bookingId = bookingId;
    this.roomName = roomName;
    this.roomUnitLabel = JsonNullable.of(roomUnitLabel);
    this.adults = adults;
    this.children = children;
    this.guestName = guestName;
    this.marketSegment = marketSegment;
    this.arrival = arrival;
    this.departure = departure;
  }

  public InHouseRow bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * The reservation reference.
   * @return bookingId
   */
  @NotNull 
  @JsonProperty("bookingId")
  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public InHouseRow roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  /**
   * The room type's name.
   * @return roomName
   */
  @NotNull 
  @JsonProperty("roomName")
  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public InHouseRow roomUnitLabel(String roomUnitLabel) {
    this.roomUnitLabel = JsonNullable.of(roomUnitLabel);
    return this;
  }

  /**
   * The physical room's label, or null if none is assigned to tonight's segment.
   * @return roomUnitLabel
   */
  @NotNull 
  @JsonProperty("roomUnitLabel")
  public JsonNullable<String> getRoomUnitLabel() {
    return roomUnitLabel;
  }

  public void setRoomUnitLabel(JsonNullable<String> roomUnitLabel) {
    this.roomUnitLabel = roomUnitLabel;
  }

  public InHouseRow adults(Integer adults) {
    this.adults = adults;
    return this;
  }

  /**
   * Get adults
   * @return adults
   */
  @NotNull 
  @JsonProperty("adults")
  public Integer getAdults() {
    return adults;
  }

  public void setAdults(Integer adults) {
    this.adults = adults;
  }

  public InHouseRow children(Integer children) {
    this.children = children;
    return this;
  }

  /**
   * Get children
   * @return children
   */
  @NotNull 
  @JsonProperty("children")
  public Integer getChildren() {
    return children;
  }

  public void setChildren(Integer children) {
    this.children = children;
  }

  public InHouseRow guestName(String guestName) {
    this.guestName = guestName;
    return this;
  }

  /**
   * Get guestName
   * @return guestName
   */
  @NotNull 
  @JsonProperty("guestName")
  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }

  public InHouseRow marketSegment(MarketSegment marketSegment) {
    this.marketSegment = marketSegment;
    return this;
  }

  /**
   * Get marketSegment
   * @return marketSegment
   */
  @NotNull @Valid 
  @JsonProperty("marketSegment")
  public MarketSegment getMarketSegment() {
    return marketSegment;
  }

  public void setMarketSegment(MarketSegment marketSegment) {
    this.marketSegment = marketSegment;
  }

  public InHouseRow arrival(String arrival) {
    this.arrival = arrival;
    return this;
  }

  /**
   * The booking's `checkIn`.
   * @return arrival
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("arrival")
  public String getArrival() {
    return arrival;
  }

  public void setArrival(String arrival) {
    this.arrival = arrival;
  }

  public InHouseRow departure(String departure) {
    this.departure = departure;
    return this;
  }

  /**
   * The booking's `checkOut`.
   * @return departure
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("departure")
  public String getDeparture() {
    return departure;
  }

  public void setDeparture(String departure) {
    this.departure = departure;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InHouseRow inHouseRow = (InHouseRow) o;
    return Objects.equals(this.bookingId, inHouseRow.bookingId) &&
        Objects.equals(this.roomName, inHouseRow.roomName) &&
        Objects.equals(this.roomUnitLabel, inHouseRow.roomUnitLabel) &&
        Objects.equals(this.adults, inHouseRow.adults) &&
        Objects.equals(this.children, inHouseRow.children) &&
        Objects.equals(this.guestName, inHouseRow.guestName) &&
        Objects.equals(this.marketSegment, inHouseRow.marketSegment) &&
        Objects.equals(this.arrival, inHouseRow.arrival) &&
        Objects.equals(this.departure, inHouseRow.departure);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, roomName, roomUnitLabel, adults, children, guestName, marketSegment, arrival, departure);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InHouseRow {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    roomUnitLabel: ").append(toIndentedString(roomUnitLabel)).append("\n");
    sb.append("    adults: ").append(toIndentedString(adults)).append("\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
    sb.append("    guestName: ").append("[REDACTED]").append("\n");
    sb.append("    marketSegment: ").append(toIndentedString(marketSegment)).append("\n");
    sb.append("    arrival: ").append(toIndentedString(arrival)).append("\n");
    sb.append("    departure: ").append(toIndentedString(departure)).append("\n");
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

