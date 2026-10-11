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
 * ReceiptPreviewLine
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ReceiptPreviewLine {

  private String text;

  private Boolean center;

  private Boolean bold;

  private Integer size;

  private Boolean fontB;

  public ReceiptPreviewLine() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReceiptPreviewLine(String text, Boolean center, Boolean bold, Integer size, Boolean fontB) {
    this.text = text;
    this.center = center;
    this.bold = bold;
    this.size = size;
    this.fontB = fontB;
  }

  public ReceiptPreviewLine text(String text) {
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

  public ReceiptPreviewLine center(Boolean center) {
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

  public ReceiptPreviewLine bold(Boolean bold) {
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

  public ReceiptPreviewLine size(Integer size) {
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

  public ReceiptPreviewLine fontB(Boolean fontB) {
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
    ReceiptPreviewLine receiptPreviewLine = (ReceiptPreviewLine) o;
    return Objects.equals(this.text, receiptPreviewLine.text) &&
        Objects.equals(this.center, receiptPreviewLine.center) &&
        Objects.equals(this.bold, receiptPreviewLine.bold) &&
        Objects.equals(this.size, receiptPreviewLine.size) &&
        Objects.equals(this.fontB, receiptPreviewLine.fontB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(text, center, bold, size, fontB);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReceiptPreviewLine {\n");
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

