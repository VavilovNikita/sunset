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
 * The last command this server sent to an air conditioner - not a reading of the unit.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymAcCommand {

  private Integer power;

  private Integer mode;

  private Integer temp;

  private Integer wind;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime sentAt;

  private String sentBy;

  public GymAcCommand() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymAcCommand(Integer power, Integer mode, Integer temp, Integer wind, OffsetDateTime sentAt, String sentBy) {
    this.power = power;
    this.mode = mode;
    this.temp = temp;
    this.wind = wind;
    this.sentAt = sentAt;
    this.sentBy = sentBy;
  }

  public GymAcCommand power(Integer power) {
    this.power = power;
    return this;
  }

  /**
   * Get power
   * @return power
   */
  @NotNull 
  @JsonProperty("power")
  public Integer getPower() {
    return power;
  }

  public void setPower(Integer power) {
    this.power = power;
  }

  public GymAcCommand mode(Integer mode) {
    this.mode = mode;
    return this;
  }

  /**
   * Get mode
   * @return mode
   */
  @NotNull 
  @JsonProperty("mode")
  public Integer getMode() {
    return mode;
  }

  public void setMode(Integer mode) {
    this.mode = mode;
  }

  public GymAcCommand temp(Integer temp) {
    this.temp = temp;
    return this;
  }

  /**
   * Get temp
   * @return temp
   */
  @NotNull 
  @JsonProperty("temp")
  public Integer getTemp() {
    return temp;
  }

  public void setTemp(Integer temp) {
    this.temp = temp;
  }

  public GymAcCommand wind(Integer wind) {
    this.wind = wind;
    return this;
  }

  /**
   * Get wind
   * @return wind
   */
  @NotNull 
  @JsonProperty("wind")
  public Integer getWind() {
    return wind;
  }

  public void setWind(Integer wind) {
    this.wind = wind;
  }

  public GymAcCommand sentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
    return this;
  }

  /**
   * Get sentAt
   * @return sentAt
   */
  @NotNull @Valid 
  @JsonProperty("sentAt")
  public OffsetDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public GymAcCommand sentBy(String sentBy) {
    this.sentBy = sentBy;
    return this;
  }

  /**
   * Name (or email) of the staff member at the time.
   * @return sentBy
   */
  @NotNull 
  @JsonProperty("sentBy")
  public String getSentBy() {
    return sentBy;
  }

  public void setSentBy(String sentBy) {
    this.sentBy = sentBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymAcCommand gymAcCommand = (GymAcCommand) o;
    return Objects.equals(this.power, gymAcCommand.power) &&
        Objects.equals(this.mode, gymAcCommand.mode) &&
        Objects.equals(this.temp, gymAcCommand.temp) &&
        Objects.equals(this.wind, gymAcCommand.wind) &&
        Objects.equals(this.sentAt, gymAcCommand.sentAt) &&
        Objects.equals(this.sentBy, gymAcCommand.sentBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(power, mode, temp, wind, sentAt, sentBy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymAcCommand {\n");
    sb.append("    power: ").append(toIndentedString(power)).append("\n");
    sb.append("    mode: ").append(toIndentedString(mode)).append("\n");
    sb.append("    temp: ").append(toIndentedString(temp)).append("\n");
    sb.append("    wind: ").append(toIndentedString(wind)).append("\n");
    sb.append("    sentAt: ").append(toIndentedString(sentAt)).append("\n");
    sb.append("    sentBy: ").append(toIndentedString(sentBy)).append("\n");
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

