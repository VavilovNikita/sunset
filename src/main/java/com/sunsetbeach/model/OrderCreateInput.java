package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * At least one of &#x60;tableId&#x60;/&#x60;bookingId&#x60;/&#x60;guestName&#x60; is expected in practice (an order with none of them is a valid but untraceable tab), but this isn&#39;t enforced server-side — same \&quot;trust the caller on shape, not on price\&quot; spirit as elsewhere.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T15:38:53.433655500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class OrderCreateInput {

  private JsonNullable<String> tableId = JsonNullable.<String>undefined();

  private JsonNullable<String> bookingId = JsonNullable.<String>undefined();

  private JsonNullable<@Size(max = 120) String> guestName = JsonNullable.<String>undefined();

  public OrderCreateInput tableId(String tableId) {
    this.tableId = JsonNullable.of(tableId);
    return this;
  }

  /**
   * Get tableId
   * @return tableId
   */
  
  @JsonProperty("tableId")
  public JsonNullable<String> getTableId() {
    return tableId;
  }

  public void setTableId(JsonNullable<String> tableId) {
    this.tableId = tableId;
  }

  public OrderCreateInput bookingId(String bookingId) {
    this.bookingId = JsonNullable.of(bookingId);
    return this;
  }

  /**
   * Get bookingId
   * @return bookingId
   */
  
  @JsonProperty("bookingId")
  public JsonNullable<String> getBookingId() {
    return bookingId;
  }

  public void setBookingId(JsonNullable<String> bookingId) {
    this.bookingId = bookingId;
  }

  public OrderCreateInput guestName(String guestName) {
    this.guestName = JsonNullable.of(guestName);
    return this;
  }

  /**
   * Get guestName
   * @return guestName
   */
  @Size(max = 120) 
  @JsonProperty("guestName")
  public JsonNullable<@Size(max = 120) String> getGuestName() {
    return guestName;
  }

  public void setGuestName(JsonNullable<String> guestName) {
    this.guestName = guestName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderCreateInput orderCreateInput = (OrderCreateInput) o;
    return equalsNullable(this.tableId, orderCreateInput.tableId) &&
        equalsNullable(this.bookingId, orderCreateInput.bookingId) &&
        equalsNullable(this.guestName, orderCreateInput.guestName);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(tableId), hashCodeNullable(bookingId), hashCodeNullable(guestName));
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
    sb.append("class OrderCreateInput {\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
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

