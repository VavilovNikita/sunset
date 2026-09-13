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
 * One &#x60;(staffArea, date)&#x60; below its &#x60;StaffAreaCoverageRule.minimumWorking&#x60; - a warning, never a reason a roster can&#39;t be saved.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterCoverageWarning {

  private StaffArea staffArea;

  private String date;

  private Integer workingCount;

  private Integer minimumWorking;

  public RosterCoverageWarning() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterCoverageWarning(StaffArea staffArea, String date, Integer workingCount, Integer minimumWorking) {
    this.staffArea = staffArea;
    this.date = date;
    this.workingCount = workingCount;
    this.minimumWorking = minimumWorking;
  }

  public RosterCoverageWarning staffArea(StaffArea staffArea) {
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

  public RosterCoverageWarning date(String date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("date")
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public RosterCoverageWarning workingCount(Integer workingCount) {
    this.workingCount = workingCount;
    return this;
  }

  /**
   * Get workingCount
   * @return workingCount
   */
  @NotNull 
  @JsonProperty("workingCount")
  public Integer getWorkingCount() {
    return workingCount;
  }

  public void setWorkingCount(Integer workingCount) {
    this.workingCount = workingCount;
  }

  public RosterCoverageWarning minimumWorking(Integer minimumWorking) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterCoverageWarning rosterCoverageWarning = (RosterCoverageWarning) o;
    return Objects.equals(this.staffArea, rosterCoverageWarning.staffArea) &&
        Objects.equals(this.date, rosterCoverageWarning.date) &&
        Objects.equals(this.workingCount, rosterCoverageWarning.workingCount) &&
        Objects.equals(this.minimumWorking, rosterCoverageWarning.minimumWorking);
  }

  @Override
  public int hashCode() {
    return Objects.hash(staffArea, date, workingCount, minimumWorking);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterCoverageWarning {\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    workingCount: ").append(toIndentedString(workingCount)).append("\n");
    sb.append("    minimumWorking: ").append(toIndentedString(minimumWorking)).append("\n");
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

