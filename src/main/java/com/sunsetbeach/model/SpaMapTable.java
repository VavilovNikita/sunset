package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.SpaMapTableAppointment;
import java.math.BigDecimal;
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
 * One SPA-zone table on the map (&#x60;GET /spa-map&#x60;), enriched with today&#39;s occupancy - the same kind of enrichment &#x60;PropertyMapUnit&#x60; already does for a room unit, computed the same way: once, server-side, from data already fetched for another read (&#x60;SpaAppointmentService#getSchedule&#x60;, the day grid&#39;s own batched query), not a per-table query. &#x60;busy&#x60; and &#x60;nextAppointmentStartTime&#x60; are two sides of one axis - a table is occupied by a &#x60;BOOKED&#x60;/&#x60;COMPLETED&#x60; appointment covering this exact moment (the same occupying-statuses set the two &#x60;EXCLUDE USING gist&#x60; constraints use, see &#x60;V43__spa_appointment_completed_still_occupies_slot.sql&#x60;), or it&#39;s free, in which case &#x60;nextAppointmentStartTime&#x60; names when that stops being true (or stays &#x60;null&#x60; for the rest of the day). &#x60;isActive&#x60; is a third, independent fact, exactly like &#x60;PropertyMapUnit.isActive&#x60; - a deactivated table is still listed here, dimmed, never excluded, and its &#x60;busy&#x60;/&#x60;nextAppointmentStartTime&#x60;/&#x60;appointments&#x60; are still the real values (whatever inactive-but-still-technically-scheduled state that implies) rather than being zeroed out - which state wins visually is a frontend decision, not encoded here. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaMapTable {

  private String tableId;

  private String label;

  private Integer capacity;

  private Boolean isActive;

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionX = JsonNullable.<BigDecimal>undefined();

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionY = JsonNullable.<BigDecimal>undefined();

  private Boolean busy;

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> nextAppointmentStartTime = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid SpaMapTableAppointment> appointments = new ArrayList<>();

  public SpaMapTable() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaMapTable(String tableId, String label, Integer capacity, Boolean isActive, BigDecimal positionX, BigDecimal positionY, Boolean busy, String nextAppointmentStartTime, List<@Valid SpaMapTableAppointment> appointments) {
    this.tableId = tableId;
    this.label = label;
    this.capacity = capacity;
    this.isActive = isActive;
    this.positionX = JsonNullable.of(positionX);
    this.positionY = JsonNullable.of(positionY);
    this.busy = busy;
    this.nextAppointmentStartTime = JsonNullable.of(nextAppointmentStartTime);
    this.appointments = appointments;
  }

  public SpaMapTable tableId(String tableId) {
    this.tableId = tableId;
    return this;
  }

  /**
   * Get tableId
   * @return tableId
   */
  @NotNull 
  @JsonProperty("tableId")
  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public SpaMapTable label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  @NotNull 
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public SpaMapTable capacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * Get capacity
   * @return capacity
   */
  @NotNull 
  @JsonProperty("capacity")
  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public SpaMapTable isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Get isActive
   * @return isActive
   */
  @NotNull 
  @JsonProperty("isActive")
  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public SpaMapTable positionX(BigDecimal positionX) {
    this.positionX = JsonNullable.of(positionX);
    return this;
  }

  /**
   * Get positionX
   * minimum: 0
   * maximum: 1
   * @return positionX
   */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionX")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionX() {
    return positionX;
  }

  public void setPositionX(JsonNullable<BigDecimal> positionX) {
    this.positionX = positionX;
  }

  public SpaMapTable positionY(BigDecimal positionY) {
    this.positionY = JsonNullable.of(positionY);
    return this;
  }

  /**
   * Get positionY
   * minimum: 0
   * maximum: 1
   * @return positionY
   */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionY")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionY() {
    return positionY;
  }

  public void setPositionY(JsonNullable<BigDecimal> positionY) {
    this.positionY = positionY;
  }

  public SpaMapTable busy(Boolean busy) {
    this.busy = busy;
    return this;
  }

  /**
   * True when a BOOKED or COMPLETED appointment on this table covers this exact moment.
   * @return busy
   */
  @NotNull 
  @JsonProperty("busy")
  public Boolean getBusy() {
    return busy;
  }

  public void setBusy(Boolean busy) {
    this.busy = busy;
  }

  public SpaMapTable nextAppointmentStartTime(String nextAppointmentStartTime) {
    this.nextAppointmentStartTime = JsonNullable.of(nextAppointmentStartTime);
    return this;
  }

  /**
   * The next BOOKED appointment's startTime today, after now - null when nothing is left today, regardless of `busy`.
   * @return nextAppointmentStartTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("nextAppointmentStartTime")
  public JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> getNextAppointmentStartTime() {
    return nextAppointmentStartTime;
  }

  public void setNextAppointmentStartTime(JsonNullable<String> nextAppointmentStartTime) {
    this.nextAppointmentStartTime = nextAppointmentStartTime;
  }

  public SpaMapTable appointments(List<@Valid SpaMapTableAppointment> appointments) {
    this.appointments = appointments;
    return this;
  }

  public SpaMapTable addAppointmentsItem(SpaMapTableAppointment appointmentsItem) {
    if (this.appointments == null) {
      this.appointments = new ArrayList<>();
    }
    this.appointments.add(appointmentsItem);
    return this;
  }

  /**
   * Every one of today's appointments on this table, regardless of status.
   * @return appointments
   */
  @NotNull @Valid 
  @JsonProperty("appointments")
  public List<@Valid SpaMapTableAppointment> getAppointments() {
    return appointments;
  }

  public void setAppointments(List<@Valid SpaMapTableAppointment> appointments) {
    this.appointments = appointments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaMapTable spaMapTable = (SpaMapTable) o;
    return Objects.equals(this.tableId, spaMapTable.tableId) &&
        Objects.equals(this.label, spaMapTable.label) &&
        Objects.equals(this.capacity, spaMapTable.capacity) &&
        Objects.equals(this.isActive, spaMapTable.isActive) &&
        Objects.equals(this.positionX, spaMapTable.positionX) &&
        Objects.equals(this.positionY, spaMapTable.positionY) &&
        Objects.equals(this.busy, spaMapTable.busy) &&
        Objects.equals(this.nextAppointmentStartTime, spaMapTable.nextAppointmentStartTime) &&
        Objects.equals(this.appointments, spaMapTable.appointments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableId, label, capacity, isActive, positionX, positionY, busy, nextAppointmentStartTime, appointments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaMapTable {\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    positionX: ").append(toIndentedString(positionX)).append("\n");
    sb.append("    positionY: ").append(toIndentedString(positionY)).append("\n");
    sb.append("    busy: ").append(toIndentedString(busy)).append("\n");
    sb.append("    nextAppointmentStartTime: ").append(toIndentedString(nextAppointmentStartTime)).append("\n");
    sb.append("    appointments: ").append(toIndentedString(appointments)).append("\n");
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

