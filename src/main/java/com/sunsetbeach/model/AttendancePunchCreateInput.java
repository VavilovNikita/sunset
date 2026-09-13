package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.PunchDirection;
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
 * Body of &#x60;POST /attendance&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class AttendancePunchCreateInput {

  private String employeeUserId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime punchAt;

  private PunchDirection direction;

  private JsonNullable<String> note = JsonNullable.<String>undefined();

  public AttendancePunchCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AttendancePunchCreateInput(String employeeUserId, OffsetDateTime punchAt, PunchDirection direction) {
    this.employeeUserId = employeeUserId;
    this.punchAt = punchAt;
    this.direction = direction;
  }

  public AttendancePunchCreateInput employeeUserId(String employeeUserId) {
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

  public AttendancePunchCreateInput punchAt(OffsetDateTime punchAt) {
    this.punchAt = punchAt;
    return this;
  }

  /**
   * Get punchAt
   * @return punchAt
   */
  @NotNull @Valid 
  @JsonProperty("punchAt")
  public OffsetDateTime getPunchAt() {
    return punchAt;
  }

  public void setPunchAt(OffsetDateTime punchAt) {
    this.punchAt = punchAt;
  }

  public AttendancePunchCreateInput direction(PunchDirection direction) {
    this.direction = direction;
    return this;
  }

  /**
   * Get direction
   * @return direction
   */
  @NotNull @Valid 
  @JsonProperty("direction")
  public PunchDirection getDirection() {
    return direction;
  }

  public void setDirection(PunchDirection direction) {
    this.direction = direction;
  }

  public AttendancePunchCreateInput note(String note) {
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
    AttendancePunchCreateInput attendancePunchCreateInput = (AttendancePunchCreateInput) o;
    return Objects.equals(this.employeeUserId, attendancePunchCreateInput.employeeUserId) &&
        Objects.equals(this.punchAt, attendancePunchCreateInput.punchAt) &&
        Objects.equals(this.direction, attendancePunchCreateInput.direction) &&
        equalsNullable(this.note, attendancePunchCreateInput.note);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeUserId, punchAt, direction, hashCodeNullable(note));
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
    sb.append("class AttendancePunchCreateInput {\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    punchAt: ").append(toIndentedString(punchAt)).append("\n");
    sb.append("    direction: ").append(toIndentedString(direction)).append("\n");
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

