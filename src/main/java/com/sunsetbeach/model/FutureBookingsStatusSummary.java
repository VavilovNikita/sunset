package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.BookingStatus;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * FutureBookingsStatusSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class FutureBookingsStatusSummary {

  private BookingStatus status;

  private Integer bookings;

  private Integer nights;

  private String totalPrice;

  public FutureBookingsStatusSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FutureBookingsStatusSummary(BookingStatus status, Integer bookings, Integer nights, String totalPrice) {
    this.status = status;
    this.bookings = bookings;
    this.nights = nights;
    this.totalPrice = totalPrice;
  }

  public FutureBookingsStatusSummary status(BookingStatus status) {
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

  public FutureBookingsStatusSummary bookings(Integer bookings) {
    this.bookings = bookings;
    return this;
  }

  /**
   * Get bookings
   * @return bookings
   */
  @NotNull 
  @JsonProperty("bookings")
  public Integer getBookings() {
    return bookings;
  }

  public void setBookings(Integer bookings) {
    this.bookings = bookings;
  }

  public FutureBookingsStatusSummary nights(Integer nights) {
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

  public FutureBookingsStatusSummary totalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * Sum of the rows' `totalPrice`, Decimal(14,2) as a string.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FutureBookingsStatusSummary futureBookingsStatusSummary = (FutureBookingsStatusSummary) o;
    return Objects.equals(this.status, futureBookingsStatusSummary.status) &&
        Objects.equals(this.bookings, futureBookingsStatusSummary.bookings) &&
        Objects.equals(this.nights, futureBookingsStatusSummary.nights) &&
        Objects.equals(this.totalPrice, futureBookingsStatusSummary.totalPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, bookings, nights, totalPrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FutureBookingsStatusSummary {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    bookings: ").append(toIndentedString(bookings)).append("\n");
    sb.append("    nights: ").append(toIndentedString(nights)).append("\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
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

