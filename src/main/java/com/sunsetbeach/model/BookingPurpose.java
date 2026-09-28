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
 * Why the room is occupied: an ordinary paying stay (`STANDARD`), a complimentary stay (`COMPLIMENTARY`), or internal house use (`HOUSE_USE`). A separate question from `BookingChannel` (how the booking reached the hotel) - a comp booked by phone is `COMPLIMENTARY` + `PHONE`. Either way it is still a booking that occupies a room and counts as room-nights sold; a room taken out of sale is a `RoomUnitBlock` instead. Nothing forces `totalPrice` to zero for a comp or house-use stay - it is priced like any other booking. `POST /bookings` (the public form) is always `STANDARD`. Bookings made before this field existed read `STANDARD`. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public enum BookingPurpose {
  
  STANDARD("STANDARD"),
  
  COMPLIMENTARY("COMPLIMENTARY"),
  
  HOUSE_USE("HOUSE_USE");

  private String value;

  BookingPurpose(String value) {
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
  public static BookingPurpose fromValue(String value) {
    for (BookingPurpose b : BookingPurpose.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

