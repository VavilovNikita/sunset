package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.GymGuestStay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * GymGuestInfo
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymGuestInfo {

  private Boolean eligible;

  private JsonNullable<String> message = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid GymGuestStay> stays = new ArrayList<>();

  private String openTime;

  private String closeTime;

  private Integer stepMinutes;

  private Integer minDurationMinutes;

  private Integer maxDurationMinutes;

  private Integer maxDailyMinutes;

  public GymGuestInfo() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymGuestInfo(Boolean eligible, List<@Valid GymGuestStay> stays, String openTime, String closeTime, Integer stepMinutes, Integer minDurationMinutes, Integer maxDurationMinutes, Integer maxDailyMinutes) {
    this.eligible = eligible;
    this.stays = stays;
    this.openTime = openTime;
    this.closeTime = closeTime;
    this.stepMinutes = stepMinutes;
    this.minDurationMinutes = minDurationMinutes;
    this.maxDurationMinutes = maxDurationMinutes;
    this.maxDailyMinutes = maxDailyMinutes;
  }

  public GymGuestInfo eligible(Boolean eligible) {
    this.eligible = eligible;
    return this;
  }

  /**
   * Get eligible
   * @return eligible
   */
  @NotNull 
  @JsonProperty("eligible")
  public Boolean getEligible() {
    return eligible;
  }

  public void setEligible(Boolean eligible) {
    this.eligible = eligible;
  }

  public GymGuestInfo message(String message) {
    this.message = JsonNullable.of(message);
    return this;
  }

  /**
   * Why the guest cannot book, in plain words; null when eligible.
   * @return message
   */
  
  @JsonProperty("message")
  public JsonNullable<String> getMessage() {
    return message;
  }

  public void setMessage(JsonNullable<String> message) {
    this.message = message;
  }

  public GymGuestInfo stays(List<@Valid GymGuestStay> stays) {
    this.stays = stays;
    return this;
  }

  public GymGuestInfo addStaysItem(GymGuestStay staysItem) {
    if (this.stays == null) {
      this.stays = new ArrayList<>();
    }
    this.stays.add(staysItem);
    return this;
  }

  /**
   * Get stays
   * @return stays
   */
  @NotNull @Valid 
  @JsonProperty("stays")
  public List<@Valid GymGuestStay> getStays() {
    return stays;
  }

  public void setStays(List<@Valid GymGuestStay> stays) {
    this.stays = stays;
  }

  public GymGuestInfo openTime(String openTime) {
    this.openTime = openTime;
    return this;
  }

  /**
   * Get openTime
   * @return openTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("openTime")
  public String getOpenTime() {
    return openTime;
  }

  public void setOpenTime(String openTime) {
    this.openTime = openTime;
  }

  public GymGuestInfo closeTime(String closeTime) {
    this.closeTime = closeTime;
    return this;
  }

  /**
   * Get closeTime
   * @return closeTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("closeTime")
  public String getCloseTime() {
    return closeTime;
  }

  public void setCloseTime(String closeTime) {
    this.closeTime = closeTime;
  }

  public GymGuestInfo stepMinutes(Integer stepMinutes) {
    this.stepMinutes = stepMinutes;
    return this;
  }

  /**
   * Get stepMinutes
   * @return stepMinutes
   */
  @NotNull 
  @JsonProperty("stepMinutes")
  public Integer getStepMinutes() {
    return stepMinutes;
  }

  public void setStepMinutes(Integer stepMinutes) {
    this.stepMinutes = stepMinutes;
  }

  public GymGuestInfo minDurationMinutes(Integer minDurationMinutes) {
    this.minDurationMinutes = minDurationMinutes;
    return this;
  }

  /**
   * Get minDurationMinutes
   * @return minDurationMinutes
   */
  @NotNull 
  @JsonProperty("minDurationMinutes")
  public Integer getMinDurationMinutes() {
    return minDurationMinutes;
  }

  public void setMinDurationMinutes(Integer minDurationMinutes) {
    this.minDurationMinutes = minDurationMinutes;
  }

  public GymGuestInfo maxDurationMinutes(Integer maxDurationMinutes) {
    this.maxDurationMinutes = maxDurationMinutes;
    return this;
  }

  /**
   * Get maxDurationMinutes
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

  public GymGuestInfo maxDailyMinutes(Integer maxDailyMinutes) {
    this.maxDailyMinutes = maxDailyMinutes;
    return this;
  }

  /**
   * Get maxDailyMinutes
   * @return maxDailyMinutes
   */
  @NotNull 
  @JsonProperty("maxDailyMinutes")
  public Integer getMaxDailyMinutes() {
    return maxDailyMinutes;
  }

  public void setMaxDailyMinutes(Integer maxDailyMinutes) {
    this.maxDailyMinutes = maxDailyMinutes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymGuestInfo gymGuestInfo = (GymGuestInfo) o;
    return Objects.equals(this.eligible, gymGuestInfo.eligible) &&
        equalsNullable(this.message, gymGuestInfo.message) &&
        Objects.equals(this.stays, gymGuestInfo.stays) &&
        Objects.equals(this.openTime, gymGuestInfo.openTime) &&
        Objects.equals(this.closeTime, gymGuestInfo.closeTime) &&
        Objects.equals(this.stepMinutes, gymGuestInfo.stepMinutes) &&
        Objects.equals(this.minDurationMinutes, gymGuestInfo.minDurationMinutes) &&
        Objects.equals(this.maxDurationMinutes, gymGuestInfo.maxDurationMinutes) &&
        Objects.equals(this.maxDailyMinutes, gymGuestInfo.maxDailyMinutes);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(eligible, hashCodeNullable(message), stays, openTime, closeTime, stepMinutes, minDurationMinutes, maxDurationMinutes, maxDailyMinutes);
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
    sb.append("class GymGuestInfo {\n");
    sb.append("    eligible: ").append(toIndentedString(eligible)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    stays: ").append(toIndentedString(stays)).append("\n");
    sb.append("    openTime: ").append(toIndentedString(openTime)).append("\n");
    sb.append("    closeTime: ").append(toIndentedString(closeTime)).append("\n");
    sb.append("    stepMinutes: ").append(toIndentedString(stepMinutes)).append("\n");
    sb.append("    minDurationMinutes: ").append(toIndentedString(minDurationMinutes)).append("\n");
    sb.append("    maxDurationMinutes: ").append(toIndentedString(maxDurationMinutes)).append("\n");
    sb.append("    maxDailyMinutes: ").append(toIndentedString(maxDailyMinutes)).append("\n");
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

