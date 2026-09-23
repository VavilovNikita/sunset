package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RosterGridImportDiffRow;
import com.sunsetbeach.model.RosterGridImportRetiredCode;
import com.sunsetbeach.model.RosterGridImportUnknownCode;
import com.sunsetbeach.model.RosterImportNameEntry;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;POST /roster/grid-import/preview&#x60; - a dry run, nothing written. Sync semantics, not wholesale replace: &#x60;diffRows&#x60; lists only the &#x60;(employeeUserId, date)&#x60; cells where the file disagrees with the current &#x60;RosterEntry&#x60; state, classified ADD/REMOVE/ CHANGE; &#x60;unchangedCount&#x60; is everything else in the file&#39;s own date range, confirmed identical. &#x60;canCommit&#x60; is true only once &#x60;unmatchedEmployees&#x60;, &#x60;retiredCodes&#x60;, and &#x60;unknownCodes&#x60; are all empty - a &#x60;lockedConflict&#x60; or &#x60;staleSinceExport&#x60; diff row never blocks it (see &#x60;RosterGridImportDiffRow&#x60;&#39;s own description of each). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterGridImportPreview {

  private String importId;

  private Integer year;

  private Integer month;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime exportedAt;

  @Valid
  private List<@Valid RosterGridImportDiffRow> diffRows = new ArrayList<>();

  private Integer addCount;

  private Integer removeCount;

  private Integer changeCount;

  private Integer unchangedCount;

  private Integer staleCount;

  private Integer lockedConflictCount;

  @Valid
  private List<@Valid RosterImportNameEntry> unmatchedEmployees = new ArrayList<>();

  @Valid
  private List<@Valid RosterGridImportRetiredCode> retiredCodes = new ArrayList<>();

  @Valid
  private List<@Valid RosterGridImportUnknownCode> unknownCodes = new ArrayList<>();

  private Boolean canCommit;

  public RosterGridImportPreview() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterGridImportPreview(String importId, Integer year, Integer month, OffsetDateTime exportedAt, List<@Valid RosterGridImportDiffRow> diffRows, Integer addCount, Integer removeCount, Integer changeCount, Integer unchangedCount, Integer staleCount, Integer lockedConflictCount, List<@Valid RosterImportNameEntry> unmatchedEmployees, List<@Valid RosterGridImportRetiredCode> retiredCodes, List<@Valid RosterGridImportUnknownCode> unknownCodes, Boolean canCommit) {
    this.importId = importId;
    this.year = year;
    this.month = month;
    this.exportedAt = exportedAt;
    this.diffRows = diffRows;
    this.addCount = addCount;
    this.removeCount = removeCount;
    this.changeCount = changeCount;
    this.unchangedCount = unchangedCount;
    this.staleCount = staleCount;
    this.lockedConflictCount = lockedConflictCount;
    this.unmatchedEmployees = unmatchedEmployees;
    this.retiredCodes = retiredCodes;
    this.unknownCodes = unknownCodes;
    this.canCommit = canCommit;
  }

  public RosterGridImportPreview importId(String importId) {
    this.importId = importId;
    return this;
  }

  /**
   * Opaque reference to this file, held server-side - pass back to `POST /roster/grid-import/commit`.
   * @return importId
   */
  @NotNull 
  @JsonProperty("importId")
  public String getImportId() {
    return importId;
  }

  public void setImportId(String importId) {
    this.importId = importId;
  }

  public RosterGridImportPreview year(Integer year) {
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

  public RosterGridImportPreview month(Integer month) {
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

  public RosterGridImportPreview exportedAt(OffsetDateTime exportedAt) {
    this.exportedAt = exportedAt;
    return this;
  }

  /**
   * This file's own export timestamp, read from its hidden metadata sheet - shown so the admin can judge how stale it might be.
   * @return exportedAt
   */
  @NotNull @Valid 
  @JsonProperty("exportedAt")
  public OffsetDateTime getExportedAt() {
    return exportedAt;
  }

  public void setExportedAt(OffsetDateTime exportedAt) {
    this.exportedAt = exportedAt;
  }

  public RosterGridImportPreview diffRows(List<@Valid RosterGridImportDiffRow> diffRows) {
    this.diffRows = diffRows;
    return this;
  }

  public RosterGridImportPreview addDiffRowsItem(RosterGridImportDiffRow diffRowsItem) {
    if (this.diffRows == null) {
      this.diffRows = new ArrayList<>();
    }
    this.diffRows.add(diffRowsItem);
    return this;
  }

  /**
   * Get diffRows
   * @return diffRows
   */
  @NotNull @Valid 
  @JsonProperty("diffRows")
  public List<@Valid RosterGridImportDiffRow> getDiffRows() {
    return diffRows;
  }

  public void setDiffRows(List<@Valid RosterGridImportDiffRow> diffRows) {
    this.diffRows = diffRows;
  }

  public RosterGridImportPreview addCount(Integer addCount) {
    this.addCount = addCount;
    return this;
  }

  /**
   * Get addCount
   * @return addCount
   */
  @NotNull 
  @JsonProperty("addCount")
  public Integer getAddCount() {
    return addCount;
  }

  public void setAddCount(Integer addCount) {
    this.addCount = addCount;
  }

  public RosterGridImportPreview removeCount(Integer removeCount) {
    this.removeCount = removeCount;
    return this;
  }

  /**
   * Get removeCount
   * @return removeCount
   */
  @NotNull 
  @JsonProperty("removeCount")
  public Integer getRemoveCount() {
    return removeCount;
  }

  public void setRemoveCount(Integer removeCount) {
    this.removeCount = removeCount;
  }

  public RosterGridImportPreview changeCount(Integer changeCount) {
    this.changeCount = changeCount;
    return this;
  }

  /**
   * Get changeCount
   * @return changeCount
   */
  @NotNull 
  @JsonProperty("changeCount")
  public Integer getChangeCount() {
    return changeCount;
  }

  public void setChangeCount(Integer changeCount) {
    this.changeCount = changeCount;
  }

  public RosterGridImportPreview unchangedCount(Integer unchangedCount) {
    this.unchangedCount = unchangedCount;
    return this;
  }

  /**
   * Get unchangedCount
   * @return unchangedCount
   */
  @NotNull 
  @JsonProperty("unchangedCount")
  public Integer getUnchangedCount() {
    return unchangedCount;
  }

  public void setUnchangedCount(Integer unchangedCount) {
    this.unchangedCount = unchangedCount;
  }

  public RosterGridImportPreview staleCount(Integer staleCount) {
    this.staleCount = staleCount;
    return this;
  }

  /**
   * How many rows in `diffRows` have `staleSinceExport: true`.
   * @return staleCount
   */
  @NotNull 
  @JsonProperty("staleCount")
  public Integer getStaleCount() {
    return staleCount;
  }

  public void setStaleCount(Integer staleCount) {
    this.staleCount = staleCount;
  }

  public RosterGridImportPreview lockedConflictCount(Integer lockedConflictCount) {
    this.lockedConflictCount = lockedConflictCount;
    return this;
  }

  /**
   * How many rows in `diffRows` have `lockedConflict: true`.
   * @return lockedConflictCount
   */
  @NotNull 
  @JsonProperty("lockedConflictCount")
  public Integer getLockedConflictCount() {
    return lockedConflictCount;
  }

  public void setLockedConflictCount(Integer lockedConflictCount) {
    this.lockedConflictCount = lockedConflictCount;
  }

  public RosterGridImportPreview unmatchedEmployees(List<@Valid RosterImportNameEntry> unmatchedEmployees) {
    this.unmatchedEmployees = unmatchedEmployees;
    return this;
  }

  public RosterGridImportPreview addUnmatchedEmployeesItem(RosterImportNameEntry unmatchedEmployeesItem) {
    if (this.unmatchedEmployees == null) {
      this.unmatchedEmployees = new ArrayList<>();
    }
    this.unmatchedEmployees.add(unmatchedEmployeesItem);
    return this;
  }

  /**
   * A row in the file with no matching employee in the metadata sheet, and no exact-name match either - resolve exactly like the hand-built importer's own name entries, via `POST /roster/import/name-mappings`.
   * @return unmatchedEmployees
   */
  @NotNull @Valid 
  @JsonProperty("unmatchedEmployees")
  public List<@Valid RosterImportNameEntry> getUnmatchedEmployees() {
    return unmatchedEmployees;
  }

  public void setUnmatchedEmployees(List<@Valid RosterImportNameEntry> unmatchedEmployees) {
    this.unmatchedEmployees = unmatchedEmployees;
  }

  public RosterGridImportPreview retiredCodes(List<@Valid RosterGridImportRetiredCode> retiredCodes) {
    this.retiredCodes = retiredCodes;
    return this;
  }

  public RosterGridImportPreview addRetiredCodesItem(RosterGridImportRetiredCode retiredCodesItem) {
    if (this.retiredCodes == null) {
      this.retiredCodes = new ArrayList<>();
    }
    this.retiredCodes.add(retiredCodesItem);
    return this;
  }

  /**
   * Get retiredCodes
   * @return retiredCodes
   */
  @NotNull @Valid 
  @JsonProperty("retiredCodes")
  public List<@Valid RosterGridImportRetiredCode> getRetiredCodes() {
    return retiredCodes;
  }

  public void setRetiredCodes(List<@Valid RosterGridImportRetiredCode> retiredCodes) {
    this.retiredCodes = retiredCodes;
  }

  public RosterGridImportPreview unknownCodes(List<@Valid RosterGridImportUnknownCode> unknownCodes) {
    this.unknownCodes = unknownCodes;
    return this;
  }

  public RosterGridImportPreview addUnknownCodesItem(RosterGridImportUnknownCode unknownCodesItem) {
    if (this.unknownCodes == null) {
      this.unknownCodes = new ArrayList<>();
    }
    this.unknownCodes.add(unknownCodesItem);
    return this;
  }

  /**
   * Get unknownCodes
   * @return unknownCodes
   */
  @NotNull @Valid 
  @JsonProperty("unknownCodes")
  public List<@Valid RosterGridImportUnknownCode> getUnknownCodes() {
    return unknownCodes;
  }

  public void setUnknownCodes(List<@Valid RosterGridImportUnknownCode> unknownCodes) {
    this.unknownCodes = unknownCodes;
  }

  public RosterGridImportPreview canCommit(Boolean canCommit) {
    this.canCommit = canCommit;
    return this;
  }

  /**
   * Get canCommit
   * @return canCommit
   */
  @NotNull 
  @JsonProperty("canCommit")
  public Boolean getCanCommit() {
    return canCommit;
  }

  public void setCanCommit(Boolean canCommit) {
    this.canCommit = canCommit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterGridImportPreview rosterGridImportPreview = (RosterGridImportPreview) o;
    return Objects.equals(this.importId, rosterGridImportPreview.importId) &&
        Objects.equals(this.year, rosterGridImportPreview.year) &&
        Objects.equals(this.month, rosterGridImportPreview.month) &&
        Objects.equals(this.exportedAt, rosterGridImportPreview.exportedAt) &&
        Objects.equals(this.diffRows, rosterGridImportPreview.diffRows) &&
        Objects.equals(this.addCount, rosterGridImportPreview.addCount) &&
        Objects.equals(this.removeCount, rosterGridImportPreview.removeCount) &&
        Objects.equals(this.changeCount, rosterGridImportPreview.changeCount) &&
        Objects.equals(this.unchangedCount, rosterGridImportPreview.unchangedCount) &&
        Objects.equals(this.staleCount, rosterGridImportPreview.staleCount) &&
        Objects.equals(this.lockedConflictCount, rosterGridImportPreview.lockedConflictCount) &&
        Objects.equals(this.unmatchedEmployees, rosterGridImportPreview.unmatchedEmployees) &&
        Objects.equals(this.retiredCodes, rosterGridImportPreview.retiredCodes) &&
        Objects.equals(this.unknownCodes, rosterGridImportPreview.unknownCodes) &&
        Objects.equals(this.canCommit, rosterGridImportPreview.canCommit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(importId, year, month, exportedAt, diffRows, addCount, removeCount, changeCount, unchangedCount, staleCount, lockedConflictCount, unmatchedEmployees, retiredCodes, unknownCodes, canCommit);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterGridImportPreview {\n");
    sb.append("    importId: ").append(toIndentedString(importId)).append("\n");
    sb.append("    year: ").append(toIndentedString(year)).append("\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    exportedAt: ").append(toIndentedString(exportedAt)).append("\n");
    sb.append("    diffRows: ").append(toIndentedString(diffRows)).append("\n");
    sb.append("    addCount: ").append(toIndentedString(addCount)).append("\n");
    sb.append("    removeCount: ").append(toIndentedString(removeCount)).append("\n");
    sb.append("    changeCount: ").append(toIndentedString(changeCount)).append("\n");
    sb.append("    unchangedCount: ").append(toIndentedString(unchangedCount)).append("\n");
    sb.append("    staleCount: ").append(toIndentedString(staleCount)).append("\n");
    sb.append("    lockedConflictCount: ").append(toIndentedString(lockedConflictCount)).append("\n");
    sb.append("    unmatchedEmployees: ").append(toIndentedString(unmatchedEmployees)).append("\n");
    sb.append("    retiredCodes: ").append(toIndentedString(retiredCodes)).append("\n");
    sb.append("    unknownCodes: ").append(toIndentedString(unknownCodes)).append("\n");
    sb.append("    canCommit: ").append(toIndentedString(canCommit)).append("\n");
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

