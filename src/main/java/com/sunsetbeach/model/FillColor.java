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
 * The one place a cell's own fill colour is read in the Excel schedule import, rather than just its text - the source spreadsheet writes two different shifts both as the bare text \"9\", told apart only by whether the cell is filled yellow (a single 09:00-18:00 shift) or light blue (a split one). See `RosterImportShiftColorMapping`'s own description for how a colour actually resolves to a `ShiftCode`. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum FillColor {
  
  YELLOW("YELLOW"),
  
  BLUE("BLUE");

  private String value;

  FillColor(String value) {
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
  public static FillColor fromValue(String value) {
    for (FillColor b : FillColor.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

