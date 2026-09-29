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
 * SiteMinderRoomTypeMappingInput
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SiteMinderRoomTypeMappingInput {

  private String siteMinderRoomType;

  private String roomId;

  public SiteMinderRoomTypeMappingInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SiteMinderRoomTypeMappingInput(String siteMinderRoomType, String roomId) {
    this.siteMinderRoomType = siteMinderRoomType;
    this.roomId = roomId;
  }

  public SiteMinderRoomTypeMappingInput siteMinderRoomType(String siteMinderRoomType) {
    this.siteMinderRoomType = siteMinderRoomType;
    return this;
  }

  /**
   * Get siteMinderRoomType
   * @return siteMinderRoomType
   */
  @NotNull @Size(min = 1, max = 200) 
  @JsonProperty("siteMinderRoomType")
  public String getSiteMinderRoomType() {
    return siteMinderRoomType;
  }

  public void setSiteMinderRoomType(String siteMinderRoomType) {
    this.siteMinderRoomType = siteMinderRoomType;
  }

  public SiteMinderRoomTypeMappingInput roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * Get roomId
   * @return roomId
   */
  @NotNull @Size(min = 1) 
  @JsonProperty("roomId")
  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SiteMinderRoomTypeMappingInput siteMinderRoomTypeMappingInput = (SiteMinderRoomTypeMappingInput) o;
    return Objects.equals(this.siteMinderRoomType, siteMinderRoomTypeMappingInput.siteMinderRoomType) &&
        Objects.equals(this.roomId, siteMinderRoomTypeMappingInput.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(siteMinderRoomType, roomId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SiteMinderRoomTypeMappingInput {\n");
    sb.append("    siteMinderRoomType: ").append(toIndentedString(siteMinderRoomType)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
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

