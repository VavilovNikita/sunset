package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.GymHallSettings;
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
 * GymHallSettingsResult
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymHallSettingsResult {

  private GymHallSettings settings;

  private Boolean saved;

  @Valid
  private List<@Valid GymReservation> outsideHours = new ArrayList<>();

  public GymHallSettingsResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymHallSettingsResult(GymHallSettings settings, Boolean saved, List<@Valid GymReservation> outsideHours) {
    this.settings = settings;
    this.saved = saved;
    this.outsideHours = outsideHours;
  }

  public GymHallSettingsResult settings(GymHallSettings settings) {
    this.settings = settings;
    return this;
  }

  /**
   * Get settings
   * @return settings
   */
  @NotNull @Valid 
  @JsonProperty("settings")
  public GymHallSettings getSettings() {
    return settings;
  }

  public void setSettings(GymHallSettings settings) {
    this.settings = settings;
  }

  public GymHallSettingsResult saved(Boolean saved) {
    this.saved = saved;
    return this;
  }

  /**
   * False when the new hours would leave reservations outside them and `force` was not set - nothing was written.
   * @return saved
   */
  @NotNull 
  @JsonProperty("saved")
  public Boolean getSaved() {
    return saved;
  }

  public void setSaved(Boolean saved) {
    this.saved = saved;
  }

  public GymHallSettingsResult outsideHours(List<@Valid GymReservation> outsideHours) {
    this.outsideHours = outsideHours;
    return this;
  }

  public GymHallSettingsResult addOutsideHoursItem(GymReservation outsideHoursItem) {
    if (this.outsideHours == null) {
      this.outsideHours = new ArrayList<>();
    }
    this.outsideHours.add(outsideHoursItem);
    return this;
  }

  /**
   * Future `BOOKED` reservations that do not fit the requested hours (empty when the hours are not narrowed over any).
   * @return outsideHours
   */
  @NotNull @Valid 
  @JsonProperty("outsideHours")
  public List<@Valid GymReservation> getOutsideHours() {
    return outsideHours;
  }

  public void setOutsideHours(List<@Valid GymReservation> outsideHours) {
    this.outsideHours = outsideHours;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymHallSettingsResult gymHallSettingsResult = (GymHallSettingsResult) o;
    return Objects.equals(this.settings, gymHallSettingsResult.settings) &&
        Objects.equals(this.saved, gymHallSettingsResult.saved) &&
        Objects.equals(this.outsideHours, gymHallSettingsResult.outsideHours);
  }

  @Override
  public int hashCode() {
    return Objects.hash(settings, saved, outsideHours);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymHallSettingsResult {\n");
    sb.append("    settings: ").append(toIndentedString(settings)).append("\n");
    sb.append("    saved: ").append(toIndentedString(saved)).append("\n");
    sb.append("    outsideHours: ").append(toIndentedString(outsideHours)).append("\n");
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

