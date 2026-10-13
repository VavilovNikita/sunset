package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.GymDeviceType;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Full replacement on update. The type-specific fields are checked by the server (relay - &#x60;switchCode&#x60;; air conditioner - &#x60;infraredId&#x60;).
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymDeviceConfigInput {

  private String name;

  private GymDeviceType type;

  private String tuyaDeviceId;

  private String infraredId;

  private String remoteId;

  private String switchCode;

  private Boolean active = true;

  public GymDeviceConfigInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymDeviceConfigInput(String name, GymDeviceType type, String tuyaDeviceId) {
    this.name = name == null ? null : name.strip();
    this.type = type;
    this.tuyaDeviceId = tuyaDeviceId;
  }

  public GymDeviceConfigInput name(String name) {
    this.name = name == null ? null : name.strip();
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 120) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name == null ? null : name.strip();
  }

  public GymDeviceConfigInput type(GymDeviceType type) {
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

  public GymDeviceConfigInput tuyaDeviceId(String tuyaDeviceId) {
    this.tuyaDeviceId = tuyaDeviceId;
    return this;
  }

  /**
   * Get tuyaDeviceId
   * @return tuyaDeviceId
   */
  @NotNull @Size(min = 1, max = 64) 
  @JsonProperty("tuyaDeviceId")
  public String getTuyaDeviceId() {
    return tuyaDeviceId;
  }

  public void setTuyaDeviceId(String tuyaDeviceId) {
    this.tuyaDeviceId = tuyaDeviceId;
  }

  public GymDeviceConfigInput infraredId(String infraredId) {
    this.infraredId = infraredId;
    return this;
  }

  /**
   * Get infraredId
   * @return infraredId
   */
  @Size(max = 64) 
  @JsonProperty("infraredId")
  public String getInfraredId() {
    return infraredId;
  }

  public void setInfraredId(String infraredId) {
    this.infraredId = infraredId;
  }

  public GymDeviceConfigInput remoteId(String remoteId) {
    this.remoteId = remoteId;
    return this;
  }

  /**
   * Get remoteId
   * @return remoteId
   */
  @Size(max = 64) 
  @JsonProperty("remoteId")
  public String getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(String remoteId) {
    this.remoteId = remoteId;
  }

  public GymDeviceConfigInput switchCode(String switchCode) {
    this.switchCode = switchCode;
    return this;
  }

  /**
   * Get switchCode
   * @return switchCode
   */
  @Size(max = 64) 
  @JsonProperty("switchCode")
  public String getSwitchCode() {
    return switchCode;
  }

  public void setSwitchCode(String switchCode) {
    this.switchCode = switchCode;
  }

  public GymDeviceConfigInput active(Boolean active) {
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
    GymDeviceConfigInput gymDeviceConfigInput = (GymDeviceConfigInput) o;
    return Objects.equals(this.name, gymDeviceConfigInput.name) &&
        Objects.equals(this.type, gymDeviceConfigInput.type) &&
        Objects.equals(this.tuyaDeviceId, gymDeviceConfigInput.tuyaDeviceId) &&
        Objects.equals(this.infraredId, gymDeviceConfigInput.infraredId) &&
        Objects.equals(this.remoteId, gymDeviceConfigInput.remoteId) &&
        Objects.equals(this.switchCode, gymDeviceConfigInput.switchCode) &&
        Objects.equals(this.active, gymDeviceConfigInput.active);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, tuyaDeviceId, infraredId, remoteId, switchCode, active);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymDeviceConfigInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    tuyaDeviceId: ").append(toIndentedString(tuyaDeviceId)).append("\n");
    sb.append("    infraredId: ").append(toIndentedString(infraredId)).append("\n");
    sb.append("    remoteId: ").append(toIndentedString(remoteId)).append("\n");
    sb.append("    switchCode: ").append(toIndentedString(switchCode)).append("\n");
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

