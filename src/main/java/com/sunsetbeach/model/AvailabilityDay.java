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
 * AvailabilityDay
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-17T16:01:20.967720600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class AvailabilityDay {

  private String date;

  private Boolean isBlocked;

  /**
   * null when `isBlocked` is false.
   */
  public enum SourceEnum {
    BOOKING("booking"),
    
    MANUAL("manual");

    private String value;

    SourceEnum(String value) {
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
    public static SourceEnum fromValue(String value) {
      for (SourceEnum b : SourceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      return null;
    }
  }

  private JsonNullable<SourceEnum> source = JsonNullable.<SourceEnum>undefined();

  public AvailabilityDay() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AvailabilityDay(String date, Boolean isBlocked, SourceEnum source) {
    this.date = date;
    this.isBlocked = isBlocked;
    this.source = JsonNullable.of(source);
  }

  public AvailabilityDay date(String date) {
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

  public AvailabilityDay isBlocked(Boolean isBlocked) {
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

  public AvailabilityDay source(SourceEnum source) {
    this.source = JsonNullable.of(source);
    return this;
  }

  /**
   * null when `isBlocked` is false.
   * @return source
   */
  @NotNull 
  @JsonProperty("source")
  public JsonNullable<SourceEnum> getSource() {
    return source;
  }

  public void setSource(JsonNullable<SourceEnum> source) {
    this.source = source;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AvailabilityDay availabilityDay = (AvailabilityDay) o;
    return Objects.equals(this.date, availabilityDay.date) &&
        Objects.equals(this.isBlocked, availabilityDay.isBlocked) &&
        Objects.equals(this.source, availabilityDay.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, isBlocked, source);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AvailabilityDay {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    isBlocked: ").append(toIndentedString(isBlocked)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
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

