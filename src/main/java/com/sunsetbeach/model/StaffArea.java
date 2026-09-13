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
 * Which part of the hotel an employee normally works in - Admin, Front Office, Maintenance, Housekeeping, Restaurant, Kitchen, matching the source spreadsheet's own grouping exactly. Deliberately not named `Department`: `PrinterDepartment` and `MenuDepartment` already exist in this API and mean something unrelated (print/ticket routing) - reusing the word here would be the same naming collision CLAUDE.md's Naming section already warns against for \"Shift\". 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum StaffArea {
  
  ADMIN("ADMIN"),
  
  FRONT_OFFICE("FRONT_OFFICE"),
  
  MAINTENANCE("MAINTENANCE"),
  
  HOUSEKEEPING("HOUSEKEEPING"),
  
  RESTAURANT("RESTAURANT"),
  
  KITCHEN("KITCHEN");

  private String value;

  StaffArea(String value) {
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
  public static StaffArea fromValue(String value) {
    for (StaffArea b : StaffArea.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

