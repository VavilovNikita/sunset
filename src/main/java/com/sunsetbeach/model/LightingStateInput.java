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
 * At least one field.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LightingStateInput {

  private Boolean isOn;

  private Integer brightness;

  private Integer presetId;

  public LightingStateInput isOn(Boolean isOn) {
    this.isOn = isOn;
    return this;
  }

  /**
   * Get isOn
   * @return isOn
   */
  
  @JsonProperty("isOn")
  public Boolean getIsOn() {
    return isOn;
  }

  public void setIsOn(Boolean isOn) {
    this.isOn = isOn;
  }

  public LightingStateInput brightness(Integer brightness) {
    this.brightness = brightness;
    return this;
  }

  /**
   * Get brightness
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

  public LightingStateInput presetId(Integer presetId) {
    this.presetId = presetId;
    return this;
  }

  /**
   * Get presetId
   * minimum: 1
   * maximum: 250
   * @return presetId
   */
  @Min(1) @Max(250) 
  @JsonProperty("presetId")
  public Integer getPresetId() {
    return presetId;
  }

  public void setPresetId(Integer presetId) {
    this.presetId = presetId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LightingStateInput lightingStateInput = (LightingStateInput) o;
    return Objects.equals(this.isOn, lightingStateInput.isOn) &&
        Objects.equals(this.brightness, lightingStateInput.brightness) &&
        Objects.equals(this.presetId, lightingStateInput.presetId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isOn, brightness, presetId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LightingStateInput {\n");
    sb.append("    isOn: ").append(toIndentedString(isOn)).append("\n");
    sb.append("    brightness: ").append(toIndentedString(brightness)).append("\n");
    sb.append("    presetId: ").append(toIndentedString(presetId)).append("\n");
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

