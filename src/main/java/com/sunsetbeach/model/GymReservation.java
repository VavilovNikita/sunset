package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.GymAutomation;
import com.sunsetbeach.model.GymReservationStatus;
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
 * A gym reservation as staff see it.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymReservation {

  private String id;

  private String bookingId;

  private String guestName;

  private JsonNullable<String> roomLabel = JsonNullable.<String>undefined();

  private String date;

  private String startTime;

  private String endTime;

  private Integer durationMinutes;

  private GymReservationStatus status;

  /**
   * Gets or Sets createdByType
   */
  public enum CreatedByTypeEnum {
    GUEST("GUEST"),
    
    STAFF("STAFF");

    private String value;

    CreatedByTypeEnum(String value) {
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
    public static CreatedByTypeEnum fromValue(String value) {
      for (CreatedByTypeEnum b : CreatedByTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private CreatedByTypeEnum createdByType;

  private String createdByName;

  private JsonNullable<String> cancelledBy = JsonNullable.<String>undefined();

  private JsonNullable<String> cancelReason = JsonNullable.<String>undefined();

  private GymAutomation automation;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public GymReservation() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymReservation(String id, String bookingId, String guestName, String date, String startTime, String endTime, Integer durationMinutes, GymReservationStatus status, CreatedByTypeEnum createdByType, String createdByName, GymAutomation automation, OffsetDateTime createdAt) {
    this.id = id;
    this.bookingId = bookingId;
    this.guestName = guestName;
    this.date = date;
    this.startTime = startTime;
    this.endTime = endTime;
    this.durationMinutes = durationMinutes;
    this.status = status;
    this.createdByType = createdByType;
    this.createdByName = createdByName;
    this.automation = automation;
    this.createdAt = createdAt;
  }

  public GymReservation id(String id) {
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

  public GymReservation bookingId(String bookingId) {
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

  public GymReservation guestName(String guestName) {
    this.guestName = guestName;
    return this;
  }

  /**
   * Get guestName
   * @return guestName
   */
  @NotNull 
  @JsonProperty("guestName")
  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }

  public GymReservation roomLabel(String roomLabel) {
    this.roomLabel = JsonNullable.of(roomLabel);
    return this;
  }

  /**
   * Get roomLabel
   * @return roomLabel
   */
  
  @JsonProperty("roomLabel")
  public JsonNullable<String> getRoomLabel() {
    return roomLabel;
  }

  public void setRoomLabel(JsonNullable<String> roomLabel) {
    this.roomLabel = roomLabel;
  }

  public GymReservation date(String date) {
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

  public GymReservation startTime(String startTime) {
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

  public GymReservation endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Get endTime
   * @return endTime
   */
  @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("endTime")
  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public GymReservation durationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Get durationMinutes
   * @return durationMinutes
   */
  @NotNull 
  @JsonProperty("durationMinutes")
  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public GymReservation status(GymReservationStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public GymReservationStatus getStatus() {
    return status;
  }

  public void setStatus(GymReservationStatus status) {
    this.status = status;
  }

  public GymReservation createdByType(CreatedByTypeEnum createdByType) {
    this.createdByType = createdByType;
    return this;
  }

  /**
   * Get createdByType
   * @return createdByType
   */
  @NotNull 
  @JsonProperty("createdByType")
  public CreatedByTypeEnum getCreatedByType() {
    return createdByType;
  }

  public void setCreatedByType(CreatedByTypeEnum createdByType) {
    this.createdByType = createdByType;
  }

  public GymReservation createdByName(String createdByName) {
    this.createdByName = createdByName;
    return this;
  }

  /**
   * Get createdByName
   * @return createdByName
   */
  @NotNull 
  @JsonProperty("createdByName")
  public String getCreatedByName() {
    return createdByName;
  }

  public void setCreatedByName(String createdByName) {
    this.createdByName = createdByName;
  }

  public GymReservation cancelledBy(String cancelledBy) {
    this.cancelledBy = JsonNullable.of(cancelledBy);
    return this;
  }

  /**
   * Get cancelledBy
   * @return cancelledBy
   */
  
  @JsonProperty("cancelledBy")
  public JsonNullable<String> getCancelledBy() {
    return cancelledBy;
  }

  public void setCancelledBy(JsonNullable<String> cancelledBy) {
    this.cancelledBy = cancelledBy;
  }

  public GymReservation cancelReason(String cancelReason) {
    this.cancelReason = JsonNullable.of(cancelReason);
    return this;
  }

  /**
   * Get cancelReason
   * @return cancelReason
   */
  
  @JsonProperty("cancelReason")
  public JsonNullable<String> getCancelReason() {
    return cancelReason;
  }

  public void setCancelReason(JsonNullable<String> cancelReason) {
    this.cancelReason = cancelReason;
  }

  public GymReservation automation(GymAutomation automation) {
    this.automation = automation;
    return this;
  }

  /**
   * Get automation
   * @return automation
   */
  @NotNull @Valid 
  @JsonProperty("automation")
  public GymAutomation getAutomation() {
    return automation;
  }

  public void setAutomation(GymAutomation automation) {
    this.automation = automation;
  }

  public GymReservation createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymReservation gymReservation = (GymReservation) o;
    return Objects.equals(this.id, gymReservation.id) &&
        Objects.equals(this.bookingId, gymReservation.bookingId) &&
        Objects.equals(this.guestName, gymReservation.guestName) &&
        equalsNullable(this.roomLabel, gymReservation.roomLabel) &&
        Objects.equals(this.date, gymReservation.date) &&
        Objects.equals(this.startTime, gymReservation.startTime) &&
        Objects.equals(this.endTime, gymReservation.endTime) &&
        Objects.equals(this.durationMinutes, gymReservation.durationMinutes) &&
        Objects.equals(this.status, gymReservation.status) &&
        Objects.equals(this.createdByType, gymReservation.createdByType) &&
        Objects.equals(this.createdByName, gymReservation.createdByName) &&
        equalsNullable(this.cancelledBy, gymReservation.cancelledBy) &&
        equalsNullable(this.cancelReason, gymReservation.cancelReason) &&
        Objects.equals(this.automation, gymReservation.automation) &&
        Objects.equals(this.createdAt, gymReservation.createdAt);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, bookingId, guestName, hashCodeNullable(roomLabel), date, startTime, endTime, durationMinutes, status, createdByType, createdByName, hashCodeNullable(cancelledBy), hashCodeNullable(cancelReason), automation, createdAt);
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
    sb.append("class GymReservation {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    roomLabel: ").append(toIndentedString(roomLabel)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    createdByType: ").append(toIndentedString(createdByType)).append("\n");
    sb.append("    createdByName: ").append(toIndentedString(createdByName)).append("\n");
    sb.append("    cancelledBy: ").append(toIndentedString(cancelledBy)).append("\n");
    sb.append("    cancelReason: ").append(toIndentedString(cancelReason)).append("\n");
    sb.append("    automation: ").append(toIndentedString(automation)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

