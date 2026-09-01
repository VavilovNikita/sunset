package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.PrintJob;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Result of an on-demand print action that may legitimately have nothing to print to.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PrintAttemptResult {

  private Boolean attempted;

  private PrintJob job;

  public PrintAttemptResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrintAttemptResult(Boolean attempted) {
    this.attempted = attempted;
  }

  public PrintAttemptResult attempted(Boolean attempted) {
    this.attempted = attempted;
    return this;
  }

  /**
   * false when there was no active printer configured for the target department - nothing was queued.
   * @return attempted
   */
  @NotNull 
  @JsonProperty("attempted")
  public Boolean getAttempted() {
    return attempted;
  }

  public void setAttempted(Boolean attempted) {
    this.attempted = attempted;
  }

  public PrintAttemptResult job(PrintJob job) {
    this.job = job;
    return this;
  }

  /**
   * Get job
   * @return job
   */
  @Valid 
  @JsonProperty("job")
  public PrintJob getJob() {
    return job;
  }

  public void setJob(PrintJob job) {
    this.job = job;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrintAttemptResult printAttemptResult = (PrintAttemptResult) o;
    return Objects.equals(this.attempted, printAttemptResult.attempted) &&
        Objects.equals(this.job, printAttemptResult.job);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attempted, job);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrintAttemptResult {\n");
    sb.append("    attempted: ").append(toIndentedString(attempted)).append("\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
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

