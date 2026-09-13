package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.ShiftCode;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One employee&#39;s shift on one date. A day off is the *absence* of a row here, never a row of its own - see this schema&#39;s own module description for why that&#39;s deliberate. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterEntry {

  private String id;

  private String employeeUserId;

  private String employeeEmail;

  private String date;

  private ShiftCode shiftCode;

  private JsonNullable<String> note = JsonNullable.<String>undefined();

  private Boolean locked;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public RosterEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterEntry(String id, String employeeUserId, String employeeEmail, String date, ShiftCode shiftCode, Boolean locked, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.employeeUserId = employeeUserId;
    this.employeeEmail = employeeEmail;
    this.date = date;
    this.shiftCode = shiftCode;
    this.locked = locked;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public RosterEntry id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public RosterEntry employeeUserId(String employeeUserId) {
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

  public RosterEntry employeeEmail(String employeeEmail) {
    this.employeeEmail = employeeEmail;
    return this;
  }

  /**
   * Get employeeEmail
   * @return employeeEmail
   */
  @NotNull 
  @JsonProperty("employeeEmail")
  public String getEmployeeEmail() {
    return employeeEmail;
  }

  public void setEmployeeEmail(String employeeEmail) {
    this.employeeEmail = employeeEmail;
  }

  public RosterEntry date(String date) {
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

  public RosterEntry shiftCode(ShiftCode shiftCode) {
    this.shiftCode = shiftCode;
    return this;
  }

  /**
   * Get shiftCode
   * @return shiftCode
   */
  @NotNull @Valid 
  @JsonProperty("shiftCode")
  public ShiftCode getShiftCode() {
    return shiftCode;
  }

  public void setShiftCode(ShiftCode shiftCode) {
    this.shiftCode = shiftCode;
  }

  public RosterEntry note(String note) {
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

  public RosterEntry locked(Boolean locked) {
    this.locked = locked;
    return this;
  }

  /**
   * \"Do not move\" - see `PATCH /roster/entries/{id}/lock`.
   * @return locked
   */
  @NotNull 
  @JsonProperty("locked")
  public Boolean getLocked() {
    return locked;
  }

  public void setLocked(Boolean locked) {
    this.locked = locked;
  }

  public RosterEntry createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public RosterEntry updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterEntry rosterEntry = (RosterEntry) o;
    return Objects.equals(this.id, rosterEntry.id) &&
        Objects.equals(this.employeeUserId, rosterEntry.employeeUserId) &&
        Objects.equals(this.employeeEmail, rosterEntry.employeeEmail) &&
        Objects.equals(this.date, rosterEntry.date) &&
        Objects.equals(this.shiftCode, rosterEntry.shiftCode) &&
        equalsNullable(this.note, rosterEntry.note) &&
        Objects.equals(this.locked, rosterEntry.locked) &&
        Objects.equals(this.createdAt, rosterEntry.createdAt) &&
        Objects.equals(this.updatedAt, rosterEntry.updatedAt);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, employeeUserId, employeeEmail, date, shiftCode, hashCodeNullable(note), locked, createdAt, updatedAt);
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
    sb.append("class RosterEntry {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    employeeEmail: ").append(toIndentedString(employeeEmail)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    shiftCode: ").append(toIndentedString(shiftCode)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    locked: ").append(toIndentedString(locked)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

