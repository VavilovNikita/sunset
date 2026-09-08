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
 * A second, independent authorization axis alongside `Role` — a sideways job, not a step on the ADMIN > MANAGER > CASHIER > WAITER ladder. A function grants access to specific endpoints on its own, without granting anything else and without being expressed as a role comparison; it never appears in the RoleHierarchy bean. `User.functions` is a set (zero or more, no duplicates) — see that schema. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum JobFunction {
  
  ENGINEER("ENGINEER"),
  
  HOUSEKEEPER("HOUSEKEEPER");

  private String value;

  JobFunction(String value) {
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
  public static JobFunction fromValue(String value) {
    for (JobFunction b : JobFunction.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

