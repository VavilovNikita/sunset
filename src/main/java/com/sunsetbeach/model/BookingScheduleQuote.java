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
 * Response of &#x60;POST /bookings/{id}/schedule/quote&#x60; - a non-mutating preview of what &#x60;PATCH /bookings/{id}/schedule&#x60; would do with the same request body. &#x60;totalPrice&#x60; is always computed (pure arithmetic over &#x60;RatePlan&#x60;/&#x60;Room.basePrice&#x60; for the requested dates, independent of availability); &#x60;available&#x60;/&#x60;reason&#x60; report whether applying this exact change would currently succeed. This is advisory, not a lock - the apply call re-validates from scratch inside its own &#x60;Serializable&#x60; transaction, so a &#x60;true&#x60; here can still lose a race to a concurrent change made between preview and apply. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T21:43:17.277610500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class BookingScheduleQuote {

  private String totalPrice;

  private Integer nights;

  private Boolean available;

  private JsonNullable<String> reason = JsonNullable.<String>undefined();

  public BookingScheduleQuote() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingScheduleQuote(String totalPrice, Integer nights, Boolean available, String reason) {
    this.totalPrice = totalPrice;
    this.nights = nights;
    this.available = available;
    this.reason = JsonNullable.of(reason);
  }

  public BookingScheduleQuote totalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string, recomputed for the requested `checkIn`/`checkOut`.
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

  public BookingScheduleQuote nights(Integer nights) {
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

  public BookingScheduleQuote available(Boolean available) {
    this.available = available;
    return this;
  }

  /**
   * Get available
   * @return available
   */
  @NotNull 
  @JsonProperty("available")
  public Boolean getAvailable() {
    return available;
  }

  public void setAvailable(Boolean available) {
    this.available = available;
  }

  public BookingScheduleQuote reason(String reason) {
    this.reason = JsonNullable.of(reason);
    return this;
  }

  /**
   * Set when `available` is `false` - the same wording `PATCH /bookings/{id}/schedule` would reject with.
   * @return reason
   */
  @NotNull 
  @JsonProperty("reason")
  public JsonNullable<String> getReason() {
    return reason;
  }

  public void setReason(JsonNullable<String> reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingScheduleQuote bookingScheduleQuote = (BookingScheduleQuote) o;
    return Objects.equals(this.totalPrice, bookingScheduleQuote.totalPrice) &&
        Objects.equals(this.nights, bookingScheduleQuote.nights) &&
        Objects.equals(this.available, bookingScheduleQuote.available) &&
        Objects.equals(this.reason, bookingScheduleQuote.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalPrice, nights, available, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingScheduleQuote {\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
    sb.append("    nights: ").append(toIndentedString(nights)).append("\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

