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
 * Body of &#x60;PATCH /shift-codes/{id}/display-color&#x60;. &#x60;displayColor&#x60; may be omitted or sent explicitly &#x60;null&#x60; to clear a code back to unset - the grid&#39;s neutral fallback applies again - so, unlike &#x60;ShiftCodeKindUpdateInput.kind&#x60;, it is deliberately left out of &#x60;required&#x60;: a &#x60;nullable: true&#x60; property that&#39;s also &#x60;required&#x60; generates a &#x60;@NotNull&#x60; that rejects the very null this endpoint needs to accept (the codegen&#39;s &#x60;JsonNullable&lt;T&gt;&#x60; wrapper unwraps before Bean Validation sees it - see CLAUDE.md&#39;s Code generation section). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftCodeDisplayColorUpdateInput {

  private JsonNullable<@Pattern(regexp = "^#[0-9a-fA-F]{6}$") String> displayColor = JsonNullable.<String>undefined();

  public ShiftCodeDisplayColorUpdateInput displayColor(String displayColor) {
    this.displayColor = JsonNullable.of(displayColor);
    return this;
  }

  /**
   * Get displayColor
   * @return displayColor
   */
  @Pattern(regexp = "^#[0-9a-fA-F]{6}$") 
  @JsonProperty("displayColor")
  public JsonNullable<@Pattern(regexp = "^#[0-9a-fA-F]{6}$") String> getDisplayColor() {
    return displayColor;
  }

  public void setDisplayColor(JsonNullable<String> displayColor) {
    this.displayColor = displayColor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftCodeDisplayColorUpdateInput shiftCodeDisplayColorUpdateInput = (ShiftCodeDisplayColorUpdateInput) o;
    return equalsNullable(this.displayColor, shiftCodeDisplayColorUpdateInput.displayColor);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(displayColor));
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
    sb.append("class ShiftCodeDisplayColorUpdateInput {\n");
    sb.append("    displayColor: ").append(toIndentedString(displayColor)).append("\n");
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

