package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ShiftChangeDecisionInput
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftChangeDecisionInput {

  /**
   * Gets or Sets decision
   */
  public enum DecisionEnum {
    APPROVE("APPROVE"),
    
    REJECT("REJECT");

    private String value;

    DecisionEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static DecisionEnum fromValue(String value) {
      for (DecisionEnum b : DecisionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private DecisionEnum decision;

  private String comment;

  public ShiftChangeDecisionInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftChangeDecisionInput(DecisionEnum decision) {
    this.decision = decision;
  }

  public ShiftChangeDecisionInput decision(DecisionEnum decision) {
    this.decision = decision;
    return this;
  }

  /**
   * Get decision
   * @return decision
   */
  @NotNull 
  @JsonProperty("decision")
  public DecisionEnum getDecision() {
    return decision;
  }

  public void setDecision(DecisionEnum decision) {
    this.decision = decision;
  }

  public ShiftChangeDecisionInput comment(String comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Shown to the employee with the decision.
   * @return comment
   */
  @Size(max = 300) 
  @JsonProperty("comment")
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftChangeDecisionInput shiftChangeDecisionInput = (ShiftChangeDecisionInput) o;
    return Objects.equals(this.decision, shiftChangeDecisionInput.decision) &&
        Objects.equals(this.comment, shiftChangeDecisionInput.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(decision, comment);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShiftChangeDecisionInput {\n");
    sb.append("    decision: ").append(toIndentedString(decision)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
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

