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
 * BookingTrackerChannelRank
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerChannelRank {

  private Integer rank;

  private BookingChannel channel;

  private String netRoomRevenue;

  private Integer roomNights;

  public BookingTrackerChannelRank() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerChannelRank(Integer rank, BookingChannel channel, String netRoomRevenue, Integer roomNights) {
    this.rank = rank;
    this.channel = channel;
    this.netRoomRevenue = netRoomRevenue;
    this.roomNights = roomNights;
  }

  public BookingTrackerChannelRank rank(Integer rank) {
    this.rank = rank;
    return this;
  }

  /**
   * Get rank
   * @return rank
   */
  @NotNull 
  @JsonProperty("rank")
  public Integer getRank() {
    return rank;
  }

  public void setRank(Integer rank) {
    this.rank = rank;
  }

  public BookingTrackerChannelRank channel(BookingChannel channel) {
    this.channel = channel;
    return this;
  }

  /**
   * Get channel
   * @return channel
   */
  @NotNull @Valid 
  @JsonProperty("channel")
  public BookingChannel getChannel() {
    return channel;
  }

  public void setChannel(BookingChannel channel) {
    this.channel = channel;
  }

  public BookingTrackerChannelRank netRoomRevenue(String netRoomRevenue) {
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

  public BookingTrackerChannelRank roomNights(Integer roomNights) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerChannelRank bookingTrackerChannelRank = (BookingTrackerChannelRank) o;
    return Objects.equals(this.rank, bookingTrackerChannelRank.rank) &&
        Objects.equals(this.channel, bookingTrackerChannelRank.channel) &&
        Objects.equals(this.netRoomRevenue, bookingTrackerChannelRank.netRoomRevenue) &&
        Objects.equals(this.roomNights, bookingTrackerChannelRank.roomNights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rank, channel, netRoomRevenue, roomNights);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerChannelRank {\n");
    sb.append("    rank: ").append(toIndentedString(rank)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    netRoomRevenue: ").append(toIndentedString(netRoomRevenue)).append("\n");
    sb.append("    roomNights: ").append(toIndentedString(roomNights)).append("\n");
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

