package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;POST /ledger/entries/{id}/reverse&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class JournalEntryReverseInput {

  private String description;

  public JournalEntryReverseInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JournalEntryReverseInput(String description) {
    this.description = description;
  }

  public JournalEntryReverseInput description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Why it is being reversed - becomes the reversing entry's description.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JournalEntryReverseInput journalEntryReverseInput = (JournalEntryReverseInput) o;
    return Objects.equals(this.description, journalEntryReverseInput.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JournalEntryReverseInput {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

