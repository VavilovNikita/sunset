package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * GymSlot
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymSlot {

  private String startTime;

  /**
   * `DAILY_LIMIT` - the booking's 120 minutes for the day are used up.
   */
  public enum StatusEnum {
    FREE("FREE"),
    
    TAKEN("TAKEN"),
    
    PAST("PAST"),
    
    DAILY_LIMIT("DAILY_LIMIT");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private StatusEnum status;

  private Integer maxDurationMinutes;

  public GymSlot() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymSlot(String startTime, StatusEnum status, Integer maxDurationMinutes) {
    this.startTime = startTime;
    this.status = status;
    this.maxDurationMinutes = maxDurationMinutes;
  }

  public GymSlot startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Get startTime
   * @return startTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("startTime")
  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public GymSlot status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * `DAILY_LIMIT` - the booking's 120 minutes for the day are used up.
   * @return status
   */
  @NotNull 
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public GymSlot maxDurationMinutes(Integer maxDurationMinutes) {
    this.maxDurationMinutes = maxDurationMinutes;
    return this;
  }

  /**
   * The longest session that can start here (0 unless `FREE`), a multiple of 30.
   * @return maxDurationMinutes
   */
  @NotNull 
  @JsonProperty("maxDurationMinutes")
  public Integer getMaxDurationMinutes() {
    return maxDurationMinutes;
  }

  public void setMaxDurationMinutes(Integer maxDurationMinutes) {
    this.maxDurationMinutes = maxDurationMinutes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymSlot gymSlot = (GymSlot) o;
    return Objects.equals(this.startTime, gymSlot.startTime) &&
        Objects.equals(this.status, gymSlot.status) &&
        Objects.equals(this.maxDurationMinutes, gymSlot.maxDurationMinutes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startTime, status, maxDurationMinutes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymSlot {\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    maxDurationMinutes: ").append(toIndentedString(maxDurationMinutes)).append("\n");
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

