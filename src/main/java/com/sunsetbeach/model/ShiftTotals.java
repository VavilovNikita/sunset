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
 * Sum of this shift&#39;s &#x60;Payment.amount&#x60;, broken out by method — CASH/CARD/OTHER only; ROOM_CHARGE payments settle against the room folio, not this shift&#39;s cash/card reconciliation, so they&#39;re reported separately as &#x60;roomCharge&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftTotals {

  private String cash;

  private String card;

  private String roomCharge;

  private String other;

  private Integer paymentCount;

  public ShiftTotals() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftTotals(String cash, String card, String roomCharge, String other, Integer paymentCount) {
    this.cash = cash;
    this.card = card;
    this.roomCharge = roomCharge;
    this.other = other;
    this.paymentCount = paymentCount;
  }

  public ShiftTotals cash(String cash) {
    this.cash = cash;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return cash
   */
  @NotNull 
  @JsonProperty("cash")
  public String getCash() {
    return cash;
  }

  public void setCash(String cash) {
    this.cash = cash;
  }

  public ShiftTotals card(String card) {
    this.card = card;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return card
   */
  @NotNull 
  @JsonProperty("card")
  public String getCard() {
    return card;
  }

  public void setCard(String card) {
    this.card = card;
  }

  public ShiftTotals roomCharge(String roomCharge) {
    this.roomCharge = roomCharge;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return roomCharge
   */
  @NotNull 
  @JsonProperty("roomCharge")
  public String getRoomCharge() {
    return roomCharge;
  }

  public void setRoomCharge(String roomCharge) {
    this.roomCharge = roomCharge;
  }

  public ShiftTotals other(String other) {
    this.other = other;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return other
   */
  @NotNull 
  @JsonProperty("other")
  public String getOther() {
    return other;
  }

  public void setOther(String other) {
    this.other = other;
  }

  public ShiftTotals paymentCount(Integer paymentCount) {
    this.paymentCount = paymentCount;
    return this;
  }

  /**
   * Get paymentCount
   * @return paymentCount
   */
  @NotNull 
  @JsonProperty("paymentCount")
  public Integer getPaymentCount() {
    return paymentCount;
  }

  public void setPaymentCount(Integer paymentCount) {
    this.paymentCount = paymentCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftTotals shiftTotals = (ShiftTotals) o;
    return Objects.equals(this.cash, shiftTotals.cash) &&
        Objects.equals(this.card, shiftTotals.card) &&
        Objects.equals(this.roomCharge, shiftTotals.roomCharge) &&
        Objects.equals(this.other, shiftTotals.other) &&
        Objects.equals(this.paymentCount, shiftTotals.paymentCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cash, card, roomCharge, other, paymentCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShiftTotals {\n");
    sb.append("    cash: ").append(toIndentedString(cash)).append("\n");
    sb.append("    card: ").append(toIndentedString(card)).append("\n");
    sb.append("    roomCharge: ").append(toIndentedString(roomCharge)).append("\n");
    sb.append("    other: ").append(toIndentedString(other)).append("\n");
    sb.append("    paymentCount: ").append(toIndentedString(paymentCount)).append("\n");
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

