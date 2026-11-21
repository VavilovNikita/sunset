package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * FutureBookingRow
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class FutureBookingRow {

  private String bookingId;

  private String guestName;

  private String roomName;

  private JsonNullable<String> roomUnitLabel = JsonNullable.<String>undefined();

  private String checkIn;

  private String checkOut;

  private Integer nights;

  private Integer adults;

  private Integer children;

  private BookingChannel channel;

  private BookingStatus status;

  private String totalPrice;

  private String balanceDue;

  private JsonNullable<String> notes = JsonNullable.<String>undefined();

  public FutureBookingRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FutureBookingRow(String bookingId, String guestName, String roomName, String roomUnitLabel, String checkIn, String checkOut, Integer nights, Integer adults, Integer children, BookingChannel channel, BookingStatus status, String totalPrice, String balanceDue, String notes) {
    this.bookingId = bookingId;
    this.guestName = guestName;
    this.roomName = roomName;
    this.roomUnitLabel = JsonNullable.of(roomUnitLabel);
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.nights = nights;
    this.adults = adults;
    this.children = children;
    this.channel = channel;
    this.status = status;
    this.totalPrice = totalPrice;
    this.balanceDue = balanceDue;
    this.notes = JsonNullable.of(notes);
  }

  public FutureBookingRow bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * Get bookingId
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

  public FutureBookingRow guestName(String guestName) {
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

  public FutureBookingRow roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  /**
   * Room type of the booking's first night.
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

  public FutureBookingRow roomUnitLabel(String roomUnitLabel) {
    this.roomUnitLabel = JsonNullable.of(roomUnitLabel);
    return this;
  }

  /**
   * Physical room of the first night; null while none is assigned.
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

  public FutureBookingRow checkIn(String checkIn) {
    this.checkIn = checkIn;
    return this;
  }

  /**
   * Get checkIn
   * @return checkIn
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("checkIn")
  public String getCheckIn() {
    return checkIn;
  }

  public void setCheckIn(String checkIn) {
    this.checkIn = checkIn;
  }

  public FutureBookingRow checkOut(String checkOut) {
    this.checkOut = checkOut;
    return this;
  }

  /**
   * Get checkOut
   * @return checkOut
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("checkOut")
  public String getCheckOut() {
    return checkOut;
  }

  public void setCheckOut(String checkOut) {
    this.checkOut = checkOut;
  }

  public FutureBookingRow nights(Integer nights) {
    this.nights = nights;
    return this;
  }

  /**
   * Get nights
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

  public FutureBookingRow adults(Integer adults) {
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

  public FutureBookingRow children(Integer children) {
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

  public FutureBookingRow channel(BookingChannel channel) {
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

  public FutureBookingRow status(BookingStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public BookingStatus getStatus() {
    return status;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }

  public FutureBookingRow totalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string. Same as `Booking.totalPrice`.
   * @return totalPrice
   */
  @NotNull 
  @JsonProperty("totalPrice")
  public String getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
  }

  public FutureBookingRow balanceDue(String balanceDue) {
    this.balanceDue = balanceDue;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string. Same as `Booking.balanceDue`.
   * @return balanceDue
   */
  @NotNull 
  @JsonProperty("balanceDue")
  public String getBalanceDue() {
    return balanceDue;
  }

  public void setBalanceDue(String balanceDue) {
    this.balanceDue = balanceDue;
  }

  public FutureBookingRow notes(String notes) {
    this.notes = JsonNullable.of(notes);
    return this;
  }

  /**
   * Get notes
   * @return notes
   */
  @NotNull 
  @JsonProperty("notes")
  public JsonNullable<String> getNotes() {
    return notes;
  }

  public void setNotes(JsonNullable<String> notes) {
    this.notes = notes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FutureBookingRow futureBookingRow = (FutureBookingRow) o;
    return Objects.equals(this.bookingId, futureBookingRow.bookingId) &&
        Objects.equals(this.guestName, futureBookingRow.guestName) &&
        Objects.equals(this.roomName, futureBookingRow.roomName) &&
        Objects.equals(this.roomUnitLabel, futureBookingRow.roomUnitLabel) &&
        Objects.equals(this.checkIn, futureBookingRow.checkIn) &&
        Objects.equals(this.checkOut, futureBookingRow.checkOut) &&
        Objects.equals(this.nights, futureBookingRow.nights) &&
        Objects.equals(this.adults, futureBookingRow.adults) &&
        Objects.equals(this.children, futureBookingRow.children) &&
        Objects.equals(this.channel, futureBookingRow.channel) &&
        Objects.equals(this.status, futureBookingRow.status) &&
        Objects.equals(this.totalPrice, futureBookingRow.totalPrice) &&
        Objects.equals(this.balanceDue, futureBookingRow.balanceDue) &&
        Objects.equals(this.notes, futureBookingRow.notes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, guestName, roomName, roomUnitLabel, checkIn, checkOut, nights, adults, children, channel, status, totalPrice, balanceDue, notes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FutureBookingRow {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    roomUnitLabel: ").append(toIndentedString(roomUnitLabel)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    nights: ").append(toIndentedString(nights)).append("\n");
    sb.append("    adults: ").append(toIndentedString(adults)).append("\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
    sb.append("    balanceDue: ").append(toIndentedString(balanceDue)).append("\n");
    sb.append("    notes: ").append("[REDACTED]").append("\n");
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

