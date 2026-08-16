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
 * The single-byte ESC/POS code page text is encoded into before sending, since receipts can contain non-ASCII (guest names, menu item names) and Latin/Thai need different tables. `PC437` (the standard US/Latin ESC/POS code page) is the default; `TIS620` covers Thai. Not a hardcoded global setting - each Printer picks its own, since a hotel can have Latin-only and Thai-capable printers side by side. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-16T20:25:37.858402100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public enum PrinterCodepage {
  
  PC437("PC437"),
  
  TIS620("TIS620");

  private String value;

  PrinterCodepage(String value) {
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
  public static PrinterCodepage fromValue(String value) {
    for (PrinterCodepage b : PrinterCodepage.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

