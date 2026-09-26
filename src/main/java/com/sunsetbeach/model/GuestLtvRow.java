package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * GuestLtvRow
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestLtvRow {

  private String guestId;

  private String name;

  private JsonNullable<String> email = JsonNullable.<String>undefined();

  private Integer bookingCount;

  private Integer totalNights;

  private String roomRevenue;

  private String roomChargesTotal;

  private String firstCheckIn;

  private String lastCheckIn;

  public GuestLtvRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestLtvRow(String guestId, String name, String email, Integer bookingCount, Integer totalNights, String roomRevenue, String roomChargesTotal, String firstCheckIn, String lastCheckIn) {
    this.guestId = guestId;
    this.name = name;
    this.email = JsonNullable.of(email);
    this.bookingCount = bookingCount;
    this.totalNights = totalNights;
    this.roomRevenue = roomRevenue;
    this.roomChargesTotal = roomChargesTotal;
    this.firstCheckIn = firstCheckIn;
    this.lastCheckIn = lastCheckIn;
  }

  public GuestLtvRow guestId(String guestId) {
    this.guestId = guestId;
    return this;
  }

  /**
   * Get guestId
   * @return guestId
   */
  @NotNull 
  @JsonProperty("guestId")
  public String getGuestId() {
    return guestId;
  }

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public GuestLtvRow name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public GuestLtvRow email(String email) {
    this.email = JsonNullable.of(email);
    return this;
  }

  /**
   * Get email
   * @return email
   */
  @NotNull 
  @JsonProperty("email")
  public JsonNullable<String> getEmail() {
    return email;
  }

  public void setEmail(JsonNullable<String> email) {
    this.email = email;
  }

  public GuestLtvRow bookingCount(Integer bookingCount) {
    this.bookingCount = bookingCount;
    return this;
  }

  /**
   * Bookings linked to this guest that are not `CANCELLED`.
   * @return bookingCount
   */
  @NotNull 
  @JsonProperty("bookingCount")
  public Integer getBookingCount() {
    return bookingCount;
  }

  public void setBookingCount(Integer bookingCount) {
    this.bookingCount = bookingCount;
  }

  public GuestLtvRow totalNights(Integer totalNights) {
    this.totalNights = totalNights;
    return this;
  }

  /**
   * Nights across those bookings' segments.
   * @return totalNights
   */
  @NotNull 
  @JsonProperty("totalNights")
  public Integer getTotalNights() {
    return totalNights;
  }

  public void setTotalNights(Integer totalNights) {
    this.totalNights = totalNights;
  }

  public GuestLtvRow roomRevenue(String roomRevenue) {
    this.roomRevenue = roomRevenue;
    return this;
  }

  /**
   * Sum of those bookings' `totalPrice` - room price only. The ranking key.
   * @return roomRevenue
   */
  @NotNull 
  @JsonProperty("roomRevenue")
  public String getRoomRevenue() {
    return roomRevenue;
  }

  public void setRoomRevenue(String roomRevenue) {
    this.roomRevenue = roomRevenue;
  }

  public GuestLtvRow roomChargesTotal(String roomChargesTotal) {
    this.roomChargesTotal = roomChargesTotal;
    return this;
  }

  /**
   * Sum of `ROOM_CHARGE` payments posted to those bookings - POS orders charged to the room. Gross of any later folio settlement: it is what was spent, not what is still owed. 
   * @return roomChargesTotal
   */
  @NotNull 
  @JsonProperty("roomChargesTotal")
  public String getRoomChargesTotal() {
    return roomChargesTotal;
  }

  public void setRoomChargesTotal(String roomChargesTotal) {
    this.roomChargesTotal = roomChargesTotal;
  }

  public GuestLtvRow firstCheckIn(String firstCheckIn) {
    this.firstCheckIn = firstCheckIn;
    return this;
  }

  /**
   * Earliest `checkIn` among those bookings.
   * @return firstCheckIn
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("firstCheckIn")
  public String getFirstCheckIn() {
    return firstCheckIn;
  }

  public void setFirstCheckIn(String firstCheckIn) {
    this.firstCheckIn = firstCheckIn;
  }

  public GuestLtvRow lastCheckIn(String lastCheckIn) {
    this.lastCheckIn = lastCheckIn;
    return this;
  }

  /**
   * Latest `checkIn` among those bookings - can be in the future.
   * @return lastCheckIn
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("lastCheckIn")
  public String getLastCheckIn() {
    return lastCheckIn;
  }

  public void setLastCheckIn(String lastCheckIn) {
    this.lastCheckIn = lastCheckIn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestLtvRow guestLtvRow = (GuestLtvRow) o;
    return Objects.equals(this.guestId, guestLtvRow.guestId) &&
        Objects.equals(this.name, guestLtvRow.name) &&
        Objects.equals(this.email, guestLtvRow.email) &&
        Objects.equals(this.bookingCount, guestLtvRow.bookingCount) &&
        Objects.equals(this.totalNights, guestLtvRow.totalNights) &&
        Objects.equals(this.roomRevenue, guestLtvRow.roomRevenue) &&
        Objects.equals(this.roomChargesTotal, guestLtvRow.roomChargesTotal) &&
        Objects.equals(this.firstCheckIn, guestLtvRow.firstCheckIn) &&
        Objects.equals(this.lastCheckIn, guestLtvRow.lastCheckIn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guestId, name, email, bookingCount, totalNights, roomRevenue, roomChargesTotal, firstCheckIn, lastCheckIn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestLtvRow {\n");
    sb.append("    guestId: ").append(toIndentedString(guestId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append("[REDACTED]").append("\n");
    sb.append("    bookingCount: ").append(toIndentedString(bookingCount)).append("\n");
    sb.append("    totalNights: ").append(toIndentedString(totalNights)).append("\n");
    sb.append("    roomRevenue: ").append(toIndentedString(roomRevenue)).append("\n");
    sb.append("    roomChargesTotal: ").append(toIndentedString(roomChargesTotal)).append("\n");
    sb.append("    firstCheckIn: ").append(toIndentedString(firstCheckIn)).append("\n");
    sb.append("    lastCheckIn: ").append(toIndentedString(lastCheckIn)).append("\n");
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

