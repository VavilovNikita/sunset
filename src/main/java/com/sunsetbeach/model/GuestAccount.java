package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A guest&#39;s own persistent, self-service login - entirely separate from the CRM &#x60;Guest&#x60; schema (see the &#x60;GuestAccountAuth&#x60; tag&#39;s own description for why). Usable only once &#x60;emailVerifiedAt&#x60; is set. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestAccount {

  private String id;

  private String email;

  private JsonNullable<String> name = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> emailVerifiedAt = JsonNullable.<OffsetDateTime>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public GuestAccount() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestAccount(String id, String email, OffsetDateTime createdAt) {
    this.id = id;
    this.email = email;
    this.createdAt = createdAt;
  }

  public GuestAccount id(String id) {
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

  public GuestAccount email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
   */
  @NotNull 
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public GuestAccount name(String name) {
    this.name = JsonNullable.of(name);
    return this;
  }

  /**
   * Get name
   * @return name
   */
  
  @JsonProperty("name")
  public JsonNullable<String> getName() {
    return name;
  }

  public void setName(JsonNullable<String> name) {
    this.name = name;
  }

  public GuestAccount emailVerifiedAt(OffsetDateTime emailVerifiedAt) {
    this.emailVerifiedAt = JsonNullable.of(emailVerifiedAt);
    return this;
  }

  /**
   * Get emailVerifiedAt
   * @return emailVerifiedAt
   */
  @Valid 
  @JsonProperty("emailVerifiedAt")
  public JsonNullable<OffsetDateTime> getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  public void setEmailVerifiedAt(JsonNullable<OffsetDateTime> emailVerifiedAt) {
    this.emailVerifiedAt = emailVerifiedAt;
  }

  public GuestAccount createdAt(OffsetDateTime createdAt) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestAccount guestAccount = (GuestAccount) o;
    return Objects.equals(this.id, guestAccount.id) &&
        Objects.equals(this.email, guestAccount.email) &&
        equalsNullable(this.name, guestAccount.name) &&
        equalsNullable(this.emailVerifiedAt, guestAccount.emailVerifiedAt) &&
        Objects.equals(this.createdAt, guestAccount.createdAt);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, email, hashCodeNullable(name), hashCodeNullable(emailVerifiedAt), createdAt);
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
    sb.append("class GuestAccount {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    email: ").append("[REDACTED]").append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    emailVerifiedAt: ").append(toIndentedString(emailVerifiedAt)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

