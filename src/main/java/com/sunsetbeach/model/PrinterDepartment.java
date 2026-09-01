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
 * What a Printer receives. `KITCHEN`/`BAR` get routed kitchen/bar tickets (split from order line items by `MenuItem.department`); `CASHIER` gets pre-bills, guest receipts, and shift Z-reports - nothing routed by menu department ever goes there. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum PrinterDepartment {
  
  KITCHEN("KITCHEN"),
  
  BAR("BAR"),
  
  CASHIER("CASHIER");

  private String value;

  PrinterDepartment(String value) {
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
  public static PrinterDepartment fromValue(String value) {
    for (PrinterDepartment b : PrinterDepartment.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

