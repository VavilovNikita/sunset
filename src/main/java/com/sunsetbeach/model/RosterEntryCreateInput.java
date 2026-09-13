package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Body of &#x60;POST /roster/entries&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterEntryCreateInput {

  private String employeeUserId;

  private String date;

  private String shiftCodeId;

  private JsonNullable<String> note = JsonNullable.<String>undefined();

  public RosterEntryCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterEntryCreateInput(String employeeUserId, String date, String shiftCodeId) {
    this.employeeUserId = employeeUserId;
    this.date = date;
    this.shiftCodeId = shiftCodeId;
  }

  public RosterEntryCreateInput employeeUserId(String employeeUserId) {
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

  public RosterEntryCreateInput date(String date) {
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

  public RosterEntryCreateInput shiftCodeId(String shiftCodeId) {
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

  public RosterEntryCreateInput note(String note) {
    this.note = JsonNullable.of(note);
    return this;
  }

  /**
   * Get note
   * @return note
   */
  
  @JsonProperty("note")
  public JsonNullable<String> getNote() {
    return note;
  }

  public void setNote(JsonNullable<String> note) {
    this.note = note;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterEntryCreateInput rosterEntryCreateInput = (RosterEntryCreateInput) o;
    return Objects.equals(this.employeeUserId, rosterEntryCreateInput.employeeUserId) &&
        Objects.equals(this.date, rosterEntryCreateInput.date) &&
        Objects.equals(this.shiftCodeId, rosterEntryCreateInput.shiftCodeId) &&
        equalsNullable(this.note, rosterEntryCreateInput.note);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeUserId, date, shiftCodeId, hashCodeNullable(note));
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
    sb.append("class RosterEntryCreateInput {\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    shiftCodeId: ").append(toIndentedString(shiftCodeId)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
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

