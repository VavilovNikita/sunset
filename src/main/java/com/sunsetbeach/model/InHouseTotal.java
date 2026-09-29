package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * InHouseTotal
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class InHouseTotal {

  private Integer rooms;

  private Integer adults;

  private Integer children;

  public InHouseTotal() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InHouseTotal(Integer rooms, Integer adults, Integer children) {
    this.rooms = rooms;
    this.adults = adults;
    this.children = children;
  }

  public InHouseTotal rooms(Integer rooms) {
    this.rooms = rooms;
    return this;
  }

  /**
   * Get rooms
   * @return rooms
   */
  @NotNull 
  @JsonProperty("rooms")
  public Integer getRooms() {
    return rooms;
  }

  public void setRooms(Integer rooms) {
    this.rooms = rooms;
  }

  public InHouseTotal adults(Integer adults) {
    this.adults = adults;
    return this;
  }

  /**
   * Get adults
   * @return adults
   */
  @NotNull 
  @JsonProperty("adults")
  public Integer getAdults() {
    return adults;
  }

  public void setAdults(Integer adults) {
    this.adults = adults;
  }

  public InHouseTotal children(Integer children) {
    this.children = children;
    return this;
  }

  /**
   * Get children
   * @return children
   */
  @NotNull 
  @JsonProperty("children")
  public Integer getChildren() {
    return children;
  }

  public void setChildren(Integer children) {
    this.children = children;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InHouseTotal inHouseTotal = (InHouseTotal) o;
    return Objects.equals(this.rooms, inHouseTotal.rooms) &&
        Objects.equals(this.adults, inHouseTotal.adults) &&
        Objects.equals(this.children, inHouseTotal.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rooms, adults, children);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InHouseTotal {\n");
    sb.append("    rooms: ").append(toIndentedString(rooms)).append("\n");
    sb.append("    adults: ").append(toIndentedString(adults)).append("\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
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

