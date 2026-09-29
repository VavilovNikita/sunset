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
 * What posted an entry. `BOOKING` - a booking became `PAID`, or left it (the mirror); `sourceId` is the booking id. `POS_ORDER` - a POS order was closed; `sourceId` is the order id. `FOLIO_PAYMENT` - money collected against a booking's room charges; `sourceId` is the folio payment id. `MANUAL` - `POST /ledger/entries` or `POST /ledger/entries/{id}/reverse`; `sourceId` is null. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum JournalSourceType {
  
  BOOKING("BOOKING"),
  
  POS_ORDER("POS_ORDER"),
  
  FOLIO_PAYMENT("FOLIO_PAYMENT"),
  
  MANUAL("MANUAL");

  private String value;

  JournalSourceType(String value) {
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
  public static JournalSourceType fromValue(String value) {
    for (JournalSourceType b : JournalSourceType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

