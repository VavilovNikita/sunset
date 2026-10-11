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
 * The printer's own built-in font for body text - A is the larger, B the smaller and denser. A thermal printer has no arbitrary fonts: these two, magnified 1x or 2x and optionally bold, are everything ESC/POS offers. (Thai text uses the printer's Thai code page, in whichever of the two the printer renders it.) 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum PrintBodyFont {
  
  A("A"),
  
  B("B");

  private String value;

  PrintBodyFont(String value) {
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
  public static PrintBodyFont fromValue(String value) {
    for (PrintBodyFont b : PrintBodyFont.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

