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
 * Optional body of &#x60;POST /bookings/{id}/check-out&#x60;. Only meaningful for an early checkout (see &#x60;CheckOutPreview.early&#x60;); ignored otherwise. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class CheckOutInput {

  private Boolean shortenStay = false;

  private Boolean chargeUnusedNights = false;

  public CheckOutInput shortenStay(Boolean shortenStay) {
    this.shortenStay = shortenStay;
    return this;
  }

  /**
   * Move `checkOut` to `CheckOutPreview.shortenedCheckOut` through the ordinary schedule change (`PATCH /bookings/{id}/schedule`'s own path), so the released nights are free again on the calendar, in availability, the night audit and every report. Their frozen rates are removed with them. 
   * @return shortenStay
   */
  
  @JsonProperty("shortenStay")
  public Boolean getShortenStay() {
    return shortenStay;
  }

  public void setShortenStay(Boolean shortenStay) {
    this.shortenStay = shortenStay;
  }

  public CheckOutInput chargeUnusedNights(Boolean chargeUnusedNights) {
    this.chargeUnusedNights = chargeUnusedNights;
    return this;
  }

  /**
   * With `shortenStay`: keep charging what the released nights were agreed at, as `Booking.earlyDepartureFee` (added to any fee already there). Without it the guest is charged only for the nights stayed. Staff decide this per checkout. 
   * @return chargeUnusedNights
   */
  
  @JsonProperty("chargeUnusedNights")
  public Boolean getChargeUnusedNights() {
    return chargeUnusedNights;
  }

  public void setChargeUnusedNights(Boolean chargeUnusedNights) {
    this.chargeUnusedNights = chargeUnusedNights;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckOutInput checkOutInput = (CheckOutInput) o;
    return Objects.equals(this.shortenStay, checkOutInput.shortenStay) &&
        Objects.equals(this.chargeUnusedNights, checkOutInput.chargeUnusedNights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(shortenStay, chargeUnusedNights);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckOutInput {\n");
    sb.append("    shortenStay: ").append(toIndentedString(shortenStay)).append("\n");
    sb.append("    chargeUnusedNights: ").append(toIndentedString(chargeUnusedNights)).append("\n");
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

