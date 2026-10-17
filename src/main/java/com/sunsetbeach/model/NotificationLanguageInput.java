package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.NotificationLanguage;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PUT /notifications/staff/{userId}/language&#x60;. &#x60;language&#x60; omitted or null clears it.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NotificationLanguageInput {

  private NotificationLanguage language;

  public NotificationLanguageInput language(NotificationLanguage language) {
    this.language = language;
    return this;
  }

  /**
   * Get language
   * @return language
   */
  @Valid 
  @JsonProperty("language")
  public NotificationLanguage getLanguage() {
    return language;
  }

  public void setLanguage(NotificationLanguage language) {
    this.language = language;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationLanguageInput notificationLanguageInput = (NotificationLanguageInput) o;
    return Objects.equals(this.language, notificationLanguageInput.language);
  }

  @Override
  public int hashCode() {
    return Objects.hash(language);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationLanguageInput {\n");
    sb.append("    language: ").append(toIndentedString(language)).append("\n");
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

