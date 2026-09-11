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
 * Where a MenuItem's kitchen/bar ticket prints - independent of `category`, which is a free-text display grouping for the menu itself (e.g. \"Mains\", \"Cocktails\") and has no effect on print routing. `SPA` is not a real ticket-printer department: a treatment item never generates a kitchen/bar ticket at all (see `OrderPrintingService`) - the guest's receipt still prints normally, since that's a separate print path keyed off the order, not this field. `SpaAppointmentTreatment.treatmentMenuItemId` must reference a `SPA`- department item with `durationMinutes` set; that's what makes an item schedulable. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum MenuDepartment {
  
  KITCHEN("KITCHEN"),
  
  BAR("BAR"),
  
  SPA("SPA");

  private String value;

  MenuDepartment(String value) {
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
  public static MenuDepartment fromValue(String value) {
    for (MenuDepartment b : MenuDepartment.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

