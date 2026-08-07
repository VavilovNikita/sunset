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
 * CARD is a ledger entry recorded on the cashier's word, not a confirmation from a payment terminal/acquirer — there is no card-processing integration. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T15:38:53.433655500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public enum PaymentMethod {
  
  CASH("CASH"),
  
  CARD("CARD"),
  
  ROOM_CHARGE("ROOM_CHARGE"),
  
  OTHER("OTHER");

  private String value;

  PaymentMethod(String value) {
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
  public static PaymentMethod fromValue(String value) {
    for (PaymentMethod b : PaymentMethod.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

