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
 * Body of &#x60;POST /roster/import/name-mappings&#x60;. Exactly one of &#x60;employeeUserId&#x60; (map to an existing account) or &#x60;newEmployeeName&#x60; (create a no-login account on the spot) must be given - never both, never neither. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportNameMappingInput {

  private String rawName;

  private String employeeUserId;

  private String newEmployeeName;

  public RosterImportNameMappingInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportNameMappingInput(String rawName) {
    this.rawName = rawName;
  }

  public RosterImportNameMappingInput rawName(String rawName) {
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

  public RosterImportNameMappingInput employeeUserId(String employeeUserId) {
    this.employeeUserId = employeeUserId;
    return this;
  }

  /**
   * Get employeeUserId
   * @return employeeUserId
   */
  
  @JsonProperty("employeeUserId")
  public String getEmployeeUserId() {
    return employeeUserId;
  }

  public void setEmployeeUserId(String employeeUserId) {
    this.employeeUserId = employeeUserId;
  }

  public RosterImportNameMappingInput newEmployeeName(String newEmployeeName) {
    this.newEmployeeName = newEmployeeName;
    return this;
  }

  /**
   * Get newEmployeeName
   * @return newEmployeeName
   */
  
  @JsonProperty("newEmployeeName")
  public String getNewEmployeeName() {
    return newEmployeeName;
  }

  public void setNewEmployeeName(String newEmployeeName) {
    this.newEmployeeName = newEmployeeName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportNameMappingInput rosterImportNameMappingInput = (RosterImportNameMappingInput) o;
    return Objects.equals(this.rawName, rosterImportNameMappingInput.rawName) &&
        Objects.equals(this.employeeUserId, rosterImportNameMappingInput.employeeUserId) &&
        Objects.equals(this.newEmployeeName, rosterImportNameMappingInput.newEmployeeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawName, employeeUserId, newEmployeeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterImportNameMappingInput {\n");
    sb.append("    rawName: ").append(toIndentedString(rawName)).append("\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    newEmployeeName: ").append(toIndentedString(newEmployeeName)).append("\n");
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

