package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.GymAcCommand;
import com.sunsetbeach.model.GymDeviceType;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A gym device as the page shows it. For &#x60;RELAY&#x60;: &#x60;isOn&#x60;, &#x60;online&#x60; and &#x60;stateReadAt&#x60; come from Tuya (possibly the 30 s cache), &#x60;stateError&#x60; says why they are missing. For &#x60;AC_IR&#x60;: only &#x60;lastCommand&#x60; (null until the first command), no live state exists. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymDevice {

  private String id;

  private String name;

  private GymDeviceType type;

  private Integer sortOrder;

  private JsonNullable<Boolean> isOn = JsonNullable.<Boolean>undefined();

  private JsonNullable<Boolean> online = JsonNullable.<Boolean>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> stateReadAt = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> stateError = JsonNullable.<String>undefined();

  private JsonNullable<GymAcCommand> lastCommand = JsonNullable.<GymAcCommand>undefined();

  public GymDevice() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymDevice(String id, String name, GymDeviceType type, Integer sortOrder) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.sortOrder = sortOrder;
  }

  public GymDevice id(String id) {
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

  public GymDevice name(String name) {
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

  public GymDevice type(GymDeviceType type) {
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

  public GymDevice sortOrder(Integer sortOrder) {
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

  public GymDevice isOn(Boolean isOn) {
    this.isOn = JsonNullable.of(isOn);
    return this;
  }

  /**
   * Get isOn
   * @return isOn
   */
  
  @JsonProperty("isOn")
  public JsonNullable<Boolean> getIsOn() {
    return isOn;
  }

  public void setIsOn(JsonNullable<Boolean> isOn) {
    this.isOn = isOn;
  }

  public GymDevice online(Boolean online) {
    this.online = JsonNullable.of(online);
    return this;
  }

  /**
   * Get online
   * @return online
   */
  
  @JsonProperty("online")
  public JsonNullable<Boolean> getOnline() {
    return online;
  }

  public void setOnline(JsonNullable<Boolean> online) {
    this.online = online;
  }

  public GymDevice stateReadAt(OffsetDateTime stateReadAt) {
    this.stateReadAt = JsonNullable.of(stateReadAt);
    return this;
  }

  /**
   * Get stateReadAt
   * @return stateReadAt
   */
  @Valid 
  @JsonProperty("stateReadAt")
  public JsonNullable<OffsetDateTime> getStateReadAt() {
    return stateReadAt;
  }

  public void setStateReadAt(JsonNullable<OffsetDateTime> stateReadAt) {
    this.stateReadAt = stateReadAt;
  }

  public GymDevice stateError(String stateError) {
    this.stateError = JsonNullable.of(stateError);
    return this;
  }

  /**
   * Get stateError
   * @return stateError
   */
  
  @JsonProperty("stateError")
  public JsonNullable<String> getStateError() {
    return stateError;
  }

  public void setStateError(JsonNullable<String> stateError) {
    this.stateError = stateError;
  }

  public GymDevice lastCommand(GymAcCommand lastCommand) {
    this.lastCommand = JsonNullable.of(lastCommand);
    return this;
  }

  /**
   * Get lastCommand
   * @return lastCommand
   */
  @Valid 
  @JsonProperty("lastCommand")
  public JsonNullable<GymAcCommand> getLastCommand() {
    return lastCommand;
  }

  public void setLastCommand(JsonNullable<GymAcCommand> lastCommand) {
    this.lastCommand = lastCommand;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymDevice gymDevice = (GymDevice) o;
    return Objects.equals(this.id, gymDevice.id) &&
        Objects.equals(this.name, gymDevice.name) &&
        Objects.equals(this.type, gymDevice.type) &&
        Objects.equals(this.sortOrder, gymDevice.sortOrder) &&
        equalsNullable(this.isOn, gymDevice.isOn) &&
        equalsNullable(this.online, gymDevice.online) &&
        equalsNullable(this.stateReadAt, gymDevice.stateReadAt) &&
        equalsNullable(this.stateError, gymDevice.stateError) &&
        equalsNullable(this.lastCommand, gymDevice.lastCommand);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, type, sortOrder, hashCodeNullable(isOn), hashCodeNullable(online), hashCodeNullable(stateReadAt), hashCodeNullable(stateError), hashCodeNullable(lastCommand));
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
    sb.append("class GymDevice {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    isOn: ").append(toIndentedString(isOn)).append("\n");
    sb.append("    online: ").append(toIndentedString(online)).append("\n");
    sb.append("    stateReadAt: ").append(toIndentedString(stateReadAt)).append("\n");
    sb.append("    stateError: ").append(toIndentedString(stateError)).append("\n");
    sb.append("    lastCommand: ").append(toIndentedString(lastCommand)).append("\n");
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

