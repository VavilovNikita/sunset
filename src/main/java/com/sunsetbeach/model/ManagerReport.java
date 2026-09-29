package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.ManagerReportDay;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * &#x60;GET /reports/manager&#x60; - see that operation for how each figure is computed.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ManagerReport {

  private String date;

  private String lastYearDate;

  private ManagerReportDay today;

  private ManagerReportDay lastYear;

  public ManagerReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ManagerReport(String date, String lastYearDate, ManagerReportDay today, ManagerReportDay lastYear) {
    this.date = date;
    this.lastYearDate = lastYearDate;
    this.today = today;
    this.lastYear = lastYear;
  }

  public ManagerReport date(String date) {
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

  public ManagerReport lastYearDate(String lastYearDate) {
    this.lastYearDate = lastYearDate;
    return this;
  }

  /**
   * Get lastYearDate
   * @return lastYearDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("lastYearDate")
  public String getLastYearDate() {
    return lastYearDate;
  }

  public void setLastYearDate(String lastYearDate) {
    this.lastYearDate = lastYearDate;
  }

  public ManagerReport today(ManagerReportDay today) {
    this.today = today;
    return this;
  }

  /**
   * Get today
   * @return today
   */
  @NotNull @Valid 
  @JsonProperty("today")
  public ManagerReportDay getToday() {
    return today;
  }

  public void setToday(ManagerReportDay today) {
    this.today = today;
  }

  public ManagerReport lastYear(ManagerReportDay lastYear) {
    this.lastYear = lastYear;
    return this;
  }

  /**
   * Get lastYear
   * @return lastYear
   */
  @NotNull @Valid 
  @JsonProperty("lastYear")
  public ManagerReportDay getLastYear() {
    return lastYear;
  }

  public void setLastYear(ManagerReportDay lastYear) {
    this.lastYear = lastYear;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ManagerReport managerReport = (ManagerReport) o;
    return Objects.equals(this.date, managerReport.date) &&
        Objects.equals(this.lastYearDate, managerReport.lastYearDate) &&
        Objects.equals(this.today, managerReport.today) &&
        Objects.equals(this.lastYear, managerReport.lastYear);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, lastYearDate, today, lastYear);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ManagerReport {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    lastYearDate: ").append(toIndentedString(lastYearDate)).append("\n");
    sb.append("    today: ").append(toIndentedString(today)).append("\n");
    sb.append("    lastYear: ").append(toIndentedString(lastYear)).append("\n");
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

