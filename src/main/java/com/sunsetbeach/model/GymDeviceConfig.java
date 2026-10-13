package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.GymDeviceType;
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
 * GymDeviceConfig
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymDeviceConfig {

  private String id;

  private String name;

  private GymDeviceType type;

  private String tuyaDeviceId;

  private JsonNullable<String> infraredId = JsonNullable.<String>undefined();

  private JsonNullable<String> remoteId = JsonNullable.<String>undefined();

  private JsonNullable<String> switchCode = JsonNullable.<String>undefined();

  private Integer sortOrder;

  private Boolean active;

  public GymDeviceConfig() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymDeviceConfig(String id, String name, GymDeviceType type, String tuyaDeviceId, Integer sortOrder, Boolean active) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.tuyaDeviceId = tuyaDeviceId;
    this.sortOrder = sortOrder;
    this.active = active;
  }

  public GymDeviceConfig id(String id) {
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

  public GymDeviceConfig name(String name) {
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

  public GymDeviceConfig type(GymDeviceType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @JsonProperty("type")
  public GymDeviceType getType() {
    return type;
  }

  public void setType(GymDeviceType type) {
    this.type = type;
  }

  public GymDeviceConfig tuyaDeviceId(String tuyaDeviceId) {
    this.tuyaDeviceId = tuyaDeviceId;
    return this;
  }

  /**
   * Get tuyaDeviceId
   * @return tuyaDeviceId
   */
  @NotNull 
  @JsonProperty("tuyaDeviceId")
  public String getTuyaDeviceId() {
    return tuyaDeviceId;
  }

  public void setTuyaDeviceId(String tuyaDeviceId) {
    this.tuyaDeviceId = tuyaDeviceId;
  }

  public GymDeviceConfig infraredId(String infraredId) {
    this.infraredId = JsonNullable.of(infraredId);
    return this;
  }

  /**
   * The IR hub (air conditioners only).
   * @return infraredId
   */
  
  @JsonProperty("infraredId")
  public JsonNullable<String> getInfraredId() {
    return infraredId;
  }

  public void setInfraredId(JsonNullable<String> infraredId) {
    this.infraredId = infraredId;
  }

  public GymDeviceConfig remoteId(String remoteId) {
    this.remoteId = JsonNullable.of(remoteId);
    return this;
  }

  /**
   * The IR remote (air conditioners only).
   * @return remoteId
   */
  
  @JsonProperty("remoteId")
  public JsonNullable<String> getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(JsonNullable<String> remoteId) {
    this.remoteId = remoteId;
  }

  public GymDeviceConfig switchCode(String switchCode) {
    this.switchCode = JsonNullable.of(switchCode);
    return this;
  }

  /**
   * The status/command code of the on-off switch (relays only), e.g. `switch_1`.
   * @return switchCode
   */
  
  @JsonProperty("switchCode")
  public JsonNullable<String> getSwitchCode() {
    return switchCode;
  }

  public void setSwitchCode(JsonNullable<String> switchCode) {
    this.switchCode = switchCode;
  }

  public GymDeviceConfig sortOrder(Integer sortOrder) {
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

  public GymDeviceConfig active(Boolean active) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymDeviceConfig gymDeviceConfig = (GymDeviceConfig) o;
    return Objects.equals(this.id, gymDeviceConfig.id) &&
        Objects.equals(this.name, gymDeviceConfig.name) &&
        Objects.equals(this.type, gymDeviceConfig.type) &&
        Objects.equals(this.tuyaDeviceId, gymDeviceConfig.tuyaDeviceId) &&
        equalsNullable(this.infraredId, gymDeviceConfig.infraredId) &&
        equalsNullable(this.remoteId, gymDeviceConfig.remoteId) &&
        equalsNullable(this.switchCode, gymDeviceConfig.switchCode) &&
        Objects.equals(this.sortOrder, gymDeviceConfig.sortOrder) &&
        Objects.equals(this.active, gymDeviceConfig.active);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, type, tuyaDeviceId, hashCodeNullable(infraredId), hashCodeNullable(remoteId), hashCodeNullable(switchCode), sortOrder, active);
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
    sb.append("class GymDeviceConfig {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    tuyaDeviceId: ").append(toIndentedString(tuyaDeviceId)).append("\n");
    sb.append("    infraredId: ").append(toIndentedString(infraredId)).append("\n");
    sb.append("    remoteId: ").append(toIndentedString(remoteId)).append("\n");
    sb.append("    switchCode: ").append(toIndentedString(switchCode)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

