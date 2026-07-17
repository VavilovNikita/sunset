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
 * Body of &#x60;priceRangeSchema&#x60;. Upserts one &#x60;RatePlan&#x60; row per day in &#x60;[from, to]&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-17T16:01:20.967720600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class PriceRangeInput {

  private String from;

  private String to;

  private BigDecimal price;

  public PriceRangeInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PriceRangeInput(String from, String to, BigDecimal price) {
    this.from = from;
    this.to = to;
    this.price = price;
  }

  public PriceRangeInput from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public PriceRangeInput to(String to) {
    this.to = to;
    return this;
  }

  /**
   * Get to
   * @return to
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public PriceRangeInput price(BigDecimal price) {
    this.price = price;
    return this;
  }

  /**
   * Coerced to a number server-side (`z.coerce.number()`); a numeric string is also accepted.
   * minimum: 0
   * @return price
   */
  @NotNull @Valid @DecimalMin(value = "0", inclusive = false) 
  @JsonProperty("price")
  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PriceRangeInput priceRangeInput = (PriceRangeInput) o;
    return Objects.equals(this.from, priceRangeInput.from) &&
        Objects.equals(this.to, priceRangeInput.to) &&
        Objects.equals(this.price, priceRangeInput.price);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, price);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PriceRangeInput {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    price: ").append(toIndentedString(price)).append("\n");
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

