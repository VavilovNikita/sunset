package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.FutureBookingRow;
import com.sunsetbeach.model.FutureBookingsStatusSummary;
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
 * FutureBookingsReport
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class FutureBookingsReport {

  private String from;

  private String to;

  @Valid
  private List<BookingStatus> statuses = new ArrayList<>();

  @Valid
  private List<@Valid FutureBookingRow> rows = new ArrayList<>();

  @Valid
  private List<@Valid FutureBookingsStatusSummary> summary = new ArrayList<>();

  public FutureBookingsReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FutureBookingsReport(String from, String to, List<BookingStatus> statuses, List<@Valid FutureBookingRow> rows, List<@Valid FutureBookingsStatusSummary> summary) {
    this.from = from;
    this.to = to;
    this.statuses = statuses;
    this.rows = rows;
    this.summary = summary;
  }

  public FutureBookingsReport from(String from) {
    this.from = from;
    return this;
  }

  /**
   * The range actually used (defaults applied).
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

  public FutureBookingsReport to(String to) {
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

  public FutureBookingsReport statuses(List<BookingStatus> statuses) {
    this.statuses = statuses;
    return this;
  }

  public FutureBookingsReport addStatusesItem(BookingStatus statusesItem) {
    if (this.statuses == null) {
      this.statuses = new ArrayList<>();
    }
    this.statuses.add(statusesItem);
    return this;
  }

  /**
   * The statuses actually used (all of `NEW`, `CONFIRMED`, `PAID` when none were asked for).
   * @return statuses
   */
  @NotNull @Valid 
  @JsonProperty("statuses")
  public List<BookingStatus> getStatuses() {
    return statuses;
  }

  public void setStatuses(List<BookingStatus> statuses) {
    this.statuses = statuses;
  }

  public FutureBookingsReport rows(List<@Valid FutureBookingRow> rows) {
    this.rows = rows;
    return this;
  }

  public FutureBookingsReport addRowsItem(FutureBookingRow rowsItem) {
    if (this.rows == null) {
      this.rows = new ArrayList<>();
    }
    this.rows.add(rowsItem);
    return this;
  }

  /**
   * Get rows
   * @return rows
   */
  @NotNull @Valid 
  @JsonProperty("rows")
  public List<@Valid FutureBookingRow> getRows() {
    return rows;
  }

  public void setRows(List<@Valid FutureBookingRow> rows) {
    this.rows = rows;
  }

  public FutureBookingsReport summary(List<@Valid FutureBookingsStatusSummary> summary) {
    this.summary = summary;
    return this;
  }

  public FutureBookingsReport addSummaryItem(FutureBookingsStatusSummary summaryItem) {
    if (this.summary == null) {
      this.summary = new ArrayList<>();
    }
    this.summary.add(summaryItem);
    return this;
  }

  /**
   * One entry per status in `statuses`, in the order `NEW`, `CONFIRMED`, `PAID`; zero counts are listed.
   * @return summary
   */
  @NotNull @Valid 
  @JsonProperty("summary")
  public List<@Valid FutureBookingsStatusSummary> getSummary() {
    return summary;
  }

  public void setSummary(List<@Valid FutureBookingsStatusSummary> summary) {
    this.summary = summary;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FutureBookingsReport futureBookingsReport = (FutureBookingsReport) o;
    return Objects.equals(this.from, futureBookingsReport.from) &&
        Objects.equals(this.to, futureBookingsReport.to) &&
        Objects.equals(this.statuses, futureBookingsReport.statuses) &&
        Objects.equals(this.rows, futureBookingsReport.rows) &&
        Objects.equals(this.summary, futureBookingsReport.summary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, statuses, rows, summary);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FutureBookingsReport {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    statuses: ").append(toIndentedString(statuses)).append("\n");
    sb.append("    rows: ").append(toIndentedString(rows)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
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

