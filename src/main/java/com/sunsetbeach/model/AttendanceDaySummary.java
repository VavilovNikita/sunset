package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftInterval;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One day of &#x60;GET /attendance/summary&#x60;. &#x60;shiftCode&#x60; is null on a day with no &#x60;RosterEntry&#x60; (a day off) - distinct from &#x60;OP&#x60;, which has a &#x60;shiftCode&#x60; but an empty &#x60;plannedIntervals&#x60;, so a day genuinely without a plan is never shown as \&quot;0 minutes planned\&quot; next to one that was deliberately left open. &#x60;incomplete&#x60; is true when this day&#39;s punches don&#39;t pair off evenly (an odd count) - closed only by recording another punch with a note through &#x60;POST /attendance&#x60;, never inferred. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class AttendanceDaySummary {

  private String date;

  private JsonNullable<ShiftCode> shiftCode = JsonNullable.<ShiftCode>undefined();

  @Valid
  private List<@Valid ShiftInterval> plannedIntervals = new ArrayList<>();

  @Valid
  private List<@Valid AttendancePunch> punches = new ArrayList<>();

  private JsonNullable<Integer> workedMinutes = JsonNullable.<Integer>undefined();

  private Boolean incomplete;

  public AttendanceDaySummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AttendanceDaySummary(String date, List<@Valid ShiftInterval> plannedIntervals, List<@Valid AttendancePunch> punches, Boolean incomplete) {
    this.date = date;
    this.plannedIntervals = plannedIntervals;
    this.punches = punches;
    this.incomplete = incomplete;
  }

  public AttendanceDaySummary date(String date) {
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

  public AttendanceDaySummary shiftCode(ShiftCode shiftCode) {
    this.shiftCode = JsonNullable.of(shiftCode);
    return this;
  }

  /**
   * Get shiftCode
   * @return shiftCode
   */
  @Valid 
  @JsonProperty("shiftCode")
  public JsonNullable<ShiftCode> getShiftCode() {
    return shiftCode;
  }

  public void setShiftCode(JsonNullable<ShiftCode> shiftCode) {
    this.shiftCode = shiftCode;
  }

  public AttendanceDaySummary plannedIntervals(List<@Valid ShiftInterval> plannedIntervals) {
    this.plannedIntervals = plannedIntervals;
    return this;
  }

  public AttendanceDaySummary addPlannedIntervalsItem(ShiftInterval plannedIntervalsItem) {
    if (this.plannedIntervals == null) {
      this.plannedIntervals = new ArrayList<>();
    }
    this.plannedIntervals.add(plannedIntervalsItem);
    return this;
  }

  /**
   * Get plannedIntervals
   * @return plannedIntervals
   */
  @NotNull @Valid 
  @JsonProperty("plannedIntervals")
  public List<@Valid ShiftInterval> getPlannedIntervals() {
    return plannedIntervals;
  }

  public void setPlannedIntervals(List<@Valid ShiftInterval> plannedIntervals) {
    this.plannedIntervals = plannedIntervals;
  }

  public AttendanceDaySummary punches(List<@Valid AttendancePunch> punches) {
    this.punches = punches;
    return this;
  }

  public AttendanceDaySummary addPunchesItem(AttendancePunch punchesItem) {
    if (this.punches == null) {
      this.punches = new ArrayList<>();
    }
    this.punches.add(punchesItem);
    return this;
  }

  /**
   * Get punches
   * @return punches
   */
  @NotNull @Valid 
  @JsonProperty("punches")
  public List<@Valid AttendancePunch> getPunches() {
    return punches;
  }

  public void setPunches(List<@Valid AttendancePunch> punches) {
    this.punches = punches;
  }

  public AttendanceDaySummary workedMinutes(Integer workedMinutes) {
    this.workedMinutes = JsonNullable.of(workedMinutes);
    return this;
  }

  /**
   * Null when there are no punches yet for this day, not zero.
   * @return workedMinutes
   */
  
  @JsonProperty("workedMinutes")
  public JsonNullable<Integer> getWorkedMinutes() {
    return workedMinutes;
  }

  public void setWorkedMinutes(JsonNullable<Integer> workedMinutes) {
    this.workedMinutes = workedMinutes;
  }

  public AttendanceDaySummary incomplete(Boolean incomplete) {
    this.incomplete = incomplete;
    return this;
  }

  /**
   * Get incomplete
   * @return incomplete
   */
  @NotNull 
  @JsonProperty("incomplete")
  public Boolean getIncomplete() {
    return incomplete;
  }

  public void setIncomplete(Boolean incomplete) {
    this.incomplete = incomplete;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttendanceDaySummary attendanceDaySummary = (AttendanceDaySummary) o;
    return Objects.equals(this.date, attendanceDaySummary.date) &&
        equalsNullable(this.shiftCode, attendanceDaySummary.shiftCode) &&
        Objects.equals(this.plannedIntervals, attendanceDaySummary.plannedIntervals) &&
        Objects.equals(this.punches, attendanceDaySummary.punches) &&
        equalsNullable(this.workedMinutes, attendanceDaySummary.workedMinutes) &&
        Objects.equals(this.incomplete, attendanceDaySummary.incomplete);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, hashCodeNullable(shiftCode), plannedIntervals, punches, hashCodeNullable(workedMinutes), incomplete);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttendanceDaySummary {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    shiftCode: ").append(toIndentedString(shiftCode)).append("\n");
    sb.append("    plannedIntervals: ").append(toIndentedString(plannedIntervals)).append("\n");
    sb.append("    punches: ").append(toIndentedString(punches)).append("\n");
    sb.append("    workedMinutes: ").append(toIndentedString(workedMinutes)).append("\n");
    sb.append("    incomplete: ").append(toIndentedString(incomplete)).append("\n");
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

