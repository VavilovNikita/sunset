package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RosterCoverageWarning;
import com.sunsetbeach.model.RosterEmployee;
import com.sunsetbeach.model.RosterEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;GET /roster&#x60; and &#x60;POST /roster/generate&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterMonth {

  private Integer year;

  private Integer month;

  @Valid
  private List<@Valid RosterEntry> entries = new ArrayList<>();

  @Valid
  private List<@Valid RosterEmployee> employees = new ArrayList<>();

  @Valid
  private List<@Valid RosterCoverageWarning> coverageWarnings = new ArrayList<>();

  public RosterMonth() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterMonth(Integer year, Integer month, List<@Valid RosterEntry> entries, List<@Valid RosterEmployee> employees, List<@Valid RosterCoverageWarning> coverageWarnings) {
    this.year = year;
    this.month = month;
    this.entries = entries;
    this.employees = employees;
    this.coverageWarnings = coverageWarnings;
  }

  public RosterMonth year(Integer year) {
    this.year = year;
    return this;
  }

  /**
   * Get year
   * @return year
   */
  @NotNull 
  @JsonProperty("year")
  public Integer getYear() {
    return year;
  }

  public void setYear(Integer year) {
    this.year = year;
  }

  public RosterMonth month(Integer month) {
    this.month = month;
    return this;
  }

  /**
   * Get month
   * @return month
   */
  @NotNull 
  @JsonProperty("month")
  public Integer getMonth() {
    return month;
  }

  public void setMonth(Integer month) {
    this.month = month;
  }

  public RosterMonth entries(List<@Valid RosterEntry> entries) {
    this.entries = entries;
    return this;
  }

  public RosterMonth addEntriesItem(RosterEntry entriesItem) {
    if (this.entries == null) {
      this.entries = new ArrayList<>();
    }
    this.entries.add(entriesItem);
    return this;
  }

  /**
   * Get entries
   * @return entries
   */
  @NotNull @Valid 
  @JsonProperty("entries")
  public List<@Valid RosterEntry> getEntries() {
    return entries;
  }

  public void setEntries(List<@Valid RosterEntry> entries) {
    this.entries = entries;
  }

  public RosterMonth employees(List<@Valid RosterEmployee> employees) {
    this.employees = employees;
    return this;
  }

  public RosterMonth addEmployeesItem(RosterEmployee employeesItem) {
    if (this.employees == null) {
      this.employees = new ArrayList<>();
    }
    this.employees.add(employeesItem);
    return this;
  }

  /**
   * Get employees
   * @return employees
   */
  @NotNull @Valid 
  @JsonProperty("employees")
  public List<@Valid RosterEmployee> getEmployees() {
    return employees;
  }

  public void setEmployees(List<@Valid RosterEmployee> employees) {
    this.employees = employees;
  }

  public RosterMonth coverageWarnings(List<@Valid RosterCoverageWarning> coverageWarnings) {
    this.coverageWarnings = coverageWarnings;
    return this;
  }

  public RosterMonth addCoverageWarningsItem(RosterCoverageWarning coverageWarningsItem) {
    if (this.coverageWarnings == null) {
      this.coverageWarnings = new ArrayList<>();
    }
    this.coverageWarnings.add(coverageWarningsItem);
    return this;
  }

  /**
   * Get coverageWarnings
   * @return coverageWarnings
   */
  @NotNull @Valid 
  @JsonProperty("coverageWarnings")
  public List<@Valid RosterCoverageWarning> getCoverageWarnings() {
    return coverageWarnings;
  }

  public void setCoverageWarnings(List<@Valid RosterCoverageWarning> coverageWarnings) {
    this.coverageWarnings = coverageWarnings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterMonth rosterMonth = (RosterMonth) o;
    return Objects.equals(this.year, rosterMonth.year) &&
        Objects.equals(this.month, rosterMonth.month) &&
        Objects.equals(this.entries, rosterMonth.entries) &&
        Objects.equals(this.employees, rosterMonth.employees) &&
        Objects.equals(this.coverageWarnings, rosterMonth.coverageWarnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(year, month, entries, employees, coverageWarnings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterMonth {\n");
    sb.append("    year: ").append(toIndentedString(year)).append("\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    entries: ").append(toIndentedString(entries)).append("\n");
    sb.append("    employees: ").append(toIndentedString(employees)).append("\n");
    sb.append("    coverageWarnings: ").append(toIndentedString(coverageWarnings)).append("\n");
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

