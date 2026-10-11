package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.PrintLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PrintLayouts
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PrintLayouts {

  private Boolean hasLogo;

  @Valid
  private List<PrintLayout> layouts = new ArrayList<>();

  public PrintLayouts() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrintLayouts(Boolean hasLogo, List<PrintLayout> layouts) {
    this.hasLogo = hasLogo;
    this.layouts = layouts;
  }

  public PrintLayouts hasLogo(Boolean hasLogo) {
    this.hasLogo = hasLogo;
    return this;
  }

  /**
   * Get hasLogo
   * @return hasLogo
   */
  @NotNull 
  @JsonProperty("hasLogo")
  public Boolean getHasLogo() {
    return hasLogo;
  }

  public void setHasLogo(Boolean hasLogo) {
    this.hasLogo = hasLogo;
  }

  public PrintLayouts layouts(List<PrintLayout> layouts) {
    this.layouts = layouts;
    return this;
  }

  public PrintLayouts addLayoutsItem(PrintLayout layoutsItem) {
    if (this.layouts == null) {
      this.layouts = new ArrayList<>();
    }
    this.layouts.add(layoutsItem);
    return this;
  }

  /**
   * Get layouts
   * @return layouts
   */
  @NotNull @Valid 
  @JsonProperty("layouts")
  public List<PrintLayout> getLayouts() {
    return layouts;
  }

  public void setLayouts(List<PrintLayout> layouts) {
    this.layouts = layouts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrintLayouts printLayouts = (PrintLayouts) o;
    return Objects.equals(this.hasLogo, printLayouts.hasLogo) &&
        Objects.equals(this.layouts, printLayouts.layouts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hasLogo, layouts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrintLayouts {\n");
    sb.append("    hasLogo: ").append(toIndentedString(hasLogo)).append("\n");
    sb.append("    layouts: ").append(toIndentedString(layouts)).append("\n");
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

