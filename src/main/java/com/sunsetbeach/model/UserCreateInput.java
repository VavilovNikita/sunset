package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.Role;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;userCreateSchema&#x60;. &#x60;email&#x60;/&#x60;password&#x60; are optional together - send both to create a login-capable account, or neither to create one that cannot authenticate at all (for staff who exist so the roster/attendance/pay-rate records have someone to point at, but never sign in - a cook or a housekeeper, say). Sending exactly one of the two is a 400 (&#x60;UserService&#x60; validates the pairing manually - see its own comment for why this can&#39;t be expressed as &#x60;required&#x60; in this schema). &#x60;name&#x60; is required either way. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class UserCreateInput {

  private String name;

  private String email;

  private String password;

  private Role role;

  public UserCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserCreateInput(String name) {
    this.name = name;
  }

  public UserCreateInput name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UserCreateInput email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
   */
  @jakarta.validation.constraints.Email(message = "Invalid email")
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserCreateInput password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Hashed with bcrypt (cost 10) before storage; never stored or returned in plaintext.
   * @return password
   */
  @Size(min = 8, max = 200) 
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public UserCreateInput role(Role role) {
    this.role = role;
    return this;
  }

  /**
   * Get role
   * @return role
   */
  @Valid 
  @JsonProperty("role")
  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserCreateInput userCreateInput = (UserCreateInput) o;
    return Objects.equals(this.name, userCreateInput.name) &&
        Objects.equals(this.email, userCreateInput.email) &&
        Objects.equals(this.password, userCreateInput.password) &&
        Objects.equals(this.role, userCreateInput.role);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, email, password, role);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserCreateInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
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

