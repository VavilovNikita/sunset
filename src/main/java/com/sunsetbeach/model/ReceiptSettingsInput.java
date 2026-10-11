package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.ReceiptBodyFont;
import com.sunsetbeach.model.ReceiptPaperWidth;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PUT /settings/receipt&#x60; and &#x60;POST /settings/receipt/preview&#x60; - every field of &#x60;ReceiptSettings&#x60; except &#x60;hasLogo&#x60;/&#x60;updatedAt&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ReceiptSettingsInput {

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

  public ReceiptSettingsInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReceiptSettingsInput(ReceiptPaperWidth paperWidth, ReceiptBodyFont bodyFont, String receiptTitle, String prebillTitle, TitleSizeEnum titleSize, TotalSizeEnum totalSize, Boolean totalBold, String headerText, Boolean headerBold, String footerText, Boolean showOrderNumber, Boolean showTime, Boolean showVatLine, Boolean showPaymentMethod, Boolean showLogo, Integer logoWidthPercent) {
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
  }

  public ReceiptSettingsInput paperWidth(ReceiptPaperWidth paperWidth) {
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

  public ReceiptSettingsInput bodyFont(ReceiptBodyFont bodyFont) {
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

  public ReceiptSettingsInput receiptTitle(String receiptTitle) {
    this.receiptTitle = receiptTitle;
    return this;
  }

  /**
   * Get receiptTitle
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

  public ReceiptSettingsInput prebillTitle(String prebillTitle) {
    this.prebillTitle = prebillTitle;
    return this;
  }

  /**
   * Get prebillTitle
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

  public ReceiptSettingsInput titleSize(TitleSizeEnum titleSize) {
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

  public ReceiptSettingsInput totalSize(TotalSizeEnum totalSize) {
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

  public ReceiptSettingsInput totalBold(Boolean totalBold) {
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

  public ReceiptSettingsInput headerText(String headerText) {
    this.headerText = headerText;
    return this;
  }

  /**
   * Get headerText
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

  public ReceiptSettingsInput headerBold(Boolean headerBold) {
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

  public ReceiptSettingsInput footerText(String footerText) {
    this.footerText = footerText;
    return this;
  }

  /**
   * Get footerText
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

  public ReceiptSettingsInput showOrderNumber(Boolean showOrderNumber) {
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

  public ReceiptSettingsInput showTime(Boolean showTime) {
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

  public ReceiptSettingsInput showVatLine(Boolean showVatLine) {
    this.showVatLine = showVatLine;
    return this;
  }

  /**
   * Get showVatLine
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

  public ReceiptSettingsInput showPaymentMethod(Boolean showPaymentMethod) {
    this.showPaymentMethod = showPaymentMethod;
    return this;
  }

  /**
   * Get showPaymentMethod
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

  public ReceiptSettingsInput showLogo(Boolean showLogo) {
    this.showLogo = showLogo;
    return this;
  }

  /**
   * Get showLogo
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

  public ReceiptSettingsInput logoWidthPercent(Integer logoWidthPercent) {
    this.logoWidthPercent = logoWidthPercent;
    return this;
  }

  /**
   * Get logoWidthPercent
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReceiptSettingsInput receiptSettingsInput = (ReceiptSettingsInput) o;
    return Objects.equals(this.paperWidth, receiptSettingsInput.paperWidth) &&
        Objects.equals(this.bodyFont, receiptSettingsInput.bodyFont) &&
        Objects.equals(this.receiptTitle, receiptSettingsInput.receiptTitle) &&
        Objects.equals(this.prebillTitle, receiptSettingsInput.prebillTitle) &&
        Objects.equals(this.titleSize, receiptSettingsInput.titleSize) &&
        Objects.equals(this.totalSize, receiptSettingsInput.totalSize) &&
        Objects.equals(this.totalBold, receiptSettingsInput.totalBold) &&
        Objects.equals(this.headerText, receiptSettingsInput.headerText) &&
        Objects.equals(this.headerBold, receiptSettingsInput.headerBold) &&
        Objects.equals(this.footerText, receiptSettingsInput.footerText) &&
        Objects.equals(this.showOrderNumber, receiptSettingsInput.showOrderNumber) &&
        Objects.equals(this.showTime, receiptSettingsInput.showTime) &&
        Objects.equals(this.showVatLine, receiptSettingsInput.showVatLine) &&
        Objects.equals(this.showPaymentMethod, receiptSettingsInput.showPaymentMethod) &&
        Objects.equals(this.showLogo, receiptSettingsInput.showLogo) &&
        Objects.equals(this.logoWidthPercent, receiptSettingsInput.logoWidthPercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paperWidth, bodyFont, receiptTitle, prebillTitle, titleSize, totalSize, totalBold, headerText, headerBold, footerText, showOrderNumber, showTime, showVatLine, showPaymentMethod, showLogo, logoWidthPercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReceiptSettingsInput {\n");
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

