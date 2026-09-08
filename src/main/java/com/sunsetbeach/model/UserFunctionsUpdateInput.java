package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.JobFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PATCH /users/{id}/functions&#x60;. Replaces the full set of job functions - send every function this user should have, not just the one being added or removed. An empty array clears every function. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class UserFunctionsUpdateInput {

  @Valid
  private List<JobFunction> functions = new ArrayList<>();

  public UserFunctionsUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserFunctionsUpdateInput(List<JobFunction> functions) {
    this.functions = functions;
  }

  public UserFunctionsUpdateInput functions(List<JobFunction> functions) {
    this.functions = functions;
    return this;
  }

  public UserFunctionsUpdateInput addFunctionsItem(JobFunction functionsItem) {
    if (this.functions == null) {
      this.functions = new ArrayList<>();
    }
    this.functions.add(functionsItem);
    return this;
  }

  /**
   * Get functions
   * @return functions
   */
  @NotNull @Valid 
  @JsonProperty("functions")
  public List<JobFunction> getFunctions() {
    return functions;
  }

  public void setFunctions(List<JobFunction> functions) {
    this.functions = functions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserFunctionsUpdateInput userFunctionsUpdateInput = (UserFunctionsUpdateInput) o;
    return Objects.equals(this.functions, userFunctionsUpdateInput.functions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(functions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserFunctionsUpdateInput {\n");
    sb.append("    functions: ").append(toIndentedString(functions)).append("\n");
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

