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
 * Body of &#x60;PATCH /users/{id}/enrollment-number&#x60;. See &#x60;User.enrollmentNumber&#x60;&#39;s own description. &#x60;enrollmentNumber&#x60; itself is deliberately not in &#x60;required&#x60; here even though the field is always expected - marking a nullable field both &#x60;required&#x60; and &#x60;nullable&#x60; generates a &#x60;@NotNull&#x60; that would reject the very null this endpoint exists to send (see CLAUDE.md&#39;s Code generation section); the service validates its presence manually instead, the same way &#x60;PUT /bookings/{id}/guest&#x60; does for its own nullable &#x60;guestId&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class UserEnrollmentNumberUpdateInput {

  private JsonNullable<Integer> enrollmentNumber = JsonNullable.<Integer>undefined();

  public UserEnrollmentNumberUpdateInput enrollmentNumber(Integer enrollmentNumber) {
    this.enrollmentNumber = JsonNullable.of(enrollmentNumber);
    return this;
  }

  /**
   * Send a number to assign it, or explicit `null` to clear it - omitting the field entirely is rejected.
   * @return enrollmentNumber
   */
  
  @JsonProperty("enrollmentNumber")
  public JsonNullable<Integer> getEnrollmentNumber() {
    return enrollmentNumber;
  }

  public void setEnrollmentNumber(JsonNullable<Integer> enrollmentNumber) {
    this.enrollmentNumber = enrollmentNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserEnrollmentNumberUpdateInput userEnrollmentNumberUpdateInput = (UserEnrollmentNumberUpdateInput) o;
    return equalsNullable(this.enrollmentNumber, userEnrollmentNumberUpdateInput.enrollmentNumber);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(enrollmentNumber));
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
    sb.append("class UserEnrollmentNumberUpdateInput {\n");
    sb.append("    enrollmentNumber: ").append(toIndentedString(enrollmentNumber)).append("\n");
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

