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
 * Used both as the array body of &#x60;POST /orders/{id}/items&#x60; (create) and the single-object body of &#x60;PATCH /orders/{id}/items/{itemId}&#x60; (full replacement of &#x60;quantity&#x60;/&#x60;note&#x60; for that line — &#x60;menuItemId&#x60; is immutable after creation but still required in the body for symmetry with the create shape; the server ignores it on PATCH). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class OrderItemInput {

  private String menuItemId;

  private Integer quantity;

  private JsonNullable<@Size(max = 500) String> note = JsonNullable.<String>undefined();

  public OrderItemInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OrderItemInput(String menuItemId, Integer quantity) {
    this.menuItemId = menuItemId;
    this.quantity = quantity;
  }

  public OrderItemInput menuItemId(String menuItemId) {
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

  public OrderItemInput quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Get quantity
   * minimum: 1
   * @return quantity
   */
  @NotNull @Min(1) 
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public OrderItemInput note(String note) {
    this.note = JsonNullable.of(note);
    return this;
  }

  /**
   * Get note
   * @return note
   */
  @Size(max = 500) 
  @JsonProperty("note")
  public JsonNullable<@Size(max = 500) String> getNote() {
    return note;
  }

  public void setNote(JsonNullable<String> note) {
    this.note = note;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderItemInput orderItemInput = (OrderItemInput) o;
    return Objects.equals(this.menuItemId, orderItemInput.menuItemId) &&
        Objects.equals(this.quantity, orderItemInput.quantity) &&
        equalsNullable(this.note, orderItemInput.note);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(menuItemId, quantity, hashCodeNullable(note));
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
    sb.append("class OrderItemInput {\n");
    sb.append("    menuItemId: ").append(toIndentedString(menuItemId)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
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

