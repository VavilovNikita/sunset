package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Body of &#x60;POST /print-jobs/dismiss&#x60;. One call closes one or many jobs at once (a single job is just an array of one) - all-or-nothing, same as &#x60;PATCH /room-units/positions&#x60;: every id is checked (exists, visible to the caller&#39;s role, currently &#x60;FAILED&#x60;, not already dismissed) before any write happens. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class DismissPrintJobsInput {

  @Valid
  private List<String> ids = new ArrayList<>();

  private String note;

  public DismissPrintJobsInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DismissPrintJobsInput(List<String> ids) {
    this.ids = ids;
  }

  public DismissPrintJobsInput ids(List<String> ids) {
    this.ids = ids;
    return this;
  }

  public DismissPrintJobsInput addIdsItem(String idsItem) {
    if (this.ids == null) {
      this.ids = new ArrayList<>();
    }
    this.ids.add(idsItem);
    return this;
  }

  /**
   * Get ids
   * @return ids
   */
  @NotNull @Size(min = 1) 
  @JsonProperty("ids")
  public List<String> getIds() {
    return ids;
  }

  public void setIds(List<String> ids) {
    this.ids = ids;
  }

  public DismissPrintJobsInput note(String note) {
    this.note = note;
    return this;
  }

  /**
   * Optional, applied to every job in this call - shown on each job and in its own audit-log entry.
   * @return note
   */
  
  @JsonProperty("note")
  public String getNote() {
    return note;
  }

  public void setNote(String note) {
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
    DismissPrintJobsInput dismissPrintJobsInput = (DismissPrintJobsInput) o;
    return Objects.equals(this.ids, dismissPrintJobsInput.ids) &&
        Objects.equals(this.note, dismissPrintJobsInput.note);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ids, note);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DismissPrintJobsInput {\n");
    sb.append("    ids: ").append(toIndentedString(ids)).append("\n");
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

