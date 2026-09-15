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
 * A cell (or, with &#x60;cellRef&#x60; absent, the sheet as a whole) that could not be read, and why. Blocks commit while any exist.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterImportIssue {

  private JsonNullable<String> cellRef = JsonNullable.<String>undefined();

  private String message;

  public RosterImportIssue() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterImportIssue(String message) {
    this.message = message;
  }

  public RosterImportIssue cellRef(String cellRef) {
    this.cellRef = JsonNullable.of(cellRef);
    return this;
  }

  /**
   * Get cellRef
   * @return cellRef
   */
  
  @JsonProperty("cellRef")
  public JsonNullable<String> getCellRef() {
    return cellRef;
  }

  public void setCellRef(JsonNullable<String> cellRef) {
    this.cellRef = cellRef;
  }

  public RosterImportIssue message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @NotNull 
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterImportIssue rosterImportIssue = (RosterImportIssue) o;
    return equalsNullable(this.cellRef, rosterImportIssue.cellRef) &&
        Objects.equals(this.message, rosterImportIssue.message);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(cellRef), message);
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
    sb.append("class RosterImportIssue {\n");
    sb.append("    cellRef: ").append(toIndentedString(cellRef)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

