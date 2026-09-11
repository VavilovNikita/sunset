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
 * One treatment on a &#x60;SpaAppointment&#x60; - see that schema&#39;s own description for why this is a child row and not a quantity. &#x60;durationMinutes&#x60; is frozen at add time from &#x60;MenuItem.durationMinutes&#x60;, same \&quot;agreed terms are frozen\&quot; precedent the appointment itself already uses for its own maintained sum. &#x60;currentPrice&#x60; is deliberately not frozen - it&#39;s a live read of &#x60;MenuItem.price&#x60; at response time, the same \&quot;denormalized, not frozen\&quot; convention &#x60;treatmentName&#x60; already uses, shown so reception can see what a treatment currently costs. It is never authoritative for billing and never sent to any write endpoint - the server always prices the actual charge live off the menu when a treatment is added to an order, the ordinary &#x60;POST /orders/{id}/items&#x60; path every other department already uses. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaAppointmentTreatment {

  private String id;

  private String treatmentMenuItemId;

  private String treatmentName;

  private Integer durationMinutes;

  private String currentPrice;

  public SpaAppointmentTreatment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaAppointmentTreatment(String id, String treatmentMenuItemId, String treatmentName, Integer durationMinutes, String currentPrice) {
    this.id = id;
    this.treatmentMenuItemId = treatmentMenuItemId;
    this.treatmentName = treatmentName;
    this.durationMinutes = durationMinutes;
    this.currentPrice = currentPrice;
  }

  public SpaAppointmentTreatment id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SpaAppointmentTreatment treatmentMenuItemId(String treatmentMenuItemId) {
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

  public SpaAppointmentTreatment treatmentName(String treatmentName) {
    this.treatmentName = treatmentName;
    return this;
  }

  /**
   * Denormalized `MenuItem.name` at read time - the treatment's current name, not frozen (unlike `durationMinutes`).
   * @return treatmentName
   */
  @NotNull 
  @JsonProperty("treatmentName")
  public String getTreatmentName() {
    return treatmentName;
  }

  public void setTreatmentName(String treatmentName) {
    this.treatmentName = treatmentName;
  }

  public SpaAppointmentTreatment durationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Frozen at add time from `MenuItem.durationMinutes`.
   * @return durationMinutes
   */
  @NotNull 
  @JsonProperty("durationMinutes")
  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public SpaAppointmentTreatment currentPrice(String currentPrice) {
    this.currentPrice = currentPrice;
    return this;
  }

  /**
   * Live `MenuItem.price` at response time - see the class description for why this isn't frozen and isn't billing-authoritative.
   * @return currentPrice
   */
  @NotNull 
  @JsonProperty("currentPrice")
  public String getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(String currentPrice) {
    this.currentPrice = currentPrice;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaAppointmentTreatment spaAppointmentTreatment = (SpaAppointmentTreatment) o;
    return Objects.equals(this.id, spaAppointmentTreatment.id) &&
        Objects.equals(this.treatmentMenuItemId, spaAppointmentTreatment.treatmentMenuItemId) &&
        Objects.equals(this.treatmentName, spaAppointmentTreatment.treatmentName) &&
        Objects.equals(this.durationMinutes, spaAppointmentTreatment.durationMinutes) &&
        Objects.equals(this.currentPrice, spaAppointmentTreatment.currentPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, treatmentMenuItemId, treatmentName, durationMinutes, currentPrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaAppointmentTreatment {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    treatmentMenuItemId: ").append(toIndentedString(treatmentMenuItemId)).append("\n");
    sb.append("    treatmentName: ").append(toIndentedString(treatmentName)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    currentPrice: ").append(toIndentedString(currentPrice)).append("\n");
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

