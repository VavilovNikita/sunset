package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Raw Prisma &#x60;Room&#x60; row as returned by &#x60;NextResponse.json(...)&#x60;. &#x60;basePrice&#x60; is a Prisma &#x60;Decimal&#x60; — it serializes to a **string**, not a JSON number. &#x60;createdAt&#x60; serializes as an ISO-8601 datetime string. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-17T16:01:20.967720600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class Room {

  private String id;

  private String name;

  private String description;

  private Integer capacity;

  private String basePrice;

  @Valid
  private List<String> images = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public Room() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Room(String id, String name, String description, Integer capacity, String basePrice, List<String> images, OffsetDateTime createdAt) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.capacity = capacity;
    this.basePrice = basePrice;
    this.images = images;
    this.createdAt = createdAt;
  }

  public Room id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Room name(String name) {
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

  public Room description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Room capacity(Integer capacity) {
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

  public Room basePrice(String basePrice) {
    this.basePrice = basePrice;
    return this;
  }

  /**
   * Decimal(10,2) rendered as a string, e.g. `\"1500.00\"`.
   * @return basePrice
   */
  @NotNull 
  @JsonProperty("basePrice")
  public String getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(String basePrice) {
    this.basePrice = basePrice;
  }

  public Room images(List<String> images) {
    this.images = images;
    return this;
  }

  public Room addImagesItem(String imagesItem) {
    if (this.images == null) {
      this.images = new ArrayList<>();
    }
    this.images.add(imagesItem);
    return this;
  }

  /**
   * Paths under `/uploads/rooms/{id}/...`.
   * @return images
   */
  @NotNull 
  @JsonProperty("images")
  public List<String> getImages() {
    return images;
  }

  public void setImages(List<String> images) {
    this.images = images;
  }

  public Room createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Room room = (Room) o;
    return Objects.equals(this.id, room.id) &&
        Objects.equals(this.name, room.name) &&
        Objects.equals(this.description, room.description) &&
        Objects.equals(this.capacity, room.capacity) &&
        Objects.equals(this.basePrice, room.basePrice) &&
        Objects.equals(this.images, room.images) &&
        Objects.equals(this.createdAt, room.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, capacity, basePrice, images, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Room {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
    sb.append("    basePrice: ").append(toIndentedString(basePrice)).append("\n");
    sb.append("    images: ").append(toIndentedString(images)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

