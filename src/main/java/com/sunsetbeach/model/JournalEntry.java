package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.JournalLine;
import com.sunsetbeach.model.JournalSourceType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One balanced, immutable posting. &#x60;totalDebit&#x60; always equals &#x60;totalCredit&#x60; - the server rejects anything else before saving. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class JournalEntry {

  private String id;

  private String entryDate;

  private String description;

  private JournalSourceType sourceType;

  private JsonNullable<String> sourceId = JsonNullable.<String>undefined();

  private JsonNullable<String> reversesEntryId = JsonNullable.<String>undefined();

  private JsonNullable<String> reversedByEntryId = JsonNullable.<String>undefined();

  private JsonNullable<String> createdByUserId = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @Valid
  private List<@Valid JournalLine> lines = new ArrayList<>();

  private String totalDebit;

  private String totalCredit;

  public JournalEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JournalEntry(String id, String entryDate, String description, JournalSourceType sourceType, String sourceId, String reversesEntryId, String reversedByEntryId, String createdByUserId, OffsetDateTime createdAt, List<@Valid JournalLine> lines, String totalDebit, String totalCredit) {
    this.id = id;
    this.entryDate = entryDate;
    this.description = description;
    this.sourceType = sourceType;
    this.sourceId = JsonNullable.of(sourceId);
    this.reversesEntryId = JsonNullable.of(reversesEntryId);
    this.reversedByEntryId = JsonNullable.of(reversedByEntryId);
    this.createdByUserId = JsonNullable.of(createdByUserId);
    this.createdAt = createdAt;
    this.lines = lines;
    this.totalDebit = totalDebit;
    this.totalCredit = totalCredit;
  }

  public JournalEntry id(String id) {
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

  public JournalEntry entryDate(String entryDate) {
    this.entryDate = entryDate;
    return this;
  }

  /**
   * The accounting date (Asia/Bangkok). Automatic entries are dated the day they were posted.
   * @return entryDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("entryDate")
  public String getEntryDate() {
    return entryDate;
  }

  public void setEntryDate(String entryDate) {
    this.entryDate = entryDate;
  }

  public JournalEntry description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public JournalEntry sourceType(JournalSourceType sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  /**
   * Get sourceType
   * @return sourceType
   */
  @NotNull @Valid 
  @JsonProperty("sourceType")
  public JournalSourceType getSourceType() {
    return sourceType;
  }

  public void setSourceType(JournalSourceType sourceType) {
    this.sourceType = sourceType;
  }

  public JournalEntry sourceId(String sourceId) {
    this.sourceId = JsonNullable.of(sourceId);
    return this;
  }

  /**
   * Get sourceId
   * @return sourceId
   */
  @NotNull 
  @JsonProperty("sourceId")
  public JsonNullable<String> getSourceId() {
    return sourceId;
  }

  public void setSourceId(JsonNullable<String> sourceId) {
    this.sourceId = sourceId;
  }

  public JournalEntry reversesEntryId(String reversesEntryId) {
    this.reversesEntryId = JsonNullable.of(reversesEntryId);
    return this;
  }

  /**
   * Set on a reversing entry - the entry it mirrors.
   * @return reversesEntryId
   */
  @NotNull 
  @JsonProperty("reversesEntryId")
  public JsonNullable<String> getReversesEntryId() {
    return reversesEntryId;
  }

  public void setReversesEntryId(JsonNullable<String> reversesEntryId) {
    this.reversesEntryId = reversesEntryId;
  }

  public JournalEntry reversedByEntryId(String reversedByEntryId) {
    this.reversedByEntryId = JsonNullable.of(reversedByEntryId);
    return this;
  }

  /**
   * Set once this entry has been reversed - the reversing entry.
   * @return reversedByEntryId
   */
  @NotNull 
  @JsonProperty("reversedByEntryId")
  public JsonNullable<String> getReversedByEntryId() {
    return reversedByEntryId;
  }

  public void setReversedByEntryId(JsonNullable<String> reversedByEntryId) {
    this.reversedByEntryId = reversedByEntryId;
  }

  public JournalEntry createdByUserId(String createdByUserId) {
    this.createdByUserId = JsonNullable.of(createdByUserId);
    return this;
  }

  /**
   * The staff member whose action posted it; null only if none was signed in.
   * @return createdByUserId
   */
  @NotNull 
  @JsonProperty("createdByUserId")
  public JsonNullable<String> getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(JsonNullable<String> createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public JournalEntry createdAt(OffsetDateTime createdAt) {
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

  public JournalEntry lines(List<@Valid JournalLine> lines) {
    this.lines = lines;
    return this;
  }

  public JournalEntry addLinesItem(JournalLine linesItem) {
    if (this.lines == null) {
      this.lines = new ArrayList<>();
    }
    this.lines.add(linesItem);
    return this;
  }

  /**
   * Get lines
   * @return lines
   */
  @NotNull @Valid 
  @JsonProperty("lines")
  public List<@Valid JournalLine> getLines() {
    return lines;
  }

  public void setLines(List<@Valid JournalLine> lines) {
    this.lines = lines;
  }

  public JournalEntry totalDebit(String totalDebit) {
    this.totalDebit = totalDebit;
    return this;
  }

  /**
   * Get totalDebit
   * @return totalDebit
   */
  @NotNull 
  @JsonProperty("totalDebit")
  public String getTotalDebit() {
    return totalDebit;
  }

  public void setTotalDebit(String totalDebit) {
    this.totalDebit = totalDebit;
  }

  public JournalEntry totalCredit(String totalCredit) {
    this.totalCredit = totalCredit;
    return this;
  }

  /**
   * Get totalCredit
   * @return totalCredit
   */
  @NotNull 
  @JsonProperty("totalCredit")
  public String getTotalCredit() {
    return totalCredit;
  }

  public void setTotalCredit(String totalCredit) {
    this.totalCredit = totalCredit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JournalEntry journalEntry = (JournalEntry) o;
    return Objects.equals(this.id, journalEntry.id) &&
        Objects.equals(this.entryDate, journalEntry.entryDate) &&
        Objects.equals(this.description, journalEntry.description) &&
        Objects.equals(this.sourceType, journalEntry.sourceType) &&
        Objects.equals(this.sourceId, journalEntry.sourceId) &&
        Objects.equals(this.reversesEntryId, journalEntry.reversesEntryId) &&
        Objects.equals(this.reversedByEntryId, journalEntry.reversedByEntryId) &&
        Objects.equals(this.createdByUserId, journalEntry.createdByUserId) &&
        Objects.equals(this.createdAt, journalEntry.createdAt) &&
        Objects.equals(this.lines, journalEntry.lines) &&
        Objects.equals(this.totalDebit, journalEntry.totalDebit) &&
        Objects.equals(this.totalCredit, journalEntry.totalCredit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, entryDate, description, sourceType, sourceId, reversesEntryId, reversedByEntryId, createdByUserId, createdAt, lines, totalDebit, totalCredit);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JournalEntry {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    entryDate: ").append(toIndentedString(entryDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    sourceType: ").append(toIndentedString(sourceType)).append("\n");
    sb.append("    sourceId: ").append(toIndentedString(sourceId)).append("\n");
    sb.append("    reversesEntryId: ").append(toIndentedString(reversesEntryId)).append("\n");
    sb.append("    reversedByEntryId: ").append(toIndentedString(reversedByEntryId)).append("\n");
    sb.append("    createdByUserId: ").append(toIndentedString(createdByUserId)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    lines: ").append(toIndentedString(lines)).append("\n");
    sb.append("    totalDebit: ").append(toIndentedString(totalDebit)).append("\n");
    sb.append("    totalCredit: ").append(toIndentedString(totalCredit)).append("\n");
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

