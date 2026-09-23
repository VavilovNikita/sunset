package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * &#x60;GET /attendance/server-time&#x60; - the server&#39;s own idea of \&quot;now\&quot;, read straight off the injected &#x60;Clock&#x60; bean (&#x60;ClockConfig&#x60;). A diagnostic snapshot, not a ticking clock: the frontend fetches this once per page load and renders it statically. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ServerTime {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime now;

  private String zone;

  public ServerTime() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ServerTime(OffsetDateTime now, String zone) {
    this.now = now;
    this.zone = zone;
  }

  public ServerTime now(OffsetDateTime now) {
    this.now = now;
    return this;
  }

  /**
   * The clock's current instant, rendered in its own zone's offset.
   * @return now
   */
  @NotNull @Valid 
  @JsonProperty("now")
  public OffsetDateTime getNow() {
    return now;
  }

  public void setNow(OffsetDateTime now) {
    this.now = now;
  }

  public ServerTime zone(String zone) {
    this.zone = zone;
    return this;
  }

  /**
   * The clock's zone id, e.g. \"Asia/Bangkok\".
   * @return zone
   */
  @NotNull 
  @JsonProperty("zone")
  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerTime serverTime = (ServerTime) o;
    return Objects.equals(this.now, serverTime.now) &&
        Objects.equals(this.zone, serverTime.zone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(now, zone);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerTime {\n");
    sb.append("    now: ").append(toIndentedString(now)).append("\n");
    sb.append("    zone: ").append(toIndentedString(zone)).append("\n");
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

