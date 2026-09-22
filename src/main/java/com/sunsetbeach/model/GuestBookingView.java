package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.BookingStatus;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The guest-facing projection of &#x60;Booking&#x60; returned by &#x60;GET /guest/bookings&#x60; - deliberately not &#x60;Booking&#x60; itself, the same \&quot;narrow, guest-facing DTO\&quot; discipline as &#x60;GuestOrderView&#x60;. Never carries &#x60;paymentNote&#x60;, &#x60;source&#x60;, &#x60;expiryReminderSent&#x60;, &#x60;guestId&#x60;/&#x60;guest&#x60;, &#x60;roomUnitId&#x60;/&#x60;roomUnit&#x60;, or occupancy timestamps/status - none of that is this guest&#39;s business or safe to hand back to their own phone. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestBookingView {

  private String id;

  private String roomName;

  private String checkIn;

  private String checkOut;

  private String totalPrice;

  private BookingStatus status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public GuestBookingView() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestBookingView(String id, String roomName, String checkIn, String checkOut, String totalPrice, BookingStatus status, OffsetDateTime createdAt) {
    this.id = id;
    this.roomName = roomName;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.totalPrice = totalPrice;
    this.status = status;
    this.createdAt = createdAt;
  }

  public GuestBookingView id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public GuestBookingView roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  /**
   * `Room.name` at read time - denormalized, not frozen (same convention as `SpaAppointmentTreatment.currentPrice`), since a room type's name is cosmetic, not an agreed term.
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

  public GuestBookingView checkIn(String checkIn) {
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

  public GuestBookingView checkOut(String checkOut) {
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

  public GuestBookingView totalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string, e.g. `\"4500.00\"` - same server-computed total as `Booking.totalPrice`.
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

  public GuestBookingView status(BookingStatus status) {
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

  public GuestBookingView createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestBookingView guestBookingView = (GuestBookingView) o;
    return Objects.equals(this.id, guestBookingView.id) &&
        Objects.equals(this.roomName, guestBookingView.roomName) &&
        Objects.equals(this.checkIn, guestBookingView.checkIn) &&
        Objects.equals(this.checkOut, guestBookingView.checkOut) &&
        Objects.equals(this.totalPrice, guestBookingView.totalPrice) &&
        Objects.equals(this.status, guestBookingView.status) &&
        Objects.equals(this.createdAt, guestBookingView.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, roomName, checkIn, checkOut, totalPrice, status, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestBookingView {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

