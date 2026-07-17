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
 * OkUpdated
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-17T16:01:20.967720600+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class OkUpdated {

  private Boolean ok;

  private Integer updated;

  public OkUpdated() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OkUpdated(Boolean ok, Integer updated) {
    this.ok = ok;
    this.updated = updated;
  }

  public OkUpdated ok(Boolean ok) {
    this.ok = ok;
    return this;
  }

  /**
   * Get ok
   * @return ok
   */
  @NotNull 
  @JsonProperty("ok")
  public Boolean getOk() {
    return ok;
  }

  public void setOk(Boolean ok) {
    this.ok = ok;
  }

  public OkUpdated updated(Integer updated) {
    this.updated = updated;
    return this;
  }

  /**
   * Number of day rows upserted.
   * @return updated
   */
  @NotNull 
  @JsonProperty("updated")
  public Integer getUpdated() {
    return updated;
  }

  public void setUpdated(Integer updated) {
    this.updated = updated;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OkUpdated okUpdated = (OkUpdated) o;
    return Objects.equals(this.ok, okUpdated.ok) &&
        Objects.equals(this.updated, okUpdated.updated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ok, updated);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OkUpdated {\n");
    sb.append("    ok: ").append(toIndentedString(ok)).append("\n");
    sb.append("    updated: ").append(toIndentedString(updated)).append("\n");
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

