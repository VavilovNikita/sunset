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
 * The spa&#39;s own floor-plan background image - &#x60;GET /spa-map&#x60;, with the actual bytes served separately from &#x60;GET /spa-map/image&#x60;, same split as &#x60;PropertyMap&#x60;/&#x60;GET /property-map/ image&#x60;. Deliberately its own image, not the property map&#39;s: the spa&#39;s floor plan is SPA- zone tables (placed via &#x60;PATCH /tables/positions&#x60;, see &#x60;Table.positionX&#x60;), a different physical layout at a different scale than the property map&#39;s rooms, and a manager replacing one must never be mistaken for replacing the other. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SpaMap {

  private JsonNullable<String> imagePath = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> imageUpdatedAt = JsonNullable.<OffsetDateTime>undefined();

  public SpaMap() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpaMap(String imagePath, OffsetDateTime imageUpdatedAt) {
    this.imagePath = JsonNullable.of(imagePath);
    this.imageUpdatedAt = JsonNullable.of(imageUpdatedAt);
  }

  public SpaMap imagePath(String imagePath) {
    this.imagePath = JsonNullable.of(imagePath);
    return this;
  }

  /**
   * Null until a manager uploads one via `POST /spa-map/image` - a normal state right after this ships.
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

  public SpaMap imageUpdatedAt(OffsetDateTime imageUpdatedAt) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaMap spaMap = (SpaMap) o;
    return Objects.equals(this.imagePath, spaMap.imagePath) &&
        Objects.equals(this.imageUpdatedAt, spaMap.imageUpdatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imagePath, imageUpdatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpaMap {\n");
    sb.append("    imagePath: ").append(toIndentedString(imagePath)).append("\n");
    sb.append("    imageUpdatedAt: ").append(toIndentedString(imageUpdatedAt)).append("\n");
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

