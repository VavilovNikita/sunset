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
 * Where the booking's money stands, computed from the folio's own `balanceDue`/`creditBalance` (`FolioBreakdown#settlement` - the one function), never stored: `OWES` (`balanceDue` > 0), `CREDIT` (collected more than was charged, `creditBalance` > 0) or `SETTLED` (neither). It exists because `BookingStatus.PAID` only says the room was paid - a `PAID` booking whose guest stayed on, or ran up POS room charges, can still owe money. Read this, not `status`, for \"is anything left to settle\". 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum SettlementStatus {
  
  SETTLED("SETTLED"),
  
  OWES("OWES"),
  
  CREDIT("CREDIT");

  private String value;

  SettlementStatus(String value) {
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
  public static SettlementStatus fromValue(String value) {
    for (SettlementStatus b : SettlementStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

