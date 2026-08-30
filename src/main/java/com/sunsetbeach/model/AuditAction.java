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
 * The fixed vocabulary of significant staff actions this system records. Deliberately not open-ended (a free-text action name) - a closed set is what makes \"filter by action type\" on `GET /audit-log` and a fixed dropdown in the admin UI possible at all. Adding a new kind of audited action means adding a value here (a plain `ALTER TYPE ... ADD VALUE` migration, same pattern already used for `Role`/`PrintDocumentType`), not widening this into a string. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-30T12:20:31.062819400+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public enum AuditAction {
  
  BOOKING_CREATED("BOOKING_CREATED"),
  
  BOOKING_STATUS_CHANGED("BOOKING_STATUS_CHANGED"),
  
  BOOKING_PAYMENT_NOTE_CHANGED("BOOKING_PAYMENT_NOTE_CHANGED"),
  
  BOOKING_SCHEDULE_CHANGED("BOOKING_SCHEDULE_CHANGED"),
  
  BOOKING_ROOM_ASSIGNED("BOOKING_ROOM_ASSIGNED"),
  
  BOOKINGS_EXPORTED("BOOKINGS_EXPORTED"),
  
  ROOM_PRICE_CHANGED("ROOM_PRICE_CHANGED"),
  
  RATE_OVERRIDE_CHANGED("RATE_OVERRIDE_CHANGED"),
  
  ORDER_CLOSED("ORDER_CLOSED"),
  
  ORDER_CANCELLED("ORDER_CANCELLED"),
  
  ROOM_CHARGE_POSTED("ROOM_CHARGE_POSTED"),
  
  SHIFT_OPENED("SHIFT_OPENED"),
  
  SHIFT_CLOSED("SHIFT_CLOSED"),
  
  SHIFT_EXPORTED("SHIFT_EXPORTED"),
  
  USER_CREATED("USER_CREATED"),
  
  USER_ROLE_CHANGED("USER_ROLE_CHANGED"),
  
  USER_ACTIVE_CHANGED("USER_ACTIVE_CHANGED"),
  
  USER_PASSWORD_RESET("USER_PASSWORD_RESET"),
  
  ROOM_UNIT_CREATED("ROOM_UNIT_CREATED"),
  
  ROOM_UNIT_UPDATED("ROOM_UNIT_UPDATED"),
  
  ROOM_UNIT_DELETED("ROOM_UNIT_DELETED"),
  
  ROOM_UNIT_BLOCK_CREATED("ROOM_UNIT_BLOCK_CREATED"),
  
  ROOM_UNIT_BLOCK_DELETED("ROOM_UNIT_BLOCK_DELETED");

  private String value;

  AuditAction(String value) {
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
  public static AuditAction fromValue(String value) {
    for (AuditAction b : AuditAction.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

