package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.TrialBalanceRow;
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
 * &#x60;GET /reports/trial-balance&#x60; - see that operation, including why it does not reconcile with Z410 for pre-ledger periods.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TrialBalanceReport {

  private String asOf;

  @Valid
  private List<@Valid TrialBalanceRow> rows = new ArrayList<>();

  private String totalDebit;

  private String totalCredit;

  private Boolean balanced;

  public TrialBalanceReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TrialBalanceReport(String asOf, List<@Valid TrialBalanceRow> rows, String totalDebit, String totalCredit, Boolean balanced) {
    this.asOf = asOf;
    this.rows = rows;
    this.totalDebit = totalDebit;
    this.totalCredit = totalCredit;
    this.balanced = balanced;
  }

  public TrialBalanceReport asOf(String asOf) {
    this.asOf = asOf;
    return this;
  }

  /**
   * Get asOf
   * @return asOf
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("asOf")
  public String getAsOf() {
    return asOf;
  }

  public void setAsOf(String asOf) {
    this.asOf = asOf;
  }

  public TrialBalanceReport rows(List<@Valid TrialBalanceRow> rows) {
    this.rows = rows;
    return this;
  }

  public TrialBalanceReport addRowsItem(TrialBalanceRow rowsItem) {
    if (this.rows == null) {
      this.rows = new ArrayList<>();
    }
    this.rows.add(rowsItem);
    return this;
  }

  /**
   * One row per account in the chart, ordered by code.
   * @return rows
   */
  @NotNull @Valid 
  @JsonProperty("rows")
  public List<@Valid TrialBalanceRow> getRows() {
    return rows;
  }

  public void setRows(List<@Valid TrialBalanceRow> rows) {
    this.rows = rows;
  }

  public TrialBalanceReport totalDebit(String totalDebit) {
    this.totalDebit = totalDebit;
    return this;
  }

  /**
   * Sum of every row's `totalDebit`.
   * @return totalDebit
   */
  @NotNull 
  @JsonProperty("totalDebit")
  public String getTotalDebit() {
    return totalDebit;
  }

  public void setTotalDebit(String totalDebit) {
    this.totalDebit = totalDebit;
  }

  public TrialBalanceReport totalCredit(String totalCredit) {
    this.totalCredit = totalCredit;
    return this;
  }

  /**
   * Sum of every row's `totalCredit`. Equal to `totalDebit` unless the posting logic has a bug.
   * @return totalCredit
   */
  @NotNull 
  @JsonProperty("totalCredit")
  public String getTotalCredit() {
    return totalCredit;
  }

  public void setTotalCredit(String totalCredit) {
    this.totalCredit = totalCredit;
  }

  public TrialBalanceReport balanced(Boolean balanced) {
    this.balanced = balanced;
    return this;
  }

  /**
   * `totalDebit == totalCredit`.
   * @return balanced
   */
  @NotNull 
  @JsonProperty("balanced")
  public Boolean getBalanced() {
    return balanced;
  }

  public void setBalanced(Boolean balanced) {
    this.balanced = balanced;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrialBalanceReport trialBalanceReport = (TrialBalanceReport) o;
    return Objects.equals(this.asOf, trialBalanceReport.asOf) &&
        Objects.equals(this.rows, trialBalanceReport.rows) &&
        Objects.equals(this.totalDebit, trialBalanceReport.totalDebit) &&
        Objects.equals(this.totalCredit, trialBalanceReport.totalCredit) &&
        Objects.equals(this.balanced, trialBalanceReport.balanced);
  }

  @Override
  public int hashCode() {
    return Objects.hash(asOf, rows, totalDebit, totalCredit, balanced);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TrialBalanceReport {\n");
    sb.append("    asOf: ").append(toIndentedString(asOf)).append("\n");
    sb.append("    rows: ").append(toIndentedString(rows)).append("\n");
    sb.append("    totalDebit: ").append(toIndentedString(totalDebit)).append("\n");
    sb.append("    totalCredit: ").append(toIndentedString(totalCredit)).append("\n");
    sb.append("    balanced: ").append(toIndentedString(balanced)).append("\n");
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

