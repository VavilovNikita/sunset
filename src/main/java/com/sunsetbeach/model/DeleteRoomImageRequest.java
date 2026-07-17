package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DeleteRoomImageRequest
 */

@JsonTypeName("deleteRoomImage_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-17T16:01:20.967720600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class DeleteRoomImageRequest {

  private String path;

  public DeleteRoomImageRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DeleteRoomImageRequest(String path) {
    this.path = path;
  }

  public DeleteRoomImageRequest path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Exact image path as stored in `Room.images`, e.g. `/uploads/rooms/{id}/169...-ab12cd.jpg`.
   * @return path
   */
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeleteRoomImageRequest deleteRoomImageRequest = (DeleteRoomImageRequest) o;
    return Objects.equals(this.path, deleteRoomImageRequest.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeleteRoomImageRequest {\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
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

