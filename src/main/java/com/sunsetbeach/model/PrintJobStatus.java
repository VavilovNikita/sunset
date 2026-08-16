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
 * PENDING = queued, either freshly created or waiting for the next automatic retry. SENT = delivered successfully (terminal). FAILED = automatic retries exhausted (terminal until a manual retry via `POST /print-jobs/{id}/retry` puts it back in play). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-16T20:25:37.858402100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
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

