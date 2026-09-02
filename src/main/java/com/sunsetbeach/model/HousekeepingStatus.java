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
 * Cleaning state of a physical room, independent of RoomUnitBlock (which pulls a unit off sale entirely, for a reason staff write out - maintenance, renovation). A DIRTY room is still sellable/assignable; `POST /bookings/{id}/check-in` warns but does not block when the room being checked into is dirty. `POST /bookings/{id}/check-out` always sets the checked-out booking's room DIRTY. Set explicitly via `PATCH /room-units/{id}/housekeeping`, CASHIER+. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum HousekeepingStatus {
  
  DIRTY("DIRTY"),
  
  CLEAN("CLEAN");

  private String value;

  HousekeepingStatus(String value) {
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
  public static HousekeepingStatus fromValue(String value) {
    for (HousekeepingStatus b : HousekeepingStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

