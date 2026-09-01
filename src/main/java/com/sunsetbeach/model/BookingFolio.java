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
 * &#x60;folioTotal&#x60; &#x3D; &#x60;roomTotal&#x60; + &#x60;roomChargesTotal&#x60;, computed on the fly by &#x60;BookingService.computeFolio&#x60;/&#x60;getFolio&#x60; - see that method&#39;s Javadoc. Never stored. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingFolio {

  private String roomTotal;

  private String roomChargesTotal;

  private String folioTotal;

  private Integer roomChargeCount;

  public BookingFolio() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingFolio(String roomTotal, String roomChargesTotal, String folioTotal, Integer roomChargeCount) {
    this.roomTotal = roomTotal;
    this.roomChargesTotal = roomChargesTotal;
    this.folioTotal = folioTotal;
    this.roomChargeCount = roomChargeCount;
  }

  public BookingFolio roomTotal(String roomTotal) {
    this.roomTotal = roomTotal;
    return this;
  }

  /**
   * `Booking.totalPrice` - decimal(10,2) rendered as a string.
   * @return roomTotal
   */
  @NotNull 
  @JsonProperty("roomTotal")
  public String getRoomTotal() {
    return roomTotal;
  }

  public void setRoomTotal(String roomTotal) {
    this.roomTotal = roomTotal;
  }

  public BookingFolio roomChargesTotal(String roomChargesTotal) {
    this.roomChargesTotal = roomChargesTotal;
    return this;
  }

  /**
   * Sum of this booking's ROOM_CHARGE `Payment.amount` rows - decimal(10,2) rendered as a string.
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

  public BookingFolio folioTotal(String folioTotal) {
    this.folioTotal = folioTotal;
    return this;
  }

  /**
   * `roomTotal` + `roomChargesTotal` - decimal(10,2) rendered as a string.
   * @return folioTotal
   */
  @NotNull 
  @JsonProperty("folioTotal")
  public String getFolioTotal() {
    return folioTotal;
  }

  public void setFolioTotal(String folioTotal) {
    this.folioTotal = folioTotal;
  }

  public BookingFolio roomChargeCount(Integer roomChargeCount) {
    this.roomChargeCount = roomChargeCount;
    return this;
  }

  /**
   * Number of ROOM_CHARGE payments summed into `roomChargesTotal` - same count as `GET /bookings/{id}/pos-orders` would return entries.
   * @return roomChargeCount
   */
  @NotNull 
  @JsonProperty("roomChargeCount")
  public Integer getRoomChargeCount() {
    return roomChargeCount;
  }

  public void setRoomChargeCount(Integer roomChargeCount) {
    this.roomChargeCount = roomChargeCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingFolio bookingFolio = (BookingFolio) o;
    return Objects.equals(this.roomTotal, bookingFolio.roomTotal) &&
        Objects.equals(this.roomChargesTotal, bookingFolio.roomChargesTotal) &&
        Objects.equals(this.folioTotal, bookingFolio.folioTotal) &&
        Objects.equals(this.roomChargeCount, bookingFolio.roomChargeCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomTotal, roomChargesTotal, folioTotal, roomChargeCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingFolio {\n");
    sb.append("    roomTotal: ").append(toIndentedString(roomTotal)).append("\n");
    sb.append("    roomChargesTotal: ").append(toIndentedString(roomChargesTotal)).append("\n");
    sb.append("    folioTotal: ").append(toIndentedString(folioTotal)).append("\n");
    sb.append("    roomChargeCount: ").append(toIndentedString(roomChargeCount)).append("\n");
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

