package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * LightingDeviceConfig
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LightingDeviceConfig {

  private String id;

  private String name;

  private String host;

  private Integer port;

  private JsonNullable<String> location = JsonNullable.<String>undefined();

  private Boolean active;

  private Integer sortOrder;

  public LightingDeviceConfig() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LightingDeviceConfig(String id, String name, String host, Integer port, Boolean active, Integer sortOrder) {
    this.id = id;
    this.name = name;
    this.host = host;
    this.port = port;
    this.active = active;
    this.sortOrder = sortOrder;
  }

  public LightingDeviceConfig id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public LightingDeviceConfig name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
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

  public LightingDeviceConfig host(String host) {
    this.host = host;
    return this;
  }

  /**
   * Get host
   * @return host
   */
  @NotNull 
  @JsonProperty("host")
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public LightingDeviceConfig port(Integer port) {
    this.port = port;
    return this;
  }

  /**
   * Get port
   * @return port
   */
  @NotNull 
  @JsonProperty("port")
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public LightingDeviceConfig location(String location) {
    this.location = JsonNullable.of(location);
    return this;
  }

  /**
   * Free text (\"Pool bar, roof\"), shown next to the name.
   * @return location
   */
  
  @JsonProperty("location")
  public JsonNullable<String> getLocation() {
    return location;
  }

  public void setLocation(JsonNullable<String> location) {
    this.location = location;
  }

  public LightingDeviceConfig active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * Get active
   * @return active
   */
  @NotNull 
  @JsonProperty("active")
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public LightingDeviceConfig sortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Get sortOrder
   * @return sortOrder
   */
  @NotNull 
  @JsonProperty("sortOrder")
  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LightingDeviceConfig lightingDeviceConfig = (LightingDeviceConfig) o;
    return Objects.equals(this.id, lightingDeviceConfig.id) &&
        Objects.equals(this.name, lightingDeviceConfig.name) &&
        Objects.equals(this.host, lightingDeviceConfig.host) &&
        Objects.equals(this.port, lightingDeviceConfig.port) &&
        equalsNullable(this.location, lightingDeviceConfig.location) &&
        Objects.equals(this.active, lightingDeviceConfig.active) &&
        Objects.equals(this.sortOrder, lightingDeviceConfig.sortOrder);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, host, port, hashCodeNullable(location), active, sortOrder);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LightingDeviceConfig {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    host: ").append(toIndentedString(host)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

