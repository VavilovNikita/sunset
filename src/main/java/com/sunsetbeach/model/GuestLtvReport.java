package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.GuestLtvRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * &#x60;GET /reports/guest-ltv&#x60; - see that operation for what counts.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestLtvReport {

  @Valid
  private List<@Valid GuestLtvRow> guests = new ArrayList<>();

  public GuestLtvReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestLtvReport(List<@Valid GuestLtvRow> guests) {
    this.guests = guests;
  }

  public GuestLtvReport guests(List<@Valid GuestLtvRow> guests) {
    this.guests = guests;
    return this;
  }

  public GuestLtvReport addGuestsItem(GuestLtvRow guestsItem) {
    if (this.guests == null) {
      this.guests = new ArrayList<>();
    }
    this.guests.add(guestsItem);
    return this;
  }

  /**
   * Get guests
   * @return guests
   */
  @NotNull @Valid 
  @JsonProperty("guests")
  public List<@Valid GuestLtvRow> getGuests() {
    return guests;
  }

  public void setGuests(List<@Valid GuestLtvRow> guests) {
    this.guests = guests;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestLtvReport guestLtvReport = (GuestLtvReport) o;
    return Objects.equals(this.guests, guestLtvReport.guests);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guests);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestLtvReport {\n");
    sb.append("    guests: ").append(toIndentedString(guests)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

