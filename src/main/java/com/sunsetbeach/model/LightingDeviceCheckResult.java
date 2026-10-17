package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.LightingDeviceInfo;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * LightingDeviceCheckResult
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LightingDeviceCheckResult {

  private Boolean reachable;

  private String error;

  private LightingDeviceInfo info;

  public LightingDeviceCheckResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LightingDeviceCheckResult(Boolean reachable) {
    this.reachable = reachable;
  }

  public LightingDeviceCheckResult reachable(Boolean reachable) {
    this.reachable = reachable;
    return this;
  }

  /**
   * Get reachable
   * @return reachable
   */
  @NotNull 
  @JsonProperty("reachable")
  public Boolean getReachable() {
    return reachable;
  }

  public void setReachable(Boolean reachable) {
    this.reachable = reachable;
  }

  public LightingDeviceCheckResult error(String error) {
    this.error = error;
    return this;
  }

  /**
   * Short plain-words reason when `reachable` is false. Never contains what the device sent back.
   * @return error
   */
  
  @JsonProperty("error")
  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public LightingDeviceCheckResult info(LightingDeviceInfo info) {
    this.info = info;
    return this;
  }

  /**
   * Get info
   * @return info
   */
  @Valid 
  @JsonProperty("info")
  public LightingDeviceInfo getInfo() {
    return info;
  }

  public void setInfo(LightingDeviceInfo info) {
    this.info = info;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LightingDeviceCheckResult lightingDeviceCheckResult = (LightingDeviceCheckResult) o;
    return Objects.equals(this.reachable, lightingDeviceCheckResult.reachable) &&
        Objects.equals(this.error, lightingDeviceCheckResult.error) &&
        Objects.equals(this.info, lightingDeviceCheckResult.info);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reachable, error, info);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LightingDeviceCheckResult {\n");
    sb.append("    reachable: ").append(toIndentedString(reachable)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    info: ").append(toIndentedString(info)).append("\n");
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

