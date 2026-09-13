package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;POST /spa-appointments/{id}/swap-table&#x60;. The dragged appointment is named by the URL (&#x60;{id}&#x60;); this names the other side - same \&quot;the gesture has a direction\&quot; reasoning as &#x60;SwapSegmentRoomUnitInput&#x60;. Only the table changes for either side - &#x60;date&#x60;, &#x60;startTime&#x60;, &#x60;therapistUserId&#x60;, and &#x60;durationMinutes&#x60; all stay exactly as they were, which is what makes this well-defined regardless of how long either treatment runs: a 30-minute treatment and a 90-minute one trade tables exactly the same way two 90-minute ones would, because nothing about \&quot;how long\&quot; or \&quot;when\&quot; ever moves. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SwapSpaAppointmentTableInput {

  private String otherAppointmentId;

  public SwapSpaAppointmentTableInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SwapSpaAppointmentTableInput(String otherAppointmentId) {
    this.otherAppointmentId = otherAppointmentId;
  }

  public SwapSpaAppointmentTableInput otherAppointmentId(String otherAppointmentId) {
    this.otherAppointmentId = otherAppointmentId;
    return this;
  }

  /**
   * The other appointment to trade tables with. Must currently be BOOKED, same as the URL's own appointment.
   * @return otherAppointmentId
   */
  @NotNull 
  @JsonProperty("otherAppointmentId")
  public String getOtherAppointmentId() {
    return otherAppointmentId;
  }

  public void setOtherAppointmentId(String otherAppointmentId) {
    this.otherAppointmentId = otherAppointmentId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SwapSpaAppointmentTableInput swapSpaAppointmentTableInput = (SwapSpaAppointmentTableInput) o;
    return Objects.equals(this.otherAppointmentId, swapSpaAppointmentTableInput.otherAppointmentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(otherAppointmentId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SwapSpaAppointmentTableInput {\n");
    sb.append("    otherAppointmentId: ").append(toIndentedString(otherAppointmentId)).append("\n");
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

