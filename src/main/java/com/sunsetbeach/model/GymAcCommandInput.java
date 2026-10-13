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
 * One air-conditioner setting. Sent whole, also when switching off (&#x60;power&#x60; 0 with the remembered mode/temp/wind).
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymAcCommandInput {

  private Integer power;

  private Integer mode;

  private Integer temp;

  private Integer wind;

  public GymAcCommandInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymAcCommandInput(Integer power, Integer mode, Integer temp, Integer wind) {
    this.power = power;
    this.mode = mode;
    this.temp = temp;
    this.wind = wind;
  }

  public GymAcCommandInput power(Integer power) {
    this.power = power;
    return this;
  }

  /**
   * 0 off, 1 on.
   * minimum: 0
   * maximum: 1
   * @return power
   */
  @NotNull @Min(0) @Max(1) 
  @JsonProperty("power")
  public Integer getPower() {
    return power;
  }

  public void setPower(Integer power) {
    this.power = power;
  }

  public GymAcCommandInput mode(Integer mode) {
    this.mode = mode;
    return this;
  }

  /**
   * 0 cool, 1 heat, 2 auto, 3 fan, 4 dry.
   * minimum: 0
   * maximum: 4
   * @return mode
   */
  @NotNull @Min(0) @Max(4) 
  @JsonProperty("mode")
  public Integer getMode() {
    return mode;
  }

  public void setMode(Integer mode) {
    this.mode = mode;
  }

  public GymAcCommandInput temp(Integer temp) {
    this.temp = temp;
    return this;
  }

  /**
   * Target temperature, degrees Celsius.
   * minimum: 16
   * maximum: 30
   * @return temp
   */
  @NotNull @Min(16) @Max(30) 
  @JsonProperty("temp")
  public Integer getTemp() {
    return temp;
  }

  public void setTemp(Integer temp) {
    this.temp = temp;
  }

  public GymAcCommandInput wind(Integer wind) {
    this.wind = wind;
    return this;
  }

  /**
   * Fan speed - 0 auto, 1 to 3 low to high.
   * minimum: 0
   * maximum: 3
   * @return wind
   */
  @NotNull @Min(0) @Max(3) 
  @JsonProperty("wind")
  public Integer getWind() {
    return wind;
  }

  public void setWind(Integer wind) {
    this.wind = wind;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymAcCommandInput gymAcCommandInput = (GymAcCommandInput) o;
    return Objects.equals(this.power, gymAcCommandInput.power) &&
        Objects.equals(this.mode, gymAcCommandInput.mode) &&
        Objects.equals(this.temp, gymAcCommandInput.temp) &&
        Objects.equals(this.wind, gymAcCommandInput.wind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(power, mode, temp, wind);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymAcCommandInput {\n");
    sb.append("    power: ").append(toIndentedString(power)).append("\n");
    sb.append("    mode: ").append(toIndentedString(mode)).append("\n");
    sb.append("    temp: ").append(toIndentedString(temp)).append("\n");
    sb.append("    wind: ").append(toIndentedString(wind)).append("\n");
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

