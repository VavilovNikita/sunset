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
 * OrderItemVoidInput
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class OrderItemVoidInput {

  private JsonNullable<@Min(1) Integer> quantity = JsonNullable.<Integer>undefined();

  private String reason;

  public OrderItemVoidInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OrderItemVoidInput(String reason) {
    this.reason = reason;
  }

  public OrderItemVoidInput quantity(Integer quantity) {
    this.quantity = JsonNullable.of(quantity);
    return this;
  }

  /**
   * How many units of the line to void; omitted or null voids the whole line.
   * minimum: 1
   * @return quantity
   */
  @Min(1) 
  @JsonProperty("quantity")
  public JsonNullable<@Min(1) Integer> getQuantity() {
    return quantity;
  }

  public void setQuantity(JsonNullable<Integer> quantity) {
    this.quantity = quantity;
  }

  public OrderItemVoidInput reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Required - recorded on the void and in the audit log.
   * @return reason
   */
  @NotNull @Size(min = 1, max = 500) 
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderItemVoidInput orderItemVoidInput = (OrderItemVoidInput) o;
    return equalsNullable(this.quantity, orderItemVoidInput.quantity) &&
        Objects.equals(this.reason, orderItemVoidInput.reason);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(quantity), reason);
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
    sb.append("class OrderItemVoidInput {\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

