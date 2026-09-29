package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The one VAT rate applied to all revenue, room and POS alike, by &#x60;GET /reports/revenue-statistic&#x60;. Stored, not hardcoded, so it can change without a deploy; seeded at 7.00 (Thailand&#39;s standard rate) as an editable default. All prices in this system are VAT-inclusive - see that report for why - so the rate is used to extract VAT from a figure, never to add it on top. Nothing else in the system reads it: no price, order total or receipt changes when it does. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class VatSettings {

  private String vatRate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public VatSettings() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VatSettings(String vatRate, OffsetDateTime updatedAt) {
    this.vatRate = vatRate;
    this.updatedAt = updatedAt;
  }

  public VatSettings vatRate(String vatRate) {
    this.vatRate = vatRate;
    return this;
  }

  /**
   * Percent, two decimals, e.g. \"7.00\".
   * @return vatRate
   */
  @NotNull 
  @JsonProperty("vatRate")
  public String getVatRate() {
    return vatRate;
  }

  public void setVatRate(String vatRate) {
    this.vatRate = vatRate;
  }

  public VatSettings updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VatSettings vatSettings = (VatSettings) o;
    return Objects.equals(this.vatRate, vatSettings.vatRate) &&
        Objects.equals(this.updatedAt, vatSettings.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vatRate, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VatSettings {\n");
    sb.append("    vatRate: ").append(toIndentedString(vatRate)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

