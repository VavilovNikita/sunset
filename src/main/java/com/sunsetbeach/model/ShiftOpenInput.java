package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ShiftOpenInput
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T15:38:53.433655500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class ShiftOpenInput {

  private BigDecimal openingCashFloat;

  public ShiftOpenInput openingCashFloat(BigDecimal openingCashFloat) {
    this.openingCashFloat = openingCashFloat;
    return this;
  }

  /**
   * Get openingCashFloat
   * minimum: 0
   * @return openingCashFloat
   */
  @Valid @DecimalMin("0") 
  @JsonProperty("openingCashFloat")
  public BigDecimal getOpeningCashFloat() {
    return openingCashFloat;
  }

  public void setOpeningCashFloat(BigDecimal openingCashFloat) {
    this.openingCashFloat = openingCashFloat;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftOpenInput shiftOpenInput = (ShiftOpenInput) o;
    return Objects.equals(this.openingCashFloat, shiftOpenInput.openingCashFloat);
  }

  @Override
  public int hashCode() {
    return Objects.hash(openingCashFloat);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShiftOpenInput {\n");
    sb.append("    openingCashFloat: ").append(toIndentedString(openingCashFloat)).append("\n");
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

