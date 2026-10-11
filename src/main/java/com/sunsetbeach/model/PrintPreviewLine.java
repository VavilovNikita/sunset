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
 * PrintPreviewLine
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PrintPreviewLine {

  private String text;

  private Boolean center;

  private Boolean bold;

  private Integer size;

  private Boolean fontB;

  public PrintPreviewLine() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrintPreviewLine(String text, Boolean center, Boolean bold, Integer size, Boolean fontB) {
    this.text = text;
    this.center = center;
    this.bold = bold;
    this.size = size;
    this.fontB = fontB;
  }

  public PrintPreviewLine text(String text) {
    this.text = text;
    return this;
  }

  /**
   * Get text
   * @return text
   */
  @NotNull 
  @JsonProperty("text")
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public PrintPreviewLine center(Boolean center) {
    this.center = center;
    return this;
  }

  /**
   * Get center
   * @return center
   */
  @NotNull 
  @JsonProperty("center")
  public Boolean getCenter() {
    return center;
  }

  public void setCenter(Boolean center) {
    this.center = center;
  }

  public PrintPreviewLine bold(Boolean bold) {
    this.bold = bold;
    return this;
  }

  /**
   * Get bold
   * @return bold
   */
  @NotNull 
  @JsonProperty("bold")
  public Boolean getBold() {
    return bold;
  }

  public void setBold(Boolean bold) {
    this.bold = bold;
  }

  public PrintPreviewLine size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * 1 or 2 - the glyph magnification this line prints at.
   * @return size
   */
  @NotNull 
  @JsonProperty("size")
  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public PrintPreviewLine fontB(Boolean fontB) {
    this.fontB = fontB;
    return this;
  }

  /**
   * True when the line prints in the printer's smaller Font B.
   * @return fontB
   */
  @NotNull 
  @JsonProperty("fontB")
  public Boolean getFontB() {
    return fontB;
  }

  public void setFontB(Boolean fontB) {
    this.fontB = fontB;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrintPreviewLine printPreviewLine = (PrintPreviewLine) o;
    return Objects.equals(this.text, printPreviewLine.text) &&
        Objects.equals(this.center, printPreviewLine.center) &&
        Objects.equals(this.bold, printPreviewLine.bold) &&
        Objects.equals(this.size, printPreviewLine.size) &&
        Objects.equals(this.fontB, printPreviewLine.fontB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(text, center, bold, size, fontB);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrintPreviewLine {\n");
    sb.append("    text: ").append(toIndentedString(text)).append("\n");
    sb.append("    center: ").append(toIndentedString(center)).append("\n");
    sb.append("    bold: ").append(toIndentedString(bold)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    fontB: ").append(toIndentedString(fontB)).append("\n");
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

