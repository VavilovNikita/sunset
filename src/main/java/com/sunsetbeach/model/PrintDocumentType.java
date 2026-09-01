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
 * `KITCHEN_TICKET` and `BAR_TICKET` are the two station tickets an order send splits into by `MenuItem.department` - separate values (not one type disambiguated by `summary` text) so the queue can be filtered to a single station. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum PrintDocumentType {
  
  KITCHEN_TICKET("KITCHEN_TICKET"),
  
  BAR_TICKET("BAR_TICKET"),
  
  PREBILL("PREBILL"),
  
  GUEST_RECEIPT("GUEST_RECEIPT"),
  
  Z_REPORT("Z_REPORT"),
  
  TEST_PAGE("TEST_PAGE");

  private String value;

  PrintDocumentType(String value) {
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
  public static PrintDocumentType fromValue(String value) {
    for (PrintDocumentType b : PrintDocumentType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

