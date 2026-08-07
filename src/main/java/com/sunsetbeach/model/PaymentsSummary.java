package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.ShiftTotals;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;GET /payments/summary&#x60;. See that operation&#39;s description for why &#x60;totals.roomCharge&#x60; is excluded from &#x60;grandTotal&#x60; - do not sum &#x60;totals.*&#x60; yourself on the client, use &#x60;grandTotal&#x60; as-is for received revenue. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T20:13:52.916230300+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class PaymentsSummary {

  private String from;

  private String to;

  private ShiftTotals totals;

  private String grandTotal;

  public PaymentsSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PaymentsSummary(String from, String to, ShiftTotals totals, String grandTotal) {
    this.from = from;
    this.to = to;
    this.totals = totals;
    this.grandTotal = grandTotal;
  }

  public PaymentsSummary from(String from) {
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

  public PaymentsSummary to(String to) {
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

  public PaymentsSummary totals(ShiftTotals totals) {
    this.totals = totals;
    return this;
  }

  /**
   * Get totals
   * @return totals
   */
  @NotNull @Valid 
  @JsonProperty("totals")
  public ShiftTotals getTotals() {
    return totals;
  }

  public void setTotals(ShiftTotals totals) {
    this.totals = totals;
  }

  public PaymentsSummary grandTotal(String grandTotal) {
    this.grandTotal = grandTotal;
    return this;
  }

  /**
   * `totals.cash + totals.card + totals.other` - decimal(10,2) rendered as a string. Deliberately excludes `totals.roomCharge`.
   * @return grandTotal
   */
  @NotNull 
  @JsonProperty("grandTotal")
  public String getGrandTotal() {
    return grandTotal;
  }

  public void setGrandTotal(String grandTotal) {
    this.grandTotal = grandTotal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PaymentsSummary paymentsSummary = (PaymentsSummary) o;
    return Objects.equals(this.from, paymentsSummary.from) &&
        Objects.equals(this.to, paymentsSummary.to) &&
        Objects.equals(this.totals, paymentsSummary.totals) &&
        Objects.equals(this.grandTotal, paymentsSummary.grandTotal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, totals, grandTotal);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaymentsSummary {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    totals: ").append(toIndentedString(totals)).append("\n");
    sb.append("    grandTotal: ").append(toIndentedString(grandTotal)).append("\n");
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

