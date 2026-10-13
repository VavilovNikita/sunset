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
 * GymTuyaUsage
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymTuyaUsage {

  private String month;

  private Long calls;

  private Integer quotaHint;

  public GymTuyaUsage() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymTuyaUsage(String month, Long calls, Integer quotaHint) {
    this.month = month;
    this.calls = calls;
    this.quotaHint = quotaHint;
  }

  public GymTuyaUsage month(String month) {
    this.month = month;
    return this;
  }

  /**
   * Calendar month in hotel (Bangkok) time.
   * @return month
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}$") 
  @JsonProperty("month")
  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  public GymTuyaUsage calls(Long calls) {
    this.calls = calls;
    return this;
  }

  /**
   * Calls to Tuya this server made in that month, token requests included.
   * @return calls
   */
  @NotNull 
  @JsonProperty("calls")
  public Long getCalls() {
    return calls;
  }

  public void setCalls(Long calls) {
    this.calls = calls;
  }

  public GymTuyaUsage quotaHint(Integer quotaHint) {
    this.quotaHint = quotaHint;
    return this;
  }

  /**
   * The trial's monthly quota as configured (`app.tuya.monthly-quota-hint`) - about 26 000, informational only.
   * @return quotaHint
   */
  @NotNull 
  @JsonProperty("quotaHint")
  public Integer getQuotaHint() {
    return quotaHint;
  }

  public void setQuotaHint(Integer quotaHint) {
    this.quotaHint = quotaHint;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymTuyaUsage gymTuyaUsage = (GymTuyaUsage) o;
    return Objects.equals(this.month, gymTuyaUsage.month) &&
        Objects.equals(this.calls, gymTuyaUsage.calls) &&
        Objects.equals(this.quotaHint, gymTuyaUsage.quotaHint);
  }

  @Override
  public int hashCode() {
    return Objects.hash(month, calls, quotaHint);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymTuyaUsage {\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    calls: ").append(toIndentedString(calls)).append("\n");
    sb.append("    quotaHint: ").append(toIndentedString(quotaHint)).append("\n");
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

