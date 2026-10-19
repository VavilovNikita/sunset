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
 * A second, independent authorization axis alongside `Role` — a sideways job, not a step on the ADMIN > MANAGER > CASHIER > WAITER ladder. A function grants access to specific endpoints on its own, without granting anything else and without being expressed as a role comparison; it never appears in the RoleHierarchy bean. `User.functions` is a set (zero or more, no duplicates) — see that schema. `THERAPIST` marks a staff account as a spa therapist for `SpaAppointment.therapistUserId` - an ordinary staff account like any other (the same account shape shifts/hours will use), not a separate person record; one whose holder never signs in costs nothing beyond the row itself. No endpoint is gated on this function in v1 (there is no therapist self-service screen yet - reception is the only surface), so it exists purely as a tag on who counts as a bookable therapist. `HR` marks the HR director, who decides employees' requests to move or remove a shift (`/shift-change-requests`) - in the staff Telegram bot or on the admin screen - and may *read* the month's roster and the attendance summary (`GET /roster`, `GET /roster/employees`, `GET /attendance/summary`) to decide on real data; every write to the roster stays MANAGER+. **At most one active user holds `HR`**: giving it to a second active user (or reactivating a deactivated holder while another holds it) is a 409 naming the current holder. While nobody holds it, or the holder is on leave (`PUT /shift-change-requests/hr-vacation`), the administrators decide, if `StaffBotSettings.fallbackToAdmin` is on. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum JobFunction {
  
  ENGINEER("ENGINEER"),
  
  HOUSEKEEPER("HOUSEKEEPER"),
  
  THERAPIST("THERAPIST"),
  
  HR("HR");

  private String value;

  JobFunction(String value) {
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
  public static JobFunction fromValue(String value) {
    for (JobFunction b : JobFunction.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

