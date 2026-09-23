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
 * Response of &#x60;POST /roster/grid-import/commit&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterGridImportResult {

  private Integer year;

  private Integer month;

  private Integer created;

  private Integer removed;

  private Integer changed;

  private Integer skippedLocked;

  private Integer skippedExcluded;

  private Integer createdShiftCodes;

  public RosterGridImportResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterGridImportResult(Integer year, Integer month, Integer created, Integer removed, Integer changed, Integer skippedLocked, Integer skippedExcluded, Integer createdShiftCodes) {
    this.year = year;
    this.month = month;
    this.created = created;
    this.removed = removed;
    this.changed = changed;
    this.skippedLocked = skippedLocked;
    this.skippedExcluded = skippedExcluded;
    this.createdShiftCodes = createdShiftCodes;
  }

  public RosterGridImportResult year(Integer year) {
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

  public RosterGridImportResult month(Integer month) {
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

  public RosterGridImportResult created(Integer created) {
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

  public RosterGridImportResult removed(Integer removed) {
    this.removed = removed;
    return this;
  }

  /**
   * Get removed
   * @return removed
   */
  @NotNull 
  @JsonProperty("removed")
  public Integer getRemoved() {
    return removed;
  }

  public void setRemoved(Integer removed) {
    this.removed = removed;
  }

  public RosterGridImportResult changed(Integer changed) {
    this.changed = changed;
    return this;
  }

  /**
   * Get changed
   * @return changed
   */
  @NotNull 
  @JsonProperty("changed")
  public Integer getChanged() {
    return changed;
  }

  public void setChanged(Integer changed) {
    this.changed = changed;
  }

  public RosterGridImportResult skippedLocked(Integer skippedLocked) {
    this.skippedLocked = skippedLocked;
    return this;
  }

  /**
   * Get skippedLocked
   * @return skippedLocked
   */
  @NotNull 
  @JsonProperty("skippedLocked")
  public Integer getSkippedLocked() {
    return skippedLocked;
  }

  public void setSkippedLocked(Integer skippedLocked) {
    this.skippedLocked = skippedLocked;
  }

  public RosterGridImportResult skippedExcluded(Integer skippedExcluded) {
    this.skippedExcluded = skippedExcluded;
    return this;
  }

  /**
   * Get skippedExcluded
   * @return skippedExcluded
   */
  @NotNull 
  @JsonProperty("skippedExcluded")
  public Integer getSkippedExcluded() {
    return skippedExcluded;
  }

  public void setSkippedExcluded(Integer skippedExcluded) {
    this.skippedExcluded = skippedExcluded;
  }

  public RosterGridImportResult createdShiftCodes(Integer createdShiftCodes) {
    this.createdShiftCodes = createdShiftCodes;
    return this;
  }

  /**
   * New `ShiftCode` rows created via `retiredCodeResolutions`/`unknownCodeResolutions` during this commit.
   * @return createdShiftCodes
   */
  @NotNull 
  @JsonProperty("createdShiftCodes")
  public Integer getCreatedShiftCodes() {
    return createdShiftCodes;
  }

  public void setCreatedShiftCodes(Integer createdShiftCodes) {
    this.createdShiftCodes = createdShiftCodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterGridImportResult rosterGridImportResult = (RosterGridImportResult) o;
    return Objects.equals(this.year, rosterGridImportResult.year) &&
        Objects.equals(this.month, rosterGridImportResult.month) &&
        Objects.equals(this.created, rosterGridImportResult.created) &&
        Objects.equals(this.removed, rosterGridImportResult.removed) &&
        Objects.equals(this.changed, rosterGridImportResult.changed) &&
        Objects.equals(this.skippedLocked, rosterGridImportResult.skippedLocked) &&
        Objects.equals(this.skippedExcluded, rosterGridImportResult.skippedExcluded) &&
        Objects.equals(this.createdShiftCodes, rosterGridImportResult.createdShiftCodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(year, month, created, removed, changed, skippedLocked, skippedExcluded, createdShiftCodes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterGridImportResult {\n");
    sb.append("    year: ").append(toIndentedString(year)).append("\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    created: ").append(toIndentedString(created)).append("\n");
    sb.append("    removed: ").append(toIndentedString(removed)).append("\n");
    sb.append("    changed: ").append(toIndentedString(changed)).append("\n");
    sb.append("    skippedLocked: ").append(toIndentedString(skippedLocked)).append("\n");
    sb.append("    skippedExcluded: ").append(toIndentedString(skippedExcluded)).append("\n");
    sb.append("    createdShiftCodes: ").append(toIndentedString(createdShiftCodes)).append("\n");
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

