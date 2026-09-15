package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.FillColor;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * RosterImportColorMappingResult
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportColorMappingResult {

  private String rawCode;

  private FillColor fillColor;

  private String resolvedCode;

  public RosterImportColorMappingResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportColorMappingResult(String rawCode, FillColor fillColor, String resolvedCode) {
    this.rawCode = rawCode;
    this.fillColor = fillColor;
    this.resolvedCode = resolvedCode;
  }

  public RosterImportColorMappingResult rawCode(String rawCode) {
    this.rawCode = rawCode;
    return this;
  }

  /**
   * Get rawCode
   * @return rawCode
   */
  @NotNull 
  @JsonProperty("rawCode")
  public String getRawCode() {
    return rawCode;
  }

  public void setRawCode(String rawCode) {
    this.rawCode = rawCode;
  }

  public RosterImportColorMappingResult fillColor(FillColor fillColor) {
    this.fillColor = fillColor;
    return this;
  }

  /**
   * Get fillColor
   * @return fillColor
   */
  @NotNull @Valid 
  @JsonProperty("fillColor")
  public FillColor getFillColor() {
    return fillColor;
  }

  public void setFillColor(FillColor fillColor) {
    this.fillColor = fillColor;
  }

  public RosterImportColorMappingResult resolvedCode(String resolvedCode) {
    this.resolvedCode = resolvedCode;
    return this;
  }

  /**
   * Get resolvedCode
   * @return resolvedCode
   */
  @NotNull 
  @JsonProperty("resolvedCode")
  public String getResolvedCode() {
    return resolvedCode;
  }

  public void setResolvedCode(String resolvedCode) {
    this.resolvedCode = resolvedCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportColorMappingResult rosterImportColorMappingResult = (RosterImportColorMappingResult) o;
    return Objects.equals(this.rawCode, rosterImportColorMappingResult.rawCode) &&
        Objects.equals(this.fillColor, rosterImportColorMappingResult.fillColor) &&
        Objects.equals(this.resolvedCode, rosterImportColorMappingResult.resolvedCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawCode, fillColor, resolvedCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterImportColorMappingResult {\n");
    sb.append("    rawCode: ").append(toIndentedString(rawCode)).append("\n");
    sb.append("    fillColor: ").append(toIndentedString(fillColor)).append("\n");
    sb.append("    resolvedCode: ").append(toIndentedString(resolvedCode)).append("\n");
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

