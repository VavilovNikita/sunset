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
 * Body of &#x60;POST /employee-pay-rates&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class EmployeePayRateCreateInput {

  private String employeeUserId;

  private String dailyRate;

  private String effectiveFrom;

  public EmployeePayRateCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmployeePayRateCreateInput(String employeeUserId, String dailyRate, String effectiveFrom) {
    this.employeeUserId = employeeUserId;
    this.dailyRate = dailyRate;
    this.effectiveFrom = effectiveFrom;
  }

  public EmployeePayRateCreateInput employeeUserId(String employeeUserId) {
    this.employeeUserId = employeeUserId;
    return this;
  }

  /**
   * Get employeeUserId
   * @return employeeUserId
   */
  @NotNull 
  @JsonProperty("employeeUserId")
  public String getEmployeeUserId() {
    return employeeUserId;
  }

  public void setEmployeeUserId(String employeeUserId) {
    this.employeeUserId = employeeUserId;
  }

  public EmployeePayRateCreateInput dailyRate(String dailyRate) {
    this.dailyRate = dailyRate;
    return this;
  }

  /**
   * Get dailyRate
   * @return dailyRate
   */
  @NotNull 
  @JsonProperty("dailyRate")
  public String getDailyRate() {
    return dailyRate;
  }

  public void setDailyRate(String dailyRate) {
    this.dailyRate = dailyRate;
  }

  public EmployeePayRateCreateInput effectiveFrom(String effectiveFrom) {
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
    EmployeePayRateCreateInput employeePayRateCreateInput = (EmployeePayRateCreateInput) o;
    return Objects.equals(this.employeeUserId, employeePayRateCreateInput.employeeUserId) &&
        Objects.equals(this.dailyRate, employeePayRateCreateInput.dailyRate) &&
        Objects.equals(this.effectiveFrom, employeePayRateCreateInput.effectiveFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeUserId, dailyRate, effectiveFrom);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmployeePayRateCreateInput {\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    dailyRate: ").append(toIndentedString(dailyRate)).append("\n");
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

