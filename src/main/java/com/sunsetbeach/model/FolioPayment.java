package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.FolioPaymentMethod;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Money actually collected against a booking&#39;s folio - the record that makes a &#x60;ROOM_CHARGE&#x60; payment&#39;s amount stop counting as owed in &#x60;BookingFolio.roomChargesTotal&#x60;/ &#x60;CheckOutResult.outstandingBalance&#x60;/&#x60;TodayBoardEntry.outstandingBalance&#x60;. Deliberately not tied to a shift - unlike &#x60;Payment&#x60;, cash collected this way is not counted in end-of-shift cash-drawer reconciliation, the same gap &#x60;Booking.status &#x3D; PAID&#x60; already has for the room portion of a stay. Rows accumulate (a guest can pay part now, the rest later) and are never edited or deleted - to correct a mistake, staff record another entry, the same append-only convention the audit log uses. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class FolioPayment {

  private String id;

  private String bookingId;

  private FolioPaymentMethod method;

  private String amount;

  private String recordedByUserId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public FolioPayment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FolioPayment(String id, String bookingId, FolioPaymentMethod method, String amount, String recordedByUserId, OffsetDateTime createdAt) {
    this.id = id;
    this.bookingId = bookingId;
    this.method = method;
    this.amount = amount;
    this.recordedByUserId = recordedByUserId;
    this.createdAt = createdAt;
  }

  public FolioPayment id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public FolioPayment bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * Get bookingId
   * @return bookingId
   */
  @NotNull 
  @JsonProperty("bookingId")
  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public FolioPayment method(FolioPaymentMethod method) {
    this.method = method;
    return this;
  }

  /**
   * Get method
   * @return method
   */
  @NotNull @Valid 
  @JsonProperty("method")
  public FolioPaymentMethod getMethod() {
    return method;
  }

  public void setMethod(FolioPaymentMethod method) {
    this.method = method;
  }

  public FolioPayment amount(String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return amount
   */
  @NotNull 
  @JsonProperty("amount")
  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public FolioPayment recordedByUserId(String recordedByUserId) {
    this.recordedByUserId = recordedByUserId;
    return this;
  }

  /**
   * Get recordedByUserId
   * @return recordedByUserId
   */
  @NotNull 
  @JsonProperty("recordedByUserId")
  public String getRecordedByUserId() {
    return recordedByUserId;
  }

  public void setRecordedByUserId(String recordedByUserId) {
    this.recordedByUserId = recordedByUserId;
  }

  public FolioPayment createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FolioPayment folioPayment = (FolioPayment) o;
    return Objects.equals(this.id, folioPayment.id) &&
        Objects.equals(this.bookingId, folioPayment.bookingId) &&
        Objects.equals(this.method, folioPayment.method) &&
        Objects.equals(this.amount, folioPayment.amount) &&
        Objects.equals(this.recordedByUserId, folioPayment.recordedByUserId) &&
        Objects.equals(this.createdAt, folioPayment.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, bookingId, method, amount, recordedByUserId, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FolioPayment {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    recordedByUserId: ").append(toIndentedString(recordedByUserId)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

