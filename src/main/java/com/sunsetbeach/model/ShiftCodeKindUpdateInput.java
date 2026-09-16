package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.ShiftCodeKind;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PATCH /shift-codes/{id}/kind&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftCodeKindUpdateInput {

  private ShiftCodeKind kind;

  public ShiftCodeKindUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftCodeKindUpdateInput(ShiftCodeKind kind) {
    this.kind = kind;
  }

  public ShiftCodeKindUpdateInput kind(ShiftCodeKind kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Get kind
   * @return kind
   */
  @NotNull @Valid 
  @JsonProperty("kind")
  public ShiftCodeKind getKind() {
    return kind;
  }

  public void setKind(ShiftCodeKind kind) {
    this.kind = kind;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftCodeKindUpdateInput shiftCodeKindUpdateInput = (ShiftCodeKindUpdateInput) o;
    return Objects.equals(this.kind, shiftCodeKindUpdateInput.kind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShiftCodeKindUpdateInput {\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
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

