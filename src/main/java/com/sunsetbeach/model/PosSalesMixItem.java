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
 * PosSalesMixItem
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PosSalesMixItem {

  private String menuItemId;

  private String name;

  private String category;

  private MenuDepartment department;

  private Integer quantity;

  private String revenue;

  public PosSalesMixItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PosSalesMixItem(String menuItemId, String name, String category, MenuDepartment department, Integer quantity, String revenue) {
    this.menuItemId = menuItemId;
    this.name = name;
    this.category = category;
    this.department = department;
    this.quantity = quantity;
    this.revenue = revenue;
  }

  public PosSalesMixItem menuItemId(String menuItemId) {
    this.menuItemId = menuItemId;
    return this;
  }

  /**
   * Get menuItemId
   * @return menuItemId
   */
  @NotNull 
  @JsonProperty("menuItemId")
  public String getMenuItemId() {
    return menuItemId;
  }

  public void setMenuItemId(String menuItemId) {
    this.menuItemId = menuItemId;
  }

  public PosSalesMixItem name(String name) {
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

  public PosSalesMixItem category(String category) {
    this.category = category;
    return this;
  }

  /**
   * Get category
   * @return category
   */
  @NotNull 
  @JsonProperty("category")
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public PosSalesMixItem department(MenuDepartment department) {
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

  public PosSalesMixItem quantity(Integer quantity) {
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

  public PosSalesMixItem revenue(String revenue) {
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
    PosSalesMixItem posSalesMixItem = (PosSalesMixItem) o;
    return Objects.equals(this.menuItemId, posSalesMixItem.menuItemId) &&
        Objects.equals(this.name, posSalesMixItem.name) &&
        Objects.equals(this.category, posSalesMixItem.category) &&
        Objects.equals(this.department, posSalesMixItem.department) &&
        Objects.equals(this.quantity, posSalesMixItem.quantity) &&
        Objects.equals(this.revenue, posSalesMixItem.revenue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(menuItemId, name, category, department, quantity, revenue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PosSalesMixItem {\n");
    sb.append("    menuItemId: ").append(toIndentedString(menuItemId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
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

