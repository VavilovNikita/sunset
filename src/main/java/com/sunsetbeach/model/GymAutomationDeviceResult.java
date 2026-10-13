package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * GymAutomationDeviceResult
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymAutomationDeviceResult {

  private String deviceName;

  /**
   * `PENDING` - still being tried (a failed attempt is retried on the next run, up to the attempt limit).
   */
  public enum StatusEnum {
    PENDING("PENDING"),
    
    OK("OK"),
    
    FAILED("FAILED"),
    
    SKIPPED("SKIPPED");

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

  /**
   * Why nothing was sent: the previous reservation had already switched it on (`COVERED_BY_PREVIOUS`), another reservation starts within the lead time so it stays on (`NEXT_BOOKING_SOON`), the reservation was over before the command was due (`MISSED`), the reservation was cancelled before switch-on (`CANCELLED`), the device was removed from settings (`DEVICE_REMOVED`). 
   */
  public enum SkipReasonEnum {
    COVERED_BY_PREVIOUS("COVERED_BY_PREVIOUS"),
    
    NEXT_BOOKING_SOON("NEXT_BOOKING_SOON"),
    
    MISSED("MISSED"),
    
    CANCELLED("CANCELLED"),
    
    DEVICE_REMOVED("DEVICE_REMOVED");

    private String value;

    SkipReasonEnum(String value) {
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
    public static SkipReasonEnum fromValue(String value) {
      for (SkipReasonEnum b : SkipReasonEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      return null;
    }
  }

  private JsonNullable<SkipReasonEnum> skipReason = JsonNullable.<SkipReasonEnum>undefined();

  private JsonNullable<String> error = JsonNullable.<String>undefined();

  private Integer attempts;

  public GymAutomationDeviceResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymAutomationDeviceResult(String deviceName, StatusEnum status, Integer attempts) {
    this.deviceName = deviceName;
    this.status = status;
    this.attempts = attempts;
  }

  public GymAutomationDeviceResult deviceName(String deviceName) {
    this.deviceName = deviceName;
    return this;
  }

  /**
   * Get deviceName
   * @return deviceName
   */
  @NotNull 
  @JsonProperty("deviceName")
  public String getDeviceName() {
    return deviceName;
  }

  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName;
  }

  public GymAutomationDeviceResult status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * `PENDING` - still being tried (a failed attempt is retried on the next run, up to the attempt limit).
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

  public GymAutomationDeviceResult skipReason(SkipReasonEnum skipReason) {
    this.skipReason = JsonNullable.of(skipReason);
    return this;
  }

  /**
   * Why nothing was sent: the previous reservation had already switched it on (`COVERED_BY_PREVIOUS`), another reservation starts within the lead time so it stays on (`NEXT_BOOKING_SOON`), the reservation was over before the command was due (`MISSED`), the reservation was cancelled before switch-on (`CANCELLED`), the device was removed from settings (`DEVICE_REMOVED`). 
   * @return skipReason
   */
  
  @JsonProperty("skipReason")
  public JsonNullable<SkipReasonEnum> getSkipReason() {
    return skipReason;
  }

  public void setSkipReason(JsonNullable<SkipReasonEnum> skipReason) {
    this.skipReason = skipReason;
  }

  public GymAutomationDeviceResult error(String error) {
    this.error = JsonNullable.of(error);
    return this;
  }

  /**
   * Tuya's failure in plain words, for a `FAILED` (or retrying) device.
   * @return error
   */
  
  @JsonProperty("error")
  public JsonNullable<String> getError() {
    return error;
  }

  public void setError(JsonNullable<String> error) {
    this.error = error;
  }

  public GymAutomationDeviceResult attempts(Integer attempts) {
    this.attempts = attempts;
    return this;
  }

  /**
   * Get attempts
   * @return attempts
   */
  @NotNull 
  @JsonProperty("attempts")
  public Integer getAttempts() {
    return attempts;
  }

  public void setAttempts(Integer attempts) {
    this.attempts = attempts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymAutomationDeviceResult gymAutomationDeviceResult = (GymAutomationDeviceResult) o;
    return Objects.equals(this.deviceName, gymAutomationDeviceResult.deviceName) &&
        Objects.equals(this.status, gymAutomationDeviceResult.status) &&
        equalsNullable(this.skipReason, gymAutomationDeviceResult.skipReason) &&
        equalsNullable(this.error, gymAutomationDeviceResult.error) &&
        Objects.equals(this.attempts, gymAutomationDeviceResult.attempts);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(deviceName, status, hashCodeNullable(skipReason), hashCodeNullable(error), attempts);
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
    sb.append("class GymAutomationDeviceResult {\n");
    sb.append("    deviceName: ").append(toIndentedString(deviceName)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    skipReason: ").append(toIndentedString(skipReason)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    attempts: ").append(toIndentedString(attempts)).append("\n");
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

