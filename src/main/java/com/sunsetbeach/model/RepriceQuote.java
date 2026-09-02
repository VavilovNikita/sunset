package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;POST /bookings/{id}/reprice/quote&#x60; - a non-mutating preview of what &#x60;POST /bookings/{id}/reprice&#x60; would do. Only nights from today onward within the segment are ever repriced - a night the guest has already stayed keeps whatever price was agreed for it, even when this is invoked mid-stay. &#x60;nightsRepriced&#x60; is &#x60;0&#x60; (and the totals are equal) when the segment has no nights left to reprice (it&#39;s entirely in the past). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RepriceQuote {

  private String oldTotalPrice;

  private String newTotalPrice;

  private Integer nightsRepriced;

  public RepriceQuote() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RepriceQuote(String oldTotalPrice, String newTotalPrice, Integer nightsRepriced) {
    this.oldTotalPrice = oldTotalPrice;
    this.newTotalPrice = newTotalPrice;
    this.nightsRepriced = nightsRepriced;
  }

  public RepriceQuote oldTotalPrice(String oldTotalPrice) {
    this.oldTotalPrice = oldTotalPrice;
    return this;
  }

  /**
   * The segment's current total, decimal(10,2) as a string.
   * @return oldTotalPrice
   */
  @NotNull 
  @JsonProperty("oldTotalPrice")
  public String getOldTotalPrice() {
    return oldTotalPrice;
  }

  public void setOldTotalPrice(String oldTotalPrice) {
    this.oldTotalPrice = oldTotalPrice;
  }

  public RepriceQuote newTotalPrice(String newTotalPrice) {
    this.newTotalPrice = newTotalPrice;
    return this;
  }

  /**
   * What the segment's total would become at today's rates, decimal(10,2) as a string.
   * @return newTotalPrice
   */
  @NotNull 
  @JsonProperty("newTotalPrice")
  public String getNewTotalPrice() {
    return newTotalPrice;
  }

  public void setNewTotalPrice(String newTotalPrice) {
    this.newTotalPrice = newTotalPrice;
  }

  public RepriceQuote nightsRepriced(Integer nightsRepriced) {
    this.nightsRepriced = nightsRepriced;
    return this;
  }

  /**
   * How many nights (today onward) this would touch.
   * @return nightsRepriced
   */
  @NotNull 
  @JsonProperty("nightsRepriced")
  public Integer getNightsRepriced() {
    return nightsRepriced;
  }

  public void setNightsRepriced(Integer nightsRepriced) {
    this.nightsRepriced = nightsRepriced;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RepriceQuote repriceQuote = (RepriceQuote) o;
    return Objects.equals(this.oldTotalPrice, repriceQuote.oldTotalPrice) &&
        Objects.equals(this.newTotalPrice, repriceQuote.newTotalPrice) &&
        Objects.equals(this.nightsRepriced, repriceQuote.nightsRepriced);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oldTotalPrice, newTotalPrice, nightsRepriced);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RepriceQuote {\n");
    sb.append("    oldTotalPrice: ").append(toIndentedString(oldTotalPrice)).append("\n");
    sb.append("    newTotalPrice: ").append(toIndentedString(newTotalPrice)).append("\n");
    sb.append("    nightsRepriced: ").append(toIndentedString(nightsRepriced)).append("\n");
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

