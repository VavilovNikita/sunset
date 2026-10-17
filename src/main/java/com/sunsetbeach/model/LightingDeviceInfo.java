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
 * What the controller&#39;s &#x60;/json/info&#x60; says about itself.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LightingDeviceInfo {

  private String name;

  private String version;

  private Integer ledCount;

  private String mac;

  public LightingDeviceInfo() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LightingDeviceInfo(String name, String version, Integer ledCount, String mac) {
    this.name = name;
    this.version = version;
    this.ledCount = ledCount;
    this.mac = mac;
  }

  public LightingDeviceInfo name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The controller's own friendly name.
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LightingDeviceInfo version(String version) {
    this.version = version;
    return this;
  }

  /**
   * WLED version (`ver`), for example `0.15.0`.
   * @return version
   */
  @NotNull 
  @JsonProperty("version")
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public LightingDeviceInfo ledCount(Integer ledCount) {
    this.ledCount = ledCount;
    return this;
  }

  /**
   * Get ledCount
   * @return ledCount
   */
  @NotNull 
  @JsonProperty("ledCount")
  public Integer getLedCount() {
    return ledCount;
  }

  public void setLedCount(Integer ledCount) {
    this.ledCount = ledCount;
  }

  public LightingDeviceInfo mac(String mac) {
    this.mac = mac;
    return this;
  }

  /**
   * Lower case, no colons, as WLED reports it.
   * @return mac
   */
  @NotNull 
  @JsonProperty("mac")
  public String getMac() {
    return mac;
  }

  public void setMac(String mac) {
    this.mac = mac;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LightingDeviceInfo lightingDeviceInfo = (LightingDeviceInfo) o;
    return Objects.equals(this.name, lightingDeviceInfo.name) &&
        Objects.equals(this.version, lightingDeviceInfo.version) &&
        Objects.equals(this.ledCount, lightingDeviceInfo.ledCount) &&
        Objects.equals(this.mac, lightingDeviceInfo.mac);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, ledCount, mac);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LightingDeviceInfo {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    ledCount: ").append(toIndentedString(ledCount)).append("\n");
    sb.append("    mac: ").append(toIndentedString(mac)).append("\n");
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

