package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.SpaAppointmentStatus;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A half-hour-grid treatment slot - occupies both a POS &#x60;Table&#x60; (in the SPA zone) and a therapist (a staff &#x60;User&#x60; holding the &#x60;THERAPIST&#x60; job function) for &#x60;[startTime, startTime + durationMinutes)&#x60; on &#x60;date&#x60;. Always names a booking - there is no walk-in path and no separate client record (hotel guests only); &#x60;guestName&#x60; is resolved from that booking at read time, not stored here. &#x60;durationMinutes&#x60; is copied from &#x60;MenuItem.durationMinutes&#x60; once, at creation - the appointment&#39;s own frozen record of how long the treatment takes, immune to the menu item being re-timed later (same \&quot;agreed terms are frozen\&quot; precedent as &#x60;BookingSegmentNightlyRate&#x60;). The appointment&#39;s end is never stored - it&#39;s always &#x60;startTime + durationMinutes&#x60;, computed wherever needed (including by the two &#x60;EXCLUDE USING gist&#x60; constraints on the &#x60;SpaAppointment&#x60; table, one keyed on &#x60;tableId&#x60; and one on &#x60;therapistUserId&#x60;, each restricted to &#x60;status IN (&#39;BOOKED&#39;, &#39;COMPLETED&#39;)&#x60; rows - see &#x60;V41__spa_appointment.sql&#x60; and &#x60;V43__spa_appointment_completed_still_occupies_slot.sql&#x60;). &#x60;date&#x60;/&#x60;startTime&#x60; are a plain date-only string and a plain local &#x60;HH:mm&#x60; string - the hotel is a single location and nothing in this API converts time zones, so there is deliberately no offset anywhere on this schema. The appointment carries no money - see &#x60;orderId&#x60;. Cancelling/shortening/early-checking- out the named booking does not cascade to this row automatically (nothing in this system cascades booking-date changes to anything else yet); those write paths instead return a warning listing affected future appointments, the same warn-don&#39;t-block shape as &#x60;RoomUnitBlockResult&#x60;. The same applies to a therapist losing the &#x60;THERAPIST&#x60; function or being deactivated while holding future &#x60;BOOKED&#x60; appointments - see &#x60;PATCH /users/{id}/active&#x60; / &#x60;PATCH /users/{id}/functions&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaAppointment {

  private String id;

  private String bookingId;

  private String guestName;

  private String tableId;

  private String tableLabel;

  private String therapistUserId;

  private String therapistEmail;

  private String treatmentMenuItemId;

  private String treatmentName;

  private String date;

  private String startTime;

  private Integer durationMinutes;

  private SpaAppointmentStatus status;

  private JsonNullable<String> orderId = JsonNullable.<String>undefined();

  private String createdByUserId;

  private JsonNullable<String> cancelledByUserId = JsonNullable.<String>undefined();

  private JsonNullable<String> cancelReason = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public SpaAppointment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaAppointment(String id, String bookingId, String guestName, String tableId, String tableLabel, String therapistUserId, String therapistEmail, String treatmentMenuItemId, String treatmentName, String date, String startTime, Integer durationMinutes, SpaAppointmentStatus status, String orderId, String createdByUserId, String cancelledByUserId, String cancelReason, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.bookingId = bookingId;
    this.guestName = guestName;
    this.tableId = tableId;
    this.tableLabel = tableLabel;
    this.therapistUserId = therapistUserId;
    this.therapistEmail = therapistEmail;
    this.treatmentMenuItemId = treatmentMenuItemId;
    this.treatmentName = treatmentName;
    this.date = date;
    this.startTime = startTime;
    this.durationMinutes = durationMinutes;
    this.status = status;
    this.orderId = JsonNullable.of(orderId);
    this.createdByUserId = createdByUserId;
    this.cancelledByUserId = JsonNullable.of(cancelledByUserId);
    this.cancelReason = JsonNullable.of(cancelReason);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public SpaAppointment id(String id) {
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

  public SpaAppointment bookingId(String bookingId) {
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

  public SpaAppointment guestName(String guestName) {
    this.guestName = guestName;
    return this;
  }

  /**
   * Denormalized from the named booking at read time - same convention as `PropertyMapCurrentBooking.guestName`.
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

  public SpaAppointment tableId(String tableId) {
    this.tableId = tableId;
    return this;
  }

  /**
   * Get tableId
   * @return tableId
   */
  @NotNull 
  @JsonProperty("tableId")
  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public SpaAppointment tableLabel(String tableLabel) {
    this.tableLabel = tableLabel;
    return this;
  }

  /**
   * Denormalized from the named table at read time.
   * @return tableLabel
   */
  @NotNull 
  @JsonProperty("tableLabel")
  public String getTableLabel() {
    return tableLabel;
  }

  public void setTableLabel(String tableLabel) {
    this.tableLabel = tableLabel;
  }

  public SpaAppointment therapistUserId(String therapistUserId) {
    this.therapistUserId = therapistUserId;
    return this;
  }

  /**
   * Get therapistUserId
   * @return therapistUserId
   */
  @NotNull 
  @JsonProperty("therapistUserId")
  public String getTherapistUserId() {
    return therapistUserId;
  }

  public void setTherapistUserId(String therapistUserId) {
    this.therapistUserId = therapistUserId;
  }

  public SpaAppointment therapistEmail(String therapistEmail) {
    this.therapistEmail = therapistEmail;
    return this;
  }

  /**
   * Denormalized from the named therapist at read time - same convention as `Order.openedByEmail`.
   * @return therapistEmail
   */
  @NotNull 
  @JsonProperty("therapistEmail")
  public String getTherapistEmail() {
    return therapistEmail;
  }

  public void setTherapistEmail(String therapistEmail) {
    this.therapistEmail = therapistEmail;
  }

  public SpaAppointment treatmentMenuItemId(String treatmentMenuItemId) {
    this.treatmentMenuItemId = treatmentMenuItemId;
    return this;
  }

  /**
   * Get treatmentMenuItemId
   * @return treatmentMenuItemId
   */
  @NotNull 
  @JsonProperty("treatmentMenuItemId")
  public String getTreatmentMenuItemId() {
    return treatmentMenuItemId;
  }

  public void setTreatmentMenuItemId(String treatmentMenuItemId) {
    this.treatmentMenuItemId = treatmentMenuItemId;
  }

  public SpaAppointment treatmentName(String treatmentName) {
    this.treatmentName = treatmentName;
    return this;
  }

  /**
   * Denormalized `MenuItem.name` at read time - the treatment's current name, not frozen (unlike `durationMinutes`).
   * @return treatmentName
   */
  @NotNull 
  @JsonProperty("treatmentName")
  public String getTreatmentName() {
    return treatmentName;
  }

  public void setTreatmentName(String treatmentName) {
    this.treatmentName = treatmentName;
  }

  public SpaAppointment date(String date) {
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

  public SpaAppointment startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Local 24-hour `HH:mm`. No time zone anywhere on this schema - see the class description.
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

  public SpaAppointment durationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Frozen at creation from `MenuItem.durationMinutes` - see the class description.
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

  public SpaAppointment status(SpaAppointmentStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public SpaAppointmentStatus getStatus() {
    return status;
  }

  public void setStatus(SpaAppointmentStatus status) {
    this.status = status;
  }

  public SpaAppointment orderId(String orderId) {
    this.orderId = JsonNullable.of(orderId);
    return this;
  }

  /**
   * The POS order that charged this treatment, if any - set when the order is opened against this appointment's table (auto-resolved server-side when unambiguous, or via an explicit `spaAppointmentId` on `OrderCreateInput` - see that field), never computed or amount-bearing here. A `COMPLETED` appointment with `orderId: null` is a real, visible gap (a treatment settled another way, or one nobody rang up yet) - deliberately not blocked, see the class description. 
   * @return orderId
   */
  @NotNull 
  @JsonProperty("orderId")
  public JsonNullable<String> getOrderId() {
    return orderId;
  }

  public void setOrderId(JsonNullable<String> orderId) {
    this.orderId = orderId;
  }

  public SpaAppointment createdByUserId(String createdByUserId) {
    this.createdByUserId = createdByUserId;
    return this;
  }

  /**
   * Get createdByUserId
   * @return createdByUserId
   */
  @NotNull 
  @JsonProperty("createdByUserId")
  public String getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(String createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public SpaAppointment cancelledByUserId(String cancelledByUserId) {
    this.cancelledByUserId = JsonNullable.of(cancelledByUserId);
    return this;
  }

  /**
   * Get cancelledByUserId
   * @return cancelledByUserId
   */
  @NotNull 
  @JsonProperty("cancelledByUserId")
  public JsonNullable<String> getCancelledByUserId() {
    return cancelledByUserId;
  }

  public void setCancelledByUserId(JsonNullable<String> cancelledByUserId) {
    this.cancelledByUserId = cancelledByUserId;
  }

  public SpaAppointment cancelReason(String cancelReason) {
    this.cancelReason = JsonNullable.of(cancelReason);
    return this;
  }

  /**
   * Get cancelReason
   * @return cancelReason
   */
  @NotNull 
  @JsonProperty("cancelReason")
  public JsonNullable<String> getCancelReason() {
    return cancelReason;
  }

  public void setCancelReason(JsonNullable<String> cancelReason) {
    this.cancelReason = cancelReason;
  }

  public SpaAppointment createdAt(OffsetDateTime createdAt) {
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

  public SpaAppointment updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaAppointment spaAppointment = (SpaAppointment) o;
    return Objects.equals(this.id, spaAppointment.id) &&
        Objects.equals(this.bookingId, spaAppointment.bookingId) &&
        Objects.equals(this.guestName, spaAppointment.guestName) &&
        Objects.equals(this.tableId, spaAppointment.tableId) &&
        Objects.equals(this.tableLabel, spaAppointment.tableLabel) &&
        Objects.equals(this.therapistUserId, spaAppointment.therapistUserId) &&
        Objects.equals(this.therapistEmail, spaAppointment.therapistEmail) &&
        Objects.equals(this.treatmentMenuItemId, spaAppointment.treatmentMenuItemId) &&
        Objects.equals(this.treatmentName, spaAppointment.treatmentName) &&
        Objects.equals(this.date, spaAppointment.date) &&
        Objects.equals(this.startTime, spaAppointment.startTime) &&
        Objects.equals(this.durationMinutes, spaAppointment.durationMinutes) &&
        Objects.equals(this.status, spaAppointment.status) &&
        Objects.equals(this.orderId, spaAppointment.orderId) &&
        Objects.equals(this.createdByUserId, spaAppointment.createdByUserId) &&
        Objects.equals(this.cancelledByUserId, spaAppointment.cancelledByUserId) &&
        Objects.equals(this.cancelReason, spaAppointment.cancelReason) &&
        Objects.equals(this.createdAt, spaAppointment.createdAt) &&
        Objects.equals(this.updatedAt, spaAppointment.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, bookingId, guestName, tableId, tableLabel, therapistUserId, therapistEmail, treatmentMenuItemId, treatmentName, date, startTime, durationMinutes, status, orderId, createdByUserId, cancelledByUserId, cancelReason, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaAppointment {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    tableLabel: ").append(toIndentedString(tableLabel)).append("\n");
    sb.append("    therapistUserId: ").append(toIndentedString(therapistUserId)).append("\n");
    sb.append("    therapistEmail: ").append(toIndentedString(therapistEmail)).append("\n");
    sb.append("    treatmentMenuItemId: ").append(toIndentedString(treatmentMenuItemId)).append("\n");
    sb.append("    treatmentName: ").append(toIndentedString(treatmentName)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    orderId: ").append(toIndentedString(orderId)).append("\n");
    sb.append("    createdByUserId: ").append(toIndentedString(createdByUserId)).append("\n");
    sb.append("    cancelledByUserId: ").append(toIndentedString(cancelledByUserId)).append("\n");
    sb.append("    cancelReason: ").append(toIndentedString(cancelReason)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

