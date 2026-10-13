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
 * `BOOKED` and `COMPLETED` hold the slot (the exclusion constraint is scoped to them); `CANCELLED` frees it. A `BOOKED` reservation turns `COMPLETED` by itself once its end has passed (the automation's sweep). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum GymReservationStatus {
  
  BOOKED("BOOKED"),
  
  COMPLETED("COMPLETED"),
  
  CANCELLED("CANCELLED");

  private String value;

  GymReservationStatus(String value) {
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
  public static GymReservationStatus fromValue(String value) {
    for (GymReservationStatus b : GymReservationStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

