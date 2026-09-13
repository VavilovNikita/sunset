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
 * Body of &#x60;POST /roster/entries/{id}/swap&#x60;. The dragged entry is named by the URL; this names the other side.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RosterSwapInput {

  private String otherEntryId;

  public RosterSwapInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterSwapInput(String otherEntryId) {
    this.otherEntryId = otherEntryId;
  }

  public RosterSwapInput otherEntryId(String otherEntryId) {
    this.otherEntryId = otherEntryId;
    return this;
  }

  /**
   * Get otherEntryId
   * @return otherEntryId
   */
  @NotNull 
  @JsonProperty("otherEntryId")
  public String getOtherEntryId() {
    return otherEntryId;
  }

  public void setOtherEntryId(String otherEntryId) {
    this.otherEntryId = otherEntryId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterSwapInput rosterSwapInput = (RosterSwapInput) o;
    return Objects.equals(this.otherEntryId, rosterSwapInput.otherEntryId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(otherEntryId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterSwapInput {\n");
    sb.append("    otherEntryId: ").append(toIndentedString(otherEntryId)).append("\n");
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

