package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.PrintPreviewLine;
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
 * A rendered sample document. &#x60;columns&#x60; is the line width in Font A/B characters at 1x for the paper and font chosen; a 2x line holds half as many. &#x60;logoPngBase64&#x60; is the logo as it will print - black and white, at its printed size in dots (&#x60;logoWidthDots&#x60; wide) - and absent when there is no logo, &#x60;showLogo&#x60; is off or the document never prints it. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PrintLayoutPreview {

  private Integer columns;

  @Valid
  private List<@Valid PrintPreviewLine> lines = new ArrayList<>();

  private String logoPngBase64;

  private Integer logoWidthDots;

  public PrintLayoutPreview() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrintLayoutPreview(Integer columns, List<@Valid PrintPreviewLine> lines) {
    this.columns = columns;
    this.lines = lines;
  }

  public PrintLayoutPreview columns(Integer columns) {
    this.columns = columns;
    return this;
  }

  /**
   * Get columns
   * @return columns
   */
  @NotNull 
  @JsonProperty("columns")
  public Integer getColumns() {
    return columns;
  }

  public void setColumns(Integer columns) {
    this.columns = columns;
  }

  public PrintLayoutPreview lines(List<@Valid PrintPreviewLine> lines) {
    this.lines = lines;
    return this;
  }

  public PrintLayoutPreview addLinesItem(PrintPreviewLine linesItem) {
    if (this.lines == null) {
      this.lines = new ArrayList<>();
    }
    this.lines.add(linesItem);
    return this;
  }

  /**
   * Get lines
   * @return lines
   */
  @NotNull @Valid 
  @JsonProperty("lines")
  public List<@Valid PrintPreviewLine> getLines() {
    return lines;
  }

  public void setLines(List<@Valid PrintPreviewLine> lines) {
    this.lines = lines;
  }

  public PrintLayoutPreview logoPngBase64(String logoPngBase64) {
    this.logoPngBase64 = logoPngBase64;
    return this;
  }

  /**
   * Get logoPngBase64
   * @return logoPngBase64
   */
  
  @JsonProperty("logoPngBase64")
  public String getLogoPngBase64() {
    return logoPngBase64;
  }

  public void setLogoPngBase64(String logoPngBase64) {
    this.logoPngBase64 = logoPngBase64;
  }

  public PrintLayoutPreview logoWidthDots(Integer logoWidthDots) {
    this.logoWidthDots = logoWidthDots;
    return this;
  }

  /**
   * Get logoWidthDots
   * @return logoWidthDots
   */
  
  @JsonProperty("logoWidthDots")
  public Integer getLogoWidthDots() {
    return logoWidthDots;
  }

  public void setLogoWidthDots(Integer logoWidthDots) {
    this.logoWidthDots = logoWidthDots;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrintLayoutPreview printLayoutPreview = (PrintLayoutPreview) o;
    return Objects.equals(this.columns, printLayoutPreview.columns) &&
        Objects.equals(this.lines, printLayoutPreview.lines) &&
        Objects.equals(this.logoPngBase64, printLayoutPreview.logoPngBase64) &&
        Objects.equals(this.logoWidthDots, printLayoutPreview.logoWidthDots);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columns, lines, logoPngBase64, logoWidthDots);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrintLayoutPreview {\n");
    sb.append("    columns: ").append(toIndentedString(columns)).append("\n");
    sb.append("    lines: ").append(toIndentedString(lines)).append("\n");
    sb.append("    logoPngBase64: ").append(toIndentedString(logoPngBase64)).append("\n");
    sb.append("    logoWidthDots: ").append(toIndentedString(logoWidthDots)).append("\n");
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

