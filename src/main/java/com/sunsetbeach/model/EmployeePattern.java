package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.Weekday;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An employee&#39;s normal roster shape - what &#x60;POST /roster/generate&#x60; reads. &#x60;defaultShiftCodeId&#x60; is nullable: some employees genuinely have no single \&quot;usual\&quot; code (several rows in the source spreadsheet had none), so generation simply leaves their dates blank for the manager to fill by hand in that case. &#x60;staffArea&#x60; here is a read-through of &#x60;User.staffArea&#x60; (see that field&#39;s own description) - the area is a fact about the person, stored once, not duplicated onto every pattern; &#x60;PUT /employee-patterns/{employeeUserId}&#x60; writes it to the &#x60;User&#x60; row, not to a column of its own here. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class EmployeePattern {

  private String employeeUserId;

  private String employeeName;

  private String employeeEmail;

  private StaffArea staffArea;

  private JsonNullable<String> defaultShiftCodeId = JsonNullable.<String>undefined();

  private Integer workDaysPerWeek;

  private Weekday weeklyDayOff;

  private String updatedByEmail;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public EmployeePattern() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmployeePattern(String employeeUserId, String employeeName, StaffArea staffArea, Integer workDaysPerWeek, Weekday weeklyDayOff, String updatedByEmail, OffsetDateTime updatedAt) {
    this.employeeUserId = employeeUserId;
    this.employeeName = employeeName;
    this.staffArea = staffArea;
    this.workDaysPerWeek = workDaysPerWeek;
    this.weeklyDayOff = weeklyDayOff;
    this.updatedByEmail = updatedByEmail;
    this.updatedAt = updatedAt;
  }

  public EmployeePattern employeeUserId(String employeeUserId) {
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

  public EmployeePattern employeeName(String employeeName) {
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

  public EmployeePattern employeeEmail(String employeeEmail) {
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

  public EmployeePattern staffArea(StaffArea staffArea) {
    this.staffArea = staffArea;
    return this;
  }

  /**
   * Get staffArea
   * @return staffArea
   */
  @NotNull @Valid 
  @JsonProperty("staffArea")
  public StaffArea getStaffArea() {
    return staffArea;
  }

  public void setStaffArea(StaffArea staffArea) {
    this.staffArea = staffArea;
  }

  public EmployeePattern defaultShiftCodeId(String defaultShiftCodeId) {
    this.defaultShiftCodeId = JsonNullable.of(defaultShiftCodeId);
    return this;
  }

  /**
   * Get defaultShiftCodeId
   * @return defaultShiftCodeId
   */
  
  @JsonProperty("defaultShiftCodeId")
  public JsonNullable<String> getDefaultShiftCodeId() {
    return defaultShiftCodeId;
  }

  public void setDefaultShiftCodeId(JsonNullable<String> defaultShiftCodeId) {
    this.defaultShiftCodeId = defaultShiftCodeId;
  }

  public EmployeePattern workDaysPerWeek(Integer workDaysPerWeek) {
    this.workDaysPerWeek = workDaysPerWeek;
    return this;
  }

  /**
   * Get workDaysPerWeek
   * minimum: 0
   * maximum: 7
   * @return workDaysPerWeek
   */
  @NotNull @Min(0) @Max(7) 
  @JsonProperty("workDaysPerWeek")
  public Integer getWorkDaysPerWeek() {
    return workDaysPerWeek;
  }

  public void setWorkDaysPerWeek(Integer workDaysPerWeek) {
    this.workDaysPerWeek = workDaysPerWeek;
  }

  public EmployeePattern weeklyDayOff(Weekday weeklyDayOff) {
    this.weeklyDayOff = weeklyDayOff;
    return this;
  }

  /**
   * Get weeklyDayOff
   * @return weeklyDayOff
   */
  @NotNull @Valid 
  @JsonProperty("weeklyDayOff")
  public Weekday getWeeklyDayOff() {
    return weeklyDayOff;
  }

  public void setWeeklyDayOff(Weekday weeklyDayOff) {
    this.weeklyDayOff = weeklyDayOff;
  }

  public EmployeePattern updatedByEmail(String updatedByEmail) {
    this.updatedByEmail = updatedByEmail;
    return this;
  }

  /**
   * Get updatedByEmail
   * @return updatedByEmail
   */
  @NotNull 
  @JsonProperty("updatedByEmail")
  public String getUpdatedByEmail() {
    return updatedByEmail;
  }

  public void setUpdatedByEmail(String updatedByEmail) {
    this.updatedByEmail = updatedByEmail;
  }

  public EmployeePattern updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmployeePattern employeePattern = (EmployeePattern) o;
    return Objects.equals(this.employeeUserId, employeePattern.employeeUserId) &&
        Objects.equals(this.employeeName, employeePattern.employeeName) &&
        Objects.equals(this.employeeEmail, employeePattern.employeeEmail) &&
        Objects.equals(this.staffArea, employeePattern.staffArea) &&
        equalsNullable(this.defaultShiftCodeId, employeePattern.defaultShiftCodeId) &&
        Objects.equals(this.workDaysPerWeek, employeePattern.workDaysPerWeek) &&
        Objects.equals(this.weeklyDayOff, employeePattern.weeklyDayOff) &&
        Objects.equals(this.updatedByEmail, employeePattern.updatedByEmail) &&
        Objects.equals(this.updatedAt, employeePattern.updatedAt);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeUserId, employeeName, employeeEmail, staffArea, hashCodeNullable(defaultShiftCodeId), workDaysPerWeek, weeklyDayOff, updatedByEmail, updatedAt);
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
    sb.append("class EmployeePattern {\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    employeeName: ").append(toIndentedString(employeeName)).append("\n");
    sb.append("    employeeEmail: ").append(toIndentedString(employeeEmail)).append("\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
    sb.append("    defaultShiftCodeId: ").append(toIndentedString(defaultShiftCodeId)).append("\n");
    sb.append("    workDaysPerWeek: ").append(toIndentedString(workDaysPerWeek)).append("\n");
    sb.append("    weeklyDayOff: ").append(toIndentedString(weeklyDayOff)).append("\n");
    sb.append("    updatedByEmail: ").append(toIndentedString(updatedByEmail)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

