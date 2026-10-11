package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * How one printed document looks - one row per document type, see V137. Every column exists for
 * every document; which of them a document actually reads is the renderer's business.
 */
@Entity
@Table(name = "PrintLayout")
public class PrintLayoutEntity {

    @Id
    private String documentType;

    private String paperWidth;
    private String bodyFont;
    private String title;
    private Integer titleSize;
    private String headerText;
    private boolean headerBold;
    private String footerText;
    private boolean showLogo;
    private Integer logoWidthPercent;
    private boolean showTime;
    private Integer itemSize;
    private boolean showLocation;
    private Integer locationSize;
    private boolean showOrderNumber;
    private boolean showWaiter;
    private boolean showItemNotes;
    private boolean showVatLine;
    private boolean showPaymentMethod;
    private Integer totalSize;
    private boolean totalBold;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public String getDocumentType() { return documentType; }
    public String getPaperWidth() { return paperWidth; }
    public void setPaperWidth(String v) { this.paperWidth = v; }
    public String getBodyFont() { return bodyFont; }
    public void setBodyFont(String v) { this.bodyFont = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public Integer getTitleSize() { return titleSize; }
    public void setTitleSize(Integer v) { this.titleSize = v; }
    public String getHeaderText() { return headerText; }
    public void setHeaderText(String v) { this.headerText = v; }
    public boolean isHeaderBold() { return headerBold; }
    public void setHeaderBold(boolean v) { this.headerBold = v; }
    public String getFooterText() { return footerText; }
    public void setFooterText(String v) { this.footerText = v; }
    public boolean isShowLogo() { return showLogo; }
    public void setShowLogo(boolean v) { this.showLogo = v; }
    public Integer getLogoWidthPercent() { return logoWidthPercent; }
    public void setLogoWidthPercent(Integer v) { this.logoWidthPercent = v; }
    public boolean isShowTime() { return showTime; }
    public void setShowTime(boolean v) { this.showTime = v; }
    public Integer getItemSize() { return itemSize; }
    public void setItemSize(Integer v) { this.itemSize = v; }
    public boolean isShowLocation() { return showLocation; }
    public void setShowLocation(boolean v) { this.showLocation = v; }
    public Integer getLocationSize() { return locationSize; }
    public void setLocationSize(Integer v) { this.locationSize = v; }
    public boolean isShowOrderNumber() { return showOrderNumber; }
    public void setShowOrderNumber(boolean v) { this.showOrderNumber = v; }
    public boolean isShowWaiter() { return showWaiter; }
    public void setShowWaiter(boolean v) { this.showWaiter = v; }
    public boolean isShowItemNotes() { return showItemNotes; }
    public void setShowItemNotes(boolean v) { this.showItemNotes = v; }
    public boolean isShowVatLine() { return showVatLine; }
    public void setShowVatLine(boolean v) { this.showVatLine = v; }
    public boolean isShowPaymentMethod() { return showPaymentMethod; }
    public void setShowPaymentMethod(boolean v) { this.showPaymentMethod = v; }
    public Integer getTotalSize() { return totalSize; }
    public void setTotalSize(Integer v) { this.totalSize = v; }
    public boolean isTotalBold() { return totalBold; }
    public void setTotalBold(boolean v) { this.totalBold = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
