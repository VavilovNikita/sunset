package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.BookingPosOrderItem;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One &#x60;Order&#x60; that was closed with &#x60;method&#x3D;ROOM_CHARGE&#x60; against this booking.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T16:23:36.900320800+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class BookingPosOrder {

  private String orderId;

  private String amount;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime paidAt;

  @Valid
  private List<@Valid BookingPosOrderItem> items = new ArrayList<>();

  public BookingPosOrder() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingPosOrder(String orderId, String amount, OffsetDateTime paidAt, List<@Valid BookingPosOrderItem> items) {
    this.orderId = orderId;
    this.amount = amount;
    this.paidAt = paidAt;
    this.items = items;
  }

  public BookingPosOrder orderId(String orderId) {
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

  public BookingPosOrder amount(String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * The `Payment.amount` recorded when this order was closed - decimal(10,2) rendered as a string.
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

  public BookingPosOrder paidAt(OffsetDateTime paidAt) {
    this.paidAt = paidAt;
    return this;
  }

  /**
   * `Payment.createdAt` - when the order was closed, not when the order was opened.
   * @return paidAt
   */
  @NotNull @Valid 
  @JsonProperty("paidAt")
  public OffsetDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(OffsetDateTime paidAt) {
    this.paidAt = paidAt;
  }

  public BookingPosOrder items(List<@Valid BookingPosOrderItem> items) {
    this.items = items;
    return this;
  }

  public BookingPosOrder addItemsItem(BookingPosOrderItem itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
   */
  @NotNull @Valid 
  @JsonProperty("items")
  public List<@Valid BookingPosOrderItem> getItems() {
    return items;
  }

  public void setItems(List<@Valid BookingPosOrderItem> items) {
    this.items = items;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingPosOrder bookingPosOrder = (BookingPosOrder) o;
    return Objects.equals(this.orderId, bookingPosOrder.orderId) &&
        Objects.equals(this.amount, bookingPosOrder.amount) &&
        Objects.equals(this.paidAt, bookingPosOrder.paidAt) &&
        Objects.equals(this.items, bookingPosOrder.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(orderId, amount, paidAt, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingPosOrder {\n");
    sb.append("    orderId: ").append(toIndentedString(orderId)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    paidAt: ").append(toIndentedString(paidAt)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

