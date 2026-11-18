package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PATCH /reports/booking-tracker/snapshots/{id}&#x60;. Omitted leaves a field alone; &#x60;null&#x60; (or blank text) clears it. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerSnapshotUpdateInput {

  private JsonNullable<@Size(max = 500) String> reason = JsonNullable.<String>undefined();

  private JsonNullable<@Size(max = 500) String> nextAction = JsonNullable.<String>undefined();

  private JsonNullable<@Size(max = 120) String> owner = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private JsonNullable<LocalDate> dueDate = JsonNullable.<LocalDate>undefined();

  public BookingTrackerSnapshotUpdateInput reason(String reason) {
    this.reason = JsonNullable.of(reason);
    return this;
  }

  /**
   * Get reason
   * @return reason
   */
  @Size(max = 500) 
  @JsonProperty("reason")
  public JsonNullable<@Size(max = 500) String> getReason() {
    return reason;
  }

  public void setReason(JsonNullable<String> reason) {
    this.reason = reason;
  }

  public BookingTrackerSnapshotUpdateInput nextAction(String nextAction) {
    this.nextAction = JsonNullable.of(nextAction);
    return this;
  }

  /**
   * Get nextAction
   * @return nextAction
   */
  @Size(max = 500) 
  @JsonProperty("nextAction")
  public JsonNullable<@Size(max = 500) String> getNextAction() {
    return nextAction;
  }

  public void setNextAction(JsonNullable<String> nextAction) {
    this.nextAction = nextAction;
  }

  public BookingTrackerSnapshotUpdateInput owner(String owner) {
    this.owner = JsonNullable.of(owner);
    return this;
  }

  /**
   * Get owner
   * @return owner
   */
  @Size(max = 120) 
  @JsonProperty("owner")
  public JsonNullable<@Size(max = 120) String> getOwner() {
    return owner;
  }

  public void setOwner(JsonNullable<String> owner) {
    this.owner = owner;
  }

  public BookingTrackerSnapshotUpdateInput dueDate(LocalDate dueDate) {
    this.dueDate = JsonNullable.of(dueDate);
    return this;
  }

  /**
   * Get dueDate
   * @return dueDate
   */
  @Valid 
  @JsonProperty("dueDate")
  public JsonNullable<LocalDate> getDueDate() {
    return dueDate;
  }

  public void setDueDate(JsonNullable<LocalDate> dueDate) {
    this.dueDate = dueDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerSnapshotUpdateInput bookingTrackerSnapshotUpdateInput = (BookingTrackerSnapshotUpdateInput) o;
    return equalsNullable(this.reason, bookingTrackerSnapshotUpdateInput.reason) &&
        equalsNullable(this.nextAction, bookingTrackerSnapshotUpdateInput.nextAction) &&
        equalsNullable(this.owner, bookingTrackerSnapshotUpdateInput.owner) &&
        equalsNullable(this.dueDate, bookingTrackerSnapshotUpdateInput.dueDate);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(reason), hashCodeNullable(nextAction), hashCodeNullable(owner), hashCodeNullable(dueDate));
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
    sb.append("class BookingTrackerSnapshotUpdateInput {\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    nextAction: ").append(toIndentedString(nextAction)).append("\n");
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
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

