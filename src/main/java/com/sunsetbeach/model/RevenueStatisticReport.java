package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RevenueStatisticRow;
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
 * &#x60;GET /reports/revenue-statistic&#x60; - see that operation for what counts and how VAT is computed.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RevenueStatisticReport {

  private String from;

  private String to;

  private String vatRate;

  @Valid
  private List<@Valid RevenueStatisticRow> rows = new ArrayList<>();

  private RevenueStatisticRow total;

  public RevenueStatisticReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RevenueStatisticReport(String from, String to, String vatRate, List<@Valid RevenueStatisticRow> rows, RevenueStatisticRow total) {
    this.from = from;
    this.to = to;
    this.vatRate = vatRate;
    this.rows = rows;
    this.total = total;
  }

  public RevenueStatisticReport from(String from) {
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

  public RevenueStatisticReport to(String to) {
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

  public RevenueStatisticReport vatRate(String vatRate) {
    this.vatRate = vatRate;
    return this;
  }

  /**
   * The VAT rate (percent, two decimals, e.g. \"7.00\") every figure in this response was computed with - the stored rate at the moment of the request.
   * @return vatRate
   */
  @NotNull 
  @JsonProperty("vatRate")
  public String getVatRate() {
    return vatRate;
  }

  public void setVatRate(String vatRate) {
    this.vatRate = vatRate;
  }

  public RevenueStatisticReport rows(List<@Valid RevenueStatisticRow> rows) {
    this.rows = rows;
    return this;
  }

  public RevenueStatisticReport addRowsItem(RevenueStatisticRow rowsItem) {
    if (this.rows == null) {
      this.rows = new ArrayList<>();
    }
    this.rows.add(rowsItem);
    return this;
  }

  /**
   * Always ROOM, FNB, SPA, in that order.
   * @return rows
   */
  @NotNull @Valid 
  @JsonProperty("rows")
  public List<@Valid RevenueStatisticRow> getRows() {
    return rows;
  }

  public void setRows(List<@Valid RevenueStatisticRow> rows) {
    this.rows = rows;
  }

  public RevenueStatisticReport total(RevenueStatisticRow total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull @Valid 
  @JsonProperty("total")
  public RevenueStatisticRow getTotal() {
    return total;
  }

  public void setTotal(RevenueStatisticRow total) {
    this.total = total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RevenueStatisticReport revenueStatisticReport = (RevenueStatisticReport) o;
    return Objects.equals(this.from, revenueStatisticReport.from) &&
        Objects.equals(this.to, revenueStatisticReport.to) &&
        Objects.equals(this.vatRate, revenueStatisticReport.vatRate) &&
        Objects.equals(this.rows, revenueStatisticReport.rows) &&
        Objects.equals(this.total, revenueStatisticReport.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, vatRate, rows, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RevenueStatisticReport {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    vatRate: ").append(toIndentedString(vatRate)).append("\n");
    sb.append("    rows: ").append(toIndentedString(rows)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
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

