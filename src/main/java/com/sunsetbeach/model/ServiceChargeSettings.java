package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The service-charge rate the booking tracker (&#x60;GET /reports/booking-tracker&#x60;) deducts from room prices, which include it. **For that analysis only**: nothing is ever charged to a guest, and no folio, POS total, receipt, ledger entry or Z-report reads it - the system still has no service charge as a thing it charges. Seeded at 10.00. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ServiceChargeSettings {

  private String serviceChargeRate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public ServiceChargeSettings() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ServiceChargeSettings(String serviceChargeRate, OffsetDateTime updatedAt) {
    this.serviceChargeRate = serviceChargeRate;
    this.updatedAt = updatedAt;
  }

  public ServiceChargeSettings serviceChargeRate(String serviceChargeRate) {
    this.serviceChargeRate = serviceChargeRate;
    return this;
  }

  /**
   * Percent, two decimals, e.g. \"10.00\".
   * @return serviceChargeRate
   */
  @NotNull 
  @JsonProperty("serviceChargeRate")
  public String getServiceChargeRate() {
    return serviceChargeRate;
  }

  public void setServiceChargeRate(String serviceChargeRate) {
    this.serviceChargeRate = serviceChargeRate;
  }

  public ServiceChargeSettings updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceChargeSettings serviceChargeSettings = (ServiceChargeSettings) o;
    return Objects.equals(this.serviceChargeRate, serviceChargeSettings.serviceChargeRate) &&
        Objects.equals(this.updatedAt, serviceChargeSettings.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceChargeRate, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceChargeSettings {\n");
    sb.append("    serviceChargeRate: ").append(toIndentedString(serviceChargeRate)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

