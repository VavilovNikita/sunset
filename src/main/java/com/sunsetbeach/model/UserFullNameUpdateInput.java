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
 * Body of &#x60;PATCH /users/{id}/full-name&#x60;. See &#x60;User.fullName&#x60;&#39;s own description. &#x60;fullName&#x60; is deliberately not in &#x60;required&#x60; - same nullable-plus-required &#x60;@NotNull&#x60; trap as &#x60;UserEnrollmentNumberUpdateInput&#x60;; the service validates its presence manually. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class UserFullNameUpdateInput {

  private JsonNullable<@Size(max = 200) String> fullName = JsonNullable.<String>undefined();

  public UserFullNameUpdateInput fullName(String fullName) {
    this.fullName = JsonNullable.of(fullName);
    return this;
  }

  /**
   * Send a string to assign it, or explicit `null` (or blank) to clear it - omitting the field entirely is rejected.
   * @return fullName
   */
  @Size(max = 200) 
  @JsonProperty("fullName")
  public JsonNullable<@Size(max = 200) String> getFullName() {
    return fullName;
  }

  public void setFullName(JsonNullable<String> fullName) {
    this.fullName = fullName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserFullNameUpdateInput userFullNameUpdateInput = (UserFullNameUpdateInput) o;
    return equalsNullable(this.fullName, userFullNameUpdateInput.fullName);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(fullName));
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
    sb.append("class UserFullNameUpdateInput {\n");
    sb.append("    fullName: ").append(toIndentedString(fullName)).append("\n");
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

