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
 * Body of &#x60;POST /roster/import/commit&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportCommitInput {

  private String importId;

  public RosterImportCommitInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportCommitInput(String importId) {
    this.importId = importId;
  }

  public RosterImportCommitInput importId(String importId) {
    this.importId = importId;
    return this;
  }

  /**
   * Get importId
   * @return importId
   */
  @NotNull 
  @JsonProperty("importId")
  public String getImportId() {
    return importId;
  }

  public void setImportId(String importId) {
    this.importId = importId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportCommitInput rosterImportCommitInput = (RosterImportCommitInput) o;
    return Objects.equals(this.importId, rosterImportCommitInput.importId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(importId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterImportCommitInput {\n");
    sb.append("    importId: ").append(toIndentedString(importId)).append("\n");
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

