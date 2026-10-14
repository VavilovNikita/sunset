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
 * GymStatus
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymStatus {

  private Boolean tuyaConfigured;

  private Integer automatedDeviceCount;

  public GymStatus() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymStatus(Boolean tuyaConfigured, Integer automatedDeviceCount) {
    this.tuyaConfigured = tuyaConfigured;
    this.automatedDeviceCount = automatedDeviceCount;
  }

  public GymStatus tuyaConfigured(Boolean tuyaConfigured) {
    this.tuyaConfigured = tuyaConfigured;
    return this;
  }

  /**
   * The server has Tuya keys and an API base. Says nothing about whether Tuya is reachable.
   * @return tuyaConfigured
   */
  @NotNull 
  @JsonProperty("tuyaConfigured")
  public Boolean getTuyaConfigured() {
    return tuyaConfigured;
  }

  public void setTuyaConfigured(Boolean tuyaConfigured) {
    this.tuyaConfigured = tuyaConfigured;
  }

  public GymStatus automatedDeviceCount(Integer automatedDeviceCount) {
    this.automatedDeviceCount = automatedDeviceCount;
    return this;
  }

  /**
   * Active devices the reservation automation switches (`inBookings`).
   * minimum: 0
   * @return automatedDeviceCount
   */
  @NotNull @Min(0) 
  @JsonProperty("automatedDeviceCount")
  public Integer getAutomatedDeviceCount() {
    return automatedDeviceCount;
  }

  public void setAutomatedDeviceCount(Integer automatedDeviceCount) {
    this.automatedDeviceCount = automatedDeviceCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymStatus gymStatus = (GymStatus) o;
    return Objects.equals(this.tuyaConfigured, gymStatus.tuyaConfigured) &&
        Objects.equals(this.automatedDeviceCount, gymStatus.automatedDeviceCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tuyaConfigured, automatedDeviceCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymStatus {\n");
    sb.append("    tuyaConfigured: ").append(toIndentedString(tuyaConfigured)).append("\n");
    sb.append("    automatedDeviceCount: ").append(toIndentedString(automatedDeviceCount)).append("\n");
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

