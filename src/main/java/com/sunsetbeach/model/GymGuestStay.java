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
 * GymGuestStay
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymGuestStay {

  private String bookingId;

  private JsonNullable<String> roomLabel = JsonNullable.<String>undefined();

  private String checkIn;

  private String checkOut;

  public GymGuestStay() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymGuestStay(String bookingId, String roomLabel, String checkIn, String checkOut) {
    this.bookingId = bookingId;
    this.roomLabel = JsonNullable.of(roomLabel);
    this.checkIn = checkIn;
    this.checkOut = checkOut;
  }

  public GymGuestStay bookingId(String bookingId) {
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

  public GymGuestStay roomLabel(String roomLabel) {
    this.roomLabel = JsonNullable.of(roomLabel);
    return this;
  }

  /**
   * Get roomLabel
   * @return roomLabel
   */
  @NotNull 
  @JsonProperty("roomLabel")
  public JsonNullable<String> getRoomLabel() {
    return roomLabel;
  }

  public void setRoomLabel(JsonNullable<String> roomLabel) {
    this.roomLabel = roomLabel;
  }

  public GymGuestStay checkIn(String checkIn) {
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

  public GymGuestStay checkOut(String checkOut) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymGuestStay gymGuestStay = (GymGuestStay) o;
    return Objects.equals(this.bookingId, gymGuestStay.bookingId) &&
        Objects.equals(this.roomLabel, gymGuestStay.roomLabel) &&
        Objects.equals(this.checkIn, gymGuestStay.checkIn) &&
        Objects.equals(this.checkOut, gymGuestStay.checkOut);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, roomLabel, checkIn, checkOut);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymGuestStay {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    roomLabel: ").append(toIndentedString(roomLabel)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
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

