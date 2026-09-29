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
 * What an import did. `UNCHANGED` - already up to date, nothing written. `SKIPPED` - nothing written on purpose (a stale update, a modification or cancellation of a reservation never imported, or a booking already cancelled in this system); `message` says which. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum SiteMinderImportAction {
  
  CREATED("CREATED"),
  
  UPDATED("UPDATED"),
  
  CANCELLED("CANCELLED"),
  
  UNCHANGED("UNCHANGED"),
  
  SKIPPED("SKIPPED");

  private String value;

  SiteMinderImportAction(String value) {
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
  public static SiteMinderImportAction fromValue(String value) {
    for (SiteMinderImportAction b : SiteMinderImportAction.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

