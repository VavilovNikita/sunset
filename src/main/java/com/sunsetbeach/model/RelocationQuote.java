package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.RelocationRateMode;
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
 * Response of &#x60;POST /bookings/{id}/relocate/quote&#x60;. Every figure is computed by the server with the same code the apply call uses, so what is shown before confirming is what is charged (to the satang). All amounts are decimal(10,2) strings of the *whole booking*. &#x60;totalPrice&#x60;, &#x60;nights&#x60;, &#x60;available&#x60; and &#x60;reason&#x60; mean what they do in &#x60;BookingScheduleQuote&#x60; (&#x60;totalPrice&#x60; is for &#x60;rateMode&#x60;, the mode that would be applied: the requested one, else the default for this move). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RelocationQuote {

  private String totalPrice;

  private Integer nights;

  private Boolean available;

  private JsonNullable<String> reason = JsonNullable.<String>undefined();

  private String currentTotalPrice;

  private Boolean sameRoomType;

  private Boolean wholeSegment;

  private RelocationRateMode rateMode;

  private Boolean mayKeepRate;

  private String keepRateTotalPrice;

  private String keepRatePriceDifference;

  private JsonNullable<String> newRateTotalPrice = JsonNullable.<String>undefined();

  private JsonNullable<String> newRatePriceDifference = JsonNullable.<String>undefined();

  public RelocationQuote() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RelocationQuote(String totalPrice, Integer nights, Boolean available, String reason, String currentTotalPrice, Boolean sameRoomType, Boolean wholeSegment, RelocationRateMode rateMode, Boolean mayKeepRate, String keepRateTotalPrice, String keepRatePriceDifference) {
    this.totalPrice = totalPrice;
    this.nights = nights;
    this.available = available;
    this.reason = JsonNullable.of(reason);
    this.currentTotalPrice = currentTotalPrice;
    this.sameRoomType = sameRoomType;
    this.wholeSegment = wholeSegment;
    this.rateMode = rateMode;
    this.mayKeepRate = mayKeepRate;
    this.keepRateTotalPrice = keepRateTotalPrice;
    this.keepRatePriceDifference = keepRatePriceDifference;
  }

  public RelocationQuote totalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * Get totalPrice
   * @return totalPrice
   */
  @NotNull 
  @JsonProperty("totalPrice")
  public String getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
  }

  public RelocationQuote nights(Integer nights) {
    this.nights = nights;
    return this;
  }

  /**
   * Nights that move to the new room.
   * @return nights
   */
  @NotNull 
  @JsonProperty("nights")
  public Integer getNights() {
    return nights;
  }

  public void setNights(Integer nights) {
    this.nights = nights;
  }

  public RelocationQuote available(Boolean available) {
    this.available = available;
    return this;
  }

  /**
   * Get available
   * @return available
   */
  @NotNull 
  @JsonProperty("available")
  public Boolean getAvailable() {
    return available;
  }

  public void setAvailable(Boolean available) {
    this.available = available;
  }

  public RelocationQuote reason(String reason) {
    this.reason = JsonNullable.of(reason);
    return this;
  }

  /**
   * Get reason
   * @return reason
   */
  @NotNull 
  @JsonProperty("reason")
  public JsonNullable<String> getReason() {
    return reason;
  }

  public void setReason(JsonNullable<String> reason) {
    this.reason = reason;
  }

  public RelocationQuote currentTotalPrice(String currentTotalPrice) {
    this.currentTotalPrice = currentTotalPrice;
    return this;
  }

  /**
   * The booking's total today.
   * @return currentTotalPrice
   */
  @NotNull 
  @JsonProperty("currentTotalPrice")
  public String getCurrentTotalPrice() {
    return currentTotalPrice;
  }

  public void setCurrentTotalPrice(String currentTotalPrice) {
    this.currentTotalPrice = currentTotalPrice;
  }

  public RelocationQuote sameRoomType(Boolean sameRoomType) {
    this.sameRoomType = sameRoomType;
    return this;
  }

  /**
   * The new room is of the type the guest is leaving - the price cannot change.
   * @return sameRoomType
   */
  @NotNull 
  @JsonProperty("sameRoomType")
  public Boolean getSameRoomType() {
    return sameRoomType;
  }

  public void setSameRoomType(Boolean sameRoomType) {
    this.sameRoomType = sameRoomType;
  }

  public RelocationQuote wholeSegment(Boolean wholeSegment) {
    this.wholeSegment = wholeSegment;
    return this;
  }

  /**
   * The move starts on the segment's first night, so the whole segment changes room and no split (nor undo) is involved. 
   * @return wholeSegment
   */
  @NotNull 
  @JsonProperty("wholeSegment")
  public Boolean getWholeSegment() {
    return wholeSegment;
  }

  public void setWholeSegment(Boolean wholeSegment) {
    this.wholeSegment = wholeSegment;
  }

  public RelocationQuote rateMode(RelocationRateMode rateMode) {
    this.rateMode = rateMode;
    return this;
  }

  /**
   * Get rateMode
   * @return rateMode
   */
  @NotNull @Valid 
  @JsonProperty("rateMode")
  public RelocationRateMode getRateMode() {
    return rateMode;
  }

  public void setRateMode(RelocationRateMode rateMode) {
    this.rateMode = rateMode;
  }

  public RelocationQuote mayKeepRate(Boolean mayKeepRate) {
    this.mayKeepRate = mayKeepRate;
    return this;
  }

  /**
   * Whether the caller may apply `KEEP_RATE` for this move (always true within one room type).
   * @return mayKeepRate
   */
  @NotNull 
  @JsonProperty("mayKeepRate")
  public Boolean getMayKeepRate() {
    return mayKeepRate;
  }

  public void setMayKeepRate(Boolean mayKeepRate) {
    this.mayKeepRate = mayKeepRate;
  }

  public RelocationQuote keepRateTotalPrice(String keepRateTotalPrice) {
    this.keepRateTotalPrice = keepRateTotalPrice;
    return this;
  }

  /**
   * The booking's total if the moved nights keep their agreed prices.
   * @return keepRateTotalPrice
   */
  @NotNull 
  @JsonProperty("keepRateTotalPrice")
  public String getKeepRateTotalPrice() {
    return keepRateTotalPrice;
  }

  public void setKeepRateTotalPrice(String keepRateTotalPrice) {
    this.keepRateTotalPrice = keepRateTotalPrice;
  }

  public RelocationQuote keepRatePriceDifference(String keepRatePriceDifference) {
    this.keepRatePriceDifference = keepRatePriceDifference;
    return this;
  }

  /**
   * `keepRateTotalPrice` minus `currentTotalPrice`, signed (\"0.00\" for a move that changes nothing).
   * @return keepRatePriceDifference
   */
  @NotNull 
  @JsonProperty("keepRatePriceDifference")
  public String getKeepRatePriceDifference() {
    return keepRatePriceDifference;
  }

  public void setKeepRatePriceDifference(String keepRatePriceDifference) {
    this.keepRatePriceDifference = keepRatePriceDifference;
  }

  public RelocationQuote newRateTotalPrice(String newRateTotalPrice) {
    this.newRateTotalPrice = JsonNullable.of(newRateTotalPrice);
    return this;
  }

  /**
   * The booking's total if the moved nights are priced at the new type's current rate. Absent within one room type.
   * @return newRateTotalPrice
   */
  
  @JsonProperty("newRateTotalPrice")
  public JsonNullable<String> getNewRateTotalPrice() {
    return newRateTotalPrice;
  }

  public void setNewRateTotalPrice(JsonNullable<String> newRateTotalPrice) {
    this.newRateTotalPrice = newRateTotalPrice;
  }

  public RelocationQuote newRatePriceDifference(String newRatePriceDifference) {
    this.newRatePriceDifference = JsonNullable.of(newRatePriceDifference);
    return this;
  }

  /**
   * `newRateTotalPrice` minus `currentTotalPrice`, signed. Absent within one room type.
   * @return newRatePriceDifference
   */
  
  @JsonProperty("newRatePriceDifference")
  public JsonNullable<String> getNewRatePriceDifference() {
    return newRatePriceDifference;
  }

  public void setNewRatePriceDifference(JsonNullable<String> newRatePriceDifference) {
    this.newRatePriceDifference = newRatePriceDifference;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RelocationQuote relocationQuote = (RelocationQuote) o;
    return Objects.equals(this.totalPrice, relocationQuote.totalPrice) &&
        Objects.equals(this.nights, relocationQuote.nights) &&
        Objects.equals(this.available, relocationQuote.available) &&
        Objects.equals(this.reason, relocationQuote.reason) &&
        Objects.equals(this.currentTotalPrice, relocationQuote.currentTotalPrice) &&
        Objects.equals(this.sameRoomType, relocationQuote.sameRoomType) &&
        Objects.equals(this.wholeSegment, relocationQuote.wholeSegment) &&
        Objects.equals(this.rateMode, relocationQuote.rateMode) &&
        Objects.equals(this.mayKeepRate, relocationQuote.mayKeepRate) &&
        Objects.equals(this.keepRateTotalPrice, relocationQuote.keepRateTotalPrice) &&
        Objects.equals(this.keepRatePriceDifference, relocationQuote.keepRatePriceDifference) &&
        equalsNullable(this.newRateTotalPrice, relocationQuote.newRateTotalPrice) &&
        equalsNullable(this.newRatePriceDifference, relocationQuote.newRatePriceDifference);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalPrice, nights, available, reason, currentTotalPrice, sameRoomType, wholeSegment, rateMode, mayKeepRate, keepRateTotalPrice, keepRatePriceDifference, hashCodeNullable(newRateTotalPrice), hashCodeNullable(newRatePriceDifference));
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
    sb.append("class RelocationQuote {\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
    sb.append("    nights: ").append(toIndentedString(nights)).append("\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    currentTotalPrice: ").append(toIndentedString(currentTotalPrice)).append("\n");
    sb.append("    sameRoomType: ").append(toIndentedString(sameRoomType)).append("\n");
    sb.append("    wholeSegment: ").append(toIndentedString(wholeSegment)).append("\n");
    sb.append("    rateMode: ").append(toIndentedString(rateMode)).append("\n");
    sb.append("    mayKeepRate: ").append(toIndentedString(mayKeepRate)).append("\n");
    sb.append("    keepRateTotalPrice: ").append(toIndentedString(keepRateTotalPrice)).append("\n");
    sb.append("    keepRatePriceDifference: ").append(toIndentedString(keepRatePriceDifference)).append("\n");
    sb.append("    newRateTotalPrice: ").append(toIndentedString(newRateTotalPrice)).append("\n");
    sb.append("    newRatePriceDifference: ").append(toIndentedString(newRatePriceDifference)).append("\n");
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

