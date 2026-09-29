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
 * One side of an entry - exactly one of &#x60;debit&#x60;/&#x60;credit&#x60; is non-zero. Money is a decimal string with two decimals.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class JournalLine {

  private String accountCode;

  private String accountName;

  private String debit;

  private String credit;

  public JournalLine() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JournalLine(String accountCode, String accountName, String debit, String credit) {
    this.accountCode = accountCode;
    this.accountName = accountName;
    this.debit = debit;
    this.credit = credit;
  }

  public JournalLine accountCode(String accountCode) {
    this.accountCode = accountCode;
    return this;
  }

  /**
   * Get accountCode
   * @return accountCode
   */
  @NotNull 
  @JsonProperty("accountCode")
  public String getAccountCode() {
    return accountCode;
  }

  public void setAccountCode(String accountCode) {
    this.accountCode = accountCode;
  }

  public JournalLine accountName(String accountName) {
    this.accountName = accountName;
    return this;
  }

  /**
   * Get accountName
   * @return accountName
   */
  @NotNull 
  @JsonProperty("accountName")
  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public JournalLine debit(String debit) {
    this.debit = debit;
    return this;
  }

  /**
   * Get debit
   * @return debit
   */
  @NotNull 
  @JsonProperty("debit")
  public String getDebit() {
    return debit;
  }

  public void setDebit(String debit) {
    this.debit = debit;
  }

  public JournalLine credit(String credit) {
    this.credit = credit;
    return this;
  }

  /**
   * Get credit
   * @return credit
   */
  @NotNull 
  @JsonProperty("credit")
  public String getCredit() {
    return credit;
  }

  public void setCredit(String credit) {
    this.credit = credit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JournalLine journalLine = (JournalLine) o;
    return Objects.equals(this.accountCode, journalLine.accountCode) &&
        Objects.equals(this.accountName, journalLine.accountName) &&
        Objects.equals(this.debit, journalLine.debit) &&
        Objects.equals(this.credit, journalLine.credit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountCode, accountName, debit, credit);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JournalLine {\n");
    sb.append("    accountCode: ").append(toIndentedString(accountCode)).append("\n");
    sb.append("    accountName: ").append(toIndentedString(accountName)).append("\n");
    sb.append("    debit: ").append(toIndentedString(debit)).append("\n");
    sb.append("    credit: ").append(toIndentedString(credit)).append("\n");
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

