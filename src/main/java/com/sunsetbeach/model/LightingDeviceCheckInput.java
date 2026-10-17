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
 * LightingDeviceCheckInput
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LightingDeviceCheckInput {

  private String host;

  private Integer port = 80;

  public LightingDeviceCheckInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LightingDeviceCheckInput(String host) {
    this.host = com.sunsetbeach.util.NameText.strip(host);
  }

  public LightingDeviceCheckInput host(String host) {
    this.host = com.sunsetbeach.util.NameText.strip(host);
    return this;
  }

  /**
   * Get host
   * @return host
   */
  @NotNull @Size(min = 1, max = 253) 
  @JsonProperty("host")
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = com.sunsetbeach.util.NameText.strip(host);
  }

  public LightingDeviceCheckInput port(Integer port) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LightingDeviceCheckInput lightingDeviceCheckInput = (LightingDeviceCheckInput) o;
    return Objects.equals(this.host, lightingDeviceCheckInput.host) &&
        Objects.equals(this.port, lightingDeviceCheckInput.port);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LightingDeviceCheckInput {\n");
    sb.append("    host: ").append(toIndentedString(host)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
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

