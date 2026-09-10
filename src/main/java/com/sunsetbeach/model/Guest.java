package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contact details only - name, email, phone, staff notes, and the stay history that falls out of &#x60;Booking.guestId&#x60; (see &#x60;GuestDetail&#x60;). No documents, no nationality, no passport; adding any of those later is a purely additive migration (new nullable columns on this same row), nothing here would need to change. &#x60;email&#x60;/&#x60;phone&#x60;/&#x60;notes&#x60; are free text a staff member typed and are marked &#x60;x-sensitive&#x60; for the same reason &#x60;Booking.guestEmail&#x60;/&#x60;guestPhone&#x60;/&#x60;paymentNote&#x60; already are - &#x60;notes&#x60; in particular is exactly the kind of field that ends up carrying incidental PII (\&quot;allergic to shellfish\&quot;, a complaint from a past stay), and this project already treats free-text staff commentary as sensitive by default. &#x60;name&#x60; stays unmarked, matching &#x60;Booking.guestName&#x60;&#39;s own precedent - see &#x60;ToStringRedactsGuestPiiTests&#x60;, which only ever asserted on email/phone/paymentNote, never a name. Deliberately carries no computed/aggregated field (no stay count, no lifetime total, no cached last-stay date) - every fact about a guest is reachable by walking to &#x60;Booking&#x60; via &#x60;guestId&#x60;, never stored here independently. This is what keeps a future merge (out of scope for now - see &#x60;PUT /bookings/{id}/guest&#x60;) mechanical: repoint every &#x60;Booking.guestId&#x60; from one &#x60;Guest&#x60; to another and delete the loser, with nothing cached anywhere that would need recomputing. The one thing that doesn&#39;t merge cleanly even so is &#x60;notes&#x60; itself - two cards for the same person can each accumulate their own free text before anyone notices, and reconciling two paragraphs is a job for a person reading both, not something a merge operation can do for you. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class Guest {

  private String id;

  private String name;

  private JsonNullable<String> email = JsonNullable.<String>undefined();

  private JsonNullable<String> phone = JsonNullable.<String>undefined();

  private JsonNullable<String> notes = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public Guest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Guest(String id, String name, String email, String phone, String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.email = JsonNullable.of(email);
    this.phone = JsonNullable.of(phone);
    this.notes = JsonNullable.of(notes);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Guest id(String id) {
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

  public Guest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Guest email(String email) {
    this.email = JsonNullable.of(email);
    return this;
  }

  /**
   * Get email
   * @return email
   */
  @NotNull 
  @JsonProperty("email")
  public JsonNullable<String> getEmail() {
    return email;
  }

  public void setEmail(JsonNullable<String> email) {
    this.email = email;
  }

  public Guest phone(String phone) {
    this.phone = JsonNullable.of(phone);
    return this;
  }

  /**
   * Get phone
   * @return phone
   */
  @NotNull 
  @JsonProperty("phone")
  public JsonNullable<String> getPhone() {
    return phone;
  }

  public void setPhone(JsonNullable<String> phone) {
    this.phone = phone;
  }

  public Guest notes(String notes) {
    this.notes = JsonNullable.of(notes);
    return this;
  }

  /**
   * Get notes
   * @return notes
   */
  @NotNull 
  @JsonProperty("notes")
  public JsonNullable<String> getNotes() {
    return notes;
  }

  public void setNotes(JsonNullable<String> notes) {
    this.notes = notes;
  }

  public Guest createdAt(OffsetDateTime createdAt) {
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

  public Guest updatedAt(OffsetDateTime updatedAt) {
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
    Guest guest = (Guest) o;
    return Objects.equals(this.id, guest.id) &&
        Objects.equals(this.name, guest.name) &&
        Objects.equals(this.email, guest.email) &&
        Objects.equals(this.phone, guest.phone) &&
        Objects.equals(this.notes, guest.notes) &&
        Objects.equals(this.createdAt, guest.createdAt) &&
        Objects.equals(this.updatedAt, guest.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, phone, notes, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Guest {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append("[REDACTED]").append("\n");
    sb.append("    phone: ").append("[REDACTED]").append("\n");
    sb.append("    notes: ").append("[REDACTED]").append("\n");
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

