package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
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
 * ShiftCloseInput
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-07T15:38:53.433655500+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class ShiftCloseInput {

  private BigDecimal closingCashCounted;

  private JsonNullable<@Size(max = 1000) String> notes = JsonNullable.<String>undefined();

  public ShiftCloseInput closingCashCounted(BigDecimal closingCashCounted) {
    this.closingCashCounted = closingCashCounted;
    return this;
  }

  /**
   * Get closingCashCounted
   * minimum: 0
   * @return closingCashCounted
   */
  @Valid @DecimalMin("0") 
  @JsonProperty("closingCashCounted")
  public BigDecimal getClosingCashCounted() {
    return closingCashCounted;
  }

  public void setClosingCashCounted(BigDecimal closingCashCounted) {
    this.closingCashCounted = closingCashCounted;
  }

  public ShiftCloseInput notes(String notes) {
    this.notes = JsonNullable.of(notes);
    return this;
  }

  /**
   * Get notes
   * @return notes
   */
  @Size(max = 1000) 
  @JsonProperty("notes")
  public JsonNullable<@Size(max = 1000) String> getNotes() {
    return notes;
  }

  public void setNotes(JsonNullable<String> notes) {
    this.notes = notes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftCloseInput shiftCloseInput = (ShiftCloseInput) o;
    return Objects.equals(this.closingCashCounted, shiftCloseInput.closingCashCounted) &&
        equalsNullable(this.notes, shiftCloseInput.notes);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(closingCashCounted, hashCodeNullable(notes));
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
    sb.append("class ShiftCloseInput {\n");
    sb.append("    closingCashCounted: ").append(toIndentedString(closingCashCounted)).append("\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
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

