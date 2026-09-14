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
 * Body of &#x60;PATCH /users/{id}/credentials&#x60; - turns a no-login account (see &#x60;UserCreateInput&#x60;) into one that can authenticate. Both fields are required: unlike &#x60;PATCH /users/{id}/password&#x60;, there is no existing password to reset, and unlike a plain email change, this account has no email to begin with. Only valid while the target account has no email yet - see that operation&#39;s own description for why changing an *existing* login email is deliberately not this endpoint&#39;s job. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class UserCredentialsInput {

  private String email;

  private String password;

  public UserCredentialsInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserCredentialsInput(String email, String password) {
    this.email = email;
    this.password = password;
  }

  public UserCredentialsInput email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
   */
  @NotNull @jakarta.validation.constraints.Email 
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserCredentialsInput password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Hashed with bcrypt (cost 10) before storage; never stored or returned in plaintext.
   * @return password
   */
  @NotNull @Size(min = 8, max = 200) 
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserCredentialsInput userCredentialsInput = (UserCredentialsInput) o;
    return Objects.equals(this.email, userCredentialsInput.email) &&
        Objects.equals(this.password, userCredentialsInput.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserCredentialsInput {\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
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

