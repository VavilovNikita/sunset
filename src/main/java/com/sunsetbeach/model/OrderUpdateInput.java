package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.OrderStatus;
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
 * Partial update (unlike &#x60;RoomInput&#x60;/&#x60;MenuItemInput&#x60;/&#x60;TableInput&#x60;): an omitted field leaves the existing value alone, an explicit &#x60;null&#x60; on &#x60;note&#x60;/&#x60;tableId&#x60; clears it. The only legal &#x60;status&#x60; transition through this endpoint is &#x60;OPEN&#x60; -&gt; &#x60;SENT&#x60;; any other value (including trying to set &#x60;PAID&#x60;/&#x60;CANCELLED&#x60; here instead of through &#x60;/close&#x60; or &#x60;/cancel&#x60;) is rejected with 400. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T15:38:53.433655500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class OrderUpdateInput {

  private JsonNullable<@Size(max = 1000) String> note = JsonNullable.<String>undefined();

  private JsonNullable<String> tableId = JsonNullable.<String>undefined();

  private OrderStatus status;

  public OrderUpdateInput note(String note) {
    this.note = JsonNullable.of(note);
    return this;
  }

  /**
   * Get note
   * @return note
   */
  @Size(max = 1000) 
  @JsonProperty("note")
  public JsonNullable<@Size(max = 1000) String> getNote() {
    return note;
  }

  public void setNote(JsonNullable<String> note) {
    this.note = note;
  }

  public OrderUpdateInput tableId(String tableId) {
    this.tableId = JsonNullable.of(tableId);
    return this;
  }

  /**
   * Get tableId
   * @return tableId
   */
  
  @JsonProperty("tableId")
  public JsonNullable<String> getTableId() {
    return tableId;
  }

  public void setTableId(JsonNullable<String> tableId) {
    this.tableId = tableId;
  }

  public OrderUpdateInput status(OrderStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @Valid 
  @JsonProperty("status")
  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderUpdateInput orderUpdateInput = (OrderUpdateInput) o;
    return equalsNullable(this.note, orderUpdateInput.note) &&
        equalsNullable(this.tableId, orderUpdateInput.tableId) &&
        Objects.equals(this.status, orderUpdateInput.status);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(note), hashCodeNullable(tableId), status);
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
    sb.append("class OrderUpdateInput {\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

