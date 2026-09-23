package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RosterGridImportExcludedCell;
import com.sunsetbeach.model.RosterGridImportRetiredCodeResolution;
import com.sunsetbeach.model.RosterGridImportUnknownCodeResolution;
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
 * Body of &#x60;POST /roster/grid-import/commit&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterGridImportCommitInput {

  private String importId;

  @Valid
  private List<@Valid RosterGridImportRetiredCodeResolution> retiredCodeResolutions = new ArrayList<>();

  @Valid
  private List<@Valid RosterGridImportUnknownCodeResolution> unknownCodeResolutions = new ArrayList<>();

  @Valid
  private List<@Valid RosterGridImportExcludedCell> excludeCells = new ArrayList<>();

  public RosterGridImportCommitInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterGridImportCommitInput(String importId) {
    this.importId = importId;
  }

  public RosterGridImportCommitInput importId(String importId) {
    this.importId = importId;
    return this;
  }

  /**
   * Get importId
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

  public RosterGridImportCommitInput retiredCodeResolutions(List<@Valid RosterGridImportRetiredCodeResolution> retiredCodeResolutions) {
    this.retiredCodeResolutions = retiredCodeResolutions;
    return this;
  }

  public RosterGridImportCommitInput addRetiredCodeResolutionsItem(RosterGridImportRetiredCodeResolution retiredCodeResolutionsItem) {
    if (this.retiredCodeResolutions == null) {
      this.retiredCodeResolutions = new ArrayList<>();
    }
    this.retiredCodeResolutions.add(retiredCodeResolutionsItem);
    return this;
  }

  /**
   * Get retiredCodeResolutions
   * @return retiredCodeResolutions
   */
  @Valid 
  @JsonProperty("retiredCodeResolutions")
  public List<@Valid RosterGridImportRetiredCodeResolution> getRetiredCodeResolutions() {
    return retiredCodeResolutions;
  }

  public void setRetiredCodeResolutions(List<@Valid RosterGridImportRetiredCodeResolution> retiredCodeResolutions) {
    this.retiredCodeResolutions = retiredCodeResolutions;
  }

  public RosterGridImportCommitInput unknownCodeResolutions(List<@Valid RosterGridImportUnknownCodeResolution> unknownCodeResolutions) {
    this.unknownCodeResolutions = unknownCodeResolutions;
    return this;
  }

  public RosterGridImportCommitInput addUnknownCodeResolutionsItem(RosterGridImportUnknownCodeResolution unknownCodeResolutionsItem) {
    if (this.unknownCodeResolutions == null) {
      this.unknownCodeResolutions = new ArrayList<>();
    }
    this.unknownCodeResolutions.add(unknownCodeResolutionsItem);
    return this;
  }

  /**
   * Get unknownCodeResolutions
   * @return unknownCodeResolutions
   */
  @Valid 
  @JsonProperty("unknownCodeResolutions")
  public List<@Valid RosterGridImportUnknownCodeResolution> getUnknownCodeResolutions() {
    return unknownCodeResolutions;
  }

  public void setUnknownCodeResolutions(List<@Valid RosterGridImportUnknownCodeResolution> unknownCodeResolutions) {
    this.unknownCodeResolutions = unknownCodeResolutions;
  }

  public RosterGridImportCommitInput excludeCells(List<@Valid RosterGridImportExcludedCell> excludeCells) {
    this.excludeCells = excludeCells;
    return this;
  }

  public RosterGridImportCommitInput addExcludeCellsItem(RosterGridImportExcludedCell excludeCellsItem) {
    if (this.excludeCells == null) {
      this.excludeCells = new ArrayList<>();
    }
    this.excludeCells.add(excludeCellsItem);
    return this;
  }

  /**
   * Non-locked cells to leave untouched even though the file's diff would otherwise apply them - typically ones flagged `staleSinceExport`. A locked cell is already always excluded and never needs to be listed here.
   * @return excludeCells
   */
  @Valid 
  @JsonProperty("excludeCells")
  public List<@Valid RosterGridImportExcludedCell> getExcludeCells() {
    return excludeCells;
  }

  public void setExcludeCells(List<@Valid RosterGridImportExcludedCell> excludeCells) {
    this.excludeCells = excludeCells;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterGridImportCommitInput rosterGridImportCommitInput = (RosterGridImportCommitInput) o;
    return Objects.equals(this.importId, rosterGridImportCommitInput.importId) &&
        Objects.equals(this.retiredCodeResolutions, rosterGridImportCommitInput.retiredCodeResolutions) &&
        Objects.equals(this.unknownCodeResolutions, rosterGridImportCommitInput.unknownCodeResolutions) &&
        Objects.equals(this.excludeCells, rosterGridImportCommitInput.excludeCells);
  }

  @Override
  public int hashCode() {
    return Objects.hash(importId, retiredCodeResolutions, unknownCodeResolutions, excludeCells);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterGridImportCommitInput {\n");
    sb.append("    importId: ").append(toIndentedString(importId)).append("\n");
    sb.append("    retiredCodeResolutions: ").append(toIndentedString(retiredCodeResolutions)).append("\n");
    sb.append("    unknownCodeResolutions: ").append(toIndentedString(unknownCodeResolutions)).append("\n");
    sb.append("    excludeCells: ").append(toIndentedString(excludeCells)).append("\n");
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

