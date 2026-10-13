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
 * Opening hours and the automation parameters. &#x60;leadMinutes&#x60; is how long before a reservation the equipment is switched on, and also the gap between two reservations below which the equipment is not switched off in between. The air-conditioner values are the scene command sent at switch-on (&#x60;GymAcCommandInput&#x60;: mode 0 cool, temp 16..30, wind 0 auto / 1..3). Defaults: 06:00-22:00, 30 minutes, mode 0, 16 degrees, wind 3. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymHallSettings {

  private String openTime;

  private String closeTime;

  private Integer leadMinutes;

  private Integer acMode;

  private Integer acTemp;

  private Integer acWind;

  public GymHallSettings() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymHallSettings(String openTime, String closeTime, Integer leadMinutes, Integer acMode, Integer acTemp, Integer acWind) {
    this.openTime = openTime;
    this.closeTime = closeTime;
    this.leadMinutes = leadMinutes;
    this.acMode = acMode;
    this.acTemp = acTemp;
    this.acWind = acWind;
  }

  public GymHallSettings openTime(String openTime) {
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

  public GymHallSettings closeTime(String closeTime) {
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

  public GymHallSettings leadMinutes(Integer leadMinutes) {
    this.leadMinutes = leadMinutes;
    return this;
  }

  /**
   * Get leadMinutes
   * minimum: 0
   * maximum: 180
   * @return leadMinutes
   */
  @NotNull @Min(0) @Max(180) 
  @JsonProperty("leadMinutes")
  public Integer getLeadMinutes() {
    return leadMinutes;
  }

  public void setLeadMinutes(Integer leadMinutes) {
    this.leadMinutes = leadMinutes;
  }

  public GymHallSettings acMode(Integer acMode) {
    this.acMode = acMode;
    return this;
  }

  /**
   * Get acMode
   * minimum: 0
   * maximum: 4
   * @return acMode
   */
  @NotNull @Min(0) @Max(4) 
  @JsonProperty("acMode")
  public Integer getAcMode() {
    return acMode;
  }

  public void setAcMode(Integer acMode) {
    this.acMode = acMode;
  }

  public GymHallSettings acTemp(Integer acTemp) {
    this.acTemp = acTemp;
    return this;
  }

  /**
   * Get acTemp
   * minimum: 16
   * maximum: 30
   * @return acTemp
   */
  @NotNull @Min(16) @Max(30) 
  @JsonProperty("acTemp")
  public Integer getAcTemp() {
    return acTemp;
  }

  public void setAcTemp(Integer acTemp) {
    this.acTemp = acTemp;
  }

  public GymHallSettings acWind(Integer acWind) {
    this.acWind = acWind;
    return this;
  }

  /**
   * Get acWind
   * minimum: 0
   * maximum: 3
   * @return acWind
   */
  @NotNull @Min(0) @Max(3) 
  @JsonProperty("acWind")
  public Integer getAcWind() {
    return acWind;
  }

  public void setAcWind(Integer acWind) {
    this.acWind = acWind;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymHallSettings gymHallSettings = (GymHallSettings) o;
    return Objects.equals(this.openTime, gymHallSettings.openTime) &&
        Objects.equals(this.closeTime, gymHallSettings.closeTime) &&
        Objects.equals(this.leadMinutes, gymHallSettings.leadMinutes) &&
        Objects.equals(this.acMode, gymHallSettings.acMode) &&
        Objects.equals(this.acTemp, gymHallSettings.acTemp) &&
        Objects.equals(this.acWind, gymHallSettings.acWind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(openTime, closeTime, leadMinutes, acMode, acTemp, acWind);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymHallSettings {\n");
    sb.append("    openTime: ").append(toIndentedString(openTime)).append("\n");
    sb.append("    closeTime: ").append(toIndentedString(closeTime)).append("\n");
    sb.append("    leadMinutes: ").append(toIndentedString(leadMinutes)).append("\n");
    sb.append("    acMode: ").append(toIndentedString(acMode)).append("\n");
    sb.append("    acTemp: ").append(toIndentedString(acTemp)).append("\n");
    sb.append("    acWind: ").append(toIndentedString(acWind)).append("\n");
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

