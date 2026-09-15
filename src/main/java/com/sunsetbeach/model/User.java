package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.Role;
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
 * Safe projection returned by the API — &#x60;passwordHash&#x60; is never included.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class User {

  private String id;

  private String name;

  private String email;

  private Role role;

  private Boolean active;

  @Valid
  private List<JobFunction> functions = new ArrayList<>();

  private Boolean overtimeEligible;

  private Integer enrollmentNumber;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public User() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public User(String id, String name, Role role, Boolean active, List<JobFunction> functions, Boolean overtimeEligible, OffsetDateTime createdAt) {
    this.id = id;
    this.name = name;
    this.role = role;
    this.active = active;
    this.functions = functions;
    this.overtimeEligible = overtimeEligible;
    this.createdAt = createdAt;
  }

  public User id(String id) {
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

  public User name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The person's name - always present, unlike `email`. This is the only durable identifier for a `POST /users` account created without login credentials (see `UserCreateInput`), so it exists independently of whether the account can authenticate. 
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

  public User email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Absent for an account created without login credentials - see `UserCreateInput`. Such an account can be given credentials later via `PATCH /users/{id}/credentials` without being recreated. 
   * @return email
   */
  
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public User role(Role role) {
    this.role = role;
    return this;
  }

  /**
   * Get role
   * @return role
   */
  @NotNull @Valid 
  @JsonProperty("role")
  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public User active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * Whether this account can currently authenticate. A disabled account's already-issued tokens are rejected on their very next request regardless of remaining validity - see `PATCH /users/{id}/active`. 
   * @return active
   */
  @NotNull 
  @JsonProperty("active")
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public User functions(List<JobFunction> functions) {
    this.functions = functions;
    return this;
  }

  public User addFunctionsItem(JobFunction functionsItem) {
    if (this.functions == null) {
      this.functions = new ArrayList<>();
    }
    this.functions.add(functionsItem);
    return this;
  }

  /**
   * This user's job functions (see `JobFunction`) - zero or more, independent of `role`. Set via `PATCH /users/{id}/functions`. Does not affect authentication - unlike a role change, changing this does not invalidate existing tokens (see that operation's own description for why). 
   * @return functions
   */
  @NotNull @Valid 
  @JsonProperty("functions")
  public List<JobFunction> getFunctions() {
    return functions;
  }

  public void setFunctions(List<JobFunction> functions) {
    this.functions = functions;
  }

  public User overtimeEligible(Boolean overtimeEligible) {
    this.overtimeEligible = overtimeEligible;
    return this;
  }

  /**
   * Whether this person is eligible for overtime - a fact about the person, not something derived from their current shift code: the hotel's own rule (\"everyone is eligible except staff on the `OP` code, plus one individual exception on an ordinary code\") has an exception that doesn't correlate with any code group, so it can't be computed on the fly. Defaults to `true` at creation (see `UserCreateInput`) and is set per person via `PATCH /users/{id}/overtime-eligibility` - nothing in this API computes or accrues overtime pay from this flag today; it only records the fact so it doesn't have to be reconstructed later. 
   * @return overtimeEligible
   */
  @NotNull 
  @JsonProperty("overtimeEligible")
  public Boolean getOvertimeEligible() {
    return overtimeEligible;
  }

  public void setOvertimeEligible(Boolean overtimeEligible) {
    this.overtimeEligible = overtimeEligible;
  }

  public User enrollmentNumber(Integer enrollmentNumber) {
    this.enrollmentNumber = enrollmentNumber;
    return this;
  }

  /**
   * The fingerprint terminal's own numeric PIN for this person, not this system's id - a device attendance punch is attributed by this number alone (see `AttendanceDevice`), so without one, nothing that terminal reports can ever be linked to this person. Absent for staff who never punch a terminal. Unique when set - two people can't share one number - and settable at creation (see `UserCreateInput`) or later via `PATCH /users/{id}/enrollment-number`. 
   * @return enrollmentNumber
   */
  
  @JsonProperty("enrollmentNumber")
  public Integer getEnrollmentNumber() {
    return enrollmentNumber;
  }

  public void setEnrollmentNumber(Integer enrollmentNumber) {
    this.enrollmentNumber = enrollmentNumber;
  }

  public User createdAt(OffsetDateTime createdAt) {
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
    User user = (User) o;
    return Objects.equals(this.id, user.id) &&
        Objects.equals(this.name, user.name) &&
        Objects.equals(this.email, user.email) &&
        Objects.equals(this.role, user.role) &&
        Objects.equals(this.active, user.active) &&
        Objects.equals(this.functions, user.functions) &&
        Objects.equals(this.overtimeEligible, user.overtimeEligible) &&
        Objects.equals(this.enrollmentNumber, user.enrollmentNumber) &&
        Objects.equals(this.createdAt, user.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, role, active, functions, overtimeEligible, enrollmentNumber, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class User {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    functions: ").append(toIndentedString(functions)).append("\n");
    sb.append("    overtimeEligible: ").append(toIndentedString(overtimeEligible)).append("\n");
    sb.append("    enrollmentNumber: ").append(toIndentedString(enrollmentNumber)).append("\n");
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

