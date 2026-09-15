package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.StaffArea;
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
 * One distinct name found in the sheet (trimmed, internal whitespace collapsed - nothing more) and how it currently resolves. Names are never matched automatically - &#x60;mapped: false&#x60; means a person must map this one, via &#x60;POST /roster/import/name-mappings&#x60;, before &#x60;POST /roster/import/commit&#x60; can succeed. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportNameEntry {

  private String rawName;

  private Integer occurrences;

  private Boolean mapped;

  private JsonNullable<String> employeeUserId = JsonNullable.<String>undefined();

  private JsonNullable<String> employeeName = JsonNullable.<String>undefined();

  private JsonNullable<StaffArea> suggestedStaffArea = JsonNullable.<StaffArea>undefined();

  public RosterImportNameEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportNameEntry(String rawName, Integer occurrences, Boolean mapped) {
    this.rawName = rawName;
    this.occurrences = occurrences;
    this.mapped = mapped;
  }

  public RosterImportNameEntry rawName(String rawName) {
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

  public RosterImportNameEntry occurrences(Integer occurrences) {
    this.occurrences = occurrences;
    return this;
  }

  /**
   * How many day-cells in the sheet belong to this name - context for how much a wrong mapping would affect.
   * @return occurrences
   */
  @NotNull 
  @JsonProperty("occurrences")
  public Integer getOccurrences() {
    return occurrences;
  }

  public void setOccurrences(Integer occurrences) {
    this.occurrences = occurrences;
  }

  public RosterImportNameEntry mapped(Boolean mapped) {
    this.mapped = mapped;
    return this;
  }

  /**
   * Get mapped
   * @return mapped
   */
  @NotNull 
  @JsonProperty("mapped")
  public Boolean getMapped() {
    return mapped;
  }

  public void setMapped(Boolean mapped) {
    this.mapped = mapped;
  }

  public RosterImportNameEntry employeeUserId(String employeeUserId) {
    this.employeeUserId = JsonNullable.of(employeeUserId);
    return this;
  }

  /**
   * Set when `mapped` is true - either a previously-remembered mapping, or one already given in this session.
   * @return employeeUserId
   */
  
  @JsonProperty("employeeUserId")
  public JsonNullable<String> getEmployeeUserId() {
    return employeeUserId;
  }

  public void setEmployeeUserId(JsonNullable<String> employeeUserId) {
    this.employeeUserId = employeeUserId;
  }

  public RosterImportNameEntry employeeName(String employeeName) {
    this.employeeName = JsonNullable.of(employeeName);
    return this;
  }

  /**
   * Get employeeName
   * @return employeeName
   */
  
  @JsonProperty("employeeName")
  public JsonNullable<String> getEmployeeName() {
    return employeeName;
  }

  public void setEmployeeName(JsonNullable<String> employeeName) {
    this.employeeName = employeeName;
  }

  public RosterImportNameEntry suggestedStaffArea(StaffArea suggestedStaffArea) {
    this.suggestedStaffArea = JsonNullable.of(suggestedStaffArea);
    return this;
  }

  /**
   * The department this name's row sits under, for context only (e.g. pre-filling which area a newly-created account is shown as belonging to) - never persisted anywhere by the import itself. Null only if the department header above this row couldn't itself be recognised, in which case this row also carries an entry in `issues`. 
   * @return suggestedStaffArea
   */
  @Valid 
  @JsonProperty("suggestedStaffArea")
  public JsonNullable<StaffArea> getSuggestedStaffArea() {
    return suggestedStaffArea;
  }

  public void setSuggestedStaffArea(JsonNullable<StaffArea> suggestedStaffArea) {
    this.suggestedStaffArea = suggestedStaffArea;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportNameEntry rosterImportNameEntry = (RosterImportNameEntry) o;
    return Objects.equals(this.rawName, rosterImportNameEntry.rawName) &&
        Objects.equals(this.occurrences, rosterImportNameEntry.occurrences) &&
        Objects.equals(this.mapped, rosterImportNameEntry.mapped) &&
        equalsNullable(this.employeeUserId, rosterImportNameEntry.employeeUserId) &&
        equalsNullable(this.employeeName, rosterImportNameEntry.employeeName) &&
        equalsNullable(this.suggestedStaffArea, rosterImportNameEntry.suggestedStaffArea);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawName, occurrences, mapped, hashCodeNullable(employeeUserId), hashCodeNullable(employeeName), hashCodeNullable(suggestedStaffArea));
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
    sb.append("class RosterImportNameEntry {\n");
    sb.append("    rawName: ").append(toIndentedString(rawName)).append("\n");
    sb.append("    occurrences: ").append(toIndentedString(occurrences)).append("\n");
    sb.append("    mapped: ").append(toIndentedString(mapped)).append("\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    employeeName: ").append(toIndentedString(employeeName)).append("\n");
    sb.append("    suggestedStaffArea: ").append(toIndentedString(suggestedStaffArea)).append("\n");
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

