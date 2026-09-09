package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.User;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;PATCH /users/{id}/active&#x60; and &#x60;PATCH /users/{id}/functions&#x60;. &#x60;warning&#x60; is set (the change still succeeds) when the user being disabled, or having &#x60;THERAPIST&#x60; removed, holds one or more future &#x60;BOOKED&#x60; spa appointments - warn, don&#39;t block, same shape as &#x60;CheckInResult&#x60;/&#x60;RoomUnitBlockResult&#x60;; nothing here cancels those appointments automatically. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class UserUpdateResult {

  private User user;

  private JsonNullable<String> warning = JsonNullable.<String>undefined();

  public UserUpdateResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserUpdateResult(User user, String warning) {
    this.user = user;
    this.warning = JsonNullable.of(warning);
  }

  public UserUpdateResult user(User user) {
    this.user = user;
    return this;
  }

  /**
   * Get user
   * @return user
   */
  @NotNull @Valid 
  @JsonProperty("user")
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public UserUpdateResult warning(String warning) {
    this.warning = JsonNullable.of(warning);
    return this;
  }

  /**
   * Get warning
   * @return warning
   */
  @NotNull 
  @JsonProperty("warning")
  public JsonNullable<String> getWarning() {
    return warning;
  }

  public void setWarning(JsonNullable<String> warning) {
    this.warning = warning;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserUpdateResult userUpdateResult = (UserUpdateResult) o;
    return Objects.equals(this.user, userUpdateResult.user) &&
        Objects.equals(this.warning, userUpdateResult.warning);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, warning);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserUpdateResult {\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    warning: ").append(toIndentedString(warning)).append("\n");
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

