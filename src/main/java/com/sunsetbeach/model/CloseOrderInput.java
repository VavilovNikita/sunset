package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.PaymentMethod;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;POST /orders/{id}/close&#x60;. No &#x60;amount&#x60; field: there&#39;s no partial-payment model (an &#x60;Order&#x60; goes straight to the terminal &#x60;PAID&#x60; status on the first close, it can&#39;t accumulate multiple payments), so the amount charged is never a client-supplied number - the server always charges &#x60;Order.total&#x60; exactly, the same \&quot;never trust the client on money\&quot; rule as &#x60;Booking.totalPrice&#x60;/&#x60;Order.total&#x60; itself. &#x60;bookingId&#x60; is required when &#x60;method&#x60; is &#x60;ROOM_CHARGE&#x60; — a cross-field rule enforced server-side, not expressible in JSON Schema (same pattern as &#x60;BookingCreateInput&#x60;&#39;s &#x60;checkIn &lt; checkOut&#x60;). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class CloseOrderInput {

  private PaymentMethod method;

  private String bookingId;

  public CloseOrderInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CloseOrderInput(PaymentMethod method) {
    this.method = method;
  }

  public CloseOrderInput method(PaymentMethod method) {
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

  public CloseOrderInput bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * Get bookingId
   * @return bookingId
   */
  
  @JsonProperty("bookingId")
  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CloseOrderInput closeOrderInput = (CloseOrderInput) o;
    return Objects.equals(this.method, closeOrderInput.method) &&
        Objects.equals(this.bookingId, closeOrderInput.bookingId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, bookingId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CloseOrderInput {\n");
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
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

