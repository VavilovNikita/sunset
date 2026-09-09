package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.Table;
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
 * Response of &#x60;GET /spa-appointments?date&#x3D;&#x60;. &#x60;openingTime&#x60;/&#x60;closingTime&#x60;/&#x60;slotMinutes&#x60; come from the backend&#39;s own config (&#x60;app.spa.*&#x60;), not a frontend constant, so the grid can never render slots the server wouldn&#39;t accept. &#x60;tables&#x60; is every active SPA-zone &#x60;Table&#x60; (the grid&#39;s rows); &#x60;appointments&#x60; is every appointment on &#x60;date&#x60; regardless of status (a cancelled/no-show slot still needs to render, just not as occupied). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaSchedule {

  private String date;

  private String openingTime;

  private String closingTime;

  private Integer slotMinutes;

  @Valid
  private List<@Valid Table> tables = new ArrayList<>();

  @Valid
  private List<@Valid SpaAppointment> appointments = new ArrayList<>();

  public SpaSchedule() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaSchedule(String date, String openingTime, String closingTime, Integer slotMinutes, List<@Valid Table> tables, List<@Valid SpaAppointment> appointments) {
    this.date = date;
    this.openingTime = openingTime;
    this.closingTime = closingTime;
    this.slotMinutes = slotMinutes;
    this.tables = tables;
    this.appointments = appointments;
  }

  public SpaSchedule date(String date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("date")
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public SpaSchedule openingTime(String openingTime) {
    this.openingTime = openingTime;
    return this;
  }

  /**
   * Get openingTime
   * @return openingTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("openingTime")
  public String getOpeningTime() {
    return openingTime;
  }

  public void setOpeningTime(String openingTime) {
    this.openingTime = openingTime;
  }

  public SpaSchedule closingTime(String closingTime) {
    this.closingTime = closingTime;
    return this;
  }

  /**
   * Get closingTime
   * @return closingTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("closingTime")
  public String getClosingTime() {
    return closingTime;
  }

  public void setClosingTime(String closingTime) {
    this.closingTime = closingTime;
  }

  public SpaSchedule slotMinutes(Integer slotMinutes) {
    this.slotMinutes = slotMinutes;
    return this;
  }

  /**
   * Get slotMinutes
   * @return slotMinutes
   */
  @NotNull 
  @JsonProperty("slotMinutes")
  public Integer getSlotMinutes() {
    return slotMinutes;
  }

  public void setSlotMinutes(Integer slotMinutes) {
    this.slotMinutes = slotMinutes;
  }

  public SpaSchedule tables(List<@Valid Table> tables) {
    this.tables = tables;
    return this;
  }

  public SpaSchedule addTablesItem(Table tablesItem) {
    if (this.tables == null) {
      this.tables = new ArrayList<>();
    }
    this.tables.add(tablesItem);
    return this;
  }

  /**
   * Get tables
   * @return tables
   */
  @NotNull @Valid 
  @JsonProperty("tables")
  public List<@Valid Table> getTables() {
    return tables;
  }

  public void setTables(List<@Valid Table> tables) {
    this.tables = tables;
  }

  public SpaSchedule appointments(List<@Valid SpaAppointment> appointments) {
    this.appointments = appointments;
    return this;
  }

  public SpaSchedule addAppointmentsItem(SpaAppointment appointmentsItem) {
    if (this.appointments == null) {
      this.appointments = new ArrayList<>();
    }
    this.appointments.add(appointmentsItem);
    return this;
  }

  /**
   * Get appointments
   * @return appointments
   */
  @NotNull @Valid 
  @JsonProperty("appointments")
  public List<@Valid SpaAppointment> getAppointments() {
    return appointments;
  }

  public void setAppointments(List<@Valid SpaAppointment> appointments) {
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
    SpaSchedule spaSchedule = (SpaSchedule) o;
    return Objects.equals(this.date, spaSchedule.date) &&
        Objects.equals(this.openingTime, spaSchedule.openingTime) &&
        Objects.equals(this.closingTime, spaSchedule.closingTime) &&
        Objects.equals(this.slotMinutes, spaSchedule.slotMinutes) &&
        Objects.equals(this.tables, spaSchedule.tables) &&
        Objects.equals(this.appointments, spaSchedule.appointments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, openingTime, closingTime, slotMinutes, tables, appointments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaSchedule {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    openingTime: ").append(toIndentedString(openingTime)).append("\n");
    sb.append("    closingTime: ").append(toIndentedString(closingTime)).append("\n");
    sb.append("    slotMinutes: ").append(toIndentedString(slotMinutes)).append("\n");
    sb.append("    tables: ").append(toIndentedString(tables)).append("\n");
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

