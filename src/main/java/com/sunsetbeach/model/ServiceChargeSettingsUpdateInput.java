package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PUT /settings/service-charge&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ServiceChargeSettingsUpdateInput {

  private BigDecimal serviceChargeRate;

  public ServiceChargeSettingsUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ServiceChargeSettingsUpdateInput(BigDecimal serviceChargeRate) {
    this.serviceChargeRate = serviceChargeRate;
  }

  public ServiceChargeSettingsUpdateInput serviceChargeRate(BigDecimal serviceChargeRate) {
    this.serviceChargeRate = serviceChargeRate;
    return this;
  }

  /**
   * Percent, at most two decimals. 0 is allowed (no service charge inside the prices).
   * minimum: 0
   * maximum: 99.99
   * @return serviceChargeRate
   */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("99.99") 
  @JsonProperty("serviceChargeRate")
  public BigDecimal getServiceChargeRate() {
    return serviceChargeRate;
  }

  public void setServiceChargeRate(BigDecimal serviceChargeRate) {
    this.serviceChargeRate = serviceChargeRate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceChargeSettingsUpdateInput serviceChargeSettingsUpdateInput = (ServiceChargeSettingsUpdateInput) o;
    return Objects.equals(this.serviceChargeRate, serviceChargeSettingsUpdateInput.serviceChargeRate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceChargeRate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceChargeSettingsUpdateInput {\n");
    sb.append("    serviceChargeRate: ").append(toIndentedString(serviceChargeRate)).append("\n");
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

