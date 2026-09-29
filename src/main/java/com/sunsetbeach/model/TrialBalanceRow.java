package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.LedgerAccountType;
import com.sunsetbeach.model.LedgerNormalBalance;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One account&#39;s totals up to and including &#x60;asOf&#x60;. Money is a decimal string with two decimals.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TrialBalanceRow {

  private String accountCode;

  private String accountName;

  private LedgerAccountType accountType;

  private LedgerNormalBalance normalBalance;

  private String totalDebit;

  private String totalCredit;

  private String balance;

  public TrialBalanceRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TrialBalanceRow(String accountCode, String accountName, LedgerAccountType accountType, LedgerNormalBalance normalBalance, String totalDebit, String totalCredit, String balance) {
    this.accountCode = accountCode;
    this.accountName = accountName;
    this.accountType = accountType;
    this.normalBalance = normalBalance;
    this.totalDebit = totalDebit;
    this.totalCredit = totalCredit;
    this.balance = balance;
  }

  public TrialBalanceRow accountCode(String accountCode) {
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

  public TrialBalanceRow accountName(String accountName) {
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

  public TrialBalanceRow accountType(LedgerAccountType accountType) {
    this.accountType = accountType;
    return this;
  }

  /**
   * Get accountType
   * @return accountType
   */
  @NotNull @Valid 
  @JsonProperty("accountType")
  public LedgerAccountType getAccountType() {
    return accountType;
  }

  public void setAccountType(LedgerAccountType accountType) {
    this.accountType = accountType;
  }

  public TrialBalanceRow normalBalance(LedgerNormalBalance normalBalance) {
    this.normalBalance = normalBalance;
    return this;
  }

  /**
   * Get normalBalance
   * @return normalBalance
   */
  @NotNull @Valid 
  @JsonProperty("normalBalance")
  public LedgerNormalBalance getNormalBalance() {
    return normalBalance;
  }

  public void setNormalBalance(LedgerNormalBalance normalBalance) {
    this.normalBalance = normalBalance;
  }

  public TrialBalanceRow totalDebit(String totalDebit) {
    this.totalDebit = totalDebit;
    return this;
  }

  /**
   * Get totalDebit
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

  public TrialBalanceRow totalCredit(String totalCredit) {
    this.totalCredit = totalCredit;
    return this;
  }

  /**
   * Get totalCredit
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

  public TrialBalanceRow balance(String balance) {
    this.balance = balance;
    return this;
  }

  /**
   * On the normal side - `totalDebit - totalCredit` for DEBIT-normal accounts, the reverse for CREDIT-normal ones. Negative is an abnormal balance.
   * @return balance
   */
  @NotNull 
  @JsonProperty("balance")
  public String getBalance() {
    return balance;
  }

  public void setBalance(String balance) {
    this.balance = balance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrialBalanceRow trialBalanceRow = (TrialBalanceRow) o;
    return Objects.equals(this.accountCode, trialBalanceRow.accountCode) &&
        Objects.equals(this.accountName, trialBalanceRow.accountName) &&
        Objects.equals(this.accountType, trialBalanceRow.accountType) &&
        Objects.equals(this.normalBalance, trialBalanceRow.normalBalance) &&
        Objects.equals(this.totalDebit, trialBalanceRow.totalDebit) &&
        Objects.equals(this.totalCredit, trialBalanceRow.totalCredit) &&
        Objects.equals(this.balance, trialBalanceRow.balance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountCode, accountName, accountType, normalBalance, totalDebit, totalCredit, balance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TrialBalanceRow {\n");
    sb.append("    accountCode: ").append(toIndentedString(accountCode)).append("\n");
    sb.append("    accountName: ").append(toIndentedString(accountName)).append("\n");
    sb.append("    accountType: ").append(toIndentedString(accountType)).append("\n");
    sb.append("    normalBalance: ").append(toIndentedString(normalBalance)).append("\n");
    sb.append("    totalDebit: ").append(toIndentedString(totalDebit)).append("\n");
    sb.append("    totalCredit: ").append(toIndentedString(totalCredit)).append("\n");
    sb.append("    balance: ").append(toIndentedString(balance)).append("\n");
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

