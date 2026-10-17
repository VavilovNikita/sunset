package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.LightingDevice;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * LightingDeviceList
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LightingDeviceList {

  private Boolean configured;

  @Valid
  private List<@Valid LightingDevice> devices = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime fetchedAt;

  public LightingDeviceList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LightingDeviceList(Boolean configured, List<@Valid LightingDevice> devices, OffsetDateTime fetchedAt) {
    this.configured = configured;
    this.devices = devices;
    this.fetchedAt = fetchedAt;
  }

  public LightingDeviceList configured(Boolean configured) {
    this.configured = configured;
    return this;
  }

  /**
   * Same as `GET /lighting/status`. When false, `devices` is empty.
   * @return configured
   */
  @NotNull 
  @JsonProperty("configured")
  public Boolean getConfigured() {
    return configured;
  }

  public void setConfigured(Boolean configured) {
    this.configured = configured;
  }

  public LightingDeviceList devices(List<@Valid LightingDevice> devices) {
    this.devices = devices;
    return this;
  }

  public LightingDeviceList addDevicesItem(LightingDevice devicesItem) {
    if (this.devices == null) {
      this.devices = new ArrayList<>();
    }
    this.devices.add(devicesItem);
    return this;
  }

  /**
   * Get devices
   * @return devices
   */
  @NotNull @Valid 
  @JsonProperty("devices")
  public List<@Valid LightingDevice> getDevices() {
    return devices;
  }

  public void setDevices(List<@Valid LightingDevice> devices) {
    this.devices = devices;
  }

  public LightingDeviceList fetchedAt(OffsetDateTime fetchedAt) {
    this.fetchedAt = fetchedAt;
    return this;
  }

  /**
   * Get fetchedAt
   * @return fetchedAt
   */
  @NotNull @Valid 
  @JsonProperty("fetchedAt")
  public OffsetDateTime getFetchedAt() {
    return fetchedAt;
  }

  public void setFetchedAt(OffsetDateTime fetchedAt) {
    this.fetchedAt = fetchedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LightingDeviceList lightingDeviceList = (LightingDeviceList) o;
    return Objects.equals(this.configured, lightingDeviceList.configured) &&
        Objects.equals(this.devices, lightingDeviceList.devices) &&
        Objects.equals(this.fetchedAt, lightingDeviceList.fetchedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configured, devices, fetchedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LightingDeviceList {\n");
    sb.append("    configured: ").append(toIndentedString(configured)).append("\n");
    sb.append("    devices: ").append(toIndentedString(devices)).append("\n");
    sb.append("    fetchedAt: ").append(toIndentedString(fetchedAt)).append("\n");
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

