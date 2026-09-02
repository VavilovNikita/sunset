package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.PropertyMapUnit;
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
 * Body of &#x60;GET /property-map&#x60; - the front desk&#39;s visual map: every physical room (placed on the map or not), enriched with today&#39;s occupancy/housekeeping/debt/block state, plus the current background image. A dedicated addition to &#x60;TodayBoard&#x60;/the booking calendar, not a replacement for either. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PropertyMap {

  private JsonNullable<String> imagePath = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> imageUpdatedAt = JsonNullable.<OffsetDateTime>undefined();

  @Valid
  private List<@Valid PropertyMapUnit> units = new ArrayList<>();

  public PropertyMap() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PropertyMap(String imagePath, OffsetDateTime imageUpdatedAt, List<@Valid PropertyMapUnit> units) {
    this.imagePath = JsonNullable.of(imagePath);
    this.imageUpdatedAt = JsonNullable.of(imageUpdatedAt);
    this.units = units;
  }

  public PropertyMap imagePath(String imagePath) {
    this.imagePath = JsonNullable.of(imagePath);
    return this;
  }

  /**
   * Null until a manager uploads one via `POST /property-map/image` - a normal state right after this ships.
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

  public PropertyMap imageUpdatedAt(OffsetDateTime imageUpdatedAt) {
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

  public PropertyMap units(List<@Valid PropertyMapUnit> units) {
    this.units = units;
    return this;
  }

  public PropertyMap addUnitsItem(PropertyMapUnit unitsItem) {
    if (this.units == null) {
      this.units = new ArrayList<>();
    }
    this.units.add(unitsItem);
    return this;
  }

  /**
   * Get units
   * @return units
   */
  @NotNull @Valid 
  @JsonProperty("units")
  public List<@Valid PropertyMapUnit> getUnits() {
    return units;
  }

  public void setUnits(List<@Valid PropertyMapUnit> units) {
    this.units = units;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertyMap propertyMap = (PropertyMap) o;
    return Objects.equals(this.imagePath, propertyMap.imagePath) &&
        Objects.equals(this.imageUpdatedAt, propertyMap.imageUpdatedAt) &&
        Objects.equals(this.units, propertyMap.units);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imagePath, imageUpdatedAt, units);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertyMap {\n");
    sb.append("    imagePath: ").append(toIndentedString(imagePath)).append("\n");
    sb.append("    imageUpdatedAt: ").append(toIndentedString(imageUpdatedAt)).append("\n");
    sb.append("    units: ").append(toIndentedString(units)).append("\n");
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

