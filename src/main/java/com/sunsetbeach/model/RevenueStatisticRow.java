package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.RevenueCode;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One revenue code&#39;s figures, or the total (where &#x60;code&#x60; is null). Money is a decimal string with two decimals; &#x60;net + vat &#x3D; gross&#x60; exactly.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RevenueStatisticRow {

  private RevenueCode code;

  private String gross;

  private String vat;

  private String net;

  public RevenueStatisticRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RevenueStatisticRow(RevenueCode code, String gross, String vat, String net) {
    this.code = code;
    this.gross = gross;
    this.vat = vat;
    this.net = net;
  }

  public RevenueStatisticRow code(RevenueCode code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
   */
  @NotNull @Valid 
  @JsonProperty("code")
  public RevenueCode getCode() {
    return code;
  }

  public void setCode(RevenueCode code) {
    this.code = code;
  }

  public RevenueStatisticRow gross(String gross) {
    this.gross = gross;
    return this;
  }

  /**
   * Revenue as charged, VAT included.
   * @return gross
   */
  @NotNull 
  @JsonProperty("gross")
  public String getGross() {
    return gross;
  }

  public void setGross(String gross) {
    this.gross = gross;
  }

  public RevenueStatisticRow vat(String vat) {
    this.vat = vat;
    return this;
  }

  /**
   * The VAT inside `gross` - `gross × vatRate / (100 + vatRate)`.
   * @return vat
   */
  @NotNull 
  @JsonProperty("vat")
  public String getVat() {
    return vat;
  }

  public void setVat(String vat) {
    this.vat = vat;
  }

  public RevenueStatisticRow net(String net) {
    this.net = net;
    return this;
  }

  /**
   * `gross - vat`.
   * @return net
   */
  @NotNull 
  @JsonProperty("net")
  public String getNet() {
    return net;
  }

  public void setNet(String net) {
    this.net = net;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RevenueStatisticRow revenueStatisticRow = (RevenueStatisticRow) o;
    return Objects.equals(this.code, revenueStatisticRow.code) &&
        Objects.equals(this.gross, revenueStatisticRow.gross) &&
        Objects.equals(this.vat, revenueStatisticRow.vat) &&
        Objects.equals(this.net, revenueStatisticRow.net);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, gross, vat, net);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RevenueStatisticRow {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    gross: ").append(toIndentedString(gross)).append("\n");
    sb.append("    vat: ").append(toIndentedString(vat)).append("\n");
    sb.append("    net: ").append(toIndentedString(net)).append("\n");
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

