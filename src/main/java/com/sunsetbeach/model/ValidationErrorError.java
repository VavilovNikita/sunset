package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Output of Zod&#39;s &#x60;error.flatten()&#x60;.
 */

@JsonTypeName("ValidationError_error")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-17T16:01:20.967720600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class ValidationErrorError {

  @Valid
  private List<String> formErrors = new ArrayList<>();

  @Valid
  private Map<String, List<String>> fieldErrors = new HashMap<>();

  public ValidationErrorError formErrors(List<String> formErrors) {
    this.formErrors = formErrors;
    return this;
  }

  public ValidationErrorError addFormErrorsItem(String formErrorsItem) {
    if (this.formErrors == null) {
      this.formErrors = new ArrayList<>();
    }
    this.formErrors.add(formErrorsItem);
    return this;
  }

  /**
   * Get formErrors
   * @return formErrors
   */
  
  @JsonProperty("formErrors")
  public List<String> getFormErrors() {
    return formErrors;
  }

  public void setFormErrors(List<String> formErrors) {
    this.formErrors = formErrors;
  }

  public ValidationErrorError fieldErrors(Map<String, List<String>> fieldErrors) {
    this.fieldErrors = fieldErrors;
    return this;
  }

  public ValidationErrorError putFieldErrorsItem(String key, List<String> fieldErrorsItem) {
    if (this.fieldErrors == null) {
      this.fieldErrors = new HashMap<>();
    }
    this.fieldErrors.put(key, fieldErrorsItem);
    return this;
  }

  /**
   * Get fieldErrors
   * @return fieldErrors
   */
  @Valid 
  @JsonProperty("fieldErrors")
  public Map<String, List<String>> getFieldErrors() {
    return fieldErrors;
  }

  public void setFieldErrors(Map<String, List<String>> fieldErrors) {
    this.fieldErrors = fieldErrors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValidationErrorError validationErrorError = (ValidationErrorError) o;
    return Objects.equals(this.formErrors, validationErrorError.formErrors) &&
        Objects.equals(this.fieldErrors, validationErrorError.fieldErrors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(formErrors, fieldErrors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValidationErrorError {\n");
    sb.append("    formErrors: ").append(toIndentedString(formErrors)).append("\n");
    sb.append("    fieldErrors: ").append(toIndentedString(fieldErrors)).append("\n");
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

