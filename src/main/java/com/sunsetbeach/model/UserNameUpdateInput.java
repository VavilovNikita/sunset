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
 * Body of &#x60;PATCH /users/{id}/name&#x60;. See &#x60;User.name&#x60;&#39;s own description. Unlike &#x60;UserFullNameUpdateInput&#x60;, &#x60;name&#x60; is a plain required string - it cannot be cleared, so there is no null case. The service also trims it and rejects a blank result, so a whitespace-only value that passes &#x60;minLength&#x60; is still a 400. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class UserNameUpdateInput {

  private String name;

  public UserNameUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserNameUpdateInput(String name) {
    this.name = name;
  }

  public UserNameUpdateInput name(String name) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserNameUpdateInput userNameUpdateInput = (UserNameUpdateInput) o;
    return Objects.equals(this.name, userNameUpdateInput.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserNameUpdateInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

