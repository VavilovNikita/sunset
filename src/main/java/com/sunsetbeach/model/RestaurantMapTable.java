package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.Zone;
import java.math.BigDecimal;
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
 * One non-SPA table on the restaurant map (&#x60;GET /restaurant-map&#x60;). &#x60;openOrderIds&#x60; are the table&#39;s &#x60;OPEN&#x60;/&#x60;SENT&#x60; orders - the same set the POS board treats as \&quot;occupied\&quot;, and more than one is possible (&#x60;POST /orders&#x60; doesn&#39;t enforce one order per table). &#x60;isActive&#x60; is an independent fact, as on &#x60;SpaMapTable&#x60;: a deactivated table is still listed, never excluded. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RestaurantMapTable {

  private String tableId;

  private String label;

  private Zone zone;

  private Integer capacity;

  private Boolean isActive;

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionX = JsonNullable.<BigDecimal>undefined();

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionY = JsonNullable.<BigDecimal>undefined();

  @Valid
  private List<String> openOrderIds = new ArrayList<>();

  public RestaurantMapTable() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RestaurantMapTable(String tableId, String label, Zone zone, Integer capacity, Boolean isActive, BigDecimal positionX, BigDecimal positionY, List<String> openOrderIds) {
    this.tableId = tableId;
    this.label = label;
    this.zone = zone;
    this.capacity = capacity;
    this.isActive = isActive;
    this.positionX = JsonNullable.of(positionX);
    this.positionY = JsonNullable.of(positionY);
    this.openOrderIds = openOrderIds;
  }

  public RestaurantMapTable tableId(String tableId) {
    this.tableId = tableId;
    return this;
  }

  /**
   * Get tableId
   * @return tableId
   */
  @NotNull 
  @JsonProperty("tableId")
  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public RestaurantMapTable label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  @NotNull 
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public RestaurantMapTable zone(Zone zone) {
    this.zone = zone;
    return this;
  }

  /**
   * Get zone
   * @return zone
   */
  @NotNull @Valid 
  @JsonProperty("zone")
  public Zone getZone() {
    return zone;
  }

  public void setZone(Zone zone) {
    this.zone = zone;
  }

  public RestaurantMapTable capacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * Get capacity
   * @return capacity
   */
  @NotNull 
  @JsonProperty("capacity")
  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public RestaurantMapTable isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Get isActive
   * @return isActive
   */
  @NotNull 
  @JsonProperty("isActive")
  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public RestaurantMapTable positionX(BigDecimal positionX) {
    this.positionX = JsonNullable.of(positionX);
    return this;
  }

  /**
   * Get positionX
   * minimum: 0
   * maximum: 1
   * @return positionX
   */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionX")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionX() {
    return positionX;
  }

  public void setPositionX(JsonNullable<BigDecimal> positionX) {
    this.positionX = positionX;
  }

  public RestaurantMapTable positionY(BigDecimal positionY) {
    this.positionY = JsonNullable.of(positionY);
    return this;
  }

  /**
   * Get positionY
   * minimum: 0
   * maximum: 1
   * @return positionY
   */
  @NotNull @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionY")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionY() {
    return positionY;
  }

  public void setPositionY(JsonNullable<BigDecimal> positionY) {
    this.positionY = positionY;
  }

  public RestaurantMapTable openOrderIds(List<String> openOrderIds) {
    this.openOrderIds = openOrderIds;
    return this;
  }

  public RestaurantMapTable addOpenOrderIdsItem(String openOrderIdsItem) {
    if (this.openOrderIds == null) {
      this.openOrderIds = new ArrayList<>();
    }
    this.openOrderIds.add(openOrderIdsItem);
    return this;
  }

  /**
   * Get openOrderIds
   * @return openOrderIds
   */
  @NotNull 
  @JsonProperty("openOrderIds")
  public List<String> getOpenOrderIds() {
    return openOrderIds;
  }

  public void setOpenOrderIds(List<String> openOrderIds) {
    this.openOrderIds = openOrderIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RestaurantMapTable restaurantMapTable = (RestaurantMapTable) o;
    return Objects.equals(this.tableId, restaurantMapTable.tableId) &&
        Objects.equals(this.label, restaurantMapTable.label) &&
        Objects.equals(this.zone, restaurantMapTable.zone) &&
        Objects.equals(this.capacity, restaurantMapTable.capacity) &&
        Objects.equals(this.isActive, restaurantMapTable.isActive) &&
        Objects.equals(this.positionX, restaurantMapTable.positionX) &&
        Objects.equals(this.positionY, restaurantMapTable.positionY) &&
        Objects.equals(this.openOrderIds, restaurantMapTable.openOrderIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableId, label, zone, capacity, isActive, positionX, positionY, openOrderIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RestaurantMapTable {\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    zone: ").append(toIndentedString(zone)).append("\n");
    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    positionX: ").append(toIndentedString(positionX)).append("\n");
    sb.append("    positionY: ").append(toIndentedString(positionY)).append("\n");
    sb.append("    openOrderIds: ").append(toIndentedString(openOrderIds)).append("\n");
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

