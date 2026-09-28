package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The record that one hotel-local date was reviewed and closed.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NightAuditClosure {

  private String date;

  private String closedByUserId;

  private String closedByName;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime closedAt;

  private JsonNullable<String> notes = JsonNullable.<String>undefined();

  public NightAuditClosure() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NightAuditClosure(String date, String closedByUserId, String closedByName, OffsetDateTime closedAt, String notes) {
    this.date = date;
    this.closedByUserId = closedByUserId;
    this.closedByName = closedByName;
    this.closedAt = closedAt;
    this.notes = JsonNullable.of(notes);
  }

  public NightAuditClosure date(String date) {
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

  public NightAuditClosure closedByUserId(String closedByUserId) {
    this.closedByUserId = closedByUserId;
    return this;
  }

  /**
   * Get closedByUserId
   * @return closedByUserId
   */
  @NotNull 
  @JsonProperty("closedByUserId")
  public String getClosedByUserId() {
    return closedByUserId;
  }

  public void setClosedByUserId(String closedByUserId) {
    this.closedByUserId = closedByUserId;
  }

  public NightAuditClosure closedByName(String closedByName) {
    this.closedByName = closedByName;
    return this;
  }

  /**
   * The closing user's current `User.name`, read live - a rename relabels past closures too.
   * @return closedByName
   */
  @NotNull 
  @JsonProperty("closedByName")
  public String getClosedByName() {
    return closedByName;
  }

  public void setClosedByName(String closedByName) {
    this.closedByName = closedByName;
  }

  public NightAuditClosure closedAt(OffsetDateTime closedAt) {
    this.closedAt = closedAt;
    return this;
  }

  /**
   * Get closedAt
   * @return closedAt
   */
  @NotNull @Valid 
  @JsonProperty("closedAt")
  public OffsetDateTime getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(OffsetDateTime closedAt) {
    this.closedAt = closedAt;
  }

  public NightAuditClosure notes(String notes) {
    this.notes = JsonNullable.of(notes);
    return this;
  }

  /**
   * Get notes
   * @return notes
   */
  @NotNull 
  @JsonProperty("notes")
  public JsonNullable<String> getNotes() {
    return notes;
  }

  public void setNotes(JsonNullable<String> notes) {
    this.notes = notes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NightAuditClosure nightAuditClosure = (NightAuditClosure) o;
    return Objects.equals(this.date, nightAuditClosure.date) &&
        Objects.equals(this.closedByUserId, nightAuditClosure.closedByUserId) &&
        Objects.equals(this.closedByName, nightAuditClosure.closedByName) &&
        Objects.equals(this.closedAt, nightAuditClosure.closedAt) &&
        Objects.equals(this.notes, nightAuditClosure.notes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, closedByUserId, closedByName, closedAt, notes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NightAuditClosure {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    closedByUserId: ").append(toIndentedString(closedByUserId)).append("\n");
    sb.append("    closedByName: ").append(toIndentedString(closedByName)).append("\n");
    sb.append("    closedAt: ").append(toIndentedString(closedAt)).append("\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
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

