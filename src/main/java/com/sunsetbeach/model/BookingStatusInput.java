package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.BookingStatus;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;bookingStatusSchema&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-17T16:01:20.967720600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class BookingStatusInput {

  private BookingStatus status;

  private JsonNullable<@Size(max = 500) String> paymentNote = JsonNullable.<String>undefined();

  public BookingStatusInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingStatusInput(BookingStatus status) {
    this.status = status;
  }

  public BookingStatusInput status(BookingStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public BookingStatus getStatus() {
    return status;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }

  public BookingStatusInput paymentNote(String paymentNote) {
    this.paymentNote = JsonNullable.of(paymentNote);
    return this;
  }

  /**
   * Get paymentNote
   * @return paymentNote
   */
  @Size(max = 500) 
  @JsonProperty("paymentNote")
  public JsonNullable<@Size(max = 500) String> getPaymentNote() {
    return paymentNote;
  }

  public void setPaymentNote(JsonNullable<String> paymentNote) {
    this.paymentNote = paymentNote;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingStatusInput bookingStatusInput = (BookingStatusInput) o;
    return Objects.equals(this.status, bookingStatusInput.status) &&
        equalsNullable(this.paymentNote, bookingStatusInput.paymentNote);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, hashCodeNullable(paymentNote));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingStatusInput {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    paymentNote: ").append(toIndentedString(paymentNote)).append("\n");
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

