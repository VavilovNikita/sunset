package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.FolioPaymentMethod;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;POST /bookings/{id}/folio-payments&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class FolioPaymentInput {

  private FolioPaymentMethod method;

  private String amount;

  public FolioPaymentInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FolioPaymentInput(FolioPaymentMethod method, String amount) {
    this.method = method;
    this.amount = amount;
  }

  public FolioPaymentInput method(FolioPaymentMethod method) {
    this.method = method;
    return this;
  }

  /**
   * Get method
   * @return method
   */
  @NotNull @Valid 
  @JsonProperty("method")
  public FolioPaymentMethod getMethod() {
    return method;
  }

  public void setMethod(FolioPaymentMethod method) {
    this.method = method;
  }

  public FolioPaymentInput amount(String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Decimal(10,2) as a string, greater than zero. Rejected with 409 if it would exceed what's currently outstanding on this booking's folio - a fat-finger guard, not a restriction on partial payment (a smaller amount than what's owed is always fine). 
   * @return amount
   */
  @NotNull 
  @JsonProperty("amount")
  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FolioPaymentInput folioPaymentInput = (FolioPaymentInput) o;
    return Objects.equals(this.method, folioPaymentInput.method) &&
        Objects.equals(this.amount, folioPaymentInput.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, amount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FolioPaymentInput {\n");
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
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

