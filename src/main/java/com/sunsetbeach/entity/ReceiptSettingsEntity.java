package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** The single row describing how the guest receipt and pre-bill look - see V136. Same singleton shape as {@link VatSettingsEntity}. */
@Entity
@Table(name = "ReceiptSettings")
public class ReceiptSettingsEntity {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    private String paperWidth;
    private String bodyFont;
    private String receiptTitle;
    private String prebillTitle;
    private Integer titleSize;
    private Integer totalSize;
    private boolean totalBold;
    private String headerText;
    private boolean headerBold;
    private String footerText;
    private boolean showOrderNumber;
    private boolean showTime;
    private boolean showVatLine;
    private boolean showPaymentMethod;
    private boolean showLogo;
    private Integer logoWidthPercent;

    private byte[] logoPng;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public String getPaperWidth() { return paperWidth; }
    public void setPaperWidth(String paperWidth) { this.paperWidth = paperWidth; }
    public String getBodyFont() { return bodyFont; }
    public void setBodyFont(String bodyFont) { this.bodyFont = bodyFont; }
    public String getReceiptTitle() { return receiptTitle; }
    public void setReceiptTitle(String receiptTitle) { this.receiptTitle = receiptTitle; }
    public String getPrebillTitle() { return prebillTitle; }
    public void setPrebillTitle(String prebillTitle) { this.prebillTitle = prebillTitle; }
    public Integer getTitleSize() { return titleSize; }
    public void setTitleSize(Integer titleSize) { this.titleSize = titleSize; }
    public Integer getTotalSize() { return totalSize; }
    public void setTotalSize(Integer totalSize) { this.totalSize = totalSize; }
    public boolean isTotalBold() { return totalBold; }
    public void setTotalBold(boolean totalBold) { this.totalBold = totalBold; }
    public String getHeaderText() { return headerText; }
    public void setHeaderText(String headerText) { this.headerText = headerText; }
    public boolean isHeaderBold() { return headerBold; }
    public void setHeaderBold(boolean headerBold) { this.headerBold = headerBold; }
    public String getFooterText() { return footerText; }
    public void setFooterText(String footerText) { this.footerText = footerText; }
    public boolean isShowOrderNumber() { return showOrderNumber; }
    public void setShowOrderNumber(boolean showOrderNumber) { this.showOrderNumber = showOrderNumber; }
    public boolean isShowTime() { return showTime; }
    public void setShowTime(boolean showTime) { this.showTime = showTime; }
    public boolean isShowVatLine() { return showVatLine; }
    public void setShowVatLine(boolean showVatLine) { this.showVatLine = showVatLine; }
    public boolean isShowPaymentMethod() { return showPaymentMethod; }
    public void setShowPaymentMethod(boolean showPaymentMethod) { this.showPaymentMethod = showPaymentMethod; }
    public boolean isShowLogo() { return showLogo; }
    public void setShowLogo(boolean showLogo) { this.showLogo = showLogo; }
    public Integer getLogoWidthPercent() { return logoWidthPercent; }
    public void setLogoWidthPercent(Integer logoWidthPercent) { this.logoWidthPercent = logoWidthPercent; }
    public byte[] getLogoPng() { return logoPng; }
    public void setLogoPng(byte[] logoPng) { this.logoPng = logoPng; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
