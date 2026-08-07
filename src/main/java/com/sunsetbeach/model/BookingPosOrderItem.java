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
 * Compact line-item summary - menu item name (not id) and quantity, nothing else.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T16:23:36.900320800+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class BookingPosOrderItem {

  private String name;

  private Integer quantity;

  public BookingPosOrderItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingPosOrderItem(String name, Integer quantity) {
    this.name = name;
    this.quantity = quantity;
  }

  public BookingPosOrderItem name(String name) {
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

  public BookingPosOrderItem quantity(Integer quantity) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingPosOrderItem bookingPosOrderItem = (BookingPosOrderItem) o;
    return Objects.equals(this.name, bookingPosOrderItem.name) &&
        Objects.equals(this.quantity, bookingPosOrderItem.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, quantity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingPosOrderItem {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
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

