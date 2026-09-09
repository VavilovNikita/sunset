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
 * Forward-only from `BOOKED` in practice: `PATCH /spa-appointments/{id}/status` accepts `COMPLETED`/`CANCELLED`/`NO_SHOW` as targets (never back to `BOOKED`, never between the three end states). Only a `BOOKED` appointment holds a slot - see `SpaAppointment`'s own description of the exclusion constraints, which are scoped `WHERE status = 'BOOKED'`, so a cancelled/no-show/completed appointment never blocks a new booking of the same table/therapist/time. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum SpaAppointmentStatus {
  
  BOOKED("BOOKED"),
  
  COMPLETED("COMPLETED"),
  
  CANCELLED("CANCELLED"),
  
  NO_SHOW("NO_SHOW");

  private String value;

  SpaAppointmentStatus(String value) {
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
  public static SpaAppointmentStatus fromValue(String value) {
    for (SpaAppointmentStatus b : SpaAppointmentStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

