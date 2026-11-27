package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;GET /bookings/{id}/check-out/preview&#x60;. &#x60;early&#x60; is true when today (hotel date) is before &#x60;checkOut&#x60;. Shortening ends the stay today, but never before the first night: a guest leaving on their arrival day is still charged one night, so a one-night booking checked out on its arrival day is not early at all. &#x60;shortenable&#x60; is false (with &#x60;reason&#x60;) when the stay can&#39;t be shortened through a schedule change - most often a relocated stay whose last room starts after today; release those nights with the relocation tools first. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class CheckOutPreview {

  private Boolean early;

  private Boolean shortenable;

  private JsonNullable<String> reason = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private JsonNullable<LocalDate> shortenedCheckOut = JsonNullable.<LocalDate>undefined();

  private Integer nightsReleased;

  private String unusedNightsAmount;

  private String currentRoomTotal;

  private String shortenedRoomTotal;

  private String outstandingBalance;

  private String balanceDueNow;

  private Boolean tonightNotCharged;

  private String shortenedOutstandingBalance;

  public CheckOutPreview() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CheckOutPreview(Boolean early, Boolean shortenable, String reason, LocalDate shortenedCheckOut, Integer nightsReleased, String unusedNightsAmount, String currentRoomTotal, String shortenedRoomTotal, String outstandingBalance, String balanceDueNow, Boolean tonightNotCharged, String shortenedOutstandingBalance) {
    this.early = early;
    this.shortenable = shortenable;
    this.reason = JsonNullable.of(reason);
    this.shortenedCheckOut = JsonNullable.of(shortenedCheckOut);
    this.nightsReleased = nightsReleased;
    this.unusedNightsAmount = unusedNightsAmount;
    this.currentRoomTotal = currentRoomTotal;
    this.shortenedRoomTotal = shortenedRoomTotal;
    this.outstandingBalance = outstandingBalance;
    this.balanceDueNow = balanceDueNow;
    this.tonightNotCharged = tonightNotCharged;
    this.shortenedOutstandingBalance = shortenedOutstandingBalance;
  }

  public CheckOutPreview early(Boolean early) {
    this.early = early;
    return this;
  }

  /**
   * Get early
   * @return early
   */
  @NotNull 
  @JsonProperty("early")
  public Boolean getEarly() {
    return early;
  }

  public void setEarly(Boolean early) {
    this.early = early;
  }

  public CheckOutPreview shortenable(Boolean shortenable) {
    this.shortenable = shortenable;
    return this;
  }

  /**
   * Get shortenable
   * @return shortenable
   */
  @NotNull 
  @JsonProperty("shortenable")
  public Boolean getShortenable() {
    return shortenable;
  }

  public void setShortenable(Boolean shortenable) {
    this.shortenable = shortenable;
  }

  public CheckOutPreview reason(String reason) {
    this.reason = JsonNullable.of(reason);
    return this;
  }

  /**
   * Get reason
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

  public CheckOutPreview shortenedCheckOut(LocalDate shortenedCheckOut) {
    this.shortenedCheckOut = JsonNullable.of(shortenedCheckOut);
    return this;
  }

  /**
   * The `checkOut` the stay would get. Null unless `early`.
   * @return shortenedCheckOut
   */
  @NotNull @Valid 
  @JsonProperty("shortenedCheckOut")
  public JsonNullable<LocalDate> getShortenedCheckOut() {
    return shortenedCheckOut;
  }

  public void setShortenedCheckOut(JsonNullable<LocalDate> shortenedCheckOut) {
    this.shortenedCheckOut = shortenedCheckOut;
  }

  public CheckOutPreview nightsReleased(Integer nightsReleased) {
    this.nightsReleased = nightsReleased;
    return this;
  }

  /**
   * Get nightsReleased
   * @return nightsReleased
   */
  @NotNull 
  @JsonProperty("nightsReleased")
  public Integer getNightsReleased() {
    return nightsReleased;
  }

  public void setNightsReleased(Integer nightsReleased) {
    this.nightsReleased = nightsReleased;
  }

  public CheckOutPreview unusedNightsAmount(String unusedNightsAmount) {
    this.unusedNightsAmount = unusedNightsAmount;
    return this;
  }

  /**
   * Decimal(10,2) as a string - what the released nights were agreed at, i.e. the early-departure fee `chargeUnusedNights` would add. `\"0.00\"` unless `shortenable`. 
   * @return unusedNightsAmount
   */
  @NotNull 
  @JsonProperty("unusedNightsAmount")
  public String getUnusedNightsAmount() {
    return unusedNightsAmount;
  }

  public void setUnusedNightsAmount(String unusedNightsAmount) {
    this.unusedNightsAmount = unusedNightsAmount;
  }

  public CheckOutPreview currentRoomTotal(String currentRoomTotal) {
    this.currentRoomTotal = currentRoomTotal;
    return this;
  }

  /**
   * Decimal(10,2) as a string - `Booking.totalPrice` + `earlyDepartureFee` now.
   * @return currentRoomTotal
   */
  @NotNull 
  @JsonProperty("currentRoomTotal")
  public String getCurrentRoomTotal() {
    return currentRoomTotal;
  }

  public void setCurrentRoomTotal(String currentRoomTotal) {
    this.currentRoomTotal = currentRoomTotal;
  }

  public CheckOutPreview shortenedRoomTotal(String shortenedRoomTotal) {
    this.shortenedRoomTotal = shortenedRoomTotal;
    return this;
  }

  /**
   * Decimal(10,2) as a string - the room total after shortening without charging the unused nights. Equals `currentRoomTotal` unless `shortenable`. 
   * @return shortenedRoomTotal
   */
  @NotNull 
  @JsonProperty("shortenedRoomTotal")
  public String getShortenedRoomTotal() {
    return shortenedRoomTotal;
  }

  public void setShortenedRoomTotal(String shortenedRoomTotal) {
    this.shortenedRoomTotal = shortenedRoomTotal;
  }

  public CheckOutPreview outstandingBalance(String outstandingBalance) {
    this.outstandingBalance = outstandingBalance;
    return this;
  }

  /**
   * Decimal(10,2) as a string - what the guest would owe right after checking out now without shortening the stay: `balanceDueNow` less tonight's overstay night, which a check-out does not charge. Equals `CheckOutResult.outstandingBalance` of a check-out without `shortenStay`. 
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

  public CheckOutPreview balanceDueNow(String balanceDueNow) {
    this.balanceDueNow = balanceDueNow;
    return this;
  }

  /**
   * Decimal(10,2) as a string - `BookingFolio.balanceDue` (`Booking.balanceDue`) at this moment, tonight's overstay night included when it has been charged. The figure the Today board shows as owed. 
   * @return balanceDueNow
   */
  @NotNull 
  @JsonProperty("balanceDueNow")
  public String getBalanceDueNow() {
    return balanceDueNow;
  }

  public void setBalanceDueNow(String balanceDueNow) {
    this.balanceDueNow = balanceDueNow;
  }

  public CheckOutPreview tonightNotCharged(Boolean tonightNotCharged) {
    this.tonightNotCharged = tonightNotCharged;
    return this;
  }

  /**
   * True when tonight's overstay night is in `balanceDueNow` and checking out today releases it - the reason the two figures differ, and the only case a screen has to show both. 
   * @return tonightNotCharged
   */
  @NotNull 
  @JsonProperty("tonightNotCharged")
  public Boolean getTonightNotCharged() {
    return tonightNotCharged;
  }

  public void setTonightNotCharged(Boolean tonightNotCharged) {
    this.tonightNotCharged = tonightNotCharged;
  }

  public CheckOutPreview shortenedOutstandingBalance(String shortenedOutstandingBalance) {
    this.shortenedOutstandingBalance = shortenedOutstandingBalance;
    return this;
  }

  /**
   * Decimal(10,2) as a string - `BookingFolio.balanceDue` as it would be after shortening without charging the unused nights (folio payments, POS charges and `PAID` applied the same way). Charging them keeps the room total, so that choice's balance is `outstandingBalance`. Equals `outstandingBalance` unless `shortenable`. 
   * @return shortenedOutstandingBalance
   */
  @NotNull 
  @JsonProperty("shortenedOutstandingBalance")
  public String getShortenedOutstandingBalance() {
    return shortenedOutstandingBalance;
  }

  public void setShortenedOutstandingBalance(String shortenedOutstandingBalance) {
    this.shortenedOutstandingBalance = shortenedOutstandingBalance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckOutPreview checkOutPreview = (CheckOutPreview) o;
    return Objects.equals(this.early, checkOutPreview.early) &&
        Objects.equals(this.shortenable, checkOutPreview.shortenable) &&
        Objects.equals(this.reason, checkOutPreview.reason) &&
        Objects.equals(this.shortenedCheckOut, checkOutPreview.shortenedCheckOut) &&
        Objects.equals(this.nightsReleased, checkOutPreview.nightsReleased) &&
        Objects.equals(this.unusedNightsAmount, checkOutPreview.unusedNightsAmount) &&
        Objects.equals(this.currentRoomTotal, checkOutPreview.currentRoomTotal) &&
        Objects.equals(this.shortenedRoomTotal, checkOutPreview.shortenedRoomTotal) &&
        Objects.equals(this.outstandingBalance, checkOutPreview.outstandingBalance) &&
        Objects.equals(this.balanceDueNow, checkOutPreview.balanceDueNow) &&
        Objects.equals(this.tonightNotCharged, checkOutPreview.tonightNotCharged) &&
        Objects.equals(this.shortenedOutstandingBalance, checkOutPreview.shortenedOutstandingBalance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(early, shortenable, reason, shortenedCheckOut, nightsReleased, unusedNightsAmount, currentRoomTotal, shortenedRoomTotal, outstandingBalance, balanceDueNow, tonightNotCharged, shortenedOutstandingBalance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckOutPreview {\n");
    sb.append("    early: ").append(toIndentedString(early)).append("\n");
    sb.append("    shortenable: ").append(toIndentedString(shortenable)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    shortenedCheckOut: ").append(toIndentedString(shortenedCheckOut)).append("\n");
    sb.append("    nightsReleased: ").append(toIndentedString(nightsReleased)).append("\n");
    sb.append("    unusedNightsAmount: ").append(toIndentedString(unusedNightsAmount)).append("\n");
    sb.append("    currentRoomTotal: ").append(toIndentedString(currentRoomTotal)).append("\n");
    sb.append("    shortenedRoomTotal: ").append(toIndentedString(shortenedRoomTotal)).append("\n");
    sb.append("    outstandingBalance: ").append(toIndentedString(outstandingBalance)).append("\n");
    sb.append("    balanceDueNow: ").append(toIndentedString(balanceDueNow)).append("\n");
    sb.append("    tonightNotCharged: ").append(toIndentedString(tonightNotCharged)).append("\n");
    sb.append("    shortenedOutstandingBalance: ").append(toIndentedString(shortenedOutstandingBalance)).append("\n");
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

