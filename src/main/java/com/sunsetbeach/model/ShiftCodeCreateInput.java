package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
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
 * Body of &#x60;POST /shift-codes&#x60;. &#x60;staffArea&#x60; is optional - omit it (or send it explicitly &#x60;null&#x60;) to define a code shared across every area; a code scoped to a real &#x60;StaffArea&#x60; takes precedence over a shared one of the same &#x60;code&#x60; string for that area only, without retiring the shared row (see &#x60;ShiftCode&#x60;&#39;s own description). Unlike every other field here, &#x60;kind&#x60; (required) is never versioned by creating a new row for it alone - see &#x60;PATCH /shift-codes/{id}/kind&#x60; for correcting one in place. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftCodeCreateInput {

  private StaffArea staffArea;

  private String code;

  private ShiftCodeKind kind;

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> startTime1 = JsonNullable.<String>undefined();

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> endTime1 = JsonNullable.<String>undefined();

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> startTime2 = JsonNullable.<String>undefined();

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> endTime2 = JsonNullable.<String>undefined();

  private Boolean countsAsWorked;

  private Boolean isPaid;

  private String effectiveFrom;

  public ShiftCodeCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftCodeCreateInput(String code, ShiftCodeKind kind, Boolean countsAsWorked, Boolean isPaid, String effectiveFrom) {
    this.code = code;
    this.kind = kind;
    this.countsAsWorked = countsAsWorked;
    this.isPaid = isPaid;
    this.effectiveFrom = effectiveFrom;
  }

  public ShiftCodeCreateInput staffArea(StaffArea staffArea) {
    this.staffArea = staffArea;
    return this;
  }

  /**
   * Get staffArea
   * @return staffArea
   */
  @Valid 
  @JsonProperty("staffArea")
  public StaffArea getStaffArea() {
    return staffArea;
  }

  public void setStaffArea(StaffArea staffArea) {
    this.staffArea = staffArea;
  }

  public ShiftCodeCreateInput code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
   */
  @NotNull 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public ShiftCodeCreateInput kind(ShiftCodeKind kind) {
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

  public ShiftCodeCreateInput startTime1(String startTime1) {
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

  public ShiftCodeCreateInput endTime1(String endTime1) {
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

  public ShiftCodeCreateInput startTime2(String startTime2) {
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

  public ShiftCodeCreateInput endTime2(String endTime2) {
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

  public ShiftCodeCreateInput countsAsWorked(Boolean countsAsWorked) {
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

  public ShiftCodeCreateInput isPaid(Boolean isPaid) {
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

  public ShiftCodeCreateInput effectiveFrom(String effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
    return this;
  }

  /**
   * Get effectiveFrom
   * @return effectiveFrom
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("effectiveFrom")
  public String getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(String effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftCodeCreateInput shiftCodeCreateInput = (ShiftCodeCreateInput) o;
    return Objects.equals(this.staffArea, shiftCodeCreateInput.staffArea) &&
        Objects.equals(this.code, shiftCodeCreateInput.code) &&
        Objects.equals(this.kind, shiftCodeCreateInput.kind) &&
        equalsNullable(this.startTime1, shiftCodeCreateInput.startTime1) &&
        equalsNullable(this.endTime1, shiftCodeCreateInput.endTime1) &&
        equalsNullable(this.startTime2, shiftCodeCreateInput.startTime2) &&
        equalsNullable(this.endTime2, shiftCodeCreateInput.endTime2) &&
        Objects.equals(this.countsAsWorked, shiftCodeCreateInput.countsAsWorked) &&
        Objects.equals(this.isPaid, shiftCodeCreateInput.isPaid) &&
        Objects.equals(this.effectiveFrom, shiftCodeCreateInput.effectiveFrom);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(staffArea, code, kind, hashCodeNullable(startTime1), hashCodeNullable(endTime1), hashCodeNullable(startTime2), hashCodeNullable(endTime2), countsAsWorked, isPaid, effectiveFrom);
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
    sb.append("class ShiftCodeCreateInput {\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    startTime1: ").append(toIndentedString(startTime1)).append("\n");
    sb.append("    endTime1: ").append(toIndentedString(endTime1)).append("\n");
    sb.append("    startTime2: ").append(toIndentedString(startTime2)).append("\n");
    sb.append("    endTime2: ").append(toIndentedString(endTime2)).append("\n");
    sb.append("    countsAsWorked: ").append(toIndentedString(countsAsWorked)).append("\n");
    sb.append("    isPaid: ").append(toIndentedString(isPaid)).append("\n");
    sb.append("    effectiveFrom: ").append(toIndentedString(effectiveFrom)).append("\n");
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

