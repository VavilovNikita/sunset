package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;roomSchema&#x60;. Full replacement on PATCH — no partial update. Does not include a unit count - a room type has zero sellable units until &#x60;RoomUnit&#x60;s are added for it via &#x60;POST /room-units&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-17T15:17:55.380996100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class RoomInput {

  private String name;

  private String description;

  private Integer capacity;

  private BigDecimal basePrice;

  public RoomInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomInput(String name, String description, Integer capacity, BigDecimal basePrice) {
    this.name = name;
    this.description = description;
    this.capacity = capacity;
    this.basePrice = basePrice;
  }

  public RoomInput name(String name) {
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

  public RoomInput description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull @Size(min = 10) 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public RoomInput capacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * Coerced to a number server-side (`z.coerce.number()`); a numeric string is also accepted.
   * minimum: 1
   * maximum: 20
   * @return capacity
   */
  @NotNull @Min(1) @Max(20) 
  @JsonProperty("capacity")
  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public RoomInput basePrice(BigDecimal basePrice) {
    this.basePrice = basePrice;
    return this;
  }

  /**
   * Coerced to a number server-side (`z.coerce.number()`); a numeric string is also accepted.
   * minimum: 0
   * @return basePrice
   */
  @NotNull @Valid @DecimalMin(value = "0", inclusive = false) 
  @JsonProperty("basePrice")
  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(BigDecimal basePrice) {
    this.basePrice = basePrice;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomInput roomInput = (RoomInput) o;
    return Objects.equals(this.name, roomInput.name) &&
        Objects.equals(this.description, roomInput.description) &&
        Objects.equals(this.capacity, roomInput.capacity) &&
        Objects.equals(this.basePrice, roomInput.basePrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, capacity, basePrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
    sb.append("    basePrice: ").append(toIndentedString(basePrice)).append("\n");
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

