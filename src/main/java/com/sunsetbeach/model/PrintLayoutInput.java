package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.PrintBodyFont;
import com.sunsetbeach.model.PrintPaperWidth;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PUT /settings/print-layouts/{documentType}&#x60; and &#x60;.../preview&#x60; - every field of &#x60;PrintLayout&#x60; except &#x60;documentType&#x60;/&#x60;updatedAt&#x60;. Which fields a document reads: all of them read &#x60;paperWidth&#x60;, &#x60;bodyFont&#x60;, &#x60;title&#x60;, &#x60;titleSize&#x60;, &#x60;headerText&#x60;, &#x60;headerBold&#x60; and &#x60;footerText&#x60;. The receipt and pre-bill also read &#x60;showLogo&#x60;/&#x60;logoWidthPercent&#x60; (no other document prints the logo), &#x60;showVatLine&#x60;, &#x60;totalSize&#x60;, &#x60;totalBold&#x60;; the receipt alone reads &#x60;showPaymentMethod&#x60;. The receipt, pre-bill and both tickets read &#x60;itemSize&#x60;, &#x60;showLocation&#x60;, &#x60;locationSize&#x60;, &#x60;showOrderNumber&#x60;; the tickets alone read &#x60;showWaiter&#x60; and &#x60;showItemNotes&#x60;. &#x60;showTime&#x60; is the \&quot;Time:\&quot; line (on the Z-report, the \&quot;Closed:\&quot; line). Headers and footers are up to 6 lines of 48 characters, printed centred; a line longer than the paper&#39;s columns is wrapped. Characters outside the printer&#39;s code page print as &#x60;?&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class PrintLayoutInput {

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

  public PrintLayoutInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrintLayoutInput(PrintPaperWidth paperWidth, PrintBodyFont bodyFont, String title, Integer titleSize, String headerText, Boolean headerBold, String footerText, Boolean showLogo, Integer logoWidthPercent, Boolean showTime, Integer itemSize, Boolean showLocation, Integer locationSize, Boolean showOrderNumber, Boolean showWaiter, Boolean showItemNotes, Boolean showVatLine, Boolean showPaymentMethod, Integer totalSize, Boolean totalBold) {
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
  }

  public PrintLayoutInput paperWidth(PrintPaperWidth paperWidth) {
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

  public PrintLayoutInput bodyFont(PrintBodyFont bodyFont) {
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

  public PrintLayoutInput title(String title) {
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

  public PrintLayoutInput titleSize(Integer titleSize) {
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

  public PrintLayoutInput headerText(String headerText) {
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

  public PrintLayoutInput headerBold(Boolean headerBold) {
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

  public PrintLayoutInput footerText(String footerText) {
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

  public PrintLayoutInput showLogo(Boolean showLogo) {
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

  public PrintLayoutInput logoWidthPercent(Integer logoWidthPercent) {
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

  public PrintLayoutInput showTime(Boolean showTime) {
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

  public PrintLayoutInput itemSize(Integer itemSize) {
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

  public PrintLayoutInput showLocation(Boolean showLocation) {
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

  public PrintLayoutInput locationSize(Integer locationSize) {
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

  public PrintLayoutInput showOrderNumber(Boolean showOrderNumber) {
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

  public PrintLayoutInput showWaiter(Boolean showWaiter) {
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

  public PrintLayoutInput showItemNotes(Boolean showItemNotes) {
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

  public PrintLayoutInput showVatLine(Boolean showVatLine) {
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

  public PrintLayoutInput showPaymentMethod(Boolean showPaymentMethod) {
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

  public PrintLayoutInput totalSize(Integer totalSize) {
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

  public PrintLayoutInput totalBold(Boolean totalBold) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrintLayoutInput printLayoutInput = (PrintLayoutInput) o;
    return Objects.equals(this.paperWidth, printLayoutInput.paperWidth) &&
        Objects.equals(this.bodyFont, printLayoutInput.bodyFont) &&
        Objects.equals(this.title, printLayoutInput.title) &&
        Objects.equals(this.titleSize, printLayoutInput.titleSize) &&
        Objects.equals(this.headerText, printLayoutInput.headerText) &&
        Objects.equals(this.headerBold, printLayoutInput.headerBold) &&
        Objects.equals(this.footerText, printLayoutInput.footerText) &&
        Objects.equals(this.showLogo, printLayoutInput.showLogo) &&
        Objects.equals(this.logoWidthPercent, printLayoutInput.logoWidthPercent) &&
        Objects.equals(this.showTime, printLayoutInput.showTime) &&
        Objects.equals(this.itemSize, printLayoutInput.itemSize) &&
        Objects.equals(this.showLocation, printLayoutInput.showLocation) &&
        Objects.equals(this.locationSize, printLayoutInput.locationSize) &&
        Objects.equals(this.showOrderNumber, printLayoutInput.showOrderNumber) &&
        Objects.equals(this.showWaiter, printLayoutInput.showWaiter) &&
        Objects.equals(this.showItemNotes, printLayoutInput.showItemNotes) &&
        Objects.equals(this.showVatLine, printLayoutInput.showVatLine) &&
        Objects.equals(this.showPaymentMethod, printLayoutInput.showPaymentMethod) &&
        Objects.equals(this.totalSize, printLayoutInput.totalSize) &&
        Objects.equals(this.totalBold, printLayoutInput.totalBold);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paperWidth, bodyFont, title, titleSize, headerText, headerBold, footerText, showLogo, logoWidthPercent, showTime, itemSize, showLocation, locationSize, showOrderNumber, showWaiter, showItemNotes, showVatLine, showPaymentMethod, totalSize, totalBold);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrintLayoutInput {\n");
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

