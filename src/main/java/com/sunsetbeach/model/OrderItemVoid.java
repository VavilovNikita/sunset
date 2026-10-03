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
 * A quantity taken off a sent line (&#x60;POST /orders/{id}/items/{itemId}/void&#x60;). Immutable once written. &#x60;unitPrice&#x60;/&#x60;note&#x60;/&#x60;sentAt&#x60; are copied from the line as it was, so what was voided can be read without the line (which is deleted once fully voided). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class OrderItemVoid {

  private String id;

  private String menuItemId;

  private Integer quantity;

  private String unitPrice;

  private JsonNullable<String> note = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> sentAt = JsonNullable.<OffsetDateTime>undefined();

  private String reason;

  private String voidedByUserId;

  private String voidedByEmail;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime voidedAt;

  public OrderItemVoid() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OrderItemVoid(String id, String menuItemId, Integer quantity, String unitPrice, String note, OffsetDateTime sentAt, String reason, String voidedByUserId, String voidedByEmail, OffsetDateTime voidedAt) {
    this.id = id;
    this.menuItemId = menuItemId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.note = JsonNullable.of(note);
    this.sentAt = JsonNullable.of(sentAt);
    this.reason = reason;
    this.voidedByUserId = voidedByUserId;
    this.voidedByEmail = voidedByEmail;
    this.voidedAt = voidedAt;
  }

  public OrderItemVoid id(String id) {
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

  public OrderItemVoid menuItemId(String menuItemId) {
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

  public OrderItemVoid quantity(Integer quantity) {
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

  public OrderItemVoid unitPrice(String unitPrice) {
    this.unitPrice = unitPrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
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

  public OrderItemVoid note(String note) {
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

  public OrderItemVoid sentAt(OffsetDateTime sentAt) {
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

  public OrderItemVoid reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Get reason
   * @return reason
   */
  @NotNull 
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public OrderItemVoid voidedByUserId(String voidedByUserId) {
    this.voidedByUserId = voidedByUserId;
    return this;
  }

  /**
   * Get voidedByUserId
   * @return voidedByUserId
   */
  @NotNull 
  @JsonProperty("voidedByUserId")
  public String getVoidedByUserId() {
    return voidedByUserId;
  }

  public void setVoidedByUserId(String voidedByUserId) {
    this.voidedByUserId = voidedByUserId;
  }

  public OrderItemVoid voidedByEmail(String voidedByEmail) {
    this.voidedByEmail = voidedByEmail;
    return this;
  }

  /**
   * Same denormalization as `Order.openedByEmail`; falls back to the raw id for a deleted user.
   * @return voidedByEmail
   */
  @NotNull 
  @JsonProperty("voidedByEmail")
  public String getVoidedByEmail() {
    return voidedByEmail;
  }

  public void setVoidedByEmail(String voidedByEmail) {
    this.voidedByEmail = voidedByEmail;
  }

  public OrderItemVoid voidedAt(OffsetDateTime voidedAt) {
    this.voidedAt = voidedAt;
    return this;
  }

  /**
   * Get voidedAt
   * @return voidedAt
   */
  @NotNull @Valid 
  @JsonProperty("voidedAt")
  public OffsetDateTime getVoidedAt() {
    return voidedAt;
  }

  public void setVoidedAt(OffsetDateTime voidedAt) {
    this.voidedAt = voidedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderItemVoid orderItemVoid = (OrderItemVoid) o;
    return Objects.equals(this.id, orderItemVoid.id) &&
        Objects.equals(this.menuItemId, orderItemVoid.menuItemId) &&
        Objects.equals(this.quantity, orderItemVoid.quantity) &&
        Objects.equals(this.unitPrice, orderItemVoid.unitPrice) &&
        Objects.equals(this.note, orderItemVoid.note) &&
        Objects.equals(this.sentAt, orderItemVoid.sentAt) &&
        Objects.equals(this.reason, orderItemVoid.reason) &&
        Objects.equals(this.voidedByUserId, orderItemVoid.voidedByUserId) &&
        Objects.equals(this.voidedByEmail, orderItemVoid.voidedByEmail) &&
        Objects.equals(this.voidedAt, orderItemVoid.voidedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, menuItemId, quantity, unitPrice, note, sentAt, reason, voidedByUserId, voidedByEmail, voidedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrderItemVoid {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    menuItemId: ").append(toIndentedString(menuItemId)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    unitPrice: ").append(toIndentedString(unitPrice)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    sentAt: ").append(toIndentedString(sentAt)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    voidedByUserId: ").append(toIndentedString(voidedByUserId)).append("\n");
    sb.append("    voidedByEmail: ").append(toIndentedString(voidedByEmail)).append("\n");
    sb.append("    voidedAt: ").append(toIndentedString(voidedAt)).append("\n");
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

