package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.ReceiptBodyFont;
import com.sunsetbeach.model.ReceiptPaperWidth;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * How the guest receipt and the pre-bill look on paper (never the kitchen/bar tickets or the Z report, which are working documents). One row, seeded with what the printer produced before this existed - 80mm, Font A, bold centered title, no header/footer/logo, every optional line on - so nothing changes until an admin edits it. &#x60;headerText&#x60;/&#x60;footerText&#x60; are up to 6 lines of up to 48 characters each, printed centered; a line longer than the columns the paper and font give is wrapped by the renderer. Characters outside the printer&#39;s code page print as &#x60;?&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ReceiptSettings {

  private ReceiptPaperWidth paperWidth;

  private ReceiptBodyFont bodyFont;

  private String receiptTitle;

  private String prebillTitle;

  /**
   * Gets or Sets titleSize
   */
  public enum TitleSizeEnum {
    NUMBER_1(1),
    
    NUMBER_2(2);

    private Integer value;

    TitleSizeEnum(Integer value) {
      this.value = value;
    }

    @JsonValue
    public Integer getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TitleSizeEnum fromValue(Integer value) {
      for (TitleSizeEnum b : TitleSizeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private TitleSizeEnum titleSize;

  /**
   * Gets or Sets totalSize
   */
  public enum TotalSizeEnum {
    NUMBER_1(1),
    
    NUMBER_2(2);

    private Integer value;

    TotalSizeEnum(Integer value) {
      this.value = value;
    }

    @JsonValue
    public Integer getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TotalSizeEnum fromValue(Integer value) {
      for (TotalSizeEnum b : TotalSizeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private TotalSizeEnum totalSize;

  private Boolean totalBold;

  private String headerText;

  private Boolean headerBold;

  private String footerText;

  private Boolean showOrderNumber;

  private Boolean showTime;

  private Boolean showVatLine;

  private Boolean showPaymentMethod;

  private Boolean showLogo;

  private Integer logoWidthPercent;

  private Boolean hasLogo;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public ReceiptSettings() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReceiptSettings(ReceiptPaperWidth paperWidth, ReceiptBodyFont bodyFont, String receiptTitle, String prebillTitle, TitleSizeEnum titleSize, TotalSizeEnum totalSize, Boolean totalBold, String headerText, Boolean headerBold, String footerText, Boolean showOrderNumber, Boolean showTime, Boolean showVatLine, Boolean showPaymentMethod, Boolean showLogo, Integer logoWidthPercent, Boolean hasLogo, OffsetDateTime updatedAt) {
    this.paperWidth = paperWidth;
    this.bodyFont = bodyFont;
    this.receiptTitle = receiptTitle;
    this.prebillTitle = prebillTitle;
    this.titleSize = titleSize;
    this.totalSize = totalSize;
    this.totalBold = totalBold;
    this.headerText = headerText;
    this.headerBold = headerBold;
    this.footerText = footerText;
    this.showOrderNumber = showOrderNumber;
    this.showTime = showTime;
    this.showVatLine = showVatLine;
    this.showPaymentMethod = showPaymentMethod;
    this.showLogo = showLogo;
    this.logoWidthPercent = logoWidthPercent;
    this.hasLogo = hasLogo;
    this.updatedAt = updatedAt;
  }

  public ReceiptSettings paperWidth(ReceiptPaperWidth paperWidth) {
    this.paperWidth = paperWidth;
    return this;
  }

  /**
   * Get paperWidth
   * @return paperWidth
   */
  @NotNull @Valid 
  @JsonProperty("paperWidth")
  public ReceiptPaperWidth getPaperWidth() {
    return paperWidth;
  }

  public void setPaperWidth(ReceiptPaperWidth paperWidth) {
    this.paperWidth = paperWidth;
  }

  public ReceiptSettings bodyFont(ReceiptBodyFont bodyFont) {
    this.bodyFont = bodyFont;
    return this;
  }

  /**
   * Get bodyFont
   * @return bodyFont
   */
  @NotNull @Valid 
  @JsonProperty("bodyFont")
  public ReceiptBodyFont getBodyFont() {
    return bodyFont;
  }

  public void setBodyFont(ReceiptBodyFont bodyFont) {
    this.bodyFont = bodyFont;
  }

  public ReceiptSettings receiptTitle(String receiptTitle) {
    this.receiptTitle = receiptTitle;
    return this;
  }

  /**
   * Printed bold and centered at the top of a receipt (\" - COPY\" is appended on a reprint). 1-24 characters.
   * @return receiptTitle
   */
  @NotNull 
  @JsonProperty("receiptTitle")
  public String getReceiptTitle() {
    return receiptTitle;
  }

  public void setReceiptTitle(String receiptTitle) {
    this.receiptTitle = receiptTitle;
  }

  public ReceiptSettings prebillTitle(String prebillTitle) {
    this.prebillTitle = prebillTitle;
    return this;
  }

  /**
   * Same, for the pre-bill. 1-24 characters.
   * @return prebillTitle
   */
  @NotNull 
  @JsonProperty("prebillTitle")
  public String getPrebillTitle() {
    return prebillTitle;
  }

  public void setPrebillTitle(String prebillTitle) {
    this.prebillTitle = prebillTitle;
  }

  public ReceiptSettings titleSize(TitleSizeEnum titleSize) {
    this.titleSize = titleSize;
    return this;
  }

  /**
   * Get titleSize
   * @return titleSize
   */
  @NotNull 
  @JsonProperty("titleSize")
  public TitleSizeEnum getTitleSize() {
    return titleSize;
  }

  public void setTitleSize(TitleSizeEnum titleSize) {
    this.titleSize = titleSize;
  }

  public ReceiptSettings totalSize(TotalSizeEnum totalSize) {
    this.totalSize = totalSize;
    return this;
  }

  /**
   * Get totalSize
   * @return totalSize
   */
  @NotNull 
  @JsonProperty("totalSize")
  public TotalSizeEnum getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(TotalSizeEnum totalSize) {
    this.totalSize = totalSize;
  }

  public ReceiptSettings totalBold(Boolean totalBold) {
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

  public ReceiptSettings headerText(String headerText) {
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

  public ReceiptSettings headerBold(Boolean headerBold) {
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

  public ReceiptSettings footerText(String footerText) {
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

  public ReceiptSettings showOrderNumber(Boolean showOrderNumber) {
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

  public ReceiptSettings showTime(Boolean showTime) {
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

  public ReceiptSettings showVatLine(Boolean showVatLine) {
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

  public ReceiptSettings showPaymentMethod(Boolean showPaymentMethod) {
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

  public ReceiptSettings showLogo(Boolean showLogo) {
    this.showLogo = showLogo;
    return this;
  }

  /**
   * Only has an effect when a logo has been uploaded (`hasLogo`).
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

  public ReceiptSettings logoWidthPercent(Integer logoWidthPercent) {
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

  public ReceiptSettings hasLogo(Boolean hasLogo) {
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

  public ReceiptSettings updatedAt(OffsetDateTime updatedAt) {
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
    ReceiptSettings receiptSettings = (ReceiptSettings) o;
    return Objects.equals(this.paperWidth, receiptSettings.paperWidth) &&
        Objects.equals(this.bodyFont, receiptSettings.bodyFont) &&
        Objects.equals(this.receiptTitle, receiptSettings.receiptTitle) &&
        Objects.equals(this.prebillTitle, receiptSettings.prebillTitle) &&
        Objects.equals(this.titleSize, receiptSettings.titleSize) &&
        Objects.equals(this.totalSize, receiptSettings.totalSize) &&
        Objects.equals(this.totalBold, receiptSettings.totalBold) &&
        Objects.equals(this.headerText, receiptSettings.headerText) &&
        Objects.equals(this.headerBold, receiptSettings.headerBold) &&
        Objects.equals(this.footerText, receiptSettings.footerText) &&
        Objects.equals(this.showOrderNumber, receiptSettings.showOrderNumber) &&
        Objects.equals(this.showTime, receiptSettings.showTime) &&
        Objects.equals(this.showVatLine, receiptSettings.showVatLine) &&
        Objects.equals(this.showPaymentMethod, receiptSettings.showPaymentMethod) &&
        Objects.equals(this.showLogo, receiptSettings.showLogo) &&
        Objects.equals(this.logoWidthPercent, receiptSettings.logoWidthPercent) &&
        Objects.equals(this.hasLogo, receiptSettings.hasLogo) &&
        Objects.equals(this.updatedAt, receiptSettings.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paperWidth, bodyFont, receiptTitle, prebillTitle, titleSize, totalSize, totalBold, headerText, headerBold, footerText, showOrderNumber, showTime, showVatLine, showPaymentMethod, showLogo, logoWidthPercent, hasLogo, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReceiptSettings {\n");
    sb.append("    paperWidth: ").append(toIndentedString(paperWidth)).append("\n");
    sb.append("    bodyFont: ").append(toIndentedString(bodyFont)).append("\n");
    sb.append("    receiptTitle: ").append(toIndentedString(receiptTitle)).append("\n");
    sb.append("    prebillTitle: ").append(toIndentedString(prebillTitle)).append("\n");
    sb.append("    titleSize: ").append(toIndentedString(titleSize)).append("\n");
    sb.append("    totalSize: ").append(toIndentedString(totalSize)).append("\n");
    sb.append("    totalBold: ").append(toIndentedString(totalBold)).append("\n");
    sb.append("    headerText: ").append(toIndentedString(headerText)).append("\n");
    sb.append("    headerBold: ").append(toIndentedString(headerBold)).append("\n");
    sb.append("    footerText: ").append(toIndentedString(footerText)).append("\n");
    sb.append("    showOrderNumber: ").append(toIndentedString(showOrderNumber)).append("\n");
    sb.append("    showTime: ").append(toIndentedString(showTime)).append("\n");
    sb.append("    showVatLine: ").append(toIndentedString(showVatLine)).append("\n");
    sb.append("    showPaymentMethod: ").append(toIndentedString(showPaymentMethod)).append("\n");
    sb.append("    showLogo: ").append(toIndentedString(showLogo)).append("\n");
    sb.append("    logoWidthPercent: ").append(toIndentedString(logoWidthPercent)).append("\n");
    sb.append("    hasLogo: ").append(toIndentedString(hasLogo)).append("\n");
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

