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
 * What the moved nights cost. `KEEP_RATE` - exactly the frozen per-night prices they already have (manual and SiteMinder prices included); `NEW_RATE` - the new room type's current rate, from today forward. Omitted means `NEW_RATE` across room types and `KEEP_RATE` within one (`NEW_RATE` within one type is a 400 - use `POST /bookings/{id}/reprice`). `KEEP_RATE` across room types needs MANAGER or above. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum RelocationRateMode {
  
  KEEP_RATE("KEEP_RATE"),
  
  NEW_RATE("NEW_RATE");

  private String value;

  RelocationRateMode(String value) {
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
  public static RelocationRateMode fromValue(String value) {
    for (RelocationRateMode b : RelocationRateMode.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

