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
 * A cell whose date already has a &#x60;RosterEntry&#x60; for that employee. Never overwritten - the import always skips these and reports them here, the same way &#x60;POST /roster/entries&#x60; itself refuses a second entry on an occupied date rather than replacing it. Resolving a collision, if the new value should actually win, means clearing or editing the existing entry by hand (in the ordinary roster grid) and re-running the import. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportCollision {

  private String employeeName;

  private String date;

  private String existingShiftCodeDescription;

  private String newShiftCodeDescription;

  public RosterImportCollision() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportCollision(String employeeName, String date, String existingShiftCodeDescription, String newShiftCodeDescription) {
    this.employeeName = employeeName;
    this.date = date;
    this.existingShiftCodeDescription = existingShiftCodeDescription;
    this.newShiftCodeDescription = newShiftCodeDescription;
  }

  public RosterImportCollision employeeName(String employeeName) {
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

  public RosterImportCollision date(String date) {
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

  public RosterImportCollision existingShiftCodeDescription(String existingShiftCodeDescription) {
    this.existingShiftCodeDescription = existingShiftCodeDescription;
    return this;
  }

  /**
   * Get existingShiftCodeDescription
   * @return existingShiftCodeDescription
   */
  @NotNull 
  @JsonProperty("existingShiftCodeDescription")
  public String getExistingShiftCodeDescription() {
    return existingShiftCodeDescription;
  }

  public void setExistingShiftCodeDescription(String existingShiftCodeDescription) {
    this.existingShiftCodeDescription = existingShiftCodeDescription;
  }

  public RosterImportCollision newShiftCodeDescription(String newShiftCodeDescription) {
    this.newShiftCodeDescription = newShiftCodeDescription;
    return this;
  }

  /**
   * Get newShiftCodeDescription
   * @return newShiftCodeDescription
   */
  @NotNull 
  @JsonProperty("newShiftCodeDescription")
  public String getNewShiftCodeDescription() {
    return newShiftCodeDescription;
  }

  public void setNewShiftCodeDescription(String newShiftCodeDescription) {
    this.newShiftCodeDescription = newShiftCodeDescription;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportCollision rosterImportCollision = (RosterImportCollision) o;
    return Objects.equals(this.employeeName, rosterImportCollision.employeeName) &&
        Objects.equals(this.date, rosterImportCollision.date) &&
        Objects.equals(this.existingShiftCodeDescription, rosterImportCollision.existingShiftCodeDescription) &&
        Objects.equals(this.newShiftCodeDescription, rosterImportCollision.newShiftCodeDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeName, date, existingShiftCodeDescription, newShiftCodeDescription);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterImportCollision {\n");
    sb.append("    employeeName: ").append(toIndentedString(employeeName)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    existingShiftCodeDescription: ").append(toIndentedString(existingShiftCodeDescription)).append("\n");
    sb.append("    newShiftCodeDescription: ").append(toIndentedString(newShiftCodeDescription)).append("\n");
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

