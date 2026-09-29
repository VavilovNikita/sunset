package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.SiteMinderImportAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * SiteMinderImportResult
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SiteMinderImportResult {

  private SiteMinderImportAction action;

  private JsonNullable<String> bookingId = JsonNullable.<String>undefined();

  @Valid
  private List<String> changes = new ArrayList<>();

  @Valid
  private List<String> warnings = new ArrayList<>();

  private JsonNullable<String> message = JsonNullable.<String>undefined();

  public SiteMinderImportResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SiteMinderImportResult(SiteMinderImportAction action, String bookingId, List<String> changes, List<String> warnings, String message) {
    this.action = action;
    this.bookingId = JsonNullable.of(bookingId);
    this.changes = changes;
    this.warnings = warnings;
    this.message = JsonNullable.of(message);
  }

  public SiteMinderImportResult action(SiteMinderImportAction action) {
    this.action = action;
    return this;
  }

  /**
   * Get action
   * @return action
   */
  @NotNull @Valid 
  @JsonProperty("action")
  public SiteMinderImportAction getAction() {
    return action;
  }

  public void setAction(SiteMinderImportAction action) {
    this.action = action;
  }

  public SiteMinderImportResult bookingId(String bookingId) {
    this.bookingId = JsonNullable.of(bookingId);
    return this;
  }

  /**
   * The booking created or matched. Null only for a `SKIPPED` reservation that was never imported.
   * @return bookingId
   */
  @NotNull 
  @JsonProperty("bookingId")
  public JsonNullable<String> getBookingId() {
    return bookingId;
  }

  public void setBookingId(JsonNullable<String> bookingId) {
    this.bookingId = bookingId;
  }

  public SiteMinderImportResult changes(List<String> changes) {
    this.changes = changes;
    return this;
  }

  public SiteMinderImportResult addChangesItem(String changesItem) {
    if (this.changes == null) {
      this.changes = new ArrayList<>();
    }
    this.changes.add(changesItem);
    return this;
  }

  /**
   * What was changed, one entry per change, for the script's log. Empty unless `UPDATED`.
   * @return changes
   */
  @NotNull 
  @JsonProperty("changes")
  public List<String> getChanges() {
    return changes;
  }

  public void setChanges(List<String> changes) {
    this.changes = changes;
  }

  public SiteMinderImportResult warnings(List<String> warnings) {
    this.warnings = warnings;
    return this;
  }

  public SiteMinderImportResult addWarningsItem(String warningsItem) {
    if (this.warnings == null) {
      this.warnings = new ArrayList<>();
    }
    this.warnings.add(warningsItem);
    return this;
  }

  /**
   * Changes applied with a caveat a person should know about - e.g. the assigned room wasn't free for the new dates and was unassigned, or the price of a `PAID` booking changed (which posts nothing to the ledger). 
   * @return warnings
   */
  @NotNull 
  @JsonProperty("warnings")
  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  public SiteMinderImportResult message(String message) {
    this.message = JsonNullable.of(message);
    return this;
  }

  /**
   * Why a reservation was `SKIPPED`. Null otherwise.
   * @return message
   */
  @NotNull 
  @JsonProperty("message")
  public JsonNullable<String> getMessage() {
    return message;
  }

  public void setMessage(JsonNullable<String> message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SiteMinderImportResult siteMinderImportResult = (SiteMinderImportResult) o;
    return Objects.equals(this.action, siteMinderImportResult.action) &&
        Objects.equals(this.bookingId, siteMinderImportResult.bookingId) &&
        Objects.equals(this.changes, siteMinderImportResult.changes) &&
        Objects.equals(this.warnings, siteMinderImportResult.warnings) &&
        Objects.equals(this.message, siteMinderImportResult.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, bookingId, changes, warnings, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SiteMinderImportResult {\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    changes: ").append(toIndentedString(changes)).append("\n");
    sb.append("    warnings: ").append(toIndentedString(warnings)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

