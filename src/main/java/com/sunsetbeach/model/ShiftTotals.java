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
 * Sum of this shift&#39;s &#x60;Payment.amount&#x60;, broken out by method — CASH/CARD/OTHER only; ROOM_CHARGE payments settle against the room folio, not this shift&#39;s cash/card reconciliation, so they&#39;re reported separately as &#x60;roomCharge&#x60;. &#x60;folioCash&#x60;/&#x60;folioCard&#x60;/ &#x60;folioOther&#x60; (shift reads only, absent from &#x60;GET /payments/summary&#x60;) are the &#x60;FolioPayment&#x60;s recorded during this shift (their &#x60;shiftId&#x60;) - money taken at reception against a booking, kept apart from POS sales. Expected cash is &#x60;openingCashFloat&#x60; + &#x60;cash&#x60; + &#x60;folioCash&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftTotals {

  private String cash;

  private String card;

  private String roomCharge;

  private String other;

  private Integer paymentCount;

  private String folioCash;

  private String folioCard;

  private String folioOther;

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

  public ShiftTotals folioCash(String folioCash) {
    this.folioCash = folioCash;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return folioCash
   */
  
  @JsonProperty("folioCash")
  public String getFolioCash() {
    return folioCash;
  }

  public void setFolioCash(String folioCash) {
    this.folioCash = folioCash;
  }

  public ShiftTotals folioCard(String folioCard) {
    this.folioCard = folioCard;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return folioCard
   */
  
  @JsonProperty("folioCard")
  public String getFolioCard() {
    return folioCard;
  }

  public void setFolioCard(String folioCard) {
    this.folioCard = folioCard;
  }

  public ShiftTotals folioOther(String folioOther) {
    this.folioOther = folioOther;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return folioOther
   */
  
  @JsonProperty("folioOther")
  public String getFolioOther() {
    return folioOther;
  }

  public void setFolioOther(String folioOther) {
    this.folioOther = folioOther;
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
        Objects.equals(this.paymentCount, shiftTotals.paymentCount) &&
        Objects.equals(this.folioCash, shiftTotals.folioCash) &&
        Objects.equals(this.folioCard, shiftTotals.folioCard) &&
        Objects.equals(this.folioOther, shiftTotals.folioOther);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cash, card, roomCharge, other, paymentCount, folioCash, folioCard, folioOther);
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
    sb.append("    folioCash: ").append(toIndentedString(folioCash)).append("\n");
    sb.append("    folioCard: ").append(toIndentedString(folioCard)).append("\n");
    sb.append("    folioOther: ").append(toIndentedString(folioOther)).append("\n");
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

