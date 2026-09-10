package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.Booking;
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
 * &#x60;GET /guests/{id}&#x60; - the full guest card. &#x60;bookings&#x60; is this guest&#39;s entire stay history (every booking with this &#x60;guestId&#x60;, regardless of status - a cancelled stay is still a real interaction and hiding it would make the card lie by omission), newest &#x60;checkIn&#x60; first. Deliberately no lifetime-spend or stay-count total here: each booking already shows its own &#x60;totalPrice&#x60;/&#x60;status&#x60; (the same server-computed figures the booking detail page itself shows, not a second computation of them), and a real rolled-up total would need to correctly sum room revenue, POS orders, and folio settlements across every booking while excluding cancellations - a reporting feature in its own right, with the same care the rest of the money logic in this API already gets, not one field bolted onto a contact card. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestDetail {

  private String id;

  private String name;

  private JsonNullable<String> email = JsonNullable.<String>undefined();

  private JsonNullable<String> phone = JsonNullable.<String>undefined();

  private JsonNullable<String> notes = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  @Valid
  private List<@Valid Booking> bookings = new ArrayList<>();

  public GuestDetail() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestDetail(String id, String name, String email, String phone, String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt, List<@Valid Booking> bookings) {
    this.id = id;
    this.name = name;
    this.email = JsonNullable.of(email);
    this.phone = JsonNullable.of(phone);
    this.notes = JsonNullable.of(notes);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.bookings = bookings;
  }

  public GuestDetail id(String id) {
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

  public GuestDetail name(String name) {
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

  public GuestDetail email(String email) {
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

  public GuestDetail phone(String phone) {
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

  public GuestDetail notes(String notes) {
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

  public GuestDetail createdAt(OffsetDateTime createdAt) {
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

  public GuestDetail updatedAt(OffsetDateTime updatedAt) {
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

  public GuestDetail bookings(List<@Valid Booking> bookings) {
    this.bookings = bookings;
    return this;
  }

  public GuestDetail addBookingsItem(Booking bookingsItem) {
    if (this.bookings == null) {
      this.bookings = new ArrayList<>();
    }
    this.bookings.add(bookingsItem);
    return this;
  }

  /**
   * Get bookings
   * @return bookings
   */
  @NotNull @Valid 
  @JsonProperty("bookings")
  public List<@Valid Booking> getBookings() {
    return bookings;
  }

  public void setBookings(List<@Valid Booking> bookings) {
    this.bookings = bookings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestDetail guestDetail = (GuestDetail) o;
    return Objects.equals(this.id, guestDetail.id) &&
        Objects.equals(this.name, guestDetail.name) &&
        Objects.equals(this.email, guestDetail.email) &&
        Objects.equals(this.phone, guestDetail.phone) &&
        Objects.equals(this.notes, guestDetail.notes) &&
        Objects.equals(this.createdAt, guestDetail.createdAt) &&
        Objects.equals(this.updatedAt, guestDetail.updatedAt) &&
        Objects.equals(this.bookings, guestDetail.bookings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, phone, notes, createdAt, updatedAt, bookings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestDetail {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append("[REDACTED]").append("\n");
    sb.append("    phone: ").append("[REDACTED]").append("\n");
    sb.append("    notes: ").append("[REDACTED]").append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("    bookings: ").append(toIndentedString(bookings)).append("\n");
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

