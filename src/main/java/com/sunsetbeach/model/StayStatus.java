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
 * Where a booking stands as a stay, derived by the server from `status` and `occupancyStatus` (never stored): `CANCELLED` (booking cancelled, whatever else), `NO_SHOW`, `CHECKED_OUT`, `IN_HOUSE` (checked in, including an overdue guest) and `UPCOMING` (nobody has arrived yet - a `NEW`/`CONFIRMED`/`PAID` booking that has not started, or whose dates have passed without a check-in and that is not yet marked no-show or cancelled). Show this, not `status`, where the question is \"what happened to this stay\" (the guest card's stay history). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum StayStatus {
  
  UPCOMING("UPCOMING"),
  
  IN_HOUSE("IN_HOUSE"),
  
  CHECKED_OUT("CHECKED_OUT"),
  
  CANCELLED("CANCELLED"),
  
  NO_SHOW("NO_SHOW");

  private String value;

  StayStatus(String value) {
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
  public static StayStatus fromValue(String value) {
    for (StayStatus b : StayStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

