package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.GuestOrderItem;
import com.sunsetbeach.model.OrderStatus;
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
 * The guest-facing projection of &#x60;Order&#x60;, returned by both &#x60;PublicOrderingApi&#x60; operations. Deliberately not &#x60;Order&#x60; itself - never carries &#x60;openedByUserId&#x60;/&#x60;openedByEmail&#x60;, &#x60;guestName&#x60;, &#x60;bookingId&#x60;, &#x60;paymentMethod&#x60;, or &#x60;guestAccessToken&#x60; (the token that gated the request that produced this response is never echoed back in it). &#x60;locationLabel&#x60; is the same \&quot;zone – table label\&quot; text &#x60;OrderPrintingService#describeLocation&#x60; puts on a printed ticket, so a guest looking at their phone sees the same identifier staff do. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestOrderView {

  private String id;

  private OrderStatus status;

  private String locationLabel;

  @Valid
  private List<@Valid GuestOrderItem> items = new ArrayList<>();

  private String total;

  public GuestOrderView() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestOrderView(String id, OrderStatus status, String locationLabel, List<@Valid GuestOrderItem> items, String total) {
    this.id = id;
    this.status = status;
    this.locationLabel = locationLabel;
    this.items = items;
    this.total = total;
  }

  public GuestOrderView id(String id) {
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

  public GuestOrderView status(OrderStatus status) {
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

  public GuestOrderView locationLabel(String locationLabel) {
    this.locationLabel = locationLabel;
    return this;
  }

  /**
   * Get locationLabel
   * @return locationLabel
   */
  @NotNull 
  @JsonProperty("locationLabel")
  public String getLocationLabel() {
    return locationLabel;
  }

  public void setLocationLabel(String locationLabel) {
    this.locationLabel = locationLabel;
  }

  public GuestOrderView items(List<@Valid GuestOrderItem> items) {
    this.items = items;
    return this;
  }

  public GuestOrderView addItemsItem(GuestOrderItem itemsItem) {
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
  public List<@Valid GuestOrderItem> getItems() {
    return items;
  }

  public void setItems(List<@Valid GuestOrderItem> items) {
    this.items = items;
  }

  public GuestOrderView total(String total) {
    this.total = total;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string, e.g. `\"1250.00\"` — same server-computed total as `Order.total`.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestOrderView guestOrderView = (GuestOrderView) o;
    return Objects.equals(this.id, guestOrderView.id) &&
        Objects.equals(this.status, guestOrderView.status) &&
        Objects.equals(this.locationLabel, guestOrderView.locationLabel) &&
        Objects.equals(this.items, guestOrderView.items) &&
        Objects.equals(this.total, guestOrderView.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status, locationLabel, items, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestOrderView {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    locationLabel: ").append(toIndentedString(locationLabel)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
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

