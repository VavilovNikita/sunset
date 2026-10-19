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
 * `LATE` - an employee has not clocked in `lateGraceMinutes` after the planned start of a shift interval (to them and to the late-recipients). `ABSENT` - still not there `absenceMinutes` after it (to the director recipients). `BLOCK_CREATED`, `BLOCK_LAST_DAY` (the morning of the block's last day) and `BLOCK_EXPIRED_OPEN` (every morning while the block's maintenance task is not DONE) go to whoever is on shift with the block's responsible function, otherwise to the director recipients. `TERMINAL_OFFLINE` / `TERMINAL_ONLINE` - an attendance terminal went silent / came back (to the director recipients). `SHIFT_REQUEST_NEW` - a shift request waits for a decision (to the deciders, with Approve / Reject buttons in Telegram); `SHIFT_REQUEST_DECIDED` / `SHIFT_REQUEST_EXPIRED` - the outcome (to the employee); `SHIFT_REQUEST_REMINDER` - a request has waited too long (to the deciders); `SHIFT_EVE_REMINDER` - tomorrow's shift, the evening before, to those who asked for it; `ROSTER_CHANGED` - a manager changed an employee's shift on a day (one message per employee and day). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum NotificationType {
  
  LATE("LATE"),
  
  ABSENT("ABSENT"),
  
  BLOCK_CREATED("BLOCK_CREATED"),
  
  BLOCK_LAST_DAY("BLOCK_LAST_DAY"),
  
  BLOCK_EXPIRED_OPEN("BLOCK_EXPIRED_OPEN"),
  
  TERMINAL_OFFLINE("TERMINAL_OFFLINE"),
  
  TERMINAL_ONLINE("TERMINAL_ONLINE"),
  
  SHIFT_REQUEST_NEW("SHIFT_REQUEST_NEW"),
  
  SHIFT_REQUEST_DECIDED("SHIFT_REQUEST_DECIDED"),
  
  SHIFT_REQUEST_EXPIRED("SHIFT_REQUEST_EXPIRED"),
  
  SHIFT_REQUEST_REMINDER("SHIFT_REQUEST_REMINDER"),
  
  SHIFT_EVE_REMINDER("SHIFT_EVE_REMINDER"),
  
  ROSTER_CHANGED("ROSTER_CHANGED");

  private String value;

  NotificationType(String value) {
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
  public static NotificationType fromValue(String value) {
    for (NotificationType b : NotificationType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

