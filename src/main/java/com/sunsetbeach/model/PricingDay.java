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
 * PricingDay
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PricingDay {

  private String date;

  private BigDecimal price;

  private Boolean isOverride;

  public PricingDay() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PricingDay(String date, BigDecimal price, Boolean isOverride) {
    this.date = date;
    this.price = price;
    this.isOverride = isOverride;
  }

  public PricingDay date(String date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("date")
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public PricingDay price(BigDecimal price) {
    this.price = price;
    return this;
  }

  /**
   * Explicitly converted with `Number(...)` server-side — a real JSON number, unlike `Room.basePrice`.
   * @return price
   */
  @NotNull @Valid 
  @JsonProperty("price")
  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public PricingDay isOverride(Boolean isOverride) {
    this.isOverride = isOverride;
    return this;
  }

  /**
   * True if this day has an explicit `RatePlan` row; false if it fell back to `Room.basePrice`.
   * @return isOverride
   */
  @NotNull 
  @JsonProperty("isOverride")
  public Boolean getIsOverride() {
    return isOverride;
  }

  public void setIsOverride(Boolean isOverride) {
    this.isOverride = isOverride;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PricingDay pricingDay = (PricingDay) o;
    return Objects.equals(this.date, pricingDay.date) &&
        Objects.equals(this.price, pricingDay.price) &&
        Objects.equals(this.isOverride, pricingDay.isOverride);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, price, isOverride);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PricingDay {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    price: ").append(toIndentedString(price)).append("\n");
    sb.append("    isOverride: ").append(toIndentedString(isOverride)).append("\n");
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

