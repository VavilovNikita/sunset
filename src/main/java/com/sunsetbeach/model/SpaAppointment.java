package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentTreatment;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A half-hour-grid treatment slot - occupies both a POS &#x60;Table&#x60; (in the SPA zone) and a therapist (a staff &#x60;User&#x60; holding the &#x60;THERAPIST&#x60; job function) for &#x60;[startTime, startTime + durationMinutes)&#x60; on &#x60;date&#x60;. Always names a booking - there is no walk-in path and no separate client record (hotel guests only); &#x60;guestName&#x60; is resolved from that booking at read time, not stored here. &#x60;treatments&#x60; is one row per treatment booked on this appointment - the same treatment added twice is two rows, not a quantity of two (see &#x60;SpaAppointmentTreatment&#x60;). &#x60;durationMinutes&#x60; on this object is a maintained sum of &#x60;treatments[].durationMinutes&#x60;, kept as its own number so the two &#x60;EXCLUDE USING gist&#x60; constraints on the &#x60;SpaAppointment&#x60; table (one keyed on &#x60;tableId&#x60;, one on &#x60;therapistUserId&#x60;, each restricted to &#x60;status IN (&#39;BOOKED&#39;, &#39;COMPLETED&#39;)&#x60; rows - see &#x60;V41__spa_appointment.sql&#x60; and &#x60;V43__spa_appointment_completed_still_occupies_slot.sql&#x60;) keep reading one plain column and never need to know the treatment table exists. The appointment&#39;s end is never stored - it&#39;s always &#x60;startTime + durationMinutes&#x60;, computed wherever needed. &#x60;date&#x60;/ &#x60;startTime&#x60; are a plain date-only string and a plain local &#x60;HH:mm&#x60; string - the hotel is a single location and nothing in this API converts time zones, so there is deliberately no offset anywhere on this schema. The appointment stores no price - see &#x60;SpaAppointmentTreatment.currentPrice&#x60; and &#x60;orderId&#x60;. Only &#x60;durationMinutes&#x60; is frozen per treatment; unlike a room night, a treatment bills through the ordinary POS &#x60;OrderItem&#x60;/menu path, which prices every line live off the menu at the moment it&#39;s added to an order - freezing a second price here would just manufacture a number that can silently disagree with the one actually charged, not protect a guest-facing quote (none is shown when an appointment is booked). See CLAUDE.md&#39;s Spa billing section for the full reasoning. &#x60;missingTreatmentNames&#x60; names the completeness warning: which of this appointment&#39;s treatments the linked order does not (yet) carry. Only meaningful once &#x60;status&#x60; is &#x60;COMPLETED&#x60; - empty for every other status, regardless of &#x60;orderId&#x60;. A &#x60;CANCELLED&#x60; linked order counts as carrying nothing (a cancelled order bills nothing), so an appointment linked to one still shows every treatment as missing. A therapist losing the &#x60;THERAPIST&#x60; function or being deactivated while holding future &#x60;BOOKED&#x60; appointments does not cascade to this row automatically - &#x60;PATCH /users/{id}/active&#x60; / &#x60;PATCH /users/{id}/functions&#x60; instead return a warning listing the affected future appointments, the same warn-don&#39;t- block shape as &#x60;RoomUnitBlockResult&#x60;. Cancelling/shortening/early-checking-out the named booking has no equivalent warning today - nothing in this system cascades a booking-date change to this row, and no write path checks for an affected future appointment the way the therapist-side paths do. That gap is real, not yet closed. 
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

  @Valid
  private List<@Valid SpaAppointmentTreatment> treatments = new ArrayList<>();

  private String date;

  private String startTime;

  private Integer durationMinutes;

  private SpaAppointmentStatus status;

  private JsonNullable<String> orderId = JsonNullable.<String>undefined();

  @Valid
  private List<String> missingTreatmentNames = new ArrayList<>();

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
  public SpaAppointment(String id, String bookingId, String guestName, String tableId, String tableLabel, String therapistUserId, String therapistEmail, List<@Valid SpaAppointmentTreatment> treatments, String date, String startTime, Integer durationMinutes, SpaAppointmentStatus status, String orderId, List<String> missingTreatmentNames, String createdByUserId, String cancelledByUserId, String cancelReason, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.bookingId = bookingId;
    this.guestName = guestName;
    this.tableId = tableId;
    this.tableLabel = tableLabel;
    this.therapistUserId = therapistUserId;
    this.therapistEmail = therapistEmail;
    this.treatments = treatments;
    this.date = date;
    this.startTime = startTime;
    this.durationMinutes = durationMinutes;
    this.status = status;
    this.orderId = JsonNullable.of(orderId);
    this.missingTreatmentNames = missingTreatmentNames;
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

  public SpaAppointment treatments(List<@Valid SpaAppointmentTreatment> treatments) {
    this.treatments = treatments;
    return this;
  }

  public SpaAppointment addTreatmentsItem(SpaAppointmentTreatment treatmentsItem) {
    if (this.treatments == null) {
      this.treatments = new ArrayList<>();
    }
    this.treatments.add(treatmentsItem);
    return this;
  }

  /**
   * One row per treatment, in no particular guaranteed order. Always at least one - an appointment cannot exist with zero treatments.
   * @return treatments
   */
  @NotNull @Valid 
  @JsonProperty("treatments")
  public List<@Valid SpaAppointmentTreatment> getTreatments() {
    return treatments;
  }

  public void setTreatments(List<@Valid SpaAppointmentTreatment> treatments) {
    this.treatments = treatments;
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
   * A maintained sum of `treatments[].durationMinutes` - see the class description.
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
   * The POS order that charged this treatment, if any - auto-resolved server-side once the order actually carries a `SPA`-department item and names this appointment's booking or table unambiguously, or set via an explicit `spaAppointmentId` on `OrderCreateInput` (see that field for exactly how), never computed or amount-bearing here. A `COMPLETED` appointment with `orderId: null` is a real, visible gap (a treatment settled another way, or one nobody rang up yet) - deliberately not blocked, see the class description. 
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

  public SpaAppointment missingTreatmentNames(List<String> missingTreatmentNames) {
    this.missingTreatmentNames = missingTreatmentNames;
    return this;
  }

  public SpaAppointment addMissingTreatmentNamesItem(String missingTreatmentNamesItem) {
    if (this.missingTreatmentNames == null) {
      this.missingTreatmentNames = new ArrayList<>();
    }
    this.missingTreatmentNames.add(missingTreatmentNamesItem);
    return this;
  }

  /**
   * See the class description. Empty when there's nothing to warn about.
   * @return missingTreatmentNames
   */
  @NotNull 
  @JsonProperty("missingTreatmentNames")
  public List<String> getMissingTreatmentNames() {
    return missingTreatmentNames;
  }

  public void setMissingTreatmentNames(List<String> missingTreatmentNames) {
    this.missingTreatmentNames = missingTreatmentNames;
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
        Objects.equals(this.treatments, spaAppointment.treatments) &&
        Objects.equals(this.date, spaAppointment.date) &&
        Objects.equals(this.startTime, spaAppointment.startTime) &&
        Objects.equals(this.durationMinutes, spaAppointment.durationMinutes) &&
        Objects.equals(this.status, spaAppointment.status) &&
        Objects.equals(this.orderId, spaAppointment.orderId) &&
        Objects.equals(this.missingTreatmentNames, spaAppointment.missingTreatmentNames) &&
        Objects.equals(this.createdByUserId, spaAppointment.createdByUserId) &&
        Objects.equals(this.cancelledByUserId, spaAppointment.cancelledByUserId) &&
        Objects.equals(this.cancelReason, spaAppointment.cancelReason) &&
        Objects.equals(this.createdAt, spaAppointment.createdAt) &&
        Objects.equals(this.updatedAt, spaAppointment.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, bookingId, guestName, tableId, tableLabel, therapistUserId, therapistEmail, treatments, date, startTime, durationMinutes, status, orderId, missingTreatmentNames, createdByUserId, cancelledByUserId, cancelReason, createdAt, updatedAt);
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
    sb.append("    treatments: ").append(toIndentedString(treatments)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    orderId: ").append(toIndentedString(orderId)).append("\n");
    sb.append("    missingTreatmentNames: ").append(toIndentedString(missingTreatmentNames)).append("\n");
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

