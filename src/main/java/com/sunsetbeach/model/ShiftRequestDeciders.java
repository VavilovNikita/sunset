package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * ShiftRequestDeciders
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftRequestDeciders {

  private String hrUserId;

  private String hrName;

  private String vacationFrom;

  private String vacationTo;

  private Boolean onVacationToday;

  private Boolean fallbackToAdmin;

  @Valid
  private List<String> effectiveDeciders = new ArrayList<>();

  private Boolean callerMayDecide;

  public ShiftRequestDeciders() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftRequestDeciders(Boolean onVacationToday, Boolean fallbackToAdmin, List<String> effectiveDeciders, Boolean callerMayDecide) {
    this.onVacationToday = onVacationToday;
    this.fallbackToAdmin = fallbackToAdmin;
    this.effectiveDeciders = effectiveDeciders;
    this.callerMayDecide = callerMayDecide;
  }

  public ShiftRequestDeciders hrUserId(String hrUserId) {
    this.hrUserId = hrUserId;
    return this;
  }

  /**
   * The active holder of the `HR` job function; absent when nobody holds it.
   * @return hrUserId
   */
  
  @JsonProperty("hrUserId")
  public String getHrUserId() {
    return hrUserId;
  }

  public void setHrUserId(String hrUserId) {
    this.hrUserId = hrUserId;
  }

  public ShiftRequestDeciders hrName(String hrName) {
    this.hrName = hrName;
    return this;
  }

  /**
   * Get hrName
   * @return hrName
   */
  
  @JsonProperty("hrName")
  public String getHrName() {
    return hrName;
  }

  public void setHrName(String hrName) {
    this.hrName = hrName;
  }

  public ShiftRequestDeciders vacationFrom(String vacationFrom) {
    this.vacationFrom = vacationFrom;
    return this;
  }

  /**
   * Get vacationFrom
   * @return vacationFrom
   */
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("vacationFrom")
  public String getVacationFrom() {
    return vacationFrom;
  }

  public void setVacationFrom(String vacationFrom) {
    this.vacationFrom = vacationFrom;
  }

  public ShiftRequestDeciders vacationTo(String vacationTo) {
    this.vacationTo = vacationTo;
    return this;
  }

  /**
   * Get vacationTo
   * @return vacationTo
   */
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("vacationTo")
  public String getVacationTo() {
    return vacationTo;
  }

  public void setVacationTo(String vacationTo) {
    this.vacationTo = vacationTo;
  }

  public ShiftRequestDeciders onVacationToday(Boolean onVacationToday) {
    this.onVacationToday = onVacationToday;
    return this;
  }

  /**
   * Get onVacationToday
   * @return onVacationToday
   */
  @NotNull 
  @JsonProperty("onVacationToday")
  public Boolean getOnVacationToday() {
    return onVacationToday;
  }

  public void setOnVacationToday(Boolean onVacationToday) {
    this.onVacationToday = onVacationToday;
  }

  public ShiftRequestDeciders fallbackToAdmin(Boolean fallbackToAdmin) {
    this.fallbackToAdmin = fallbackToAdmin;
    return this;
  }

  /**
   * Get fallbackToAdmin
   * @return fallbackToAdmin
   */
  @NotNull 
  @JsonProperty("fallbackToAdmin")
  public Boolean getFallbackToAdmin() {
    return fallbackToAdmin;
  }

  public void setFallbackToAdmin(Boolean fallbackToAdmin) {
    this.fallbackToAdmin = fallbackToAdmin;
  }

  public ShiftRequestDeciders effectiveDeciders(List<String> effectiveDeciders) {
    this.effectiveDeciders = effectiveDeciders;
    return this;
  }

  public ShiftRequestDeciders addEffectiveDecidersItem(String effectiveDecidersItem) {
    if (this.effectiveDeciders == null) {
      this.effectiveDeciders = new ArrayList<>();
    }
    this.effectiveDeciders.add(effectiveDecidersItem);
    return this;
  }

  /**
   * Names of those who decide today - the `HR` holder, or the administrators while there is none / they are away and `fallbackToAdmin` is on. Empty means requests wait.
   * @return effectiveDeciders
   */
  @NotNull 
  @JsonProperty("effectiveDeciders")
  public List<String> getEffectiveDeciders() {
    return effectiveDeciders;
  }

  public void setEffectiveDeciders(List<String> effectiveDeciders) {
    this.effectiveDeciders = effectiveDeciders;
  }

  public ShiftRequestDeciders callerMayDecide(Boolean callerMayDecide) {
    this.callerMayDecide = callerMayDecide;
    return this;
  }

  /**
   * Get callerMayDecide
   * @return callerMayDecide
   */
  @NotNull 
  @JsonProperty("callerMayDecide")
  public Boolean getCallerMayDecide() {
    return callerMayDecide;
  }

  public void setCallerMayDecide(Boolean callerMayDecide) {
    this.callerMayDecide = callerMayDecide;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftRequestDeciders shiftRequestDeciders = (ShiftRequestDeciders) o;
    return Objects.equals(this.hrUserId, shiftRequestDeciders.hrUserId) &&
        Objects.equals(this.hrName, shiftRequestDeciders.hrName) &&
        Objects.equals(this.vacationFrom, shiftRequestDeciders.vacationFrom) &&
        Objects.equals(this.vacationTo, shiftRequestDeciders.vacationTo) &&
        Objects.equals(this.onVacationToday, shiftRequestDeciders.onVacationToday) &&
        Objects.equals(this.fallbackToAdmin, shiftRequestDeciders.fallbackToAdmin) &&
        Objects.equals(this.effectiveDeciders, shiftRequestDeciders.effectiveDeciders) &&
        Objects.equals(this.callerMayDecide, shiftRequestDeciders.callerMayDecide);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hrUserId, hrName, vacationFrom, vacationTo, onVacationToday, fallbackToAdmin, effectiveDeciders, callerMayDecide);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShiftRequestDeciders {\n");
    sb.append("    hrUserId: ").append(toIndentedString(hrUserId)).append("\n");
    sb.append("    hrName: ").append(toIndentedString(hrName)).append("\n");
    sb.append("    vacationFrom: ").append(toIndentedString(vacationFrom)).append("\n");
    sb.append("    vacationTo: ").append(toIndentedString(vacationTo)).append("\n");
    sb.append("    onVacationToday: ").append(toIndentedString(onVacationToday)).append("\n");
    sb.append("    fallbackToAdmin: ").append(toIndentedString(fallbackToAdmin)).append("\n");
    sb.append("    effectiveDeciders: ").append(toIndentedString(effectiveDeciders)).append("\n");
    sb.append("    callerMayDecide: ").append(toIndentedString(callerMayDecide)).append("\n");
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

