package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RosterImportCodeEntry;
import com.sunsetbeach.model.RosterImportCollision;
import com.sunsetbeach.model.RosterImportIssue;
import com.sunsetbeach.model.RosterImportNameEntry;
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
 * Response of &#x60;POST /roster/import/preview&#x60; - a dry run. Nothing is written by this call, not even a remembered mapping; it only reports what parsing the file, as it stands right now, would do. &#x60;canCommit&#x60; is true only once every name is mapped, every ambiguous \&quot;9\&quot; is resolved, and &#x60;issues&#x60; is empty. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportPreview {

  private String importId;

  private Integer year;

  private Integer month;

  @Valid
  private List<@Valid RosterImportNameEntry> names = new ArrayList<>();

  @Valid
  private List<@Valid RosterImportCodeEntry> codes = new ArrayList<>();

  @Valid
  private List<@Valid RosterImportIssue> issues = new ArrayList<>();

  @Valid
  private List<@Valid RosterImportCollision> collisions = new ArrayList<>();

  private Integer entriesToCreate;

  private Boolean canCommit;

  public RosterImportPreview() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportPreview(String importId, Integer year, Integer month, List<@Valid RosterImportNameEntry> names, List<@Valid RosterImportCodeEntry> codes, List<@Valid RosterImportIssue> issues, List<@Valid RosterImportCollision> collisions, Integer entriesToCreate, Boolean canCommit) {
    this.importId = importId;
    this.year = year;
    this.month = month;
    this.names = names;
    this.codes = codes;
    this.issues = issues;
    this.collisions = collisions;
    this.entriesToCreate = entriesToCreate;
    this.canCommit = canCommit;
  }

  public RosterImportPreview importId(String importId) {
    this.importId = importId;
    return this;
  }

  /**
   * Opaque reference to this file, held server-side - pass back to `POST /roster/import/commit` once `canCommit` is true.
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

  public RosterImportPreview year(Integer year) {
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

  public RosterImportPreview month(Integer month) {
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

  public RosterImportPreview names(List<@Valid RosterImportNameEntry> names) {
    this.names = names;
    return this;
  }

  public RosterImportPreview addNamesItem(RosterImportNameEntry namesItem) {
    if (this.names == null) {
      this.names = new ArrayList<>();
    }
    this.names.add(namesItem);
    return this;
  }

  /**
   * Get names
   * @return names
   */
  @NotNull @Valid 
  @JsonProperty("names")
  public List<@Valid RosterImportNameEntry> getNames() {
    return names;
  }

  public void setNames(List<@Valid RosterImportNameEntry> names) {
    this.names = names;
  }

  public RosterImportPreview codes(List<@Valid RosterImportCodeEntry> codes) {
    this.codes = codes;
    return this;
  }

  public RosterImportPreview addCodesItem(RosterImportCodeEntry codesItem) {
    if (this.codes == null) {
      this.codes = new ArrayList<>();
    }
    this.codes.add(codesItem);
    return this;
  }

  /**
   * Get codes
   * @return codes
   */
  @NotNull @Valid 
  @JsonProperty("codes")
  public List<@Valid RosterImportCodeEntry> getCodes() {
    return codes;
  }

  public void setCodes(List<@Valid RosterImportCodeEntry> codes) {
    this.codes = codes;
  }

  public RosterImportPreview issues(List<@Valid RosterImportIssue> issues) {
    this.issues = issues;
    return this;
  }

  public RosterImportPreview addIssuesItem(RosterImportIssue issuesItem) {
    if (this.issues == null) {
      this.issues = new ArrayList<>();
    }
    this.issues.add(issuesItem);
    return this;
  }

  /**
   * Get issues
   * @return issues
   */
  @NotNull @Valid 
  @JsonProperty("issues")
  public List<@Valid RosterImportIssue> getIssues() {
    return issues;
  }

  public void setIssues(List<@Valid RosterImportIssue> issues) {
    this.issues = issues;
  }

  public RosterImportPreview collisions(List<@Valid RosterImportCollision> collisions) {
    this.collisions = collisions;
    return this;
  }

  public RosterImportPreview addCollisionsItem(RosterImportCollision collisionsItem) {
    if (this.collisions == null) {
      this.collisions = new ArrayList<>();
    }
    this.collisions.add(collisionsItem);
    return this;
  }

  /**
   * Get collisions
   * @return collisions
   */
  @NotNull @Valid 
  @JsonProperty("collisions")
  public List<@Valid RosterImportCollision> getCollisions() {
    return collisions;
  }

  public void setCollisions(List<@Valid RosterImportCollision> collisions) {
    this.collisions = collisions;
  }

  public RosterImportPreview entriesToCreate(Integer entriesToCreate) {
    this.entriesToCreate = entriesToCreate;
    return this;
  }

  /**
   * How many `RosterEntry` rows committing right now would actually create - excludes collisions and anything still unresolved.
   * @return entriesToCreate
   */
  @NotNull 
  @JsonProperty("entriesToCreate")
  public Integer getEntriesToCreate() {
    return entriesToCreate;
  }

  public void setEntriesToCreate(Integer entriesToCreate) {
    this.entriesToCreate = entriesToCreate;
  }

  public RosterImportPreview canCommit(Boolean canCommit) {
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
    RosterImportPreview rosterImportPreview = (RosterImportPreview) o;
    return Objects.equals(this.importId, rosterImportPreview.importId) &&
        Objects.equals(this.year, rosterImportPreview.year) &&
        Objects.equals(this.month, rosterImportPreview.month) &&
        Objects.equals(this.names, rosterImportPreview.names) &&
        Objects.equals(this.codes, rosterImportPreview.codes) &&
        Objects.equals(this.issues, rosterImportPreview.issues) &&
        Objects.equals(this.collisions, rosterImportPreview.collisions) &&
        Objects.equals(this.entriesToCreate, rosterImportPreview.entriesToCreate) &&
        Objects.equals(this.canCommit, rosterImportPreview.canCommit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(importId, year, month, names, codes, issues, collisions, entriesToCreate, canCommit);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterImportPreview {\n");
    sb.append("    importId: ").append(toIndentedString(importId)).append("\n");
    sb.append("    year: ").append(toIndentedString(year)).append("\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    names: ").append(toIndentedString(names)).append("\n");
    sb.append("    codes: ").append(toIndentedString(codes)).append("\n");
    sb.append("    issues: ").append(toIndentedString(issues)).append("\n");
    sb.append("    collisions: ").append(toIndentedString(collisions)).append("\n");
    sb.append("    entriesToCreate: ").append(toIndentedString(entriesToCreate)).append("\n");
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

