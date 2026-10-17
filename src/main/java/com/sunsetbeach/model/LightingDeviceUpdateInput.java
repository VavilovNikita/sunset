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
 * Partial update - only the fields present change.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LightingDeviceUpdateInput {

  private String name;

  private String host;

  private Integer port;

  private String location;

  private Boolean active;

  public LightingDeviceUpdateInput name(String name) {
    this.name = com.sunsetbeach.util.NameText.strip(name);
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @Size(min = 1, max = 120) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = com.sunsetbeach.util.NameText.strip(name);
  }

  public LightingDeviceUpdateInput host(String host) {
    this.host = com.sunsetbeach.util.NameText.strip(host);
    return this;
  }

  /**
   * Get host
   * @return host
   */
  @Size(min = 1, max = 253) 
  @JsonProperty("host")
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = com.sunsetbeach.util.NameText.strip(host);
  }

  public LightingDeviceUpdateInput port(Integer port) {
    this.port = port;
    return this;
  }

  /**
   * Get port
   * minimum: 1
   * maximum: 65535
   * @return port
   */
  @Min(1) @Max(65535) 
  @JsonProperty("port")
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public LightingDeviceUpdateInput location(String location) {
    this.location = location;
    return this;
  }

  /**
   * Empty string clears the location.
   * @return location
   */
  @Size(max = 200) 
  @JsonProperty("location")
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public LightingDeviceUpdateInput active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * Get active
   * @return active
   */
  
  @JsonProperty("active")
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LightingDeviceUpdateInput lightingDeviceUpdateInput = (LightingDeviceUpdateInput) o;
    return Objects.equals(this.name, lightingDeviceUpdateInput.name) &&
        Objects.equals(this.host, lightingDeviceUpdateInput.host) &&
        Objects.equals(this.port, lightingDeviceUpdateInput.port) &&
        Objects.equals(this.location, lightingDeviceUpdateInput.location) &&
        Objects.equals(this.active, lightingDeviceUpdateInput.active);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, host, port, location, active);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LightingDeviceUpdateInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    host: ").append(toIndentedString(host)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
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

