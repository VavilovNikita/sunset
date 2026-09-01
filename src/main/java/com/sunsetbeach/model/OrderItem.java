package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A single line of an Order. &#x60;unitPrice&#x60; is a snapshot of &#x60;MenuItem.price&#x60; at the moment the item was added — later menu price changes never retroactively affect it. &#x60;sentAt&#x60; is set the moment this line is included in a kitchen/bar ticket print (the original send, or a later re-order print — see &#x60;POST /orders/{id}/items&#x60;); it&#39;s set regardless of whether that print actually reached the printer, same \&quot;dispatched, not delivered\&quot; contract as &#x60;Order.status&#x60; transitioning to &#x60;SENT&#x60;. Two lines are candidates for merging (see &#x60;POST /orders/{id}/items&#x60;) only when both have &#x60;sentAt: null&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class OrderItem {

  private String id;

  private String orderId;

  private String menuItemId;

  private Integer quantity;

  private String unitPrice;

  private JsonNullable<String> note = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> sentAt = JsonNullable.<OffsetDateTime>undefined();

  public OrderItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OrderItem(String id, String orderId, String menuItemId, Integer quantity, String unitPrice, String note, OffsetDateTime createdAt, OffsetDateTime sentAt) {
    this.id = id;
    this.orderId = orderId;
    this.menuItemId = menuItemId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.note = JsonNullable.of(note);
    this.createdAt = createdAt;
    this.sentAt = JsonNullable.of(sentAt);
  }

  public OrderItem id(String id) {
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

  public OrderItem orderId(String orderId) {
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

  public OrderItem menuItemId(String menuItemId) {
    this.menuItemId = menuItemId;
    return this;
  }

  /**
   * Get menuItemId
   * @return menuItemId
   */
  @NotNull 
  @JsonProperty("menuItemId")
  public String getMenuItemId() {
    return menuItemId;
  }

  public void setMenuItemId(String menuItemId) {
    this.menuItemId = menuItemId;
  }

  public OrderItem quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Get quantity
   * @return quantity
   */
  @NotNull 
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public OrderItem unitPrice(String unitPrice) {
    this.unitPrice = unitPrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string, snapshotted at insert time.
   * @return unitPrice
   */
  @NotNull 
  @JsonProperty("unitPrice")
  public String getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(String unitPrice) {
    this.unitPrice = unitPrice;
  }

  public OrderItem note(String note) {
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

  public OrderItem createdAt(OffsetDateTime createdAt) {
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

  public OrderItem sentAt(OffsetDateTime sentAt) {
    this.sentAt = JsonNullable.of(sentAt);
    return this;
  }

  /**
   * Get sentAt
   * @return sentAt
   */
  @NotNull @Valid 
  @JsonProperty("sentAt")
  public JsonNullable<OffsetDateTime> getSentAt() {
    return sentAt;
  }

  public void setSentAt(JsonNullable<OffsetDateTime> sentAt) {
    this.sentAt = sentAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderItem orderItem = (OrderItem) o;
    return Objects.equals(this.id, orderItem.id) &&
        Objects.equals(this.orderId, orderItem.orderId) &&
        Objects.equals(this.menuItemId, orderItem.menuItemId) &&
        Objects.equals(this.quantity, orderItem.quantity) &&
        Objects.equals(this.unitPrice, orderItem.unitPrice) &&
        Objects.equals(this.note, orderItem.note) &&
        Objects.equals(this.createdAt, orderItem.createdAt) &&
        Objects.equals(this.sentAt, orderItem.sentAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, orderId, menuItemId, quantity, unitPrice, note, createdAt, sentAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrderItem {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    orderId: ").append(toIndentedString(orderId)).append("\n");
    sb.append("    menuItemId: ").append(toIndentedString(menuItemId)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    unitPrice: ").append(toIndentedString(unitPrice)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    sentAt: ").append(toIndentedString(sentAt)).append("\n");
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

