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
 * Sort column for `GET /bookings/search`. `ROOM` sorts by the room type's name. Defaults to `CHECK_IN`.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum BookingSortField {
  
  GUEST_NAME("GUEST_NAME"),
  
  ROOM("ROOM"),
  
  CHECK_IN("CHECK_IN"),
  
  CHECK_OUT("CHECK_OUT"),
  
  TOTAL_PRICE("TOTAL_PRICE"),
  
  STATUS("STATUS"),
  
  CREATED_AT("CREATED_AT");

  private String value;

  BookingSortField(String value) {
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
  public static BookingSortField fromValue(String value) {
    for (BookingSortField b : BookingSortField.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

