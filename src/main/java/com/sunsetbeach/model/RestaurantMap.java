package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RestaurantMapTable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The restaurant&#39;s own floor-plan background image and its tables - &#x60;GET /restaurant-map&#x60;, with the image bytes served from &#x60;GET /restaurant-map/image&#x60;, same split as &#x60;SpaMap&#x60;. Its own image, not the spa&#39;s or the property map&#39;s: a different room, and replacing one must never be mistaken for replacing another. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RestaurantMap {

  private JsonNullable<String> imagePath = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> imageUpdatedAt = JsonNullable.<OffsetDateTime>undefined();

  @Valid
  private List<@Valid RestaurantMapTable> tables = new ArrayList<>();

  public RestaurantMap() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RestaurantMap(String imagePath, OffsetDateTime imageUpdatedAt, List<@Valid RestaurantMapTable> tables) {
    this.imagePath = JsonNullable.of(imagePath);
    this.imageUpdatedAt = JsonNullable.of(imageUpdatedAt);
    this.tables = tables;
  }

  public RestaurantMap imagePath(String imagePath) {
    this.imagePath = JsonNullable.of(imagePath);
    return this;
  }

  /**
   * Null until a manager uploads one via `POST /restaurant-map/image`.
   * @return imagePath
   */
  @NotNull 
  @JsonProperty("imagePath")
  public JsonNullable<String> getImagePath() {
    return imagePath;
  }

  public void setImagePath(JsonNullable<String> imagePath) {
    this.imagePath = imagePath;
  }

  public RestaurantMap imageUpdatedAt(OffsetDateTime imageUpdatedAt) {
    this.imageUpdatedAt = JsonNullable.of(imageUpdatedAt);
    return this;
  }

  /**
   * Get imageUpdatedAt
   * @return imageUpdatedAt
   */
  @NotNull @Valid 
  @JsonProperty("imageUpdatedAt")
  public JsonNullable<OffsetDateTime> getImageUpdatedAt() {
    return imageUpdatedAt;
  }

  public void setImageUpdatedAt(JsonNullable<OffsetDateTime> imageUpdatedAt) {
    this.imageUpdatedAt = imageUpdatedAt;
  }

  public RestaurantMap tables(List<@Valid RestaurantMapTable> tables) {
    this.tables = tables;
    return this;
  }

  public RestaurantMap addTablesItem(RestaurantMapTable tablesItem) {
    if (this.tables == null) {
      this.tables = new ArrayList<>();
    }
    this.tables.add(tablesItem);
    return this;
  }

  /**
   * Get tables
   * @return tables
   */
  @NotNull @Valid 
  @JsonProperty("tables")
  public List<@Valid RestaurantMapTable> getTables() {
    return tables;
  }

  public void setTables(List<@Valid RestaurantMapTable> tables) {
    this.tables = tables;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RestaurantMap restaurantMap = (RestaurantMap) o;
    return Objects.equals(this.imagePath, restaurantMap.imagePath) &&
        Objects.equals(this.imageUpdatedAt, restaurantMap.imageUpdatedAt) &&
        Objects.equals(this.tables, restaurantMap.tables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imagePath, imageUpdatedAt, tables);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RestaurantMap {\n");
    sb.append("    imagePath: ").append(toIndentedString(imagePath)).append("\n");
    sb.append("    imageUpdatedAt: ").append(toIndentedString(imageUpdatedAt)).append("\n");
    sb.append("    tables: ").append(toIndentedString(tables)).append("\n");
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

