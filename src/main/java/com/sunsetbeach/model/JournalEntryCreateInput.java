package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.JournalLineInput;
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
 * Body of &#x60;POST /ledger/entries&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class JournalEntryCreateInput {

  private String entryDate;

  private String description;

  @Valid
  private List<@Valid JournalLineInput> lines = new ArrayList<>();

  public JournalEntryCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JournalEntryCreateInput(String entryDate, String description, List<@Valid JournalLineInput> lines) {
    this.entryDate = entryDate;
    this.description = description;
    this.lines = lines;
  }

  public JournalEntryCreateInput entryDate(String entryDate) {
    this.entryDate = entryDate;
    return this;
  }

  /**
   * The accounting date, not in the future.
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

  public JournalEntryCreateInput description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull @Size(max = 500) 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public JournalEntryCreateInput lines(List<@Valid JournalLineInput> lines) {
    this.lines = lines;
    return this;
  }

  public JournalEntryCreateInput addLinesItem(JournalLineInput linesItem) {
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
  @NotNull @Valid @Size(min = 2) 
  @JsonProperty("lines")
  public List<@Valid JournalLineInput> getLines() {
    return lines;
  }

  public void setLines(List<@Valid JournalLineInput> lines) {
    this.lines = lines;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JournalEntryCreateInput journalEntryCreateInput = (JournalEntryCreateInput) o;
    return Objects.equals(this.entryDate, journalEntryCreateInput.entryDate) &&
        Objects.equals(this.description, journalEntryCreateInput.description) &&
        Objects.equals(this.lines, journalEntryCreateInput.lines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entryDate, description, lines);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JournalEntryCreateInput {\n");
    sb.append("    entryDate: ").append(toIndentedString(entryDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    lines: ").append(toIndentedString(lines)).append("\n");
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

