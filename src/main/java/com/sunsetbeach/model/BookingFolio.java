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
 * What a booking has been charged, what has been collected, and what is still owed - computed on the fly by &#x60;BookingService&#x60; (&#x60;computeFolioBreakdown&#x60;), never stored. The one place \&quot;how much does this guest owe\&quot; is answered; &#x60;outstandingBalance&#x60; everywhere else is &#x60;balanceDue&#x60; here.  Charged: &#x60;roomTotal&#x60; (the nights stayed) + &#x60;earlyDepartureFee&#x60; + &#x60;roomChargesGross&#x60; (POS orders charged to the room) &#x3D; &#x60;folioTotal&#x60;. Collected: &#x60;paidTotal&#x60; (every &#x60;FolioPayment&#x60;), applied to the POS charges first and then to the room, plus &#x60;settledOutside&#x60; - the room amount not covered by folio payments on a booking marked &#x60;PAID&#x60; by hand (prepaid on an OTA, bank transfer: collected, just not through this system). &#x60;balanceDue&#x60; &#x3D; &#x60;folioTotal&#x60; - &#x60;paidTotal&#x60; - &#x60;settledOutside&#x60;, never below zero; &#x60;creditBalance&#x60; is what was collected beyond &#x60;folioTotal&#x60; (a stay shortened after it was paid), which this system has no refund record for. A &#x60;CANCELLED&#x60; booking owes nothing for the room - &#x60;roomTotal&#x60; and &#x60;earlyDepartureFee&#x60; still show what it was priced at, but neither counts in &#x60;folioTotal&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingFolio {

  private String roomTotal;

  private String earlyDepartureFee;

  private String roomChargesGross;

  private String roomChargesTotal;

  private String folioTotal;

  private String paidTotal;

  private String settledOutside;

  private String balanceDue;

  private String creditBalance;

  private Integer roomChargeCount;

  public BookingFolio() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingFolio(String roomTotal, String earlyDepartureFee, String roomChargesGross, String roomChargesTotal, String folioTotal, String paidTotal, String settledOutside, String balanceDue, String creditBalance, Integer roomChargeCount) {
    this.roomTotal = roomTotal;
    this.earlyDepartureFee = earlyDepartureFee;
    this.roomChargesGross = roomChargesGross;
    this.roomChargesTotal = roomChargesTotal;
    this.folioTotal = folioTotal;
    this.paidTotal = paidTotal;
    this.settledOutside = settledOutside;
    this.balanceDue = balanceDue;
    this.creditBalance = creditBalance;
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

  public BookingFolio earlyDepartureFee(String earlyDepartureFee) {
    this.earlyDepartureFee = earlyDepartureFee;
    return this;
  }

  /**
   * `Booking.earlyDepartureFee` - decimal(10,2) rendered as a string.
   * @return earlyDepartureFee
   */
  @NotNull 
  @JsonProperty("earlyDepartureFee")
  public String getEarlyDepartureFee() {
    return earlyDepartureFee;
  }

  public void setEarlyDepartureFee(String earlyDepartureFee) {
    this.earlyDepartureFee = earlyDepartureFee;
  }

  public BookingFolio roomChargesGross(String roomChargesGross) {
    this.roomChargesGross = roomChargesGross;
    return this;
  }

  /**
   * Sum of this booking's ROOM_CHARGE `Payment.amount` rows - decimal(10,2) rendered as a string. 
   * @return roomChargesGross
   */
  @NotNull 
  @JsonProperty("roomChargesGross")
  public String getRoomChargesGross() {
    return roomChargesGross;
  }

  public void setRoomChargesGross(String roomChargesGross) {
    this.roomChargesGross = roomChargesGross;
  }

  public BookingFolio roomChargesTotal(String roomChargesTotal) {
    this.roomChargesTotal = roomChargesTotal;
    return this;
  }

  /**
   * The POS room charges still uncollected: `roomChargesGross` minus the folio payments applied to them (payments go to POS charges first) - decimal(10,2) rendered as a string. What the \"owes for POS charges\" hints on a `PAID` booking read. 
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
   * Everything charged (see the schema description) - decimal(10,2) rendered as a string.
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

  public BookingFolio paidTotal(String paidTotal) {
    this.paidTotal = paidTotal;
    return this;
  }

  /**
   * Sum of every `FolioPayment` - decimal(10,2) rendered as a string.
   * @return paidTotal
   */
  @NotNull 
  @JsonProperty("paidTotal")
  public String getPaidTotal() {
    return paidTotal;
  }

  public void setPaidTotal(String paidTotal) {
    this.paidTotal = paidTotal;
  }

  public BookingFolio settledOutside(String settledOutside) {
    this.settledOutside = settledOutside;
    return this;
  }

  /**
   * See the schema description - decimal(10,2) rendered as a string.
   * @return settledOutside
   */
  @NotNull 
  @JsonProperty("settledOutside")
  public String getSettledOutside() {
    return settledOutside;
  }

  public void setSettledOutside(String settledOutside) {
    this.settledOutside = settledOutside;
  }

  public BookingFolio balanceDue(String balanceDue) {
    this.balanceDue = balanceDue;
    return this;
  }

  /**
   * What is still to collect - decimal(10,2) rendered as a string.
   * @return balanceDue
   */
  @NotNull 
  @JsonProperty("balanceDue")
  public String getBalanceDue() {
    return balanceDue;
  }

  public void setBalanceDue(String balanceDue) {
    this.balanceDue = balanceDue;
  }

  public BookingFolio creditBalance(String creditBalance) {
    this.creditBalance = creditBalance;
    return this;
  }

  /**
   * Collected beyond what was charged - decimal(10,2) rendered as a string.
   * @return creditBalance
   */
  @NotNull 
  @JsonProperty("creditBalance")
  public String getCreditBalance() {
    return creditBalance;
  }

  public void setCreditBalance(String creditBalance) {
    this.creditBalance = creditBalance;
  }

  public BookingFolio roomChargeCount(Integer roomChargeCount) {
    this.roomChargeCount = roomChargeCount;
    return this;
  }

  /**
   * Number of ROOM_CHARGE payments this stay has ever generated (a raw historical count, not \"how many are still unsettled\") - same count as `GET /bookings/{id}/pos-orders` would return entries. 
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
        Objects.equals(this.earlyDepartureFee, bookingFolio.earlyDepartureFee) &&
        Objects.equals(this.roomChargesGross, bookingFolio.roomChargesGross) &&
        Objects.equals(this.roomChargesTotal, bookingFolio.roomChargesTotal) &&
        Objects.equals(this.folioTotal, bookingFolio.folioTotal) &&
        Objects.equals(this.paidTotal, bookingFolio.paidTotal) &&
        Objects.equals(this.settledOutside, bookingFolio.settledOutside) &&
        Objects.equals(this.balanceDue, bookingFolio.balanceDue) &&
        Objects.equals(this.creditBalance, bookingFolio.creditBalance) &&
        Objects.equals(this.roomChargeCount, bookingFolio.roomChargeCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomTotal, earlyDepartureFee, roomChargesGross, roomChargesTotal, folioTotal, paidTotal, settledOutside, balanceDue, creditBalance, roomChargeCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingFolio {\n");
    sb.append("    roomTotal: ").append(toIndentedString(roomTotal)).append("\n");
    sb.append("    earlyDepartureFee: ").append(toIndentedString(earlyDepartureFee)).append("\n");
    sb.append("    roomChargesGross: ").append(toIndentedString(roomChargesGross)).append("\n");
    sb.append("    roomChargesTotal: ").append(toIndentedString(roomChargesTotal)).append("\n");
    sb.append("    folioTotal: ").append(toIndentedString(folioTotal)).append("\n");
    sb.append("    paidTotal: ").append(toIndentedString(paidTotal)).append("\n");
    sb.append("    settledOutside: ").append(toIndentedString(settledOutside)).append("\n");
    sb.append("    balanceDue: ").append(toIndentedString(balanceDue)).append("\n");
    sb.append("    creditBalance: ").append(toIndentedString(creditBalance)).append("\n");
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

