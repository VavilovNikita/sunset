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
 * One row of &#x60;GET /shifts&#x60; - the same fields as &#x60;ShiftSummary&#x60; plus what a manager reviewing a period actually needs and would otherwise have to look up separately: &#x60;openedByEmail&#x60;/&#x60;closedByEmail&#x60; (so this list doesn&#39;t require cross-referencing user ids against &#x60;GET /users&#x60;) and the reconciliation numbers themselves, &#x60;expectedCash&#x60;/&#x60;discrepancy&#x60; - the same arithmetic as the printed Z-report and the CSV export (&#x60;ShiftService&#x60;): &#x60;expectedCash&#x60; &#x3D; &#x60;openingCashFloat&#x60; (0 if unset) + this shift&#39;s cash payments; &#x60;discrepancy&#x60; &#x3D; &#x60;closingCashCounted&#x60; - &#x60;expectedCash&#x60;, positive meaning more cash counted than expected, negative meaning less. &#x60;discrepancy&#x60; is &#x60;null&#x60; exactly when &#x60;closingCashCounted&#x60; is - an open or never-recounted shift has nothing to compare. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftListItem {

  private String id;

  private String openedByUserId;

  private String openedByEmail;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime openedAt;

  private JsonNullable<String> closedByUserId = JsonNullable.<String>undefined();

  private JsonNullable<String> closedByEmail = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> closedAt = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> openingCashFloat = JsonNullable.<String>undefined();

  private JsonNullable<String> closingCashCounted = JsonNullable.<String>undefined();

  private ShiftStatus status;

  private ShiftTotals totals;

  private String expectedCash;

  private JsonNullable<String> discrepancy = JsonNullable.<String>undefined();

  public ShiftListItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftListItem(String id, String openedByUserId, String openedByEmail, OffsetDateTime openedAt, String closedByUserId, String closedByEmail, OffsetDateTime closedAt, String openingCashFloat, String closingCashCounted, ShiftStatus status, ShiftTotals totals, String expectedCash, String discrepancy) {
    this.id = id;
    this.openedByUserId = openedByUserId;
    this.openedByEmail = openedByEmail;
    this.openedAt = openedAt;
    this.closedByUserId = JsonNullable.of(closedByUserId);
    this.closedByEmail = JsonNullable.of(closedByEmail);
    this.closedAt = JsonNullable.of(closedAt);
    this.openingCashFloat = JsonNullable.of(openingCashFloat);
    this.closingCashCounted = JsonNullable.of(closingCashCounted);
    this.status = status;
    this.totals = totals;
    this.expectedCash = expectedCash;
    this.discrepancy = JsonNullable.of(discrepancy);
  }

  public ShiftListItem id(String id) {
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

  public ShiftListItem openedByUserId(String openedByUserId) {
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

  public ShiftListItem openedByEmail(String openedByEmail) {
    this.openedByEmail = openedByEmail;
    return this;
  }

  /**
   * Get openedByEmail
   * @return openedByEmail
   */
  @NotNull 
  @JsonProperty("openedByEmail")
  public String getOpenedByEmail() {
    return openedByEmail;
  }

  public void setOpenedByEmail(String openedByEmail) {
    this.openedByEmail = openedByEmail;
  }

  public ShiftListItem openedAt(OffsetDateTime openedAt) {
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

  public ShiftListItem closedByUserId(String closedByUserId) {
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

  public ShiftListItem closedByEmail(String closedByEmail) {
    this.closedByEmail = JsonNullable.of(closedByEmail);
    return this;
  }

  /**
   * Get closedByEmail
   * @return closedByEmail
   */
  @NotNull 
  @JsonProperty("closedByEmail")
  public JsonNullable<String> getClosedByEmail() {
    return closedByEmail;
  }

  public void setClosedByEmail(JsonNullable<String> closedByEmail) {
    this.closedByEmail = closedByEmail;
  }

  public ShiftListItem closedAt(OffsetDateTime closedAt) {
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

  public ShiftListItem openingCashFloat(String openingCashFloat) {
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

  public ShiftListItem closingCashCounted(String closingCashCounted) {
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

  public ShiftListItem status(ShiftStatus status) {
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

  public ShiftListItem totals(ShiftTotals totals) {
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

  public ShiftListItem expectedCash(String expectedCash) {
    this.expectedCash = expectedCash;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string.
   * @return expectedCash
   */
  @NotNull 
  @JsonProperty("expectedCash")
  public String getExpectedCash() {
    return expectedCash;
  }

  public void setExpectedCash(String expectedCash) {
    this.expectedCash = expectedCash;
  }

  public ShiftListItem discrepancy(String discrepancy) {
    this.discrepancy = JsonNullable.of(discrepancy);
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string, or null if closingCashCounted is null.
   * @return discrepancy
   */
  @NotNull 
  @JsonProperty("discrepancy")
  public JsonNullable<String> getDiscrepancy() {
    return discrepancy;
  }

  public void setDiscrepancy(JsonNullable<String> discrepancy) {
    this.discrepancy = discrepancy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftListItem shiftListItem = (ShiftListItem) o;
    return Objects.equals(this.id, shiftListItem.id) &&
        Objects.equals(this.openedByUserId, shiftListItem.openedByUserId) &&
        Objects.equals(this.openedByEmail, shiftListItem.openedByEmail) &&
        Objects.equals(this.openedAt, shiftListItem.openedAt) &&
        Objects.equals(this.closedByUserId, shiftListItem.closedByUserId) &&
        Objects.equals(this.closedByEmail, shiftListItem.closedByEmail) &&
        Objects.equals(this.closedAt, shiftListItem.closedAt) &&
        Objects.equals(this.openingCashFloat, shiftListItem.openingCashFloat) &&
        Objects.equals(this.closingCashCounted, shiftListItem.closingCashCounted) &&
        Objects.equals(this.status, shiftListItem.status) &&
        Objects.equals(this.totals, shiftListItem.totals) &&
        Objects.equals(this.expectedCash, shiftListItem.expectedCash) &&
        Objects.equals(this.discrepancy, shiftListItem.discrepancy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, openedByUserId, openedByEmail, openedAt, closedByUserId, closedByEmail, closedAt, openingCashFloat, closingCashCounted, status, totals, expectedCash, discrepancy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShiftListItem {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    openedByUserId: ").append(toIndentedString(openedByUserId)).append("\n");
    sb.append("    openedByEmail: ").append(toIndentedString(openedByEmail)).append("\n");
    sb.append("    openedAt: ").append(toIndentedString(openedAt)).append("\n");
    sb.append("    closedByUserId: ").append(toIndentedString(closedByUserId)).append("\n");
    sb.append("    closedByEmail: ").append(toIndentedString(closedByEmail)).append("\n");
    sb.append("    closedAt: ").append(toIndentedString(closedAt)).append("\n");
    sb.append("    openingCashFloat: ").append(toIndentedString(openingCashFloat)).append("\n");
    sb.append("    closingCashCounted: ").append(toIndentedString(closingCashCounted)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    totals: ").append(toIndentedString(totals)).append("\n");
    sb.append("    expectedCash: ").append(toIndentedString(expectedCash)).append("\n");
    sb.append("    discrepancy: ").append(toIndentedString(discrepancy)).append("\n");
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

