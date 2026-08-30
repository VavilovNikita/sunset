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
 * Which kind of record `AuditLogEntry.entityId` refers to. Paired with `entityId` to look up one record's own history (e.g. a booking's). Values are SCREAMING_SNAKE_CASE, not the PascalCase entity class names, deliberately: Spring MVC's default `@RequestParam` enum conversion for a query parameter (unlike Jackson's JSON body deserialization) uses the enum constant's own `name()`, not a custom `@JsonValue` - a PascalCase value here would deserialize correctly in a JSON response body but fail to bind as a query parameter (`entityType=Booking` 400s while `entityType=BOOKING` works), which is exactly the asymmetry every other enum in this API avoids by keeping its JSON value identical to its Java constant name. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-30T12:57:43.105533+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public enum AuditEntityType {
  
  BOOKING("BOOKING"),
  
  ROOM("ROOM"),
  
  ORDER("ORDER"),
  
  SHIFT("SHIFT"),
  
  USER("USER"),
  
  ROOM_UNIT("ROOM_UNIT");

  private String value;

  AuditEntityType(String value) {
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
  public static AuditEntityType fromValue(String value) {
    for (AuditEntityType b : AuditEntityType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

