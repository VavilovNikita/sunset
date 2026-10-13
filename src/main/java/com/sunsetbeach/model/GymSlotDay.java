package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.GymSlot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * GymSlotDay
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymSlotDay {

  private String date;

  private String bookingId;

  private String openTime;

  private String closeTime;

  private Integer stepMinutes;

  private Integer remainingMinutes;

  @Valid
  private List<@Valid GymSlot> slots = new ArrayList<>();

  public GymSlotDay() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymSlotDay(String date, String bookingId, String openTime, String closeTime, Integer stepMinutes, Integer remainingMinutes, List<@Valid GymSlot> slots) {
    this.date = date;
    this.bookingId = bookingId;
    this.openTime = openTime;
    this.closeTime = closeTime;
    this.stepMinutes = stepMinutes;
    this.remainingMinutes = remainingMinutes;
    this.slots = slots;
  }

  public GymSlotDay date(String date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("date")
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public GymSlotDay bookingId(String bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * Get bookingId
   * @return bookingId
   */
  @NotNull 
  @JsonProperty("bookingId")
  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public GymSlotDay openTime(String openTime) {
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

  public GymSlotDay closeTime(String closeTime) {
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

  public GymSlotDay stepMinutes(Integer stepMinutes) {
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

  public GymSlotDay remainingMinutes(Integer remainingMinutes) {
    this.remainingMinutes = remainingMinutes;
    return this;
  }

  /**
   * What is left of the booking's daily allowance on this date.
   * @return remainingMinutes
   */
  @NotNull 
  @JsonProperty("remainingMinutes")
  public Integer getRemainingMinutes() {
    return remainingMinutes;
  }

  public void setRemainingMinutes(Integer remainingMinutes) {
    this.remainingMinutes = remainingMinutes;
  }

  public GymSlotDay slots(List<@Valid GymSlot> slots) {
    this.slots = slots;
    return this;
  }

  public GymSlotDay addSlotsItem(GymSlot slotsItem) {
    if (this.slots == null) {
      this.slots = new ArrayList<>();
    }
    this.slots.add(slotsItem);
    return this;
  }

  /**
   * Get slots
   * @return slots
   */
  @NotNull @Valid 
  @JsonProperty("slots")
  public List<@Valid GymSlot> getSlots() {
    return slots;
  }

  public void setSlots(List<@Valid GymSlot> slots) {
    this.slots = slots;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymSlotDay gymSlotDay = (GymSlotDay) o;
    return Objects.equals(this.date, gymSlotDay.date) &&
        Objects.equals(this.bookingId, gymSlotDay.bookingId) &&
        Objects.equals(this.openTime, gymSlotDay.openTime) &&
        Objects.equals(this.closeTime, gymSlotDay.closeTime) &&
        Objects.equals(this.stepMinutes, gymSlotDay.stepMinutes) &&
        Objects.equals(this.remainingMinutes, gymSlotDay.remainingMinutes) &&
        Objects.equals(this.slots, gymSlotDay.slots);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, bookingId, openTime, closeTime, stepMinutes, remainingMinutes, slots);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymSlotDay {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    openTime: ").append(toIndentedString(openTime)).append("\n");
    sb.append("    closeTime: ").append(toIndentedString(closeTime)).append("\n");
    sb.append("    stepMinutes: ").append(toIndentedString(stepMinutes)).append("\n");
    sb.append("    remainingMinutes: ").append(toIndentedString(remainingMinutes)).append("\n");
    sb.append("    slots: ").append(toIndentedString(slots)).append("\n");
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

