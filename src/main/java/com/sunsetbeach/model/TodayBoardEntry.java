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
 * One row of &#x60;GET /bookings/today&#x60;. Whether a room still needs assigning (&#x60;booking.roomUnitId&#x60;) or cleaning (&#x60;booking.roomUnit.housekeepingStatus&#x60;) is already on the embedded &#x60;Booking&#x60; - this only adds what isn&#39;t: the folio balance, which needs its own payment lookup per booking. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TodayBoardEntry {

  private Booking booking;

  private String outstandingBalance;

  public TodayBoardEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TodayBoardEntry(Booking booking, String outstandingBalance) {
    this.booking = booking;
    this.outstandingBalance = outstandingBalance;
  }

  public TodayBoardEntry booking(Booking booking) {
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

  public TodayBoardEntry outstandingBalance(String outstandingBalance) {
    this.outstandingBalance = outstandingBalance;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string. Same meaning as `CheckOutResult.outstandingBalance`.
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
    TodayBoardEntry todayBoardEntry = (TodayBoardEntry) o;
    return Objects.equals(this.booking, todayBoardEntry.booking) &&
        Objects.equals(this.outstandingBalance, todayBoardEntry.outstandingBalance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(booking, outstandingBalance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TodayBoardEntry {\n");
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

