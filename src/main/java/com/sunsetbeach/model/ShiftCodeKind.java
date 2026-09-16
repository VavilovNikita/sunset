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
 * What a shift code actually is, named rather than inferred from `countsAsWorked`/`isPaid`/ interval shape every time something needs to know (the roster grid's own daily totals used to match `code == \"PH\"` literally to count absences - fragile, since codes are versioned and the text can be retyped, and a silently-zero count reads exactly like an ordinary day rather than a broken counter). `MORNING`/`EVENING` both mean an ordinary single-interval shift - the distinction is which half of the day it falls in, not a difference in how either renders (the roster grid's own colour choice is a continuous scale by actual start time, not this field - see the frontend's own design notes). `SPLIT` is a two-interval shift (`startTime2`/`endTime2` set). `OPEN_SCHEDULE` is `OP` - `countsAsWorked` true, no fixed interval. `ABSENCE` is `PH` and anything like it - `countsAsWorked` false, no fixed interval; not further split into holiday/annual-leave/kept-day-off, matching how `PH` itself has always stayed one code for exactly that reason. Chosen by a person at creation (see `ShiftCodeCreateInput`) - not inferred, though `ShiftCode.suggestedKind` offers a default guessed from the code's own shape for confirming one that predates this field. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum ShiftCodeKind {
  
  MORNING("MORNING"),
  
  SPLIT("SPLIT"),
  
  EVENING("EVENING"),
  
  OPEN_SCHEDULE("OPEN_SCHEDULE"),
  
  ABSENCE("ABSENCE");

  private String value;

  ShiftCodeKind(String value) {
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
  public static ShiftCodeKind fromValue(String value) {
    for (ShiftCodeKind b : ShiftCodeKind.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

