package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.StaffArea;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * How many people of the employee&#39;s department (&#x60;User.staffArea&#x60;) would still work on the day(s) concerned if the request were approved, against the department&#39;s minimum (&#x60;StaffAreaCoverageRule&#x60;). Counted exactly like the roster grid&#39;s coverage. A warning for the decider, never a block. Absent when the employee has no department or the department has no minimum. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftRequestCoverage {

  private StaffArea staffArea;

  private Integer minimumWorking;

  private String sourceDate;

  private Integer sourceWorkingAfter;

  private String targetDate;

  private Integer targetWorkingAfter;

  private Boolean belowMinimum;

  public ShiftRequestCoverage() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftRequestCoverage(StaffArea staffArea, Integer minimumWorking, String sourceDate, Integer sourceWorkingAfter, Boolean belowMinimum) {
    this.staffArea = staffArea;
    this.minimumWorking = minimumWorking;
    this.sourceDate = sourceDate;
    this.sourceWorkingAfter = sourceWorkingAfter;
    this.belowMinimum = belowMinimum;
  }

  public ShiftRequestCoverage staffArea(StaffArea staffArea) {
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

  public ShiftRequestCoverage minimumWorking(Integer minimumWorking) {
    this.minimumWorking = minimumWorking;
    return this;
  }

  /**
   * Get minimumWorking
   * @return minimumWorking
   */
  @NotNull 
  @JsonProperty("minimumWorking")
  public Integer getMinimumWorking() {
    return minimumWorking;
  }

  public void setMinimumWorking(Integer minimumWorking) {
    this.minimumWorking = minimumWorking;
  }

  public ShiftRequestCoverage sourceDate(String sourceDate) {
    this.sourceDate = sourceDate;
    return this;
  }

  /**
   * Get sourceDate
   * @return sourceDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("sourceDate")
  public String getSourceDate() {
    return sourceDate;
  }

  public void setSourceDate(String sourceDate) {
    this.sourceDate = sourceDate;
  }

  public ShiftRequestCoverage sourceWorkingAfter(Integer sourceWorkingAfter) {
    this.sourceWorkingAfter = sourceWorkingAfter;
    return this;
  }

  /**
   * Working on `sourceDate` after the employee leaves it.
   * @return sourceWorkingAfter
   */
  @NotNull 
  @JsonProperty("sourceWorkingAfter")
  public Integer getSourceWorkingAfter() {
    return sourceWorkingAfter;
  }

  public void setSourceWorkingAfter(Integer sourceWorkingAfter) {
    this.sourceWorkingAfter = sourceWorkingAfter;
  }

  public ShiftRequestCoverage targetDate(String targetDate) {
    this.targetDate = targetDate;
    return this;
  }

  /**
   * Get targetDate
   * @return targetDate
   */
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("targetDate")
  public String getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(String targetDate) {
    this.targetDate = targetDate;
  }

  public ShiftRequestCoverage targetWorkingAfter(Integer targetWorkingAfter) {
    this.targetWorkingAfter = targetWorkingAfter;
    return this;
  }

  /**
   * Working on `targetDate` after the employee joins it (MOVE only).
   * @return targetWorkingAfter
   */
  
  @JsonProperty("targetWorkingAfter")
  public Integer getTargetWorkingAfter() {
    return targetWorkingAfter;
  }

  public void setTargetWorkingAfter(Integer targetWorkingAfter) {
    this.targetWorkingAfter = targetWorkingAfter;
  }

  public ShiftRequestCoverage belowMinimum(Boolean belowMinimum) {
    this.belowMinimum = belowMinimum;
    return this;
  }

  /**
   * True when either day would be below the minimum.
   * @return belowMinimum
   */
  @NotNull 
  @JsonProperty("belowMinimum")
  public Boolean getBelowMinimum() {
    return belowMinimum;
  }

  public void setBelowMinimum(Boolean belowMinimum) {
    this.belowMinimum = belowMinimum;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftRequestCoverage shiftRequestCoverage = (ShiftRequestCoverage) o;
    return Objects.equals(this.staffArea, shiftRequestCoverage.staffArea) &&
        Objects.equals(this.minimumWorking, shiftRequestCoverage.minimumWorking) &&
        Objects.equals(this.sourceDate, shiftRequestCoverage.sourceDate) &&
        Objects.equals(this.sourceWorkingAfter, shiftRequestCoverage.sourceWorkingAfter) &&
        Objects.equals(this.targetDate, shiftRequestCoverage.targetDate) &&
        Objects.equals(this.targetWorkingAfter, shiftRequestCoverage.targetWorkingAfter) &&
        Objects.equals(this.belowMinimum, shiftRequestCoverage.belowMinimum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(staffArea, minimumWorking, sourceDate, sourceWorkingAfter, targetDate, targetWorkingAfter, belowMinimum);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShiftRequestCoverage {\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
    sb.append("    minimumWorking: ").append(toIndentedString(minimumWorking)).append("\n");
    sb.append("    sourceDate: ").append(toIndentedString(sourceDate)).append("\n");
    sb.append("    sourceWorkingAfter: ").append(toIndentedString(sourceWorkingAfter)).append("\n");
    sb.append("    targetDate: ").append(toIndentedString(targetDate)).append("\n");
    sb.append("    targetWorkingAfter: ").append(toIndentedString(targetWorkingAfter)).append("\n");
    sb.append("    belowMinimum: ").append(toIndentedString(belowMinimum)).append("\n");
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

