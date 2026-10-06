package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.ShiftCodeKind;
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
 * Body of &#x60;POST /shift-codes/{id}/versions&#x60;. No &#x60;staffArea&#x60; or &#x60;code&#x60; - they are the edited code&#39;s own. &#x60;effectiveFrom&#x60; is optional (today when omitted). The interval fields and &#x60;displayColor&#x60; are nullable and so left out of &#x60;required&#x60; (see CLAUDE.md&#39;s Code generation section); omitting one means \&quot;none\&quot; (an OP/PH-style code with no hours, or no colour), not \&quot;keep the old value\&quot; - the form sends every field. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftCodeVersionInput {

  private String effectiveFrom;

  private ShiftCodeKind kind;

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> startTime1 = JsonNullable.<String>undefined();

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> endTime1 = JsonNullable.<String>undefined();

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> startTime2 = JsonNullable.<String>undefined();

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> endTime2 = JsonNullable.<String>undefined();

  private Boolean countsAsWorked;

  private Boolean isPaid;

  private JsonNullable<@Pattern(regexp = "^#[0-9a-fA-F]{6}$") String> displayColor = JsonNullable.<String>undefined();

  public ShiftCodeVersionInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftCodeVersionInput(ShiftCodeKind kind, Boolean countsAsWorked, Boolean isPaid) {
    this.kind = kind;
    this.countsAsWorked = countsAsWorked;
    this.isPaid = isPaid;
  }

  public ShiftCodeVersionInput effectiveFrom(String effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
    return this;
  }

  /**
   * First day the new terms apply, `YYYY-MM-DD`. Defaults to today.
   * @return effectiveFrom
   */
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("effectiveFrom")
  public String getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(String effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public ShiftCodeVersionInput kind(ShiftCodeKind kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Get kind
   * @return kind
   */
  @NotNull @Valid 
  @JsonProperty("kind")
  public ShiftCodeKind getKind() {
    return kind;
  }

  public void setKind(ShiftCodeKind kind) {
    this.kind = kind;
  }

  public ShiftCodeVersionInput startTime1(String startTime1) {
    this.startTime1 = JsonNullable.of(startTime1);
    return this;
  }

  /**
   * Get startTime1
   * @return startTime1
   */
  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("startTime1")
  public JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> getStartTime1() {
    return startTime1;
  }

  public void setStartTime1(JsonNullable<String> startTime1) {
    this.startTime1 = startTime1;
  }

  public ShiftCodeVersionInput endTime1(String endTime1) {
    this.endTime1 = JsonNullable.of(endTime1);
    return this;
  }

  /**
   * Get endTime1
   * @return endTime1
   */
  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("endTime1")
  public JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> getEndTime1() {
    return endTime1;
  }

  public void setEndTime1(JsonNullable<String> endTime1) {
    this.endTime1 = endTime1;
  }

  public ShiftCodeVersionInput startTime2(String startTime2) {
    this.startTime2 = JsonNullable.of(startTime2);
    return this;
  }

  /**
   * Get startTime2
   * @return startTime2
   */
  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("startTime2")
  public JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> getStartTime2() {
    return startTime2;
  }

  public void setStartTime2(JsonNullable<String> startTime2) {
    this.startTime2 = startTime2;
  }

  public ShiftCodeVersionInput endTime2(String endTime2) {
    this.endTime2 = JsonNullable.of(endTime2);
    return this;
  }

  /**
   * Get endTime2
   * @return endTime2
   */
  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("endTime2")
  public JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> getEndTime2() {
    return endTime2;
  }

  public void setEndTime2(JsonNullable<String> endTime2) {
    this.endTime2 = endTime2;
  }

  public ShiftCodeVersionInput countsAsWorked(Boolean countsAsWorked) {
    this.countsAsWorked = countsAsWorked;
    return this;
  }

  /**
   * Get countsAsWorked
   * @return countsAsWorked
   */
  @NotNull 
  @JsonProperty("countsAsWorked")
  public Boolean getCountsAsWorked() {
    return countsAsWorked;
  }

  public void setCountsAsWorked(Boolean countsAsWorked) {
    this.countsAsWorked = countsAsWorked;
  }

  public ShiftCodeVersionInput isPaid(Boolean isPaid) {
    this.isPaid = isPaid;
    return this;
  }

  /**
   * Get isPaid
   * @return isPaid
   */
  @NotNull 
  @JsonProperty("isPaid")
  public Boolean getIsPaid() {
    return isPaid;
  }

  public void setIsPaid(Boolean isPaid) {
    this.isPaid = isPaid;
  }

  public ShiftCodeVersionInput displayColor(String displayColor) {
    this.displayColor = JsonNullable.of(displayColor);
    return this;
  }

  /**
   * Get displayColor
   * @return displayColor
   */
  @Pattern(regexp = "^#[0-9a-fA-F]{6}$") 
  @JsonProperty("displayColor")
  public JsonNullable<@Pattern(regexp = "^#[0-9a-fA-F]{6}$") String> getDisplayColor() {
    return displayColor;
  }

  public void setDisplayColor(JsonNullable<String> displayColor) {
    this.displayColor = displayColor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftCodeVersionInput shiftCodeVersionInput = (ShiftCodeVersionInput) o;
    return Objects.equals(this.effectiveFrom, shiftCodeVersionInput.effectiveFrom) &&
        Objects.equals(this.kind, shiftCodeVersionInput.kind) &&
        equalsNullable(this.startTime1, shiftCodeVersionInput.startTime1) &&
        equalsNullable(this.endTime1, shiftCodeVersionInput.endTime1) &&
        equalsNullable(this.startTime2, shiftCodeVersionInput.startTime2) &&
        equalsNullable(this.endTime2, shiftCodeVersionInput.endTime2) &&
        Objects.equals(this.countsAsWorked, shiftCodeVersionInput.countsAsWorked) &&
        Objects.equals(this.isPaid, shiftCodeVersionInput.isPaid) &&
        equalsNullable(this.displayColor, shiftCodeVersionInput.displayColor);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(effectiveFrom, kind, hashCodeNullable(startTime1), hashCodeNullable(endTime1), hashCodeNullable(startTime2), hashCodeNullable(endTime2), countsAsWorked, isPaid, hashCodeNullable(displayColor));
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
    sb.append("class ShiftCodeVersionInput {\n");
    sb.append("    effectiveFrom: ").append(toIndentedString(effectiveFrom)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    startTime1: ").append(toIndentedString(startTime1)).append("\n");
    sb.append("    endTime1: ").append(toIndentedString(endTime1)).append("\n");
    sb.append("    startTime2: ").append(toIndentedString(startTime2)).append("\n");
    sb.append("    endTime2: ").append(toIndentedString(endTime2)).append("\n");
    sb.append("    countsAsWorked: ").append(toIndentedString(countsAsWorked)).append("\n");
    sb.append("    isPaid: ").append(toIndentedString(isPaid)).append("\n");
    sb.append("    displayColor: ").append(toIndentedString(displayColor)).append("\n");
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

