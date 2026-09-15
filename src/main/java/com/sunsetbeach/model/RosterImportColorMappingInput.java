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
 * Body of &#x60;POST /roster/import/color-mappings&#x60;. Not scoped to an area - which code text a colour means is the same fact everywhere in the file (see &#x60;RosterImportShiftColorMapping&#x60;&#39;s own description on the backend); &#x60;shiftCodeId&#x60; just needs to be an already-existing, active &#x60;ShiftCode&#x60; whose own shape (single interval or two) actually matches &#x60;fillColor&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportColorMappingInput {

  private String rawCode;

  private FillColor fillColor;

  private String shiftCodeId;

  public RosterImportColorMappingInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportColorMappingInput(String rawCode, FillColor fillColor, String shiftCodeId) {
    this.rawCode = rawCode;
    this.fillColor = fillColor;
    this.shiftCodeId = shiftCodeId;
  }

  public RosterImportColorMappingInput rawCode(String rawCode) {
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

  public RosterImportColorMappingInput fillColor(FillColor fillColor) {
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

  public RosterImportColorMappingInput shiftCodeId(String shiftCodeId) {
    this.shiftCodeId = shiftCodeId;
    return this;
  }

  /**
   * Get shiftCodeId
   * @return shiftCodeId
   */
  @NotNull 
  @JsonProperty("shiftCodeId")
  public String getShiftCodeId() {
    return shiftCodeId;
  }

  public void setShiftCodeId(String shiftCodeId) {
    this.shiftCodeId = shiftCodeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportColorMappingInput rosterImportColorMappingInput = (RosterImportColorMappingInput) o;
    return Objects.equals(this.rawCode, rosterImportColorMappingInput.rawCode) &&
        Objects.equals(this.fillColor, rosterImportColorMappingInput.fillColor) &&
        Objects.equals(this.shiftCodeId, rosterImportColorMappingInput.shiftCodeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawCode, fillColor, shiftCodeId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterImportColorMappingInput {\n");
    sb.append("    rawCode: ").append(toIndentedString(rawCode)).append("\n");
    sb.append("    fillColor: ").append(toIndentedString(fillColor)).append("\n");
    sb.append("    shiftCodeId: ").append(toIndentedString(shiftCodeId)).append("\n");
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

