package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.NightAuditBooking;
import com.sunsetbeach.model.NightAuditClosure;
import com.sunsetbeach.model.OccupancyReportRow;
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
 * &#x60;GET /night-audit&#x60; - see that operation for what each list contains.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NightAudit {

  private String date;

  @Valid
  private List<@Valid NightAuditBooking> missedArrivals = new ArrayList<>();

  @Valid
  private List<@Valid NightAuditBooking> missedDepartures = new ArrayList<>();

  private OccupancyReportRow snapshot;

  private JsonNullable<NightAuditClosure> closure = JsonNullable.<NightAuditClosure>undefined();

  public NightAudit() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NightAudit(String date, List<@Valid NightAuditBooking> missedArrivals, List<@Valid NightAuditBooking> missedDepartures, OccupancyReportRow snapshot, NightAuditClosure closure) {
    this.date = date;
    this.missedArrivals = missedArrivals;
    this.missedDepartures = missedDepartures;
    this.snapshot = snapshot;
    this.closure = JsonNullable.of(closure);
  }

  public NightAudit date(String date) {
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

  public NightAudit missedArrivals(List<@Valid NightAuditBooking> missedArrivals) {
    this.missedArrivals = missedArrivals;
    return this;
  }

  public NightAudit addMissedArrivalsItem(NightAuditBooking missedArrivalsItem) {
    if (this.missedArrivals == null) {
      this.missedArrivals = new ArrayList<>();
    }
    this.missedArrivals.add(missedArrivalsItem);
    return this;
  }

  /**
   * Sorted by `checkIn`, oldest first.
   * @return missedArrivals
   */
  @NotNull @Valid 
  @JsonProperty("missedArrivals")
  public List<@Valid NightAuditBooking> getMissedArrivals() {
    return missedArrivals;
  }

  public void setMissedArrivals(List<@Valid NightAuditBooking> missedArrivals) {
    this.missedArrivals = missedArrivals;
  }

  public NightAudit missedDepartures(List<@Valid NightAuditBooking> missedDepartures) {
    this.missedDepartures = missedDepartures;
    return this;
  }

  public NightAudit addMissedDeparturesItem(NightAuditBooking missedDeparturesItem) {
    if (this.missedDepartures == null) {
      this.missedDepartures = new ArrayList<>();
    }
    this.missedDepartures.add(missedDeparturesItem);
    return this;
  }

  /**
   * Sorted by `checkOut`, oldest first.
   * @return missedDepartures
   */
  @NotNull @Valid 
  @JsonProperty("missedDepartures")
  public List<@Valid NightAuditBooking> getMissedDepartures() {
    return missedDepartures;
  }

  public void setMissedDepartures(List<@Valid NightAuditBooking> missedDepartures) {
    this.missedDepartures = missedDepartures;
  }

  public NightAudit snapshot(OccupancyReportRow snapshot) {
    this.snapshot = snapshot;
    return this;
  }

  /**
   * Get snapshot
   * @return snapshot
   */
  @NotNull @Valid 
  @JsonProperty("snapshot")
  public OccupancyReportRow getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(OccupancyReportRow snapshot) {
    this.snapshot = snapshot;
  }

  public NightAudit closure(NightAuditClosure closure) {
    this.closure = JsonNullable.of(closure);
    return this;
  }

  /**
   * This date's closure, or null if nobody has closed it yet.
   * @return closure
   */
  @NotNull @Valid 
  @JsonProperty("closure")
  public JsonNullable<NightAuditClosure> getClosure() {
    return closure;
  }

  public void setClosure(JsonNullable<NightAuditClosure> closure) {
    this.closure = closure;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NightAudit nightAudit = (NightAudit) o;
    return Objects.equals(this.date, nightAudit.date) &&
        Objects.equals(this.missedArrivals, nightAudit.missedArrivals) &&
        Objects.equals(this.missedDepartures, nightAudit.missedDepartures) &&
        Objects.equals(this.snapshot, nightAudit.snapshot) &&
        Objects.equals(this.closure, nightAudit.closure);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, missedArrivals, missedDepartures, snapshot, closure);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NightAudit {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    missedArrivals: ").append(toIndentedString(missedArrivals)).append("\n");
    sb.append("    missedDepartures: ").append(toIndentedString(missedDepartures)).append("\n");
    sb.append("    snapshot: ").append(toIndentedString(snapshot)).append("\n");
    sb.append("    closure: ").append(toIndentedString(closure)).append("\n");
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

