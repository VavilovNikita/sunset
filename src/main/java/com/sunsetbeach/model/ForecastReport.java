package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.ForecastDay;
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
 * &#x60;GET /reports/forecast&#x60; - see that operation for how each figure is computed.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ForecastReport {

  private String from;

  private String to;

  @Valid
  private List<@Valid ForecastDay> days = new ArrayList<>();

  public ForecastReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ForecastReport(String from, String to, List<@Valid ForecastDay> days) {
    this.from = from;
    this.to = to;
    this.days = days;
  }

  public ForecastReport from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public ForecastReport to(String to) {
    this.to = to;
    return this;
  }

  /**
   * Get to
   * @return to
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public ForecastReport days(List<@Valid ForecastDay> days) {
    this.days = days;
    return this;
  }

  public ForecastReport addDaysItem(ForecastDay daysItem) {
    if (this.days == null) {
      this.days = new ArrayList<>();
    }
    this.days.add(daysItem);
    return this;
  }

  /**
   * One entry per date in the range, in date order.
   * @return days
   */
  @NotNull @Valid 
  @JsonProperty("days")
  public List<@Valid ForecastDay> getDays() {
    return days;
  }

  public void setDays(List<@Valid ForecastDay> days) {
    this.days = days;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForecastReport forecastReport = (ForecastReport) o;
    return Objects.equals(this.from, forecastReport.from) &&
        Objects.equals(this.to, forecastReport.to) &&
        Objects.equals(this.days, forecastReport.days);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, days);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForecastReport {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    days: ").append(toIndentedString(days)).append("\n");
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

