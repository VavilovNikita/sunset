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
 * `PRE_ARRIVAL` - a few days before check-in. `POST_STAY` - a few days after checkout, with a review link when one is configured. `WIN_BACK` - a guest who hasn't stayed in a long while. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum LifecycleEmailType {
  
  PRE_ARRIVAL("PRE_ARRIVAL"),
  
  POST_STAY("POST_STAY"),
  
  WIN_BACK("WIN_BACK");

  private String value;

  LifecycleEmailType(String value) {
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
  public static LifecycleEmailType fromValue(String value) {
    for (LifecycleEmailType b : LifecycleEmailType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

