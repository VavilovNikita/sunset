package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintJobStatus;
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
 * A queued/attempted print. &#x60;payload&#x60; (the raw ESC/POS bytes actually sent) is intentionally not exposed here - it&#39;s binary and only meaningful to a printer. Dismissing a job (&#x60;POST /print-jobs/dismiss&#x60;) never deletes it or changes &#x60;status&#x60; - a dismissed job is still &#x60;FAILED&#x60;, just marked as not needing further attention. See &#x60;dismissedAt&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PrintJob {

  private String id;

  private String printerId;

  private PrintDocumentType documentType;

  private String summary;

  private PrintJobStatus status;

  private Integer attempts;

  private JsonNullable<String> lastError = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> dismissedAt = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> dismissedByUserId = JsonNullable.<String>undefined();

  private JsonNullable<String> dismissNote = JsonNullable.<String>undefined();

  public PrintJob() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrintJob(String id, String printerId, PrintDocumentType documentType, String summary, PrintJobStatus status, Integer attempts, String lastError, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.printerId = printerId;
    this.documentType = documentType;
    this.summary = summary;
    this.status = status;
    this.attempts = attempts;
    this.lastError = JsonNullable.of(lastError);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public PrintJob id(String id) {
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

  public PrintJob printerId(String printerId) {
    this.printerId = printerId;
    return this;
  }

  /**
   * Get printerId
   * @return printerId
   */
  @NotNull 
  @JsonProperty("printerId")
  public String getPrinterId() {
    return printerId;
  }

  public void setPrinterId(String printerId) {
    this.printerId = printerId;
  }

  public PrintJob documentType(PrintDocumentType documentType) {
    this.documentType = documentType;
    return this;
  }

  /**
   * Get documentType
   * @return documentType
   */
  @NotNull @Valid 
  @JsonProperty("documentType")
  public PrintDocumentType getDocumentType() {
    return documentType;
  }

  public void setDocumentType(PrintDocumentType documentType) {
    this.documentType = documentType;
  }

  public PrintJob summary(String summary) {
    this.summary = summary;
    return this;
  }

  /**
   * Human-readable one-liner, e.g. \"Kitchen ticket — Order #ab12cd34\" or \"Bar ticket — Order #ab12cd34\" or, for a `GUEST_RECEIPT`, \"Guest receipt — Order #ab12cd34 (CARD)\" / \"Guest receipt — Order #ab12cd34 (ROOM_CHARGE — Jane Doe)\" - enough to tell two receipts on the same order apart without opening `GET /print-jobs/{id}/preview`. Never carries a money amount. 
   * @return summary
   */
  @NotNull 
  @JsonProperty("summary")
  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public PrintJob status(PrintJobStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public PrintJobStatus getStatus() {
    return status;
  }

  public void setStatus(PrintJobStatus status) {
    this.status = status;
  }

  public PrintJob attempts(Integer attempts) {
    this.attempts = attempts;
    return this;
  }

  /**
   * Number of delivery attempts made so far (automatic + manual).
   * @return attempts
   */
  @NotNull 
  @JsonProperty("attempts")
  public Integer getAttempts() {
    return attempts;
  }

  public void setAttempts(Integer attempts) {
    this.attempts = attempts;
  }

  public PrintJob lastError(String lastError) {
    this.lastError = JsonNullable.of(lastError);
    return this;
  }

  /**
   * Exception message from the most recent failed attempt; null if it has never failed.
   * @return lastError
   */
  @NotNull 
  @JsonProperty("lastError")
  public JsonNullable<String> getLastError() {
    return lastError;
  }

  public void setLastError(JsonNullable<String> lastError) {
    this.lastError = lastError;
  }

  public PrintJob createdAt(OffsetDateTime createdAt) {
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

  public PrintJob updatedAt(OffsetDateTime updatedAt) {
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

  public PrintJob dismissedAt(OffsetDateTime dismissedAt) {
    this.dismissedAt = JsonNullable.of(dismissedAt);
    return this;
  }

  /**
   * Set once this job is closed as not-actionable (`POST /print-jobs/dismiss`) - null for every job by default. `GET /print-jobs` excludes dismissed jobs unless `includeDismissed=true` is passed. 
   * @return dismissedAt
   */
  @Valid 
  @JsonProperty("dismissedAt")
  public JsonNullable<OffsetDateTime> getDismissedAt() {
    return dismissedAt;
  }

  public void setDismissedAt(JsonNullable<OffsetDateTime> dismissedAt) {
    this.dismissedAt = dismissedAt;
  }

  public PrintJob dismissedByUserId(String dismissedByUserId) {
    this.dismissedByUserId = JsonNullable.of(dismissedByUserId);
    return this;
  }

  /**
   * Get dismissedByUserId
   * @return dismissedByUserId
   */
  
  @JsonProperty("dismissedByUserId")
  public JsonNullable<String> getDismissedByUserId() {
    return dismissedByUserId;
  }

  public void setDismissedByUserId(JsonNullable<String> dismissedByUserId) {
    this.dismissedByUserId = dismissedByUserId;
  }

  public PrintJob dismissNote(String dismissNote) {
    this.dismissNote = JsonNullable.of(dismissNote);
    return this;
  }

  /**
   * Optional free-text context supplied at dismiss time (e.g. \"printer replaced 2026-09-02\").
   * @return dismissNote
   */
  
  @JsonProperty("dismissNote")
  public JsonNullable<String> getDismissNote() {
    return dismissNote;
  }

  public void setDismissNote(JsonNullable<String> dismissNote) {
    this.dismissNote = dismissNote;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrintJob printJob = (PrintJob) o;
    return Objects.equals(this.id, printJob.id) &&
        Objects.equals(this.printerId, printJob.printerId) &&
        Objects.equals(this.documentType, printJob.documentType) &&
        Objects.equals(this.summary, printJob.summary) &&
        Objects.equals(this.status, printJob.status) &&
        Objects.equals(this.attempts, printJob.attempts) &&
        Objects.equals(this.lastError, printJob.lastError) &&
        Objects.equals(this.createdAt, printJob.createdAt) &&
        Objects.equals(this.updatedAt, printJob.updatedAt) &&
        equalsNullable(this.dismissedAt, printJob.dismissedAt) &&
        equalsNullable(this.dismissedByUserId, printJob.dismissedByUserId) &&
        equalsNullable(this.dismissNote, printJob.dismissNote);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, printerId, documentType, summary, status, attempts, lastError, createdAt, updatedAt, hashCodeNullable(dismissedAt), hashCodeNullable(dismissedByUserId), hashCodeNullable(dismissNote));
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
    sb.append("class PrintJob {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    printerId: ").append(toIndentedString(printerId)).append("\n");
    sb.append("    documentType: ").append(toIndentedString(documentType)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    attempts: ").append(toIndentedString(attempts)).append("\n");
    sb.append("    lastError: ").append(toIndentedString(lastError)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("    dismissedAt: ").append(toIndentedString(dismissedAt)).append("\n");
    sb.append("    dismissedByUserId: ").append(toIndentedString(dismissedByUserId)).append("\n");
    sb.append("    dismissNote: ").append(toIndentedString(dismissNote)).append("\n");
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

