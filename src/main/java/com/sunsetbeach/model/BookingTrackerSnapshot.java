package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One Friday&#39;s frozen reading of one stay month: written at 23:55 Bangkok by a scheduled job for every month of the current and the next season (Nov-Apr), never recomputed and never taken by hand. A Friday the server missed is written when it is back, as at that moment, and &#x60;late&#x60; is true. &#x60;pickup*&#x60; is the change against the previous snapshot of the same month, null for the first. Only &#x60;reason&#x60;, &#x60;nextAction&#x60;, &#x60;owner&#x60; and &#x60;dueDate&#x60; are editable. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerSnapshot {

  private String id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate snapshotDate;

  private String stayMonth;

  private Boolean late;

  private Integer roomNights;

  private String netRoomRevenue;

  private JsonNullable<String> occupancyPercent = JsonNullable.<String>undefined();

  private JsonNullable<Integer> pickupRoomNights = JsonNullable.<Integer>undefined();

  private JsonNullable<String> pickupNetRevenue = JsonNullable.<String>undefined();

  private JsonNullable<String> reason = JsonNullable.<String>undefined();

  private JsonNullable<String> nextAction = JsonNullable.<String>undefined();

  private JsonNullable<String> owner = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private JsonNullable<LocalDate> dueDate = JsonNullable.<LocalDate>undefined();

  public BookingTrackerSnapshot() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerSnapshot(String id, LocalDate snapshotDate, String stayMonth, Boolean late, Integer roomNights, String netRoomRevenue, String occupancyPercent, Integer pickupRoomNights, String pickupNetRevenue, String reason, String nextAction, String owner, LocalDate dueDate) {
    this.id = id;
    this.snapshotDate = snapshotDate;
    this.stayMonth = stayMonth;
    this.late = late;
    this.roomNights = roomNights;
    this.netRoomRevenue = netRoomRevenue;
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    this.pickupRoomNights = JsonNullable.of(pickupRoomNights);
    this.pickupNetRevenue = JsonNullable.of(pickupNetRevenue);
    this.reason = JsonNullable.of(reason);
    this.nextAction = JsonNullable.of(nextAction);
    this.owner = JsonNullable.of(owner);
    this.dueDate = JsonNullable.of(dueDate);
  }

  public BookingTrackerSnapshot id(String id) {
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

  public BookingTrackerSnapshot snapshotDate(LocalDate snapshotDate) {
    this.snapshotDate = snapshotDate;
    return this;
  }

  /**
   * Get snapshotDate
   * @return snapshotDate
   */
  @NotNull @Valid 
  @JsonProperty("snapshotDate")
  public LocalDate getSnapshotDate() {
    return snapshotDate;
  }

  public void setSnapshotDate(LocalDate snapshotDate) {
    this.snapshotDate = snapshotDate;
  }

  public BookingTrackerSnapshot stayMonth(String stayMonth) {
    this.stayMonth = stayMonth;
    return this;
  }

  /**
   * Get stayMonth
   * @return stayMonth
   */
  @NotNull 
  @JsonProperty("stayMonth")
  public String getStayMonth() {
    return stayMonth;
  }

  public void setStayMonth(String stayMonth) {
    this.stayMonth = stayMonth;
  }

  public BookingTrackerSnapshot late(Boolean late) {
    this.late = late;
    return this;
  }

  /**
   * Get late
   * @return late
   */
  @NotNull 
  @JsonProperty("late")
  public Boolean getLate() {
    return late;
  }

  public void setLate(Boolean late) {
    this.late = late;
  }

  public BookingTrackerSnapshot roomNights(Integer roomNights) {
    this.roomNights = roomNights;
    return this;
  }

  /**
   * Get roomNights
   * @return roomNights
   */
  @NotNull 
  @JsonProperty("roomNights")
  public Integer getRoomNights() {
    return roomNights;
  }

  public void setRoomNights(Integer roomNights) {
    this.roomNights = roomNights;
  }

  public BookingTrackerSnapshot netRoomRevenue(String netRoomRevenue) {
    this.netRoomRevenue = netRoomRevenue;
    return this;
  }

  /**
   * Get netRoomRevenue
   * @return netRoomRevenue
   */
  @NotNull 
  @JsonProperty("netRoomRevenue")
  public String getNetRoomRevenue() {
    return netRoomRevenue;
  }

  public void setNetRoomRevenue(String netRoomRevenue) {
    this.netRoomRevenue = netRoomRevenue;
  }

  public BookingTrackerSnapshot occupancyPercent(String occupancyPercent) {
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    return this;
  }

  /**
   * Get occupancyPercent
   * @return occupancyPercent
   */
  @NotNull 
  @JsonProperty("occupancyPercent")
  public JsonNullable<String> getOccupancyPercent() {
    return occupancyPercent;
  }

  public void setOccupancyPercent(JsonNullable<String> occupancyPercent) {
    this.occupancyPercent = occupancyPercent;
  }

  public BookingTrackerSnapshot pickupRoomNights(Integer pickupRoomNights) {
    this.pickupRoomNights = JsonNullable.of(pickupRoomNights);
    return this;
  }

  /**
   * Get pickupRoomNights
   * @return pickupRoomNights
   */
  @NotNull 
  @JsonProperty("pickupRoomNights")
  public JsonNullable<Integer> getPickupRoomNights() {
    return pickupRoomNights;
  }

  public void setPickupRoomNights(JsonNullable<Integer> pickupRoomNights) {
    this.pickupRoomNights = pickupRoomNights;
  }

  public BookingTrackerSnapshot pickupNetRevenue(String pickupNetRevenue) {
    this.pickupNetRevenue = JsonNullable.of(pickupNetRevenue);
    return this;
  }

  /**
   * Get pickupNetRevenue
   * @return pickupNetRevenue
   */
  @NotNull 
  @JsonProperty("pickupNetRevenue")
  public JsonNullable<String> getPickupNetRevenue() {
    return pickupNetRevenue;
  }

  public void setPickupNetRevenue(JsonNullable<String> pickupNetRevenue) {
    this.pickupNetRevenue = pickupNetRevenue;
  }

  public BookingTrackerSnapshot reason(String reason) {
    this.reason = JsonNullable.of(reason);
    return this;
  }

  /**
   * Reason / issue.
   * @return reason
   */
  @NotNull 
  @JsonProperty("reason")
  public JsonNullable<String> getReason() {
    return reason;
  }

  public void setReason(JsonNullable<String> reason) {
    this.reason = reason;
  }

  public BookingTrackerSnapshot nextAction(String nextAction) {
    this.nextAction = JsonNullable.of(nextAction);
    return this;
  }

  /**
   * Get nextAction
   * @return nextAction
   */
  @NotNull 
  @JsonProperty("nextAction")
  public JsonNullable<String> getNextAction() {
    return nextAction;
  }

  public void setNextAction(JsonNullable<String> nextAction) {
    this.nextAction = nextAction;
  }

  public BookingTrackerSnapshot owner(String owner) {
    this.owner = JsonNullable.of(owner);
    return this;
  }

  /**
   * Get owner
   * @return owner
   */
  @NotNull 
  @JsonProperty("owner")
  public JsonNullable<String> getOwner() {
    return owner;
  }

  public void setOwner(JsonNullable<String> owner) {
    this.owner = owner;
  }

  public BookingTrackerSnapshot dueDate(LocalDate dueDate) {
    this.dueDate = JsonNullable.of(dueDate);
    return this;
  }

  /**
   * Get dueDate
   * @return dueDate
   */
  @NotNull @Valid 
  @JsonProperty("dueDate")
  public JsonNullable<LocalDate> getDueDate() {
    return dueDate;
  }

  public void setDueDate(JsonNullable<LocalDate> dueDate) {
    this.dueDate = dueDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerSnapshot bookingTrackerSnapshot = (BookingTrackerSnapshot) o;
    return Objects.equals(this.id, bookingTrackerSnapshot.id) &&
        Objects.equals(this.snapshotDate, bookingTrackerSnapshot.snapshotDate) &&
        Objects.equals(this.stayMonth, bookingTrackerSnapshot.stayMonth) &&
        Objects.equals(this.late, bookingTrackerSnapshot.late) &&
        Objects.equals(this.roomNights, bookingTrackerSnapshot.roomNights) &&
        Objects.equals(this.netRoomRevenue, bookingTrackerSnapshot.netRoomRevenue) &&
        Objects.equals(this.occupancyPercent, bookingTrackerSnapshot.occupancyPercent) &&
        Objects.equals(this.pickupRoomNights, bookingTrackerSnapshot.pickupRoomNights) &&
        Objects.equals(this.pickupNetRevenue, bookingTrackerSnapshot.pickupNetRevenue) &&
        Objects.equals(this.reason, bookingTrackerSnapshot.reason) &&
        Objects.equals(this.nextAction, bookingTrackerSnapshot.nextAction) &&
        Objects.equals(this.owner, bookingTrackerSnapshot.owner) &&
        Objects.equals(this.dueDate, bookingTrackerSnapshot.dueDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, snapshotDate, stayMonth, late, roomNights, netRoomRevenue, occupancyPercent, pickupRoomNights, pickupNetRevenue, reason, nextAction, owner, dueDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerSnapshot {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    snapshotDate: ").append(toIndentedString(snapshotDate)).append("\n");
    sb.append("    stayMonth: ").append(toIndentedString(stayMonth)).append("\n");
    sb.append("    late: ").append(toIndentedString(late)).append("\n");
    sb.append("    roomNights: ").append(toIndentedString(roomNights)).append("\n");
    sb.append("    netRoomRevenue: ").append(toIndentedString(netRoomRevenue)).append("\n");
    sb.append("    occupancyPercent: ").append(toIndentedString(occupancyPercent)).append("\n");
    sb.append("    pickupRoomNights: ").append(toIndentedString(pickupRoomNights)).append("\n");
    sb.append("    pickupNetRevenue: ").append(toIndentedString(pickupNetRevenue)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    nextAction: ").append(toIndentedString(nextAction)).append("\n");
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
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

