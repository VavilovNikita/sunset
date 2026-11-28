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
 * Why the earlier guest is still in the room, the first that applies: `PAST_CHECK_OUT` (checked in after `checkOut`), `AWAITING_ROOM` (their own move took effect with no new room picked, see `PendingRoomMove`), `DUE_OUT_TODAY` (leaving today, not checked out yet). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum RoomStillHeldState {
  
  PAST_CHECK_OUT("PAST_CHECK_OUT"),
  
  AWAITING_ROOM("AWAITING_ROOM"),
  
  DUE_OUT_TODAY("DUE_OUT_TODAY");

  private String value;

  RoomStillHeldState(String value) {
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
  public static RoomStillHeldState fromValue(String value) {
    for (RoomStillHeldState b : RoomStillHeldState.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

