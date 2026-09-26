package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.PosSalesMixCategory;
import com.sunsetbeach.model.PosSalesMixDepartment;
import com.sunsetbeach.model.PosSalesMixItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * &#x60;GET /reports/pos-sales-mix&#x60; - see that operation for what counts.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PosSalesMixReport {

  private String from;

  private String to;

  private Integer totalQuantity;

  private String totalRevenue;

  @Valid
  private List<@Valid PosSalesMixItem> items = new ArrayList<>();

  @Valid
  private List<@Valid PosSalesMixCategory> categories = new ArrayList<>();

  @Valid
  private List<@Valid PosSalesMixDepartment> departments = new ArrayList<>();

  public PosSalesMixReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PosSalesMixReport(String from, String to, Integer totalQuantity, String totalRevenue, List<@Valid PosSalesMixItem> items, List<@Valid PosSalesMixCategory> categories, List<@Valid PosSalesMixDepartment> departments) {
    this.from = from;
    this.to = to;
    this.totalQuantity = totalQuantity;
    this.totalRevenue = totalRevenue;
    this.items = items;
    this.categories = categories;
    this.departments = departments;
  }

  public PosSalesMixReport from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public PosSalesMixReport to(String to) {
    this.to = to;
    return this;
  }

  /**
   * Get to
   * @return to
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public PosSalesMixReport totalQuantity(Integer totalQuantity) {
    this.totalQuantity = totalQuantity;
    return this;
  }

  /**
   * Get totalQuantity
   * @return totalQuantity
   */
  @NotNull 
  @JsonProperty("totalQuantity")
  public Integer getTotalQuantity() {
    return totalQuantity;
  }

  public void setTotalQuantity(Integer totalQuantity) {
    this.totalQuantity = totalQuantity;
  }

  public PosSalesMixReport totalRevenue(String totalRevenue) {
    this.totalRevenue = totalRevenue;
    return this;
  }

  /**
   * Get totalRevenue
   * @return totalRevenue
   */
  @NotNull 
  @JsonProperty("totalRevenue")
  public String getTotalRevenue() {
    return totalRevenue;
  }

  public void setTotalRevenue(String totalRevenue) {
    this.totalRevenue = totalRevenue;
  }

  public PosSalesMixReport items(List<@Valid PosSalesMixItem> items) {
    this.items = items;
    return this;
  }

  public PosSalesMixReport addItemsItem(PosSalesMixItem itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
   */
  @NotNull @Valid 
  @JsonProperty("items")
  public List<@Valid PosSalesMixItem> getItems() {
    return items;
  }

  public void setItems(List<@Valid PosSalesMixItem> items) {
    this.items = items;
  }

  public PosSalesMixReport categories(List<@Valid PosSalesMixCategory> categories) {
    this.categories = categories;
    return this;
  }

  public PosSalesMixReport addCategoriesItem(PosSalesMixCategory categoriesItem) {
    if (this.categories == null) {
      this.categories = new ArrayList<>();
    }
    this.categories.add(categoriesItem);
    return this;
  }

  /**
   * Get categories
   * @return categories
   */
  @NotNull @Valid 
  @JsonProperty("categories")
  public List<@Valid PosSalesMixCategory> getCategories() {
    return categories;
  }

  public void setCategories(List<@Valid PosSalesMixCategory> categories) {
    this.categories = categories;
  }

  public PosSalesMixReport departments(List<@Valid PosSalesMixDepartment> departments) {
    this.departments = departments;
    return this;
  }

  public PosSalesMixReport addDepartmentsItem(PosSalesMixDepartment departmentsItem) {
    if (this.departments == null) {
      this.departments = new ArrayList<>();
    }
    this.departments.add(departmentsItem);
    return this;
  }

  /**
   * Get departments
   * @return departments
   */
  @NotNull @Valid 
  @JsonProperty("departments")
  public List<@Valid PosSalesMixDepartment> getDepartments() {
    return departments;
  }

  public void setDepartments(List<@Valid PosSalesMixDepartment> departments) {
    this.departments = departments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PosSalesMixReport posSalesMixReport = (PosSalesMixReport) o;
    return Objects.equals(this.from, posSalesMixReport.from) &&
        Objects.equals(this.to, posSalesMixReport.to) &&
        Objects.equals(this.totalQuantity, posSalesMixReport.totalQuantity) &&
        Objects.equals(this.totalRevenue, posSalesMixReport.totalRevenue) &&
        Objects.equals(this.items, posSalesMixReport.items) &&
        Objects.equals(this.categories, posSalesMixReport.categories) &&
        Objects.equals(this.departments, posSalesMixReport.departments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, totalQuantity, totalRevenue, items, categories, departments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PosSalesMixReport {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    totalQuantity: ").append(toIndentedString(totalQuantity)).append("\n");
    sb.append("    totalRevenue: ").append(toIndentedString(totalRevenue)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    categories: ").append(toIndentedString(categories)).append("\n");
    sb.append("    departments: ").append(toIndentedString(departments)).append("\n");
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

