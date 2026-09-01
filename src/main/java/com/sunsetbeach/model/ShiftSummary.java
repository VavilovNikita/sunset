package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.ShiftTotals;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ShiftSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftSummary {

  private String id;

  private String openedByUserId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime openedAt;

  private JsonNullable<String> closedByUserId = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> closedAt = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> openingCashFloat = JsonNullable.<String>undefined();

  private JsonNullable<String> closingCashCounted = JsonNullable.<String>undefined();

  private ShiftStatus status;

  private JsonNullable<String> notes = JsonNullable.<String>undefined();

  private ShiftTotals totals;

  public ShiftSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftSummary(String id, String openedByUserId, OffsetDateTime openedAt, String closedByUserId, OffsetDateTime closedAt, String openingCashFloat, String closingCashCounted, ShiftStatus status, String notes, ShiftTotals totals) {
    this.id = id;
    this.openedByUserId = openedByUserId;
    this.openedAt = openedAt;
    this.closedByUserId = JsonNullable.of(closedByUserId);
    this.closedAt = JsonNullable.of(closedAt);
    this.openingCashFloat = JsonNullable.of(openingCashFloat);
    this.closingCashCounted = JsonNullable.of(closingCashCounted);
    this.status = status;
    this.notes = JsonNullable.of(notes);
    this.totals = totals;
  }

  public ShiftSummary id(String id) {
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

  public ShiftSummary openedByUserId(String openedByUserId) {
    this.openedByUserId = openedByUserId;
    return this;
  }

  /**
   * Get openedByUserId
   * @return openedByUserId
   */
  @NotNull 
  @JsonProperty("openedByUserId")
  public String getOpenedByUserId() {
    return openedByUserId;
  }

  public void setOpenedByUserId(String openedByUserId) {
    this.openedByUserId = openedByUserId;
  }

  public ShiftSummary openedAt(OffsetDateTime openedAt) {
    this.openedAt = openedAt;
    return this;
  }

  /**
   * Get openedAt
   * @return openedAt
   */
  @NotNull @Valid 
  @JsonProperty("openedAt")
  public OffsetDateTime getOpenedAt() {
    return openedAt;
  }

  public void setOpenedAt(OffsetDateTime openedAt) {
    this.openedAt = openedAt;
  }

  public ShiftSummary closedByUserId(String closedByUserId) {
    this.closedByUserId = JsonNullable.of(closedByUserId);
    return this;
  }

  /**
   * Get closedByUserId
   * @return closedByUserId
   */
  @NotNull 
  @JsonProperty("closedByUserId")
  public JsonNullable<String> getClosedByUserId() {
    return closedByUserId;
  }

  public void setClosedByUserId(JsonNullable<String> closedByUserId) {
    this.closedByUserId = closedByUserId;
  }

  public ShiftSummary closedAt(OffsetDateTime closedAt) {
    this.closedAt = JsonNullable.of(closedAt);
    return this;
  }

  /**
   * Get closedAt
   * @return closedAt
   */
  @NotNull @Valid 
  @JsonProperty("closedAt")
  public JsonNullable<OffsetDateTime> getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(JsonNullable<OffsetDateTime> closedAt) {
    this.closedAt = closedAt;
  }

  public ShiftSummary openingCashFloat(String openingCashFloat) {
    this.openingCashFloat = JsonNullable.of(openingCashFloat);
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return openingCashFloat
   */
  @NotNull 
  @JsonProperty("openingCashFloat")
  public JsonNullable<String> getOpeningCashFloat() {
    return openingCashFloat;
  }

  public void setOpeningCashFloat(JsonNullable<String> openingCashFloat) {
    this.openingCashFloat = openingCashFloat;
  }

  public ShiftSummary closingCashCounted(String closingCashCounted) {
    this.closingCashCounted = JsonNullable.of(closingCashCounted);
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return closingCashCounted
   */
  @NotNull 
  @JsonProperty("closingCashCounted")
  public JsonNullable<String> getClosingCashCounted() {
    return closingCashCounted;
  }

  public void setClosingCashCounted(JsonNullable<String> closingCashCounted) {
    this.closingCashCounted = closingCashCounted;
  }

  public ShiftSummary status(ShiftStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public ShiftStatus getStatus() {
    return status;
  }

  public void setStatus(ShiftStatus status) {
    this.status = status;
  }

  public ShiftSummary notes(String notes) {
    this.notes = JsonNullable.of(notes);
    return this;
  }

  /**
   * Get notes
   * @return notes
   */
  @NotNull 
  @JsonProperty("notes")
  public JsonNullable<String> getNotes() {
    return notes;
  }

  public void setNotes(JsonNullable<String> notes) {
    this.notes = notes;
  }

  public ShiftSummary totals(ShiftTotals totals) {
    this.totals = totals;
    return this;
  }

  /**
   * Get totals
   * @return totals
   */
  @NotNull @Valid 
  @JsonProperty("totals")
  public ShiftTotals getTotals() {
    return totals;
  }

  public void setTotals(ShiftTotals totals) {
    this.totals = totals;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftSummary shiftSummary = (ShiftSummary) o;
    return Objects.equals(this.id, shiftSummary.id) &&
        Objects.equals(this.openedByUserId, shiftSummary.openedByUserId) &&
        Objects.equals(this.openedAt, shiftSummary.openedAt) &&
        Objects.equals(this.closedByUserId, shiftSummary.closedByUserId) &&
        Objects.equals(this.closedAt, shiftSummary.closedAt) &&
        Objects.equals(this.openingCashFloat, shiftSummary.openingCashFloat) &&
        Objects.equals(this.closingCashCounted, shiftSummary.closingCashCounted) &&
        Objects.equals(this.status, shiftSummary.status) &&
        Objects.equals(this.notes, shiftSummary.notes) &&
        Objects.equals(this.totals, shiftSummary.totals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, openedByUserId, openedAt, closedByUserId, closedAt, openingCashFloat, closingCashCounted, status, notes, totals);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShiftSummary {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    openedByUserId: ").append(toIndentedString(openedByUserId)).append("\n");
    sb.append("    openedAt: ").append(toIndentedString(openedAt)).append("\n");
    sb.append("    closedByUserId: ").append(toIndentedString(closedByUserId)).append("\n");
    sb.append("    closedAt: ").append(toIndentedString(closedAt)).append("\n");
    sb.append("    openingCashFloat: ").append(toIndentedString(openingCashFloat)).append("\n");
    sb.append("    closingCashCounted: ").append(toIndentedString(closingCashCounted)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
    sb.append("    totals: ").append(toIndentedString(totals)).append("\n");
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

