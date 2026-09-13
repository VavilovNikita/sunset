package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.PunchSource;
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
 * One raw clock-in or clock-out - not a paired session. A split shift&#39;s four punches are simply four rows; pairing consecutive &#x60;IN&#x60;/&#x60;OUT&#x60; punches into worked intervals happens at read time (&#x60;GET /attendance/summary&#x60;), never at write time, so nothing here needs to know in advance whether a day is a single shift, a split, or &#x60;OP&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class AttendancePunch {

  private String id;

  private String employeeUserId;

  private String employeeEmail;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime punchAt;

  private PunchDirection direction;

  private PunchSource source;

  private JsonNullable<String> recordedByEmail = JsonNullable.<String>undefined();

  private JsonNullable<String> note = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public AttendancePunch() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AttendancePunch(String id, String employeeUserId, String employeeEmail, OffsetDateTime punchAt, PunchDirection direction, PunchSource source, OffsetDateTime createdAt) {
    this.id = id;
    this.employeeUserId = employeeUserId;
    this.employeeEmail = employeeEmail;
    this.punchAt = punchAt;
    this.direction = direction;
    this.source = source;
    this.createdAt = createdAt;
  }

  public AttendancePunch id(String id) {
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

  public AttendancePunch employeeUserId(String employeeUserId) {
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

  public AttendancePunch employeeEmail(String employeeEmail) {
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

  public AttendancePunch punchAt(OffsetDateTime punchAt) {
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

  public AttendancePunch direction(PunchDirection direction) {
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

  public AttendancePunch source(PunchSource source) {
    this.source = source;
    return this;
  }

  /**
   * Get source
   * @return source
   */
  @NotNull @Valid 
  @JsonProperty("source")
  public PunchSource getSource() {
    return source;
  }

  public void setSource(PunchSource source) {
    this.source = source;
  }

  public AttendancePunch recordedByEmail(String recordedByEmail) {
    this.recordedByEmail = JsonNullable.of(recordedByEmail);
    return this;
  }

  /**
   * Who typed this in, for a `MANUAL` punch. Null for `SCANNER`.
   * @return recordedByEmail
   */
  
  @JsonProperty("recordedByEmail")
  public JsonNullable<String> getRecordedByEmail() {
    return recordedByEmail;
  }

  public void setRecordedByEmail(JsonNullable<String> recordedByEmail) {
    this.recordedByEmail = recordedByEmail;
  }

  public AttendancePunch note(String note) {
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

  public AttendancePunch createdAt(OffsetDateTime createdAt) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttendancePunch attendancePunch = (AttendancePunch) o;
    return Objects.equals(this.id, attendancePunch.id) &&
        Objects.equals(this.employeeUserId, attendancePunch.employeeUserId) &&
        Objects.equals(this.employeeEmail, attendancePunch.employeeEmail) &&
        Objects.equals(this.punchAt, attendancePunch.punchAt) &&
        Objects.equals(this.direction, attendancePunch.direction) &&
        Objects.equals(this.source, attendancePunch.source) &&
        equalsNullable(this.recordedByEmail, attendancePunch.recordedByEmail) &&
        equalsNullable(this.note, attendancePunch.note) &&
        Objects.equals(this.createdAt, attendancePunch.createdAt);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, employeeUserId, employeeEmail, punchAt, direction, source, hashCodeNullable(recordedByEmail), hashCodeNullable(note), createdAt);
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
    sb.append("class AttendancePunch {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    employeeEmail: ").append(toIndentedString(employeeEmail)).append("\n");
    sb.append("    punchAt: ").append(toIndentedString(punchAt)).append("\n");
    sb.append("    direction: ").append(toIndentedString(direction)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    recordedByEmail: ").append(toIndentedString(recordedByEmail)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

