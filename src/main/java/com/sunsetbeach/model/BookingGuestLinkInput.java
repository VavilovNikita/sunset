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
 * Body of &#x60;PUT /bookings/{id}/guest&#x60;. &#x60;guestId&#x60; must be present in the payload (either a string or explicit &#x60;null&#x60;, which clears the link) - deliberately NOT listed under &#x60;required&#x60; below despite that, same reasoning as &#x60;RoomUnitAssignmentInput.roomUnitId&#x60;: a generated &#x60;@NotNull&#x60; on a &#x60;nullable: true&#x60; + &#x60;required&#x60; property would validate the *unwrapped* value, rejecting the exact &#x60;null&#x60; this field exists to accept. Presence is checked manually in &#x60;BookingService&#x60;. Relinking to a different guest needs no confirmation and no state check - nothing downstream depends on which &#x60;Guest&#x60; a booking points to except that guest&#39;s own stay history, so there is nothing to protect against the way there is for occupancy or payment actions. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingGuestLinkInput {

  private JsonNullable<String> guestId = JsonNullable.<String>undefined();

  public BookingGuestLinkInput guestId(String guestId) {
    this.guestId = JsonNullable.of(guestId);
    return this;
  }

  /**
   * Get guestId
   * @return guestId
   */
  
  @JsonProperty("guestId")
  public JsonNullable<String> getGuestId() {
    return guestId;
  }

  public void setGuestId(JsonNullable<String> guestId) {
    this.guestId = guestId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingGuestLinkInput bookingGuestLinkInput = (BookingGuestLinkInput) o;
    return equalsNullable(this.guestId, bookingGuestLinkInput.guestId);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(guestId));
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
    sb.append("class BookingGuestLinkInput {\n");
    sb.append("    guestId: ").append(toIndentedString(guestId)).append("\n");
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

