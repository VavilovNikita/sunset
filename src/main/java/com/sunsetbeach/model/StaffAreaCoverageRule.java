package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.StaffArea;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * StaffAreaCoverageRule
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class StaffAreaCoverageRule {

  private StaffArea staffArea;

  private Integer minimumWorking;

  private String updatedByEmail;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public StaffAreaCoverageRule() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StaffAreaCoverageRule(StaffArea staffArea, Integer minimumWorking, String updatedByEmail, OffsetDateTime updatedAt) {
    this.staffArea = staffArea;
    this.minimumWorking = minimumWorking;
    this.updatedByEmail = updatedByEmail;
    this.updatedAt = updatedAt;
  }

  public StaffAreaCoverageRule staffArea(StaffArea staffArea) {
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

  public StaffAreaCoverageRule minimumWorking(Integer minimumWorking) {
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

  public StaffAreaCoverageRule updatedByEmail(String updatedByEmail) {
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

  public StaffAreaCoverageRule updatedAt(OffsetDateTime updatedAt) {
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
    StaffAreaCoverageRule staffAreaCoverageRule = (StaffAreaCoverageRule) o;
    return Objects.equals(this.staffArea, staffAreaCoverageRule.staffArea) &&
        Objects.equals(this.minimumWorking, staffAreaCoverageRule.minimumWorking) &&
        Objects.equals(this.updatedByEmail, staffAreaCoverageRule.updatedByEmail) &&
        Objects.equals(this.updatedAt, staffAreaCoverageRule.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(staffArea, minimumWorking, updatedByEmail, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffAreaCoverageRule {\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
    sb.append("    minimumWorking: ").append(toIndentedString(minimumWorking)).append("\n");
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

