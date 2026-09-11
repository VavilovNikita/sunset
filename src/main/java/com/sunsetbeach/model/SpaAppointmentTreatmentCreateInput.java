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
 * Body of &#x60;POST /spa-appointments/{id}/treatments&#x60;. &#x60;treatmentMenuItemId&#x60; must reference a &#x60;SPA&#x60;-department item with &#x60;durationMinutes&#x60; set, same validation as &#x60;SpaAppointmentCreateInput&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaAppointmentTreatmentCreateInput {

  private String treatmentMenuItemId;

  public SpaAppointmentTreatmentCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaAppointmentTreatmentCreateInput(String treatmentMenuItemId) {
    this.treatmentMenuItemId = treatmentMenuItemId;
  }

  public SpaAppointmentTreatmentCreateInput treatmentMenuItemId(String treatmentMenuItemId) {
    this.treatmentMenuItemId = treatmentMenuItemId;
    return this;
  }

  /**
   * Get treatmentMenuItemId
   * @return treatmentMenuItemId
   */
  @NotNull 
  @JsonProperty("treatmentMenuItemId")
  public String getTreatmentMenuItemId() {
    return treatmentMenuItemId;
  }

  public void setTreatmentMenuItemId(String treatmentMenuItemId) {
    this.treatmentMenuItemId = treatmentMenuItemId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaAppointmentTreatmentCreateInput spaAppointmentTreatmentCreateInput = (SpaAppointmentTreatmentCreateInput) o;
    return Objects.equals(this.treatmentMenuItemId, spaAppointmentTreatmentCreateInput.treatmentMenuItemId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(treatmentMenuItemId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaAppointmentTreatmentCreateInput {\n");
    sb.append("    treatmentMenuItemId: ").append(toIndentedString(treatmentMenuItemId)).append("\n");
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

