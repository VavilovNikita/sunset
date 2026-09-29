package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.LedgerAccountType;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;POST /ledger/accounts&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LedgerAccountCreateInput {

  private String code;

  private String name;

  private LedgerAccountType type;

  public LedgerAccountCreateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LedgerAccountCreateInput(String code, String name, LedgerAccountType type) {
    this.code = code;
    this.name = name;
    this.type = type;
  }

  public LedgerAccountCreateInput code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Unique; digits, capital letters and hyphens, at most 20 characters (e.g. \"6100\").
   * @return code
   */
  @NotNull @Pattern(regexp = "^[0-9A-Z][0-9A-Z-]{0,19}$") 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public LedgerAccountCreateInput name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 100) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LedgerAccountCreateInput type(LedgerAccountType type) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LedgerAccountCreateInput ledgerAccountCreateInput = (LedgerAccountCreateInput) o;
    return Objects.equals(this.code, ledgerAccountCreateInput.code) &&
        Objects.equals(this.name, ledgerAccountCreateInput.name) &&
        Objects.equals(this.type, ledgerAccountCreateInput.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, name, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LedgerAccountCreateInput {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

