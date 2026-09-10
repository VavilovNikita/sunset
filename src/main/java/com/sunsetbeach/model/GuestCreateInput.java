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
 * Body of &#x60;POST /guests&#x60;. Nothing here is checked against existing guests before creating - no automatic merging, see &#x60;Guest&#x60;&#39;s own description - &#x60;GET /guests?q&#x3D;&#x60; is how a caller checks for a likely duplicate (by name, email, or phone) *before* submitting this, and is expected to be called for exactly that reason; this endpoint itself never blocks on it. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestCreateInput {

  private String name;

  private JsonNullable<String> email = JsonNullable.<String>undefined();

  private JsonNullable<String> phone = JsonNullable.<String>undefined();

  private JsonNullable<@Size(max = 2000) String> notes = JsonNullable.<String>undefined();

  public GuestCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestCreateInput(String name) {
    this.name = name;
  }

  public GuestCreateInput name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 120) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public GuestCreateInput email(String email) {
    this.email = JsonNullable.of(email);
    return this;
  }

  /**
   * Get email
   * @return email
   */
  
  @JsonProperty("email")
  public JsonNullable<String> getEmail() {
    return email;
  }

  public void setEmail(JsonNullable<String> email) {
    this.email = email;
  }

  public GuestCreateInput phone(String phone) {
    this.phone = JsonNullable.of(phone);
    return this;
  }

  /**
   * Get phone
   * @return phone
   */
  
  @JsonProperty("phone")
  public JsonNullable<String> getPhone() {
    return phone;
  }

  public void setPhone(JsonNullable<String> phone) {
    this.phone = phone;
  }

  public GuestCreateInput notes(String notes) {
    this.notes = JsonNullable.of(notes);
    return this;
  }

  /**
   * Get notes
   * @return notes
   */
  @Size(max = 2000) 
  @JsonProperty("notes")
  public JsonNullable<@Size(max = 2000) String> getNotes() {
    return notes;
  }

  public void setNotes(JsonNullable<String> notes) {
    this.notes = notes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestCreateInput guestCreateInput = (GuestCreateInput) o;
    return Objects.equals(this.name, guestCreateInput.name) &&
        equalsNullable(this.email, guestCreateInput.email) &&
        equalsNullable(this.phone, guestCreateInput.phone) &&
        equalsNullable(this.notes, guestCreateInput.notes);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, hashCodeNullable(email), hashCodeNullable(phone), hashCodeNullable(notes));
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
    sb.append("class GuestCreateInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append("[REDACTED]").append("\n");
    sb.append("    phone: ").append("[REDACTED]").append("\n");
    sb.append("    notes: ").append("[REDACTED]").append("\n");
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

