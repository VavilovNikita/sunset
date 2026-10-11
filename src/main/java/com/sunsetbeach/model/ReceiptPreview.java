package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.ReceiptPreviewLine;
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
 * A rendered sample receipt. &#x60;columns&#x60; is the line width in Font A/B characters at 1x for the paper and font chosen; a 2x line holds half as many. &#x60;logoPngBase64&#x60; is the logo as it will print - black and white, at its printed size in dots (&#x60;logoWidthDots&#x60; wide) - and absent when there is no logo or &#x60;showLogo&#x60; is off. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ReceiptPreview {

  private Integer columns;

  @Valid
  private List<@Valid ReceiptPreviewLine> lines = new ArrayList<>();

  private String logoPngBase64;

  private Integer logoWidthDots;

  public ReceiptPreview() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReceiptPreview(Integer columns, List<@Valid ReceiptPreviewLine> lines) {
    this.columns = columns;
    this.lines = lines;
  }

  public ReceiptPreview columns(Integer columns) {
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

  public ReceiptPreview lines(List<@Valid ReceiptPreviewLine> lines) {
    this.lines = lines;
    return this;
  }

  public ReceiptPreview addLinesItem(ReceiptPreviewLine linesItem) {
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
  public List<@Valid ReceiptPreviewLine> getLines() {
    return lines;
  }

  public void setLines(List<@Valid ReceiptPreviewLine> lines) {
    this.lines = lines;
  }

  public ReceiptPreview logoPngBase64(String logoPngBase64) {
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

  public ReceiptPreview logoWidthDots(Integer logoWidthDots) {
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
    ReceiptPreview receiptPreview = (ReceiptPreview) o;
    return Objects.equals(this.columns, receiptPreview.columns) &&
        Objects.equals(this.lines, receiptPreview.lines) &&
        Objects.equals(this.logoPngBase64, receiptPreview.logoPngBase64) &&
        Objects.equals(this.logoWidthDots, receiptPreview.logoWidthDots);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columns, lines, logoPngBase64, logoWidthDots);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReceiptPreview {\n");
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

