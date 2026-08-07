package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.OrderItem;
import com.sunsetbeach.model.OrderStatus;
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
 * &#x60;total&#x60; is always server-computed from &#x60;items&#x60; — never taken from the client (same rule as &#x60;Booking.totalPrice&#x60;). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T15:38:53.433655500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class Order {

  private String id;

  private JsonNullable<String> tableId = JsonNullable.<String>undefined();

  private JsonNullable<String> bookingId = JsonNullable.<String>undefined();

  private JsonNullable<String> guestName = JsonNullable.<String>undefined();

  private OrderStatus status;

  private String openedByUserId;

  private String total;

  private JsonNullable<String> note = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid OrderItem> items = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public Order() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Order(String id, String tableId, String bookingId, String guestName, OrderStatus status, String openedByUserId, String total, String note, List<@Valid OrderItem> items, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.tableId = JsonNullable.of(tableId);
    this.bookingId = JsonNullable.of(bookingId);
    this.guestName = JsonNullable.of(guestName);
    this.status = status;
    this.openedByUserId = openedByUserId;
    this.total = total;
    this.note = JsonNullable.of(note);
    this.items = items;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Order id(String id) {
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

  public Order tableId(String tableId) {
    this.tableId = JsonNullable.of(tableId);
    return this;
  }

  /**
   * Get tableId
   * @return tableId
   */
  @NotNull 
  @JsonProperty("tableId")
  public JsonNullable<String> getTableId() {
    return tableId;
  }

  public void setTableId(JsonNullable<String> tableId) {
    this.tableId = tableId;
  }

  public Order bookingId(String bookingId) {
    this.bookingId = JsonNullable.of(bookingId);
    return this;
  }

  /**
   * Get bookingId
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

  public Order guestName(String guestName) {
    this.guestName = JsonNullable.of(guestName);
    return this;
  }

  /**
   * Get guestName
   * @return guestName
   */
  @NotNull 
  @JsonProperty("guestName")
  public JsonNullable<String> getGuestName() {
    return guestName;
  }

  public void setGuestName(JsonNullable<String> guestName) {
    this.guestName = guestName;
  }

  public Order status(OrderStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public Order openedByUserId(String openedByUserId) {
    this.openedByUserId = openedByUserId;
    return this;
  }

  /**
   * Get openedByUserId
   * @return openedByUserId
   */
  @NotNull 
  @JsonProperty("openedByUserId")
  public String getOpenedByUserId() {
    return openedByUserId;
  }

  public void setOpenedByUserId(String openedByUserId) {
    this.openedByUserId = openedByUserId;
  }

  public Order total(String total) {
    this.total = total;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string, e.g. `\"1250.00\"`.
   * @return total
   */
  @NotNull 
  @JsonProperty("total")
  public String getTotal() {
    return total;
  }

  public void setTotal(String total) {
    this.total = total;
  }

  public Order note(String note) {
    this.note = JsonNullable.of(note);
    return this;
  }

  /**
   * Get note
   * @return note
   */
  @NotNull 
  @JsonProperty("note")
  public JsonNullable<String> getNote() {
    return note;
  }

  public void setNote(JsonNullable<String> note) {
    this.note = note;
  }

  public Order items(List<@Valid OrderItem> items) {
    this.items = items;
    return this;
  }

  public Order addItemsItem(OrderItem itemsItem) {
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
  public List<@Valid OrderItem> getItems() {
    return items;
  }

  public void setItems(List<@Valid OrderItem> items) {
    this.items = items;
  }

  public Order createdAt(OffsetDateTime createdAt) {
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

  public Order updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Order order = (Order) o;
    return Objects.equals(this.id, order.id) &&
        Objects.equals(this.tableId, order.tableId) &&
        Objects.equals(this.bookingId, order.bookingId) &&
        Objects.equals(this.guestName, order.guestName) &&
        Objects.equals(this.status, order.status) &&
        Objects.equals(this.openedByUserId, order.openedByUserId) &&
        Objects.equals(this.total, order.total) &&
        Objects.equals(this.note, order.note) &&
        Objects.equals(this.items, order.items) &&
        Objects.equals(this.createdAt, order.createdAt) &&
        Objects.equals(this.updatedAt, order.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, tableId, bookingId, guestName, status, openedByUserId, total, note, items, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Order {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    openedByUserId: ").append(toIndentedString(openedByUserId)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

