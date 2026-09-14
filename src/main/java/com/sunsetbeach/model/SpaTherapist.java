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
 * Minimal projection of a staff &#x60;User&#x60; holding the &#x60;THERAPIST&#x60; job function, for &#x60;GET /spa-appointments/therapists&#x60; - narrower than &#x60;GET /users&#x60; (ADMIN-only, deliberately outside the role hierarchy) so a CASHIER creating an appointment can list valid therapists without that escalation, the same \&quot;a role that may act must be able to read what the action needs\&quot; fix already applied to &#x60;GET /room-units&#x60;/&#x60;GET /availability&#x60;. Excludes an inactive user - see &#x60;PATCH /users/{id}/active&#x60;. &#x60;email&#x60; is absent for a no-login account (see &#x60;UserCreateInput&#x60;) - THERAPIST is an ordinary staff tag, unrelated to whether the account can sign in, so &#x60;name&#x60; is the field this picker displays, never absent. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaTherapist {

  private String id;

  private String name;

  private String email;

  public SpaTherapist() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaTherapist(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public SpaTherapist id(String id) {
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

  public SpaTherapist name(String name) {
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

  public SpaTherapist email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
   */
  
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaTherapist spaTherapist = (SpaTherapist) o;
    return Objects.equals(this.id, spaTherapist.id) &&
        Objects.equals(this.name, spaTherapist.name) &&
        Objects.equals(this.email, spaTherapist.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaTherapist {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
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

