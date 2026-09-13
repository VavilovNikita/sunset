package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.SpaAppointmentStatus;
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
 * A summary of one of today&#39;s appointments on a &#x60;SpaMapTable&#x60;, for the map&#39;s own detail panel - every status, not just &#x60;BOOKED&#x60;/&#x60;COMPLETED&#x60; (a cancelled/no-show slot is still part of the day&#39;s own story, same \&quot;still needs to render\&quot; reasoning &#x60;SpaSchedule.appointments&#x60; already documents). Not a substitute for &#x60;SpaAppointment&#x60; - no id-level editing lives here, only enough to list and link out to the real schedule. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaMapTableAppointment {

  private String id;

  private String startTime;

  private Integer durationMinutes;

  private SpaAppointmentStatus status;

  private String guestName;

  @Valid
  private List<String> treatmentNames = new ArrayList<>();

  public SpaMapTableAppointment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaMapTableAppointment(String id, String startTime, Integer durationMinutes, SpaAppointmentStatus status, String guestName, List<String> treatmentNames) {
    this.id = id;
    this.startTime = startTime;
    this.durationMinutes = durationMinutes;
    this.status = status;
    this.guestName = guestName;
    this.treatmentNames = treatmentNames;
  }

  public SpaMapTableAppointment id(String id) {
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

  public SpaMapTableAppointment startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Get startTime
   * @return startTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("startTime")
  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public SpaMapTableAppointment durationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Get durationMinutes
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

  public SpaMapTableAppointment status(SpaAppointmentStatus status) {
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

  public SpaMapTableAppointment guestName(String guestName) {
    this.guestName = guestName;
    return this;
  }

  /**
   * Get guestName
   * @return guestName
   */
  @NotNull 
  @JsonProperty("guestName")
  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }

  public SpaMapTableAppointment treatmentNames(List<String> treatmentNames) {
    this.treatmentNames = treatmentNames;
    return this;
  }

  public SpaMapTableAppointment addTreatmentNamesItem(String treatmentNamesItem) {
    if (this.treatmentNames == null) {
      this.treatmentNames = new ArrayList<>();
    }
    this.treatmentNames.add(treatmentNamesItem);
    return this;
  }

  /**
   * Get treatmentNames
   * @return treatmentNames
   */
  @NotNull 
  @JsonProperty("treatmentNames")
  public List<String> getTreatmentNames() {
    return treatmentNames;
  }

  public void setTreatmentNames(List<String> treatmentNames) {
    this.treatmentNames = treatmentNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaMapTableAppointment spaMapTableAppointment = (SpaMapTableAppointment) o;
    return Objects.equals(this.id, spaMapTableAppointment.id) &&
        Objects.equals(this.startTime, spaMapTableAppointment.startTime) &&
        Objects.equals(this.durationMinutes, spaMapTableAppointment.durationMinutes) &&
        Objects.equals(this.status, spaMapTableAppointment.status) &&
        Objects.equals(this.guestName, spaMapTableAppointment.guestName) &&
        Objects.equals(this.treatmentNames, spaMapTableAppointment.treatmentNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, startTime, durationMinutes, status, guestName, treatmentNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaMapTableAppointment {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    treatmentNames: ").append(toIndentedString(treatmentNames)).append("\n");
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

