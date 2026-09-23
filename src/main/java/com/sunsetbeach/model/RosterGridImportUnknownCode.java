package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * A code found in the file with no export metadata at all for this cell - the row and/or the code text were added or retyped by hand after export, and this exact text has no active &#x60;ShiftCode&#x60; for this area either. There is nothing to prefill from - resolve with a &#x60;RosterGridImportUnknownCodeResolution&#x60; carrying a full new definition, the same fields &#x60;POST /shift-codes&#x60; itself takes. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterGridImportUnknownCode {

  private String rawCode;

  private JsonNullable<StaffArea> staffArea = JsonNullable.<StaffArea>undefined();

  private Integer occurrences;

  public RosterGridImportUnknownCode() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterGridImportUnknownCode(String rawCode, Integer occurrences) {
    this.rawCode = rawCode;
    this.occurrences = occurrences;
  }

  public RosterGridImportUnknownCode rawCode(String rawCode) {
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

  public RosterGridImportUnknownCode staffArea(StaffArea staffArea) {
    this.staffArea = JsonNullable.of(staffArea);
    return this;
  }

  /**
   * The area of the row(s) this code text was found on - null only if that row's own department couldn't be determined.
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

  public RosterGridImportUnknownCode occurrences(Integer occurrences) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterGridImportUnknownCode rosterGridImportUnknownCode = (RosterGridImportUnknownCode) o;
    return Objects.equals(this.rawCode, rosterGridImportUnknownCode.rawCode) &&
        equalsNullable(this.staffArea, rosterGridImportUnknownCode.staffArea) &&
        Objects.equals(this.occurrences, rosterGridImportUnknownCode.occurrences);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawCode, hashCodeNullable(staffArea), occurrences);
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
    sb.append("class RosterGridImportUnknownCode {\n");
    sb.append("    rawCode: ").append(toIndentedString(rawCode)).append("\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
    sb.append("    occurrences: ").append(toIndentedString(occurrences)).append("\n");
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

