package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.CalendarBooking;
import com.sunsetbeach.model.RoomTypeCalendar;
import com.sunsetbeach.model.RoomUnitBlock;
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
 * Everything the booking calendar grid needs for one &#x60;[from, to)&#x60; date range in a single request: room types with their physical units and per-day availability counts, every overlapping non-cancelled booking, and every overlapping &#x60;RoomUnitBlock&#x60; (returned raw - overlapping blocks on the same unit are not merged or deduplicated server-side, same as &#x60;GET /room-units/{id}/blocks&#x60;; rendering the overlap is a client concern). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T21:43:17.277610500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class BookingCalendarResponse {

  private String from;

  private String to;

  @Valid
  private List<@Valid RoomTypeCalendar> roomTypes = new ArrayList<>();

  @Valid
  private List<@Valid CalendarBooking> bookings = new ArrayList<>();

  @Valid
  private List<@Valid RoomUnitBlock> blocks = new ArrayList<>();

  public BookingCalendarResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingCalendarResponse(String from, String to, List<@Valid RoomTypeCalendar> roomTypes, List<@Valid CalendarBooking> bookings, List<@Valid RoomUnitBlock> blocks) {
    this.from = from;
    this.to = to;
    this.roomTypes = roomTypes;
    this.bookings = bookings;
    this.blocks = blocks;
  }

  public BookingCalendarResponse from(String from) {
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

  public BookingCalendarResponse to(String to) {
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

  public BookingCalendarResponse roomTypes(List<@Valid RoomTypeCalendar> roomTypes) {
    this.roomTypes = roomTypes;
    return this;
  }

  public BookingCalendarResponse addRoomTypesItem(RoomTypeCalendar roomTypesItem) {
    if (this.roomTypes == null) {
      this.roomTypes = new ArrayList<>();
    }
    this.roomTypes.add(roomTypesItem);
    return this;
  }

  /**
   * Get roomTypes
   * @return roomTypes
   */
  @NotNull @Valid 
  @JsonProperty("roomTypes")
  public List<@Valid RoomTypeCalendar> getRoomTypes() {
    return roomTypes;
  }

  public void setRoomTypes(List<@Valid RoomTypeCalendar> roomTypes) {
    this.roomTypes = roomTypes;
  }

  public BookingCalendarResponse bookings(List<@Valid CalendarBooking> bookings) {
    this.bookings = bookings;
    return this;
  }

  public BookingCalendarResponse addBookingsItem(CalendarBooking bookingsItem) {
    if (this.bookings == null) {
      this.bookings = new ArrayList<>();
    }
    this.bookings.add(bookingsItem);
    return this;
  }

  /**
   * Get bookings
   * @return bookings
   */
  @NotNull @Valid 
  @JsonProperty("bookings")
  public List<@Valid CalendarBooking> getBookings() {
    return bookings;
  }

  public void setBookings(List<@Valid CalendarBooking> bookings) {
    this.bookings = bookings;
  }

  public BookingCalendarResponse blocks(List<@Valid RoomUnitBlock> blocks) {
    this.blocks = blocks;
    return this;
  }

  public BookingCalendarResponse addBlocksItem(RoomUnitBlock blocksItem) {
    if (this.blocks == null) {
      this.blocks = new ArrayList<>();
    }
    this.blocks.add(blocksItem);
    return this;
  }

  /**
   * Get blocks
   * @return blocks
   */
  @NotNull @Valid 
  @JsonProperty("blocks")
  public List<@Valid RoomUnitBlock> getBlocks() {
    return blocks;
  }

  public void setBlocks(List<@Valid RoomUnitBlock> blocks) {
    this.blocks = blocks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingCalendarResponse bookingCalendarResponse = (BookingCalendarResponse) o;
    return Objects.equals(this.from, bookingCalendarResponse.from) &&
        Objects.equals(this.to, bookingCalendarResponse.to) &&
        Objects.equals(this.roomTypes, bookingCalendarResponse.roomTypes) &&
        Objects.equals(this.bookings, bookingCalendarResponse.bookings) &&
        Objects.equals(this.blocks, bookingCalendarResponse.blocks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, roomTypes, bookings, blocks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingCalendarResponse {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    roomTypes: ").append(toIndentedString(roomTypes)).append("\n");
    sb.append("    bookings: ").append(toIndentedString(bookings)).append("\n");
    sb.append("    blocks: ").append(toIndentedString(blocks)).append("\n");
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

