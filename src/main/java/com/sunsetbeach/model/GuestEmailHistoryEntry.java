package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.LifecycleEmailType;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One automated lifecycle email sent to a guest - see &#x60;GuestDetail.emailHistory&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestEmailHistoryEntry {

  private LifecycleEmailType type;

  private String subject;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime sentAt;

  private JsonNullable<String> bookingId = JsonNullable.<String>undefined();

  public GuestEmailHistoryEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestEmailHistoryEntry(LifecycleEmailType type, String subject, OffsetDateTime sentAt, String bookingId) {
    this.type = type;
    this.subject = subject;
    this.sentAt = sentAt;
    this.bookingId = JsonNullable.of(bookingId);
  }

  public GuestEmailHistoryEntry type(LifecycleEmailType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @JsonProperty("type")
  public LifecycleEmailType getType() {
    return type;
  }

  public void setType(LifecycleEmailType type) {
    this.type = type;
  }

  public GuestEmailHistoryEntry subject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Get subject
   * @return subject
   */
  @NotNull 
  @JsonProperty("subject")
  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public GuestEmailHistoryEntry sentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
    return this;
  }

  /**
   * Get sentAt
   * @return sentAt
   */
  @NotNull @Valid 
  @JsonProperty("sentAt")
  public OffsetDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public GuestEmailHistoryEntry bookingId(String bookingId) {
    this.bookingId = JsonNullable.of(bookingId);
    return this;
  }

  /**
   * The stay the email was about. Null for `WIN_BACK`, which isn't about one stay.
   * @return bookingId
   */
  @NotNull 
  @JsonProperty("bookingId")
  public JsonNullable<String> getBookingId() {
    return bookingId;
  }

  public void setBookingId(JsonNullable<String> bookingId) {
    this.bookingId = bookingId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestEmailHistoryEntry guestEmailHistoryEntry = (GuestEmailHistoryEntry) o;
    return Objects.equals(this.type, guestEmailHistoryEntry.type) &&
        Objects.equals(this.subject, guestEmailHistoryEntry.subject) &&
        Objects.equals(this.sentAt, guestEmailHistoryEntry.sentAt) &&
        Objects.equals(this.bookingId, guestEmailHistoryEntry.bookingId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, subject, sentAt, bookingId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestEmailHistoryEntry {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    sentAt: ").append(toIndentedString(sentAt)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
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

