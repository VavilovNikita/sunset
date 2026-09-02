package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * PENDING = queued, either freshly created or waiting for the next automatic retry. SENT = delivered successfully (terminal). FAILED = automatic retries exhausted - stays `FAILED` from here on regardless of what happens next: a manual retry (`POST /print-jobs/{id}/retry`) either succeeds (moves to `SENT`) or fails again, and dismissing it (`POST /print-jobs/dismiss`, see `PrintJob.dismissedAt`) doesn't change this value at all - it only stops the job counting as needing attention. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum PrintJobStatus {
  
  PENDING("PENDING"),
  
  SENT("SENT"),
  
  FAILED("FAILED");

  private String value;

  PrintJobStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static PrintJobStatus fromValue(String value) {
    for (PrintJobStatus b : PrintJobStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

