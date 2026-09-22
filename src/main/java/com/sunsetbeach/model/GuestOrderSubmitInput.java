package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.OrderItemInput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;POST /guest/orders&#x60; - see that operation&#39;s own description.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestOrderSubmitInput {

  private String bookingId;

  @Valid
  private List<@Valid OrderItemInput> items = new ArrayList<>();

  public GuestOrderSubmitInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestOrderSubmitInput(String bookingId, List<@Valid OrderItemInput> items) {
    this.bookingId = bookingId;
    this.items = items;
  }

  public GuestOrderSubmitInput bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * Get bookingId
   * @return bookingId
   */
  @NotNull 
  @JsonProperty("bookingId")
  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public GuestOrderSubmitInput items(List<@Valid OrderItemInput> items) {
    this.items = items;
    return this;
  }

  public GuestOrderSubmitInput addItemsItem(OrderItemInput itemsItem) {
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
  public List<@Valid OrderItemInput> getItems() {
    return items;
  }

  public void setItems(List<@Valid OrderItemInput> items) {
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
    GuestOrderSubmitInput guestOrderSubmitInput = (GuestOrderSubmitInput) o;
    return Objects.equals(this.bookingId, guestOrderSubmitInput.bookingId) &&
        Objects.equals(this.items, guestOrderSubmitInput.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestOrderSubmitInput {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
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

