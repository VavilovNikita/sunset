package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.StaffArea;
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
 * Body of &#x60;PATCH /users/{id}/staff-area&#x60;. Same nullable-but-required-presence shape as &#x60;UserEnrollmentNumberUpdateInput&#x60;, for the same reason (see CLAUDE.md&#39;s Code generation section) - &#x60;staffArea&#x60; is deliberately not in &#x60;required&#x60; even though it&#39;s always expected, so the service validates its presence manually instead of generating a &#x60;@NotNull&#x60; that would reject the very null this endpoint exists to send. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class UserStaffAreaUpdateInput {

  private JsonNullable<StaffArea> staffArea = JsonNullable.<StaffArea>undefined();

  public UserStaffAreaUpdateInput staffArea(StaffArea staffArea) {
    this.staffArea = JsonNullable.of(staffArea);
    return this;
  }

  /**
   * Send an area to assign it, or explicit `null` to clear it - omitting the field entirely is rejected.
   * @return staffArea
   */
  @Valid 
  @JsonProperty("staffArea")
  public JsonNullable<StaffArea> getStaffArea() {
    return staffArea;
  }

  public void setStaffArea(JsonNullable<StaffArea> staffArea) {
    this.staffArea = staffArea;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserStaffAreaUpdateInput userStaffAreaUpdateInput = (UserStaffAreaUpdateInput) o;
    return equalsNullable(this.staffArea, userStaffAreaUpdateInput.staffArea);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(staffArea));
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
    sb.append("class UserStaffAreaUpdateInput {\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
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

