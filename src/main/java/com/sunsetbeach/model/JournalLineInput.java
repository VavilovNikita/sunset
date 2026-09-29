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
 * Give exactly one of &#x60;debit&#x60;/&#x60;credit&#x60;, a decimal string greater than zero with at most two decimals.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class JournalLineInput {

  private String accountCode;

  private String debit;

  private String credit;

  public JournalLineInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JournalLineInput(String accountCode) {
    this.accountCode = accountCode;
  }

  public JournalLineInput accountCode(String accountCode) {
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

  public JournalLineInput debit(String debit) {
    this.debit = debit;
    return this;
  }

  /**
   * Get debit
   * @return debit
   */
  
  @JsonProperty("debit")
  public String getDebit() {
    return debit;
  }

  public void setDebit(String debit) {
    this.debit = debit;
  }

  public JournalLineInput credit(String credit) {
    this.credit = credit;
    return this;
  }

  /**
   * Get credit
   * @return credit
   */
  
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
    JournalLineInput journalLineInput = (JournalLineInput) o;
    return Objects.equals(this.accountCode, journalLineInput.accountCode) &&
        Objects.equals(this.debit, journalLineInput.debit) &&
        Objects.equals(this.credit, journalLineInput.credit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountCode, debit, credit);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JournalLineInput {\n");
    sb.append("    accountCode: ").append(toIndentedString(accountCode)).append("\n");
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

