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
 * RosterImportNameMappingResult
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportNameMappingResult {

  private String rawName;

  private String employeeUserId;

  private String employeeName;

  public RosterImportNameMappingResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportNameMappingResult(String rawName, String employeeUserId, String employeeName) {
    this.rawName = rawName;
    this.employeeUserId = employeeUserId;
    this.employeeName = employeeName;
  }

  public RosterImportNameMappingResult rawName(String rawName) {
    this.rawName = rawName;
    return this;
  }

  /**
   * Get rawName
   * @return rawName
   */
  @NotNull 
  @JsonProperty("rawName")
  public String getRawName() {
    return rawName;
  }

  public void setRawName(String rawName) {
    this.rawName = rawName;
  }

  public RosterImportNameMappingResult employeeUserId(String employeeUserId) {
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

  public RosterImportNameMappingResult employeeName(String employeeName) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportNameMappingResult rosterImportNameMappingResult = (RosterImportNameMappingResult) o;
    return Objects.equals(this.rawName, rosterImportNameMappingResult.rawName) &&
        Objects.equals(this.employeeUserId, rosterImportNameMappingResult.employeeUserId) &&
        Objects.equals(this.employeeName, rosterImportNameMappingResult.employeeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawName, employeeUserId, employeeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterImportNameMappingResult {\n");
    sb.append("    rawName: ").append(toIndentedString(rawName)).append("\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    employeeName: ").append(toIndentedString(employeeName)).append("\n");
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

