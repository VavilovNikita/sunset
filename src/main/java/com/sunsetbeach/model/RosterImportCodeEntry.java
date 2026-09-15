package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.FillColor;
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
 * One distinct code found in the sheet, and how it currently resolves to a &#x60;ShiftCode&#x60;. Grouped by text alone (plus &#x60;fillColor&#x60; for the ambiguous \&quot;9\&quot;) - never by area. Which &#x60;ShiftCode&#x60; a code text actually means for a given cell is resolved separately, per area, at commit time (&#x60;ShiftCodeService#resolveActive&#x60;); what this entry reports is the one fact that&#39;s the same everywhere in the file - \&quot;9\&quot; means exactly two things, told apart by colour, not one pair per department. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportCodeEntry {

  private String rawCode;

  private JsonNullable<FillColor> fillColor = JsonNullable.<FillColor>undefined();

  private Integer occurrences;

  private Boolean resolved;

  private JsonNullable<String> shiftCodeDescription = JsonNullable.<String>undefined();

  public RosterImportCodeEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportCodeEntry(String rawCode, Integer occurrences, Boolean resolved) {
    this.rawCode = rawCode;
    this.occurrences = occurrences;
    this.resolved = resolved;
  }

  public RosterImportCodeEntry rawCode(String rawCode) {
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

  public RosterImportCodeEntry fillColor(FillColor fillColor) {
    this.fillColor = JsonNullable.of(fillColor);
    return this;
  }

  /**
   * Set only for the ambiguous \"9\" - see `FillColor`'s own description. Every other code is read by text alone.
   * @return fillColor
   */
  @Valid 
  @JsonProperty("fillColor")
  public JsonNullable<FillColor> getFillColor() {
    return fillColor;
  }

  public void setFillColor(JsonNullable<FillColor> fillColor) {
    this.fillColor = fillColor;
  }

  public RosterImportCodeEntry occurrences(Integer occurrences) {
    this.occurrences = occurrences;
    return this;
  }

  /**
   * Get occurrences
   * @return occurrences
   */
  @NotNull 
  @JsonProperty("occurrences")
  public Integer getOccurrences() {
    return occurrences;
  }

  public void setOccurrences(Integer occurrences) {
    this.occurrences = occurrences;
  }

  public RosterImportCodeEntry resolved(Boolean resolved) {
    this.resolved = resolved;
    return this;
  }

  /**
   * Get resolved
   * @return resolved
   */
  @NotNull 
  @JsonProperty("resolved")
  public Boolean getResolved() {
    return resolved;
  }

  public void setResolved(Boolean resolved) {
    this.resolved = resolved;
  }

  public RosterImportCodeEntry shiftCodeDescription(String shiftCodeDescription) {
    this.shiftCodeDescription = JsonNullable.of(shiftCodeDescription);
    return this;
  }

  /**
   * The resolved `ShiftCode`'s own hours (e.g. \"07:00-16:00\", \"Open Schedule\") - set only when `resolved` is true.
   * @return shiftCodeDescription
   */
  
  @JsonProperty("shiftCodeDescription")
  public JsonNullable<String> getShiftCodeDescription() {
    return shiftCodeDescription;
  }

  public void setShiftCodeDescription(JsonNullable<String> shiftCodeDescription) {
    this.shiftCodeDescription = shiftCodeDescription;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportCodeEntry rosterImportCodeEntry = (RosterImportCodeEntry) o;
    return Objects.equals(this.rawCode, rosterImportCodeEntry.rawCode) &&
        equalsNullable(this.fillColor, rosterImportCodeEntry.fillColor) &&
        Objects.equals(this.occurrences, rosterImportCodeEntry.occurrences) &&
        Objects.equals(this.resolved, rosterImportCodeEntry.resolved) &&
        equalsNullable(this.shiftCodeDescription, rosterImportCodeEntry.shiftCodeDescription);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawCode, hashCodeNullable(fillColor), occurrences, resolved, hashCodeNullable(shiftCodeDescription));
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
    sb.append("class RosterImportCodeEntry {\n");
    sb.append("    rawCode: ").append(toIndentedString(rawCode)).append("\n");
    sb.append("    fillColor: ").append(toIndentedString(fillColor)).append("\n");
    sb.append("    occurrences: ").append(toIndentedString(occurrences)).append("\n");
    sb.append("    resolved: ").append(toIndentedString(resolved)).append("\n");
    sb.append("    shiftCodeDescription: ").append(toIndentedString(shiftCodeDescription)).append("\n");
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

