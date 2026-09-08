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
 * Forward-only in practice (see `PATCH /maintenance-tasks/{id}/status`): OPEN -> IN_PROGRESS -> DONE, skipping IN_PROGRESS allowed, going backward or repeating the current status is not. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum MaintenanceTaskStatus {
  
  OPEN("OPEN"),
  
  IN_PROGRESS("IN_PROGRESS"),
  
  DONE("DONE");

  private String value;

  MaintenanceTaskStatus(String value) {
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
  public static MaintenanceTaskStatus fromValue(String value) {
    for (MaintenanceTaskStatus b : MaintenanceTaskStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

