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
 * Body of &#x60;PATCH /guest/password&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestPasswordChangeInput {

  private String currentPassword;

  private String newPassword;

  public GuestPasswordChangeInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestPasswordChangeInput(String currentPassword, String newPassword) {
    this.currentPassword = currentPassword;
    this.newPassword = newPassword;
  }

  public GuestPasswordChangeInput currentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
    return this;
  }

  /**
   * Verified against the caller's own `GuestAccount.passwordHash` before the change is accepted.
   * @return currentPassword
   */
  @NotNull 
  @JsonProperty("currentPassword")
  public String getCurrentPassword() {
    return currentPassword;
  }

  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public GuestPasswordChangeInput newPassword(String newPassword) {
    this.newPassword = newPassword;
    return this;
  }

  /**
   * Get newPassword
   * @return newPassword
   */
  @NotNull @Size(min = 8, max = 200) 
  @JsonProperty("newPassword")
  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestPasswordChangeInput guestPasswordChangeInput = (GuestPasswordChangeInput) o;
    return Objects.equals(this.currentPassword, guestPasswordChangeInput.currentPassword) &&
        Objects.equals(this.newPassword, guestPasswordChangeInput.newPassword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentPassword, newPassword);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuestPasswordChangeInput {\n");
    sb.append("    currentPassword: ").append(toIndentedString(currentPassword)).append("\n");
    sb.append("    newPassword: ").append(toIndentedString(newPassword)).append("\n");
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

