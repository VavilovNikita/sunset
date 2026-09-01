package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.PricingDay;
import java.math.BigDecimal;
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
 * PricingResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PricingResponse {

  private BigDecimal basePrice;

  @Valid
  private List<@Valid PricingDay> days = new ArrayList<>();

  public PricingResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PricingResponse(BigDecimal basePrice, List<@Valid PricingDay> days) {
    this.basePrice = basePrice;
    this.days = days;
  }

  public PricingResponse basePrice(BigDecimal basePrice) {
    this.basePrice = basePrice;
    return this;
  }

  /**
   * Room.basePrice explicitly converted with `Number(...)`.
   * @return basePrice
   */
  @NotNull @Valid 
  @JsonProperty("basePrice")
  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(BigDecimal basePrice) {
    this.basePrice = basePrice;
  }

  public PricingResponse days(List<@Valid PricingDay> days) {
    this.days = days;
    return this;
  }

  public PricingResponse addDaysItem(PricingDay daysItem) {
    if (this.days == null) {
      this.days = new ArrayList<>();
    }
    this.days.add(daysItem);
    return this;
  }

  /**
   * Get days
   * @return days
   */
  @NotNull @Valid 
  @JsonProperty("days")
  public List<@Valid PricingDay> getDays() {
    return days;
  }

  public void setDays(List<@Valid PricingDay> days) {
    this.days = days;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PricingResponse pricingResponse = (PricingResponse) o;
    return Objects.equals(this.basePrice, pricingResponse.basePrice) &&
        Objects.equals(this.days, pricingResponse.days);
  }

  @Override
  public int hashCode() {
    return Objects.hash(basePrice, days);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PricingResponse {\n");
    sb.append("    basePrice: ").append(toIndentedString(basePrice)).append("\n");
    sb.append("    days: ").append(toIndentedString(days)).append("\n");
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

