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
 * How a booking reached the hotel - a label staff record from what they already know (the guest booked by phone, walked in, said they found the hotel on Airbnb). Not an OTA integration: nothing is synced with any channel. `POST /bookings` (the public form) is always `DIRECT`, set server-side. Unrelated to the internal public-form-vs-front-desk distinction that drives auto-expiry of unconfirmed public inquiries, which is never exposed and never changes when this does. Bookings made before this field existed read `DIRECT` if they came through the public form and `OTHER` if staff entered them (their real channel was never recorded). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum BookingChannel {
  
  DIRECT("DIRECT"),
  
  PHONE("PHONE"),
  
  WALK_IN("WALK_IN"),
  
  BOOKING_COM("BOOKING_COM"),
  
  AIRBNB("AIRBNB"),
  
  AGODA("AGODA"),
  
  OTHER("OTHER");

  private String value;

  BookingChannel(String value) {
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
  public static BookingChannel fromValue(String value) {
    for (BookingChannel b : BookingChannel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

