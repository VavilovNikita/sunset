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
 * Body of &#x60;bookingCreateSchema&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingCreateInput {

  private String roomId;

  private String guestName;

  private String guestEmail;

  private String guestPhone;

  private String checkIn;

  private String checkOut;

  private Integer adults;

  private Integer children = 0;

  public BookingCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingCreateInput(String roomId, String guestName, String guestEmail, String guestPhone, String checkIn, String checkOut, Integer adults) {
    this.roomId = roomId;
    this.guestName = guestName;
    this.guestEmail = guestEmail;
    this.guestPhone = guestPhone;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.adults = adults;
  }

  public BookingCreateInput roomId(String roomId) {
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

  public BookingCreateInput guestName(String guestName) {
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

  public BookingCreateInput guestEmail(String guestEmail) {
    this.guestEmail = guestEmail;
    return this;
  }

  /**
   * Get guestEmail
   * @return guestEmail
   */
  @NotNull @jakarta.validation.constraints.Email(message = "Invalid email")
  @JsonProperty("guestEmail")
  public String getGuestEmail() {
    return guestEmail;
  }

  public void setGuestEmail(String guestEmail) {
    this.guestEmail = guestEmail;
  }

  public BookingCreateInput guestPhone(String guestPhone) {
    this.guestPhone = guestPhone;
    return this;
  }

  /**
   * Get guestPhone
   * @return guestPhone
   */
  @NotNull @Size(min = 5, max = 40) 
  @JsonProperty("guestPhone")
  public String getGuestPhone() {
    return guestPhone;
  }

  public void setGuestPhone(String guestPhone) {
    this.guestPhone = guestPhone;
  }

  public BookingCreateInput checkIn(String checkIn) {
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

  public BookingCreateInput checkOut(String checkOut) {
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

  public BookingCreateInput adults(Integer adults) {
    this.adults = adults;
    return this;
  }

  /**
   * Number of adults in the party - at least one. The guest knows their own party size, so unlike `channel`/`purpose` this is collected on the public form too.
   * minimum: 1
   * @return adults
   */
  @NotNull @Min(1) 
  @JsonProperty("adults")
  public Integer getAdults() {
    return adults;
  }

  public void setAdults(Integer adults) {
    this.adults = adults;
  }

  public BookingCreateInput children(Integer children) {
    this.children = children;
    return this;
  }

  /**
   * Number of children in the party. Optional, defaults to 0.
   * minimum: 0
   * @return children
   */
  @Min(0) 
  @JsonProperty("children")
  public Integer getChildren() {
    return children;
  }

  public void setChildren(Integer children) {
    this.children = children;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingCreateInput bookingCreateInput = (BookingCreateInput) o;
    return Objects.equals(this.roomId, bookingCreateInput.roomId) &&
        Objects.equals(this.guestName, bookingCreateInput.guestName) &&
        Objects.equals(this.guestEmail, bookingCreateInput.guestEmail) &&
        Objects.equals(this.guestPhone, bookingCreateInput.guestPhone) &&
        Objects.equals(this.checkIn, bookingCreateInput.checkIn) &&
        Objects.equals(this.checkOut, bookingCreateInput.checkOut) &&
        Objects.equals(this.adults, bookingCreateInput.adults) &&
        Objects.equals(this.children, bookingCreateInput.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, guestName, guestEmail, guestPhone, checkIn, checkOut, adults, children);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingCreateInput {\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    guestEmail: ").append("[REDACTED]").append("\n");
    sb.append("    guestPhone: ").append("[REDACTED]").append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    adults: ").append(toIndentedString(adults)).append("\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
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

