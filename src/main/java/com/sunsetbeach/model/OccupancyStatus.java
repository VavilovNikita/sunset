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
 * Whether the guest is physically at the hotel - deliberately separate from `BookingStatus`, which stays commercial only (confirmed/paid/cancelled). One value per booking, not per segment: a guest checks in once and checks out once regardless of how many times they're relocated to a different room mid-stay. Never affects availability - the engine behind `GET /availability/{roomId}` and the booking calendar reads `BookingSegment`/`Booking.status` only, never this field. `NO_SHOW` is a label, not an action: it changes nothing about the booking's dates or `status`, and does not release any nights - the deliberate way to actually free them is cancelling or shortening the booking, a separate step. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum OccupancyStatus {
  
  EXPECTED("EXPECTED"),
  
  CHECKED_IN("CHECKED_IN"),
  
  CHECKED_OUT("CHECKED_OUT"),
  
  NO_SHOW("NO_SHOW");

  private String value;

  OccupancyStatus(String value) {
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
  public static OccupancyStatus fromValue(String value) {
    for (OccupancyStatus b : OccupancyStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

