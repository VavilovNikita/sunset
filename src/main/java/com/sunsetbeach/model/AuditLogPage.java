package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.AuditLogEntry;
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
 * One page of &#x60;GET /audit-log&#x60; results, newest first.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class AuditLogPage {

  @Valid
  private List<@Valid AuditLogEntry> items = new ArrayList<>();

  private Integer page;

  private Integer pageSize;

  private Integer totalCount;

  public AuditLogPage() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AuditLogPage(List<@Valid AuditLogEntry> items, Integer page, Integer pageSize, Integer totalCount) {
    this.items = items;
    this.page = page;
    this.pageSize = pageSize;
    this.totalCount = totalCount;
  }

  public AuditLogPage items(List<@Valid AuditLogEntry> items) {
    this.items = items;
    return this;
  }

  public AuditLogPage addItemsItem(AuditLogEntry itemsItem) {
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
  public List<@Valid AuditLogEntry> getItems() {
    return items;
  }

  public void setItems(List<@Valid AuditLogEntry> items) {
    this.items = items;
  }

  public AuditLogPage page(Integer page) {
    this.page = page;
    return this;
  }

  /**
   * Get page
   * @return page
   */
  @NotNull 
  @JsonProperty("page")
  public Integer getPage() {
    return page;
  }

  public void setPage(Integer page) {
    this.page = page;
  }

  public AuditLogPage pageSize(Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * Get pageSize
   * @return pageSize
   */
  @NotNull 
  @JsonProperty("pageSize")
  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public AuditLogPage totalCount(Integer totalCount) {
    this.totalCount = totalCount;
    return this;
  }

  /**
   * Total number of matching entries across all pages, for rendering pagination controls - not just `items.length`.
   * @return totalCount
   */
  @NotNull 
  @JsonProperty("totalCount")
  public Integer getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Integer totalCount) {
    this.totalCount = totalCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuditLogPage auditLogPage = (AuditLogPage) o;
    return Objects.equals(this.items, auditLogPage.items) &&
        Objects.equals(this.page, auditLogPage.page) &&
        Objects.equals(this.pageSize, auditLogPage.pageSize) &&
        Objects.equals(this.totalCount, auditLogPage.totalCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, page, pageSize, totalCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuditLogPage {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    page: ").append(toIndentedString(page)).append("\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    totalCount: ").append(toIndentedString(totalCount)).append("\n");
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

