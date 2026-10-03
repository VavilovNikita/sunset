package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RosterMonth;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * RosterCopyResult
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterCopyResult {

  private RosterMonth month;

  private String sourceFrom;

  private String sourceTo;

  private Integer created;

  private Integer skippedExisting;

  private Integer skippedNoCurrentCode;

  public RosterCopyResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterCopyResult(RosterMonth month, String sourceFrom, String sourceTo, Integer created, Integer skippedExisting, Integer skippedNoCurrentCode) {
    this.month = month;
    this.sourceFrom = sourceFrom;
    this.sourceTo = sourceTo;
    this.created = created;
    this.skippedExisting = skippedExisting;
    this.skippedNoCurrentCode = skippedNoCurrentCode;
  }

  public RosterCopyResult month(RosterMonth month) {
    this.month = month;
    return this;
  }

  /**
   * Get month
   * @return month
   */
  @NotNull @Valid 
  @JsonProperty("month")
  public RosterMonth getMonth() {
    return month;
  }

  public void setMonth(RosterMonth month) {
    this.month = month;
  }

  public RosterCopyResult sourceFrom(String sourceFrom) {
    this.sourceFrom = sourceFrom;
    return this;
  }

  /**
   * Get sourceFrom
   * @return sourceFrom
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("sourceFrom")
  public String getSourceFrom() {
    return sourceFrom;
  }

  public void setSourceFrom(String sourceFrom) {
    this.sourceFrom = sourceFrom;
  }

  public RosterCopyResult sourceTo(String sourceTo) {
    this.sourceTo = sourceTo;
    return this;
  }

  /**
   * The last source date actually used - the range cut down to whole weeks.
   * @return sourceTo
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("sourceTo")
  public String getSourceTo() {
    return sourceTo;
  }

  public void setSourceTo(String sourceTo) {
    this.sourceTo = sourceTo;
  }

  public RosterCopyResult created(Integer created) {
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

  public RosterCopyResult skippedExisting(Integer skippedExisting) {
    this.skippedExisting = skippedExisting;
    return this;
  }

  /**
   * Target cells that already had an entry and were left alone.
   * @return skippedExisting
   */
  @NotNull 
  @JsonProperty("skippedExisting")
  public Integer getSkippedExisting() {
    return skippedExisting;
  }

  public void setSkippedExisting(Integer skippedExisting) {
    this.skippedExisting = skippedExisting;
  }

  public RosterCopyResult skippedNoCurrentCode(Integer skippedNoCurrentCode) {
    this.skippedNoCurrentCode = skippedNoCurrentCode;
    return this;
  }

  /**
   * Cells whose shift code has no version in force on the target date.
   * @return skippedNoCurrentCode
   */
  @NotNull 
  @JsonProperty("skippedNoCurrentCode")
  public Integer getSkippedNoCurrentCode() {
    return skippedNoCurrentCode;
  }

  public void setSkippedNoCurrentCode(Integer skippedNoCurrentCode) {
    this.skippedNoCurrentCode = skippedNoCurrentCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterCopyResult rosterCopyResult = (RosterCopyResult) o;
    return Objects.equals(this.month, rosterCopyResult.month) &&
        Objects.equals(this.sourceFrom, rosterCopyResult.sourceFrom) &&
        Objects.equals(this.sourceTo, rosterCopyResult.sourceTo) &&
        Objects.equals(this.created, rosterCopyResult.created) &&
        Objects.equals(this.skippedExisting, rosterCopyResult.skippedExisting) &&
        Objects.equals(this.skippedNoCurrentCode, rosterCopyResult.skippedNoCurrentCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(month, sourceFrom, sourceTo, created, skippedExisting, skippedNoCurrentCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterCopyResult {\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    sourceFrom: ").append(toIndentedString(sourceFrom)).append("\n");
    sb.append("    sourceTo: ").append(toIndentedString(sourceTo)).append("\n");
    sb.append("    created: ").append(toIndentedString(created)).append("\n");
    sb.append("    skippedExisting: ").append(toIndentedString(skippedExisting)).append("\n");
    sb.append("    skippedNoCurrentCode: ").append(toIndentedString(skippedNoCurrentCode)).append("\n");
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

