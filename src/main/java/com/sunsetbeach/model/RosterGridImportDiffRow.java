package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * One &#x60;(employeeUserId, date)&#x60; cell where the uploaded file disagrees with the current &#x60;RosterEntry&#x60; state. Unchanged cells are never listed here, only counted (&#x60;RosterGridImportPreview.unchangedCount&#x60;) - see that schema&#39;s own description for why a full re-sync, not a wholesale replace, is how this import behaves. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterGridImportDiffRow {

  private String employeeUserId;

  private String employeeName;

  private String date;

  /**
   * ADD: blank in the DB, coded in the file. REMOVE: coded in the DB, blank in the file. CHANGE: coded differently in each.
   */
  public enum ChangeTypeEnum {
    ADD("ADD"),
    
    REMOVE("REMOVE"),
    
    CHANGE("CHANGE");

    private String value;

    ChangeTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ChangeTypeEnum fromValue(String value) {
      for (ChangeTypeEnum b : ChangeTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private ChangeTypeEnum changeType;

  private JsonNullable<String> previousCode = JsonNullable.<String>undefined();

  private JsonNullable<String> newCode = JsonNullable.<String>undefined();

  /**
   * POSITIONAL: both the employee row and this exact cell matched the export's own hidden metadata - no name or code text was ever compared. FALLBACK_NAME/FALLBACK_CODE/ FALLBACK_BOTH: the row and/or this cell has no matching metadata (added or retyped by hand since export), so that side resolved the ordinary way instead - by name against `/roster/import/name-mappings`, and/or by matching `newCode` against an active `ShiftCode` for this employee's area. 
   */
  public enum ResolutionEnum {
    POSITIONAL("POSITIONAL"),
    
    FALLBACK_NAME("FALLBACK_NAME"),
    
    FALLBACK_CODE("FALLBACK_CODE"),
    
    FALLBACK_BOTH("FALLBACK_BOTH");

    private String value;

    ResolutionEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ResolutionEnum fromValue(String value) {
      for (ResolutionEnum b : ResolutionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private ResolutionEnum resolution;

  private Boolean lockedConflict;

  private Boolean staleSinceExport;

  private JsonNullable<String> pendingRetiredCodeId = JsonNullable.<String>undefined();

  private Boolean pendingUnknownCode;

  public RosterGridImportDiffRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterGridImportDiffRow(String employeeUserId, String employeeName, String date, ChangeTypeEnum changeType, ResolutionEnum resolution, Boolean lockedConflict, Boolean staleSinceExport) {
    this.employeeUserId = employeeUserId;
    this.employeeName = employeeName;
    this.date = date;
    this.changeType = changeType;
    this.resolution = resolution;
    this.lockedConflict = lockedConflict;
    this.staleSinceExport = staleSinceExport;
  }

  public RosterGridImportDiffRow employeeUserId(String employeeUserId) {
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

  public RosterGridImportDiffRow employeeName(String employeeName) {
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

  public RosterGridImportDiffRow date(String date) {
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

  public RosterGridImportDiffRow changeType(ChangeTypeEnum changeType) {
    this.changeType = changeType;
    return this;
  }

  /**
   * ADD: blank in the DB, coded in the file. REMOVE: coded in the DB, blank in the file. CHANGE: coded differently in each.
   * @return changeType
   */
  @NotNull 
  @JsonProperty("changeType")
  public ChangeTypeEnum getChangeType() {
    return changeType;
  }

  public void setChangeType(ChangeTypeEnum changeType) {
    this.changeType = changeType;
  }

  public RosterGridImportDiffRow previousCode(String previousCode) {
    this.previousCode = JsonNullable.of(previousCode);
    return this;
  }

  /**
   * The current `RosterEntry`'s own `ShiftCode.code` - set for REMOVE and CHANGE, absent for ADD.
   * @return previousCode
   */
  
  @JsonProperty("previousCode")
  public JsonNullable<String> getPreviousCode() {
    return previousCode;
  }

  public void setPreviousCode(JsonNullable<String> previousCode) {
    this.previousCode = previousCode;
  }

  public RosterGridImportDiffRow newCode(String newCode) {
    this.newCode = JsonNullable.of(newCode);
    return this;
  }

  /**
   * The file cell's code text - set for ADD and CHANGE, absent for REMOVE.
   * @return newCode
   */
  
  @JsonProperty("newCode")
  public JsonNullable<String> getNewCode() {
    return newCode;
  }

  public void setNewCode(JsonNullable<String> newCode) {
    this.newCode = newCode;
  }

  public RosterGridImportDiffRow resolution(ResolutionEnum resolution) {
    this.resolution = resolution;
    return this;
  }

  /**
   * POSITIONAL: both the employee row and this exact cell matched the export's own hidden metadata - no name or code text was ever compared. FALLBACK_NAME/FALLBACK_CODE/ FALLBACK_BOTH: the row and/or this cell has no matching metadata (added or retyped by hand since export), so that side resolved the ordinary way instead - by name against `/roster/import/name-mappings`, and/or by matching `newCode` against an active `ShiftCode` for this employee's area. 
   * @return resolution
   */
  @NotNull 
  @JsonProperty("resolution")
  public ResolutionEnum getResolution() {
    return resolution;
  }

  public void setResolution(ResolutionEnum resolution) {
    this.resolution = resolution;
  }

  public RosterGridImportDiffRow lockedConflict(Boolean lockedConflict) {
    this.lockedConflict = lockedConflict;
    return this;
  }

  /**
   * True when this is a REMOVE or CHANGE and the existing `RosterEntry` has `locked: true` - `POST /roster/grid-import/commit` never touches this row regardless of the file, and counts it in `skippedLocked`. Never true for ADD (nothing existing to lock). 
   * @return lockedConflict
   */
  @NotNull 
  @JsonProperty("lockedConflict")
  public Boolean getLockedConflict() {
    return lockedConflict;
  }

  public void setLockedConflict(Boolean lockedConflict) {
    this.lockedConflict = lockedConflict;
  }

  public RosterGridImportDiffRow staleSinceExport(Boolean staleSinceExport) {
    this.staleSinceExport = staleSinceExport;
    return this;
  }

  /**
   * True when the existing `RosterEntry` (for REMOVE/CHANGE) was created or modified after this file's own `RosterGridImportPreview.exportedAt` - the file may not know about a change made through the ordinary roster grid after it was exported. Never blocks commit on its own; list it in `RosterGridImportCommitInput.excludeCells` to skip it. Never true for ADD. 
   * @return staleSinceExport
   */
  @NotNull 
  @JsonProperty("staleSinceExport")
  public Boolean getStaleSinceExport() {
    return staleSinceExport;
  }

  public void setStaleSinceExport(Boolean staleSinceExport) {
    this.staleSinceExport = staleSinceExport;
  }

  public RosterGridImportDiffRow pendingRetiredCodeId(String pendingRetiredCodeId) {
    this.pendingRetiredCodeId = JsonNullable.of(pendingRetiredCodeId);
    return this;
  }

  /**
   * Set when `newCode` resolves, by metadata, to a `ShiftCode` id listed in `RosterGridImportPreview.retiredCodes` - resolve that entry before this row can be applied.
   * @return pendingRetiredCodeId
   */
  
  @JsonProperty("pendingRetiredCodeId")
  public JsonNullable<String> getPendingRetiredCodeId() {
    return pendingRetiredCodeId;
  }

  public void setPendingRetiredCodeId(JsonNullable<String> pendingRetiredCodeId) {
    this.pendingRetiredCodeId = pendingRetiredCodeId;
  }

  public RosterGridImportDiffRow pendingUnknownCode(Boolean pendingUnknownCode) {
    this.pendingUnknownCode = pendingUnknownCode;
    return this;
  }

  /**
   * True when `newCode` has no metadata and matches no active `ShiftCode` - listed in `RosterGridImportPreview.unknownCodes`, needs a definition before this row can be applied.
   * @return pendingUnknownCode
   */
  
  @JsonProperty("pendingUnknownCode")
  public Boolean getPendingUnknownCode() {
    return pendingUnknownCode;
  }

  public void setPendingUnknownCode(Boolean pendingUnknownCode) {
    this.pendingUnknownCode = pendingUnknownCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterGridImportDiffRow rosterGridImportDiffRow = (RosterGridImportDiffRow) o;
    return Objects.equals(this.employeeUserId, rosterGridImportDiffRow.employeeUserId) &&
        Objects.equals(this.employeeName, rosterGridImportDiffRow.employeeName) &&
        Objects.equals(this.date, rosterGridImportDiffRow.date) &&
        Objects.equals(this.changeType, rosterGridImportDiffRow.changeType) &&
        equalsNullable(this.previousCode, rosterGridImportDiffRow.previousCode) &&
        equalsNullable(this.newCode, rosterGridImportDiffRow.newCode) &&
        Objects.equals(this.resolution, rosterGridImportDiffRow.resolution) &&
        Objects.equals(this.lockedConflict, rosterGridImportDiffRow.lockedConflict) &&
        Objects.equals(this.staleSinceExport, rosterGridImportDiffRow.staleSinceExport) &&
        equalsNullable(this.pendingRetiredCodeId, rosterGridImportDiffRow.pendingRetiredCodeId) &&
        Objects.equals(this.pendingUnknownCode, rosterGridImportDiffRow.pendingUnknownCode);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeUserId, employeeName, date, changeType, hashCodeNullable(previousCode), hashCodeNullable(newCode), resolution, lockedConflict, staleSinceExport, hashCodeNullable(pendingRetiredCodeId), pendingUnknownCode);
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
    sb.append("class RosterGridImportDiffRow {\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    employeeName: ").append(toIndentedString(employeeName)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    changeType: ").append(toIndentedString(changeType)).append("\n");
    sb.append("    previousCode: ").append(toIndentedString(previousCode)).append("\n");
    sb.append("    newCode: ").append(toIndentedString(newCode)).append("\n");
    sb.append("    resolution: ").append(toIndentedString(resolution)).append("\n");
    sb.append("    lockedConflict: ").append(toIndentedString(lockedConflict)).append("\n");
    sb.append("    staleSinceExport: ").append(toIndentedString(staleSinceExport)).append("\n");
    sb.append("    pendingRetiredCodeId: ").append(toIndentedString(pendingRetiredCodeId)).append("\n");
    sb.append("    pendingUnknownCode: ").append(toIndentedString(pendingUnknownCode)).append("\n");
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

