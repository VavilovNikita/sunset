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
 * Body of &#x60;PUT /settings/vat&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class VatSettingsUpdateInput {

  private BigDecimal vatRate;

  public VatSettingsUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VatSettingsUpdateInput(BigDecimal vatRate) {
    this.vatRate = vatRate;
  }

  public VatSettingsUpdateInput vatRate(BigDecimal vatRate) {
    this.vatRate = vatRate;
    return this;
  }

  /**
   * Percent, at most two decimals. 0 is allowed (VAT not charged).
   * minimum: 0
   * maximum: 99.99
   * @return vatRate
   */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("99.99") 
  @JsonProperty("vatRate")
  public BigDecimal getVatRate() {
    return vatRate;
  }

  public void setVatRate(BigDecimal vatRate) {
    this.vatRate = vatRate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VatSettingsUpdateInput vatSettingsUpdateInput = (VatSettingsUpdateInput) o;
    return Objects.equals(this.vatRate, vatSettingsUpdateInput.vatRate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vatRate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VatSettingsUpdateInput {\n");
    sb.append("    vatRate: ").append(toIndentedString(vatRate)).append("\n");
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

