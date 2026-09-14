package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One version of an employee&#39;s daily rate - never edited, only superseded, the same \&quot;agreed terms are frozen\&quot; shape as &#x60;ShiftCode&#x60; and &#x60;BookingSegmentNightlyRate&#x60;, so a rate change partway through a month prices each day correctly against whichever rate was actually in effect that day. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class EmployeePayRate {

  private String id;

  private String employeeUserId;

  private String employeeName;

  private String employeeEmail;

  private String dailyRate;

  private String effectiveFrom;

  private String createdByEmail;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public EmployeePayRate() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmployeePayRate(String id, String employeeUserId, String employeeName, String dailyRate, String effectiveFrom, String createdByEmail, OffsetDateTime createdAt) {
    this.id = id;
    this.employeeUserId = employeeUserId;
    this.employeeName = employeeName;
    this.dailyRate = dailyRate;
    this.effectiveFrom = effectiveFrom;
    this.createdByEmail = createdByEmail;
    this.createdAt = createdAt;
  }

  public EmployeePayRate id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public EmployeePayRate employeeUserId(String employeeUserId) {
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

  public EmployeePayRate employeeName(String employeeName) {
    this.employeeName = employeeName;
    return this;
  }

  /**
   * Get employeeName
   * @return employeeName
   */
  @NotNull 
  @JsonProperty("employeeName")
  public String getEmployeeName() {
    return employeeName;
  }

  public void setEmployeeName(String employeeName) {
    this.employeeName = employeeName;
  }

  public EmployeePayRate employeeEmail(String employeeEmail) {
    this.employeeEmail = employeeEmail;
    return this;
  }

  /**
   * Get employeeEmail
   * @return employeeEmail
   */
  
  @JsonProperty("employeeEmail")
  public String getEmployeeEmail() {
    return employeeEmail;
  }

  public void setEmployeeEmail(String employeeEmail) {
    this.employeeEmail = employeeEmail;
  }

  public EmployeePayRate dailyRate(String dailyRate) {
    this.dailyRate = dailyRate;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
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

  public EmployeePayRate effectiveFrom(String effectiveFrom) {
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

  public EmployeePayRate createdByEmail(String createdByEmail) {
    this.createdByEmail = createdByEmail;
    return this;
  }

  /**
   * Get createdByEmail
   * @return createdByEmail
   */
  @NotNull 
  @JsonProperty("createdByEmail")
  public String getCreatedByEmail() {
    return createdByEmail;
  }

  public void setCreatedByEmail(String createdByEmail) {
    this.createdByEmail = createdByEmail;
  }

  public EmployeePayRate createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmployeePayRate employeePayRate = (EmployeePayRate) o;
    return Objects.equals(this.id, employeePayRate.id) &&
        Objects.equals(this.employeeUserId, employeePayRate.employeeUserId) &&
        Objects.equals(this.employeeName, employeePayRate.employeeName) &&
        Objects.equals(this.employeeEmail, employeePayRate.employeeEmail) &&
        Objects.equals(this.dailyRate, employeePayRate.dailyRate) &&
        Objects.equals(this.effectiveFrom, employeePayRate.effectiveFrom) &&
        Objects.equals(this.createdByEmail, employeePayRate.createdByEmail) &&
        Objects.equals(this.createdAt, employeePayRate.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, employeeUserId, employeeName, employeeEmail, dailyRate, effectiveFrom, createdByEmail, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmployeePayRate {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    employeeName: ").append(toIndentedString(employeeName)).append("\n");
    sb.append("    employeeEmail: ").append(toIndentedString(employeeEmail)).append("\n");
    sb.append("    dailyRate: ").append(toIndentedString(dailyRate)).append("\n");
    sb.append("    effectiveFrom: ").append(toIndentedString(effectiveFrom)).append("\n");
    sb.append("    createdByEmail: ").append(toIndentedString(createdByEmail)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

