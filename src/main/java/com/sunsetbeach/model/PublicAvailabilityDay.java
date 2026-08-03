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
 * Like &#x60;AvailabilityDay&#x60; but without &#x60;source&#x60; - guests aren&#39;t told whether a blocked day is a real booking or a manual staff block.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-03T20:50:04.328032600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class PublicAvailabilityDay {

  private String date;

  private Boolean isBlocked;

  public PublicAvailabilityDay() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PublicAvailabilityDay(String date, Boolean isBlocked) {
    this.date = date;
    this.isBlocked = isBlocked;
  }

  public PublicAvailabilityDay date(String date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("date")
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public PublicAvailabilityDay isBlocked(Boolean isBlocked) {
    this.isBlocked = isBlocked;
    return this;
  }

  /**
   * Get isBlocked
   * @return isBlocked
   */
  @NotNull 
  @JsonProperty("isBlocked")
  public Boolean getIsBlocked() {
    return isBlocked;
  }

  public void setIsBlocked(Boolean isBlocked) {
    this.isBlocked = isBlocked;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PublicAvailabilityDay publicAvailabilityDay = (PublicAvailabilityDay) o;
    return Objects.equals(this.date, publicAvailabilityDay.date) &&
        Objects.equals(this.isBlocked, publicAvailabilityDay.isBlocked);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, isBlocked);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublicAvailabilityDay {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    isBlocked: ").append(toIndentedString(isBlocked)).append("\n");
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

