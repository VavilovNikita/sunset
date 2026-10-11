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
 * The documents whose look is configurable. `RECEIPT` is the guest receipt (and its reprint, titled with \" - COPY\" appended), `PREBILL` the pre-bill, `KITCHEN_TICKET` and `BAR_TICKET` the order tickets (their void tickets use the same layout with the fixed title \"*** VOID ***\"), `Z_REPORT` the shift-close report. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum PrintLayoutDocument {
  
  RECEIPT("RECEIPT"),
  
  PREBILL("PREBILL"),
  
  KITCHEN_TICKET("KITCHEN_TICKET"),
  
  BAR_TICKET("BAR_TICKET"),
  
  Z_REPORT("Z_REPORT");

  private String value;

  PrintLayoutDocument(String value) {
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
  public static PrintLayoutDocument fromValue(String value) {
    for (PrintLayoutDocument b : PrintLayoutDocument.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

