package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPurpose;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingStatusInput {

  private BookingStatus status;

  private JsonNullable<@Size(max = 500) String> paymentNote = JsonNullable.<String>undefined();

  private BookingChannel channel;

  private BookingPurpose purpose;

  private Integer adults;

  private Integer children;

  private JsonNullable<@Size(max = 500) String> cancellationReason = JsonNullable.<String>undefined();

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

  public BookingStatusInput channel(BookingChannel channel) {
    this.channel = channel;
    return this;
  }

  /**
   * Get channel
   * @return channel
   */
  @Valid 
  @JsonProperty("channel")
  public BookingChannel getChannel() {
    return channel;
  }

  public void setChannel(BookingChannel channel) {
    this.channel = channel;
  }

  public BookingStatusInput purpose(BookingPurpose purpose) {
    this.purpose = purpose;
    return this;
  }

  /**
   * Get purpose
   * @return purpose
   */
  @Valid 
  @JsonProperty("purpose")
  public BookingPurpose getPurpose() {
    return purpose;
  }

  public void setPurpose(BookingPurpose purpose) {
    this.purpose = purpose;
  }

  public BookingStatusInput adults(Integer adults) {
    this.adults = adults;
    return this;
  }

  /**
   * Optional. Omitted leaves the adult count as it is; present sets it. Not nullable.
   * minimum: 1
   * @return adults
   */
  @Min(1) 
  @JsonProperty("adults")
  public Integer getAdults() {
    return adults;
  }

  public void setAdults(Integer adults) {
    this.adults = adults;
  }

  public BookingStatusInput children(Integer children) {
    this.children = children;
    return this;
  }

  /**
   * Optional. Omitted leaves the child count as it is; present sets it. Not nullable.
   * minimum: 0
   * @return children
   */
  @Min(0) 
  @JsonProperty("children")
  public Integer getChildren() {
    return children;
  }

  public void setChildren(Integer children) {
    this.children = children;
  }

  public BookingStatusInput cancellationReason(String cancellationReason) {
    this.cancellationReason = JsonNullable.of(cancellationReason);
    return this;
  }

  /**
   * Why the booking is being cancelled. Read only when this request moves the booking into `CANCELLED` (ignored otherwise); stored as `Booking.cancellationReason` and repeated in that status change's audit entry. Optional at the API so system paths (the SiteMinder import, the expiry sweep) keep working; the admin screens require it before they send a cancel. 
   * @return cancellationReason
   */
  @Size(max = 500) 
  @JsonProperty("cancellationReason")
  public JsonNullable<@Size(max = 500) String> getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(JsonNullable<String> cancellationReason) {
    this.cancellationReason = cancellationReason;
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
        equalsNullable(this.paymentNote, bookingStatusInput.paymentNote) &&
        Objects.equals(this.channel, bookingStatusInput.channel) &&
        Objects.equals(this.purpose, bookingStatusInput.purpose) &&
        Objects.equals(this.adults, bookingStatusInput.adults) &&
        Objects.equals(this.children, bookingStatusInput.children) &&
        equalsNullable(this.cancellationReason, bookingStatusInput.cancellationReason);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, hashCodeNullable(paymentNote), channel, purpose, adults, children, hashCodeNullable(cancellationReason));
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
    sb.append("    paymentNote: ").append("[REDACTED]").append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    purpose: ").append(toIndentedString(purpose)).append("\n");
    sb.append("    adults: ").append(toIndentedString(adults)).append("\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
    sb.append("    cancellationReason: ").append(toIndentedString(cancellationReason)).append("\n");
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

