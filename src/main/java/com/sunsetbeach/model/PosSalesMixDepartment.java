package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.MenuDepartment;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PosSalesMixDepartment
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PosSalesMixDepartment {

  private MenuDepartment department;

  private Integer quantity;

  private String revenue;

  public PosSalesMixDepartment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PosSalesMixDepartment(MenuDepartment department, Integer quantity, String revenue) {
    this.department = department;
    this.quantity = quantity;
    this.revenue = revenue;
  }

  public PosSalesMixDepartment department(MenuDepartment department) {
    this.department = department;
    return this;
  }

  /**
   * Get department
   * @return department
   */
  @NotNull @Valid 
  @JsonProperty("department")
  public MenuDepartment getDepartment() {
    return department;
  }

  public void setDepartment(MenuDepartment department) {
    this.department = department;
  }

  public PosSalesMixDepartment quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Get quantity
   * @return quantity
   */
  @NotNull 
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public PosSalesMixDepartment revenue(String revenue) {
    this.revenue = revenue;
    return this;
  }

  /**
   * Get revenue
   * @return revenue
   */
  @NotNull 
  @JsonProperty("revenue")
  public String getRevenue() {
    return revenue;
  }

  public void setRevenue(String revenue) {
    this.revenue = revenue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PosSalesMixDepartment posSalesMixDepartment = (PosSalesMixDepartment) o;
    return Objects.equals(this.department, posSalesMixDepartment.department) &&
        Objects.equals(this.quantity, posSalesMixDepartment.quantity) &&
        Objects.equals(this.revenue, posSalesMixDepartment.revenue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(department, quantity, revenue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PosSalesMixDepartment {\n");
    sb.append("    department: ").append(toIndentedString(department)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    revenue: ").append(toIndentedString(revenue)).append("\n");
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

