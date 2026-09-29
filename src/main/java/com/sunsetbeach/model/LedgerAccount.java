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
 * One account in the chart. Seeded: &#x60;1000&#x60; Cash/Bank, &#x60;1100&#x60; Guest Ledger (room charges not yet collected), &#x60;2100&#x60; VAT Payable, &#x60;3000&#x60; Owner Equity, &#x60;4000&#x60; Room Revenue, &#x60;4100&#x60; F&amp;B Revenue, &#x60;4200&#x60; SPA Revenue, &#x60;6000&#x60; General Expense. Cash/Bank is one account, not split by method: POS and folio payments record CASH/CARD/OTHER, but a booking&#39;s room settlement (&#x60;status &#x3D; PAID&#x60;) records no method at all, so a split could not be kept for room revenue. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LedgerAccount {

  private String code;

  private String name;

  private LedgerAccountType type;

  private LedgerNormalBalance normalBalance;

  public LedgerAccount() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LedgerAccount(String code, String name, LedgerAccountType type, LedgerNormalBalance normalBalance) {
    this.code = code;
    this.name = name;
    this.type = type;
    this.normalBalance = normalBalance;
  }

  public LedgerAccount code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
   */
  @NotNull 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public LedgerAccount name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LedgerAccount type(LedgerAccountType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @JsonProperty("type")
  public LedgerAccountType getType() {
    return type;
  }

  public void setType(LedgerAccountType type) {
    this.type = type;
  }

  public LedgerAccount normalBalance(LedgerNormalBalance normalBalance) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LedgerAccount ledgerAccount = (LedgerAccount) o;
    return Objects.equals(this.code, ledgerAccount.code) &&
        Objects.equals(this.name, ledgerAccount.name) &&
        Objects.equals(this.type, ledgerAccount.type) &&
        Objects.equals(this.normalBalance, ledgerAccount.normalBalance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, name, type, normalBalance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LedgerAccount {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    normalBalance: ").append(toIndentedString(normalBalance)).append("\n");
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

