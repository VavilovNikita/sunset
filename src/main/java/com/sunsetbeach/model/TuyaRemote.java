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
 * TuyaRemote
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TuyaRemote {

  private String remoteId;

  private JsonNullable<String> name = JsonNullable.<String>undefined();

  public TuyaRemote() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TuyaRemote(String remoteId, String name) {
    this.remoteId = remoteId;
    this.name = JsonNullable.of(name);
  }

  public TuyaRemote remoteId(String remoteId) {
    this.remoteId = remoteId;
    return this;
  }

  /**
   * Get remoteId
   * @return remoteId
   */
  @NotNull 
  @JsonProperty("remoteId")
  public String getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(String remoteId) {
    this.remoteId = remoteId;
  }

  public TuyaRemote name(String name) {
    this.name = JsonNullable.of(name);
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public JsonNullable<String> getName() {
    return name;
  }

  public void setName(JsonNullable<String> name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TuyaRemote tuyaRemote = (TuyaRemote) o;
    return Objects.equals(this.remoteId, tuyaRemote.remoteId) &&
        Objects.equals(this.name, tuyaRemote.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(remoteId, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TuyaRemote {\n");
    sb.append("    remoteId: ").append(toIndentedString(remoteId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

