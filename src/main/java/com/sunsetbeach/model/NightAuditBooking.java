package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One booking on a night-audit list - just enough to recognise it and open it.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NightAuditBooking {

  private String id;

  private String guestName;

  private String roomName;

  private JsonNullable<String> roomUnitLabel = JsonNullable.<String>undefined();

  private String checkIn;

  private String checkOut;

  private BookingStatus status;

  private OccupancyStatus occupancyStatus;

  public NightAuditBooking() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NightAuditBooking(String id, String guestName, String roomName, String roomUnitLabel, String checkIn, String checkOut, BookingStatus status, OccupancyStatus occupancyStatus) {
    this.id = id;
    this.guestName = guestName;
    this.roomName = roomName;
    this.roomUnitLabel = JsonNullable.of(roomUnitLabel);
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.status = status;
    this.occupancyStatus = occupancyStatus;
  }

  public NightAuditBooking id(String id) {
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

  public NightAuditBooking guestName(String guestName) {
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

  public NightAuditBooking roomName(String roomName) {
    this.roomName = roomName;
    return this;
  }

  /**
   * The room type's name.
   * @return roomName
   */
  @NotNull 
  @JsonProperty("roomName")
  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public NightAuditBooking roomUnitLabel(String roomUnitLabel) {
    this.roomUnitLabel = JsonNullable.of(roomUnitLabel);
    return this;
  }

  /**
   * The assigned physical room's label, or null if none is assigned.
   * @return roomUnitLabel
   */
  @NotNull 
  @JsonProperty("roomUnitLabel")
  public JsonNullable<String> getRoomUnitLabel() {
    return roomUnitLabel;
  }

  public void setRoomUnitLabel(JsonNullable<String> roomUnitLabel) {
    this.roomUnitLabel = roomUnitLabel;
  }

  public NightAuditBooking checkIn(String checkIn) {
    this.checkIn = checkIn;
    return this;
  }

  /**
   * Get checkIn
   * @return checkIn
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("checkIn")
  public String getCheckIn() {
    return checkIn;
  }

  public void setCheckIn(String checkIn) {
    this.checkIn = checkIn;
  }

  public NightAuditBooking checkOut(String checkOut) {
    this.checkOut = checkOut;
    return this;
  }

  /**
   * Get checkOut
   * @return checkOut
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("checkOut")
  public String getCheckOut() {
    return checkOut;
  }

  public void setCheckOut(String checkOut) {
    this.checkOut = checkOut;
  }

  public NightAuditBooking status(BookingStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public BookingStatus getStatus() {
    return status;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }

  public NightAuditBooking occupancyStatus(OccupancyStatus occupancyStatus) {
    this.occupancyStatus = occupancyStatus;
    return this;
  }

  /**
   * Get occupancyStatus
   * @return occupancyStatus
   */
  @NotNull @Valid 
  @JsonProperty("occupancyStatus")
  public OccupancyStatus getOccupancyStatus() {
    return occupancyStatus;
  }

  public void setOccupancyStatus(OccupancyStatus occupancyStatus) {
    this.occupancyStatus = occupancyStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NightAuditBooking nightAuditBooking = (NightAuditBooking) o;
    return Objects.equals(this.id, nightAuditBooking.id) &&
        Objects.equals(this.guestName, nightAuditBooking.guestName) &&
        Objects.equals(this.roomName, nightAuditBooking.roomName) &&
        Objects.equals(this.roomUnitLabel, nightAuditBooking.roomUnitLabel) &&
        Objects.equals(this.checkIn, nightAuditBooking.checkIn) &&
        Objects.equals(this.checkOut, nightAuditBooking.checkOut) &&
        Objects.equals(this.status, nightAuditBooking.status) &&
        Objects.equals(this.occupancyStatus, nightAuditBooking.occupancyStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, guestName, roomName, roomUnitLabel, checkIn, checkOut, status, occupancyStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NightAuditBooking {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    guestName: ").append("[REDACTED]").append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
    sb.append("    roomUnitLabel: ").append(toIndentedString(roomUnitLabel)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    occupancyStatus: ").append(toIndentedString(occupancyStatus)).append("\n");
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

