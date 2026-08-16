package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.MenuDepartment;
import java.math.BigDecimal;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Full replacement on PATCH — no partial update (same convention as &#x60;RoomInput&#x60;). &#x60;department&#x60; defaults to &#x60;KITCHEN&#x60; (most menu items are food) - existing rows migrated the same way; staff reassign drinks to &#x60;BAR&#x60; explicitly. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-16T20:25:37.858402100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class MenuItemInput {

  private String name;

  private String description;

  private String category;

  private MenuDepartment department;

  private BigDecimal price;

  private Boolean isAvailable = true;

  public MenuItemInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MenuItemInput(String name, String description, String category, BigDecimal price) {
    this.name = name;
    this.description = description;
    this.category = category;
    this.price = price;
  }

  public MenuItemInput name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 2, max = 120) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public MenuItemInput description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull @Size(min = 1, max = 2000) 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public MenuItemInput category(String category) {
    this.category = category;
    return this;
  }

  /**
   * Get category
   * @return category
   */
  @NotNull @Size(min = 1, max = 60) 
  @JsonProperty("category")
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public MenuItemInput department(MenuDepartment department) {
    this.department = department;
    return this;
  }

  /**
   * Get department
   * @return department
   */
  @Valid 
  @JsonProperty("department")
  public MenuDepartment getDepartment() {
    return department;
  }

  public void setDepartment(MenuDepartment department) {
    this.department = department;
  }

  public MenuItemInput price(BigDecimal price) {
    this.price = price;
    return this;
  }

  /**
   * Get price
   * minimum: 0
   * @return price
   */
  @NotNull @Valid @DecimalMin(value = "0", inclusive = false) 
  @JsonProperty("price")
  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public MenuItemInput isAvailable(Boolean isAvailable) {
    this.isAvailable = isAvailable;
    return this;
  }

  /**
   * Get isAvailable
   * @return isAvailable
   */
  
  @JsonProperty("isAvailable")
  public Boolean getIsAvailable() {
    return isAvailable;
  }

  public void setIsAvailable(Boolean isAvailable) {
    this.isAvailable = isAvailable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MenuItemInput menuItemInput = (MenuItemInput) o;
    return Objects.equals(this.name, menuItemInput.name) &&
        Objects.equals(this.description, menuItemInput.description) &&
        Objects.equals(this.category, menuItemInput.category) &&
        Objects.equals(this.department, menuItemInput.department) &&
        Objects.equals(this.price, menuItemInput.price) &&
        Objects.equals(this.isAvailable, menuItemInput.isAvailable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, category, department, price, isAvailable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MenuItemInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    department: ").append(toIndentedString(department)).append("\n");
    sb.append("    price: ").append(toIndentedString(price)).append("\n");
    sb.append("    isAvailable: ").append(toIndentedString(isAvailable)).append("\n");
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

