package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.PendingRoomMove;
import com.sunsetbeach.model.RoomStillHeld;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The booking a room unit is showing on the map right now - either a guest checked in this moment, or, only when the unit isn&#39;t currently occupied, a guest expected to check in today. Never both - a unit shows at most one of these. \&quot;The unit\&quot; is the one the stay is in today, not the booking&#39;s last one: a guest with a move planned for a later night is shown where they are. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PropertyMapCurrentBooking {

  private PendingRoomMove pendingRoomMove;

  private RoomStillHeld roomStillHeld;

  private String bookingId;

  private String guestName;

  private String checkOut;

  private OccupancyStatus occupancyStatus;

  private Boolean owes;

  private String outstandingBalance;

  private Integer overdueDays;

  public PropertyMapCurrentBooking() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PropertyMapCurrentBooking(String bookingId, String guestName, String checkOut, OccupancyStatus occupancyStatus, Boolean owes, String outstandingBalance) {
    this.bookingId = bookingId;
    this.guestName = guestName;
    this.checkOut = checkOut;
    this.occupancyStatus = occupancyStatus;
    this.owes = owes;
    this.outstandingBalance = outstandingBalance;
  }

  public PropertyMapCurrentBooking pendingRoomMove(PendingRoomMove pendingRoomMove) {
    this.pendingRoomMove = pendingRoomMove;
    return this;
  }

  /**
   * Get pendingRoomMove
   * @return pendingRoomMove
   */
  @Valid 
  @JsonProperty("pendingRoomMove")
  public PendingRoomMove getPendingRoomMove() {
    return pendingRoomMove;
  }

  public void setPendingRoomMove(PendingRoomMove pendingRoomMove) {
    this.pendingRoomMove = pendingRoomMove;
  }

  public PropertyMapCurrentBooking roomStillHeld(RoomStillHeld roomStillHeld) {
    this.roomStillHeld = roomStillHeld;
    return this;
  }

  /**
   * Get roomStillHeld
   * @return roomStillHeld
   */
  @Valid 
  @JsonProperty("roomStillHeld")
  public RoomStillHeld getRoomStillHeld() {
    return roomStillHeld;
  }

  public void setRoomStillHeld(RoomStillHeld roomStillHeld) {
    this.roomStillHeld = roomStillHeld;
  }

  public PropertyMapCurrentBooking bookingId(String bookingId) {
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

  public PropertyMapCurrentBooking guestName(String guestName) {
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

  public PropertyMapCurrentBooking checkOut(String checkOut) {
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

  public PropertyMapCurrentBooking occupancyStatus(OccupancyStatus occupancyStatus) {
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

  public PropertyMapCurrentBooking owes(Boolean owes) {
    this.owes = owes;
    return this;
  }

  /**
   * As `Booking.owes` - the balance has fallen due. False for an arrival who is merely unpaid.
   * @return owes
   */
  @NotNull 
  @JsonProperty("owes")
  public Boolean getOwes() {
    return owes;
  }

  public void setOwes(Boolean owes) {
    this.owes = owes;
  }

  public PropertyMapCurrentBooking outstandingBalance(String outstandingBalance) {
    this.outstandingBalance = outstandingBalance;
    return this;
  }

  /**
   * Decimal(10,2) as a string, from `BookingService#computeOutstandingBalance` - the same PAID-aware figure the check-out warning and `TodayBoardEntry` already use, not re-derived. 
   * @return outstandingBalance
   */
  @NotNull 
  @JsonProperty("outstandingBalance")
  public String getOutstandingBalance() {
    return outstandingBalance;
  }

  public void setOutstandingBalance(String outstandingBalance) {
    this.outstandingBalance = outstandingBalance;
  }

  public PropertyMapCurrentBooking overdueDays(Integer overdueDays) {
    this.overdueDays = overdueDays;
    return this;
  }

  /**
   * How many days past `checkOut` a still-`CHECKED_IN` guest is (`0` when not overdue). The one rule for this lives in `OverstayRule` - an overdue guest is still in the house and still occupies their room tonight, on every screen. Same figure the night audit's missed departures show. 
   * @return overdueDays
   */
  
  @JsonProperty("overdueDays")
  public Integer getOverdueDays() {
    return overdueDays;
  }

  public void setOverdueDays(Integer overdueDays) {
    this.overdueDays = overdueDays;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertyMapCurrentBooking propertyMapCurrentBooking = (PropertyMapCurrentBooking) o;
    return Objects.equals(this.pendingRoomMove, propertyMapCurrentBooking.pendingRoomMove) &&
        Objects.equals(this.roomStillHeld, propertyMapCurrentBooking.roomStillHeld) &&
        Objects.equals(this.bookingId, propertyMapCurrentBooking.bookingId) &&
        Objects.equals(this.guestName, propertyMapCurrentBooking.guestName) &&
        Objects.equals(this.checkOut, propertyMapCurrentBooking.checkOut) &&
        Objects.equals(this.occupancyStatus, propertyMapCurrentBooking.occupancyStatus) &&
        Objects.equals(this.owes, propertyMapCurrentBooking.owes) &&
        Objects.equals(this.outstandingBalance, propertyMapCurrentBooking.outstandingBalance) &&
        Objects.equals(this.overdueDays, propertyMapCurrentBooking.overdueDays);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pendingRoomMove, roomStillHeld, bookingId, guestName, checkOut, occupancyStatus, owes, outstandingBalance, overdueDays);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertyMapCurrentBooking {\n");
    sb.append("    pendingRoomMove: ").append(toIndentedString(pendingRoomMove)).append("\n");
    sb.append("    roomStillHeld: ").append(toIndentedString(roomStillHeld)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    occupancyStatus: ").append(toIndentedString(occupancyStatus)).append("\n");
    sb.append("    owes: ").append(toIndentedString(owes)).append("\n");
    sb.append("    outstandingBalance: ").append(toIndentedString(outstandingBalance)).append("\n");
    sb.append("    overdueDays: ").append(toIndentedString(overdueDays)).append("\n");
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

