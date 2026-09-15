package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.StaffArea;
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
 * One distinct code found in the sheet, and how it currently resolves to a &#x60;ShiftCode&#x60;. The ambiguous \&quot;9\&quot; is grouped by &#x60;(staffArea, fillColor)&#x60;, not just text, because which &#x60;ShiftCode&#x60; it means is resolved per area - real data confirms this matters: the same area can use both colours of \&quot;9\&quot; (Kitchen does, in different rows or even the same person&#39;s different days), so a single global \&quot;9\&quot; entry would hide that more than one &#x60;(area, colour)&#x60; mapping is actually needed. Every other code is grouped by text alone, since a person only ever needs to *see* a per-area difference in hours here, never to act on one through this endpoint. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportCodeEntry {

  private String rawCode;

  private JsonNullable<FillColor> fillColor = JsonNullable.<FillColor>undefined();

  private JsonNullable<StaffArea> staffArea = JsonNullable.<StaffArea>undefined();

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

  public RosterImportCodeEntry staffArea(StaffArea staffArea) {
    this.staffArea = JsonNullable.of(staffArea);
    return this;
  }

  /**
   * Set only alongside `fillColor` - the area this specific \"9\"/colour combination was found in, needed to call `POST /roster/import/color-mappings`.
   * @return staffArea
   */
  @Valid 
  @JsonProperty("staffArea")
  public JsonNullable<StaffArea> getStaffArea() {
    return staffArea;
  }

  public void setStaffArea(JsonNullable<StaffArea> staffArea) {
    this.staffArea = staffArea;
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
        equalsNullable(this.staffArea, rosterImportCodeEntry.staffArea) &&
        Objects.equals(this.occurrences, rosterImportCodeEntry.occurrences) &&
        Objects.equals(this.resolved, rosterImportCodeEntry.resolved) &&
        equalsNullable(this.shiftCodeDescription, rosterImportCodeEntry.shiftCodeDescription);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawCode, hashCodeNullable(fillColor), hashCodeNullable(staffArea), occurrences, resolved, hashCodeNullable(shiftCodeDescription));
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
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
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

