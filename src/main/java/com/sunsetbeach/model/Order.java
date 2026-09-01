package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.OrderItem;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
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
 * &#x60;total&#x60; is always server-computed from &#x60;items&#x60; — never taken from the client (same rule as &#x60;Booking.totalPrice&#x60;). &#x60;paymentMethod&#x60; is a convenience denormalization of the &#x60;Payment&#x60; this order settled with (there&#39;s at most one - see &#x60;Payment_unique_per_order&#x60;); it&#39;s &#x60;null&#x60; for anything still &#x60;OPEN&#x60;/&#x60;SENT&#x60;/&#x60;CANCELLED&#x60;, and set once and never changed once the order is &#x60;PAID&#x60;. Exists so a closed-order list/detail view can show how it was paid without a second round trip - there&#39;s no &#x60;GET /payments/{id}&#x60; or &#x60;?orderId&#x3D;&#x60; filter on &#x60;Payment&#x60; to fetch it separately. &#x60;openedByEmail&#x60; is the same kind of denormalization of &#x60;openedByUserId&#x60; that &#x60;ShiftListItem.openedByEmail&#x60; is of &#x60;Shift.openedByUserId&#x60; - a MANAGER building the staff filter on &#x60;GET /orders&#x60; can&#39;t fall back to &#x60;GET /users&#x60; (ADMIN-only) the way a CASHIER+ page elsewhere in this API can. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class Order {

  private String id;

  private JsonNullable<String> tableId = JsonNullable.<String>undefined();

  private JsonNullable<String> bookingId = JsonNullable.<String>undefined();

  private JsonNullable<String> guestName = JsonNullable.<String>undefined();

  private OrderStatus status;

  private String openedByUserId;

  private String openedByEmail;

  private String total;

  private JsonNullable<String> note = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid OrderItem> items = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  private JsonNullable<PaymentMethod> paymentMethod = JsonNullable.<PaymentMethod>undefined();

  public Order() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Order(String id, String tableId, String bookingId, String guestName, OrderStatus status, String openedByUserId, String openedByEmail, String total, String note, List<@Valid OrderItem> items, OffsetDateTime createdAt, OffsetDateTime updatedAt, PaymentMethod paymentMethod) {
    this.id = id;
    this.tableId = JsonNullable.of(tableId);
    this.bookingId = JsonNullable.of(bookingId);
    this.guestName = JsonNullable.of(guestName);
    this.status = status;
    this.openedByUserId = openedByUserId;
    this.openedByEmail = openedByEmail;
    this.total = total;
    this.note = JsonNullable.of(note);
    this.items = items;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.paymentMethod = JsonNullable.of(paymentMethod);
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

  public Order openedByEmail(String openedByEmail) {
    this.openedByEmail = openedByEmail;
    return this;
  }

  /**
   * Get openedByEmail
   * @return openedByEmail
   */
  @NotNull 
  @JsonProperty("openedByEmail")
  public String getOpenedByEmail() {
    return openedByEmail;
  }

  public void setOpenedByEmail(String openedByEmail) {
    this.openedByEmail = openedByEmail;
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

  public Order paymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = JsonNullable.of(paymentMethod);
    return this;
  }

  /**
   * Get paymentMethod
   * @return paymentMethod
   */
  @NotNull @Valid 
  @JsonProperty("paymentMethod")
  public JsonNullable<PaymentMethod> getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(JsonNullable<PaymentMethod> paymentMethod) {
    this.paymentMethod = paymentMethod;
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
        Objects.equals(this.openedByEmail, order.openedByEmail) &&
        Objects.equals(this.total, order.total) &&
        Objects.equals(this.note, order.note) &&
        Objects.equals(this.items, order.items) &&
        Objects.equals(this.createdAt, order.createdAt) &&
        Objects.equals(this.updatedAt, order.updatedAt) &&
        Objects.equals(this.paymentMethod, order.paymentMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, tableId, bookingId, guestName, status, openedByUserId, openedByEmail, total, note, items, createdAt, updatedAt, paymentMethod);
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
    sb.append("    openedByEmail: ").append(toIndentedString(openedByEmail)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("    paymentMethod: ").append(toIndentedString(paymentMethod)).append("\n");
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

