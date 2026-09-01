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
 * Hierarchical: ADMIN > MANAGER > CASHIER > WAITER (see RoleHierarchy bean). A role check written as e.g. \"CASHIER\" is satisfied by CASHIER, MANAGER, or ADMIN. `/users/_**` is the one exception — hard-restricted to ADMIN regardless of hierarchy. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum Role {
  
  ADMIN("ADMIN"),
  
  MANAGER("MANAGER"),
  
  CASHIER("CASHIER"),
  
  WAITER("WAITER");

  private String value;

  Role(String value) {
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
  public static Role fromValue(String value) {
    for (Role b : Role.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

