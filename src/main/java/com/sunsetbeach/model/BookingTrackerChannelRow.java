package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.BookingChannel;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * BookingTrackerChannelRow
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerChannelRow {

  private JsonNullable<BookingChannel> channel = JsonNullable.<BookingChannel>undefined();

  private Integer roomNights;

  private String netRoomRevenue;

  private JsonNullable<String> netAdr = JsonNullable.<String>undefined();

  private JsonNullable<String> roomNightSharePercent = JsonNullable.<String>undefined();

  private JsonNullable<String> revenueSharePercent = JsonNullable.<String>undefined();

  public BookingTrackerChannelRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerChannelRow(BookingChannel channel, Integer roomNights, String netRoomRevenue, String netAdr, String roomNightSharePercent, String revenueSharePercent) {
    this.channel = JsonNullable.of(channel);
    this.roomNights = roomNights;
    this.netRoomRevenue = netRoomRevenue;
    this.netAdr = JsonNullable.of(netAdr);
    this.roomNightSharePercent = JsonNullable.of(roomNightSharePercent);
    this.revenueSharePercent = JsonNullable.of(revenueSharePercent);
  }

  public BookingTrackerChannelRow channel(BookingChannel channel) {
    this.channel = JsonNullable.of(channel);
    return this;
  }

  /**
   * Null on the total row.
   * @return channel
   */
  @NotNull @Valid 
  @JsonProperty("channel")
  public JsonNullable<BookingChannel> getChannel() {
    return channel;
  }

  public void setChannel(JsonNullable<BookingChannel> channel) {
    this.channel = channel;
  }

  public BookingTrackerChannelRow roomNights(Integer roomNights) {
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

  public BookingTrackerChannelRow netRoomRevenue(String netRoomRevenue) {
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

  public BookingTrackerChannelRow netAdr(String netAdr) {
    this.netAdr = JsonNullable.of(netAdr);
    return this;
  }

  /**
   * Get netAdr
   * @return netAdr
   */
  @NotNull 
  @JsonProperty("netAdr")
  public JsonNullable<String> getNetAdr() {
    return netAdr;
  }

  public void setNetAdr(JsonNullable<String> netAdr) {
    this.netAdr = netAdr;
  }

  public BookingTrackerChannelRow roomNightSharePercent(String roomNightSharePercent) {
    this.roomNightSharePercent = JsonNullable.of(roomNightSharePercent);
    return this;
  }

  /**
   * Get roomNightSharePercent
   * @return roomNightSharePercent
   */
  @NotNull 
  @JsonProperty("roomNightSharePercent")
  public JsonNullable<String> getRoomNightSharePercent() {
    return roomNightSharePercent;
  }

  public void setRoomNightSharePercent(JsonNullable<String> roomNightSharePercent) {
    this.roomNightSharePercent = roomNightSharePercent;
  }

  public BookingTrackerChannelRow revenueSharePercent(String revenueSharePercent) {
    this.revenueSharePercent = JsonNullable.of(revenueSharePercent);
    return this;
  }

  /**
   * Get revenueSharePercent
   * @return revenueSharePercent
   */
  @NotNull 
  @JsonProperty("revenueSharePercent")
  public JsonNullable<String> getRevenueSharePercent() {
    return revenueSharePercent;
  }

  public void setRevenueSharePercent(JsonNullable<String> revenueSharePercent) {
    this.revenueSharePercent = revenueSharePercent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerChannelRow bookingTrackerChannelRow = (BookingTrackerChannelRow) o;
    return Objects.equals(this.channel, bookingTrackerChannelRow.channel) &&
        Objects.equals(this.roomNights, bookingTrackerChannelRow.roomNights) &&
        Objects.equals(this.netRoomRevenue, bookingTrackerChannelRow.netRoomRevenue) &&
        Objects.equals(this.netAdr, bookingTrackerChannelRow.netAdr) &&
        Objects.equals(this.roomNightSharePercent, bookingTrackerChannelRow.roomNightSharePercent) &&
        Objects.equals(this.revenueSharePercent, bookingTrackerChannelRow.revenueSharePercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channel, roomNights, netRoomRevenue, netAdr, roomNightSharePercent, revenueSharePercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerChannelRow {\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    roomNights: ").append(toIndentedString(roomNights)).append("\n");
    sb.append("    netRoomRevenue: ").append(toIndentedString(netRoomRevenue)).append("\n");
    sb.append("    netAdr: ").append(toIndentedString(netAdr)).append("\n");
    sb.append("    roomNightSharePercent: ").append(toIndentedString(roomNightSharePercent)).append("\n");
    sb.append("    revenueSharePercent: ").append(toIndentedString(revenueSharePercent)).append("\n");
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

