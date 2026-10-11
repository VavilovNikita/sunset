package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.PrintBodyFont;
import com.sunsetbeach.model.PrintLayoutDocument;
import com.sunsetbeach.model.PrintPaperWidth;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * How one printed document looks on paper. Seeded with what each document printed before this existed - 80mm, Font A, bold centred title, no header/footer/logo, every optional line on - so nothing changes until an admin edits it. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PrintLayout {

  private PrintPaperWidth paperWidth;

  private PrintBodyFont bodyFont;

  private String title;

  private Integer titleSize;

  private String headerText;

  private Boolean headerBold;

  private String footerText;

  private Boolean showLogo;

  private Integer logoWidthPercent;

  private Boolean showTime;

  private Integer itemSize;

  private Boolean showLocation;

  private Integer locationSize;

  private Boolean showOrderNumber;

  private Boolean showWaiter;

  private Boolean showItemNotes;

  private Boolean showVatLine;

  private Boolean showPaymentMethod;

  private Integer totalSize;

  private Boolean totalBold;

  private PrintLayoutDocument documentType;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public PrintLayout() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrintLayout(PrintPaperWidth paperWidth, PrintBodyFont bodyFont, String title, Integer titleSize, String headerText, Boolean headerBold, String footerText, Boolean showLogo, Integer logoWidthPercent, Boolean showTime, Integer itemSize, Boolean showLocation, Integer locationSize, Boolean showOrderNumber, Boolean showWaiter, Boolean showItemNotes, Boolean showVatLine, Boolean showPaymentMethod, Integer totalSize, Boolean totalBold, PrintLayoutDocument documentType, OffsetDateTime updatedAt) {
    this.paperWidth = paperWidth;
    this.bodyFont = bodyFont;
    this.title = title;
    this.titleSize = titleSize;
    this.headerText = headerText;
    this.headerBold = headerBold;
    this.footerText = footerText;
    this.showLogo = showLogo;
    this.logoWidthPercent = logoWidthPercent;
    this.showTime = showTime;
    this.itemSize = itemSize;
    this.showLocation = showLocation;
    this.locationSize = locationSize;
    this.showOrderNumber = showOrderNumber;
    this.showWaiter = showWaiter;
    this.showItemNotes = showItemNotes;
    this.showVatLine = showVatLine;
    this.showPaymentMethod = showPaymentMethod;
    this.totalSize = totalSize;
    this.totalBold = totalBold;
    this.documentType = documentType;
    this.updatedAt = updatedAt;
  }

  public PrintLayout paperWidth(PrintPaperWidth paperWidth) {
    this.paperWidth = paperWidth;
    return this;
  }

  /**
   * Get paperWidth
   * @return paperWidth
   */
  @NotNull @Valid 
  @JsonProperty("paperWidth")
  public PrintPaperWidth getPaperWidth() {
    return paperWidth;
  }

  public void setPaperWidth(PrintPaperWidth paperWidth) {
    this.paperWidth = paperWidth;
  }

  public PrintLayout bodyFont(PrintBodyFont bodyFont) {
    this.bodyFont = bodyFont;
    return this;
  }

  /**
   * Get bodyFont
   * @return bodyFont
   */
  @NotNull @Valid 
  @JsonProperty("bodyFont")
  public PrintBodyFont getBodyFont() {
    return bodyFont;
  }

  public void setBodyFont(PrintBodyFont bodyFont) {
    this.bodyFont = bodyFont;
  }

  public PrintLayout title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Printed bold and centred at the top. 1-24 characters.
   * @return title
   */
  @NotNull 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public PrintLayout titleSize(Integer titleSize) {
    this.titleSize = titleSize;
    return this;
  }

  /**
   * Glyph magnification - ESC/POS has only multiples of the base glyph, no point sizes.
   * minimum: 1
   * maximum: 2
   * @return titleSize
   */
  @NotNull @Min(1) @Max(2) 
  @JsonProperty("titleSize")
  public Integer getTitleSize() {
    return titleSize;
  }

  public void setTitleSize(Integer titleSize) {
    this.titleSize = titleSize;
  }

  public PrintLayout headerText(String headerText) {
    this.headerText = headerText;
    return this;
  }

  /**
   * Under the logo, above the title - hotel name, address, phone, tax id. Newline-separated.
   * @return headerText
   */
  @NotNull 
  @JsonProperty("headerText")
  public String getHeaderText() {
    return headerText;
  }

  public void setHeaderText(String headerText) {
    this.headerText = headerText;
  }

  public PrintLayout headerBold(Boolean headerBold) {
    this.headerBold = headerBold;
    return this;
  }

  /**
   * Get headerBold
   * @return headerBold
   */
  @NotNull 
  @JsonProperty("headerBold")
  public Boolean getHeaderBold() {
    return headerBold;
  }

  public void setHeaderBold(Boolean headerBold) {
    this.headerBold = headerBold;
  }

  public PrintLayout footerText(String footerText) {
    this.footerText = footerText;
    return this;
  }

  /**
   * Last lines, e.g. a thank-you or opening hours. Newline-separated.
   * @return footerText
   */
  @NotNull 
  @JsonProperty("footerText")
  public String getFooterText() {
    return footerText;
  }

  public void setFooterText(String footerText) {
    this.footerText = footerText;
  }

  public PrintLayout showLogo(Boolean showLogo) {
    this.showLogo = showLogo;
    return this;
  }

  /**
   * Only has an effect on the receipt and pre-bill, and only when a logo is uploaded (`PrintLayouts.hasLogo`).
   * @return showLogo
   */
  @NotNull 
  @JsonProperty("showLogo")
  public Boolean getShowLogo() {
    return showLogo;
  }

  public void setShowLogo(Boolean showLogo) {
    this.showLogo = showLogo;
  }

  public PrintLayout logoWidthPercent(Integer logoWidthPercent) {
    this.logoWidthPercent = logoWidthPercent;
    return this;
  }

  /**
   * Width of the printed logo as a share of the paper's dot width.
   * minimum: 20
   * maximum: 100
   * @return logoWidthPercent
   */
  @NotNull @Min(20) @Max(100) 
  @JsonProperty("logoWidthPercent")
  public Integer getLogoWidthPercent() {
    return logoWidthPercent;
  }

  public void setLogoWidthPercent(Integer logoWidthPercent) {
    this.logoWidthPercent = logoWidthPercent;
  }

  public PrintLayout showTime(Boolean showTime) {
    this.showTime = showTime;
    return this;
  }

  /**
   * Get showTime
   * @return showTime
   */
  @NotNull 
  @JsonProperty("showTime")
  public Boolean getShowTime() {
    return showTime;
  }

  public void setShowTime(Boolean showTime) {
    this.showTime = showTime;
  }

  public PrintLayout itemSize(Integer itemSize) {
    this.itemSize = itemSize;
    return this;
  }

  /**
   * Magnification of the item lines (and their notes). 2 halves the columns an item name has.
   * minimum: 1
   * maximum: 2
   * @return itemSize
   */
  @NotNull @Min(1) @Max(2) 
  @JsonProperty("itemSize")
  public Integer getItemSize() {
    return itemSize;
  }

  public void setItemSize(Integer itemSize) {
    this.itemSize = itemSize;
  }

  public PrintLayout showLocation(Boolean showLocation) {
    this.showLocation = showLocation;
    return this;
  }

  /**
   * The table / zone / takeaway line.
   * @return showLocation
   */
  @NotNull 
  @JsonProperty("showLocation")
  public Boolean getShowLocation() {
    return showLocation;
  }

  public void setShowLocation(Boolean showLocation) {
    this.showLocation = showLocation;
  }

  public PrintLayout locationSize(Integer locationSize) {
    this.locationSize = locationSize;
    return this;
  }

  /**
   * Get locationSize
   * minimum: 1
   * maximum: 2
   * @return locationSize
   */
  @NotNull @Min(1) @Max(2) 
  @JsonProperty("locationSize")
  public Integer getLocationSize() {
    return locationSize;
  }

  public void setLocationSize(Integer locationSize) {
    this.locationSize = locationSize;
  }

  public PrintLayout showOrderNumber(Boolean showOrderNumber) {
    this.showOrderNumber = showOrderNumber;
    return this;
  }

  /**
   * Get showOrderNumber
   * @return showOrderNumber
   */
  @NotNull 
  @JsonProperty("showOrderNumber")
  public Boolean getShowOrderNumber() {
    return showOrderNumber;
  }

  public void setShowOrderNumber(Boolean showOrderNumber) {
    this.showOrderNumber = showOrderNumber;
  }

  public PrintLayout showWaiter(Boolean showWaiter) {
    this.showWaiter = showWaiter;
    return this;
  }

  /**
   * Tickets only - who opened the order.
   * @return showWaiter
   */
  @NotNull 
  @JsonProperty("showWaiter")
  public Boolean getShowWaiter() {
    return showWaiter;
  }

  public void setShowWaiter(Boolean showWaiter) {
    this.showWaiter = showWaiter;
  }

  public PrintLayout showItemNotes(Boolean showItemNotes) {
    this.showItemNotes = showItemNotes;
    return this;
  }

  /**
   * Tickets only - the \"note:\" line under an item.
   * @return showItemNotes
   */
  @NotNull 
  @JsonProperty("showItemNotes")
  public Boolean getShowItemNotes() {
    return showItemNotes;
  }

  public void setShowItemNotes(Boolean showItemNotes) {
    this.showItemNotes = showItemNotes;
  }

  public PrintLayout showVatLine(Boolean showVatLine) {
    this.showVatLine = showVatLine;
    return this;
  }

  /**
   * The \"incl. VAT x%\" line under the total - a breakdown of the total, never added to it.
   * @return showVatLine
   */
  @NotNull 
  @JsonProperty("showVatLine")
  public Boolean getShowVatLine() {
    return showVatLine;
  }

  public void setShowVatLine(Boolean showVatLine) {
    this.showVatLine = showVatLine;
  }

  public PrintLayout showPaymentMethod(Boolean showPaymentMethod) {
    this.showPaymentMethod = showPaymentMethod;
    return this;
  }

  /**
   * The payment method / room-charge block on a paid receipt.
   * @return showPaymentMethod
   */
  @NotNull 
  @JsonProperty("showPaymentMethod")
  public Boolean getShowPaymentMethod() {
    return showPaymentMethod;
  }

  public void setShowPaymentMethod(Boolean showPaymentMethod) {
    this.showPaymentMethod = showPaymentMethod;
  }

  public PrintLayout totalSize(Integer totalSize) {
    this.totalSize = totalSize;
    return this;
  }

  /**
   * Get totalSize
   * minimum: 1
   * maximum: 2
   * @return totalSize
   */
  @NotNull @Min(1) @Max(2) 
  @JsonProperty("totalSize")
  public Integer getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(Integer totalSize) {
    this.totalSize = totalSize;
  }

  public PrintLayout totalBold(Boolean totalBold) {
    this.totalBold = totalBold;
    return this;
  }

  /**
   * Get totalBold
   * @return totalBold
   */
  @NotNull 
  @JsonProperty("totalBold")
  public Boolean getTotalBold() {
    return totalBold;
  }

  public void setTotalBold(Boolean totalBold) {
    this.totalBold = totalBold;
  }

  public PrintLayout documentType(PrintLayoutDocument documentType) {
    this.documentType = documentType;
    return this;
  }

  /**
   * Get documentType
   * @return documentType
   */
  @NotNull @Valid 
  @JsonProperty("documentType")
  public PrintLayoutDocument getDocumentType() {
    return documentType;
  }

  public void setDocumentType(PrintLayoutDocument documentType) {
    this.documentType = documentType;
  }

  public PrintLayout updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrintLayout printLayout = (PrintLayout) o;
    return Objects.equals(this.paperWidth, printLayout.paperWidth) &&
        Objects.equals(this.bodyFont, printLayout.bodyFont) &&
        Objects.equals(this.title, printLayout.title) &&
        Objects.equals(this.titleSize, printLayout.titleSize) &&
        Objects.equals(this.headerText, printLayout.headerText) &&
        Objects.equals(this.headerBold, printLayout.headerBold) &&
        Objects.equals(this.footerText, printLayout.footerText) &&
        Objects.equals(this.showLogo, printLayout.showLogo) &&
        Objects.equals(this.logoWidthPercent, printLayout.logoWidthPercent) &&
        Objects.equals(this.showTime, printLayout.showTime) &&
        Objects.equals(this.itemSize, printLayout.itemSize) &&
        Objects.equals(this.showLocation, printLayout.showLocation) &&
        Objects.equals(this.locationSize, printLayout.locationSize) &&
        Objects.equals(this.showOrderNumber, printLayout.showOrderNumber) &&
        Objects.equals(this.showWaiter, printLayout.showWaiter) &&
        Objects.equals(this.showItemNotes, printLayout.showItemNotes) &&
        Objects.equals(this.showVatLine, printLayout.showVatLine) &&
        Objects.equals(this.showPaymentMethod, printLayout.showPaymentMethod) &&
        Objects.equals(this.totalSize, printLayout.totalSize) &&
        Objects.equals(this.totalBold, printLayout.totalBold) &&
        Objects.equals(this.documentType, printLayout.documentType) &&
        Objects.equals(this.updatedAt, printLayout.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paperWidth, bodyFont, title, titleSize, headerText, headerBold, footerText, showLogo, logoWidthPercent, showTime, itemSize, showLocation, locationSize, showOrderNumber, showWaiter, showItemNotes, showVatLine, showPaymentMethod, totalSize, totalBold, documentType, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrintLayout {\n");
    sb.append("    paperWidth: ").append(toIndentedString(paperWidth)).append("\n");
    sb.append("    bodyFont: ").append(toIndentedString(bodyFont)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    titleSize: ").append(toIndentedString(titleSize)).append("\n");
    sb.append("    headerText: ").append(toIndentedString(headerText)).append("\n");
    sb.append("    headerBold: ").append(toIndentedString(headerBold)).append("\n");
    sb.append("    footerText: ").append(toIndentedString(footerText)).append("\n");
    sb.append("    showLogo: ").append(toIndentedString(showLogo)).append("\n");
    sb.append("    logoWidthPercent: ").append(toIndentedString(logoWidthPercent)).append("\n");
    sb.append("    showTime: ").append(toIndentedString(showTime)).append("\n");
    sb.append("    itemSize: ").append(toIndentedString(itemSize)).append("\n");
    sb.append("    showLocation: ").append(toIndentedString(showLocation)).append("\n");
    sb.append("    locationSize: ").append(toIndentedString(locationSize)).append("\n");
    sb.append("    showOrderNumber: ").append(toIndentedString(showOrderNumber)).append("\n");
    sb.append("    showWaiter: ").append(toIndentedString(showWaiter)).append("\n");
    sb.append("    showItemNotes: ").append(toIndentedString(showItemNotes)).append("\n");
    sb.append("    showVatLine: ").append(toIndentedString(showVatLine)).append("\n");
    sb.append("    showPaymentMethod: ").append(toIndentedString(showPaymentMethod)).append("\n");
    sb.append("    totalSize: ").append(toIndentedString(totalSize)).append("\n");
    sb.append("    totalBold: ").append(toIndentedString(totalBold)).append("\n");
    sb.append("    documentType: ").append(toIndentedString(documentType)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

