package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.Booking;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;POST /bookings/{id}/check-out&#x60;. &#x60;outstandingBalance&#x60; is the same number &#x60;GET /bookings/{id}/folio&#x60; would report at this instant (room total, if &#x60;status&#x60; isn&#39;t yet &#x60;PAID&#x60;, plus any uncollected &#x60;ROOM_CHARGE&#x60; payments) - the last moment front desk can act on it before the guest walks out. &#x60;\&quot;0.00\&quot;&#x60; means nothing is owed, not that the field was skipped. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class CheckOutResult {

  private Booking booking;

  private String outstandingBalance;

  public CheckOutResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CheckOutResult(Booking booking, String outstandingBalance) {
    this.booking = booking;
    this.outstandingBalance = outstandingBalance;
  }

  public CheckOutResult booking(Booking booking) {
    this.booking = booking;
    return this;
  }

  /**
   * Get booking
   * @return booking
   */
  @NotNull @Valid 
  @JsonProperty("booking")
  public Booking getBooking() {
    return booking;
  }

  public void setBooking(Booking booking) {
    this.booking = booking;
  }

  public CheckOutResult outstandingBalance(String outstandingBalance) {
    this.outstandingBalance = outstandingBalance;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return outstandingBalance
   */
  @NotNull 
  @JsonProperty("outstandingBalance")
  public String getOutstandingBalance() {
    return outstandingBalance;
  }

  public void setOutstandingBalance(String outstandingBalance) {
    this.outstandingBalance = outstandingBalance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckOutResult checkOutResult = (CheckOutResult) o;
    return Objects.equals(this.booking, checkOutResult.booking) &&
        Objects.equals(this.outstandingBalance, checkOutResult.outstandingBalance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(booking, outstandingBalance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckOutResult {\n");
    sb.append("    booking: ").append(toIndentedString(booking)).append("\n");
    sb.append("    outstandingBalance: ").append(toIndentedString(outstandingBalance)).append("\n");
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

