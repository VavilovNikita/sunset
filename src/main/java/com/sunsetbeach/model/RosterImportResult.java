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
 * Response of &#x60;POST /roster/import/commit&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportResult {

  private Integer year;

  private Integer month;

  private Integer created;

  private Integer skippedCollisions;

  public RosterImportResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportResult(Integer year, Integer month, Integer created, Integer skippedCollisions) {
    this.year = year;
    this.month = month;
    this.created = created;
    this.skippedCollisions = skippedCollisions;
  }

  public RosterImportResult year(Integer year) {
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

  public RosterImportResult month(Integer month) {
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

  public RosterImportResult created(Integer created) {
    this.created = created;
    return this;
  }

  /**
   * Get created
   * @return created
   */
  @NotNull 
  @JsonProperty("created")
  public Integer getCreated() {
    return created;
  }

  public void setCreated(Integer created) {
    this.created = created;
  }

  public RosterImportResult skippedCollisions(Integer skippedCollisions) {
    this.skippedCollisions = skippedCollisions;
    return this;
  }

  /**
   * Get skippedCollisions
   * @return skippedCollisions
   */
  @NotNull 
  @JsonProperty("skippedCollisions")
  public Integer getSkippedCollisions() {
    return skippedCollisions;
  }

  public void setSkippedCollisions(Integer skippedCollisions) {
    this.skippedCollisions = skippedCollisions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportResult rosterImportResult = (RosterImportResult) o;
    return Objects.equals(this.year, rosterImportResult.year) &&
        Objects.equals(this.month, rosterImportResult.month) &&
        Objects.equals(this.created, rosterImportResult.created) &&
        Objects.equals(this.skippedCollisions, rosterImportResult.skippedCollisions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(year, month, created, skippedCollisions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterImportResult {\n");
    sb.append("    year: ").append(toIndentedString(year)).append("\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    created: ").append(toIndentedString(created)).append("\n");
    sb.append("    skippedCollisions: ").append(toIndentedString(skippedCollisions)).append("\n");
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

