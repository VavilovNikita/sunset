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
 * Where one employee stands against their own shift, right now - `GET /attendance/today`. Computed live from today's `ShiftCode` interval(s) plus the same punch pairing `GET /attendance/summary` and the roster actuals export already use (`AttendancePunchPairing`) - never precomputed or cached, since every value here is only true as of this instant.  `OPEN_SCHEDULE` (`OP`) has no fixed interval, so it only ever takes three of these values - `NOT_YET_ARRIVED`, `ON_SHIFT`, `FINISHED` - the rest apply only to a fixed-interval code. `NOT_YET_ARRIVED`: no punches yet today. `ON_SHIFT`: a trailing unmatched `IN` punch - currently clocked in, for either shape. `FINISHED`: every applicable interval satisfied by a complete pair (or, for `OP`, at least one complete pair), no trailing unmatched `IN`. `SCHEDULED`: more than the configured upcoming-window away from the current interval's own start, not yet arrived. `ARRIVING_SOON`: inside that window, not yet arrived. `LATE`: at or past the interval's own start but still at or before its own end, not yet arrived - could still show up. `MISSED`: past the interval's own end with zero punches matched to it - the same fact the actuals export's \"Late & anomalies\" sheet already calls `MISSED`, just observed live instead of at month-end. `BETWEEN_SHIFTS`: a split shift only - interval 1 already complete, well before interval 2's own upcoming-window opens; kept distinct from `SCHEDULED` so someone who has already shown up once today never reads as indistinguishable from someone who hasn't shown up at all. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum TodayShiftState {
  
  NOT_YET_ARRIVED("NOT_YET_ARRIVED"),
  
  ON_SHIFT("ON_SHIFT"),
  
  FINISHED("FINISHED"),
  
  SCHEDULED("SCHEDULED"),
  
  ARRIVING_SOON("ARRIVING_SOON"),
  
  LATE("LATE"),
  
  BETWEEN_SHIFTS("BETWEEN_SHIFTS"),
  
  MISSED("MISSED");

  private String value;

  TodayShiftState(String value) {
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
  public static TodayShiftState fromValue(String value) {
    for (TodayShiftState b : TodayShiftState.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

