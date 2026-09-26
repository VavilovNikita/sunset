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
 * Staff-facing summary of the &#x60;GuestAccount&#x60; linked to a &#x60;Guest&#x60; card - presence and verification only. Deliberately no id, email, password hash, token version, or verification token: staff can see that a guest has an account, never anything that would help act as it. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestAccountLinkSummary {

  private Boolean emailVerified;

  public GuestAccountLinkSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestAccountLinkSummary(Boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public GuestAccountLinkSummary emailVerified(Boolean emailVerified) {
    this.emailVerified = emailVerified;
    return this;
  }

  /**
   * False while the guest hasn't confirmed their email yet (they can't log in until they do).
   * @return emailVerified
   */
  @NotNull 
  @JsonProperty("emailVerified")
  public Boolean getEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(Boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestAccountLinkSummary guestAccountLinkSummary = (GuestAccountLinkSummary) o;
    return Objects.equals(this.emailVerified, guestAccountLinkSummary.emailVerified);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailVerified);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestAccountLinkSummary {\n");
    sb.append("    emailVerified: ").append(toIndentedString(emailVerified)).append("\n");
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

