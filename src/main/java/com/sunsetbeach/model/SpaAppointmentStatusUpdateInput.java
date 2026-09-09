package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.SpaAppointmentStatus;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PATCH /spa-appointments/{id}/status&#x60;. Only &#x60;COMPLETED&#x60;, &#x60;CANCELLED&#x60;, &#x60;NO_SHOW&#x60; are legal targets - see &#x60;SpaAppointmentStatus&#x60;. &#x60;cancelReason&#x60; is only meaningful (and only read) when &#x60;status&#x60; is &#x60;CANCELLED&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaAppointmentStatusUpdateInput {

  private SpaAppointmentStatus status;

  private String cancelReason;

  public SpaAppointmentStatusUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaAppointmentStatusUpdateInput(SpaAppointmentStatus status) {
    this.status = status;
  }

  public SpaAppointmentStatusUpdateInput status(SpaAppointmentStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public SpaAppointmentStatus getStatus() {
    return status;
  }

  public void setStatus(SpaAppointmentStatus status) {
    this.status = status;
  }

  public SpaAppointmentStatusUpdateInput cancelReason(String cancelReason) {
    this.cancelReason = cancelReason;
    return this;
  }

  /**
   * Get cancelReason
   * @return cancelReason
   */
  @Size(max = 500) 
  @JsonProperty("cancelReason")
  public String getCancelReason() {
    return cancelReason;
  }

  public void setCancelReason(String cancelReason) {
    this.cancelReason = cancelReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaAppointmentStatusUpdateInput spaAppointmentStatusUpdateInput = (SpaAppointmentStatusUpdateInput) o;
    return Objects.equals(this.status, spaAppointmentStatusUpdateInput.status) &&
        Objects.equals(this.cancelReason, spaAppointmentStatusUpdateInput.cancelReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, cancelReason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaAppointmentStatusUpdateInput {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    cancelReason: ").append(toIndentedString(cancelReason)).append("\n");
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

