package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.SpaAppointment;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;POST /spa-appointments&#x60;. &#x60;warning&#x60; is set (creation still succeeds) when &#x60;date&#x60; falls outside the named booking&#39;s stay - see &#x60;SpaAppointmentCreateInput&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaAppointmentResult {

  private SpaAppointment appointment;

  private JsonNullable<String> warning = JsonNullable.<String>undefined();

  public SpaAppointmentResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaAppointmentResult(SpaAppointment appointment, String warning) {
    this.appointment = appointment;
    this.warning = JsonNullable.of(warning);
  }

  public SpaAppointmentResult appointment(SpaAppointment appointment) {
    this.appointment = appointment;
    return this;
  }

  /**
   * Get appointment
   * @return appointment
   */
  @NotNull @Valid 
  @JsonProperty("appointment")
  public SpaAppointment getAppointment() {
    return appointment;
  }

  public void setAppointment(SpaAppointment appointment) {
    this.appointment = appointment;
  }

  public SpaAppointmentResult warning(String warning) {
    this.warning = JsonNullable.of(warning);
    return this;
  }

  /**
   * Get warning
   * @return warning
   */
  @NotNull 
  @JsonProperty("warning")
  public JsonNullable<String> getWarning() {
    return warning;
  }

  public void setWarning(JsonNullable<String> warning) {
    this.warning = warning;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaAppointmentResult spaAppointmentResult = (SpaAppointmentResult) o;
    return Objects.equals(this.appointment, spaAppointmentResult.appointment) &&
        Objects.equals(this.warning, spaAppointmentResult.warning);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appointment, warning);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaAppointmentResult {\n");
    sb.append("    appointment: ").append(toIndentedString(appointment)).append("\n");
    sb.append("    warning: ").append(toIndentedString(warning)).append("\n");
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

