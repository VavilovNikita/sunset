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
 * A &#x60;RoomUnitBlock&#x60; covering today - independent of &#x60;PropertyMapUnit.isActive&#x60;, see that schema&#39;s description for why the two must not be collapsed into one flag. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PropertyMapActiveBlock {

  private String reason;

  private String fromDate;

  private String toDate;

  public PropertyMapActiveBlock() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PropertyMapActiveBlock(String reason, String fromDate, String toDate) {
    this.reason = reason;
    this.fromDate = fromDate;
    this.toDate = toDate;
  }

  public PropertyMapActiveBlock reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Get reason
   * @return reason
   */
  @NotNull 
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public PropertyMapActiveBlock fromDate(String fromDate) {
    this.fromDate = fromDate;
    return this;
  }

  /**
   * Get fromDate
   * @return fromDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("fromDate")
  public String getFromDate() {
    return fromDate;
  }

  public void setFromDate(String fromDate) {
    this.fromDate = fromDate;
  }

  public PropertyMapActiveBlock toDate(String toDate) {
    this.toDate = toDate;
    return this;
  }

  /**
   * Get toDate
   * @return toDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("toDate")
  public String getToDate() {
    return toDate;
  }

  public void setToDate(String toDate) {
    this.toDate = toDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertyMapActiveBlock propertyMapActiveBlock = (PropertyMapActiveBlock) o;
    return Objects.equals(this.reason, propertyMapActiveBlock.reason) &&
        Objects.equals(this.fromDate, propertyMapActiveBlock.fromDate) &&
        Objects.equals(this.toDate, propertyMapActiveBlock.toDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reason, fromDate, toDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertyMapActiveBlock {\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    fromDate: ").append(toIndentedString(fromDate)).append("\n");
    sb.append("    toDate: ").append(toIndentedString(toDate)).append("\n");
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

