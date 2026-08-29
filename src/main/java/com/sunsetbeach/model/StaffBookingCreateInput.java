package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Body of &#x60;POST /bookings/staff&#x60;. Front-desk booking creation (walk-in guest at the counter), distinct from the public &#x60;POST /bookings&#x60; guest inquiry flow: no new-booking notification email is sent (that email exists to tell ADMIN/MANAGER about a guest-submitted request, not every reservation staff makes at the counter), and guest contact details are optional - a walk-in may not have an email address on hand. If &#x60;roomUnitId&#x60; is given, the physical room is assigned atomically in the same &#x60;Serializable&#x60; transaction as the booking itself: unlike &#x60;POST /bookings&#x60; followed by &#x60;PUT /bookings/{id}/room-unit&#x60;, there is no window where the booking exists without the room that was requested for it - either both succeed or neither does. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T21:43:17.277610500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class StaffBookingCreateInput {

  private String roomId;

  private String guestName;

  private JsonNullable<@jakarta.validation.constraints.Email String> guestEmail = JsonNullable.<String>undefined();

  private JsonNullable<@Size(min = 5, max = 40) String> guestPhone = JsonNullable.<String>undefined();

  private String checkIn;

  private String checkOut;

  private JsonNullable<String> roomUnitId = JsonNullable.<String>undefined();

  public StaffBookingCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StaffBookingCreateInput(String roomId, String guestName, String checkIn, String checkOut) {
    this.roomId = roomId;
    this.guestName = guestName;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
  }

  public StaffBookingCreateInput roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * Get roomId
   * @return roomId
   */
  @NotNull @Size(min = 1) 
  @JsonProperty("roomId")
  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public StaffBookingCreateInput guestName(String guestName) {
    this.guestName = guestName;
    return this;
  }

  /**
   * Get guestName
   * @return guestName
   */
  @NotNull @Size(min = 2, max = 120) 
  @JsonProperty("guestName")
  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }

  public StaffBookingCreateInput guestEmail(String guestEmail) {
    this.guestEmail = JsonNullable.of(guestEmail);
    return this;
  }

  /**
   * Get guestEmail
   * @return guestEmail
   */
  @jakarta.validation.constraints.Email 
  @JsonProperty("guestEmail")
  public JsonNullable<@jakarta.validation.constraints.Email String> getGuestEmail() {
    return guestEmail;
  }

  public void setGuestEmail(JsonNullable<String> guestEmail) {
    this.guestEmail = guestEmail;
  }

  public StaffBookingCreateInput guestPhone(String guestPhone) {
    this.guestPhone = JsonNullable.of(guestPhone);
    return this;
  }

  /**
   * Get guestPhone
   * @return guestPhone
   */
  @Size(min = 5, max = 40) 
  @JsonProperty("guestPhone")
  public JsonNullable<@Size(min = 5, max = 40) String> getGuestPhone() {
    return guestPhone;
  }

  public void setGuestPhone(JsonNullable<String> guestPhone) {
    this.guestPhone = guestPhone;
  }

  public StaffBookingCreateInput checkIn(String checkIn) {
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

  public StaffBookingCreateInput checkOut(String checkOut) {
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

  public StaffBookingCreateInput roomUnitId(String roomUnitId) {
    this.roomUnitId = JsonNullable.of(roomUnitId);
    return this;
  }

  /**
   * Physical room to assign immediately, or omit/null to leave the booking unassigned (same as a `POST /bookings` booking today).
   * @return roomUnitId
   */
  
  @JsonProperty("roomUnitId")
  public JsonNullable<String> getRoomUnitId() {
    return roomUnitId;
  }

  public void setRoomUnitId(JsonNullable<String> roomUnitId) {
    this.roomUnitId = roomUnitId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StaffBookingCreateInput staffBookingCreateInput = (StaffBookingCreateInput) o;
    return Objects.equals(this.roomId, staffBookingCreateInput.roomId) &&
        Objects.equals(this.guestName, staffBookingCreateInput.guestName) &&
        equalsNullable(this.guestEmail, staffBookingCreateInput.guestEmail) &&
        equalsNullable(this.guestPhone, staffBookingCreateInput.guestPhone) &&
        Objects.equals(this.checkIn, staffBookingCreateInput.checkIn) &&
        Objects.equals(this.checkOut, staffBookingCreateInput.checkOut) &&
        equalsNullable(this.roomUnitId, staffBookingCreateInput.roomUnitId);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, guestName, hashCodeNullable(guestEmail), hashCodeNullable(guestPhone), checkIn, checkOut, hashCodeNullable(roomUnitId));
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
    sb.append("class StaffBookingCreateInput {\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    // Redacted (hand-edited after codegen, reapply on regeneration) - see Booking.java's toString.
    sb.append("    guestEmail: [REDACTED]\n");
    sb.append("    guestPhone: [REDACTED]\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    roomUnitId: ").append(toIndentedString(roomUnitId)).append("\n");
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

