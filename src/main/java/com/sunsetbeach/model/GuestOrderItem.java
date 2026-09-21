package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One line of a &#x60;GuestOrderView&#x60; - the guest-facing projection of &#x60;OrderItem&#x60;. Carries the menu item&#39;s name (a guest has no way to resolve &#x60;menuItemId&#x60; on their own) instead of the id itself, and deliberately nothing about &#x60;sentAt&#x60; - whether a line has already gone to the kitchen is staff information, not something a guest&#39;s phone needs to render. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestOrderItem {

  private String name;

  private Integer quantity;

  private JsonNullable<String> note = JsonNullable.<String>undefined();

  private String unitPrice;

  public GuestOrderItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestOrderItem(String name, Integer quantity, String note, String unitPrice) {
    this.name = name;
    this.quantity = quantity;
    this.note = JsonNullable.of(note);
    this.unitPrice = unitPrice;
  }

  public GuestOrderItem name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public GuestOrderItem quantity(Integer quantity) {
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

  public GuestOrderItem note(String note) {
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

  public GuestOrderItem unitPrice(String unitPrice) {
    this.unitPrice = unitPrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string - same snapshot-at-add-time value as `OrderItem.unitPrice`.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestOrderItem guestOrderItem = (GuestOrderItem) o;
    return Objects.equals(this.name, guestOrderItem.name) &&
        Objects.equals(this.quantity, guestOrderItem.quantity) &&
        Objects.equals(this.note, guestOrderItem.note) &&
        Objects.equals(this.unitPrice, guestOrderItem.unitPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, quantity, note, unitPrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestOrderItem {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    unitPrice: ").append(toIndentedString(unitPrice)).append("\n");
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

