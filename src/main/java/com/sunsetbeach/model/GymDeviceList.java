package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.GymDevice;
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
 * GymDeviceList
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymDeviceList {

  @Valid
  private List<@Valid GymDevice> devices = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime fetchedAt;

  public GymDeviceList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymDeviceList(List<@Valid GymDevice> devices, OffsetDateTime fetchedAt) {
    this.devices = devices;
    this.fetchedAt = fetchedAt;
  }

  public GymDeviceList devices(List<@Valid GymDevice> devices) {
    this.devices = devices;
    return this;
  }

  public GymDeviceList addDevicesItem(GymDevice devicesItem) {
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
  public List<@Valid GymDevice> getDevices() {
    return devices;
  }

  public void setDevices(List<@Valid GymDevice> devices) {
    this.devices = devices;
  }

  public GymDeviceList fetchedAt(OffsetDateTime fetchedAt) {
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
    GymDeviceList gymDeviceList = (GymDeviceList) o;
    return Objects.equals(this.devices, gymDeviceList.devices) &&
        Objects.equals(this.fetchedAt, gymDeviceList.fetchedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(devices, fetchedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymDeviceList {\n");
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

