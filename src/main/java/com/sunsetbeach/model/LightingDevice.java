package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.LightingPreset;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * A controller with its live state. When &#x60;online&#x60; is false, &#x60;isOn&#x60;, &#x60;brightness&#x60; and &#x60;presetId&#x60; are absent and &#x60;stateError&#x60; says why. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LightingDevice {

  private String id;

  private String name;

  private JsonNullable<String> location = JsonNullable.<String>undefined();

  private Integer sortOrder;

  private Boolean online;

  private Boolean isOn;

  private Integer brightness;

  private JsonNullable<Integer> presetId = JsonNullable.<Integer>undefined();

  @Valid
  private List<@Valid LightingPreset> presets = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime stateReadAt;

  private String stateError;

  public LightingDevice() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LightingDevice(String id, String name, Boolean online, List<@Valid LightingPreset> presets) {
    this.id = id;
    this.name = name;
    this.online = online;
    this.presets = presets;
  }

  public LightingDevice id(String id) {
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

  public LightingDevice name(String name) {
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

  public LightingDevice location(String location) {
    this.location = JsonNullable.of(location);
    return this;
  }

  /**
   * Get location
   * @return location
   */
  
  @JsonProperty("location")
  public JsonNullable<String> getLocation() {
    return location;
  }

  public void setLocation(JsonNullable<String> location) {
    this.location = location;
  }

  public LightingDevice sortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Get sortOrder
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public LightingDevice online(Boolean online) {
    this.online = online;
    return this;
  }

  /**
   * Get online
   * @return online
   */
  @NotNull 
  @JsonProperty("online")
  public Boolean getOnline() {
    return online;
  }

  public void setOnline(Boolean online) {
    this.online = online;
  }

  public LightingDevice isOn(Boolean isOn) {
    this.isOn = isOn;
    return this;
  }

  /**
   * Whether the lights are on. Absent when offline.
   * @return isOn
   */
  
  @JsonProperty("isOn")
  public Boolean getIsOn() {
    return isOn;
  }

  public void setIsOn(Boolean isOn) {
    this.isOn = isOn;
  }

  public LightingDevice brightness(Integer brightness) {
    this.brightness = brightness;
    return this;
  }

  /**
   * WLED's `bri` - the last brightness when the lights are off. Absent when offline.
   * minimum: 0
   * maximum: 255
   * @return brightness
   */
  @Min(0) @Max(255) 
  @JsonProperty("brightness")
  public Integer getBrightness() {
    return brightness;
  }

  public void setBrightness(Integer brightness) {
    this.brightness = brightness;
  }

  public LightingDevice presetId(Integer presetId) {
    this.presetId = JsonNullable.of(presetId);
    return this;
  }

  /**
   * The preset currently applied; null when none (WLED clears it when the look is changed by hand). Absent when offline.
   * @return presetId
   */
  
  @JsonProperty("presetId")
  public JsonNullable<Integer> getPresetId() {
    return presetId;
  }

  public void setPresetId(JsonNullable<Integer> presetId) {
    this.presetId = presetId;
  }

  public LightingDevice presets(List<@Valid LightingPreset> presets) {
    this.presets = presets;
    return this;
  }

  public LightingDevice addPresetsItem(LightingPreset presetsItem) {
    if (this.presets == null) {
      this.presets = new ArrayList<>();
    }
    this.presets.add(presetsItem);
    return this;
  }

  /**
   * Presets stored on this controller (they differ per controller); empty when offline or when they could not be read.
   * @return presets
   */
  @NotNull @Valid 
  @JsonProperty("presets")
  public List<@Valid LightingPreset> getPresets() {
    return presets;
  }

  public void setPresets(List<@Valid LightingPreset> presets) {
    this.presets = presets;
  }

  public LightingDevice stateReadAt(OffsetDateTime stateReadAt) {
    this.stateReadAt = stateReadAt;
    return this;
  }

  /**
   * When the state was read from the controller (it may be a few seconds old - see the cache note on the list).
   * @return stateReadAt
   */
  @Valid 
  @JsonProperty("stateReadAt")
  public OffsetDateTime getStateReadAt() {
    return stateReadAt;
  }

  public void setStateReadAt(OffsetDateTime stateReadAt) {
    this.stateReadAt = stateReadAt;
  }

  public LightingDevice stateError(String stateError) {
    this.stateError = stateError;
    return this;
  }

  /**
   * Get stateError
   * @return stateError
   */
  
  @JsonProperty("stateError")
  public String getStateError() {
    return stateError;
  }

  public void setStateError(String stateError) {
    this.stateError = stateError;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LightingDevice lightingDevice = (LightingDevice) o;
    return Objects.equals(this.id, lightingDevice.id) &&
        Objects.equals(this.name, lightingDevice.name) &&
        equalsNullable(this.location, lightingDevice.location) &&
        Objects.equals(this.sortOrder, lightingDevice.sortOrder) &&
        Objects.equals(this.online, lightingDevice.online) &&
        Objects.equals(this.isOn, lightingDevice.isOn) &&
        Objects.equals(this.brightness, lightingDevice.brightness) &&
        equalsNullable(this.presetId, lightingDevice.presetId) &&
        Objects.equals(this.presets, lightingDevice.presets) &&
        Objects.equals(this.stateReadAt, lightingDevice.stateReadAt) &&
        Objects.equals(this.stateError, lightingDevice.stateError);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, hashCodeNullable(location), sortOrder, online, isOn, brightness, hashCodeNullable(presetId), presets, stateReadAt, stateError);
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
    sb.append("class LightingDevice {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    online: ").append(toIndentedString(online)).append("\n");
    sb.append("    isOn: ").append(toIndentedString(isOn)).append("\n");
    sb.append("    brightness: ").append(toIndentedString(brightness)).append("\n");
    sb.append("    presetId: ").append(toIndentedString(presetId)).append("\n");
    sb.append("    presets: ").append(toIndentedString(presets)).append("\n");
    sb.append("    stateReadAt: ").append(toIndentedString(stateReadAt)).append("\n");
    sb.append("    stateError: ").append(toIndentedString(stateError)).append("\n");
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

