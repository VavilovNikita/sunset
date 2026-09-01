package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;POST /bookings/{id}/undo-relocation&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RelocationUndoInput {

  private String splitDate;

  public RelocationUndoInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RelocationUndoInput(String splitDate) {
    this.splitDate = splitDate;
  }

  public RelocationUndoInput splitDate(String splitDate) {
    this.splitDate = splitDate;
    return this;
  }

  /**
   * The boundary date between the two segments to merge - some segment's `checkOut` equal to the next segment's `checkIn`.
   * @return splitDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("splitDate")
  public String getSplitDate() {
    return splitDate;
  }

  public void setSplitDate(String splitDate) {
    this.splitDate = splitDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RelocationUndoInput relocationUndoInput = (RelocationUndoInput) o;
    return Objects.equals(this.splitDate, relocationUndoInput.splitDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(splitDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RelocationUndoInput {\n");
    sb.append("    splitDate: ").append(toIndentedString(splitDate)).append("\n");
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

