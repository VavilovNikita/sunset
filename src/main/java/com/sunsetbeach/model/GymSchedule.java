package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.GymReservation;
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
 * GymSchedule
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymSchedule {

  private String from;

  private String to;

  private String openTime;

  private String closeTime;

  private Integer stepMinutes;

  private Integer minDurationMinutes;

  private Integer maxDurationMinutes;

  private Integer maxDailyMinutes;

  @Valid
  private List<@Valid GymReservation> reservations = new ArrayList<>();

  public GymSchedule() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymSchedule(String from, String to, String openTime, String closeTime, Integer stepMinutes, Integer minDurationMinutes, Integer maxDurationMinutes, Integer maxDailyMinutes, List<@Valid GymReservation> reservations) {
    this.from = from;
    this.to = to;
    this.openTime = openTime;
    this.closeTime = closeTime;
    this.stepMinutes = stepMinutes;
    this.minDurationMinutes = minDurationMinutes;
    this.maxDurationMinutes = maxDurationMinutes;
    this.maxDailyMinutes = maxDailyMinutes;
    this.reservations = reservations;
  }

  public GymSchedule from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public GymSchedule to(String to) {
    this.to = to;
    return this;
  }

  /**
   * Get to
   * @return to
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public GymSchedule openTime(String openTime) {
    this.openTime = openTime;
    return this;
  }

  /**
   * Get openTime
   * @return openTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("openTime")
  public String getOpenTime() {
    return openTime;
  }

  public void setOpenTime(String openTime) {
    this.openTime = openTime;
  }

  public GymSchedule closeTime(String closeTime) {
    this.closeTime = closeTime;
    return this;
  }

  /**
   * Get closeTime
   * @return closeTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("closeTime")
  public String getCloseTime() {
    return closeTime;
  }

  public void setCloseTime(String closeTime) {
    this.closeTime = closeTime;
  }

  public GymSchedule stepMinutes(Integer stepMinutes) {
    this.stepMinutes = stepMinutes;
    return this;
  }

  /**
   * Get stepMinutes
   * @return stepMinutes
   */
  @NotNull 
  @JsonProperty("stepMinutes")
  public Integer getStepMinutes() {
    return stepMinutes;
  }

  public void setStepMinutes(Integer stepMinutes) {
    this.stepMinutes = stepMinutes;
  }

  public GymSchedule minDurationMinutes(Integer minDurationMinutes) {
    this.minDurationMinutes = minDurationMinutes;
    return this;
  }

  /**
   * Get minDurationMinutes
   * @return minDurationMinutes
   */
  @NotNull 
  @JsonProperty("minDurationMinutes")
  public Integer getMinDurationMinutes() {
    return minDurationMinutes;
  }

  public void setMinDurationMinutes(Integer minDurationMinutes) {
    this.minDurationMinutes = minDurationMinutes;
  }

  public GymSchedule maxDurationMinutes(Integer maxDurationMinutes) {
    this.maxDurationMinutes = maxDurationMinutes;
    return this;
  }

  /**
   * Get maxDurationMinutes
   * @return maxDurationMinutes
   */
  @NotNull 
  @JsonProperty("maxDurationMinutes")
  public Integer getMaxDurationMinutes() {
    return maxDurationMinutes;
  }

  public void setMaxDurationMinutes(Integer maxDurationMinutes) {
    this.maxDurationMinutes = maxDurationMinutes;
  }

  public GymSchedule maxDailyMinutes(Integer maxDailyMinutes) {
    this.maxDailyMinutes = maxDailyMinutes;
    return this;
  }

  /**
   * Get maxDailyMinutes
   * @return maxDailyMinutes
   */
  @NotNull 
  @JsonProperty("maxDailyMinutes")
  public Integer getMaxDailyMinutes() {
    return maxDailyMinutes;
  }

  public void setMaxDailyMinutes(Integer maxDailyMinutes) {
    this.maxDailyMinutes = maxDailyMinutes;
  }

  public GymSchedule reservations(List<@Valid GymReservation> reservations) {
    this.reservations = reservations;
    return this;
  }

  public GymSchedule addReservationsItem(GymReservation reservationsItem) {
    if (this.reservations == null) {
      this.reservations = new ArrayList<>();
    }
    this.reservations.add(reservationsItem);
    return this;
  }

  /**
   * Get reservations
   * @return reservations
   */
  @NotNull @Valid 
  @JsonProperty("reservations")
  public List<@Valid GymReservation> getReservations() {
    return reservations;
  }

  public void setReservations(List<@Valid GymReservation> reservations) {
    this.reservations = reservations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymSchedule gymSchedule = (GymSchedule) o;
    return Objects.equals(this.from, gymSchedule.from) &&
        Objects.equals(this.to, gymSchedule.to) &&
        Objects.equals(this.openTime, gymSchedule.openTime) &&
        Objects.equals(this.closeTime, gymSchedule.closeTime) &&
        Objects.equals(this.stepMinutes, gymSchedule.stepMinutes) &&
        Objects.equals(this.minDurationMinutes, gymSchedule.minDurationMinutes) &&
        Objects.equals(this.maxDurationMinutes, gymSchedule.maxDurationMinutes) &&
        Objects.equals(this.maxDailyMinutes, gymSchedule.maxDailyMinutes) &&
        Objects.equals(this.reservations, gymSchedule.reservations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, openTime, closeTime, stepMinutes, minDurationMinutes, maxDurationMinutes, maxDailyMinutes, reservations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymSchedule {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    openTime: ").append(toIndentedString(openTime)).append("\n");
    sb.append("    closeTime: ").append(toIndentedString(closeTime)).append("\n");
    sb.append("    stepMinutes: ").append(toIndentedString(stepMinutes)).append("\n");
    sb.append("    minDurationMinutes: ").append(toIndentedString(minDurationMinutes)).append("\n");
    sb.append("    maxDurationMinutes: ").append(toIndentedString(maxDurationMinutes)).append("\n");
    sb.append("    maxDailyMinutes: ").append(toIndentedString(maxDailyMinutes)).append("\n");
    sb.append("    reservations: ").append(toIndentedString(reservations)).append("\n");
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

