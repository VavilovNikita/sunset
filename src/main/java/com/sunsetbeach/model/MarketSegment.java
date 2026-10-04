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
 * `COM` complimentary, `DIR` direct (`DIRECT`/`PHONE`), `HFO` house use, `OTA` online travel agents (including a SiteMinder-imported `OTHER`), `OTH` other / not recorded (a staff-entered `OTHER` booking), `WLK` walk-in. Derived from a booking's `purpose` and `channel`, never stored - see `GET /reports/market-segment`. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum MarketSegment {
  
  COM("COM"),
  
  DIR("DIR"),
  
  HFO("HFO"),
  
  OTA("OTA"),
  
  OTH("OTH"),
  
  WLK("WLK");

  private String value;

  MarketSegment(String value) {
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
  public static MarketSegment fromValue(String value) {
    for (MarketSegment b : MarketSegment.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

