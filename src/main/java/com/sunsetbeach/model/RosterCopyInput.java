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
 * Body of &#x60;POST /roster/copy&#x60;. Leave both source dates out to copy the whole previous month.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterCopyInput {

  private Integer year;

  private Integer month;

  private String sourceFrom;

  private String sourceTo;

  public RosterCopyInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterCopyInput(Integer year, Integer month) {
    this.year = year;
    this.month = month;
  }

  public RosterCopyInput year(Integer year) {
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

  public RosterCopyInput month(Integer month) {
    this.month = month;
    return this;
  }

  /**
   * Get month
   * minimum: 1
   * maximum: 12
   * @return month
   */
  @NotNull @Min(1) @Max(12) 
  @JsonProperty("month")
  public Integer getMonth() {
    return month;
  }

  public void setMonth(Integer month) {
    this.month = month;
  }

  public RosterCopyInput sourceFrom(String sourceFrom) {
    this.sourceFrom = sourceFrom;
    return this;
  }

  /**
   * First source date (inclusive). Give both or neither.
   * @return sourceFrom
   */
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("sourceFrom")
  public String getSourceFrom() {
    return sourceFrom;
  }

  public void setSourceFrom(String sourceFrom) {
    this.sourceFrom = sourceFrom;
  }

  public RosterCopyInput sourceTo(String sourceTo) {
    this.sourceTo = sourceTo;
    return this;
  }

  /**
   * Last source date (inclusive).
   * @return sourceTo
   */
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("sourceTo")
  public String getSourceTo() {
    return sourceTo;
  }

  public void setSourceTo(String sourceTo) {
    this.sourceTo = sourceTo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterCopyInput rosterCopyInput = (RosterCopyInput) o;
    return Objects.equals(this.year, rosterCopyInput.year) &&
        Objects.equals(this.month, rosterCopyInput.month) &&
        Objects.equals(this.sourceFrom, rosterCopyInput.sourceFrom) &&
        Objects.equals(this.sourceTo, rosterCopyInput.sourceTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(year, month, sourceFrom, sourceTo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterCopyInput {\n");
    sb.append("    year: ").append(toIndentedString(year)).append("\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    sourceFrom: ").append(toIndentedString(sourceFrom)).append("\n");
    sb.append("    sourceTo: ").append(toIndentedString(sourceTo)).append("\n");
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

