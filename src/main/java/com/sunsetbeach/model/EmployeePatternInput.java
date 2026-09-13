package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.Weekday;
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
 * Body of &#x60;PUT /employee-patterns/{employeeUserId}&#x60;. Full replacement.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class EmployeePatternInput {

  private StaffArea staffArea;

  private JsonNullable<String> defaultShiftCodeId = JsonNullable.<String>undefined();

  private Integer workDaysPerWeek;

  private Weekday weeklyDayOff;

  public EmployeePatternInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmployeePatternInput(StaffArea staffArea, Integer workDaysPerWeek, Weekday weeklyDayOff) {
    this.staffArea = staffArea;
    this.workDaysPerWeek = workDaysPerWeek;
    this.weeklyDayOff = weeklyDayOff;
  }

  public EmployeePatternInput staffArea(StaffArea staffArea) {
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

  public EmployeePatternInput defaultShiftCodeId(String defaultShiftCodeId) {
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

  public EmployeePatternInput workDaysPerWeek(Integer workDaysPerWeek) {
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

  public EmployeePatternInput weeklyDayOff(Weekday weeklyDayOff) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmployeePatternInput employeePatternInput = (EmployeePatternInput) o;
    return Objects.equals(this.staffArea, employeePatternInput.staffArea) &&
        equalsNullable(this.defaultShiftCodeId, employeePatternInput.defaultShiftCodeId) &&
        Objects.equals(this.workDaysPerWeek, employeePatternInput.workDaysPerWeek) &&
        Objects.equals(this.weeklyDayOff, employeePatternInput.weeklyDayOff);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(staffArea, hashCodeNullable(defaultShiftCodeId), workDaysPerWeek, weeklyDayOff);
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
    sb.append("class EmployeePatternInput {\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
    sb.append("    defaultShiftCodeId: ").append(toIndentedString(defaultShiftCodeId)).append("\n");
    sb.append("    workDaysPerWeek: ").append(toIndentedString(workDaysPerWeek)).append("\n");
    sb.append("    weeklyDayOff: ").append(toIndentedString(weeklyDayOff)).append("\n");
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

