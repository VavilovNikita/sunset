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
 * `PENDING` until decided. `APPROVED` / `REJECTED` by a decider, `CANCELLED` withdrawn, `EXPIRED` - the shift began undecided, or the entry changed after the request was filed so nothing was applied (`expiredReason` says which). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum ShiftChangeRequestStatus {
  
  PENDING("PENDING"),
  
  APPROVED("APPROVED"),
  
  REJECTED("REJECTED"),
  
  CANCELLED("CANCELLED"),
  
  EXPIRED("EXPIRED");

  private String value;

  ShiftChangeRequestStatus(String value) {
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
  public static ShiftChangeRequestStatus fromValue(String value) {
    for (ShiftChangeRequestStatus b : ShiftChangeRequestStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

