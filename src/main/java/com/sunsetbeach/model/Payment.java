package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.PaymentMethod;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Payment
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class Payment {

  private String id;

  private String orderId;

  private PaymentMethod method;

  private String amount;

  private JsonNullable<String> bookingId = JsonNullable.<String>undefined();

  private String recordedByUserId;

  private String shiftId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public Payment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Payment(String id, String orderId, PaymentMethod method, String amount, String bookingId, String recordedByUserId, String shiftId, OffsetDateTime createdAt) {
    this.id = id;
    this.orderId = orderId;
    this.method = method;
    this.amount = amount;
    this.bookingId = JsonNullable.of(bookingId);
    this.recordedByUserId = recordedByUserId;
    this.shiftId = shiftId;
    this.createdAt = createdAt;
  }

  public Payment id(String id) {
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

  public Payment orderId(String orderId) {
    this.orderId = orderId;
    return this;
  }

  /**
   * Get orderId
   * @return orderId
   */
  @NotNull 
  @JsonProperty("orderId")
  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public Payment method(PaymentMethod method) {
    this.method = method;
    return this;
  }

  /**
   * Get method
   * @return method
   */
  @NotNull @Valid 
  @JsonProperty("method")
  public PaymentMethod getMethod() {
    return method;
  }

  public void setMethod(PaymentMethod method) {
    this.method = method;
  }

  public Payment amount(String amount) {
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

  public Payment bookingId(String bookingId) {
    this.bookingId = JsonNullable.of(bookingId);
    return this;
  }

  /**
   * Set only when `method` is `ROOM_CHARGE`.
   * @return bookingId
   */
  @NotNull 
  @JsonProperty("bookingId")
  public JsonNullable<String> getBookingId() {
    return bookingId;
  }

  public void setBookingId(JsonNullable<String> bookingId) {
    this.bookingId = bookingId;
  }

  public Payment recordedByUserId(String recordedByUserId) {
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

  public Payment shiftId(String shiftId) {
    this.shiftId = shiftId;
    return this;
  }

  /**
   * Get shiftId
   * @return shiftId
   */
  @NotNull 
  @JsonProperty("shiftId")
  public String getShiftId() {
    return shiftId;
  }

  public void setShiftId(String shiftId) {
    this.shiftId = shiftId;
  }

  public Payment createdAt(OffsetDateTime createdAt) {
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
    Payment payment = (Payment) o;
    return Objects.equals(this.id, payment.id) &&
        Objects.equals(this.orderId, payment.orderId) &&
        Objects.equals(this.method, payment.method) &&
        Objects.equals(this.amount, payment.amount) &&
        Objects.equals(this.bookingId, payment.bookingId) &&
        Objects.equals(this.recordedByUserId, payment.recordedByUserId) &&
        Objects.equals(this.shiftId, payment.shiftId) &&
        Objects.equals(this.createdAt, payment.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, orderId, method, amount, bookingId, recordedByUserId, shiftId, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Payment {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    orderId: ").append(toIndentedString(orderId)).append("\n");
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    recordedByUserId: ").append(toIndentedString(recordedByUserId)).append("\n");
    sb.append("    shiftId: ").append(toIndentedString(shiftId)).append("\n");
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

