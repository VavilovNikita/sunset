package com.sunsetbeach.service;

import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.entity.ReceiptSettingsEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.PrintJobMapper;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.model.ReceiptBodyFont;
import com.sunsetbeach.model.ReceiptPaperWidth;
import com.sunsetbeach.model.ReceiptPreview;
import com.sunsetbeach.model.ReceiptPreviewLine;
import com.sunsetbeach.model.ReceiptSettings;
import com.sunsetbeach.model.ReceiptSettingsInput;
import com.sunsetbeach.repository.ReceiptSettingsRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code /settings/receipt/**} - the one row V136 seeds (see {@link ReceiptSettingsEntity}), ADMIN
 * only at the route level like every {@code /settings/**}. {@link OrderPrintingService} reads
 * {@link #style()}/{@link #logoPng()} on every pre-bill and receipt, never cached, so a change
 * applies to the next print without a restart. Kitchen/bar tickets and the Z report are working
 * documents and deliberately don't read any of this.
 */
@Service
public class ReceiptSettingsService {

    static final int MAX_TITLE = 24;
    static final int MAX_TEXT_LINES = 6;
    static final int MAX_TEXT_LINE_LENGTH = 48;

    private final ReceiptSettingsRepository repository;
    private final AuditLogService auditLogService;
    private final ImageUploadValidator imageUploadValidator;
    private final PrintService printService;
    private final PrintJobMapper printJobMapper;
    private final VatSettingsService vatSettingsService;
    private final Clock clock;

    public ReceiptSettingsService(
            ReceiptSettingsRepository repository,
            AuditLogService auditLogService,
            ImageUploadValidator imageUploadValidator,
            PrintService printService,
            PrintJobMapper printJobMapper,
            VatSettingsService vatSettingsService,
            Clock clock) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.imageUploadValidator = imageUploadValidator;
        this.printService = printService;
        this.printJobMapper = printJobMapper;
        this.vatSettingsService = vatSettingsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReceiptSettings get() {
        return toDto(getEntity());
    }

    @Transactional(readOnly = true)
    public ReceiptRenderer.Style style() {
        return ReceiptRenderer.Style.from(getEntity());
    }

    /** The stored logo, or null - whether it prints at all is {@link ReceiptRenderer.Style#showLogo()}. */
    @Transactional(readOnly = true)
    public byte[] logoPng() {
        return getEntity().getLogoPng();
    }

    /** The row always exists - V136 seeds it and nothing deletes it - so a missing one is a broken database, not a 404. */
    private ReceiptSettingsEntity getEntity() {
        return repository.findById(ReceiptSettingsEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("ReceiptSettings row is missing - V136 seeds it"));
    }

    @Transactional
    public ReceiptSettings update(ReceiptSettingsInput input) {
        Normalised n = validate(input);
        ReceiptSettingsEntity entity = getEntity();

        List<String> changed = new ArrayList<>();
        changed.addAll(diff("paperWidth", entity.getPaperWidth(), n.paperWidth));
        changed.addAll(diff("bodyFont", entity.getBodyFont(), n.bodyFont));
        changed.addAll(diff("receiptTitle", entity.getReceiptTitle(), n.receiptTitle));
        changed.addAll(diff("prebillTitle", entity.getPrebillTitle(), n.prebillTitle));
        changed.addAll(diff("titleSize", entity.getTitleSize(), n.titleSize));
        changed.addAll(diff("totalSize", entity.getTotalSize(), n.totalSize));
        changed.addAll(diff("totalBold", entity.isTotalBold(), n.totalBold));
        changed.addAll(diff("headerText", entity.getHeaderText(), n.headerText));
        changed.addAll(diff("headerBold", entity.isHeaderBold(), n.headerBold));
        changed.addAll(diff("footerText", entity.getFooterText(), n.footerText));
        changed.addAll(diff("showOrderNumber", entity.isShowOrderNumber(), n.showOrderNumber));
        changed.addAll(diff("showTime", entity.isShowTime(), n.showTime));
        changed.addAll(diff("showVatLine", entity.isShowVatLine(), n.showVatLine));
        changed.addAll(diff("showPaymentMethod", entity.isShowPaymentMethod(), n.showPaymentMethod));
        changed.addAll(diff("showLogo", entity.isShowLogo(), n.showLogo));
        changed.addAll(diff("logoWidthPercent", entity.getLogoWidthPercent(), n.logoWidthPercent));

        entity.setPaperWidth(n.paperWidth);
        entity.setBodyFont(n.bodyFont);
        entity.setReceiptTitle(n.receiptTitle);
        entity.setPrebillTitle(n.prebillTitle);
        entity.setTitleSize(n.titleSize);
        entity.setTotalSize(n.totalSize);
        entity.setTotalBold(n.totalBold);
        entity.setHeaderText(n.headerText);
        entity.setHeaderBold(n.headerBold);
        entity.setFooterText(n.footerText);
        entity.setShowOrderNumber(n.showOrderNumber);
        entity.setShowTime(n.showTime);
        entity.setShowVatLine(n.showVatLine);
        entity.setShowPaymentMethod(n.showPaymentMethod);
        entity.setShowLogo(n.showLogo);
        entity.setLogoWidthPercent(n.logoWidthPercent);
        ReceiptSettingsEntity saved = repository.saveAndFlush(entity);

        // An unchanged PUT records nothing - same as VatSettingsService#update.
        if (!changed.isEmpty()) {
            auditLogService.record(
                    AuditAction.RECEIPT_SETTINGS_UPDATED,
                    AuditEntityType.SETTINGS,
                    null,
                    "Receipt layout changed: " + String.join(", ", changed));
        }
        return toDto(saved);
    }

    @Transactional
    public ReceiptSettings setLogo(MultipartFile file) {
        ImageUploadValidator.ValidatedImage image = imageUploadValidator.validate(file);
        if (".webp".equals(image.extension())) {
            throw new BadRequestException("WebP is not supported for the receipt logo - use PNG or JPEG");
        }
        byte[] stored = ReceiptLogo.normalize(image.bytes());
        ReceiptSettingsEntity entity = getEntity();
        entity.setLogoPng(stored);
        ReceiptSettingsEntity saved = repository.saveAndFlush(entity);
        auditLogService.record(AuditAction.RECEIPT_LOGO_CHANGED, AuditEntityType.SETTINGS, null, "Receipt logo uploaded");
        return toDto(saved);
    }

    @Transactional
    public ReceiptSettings removeLogo() {
        ReceiptSettingsEntity entity = getEntity();
        if (entity.getLogoPng() == null) {
            return toDto(entity);
        }
        entity.setLogoPng(null);
        entity.setShowLogo(false);
        ReceiptSettingsEntity saved = repository.saveAndFlush(entity);
        auditLogService.record(AuditAction.RECEIPT_LOGO_CHANGED, AuditEntityType.SETTINGS, null, "Receipt logo removed");
        return toDto(saved);
    }

    /** The unsaved editor state rendered on the sample order, with the stored logo. Writes nothing. */
    @Transactional(readOnly = true)
    public ReceiptPreview preview(ReceiptSettingsInput input) {
        Normalised n = validate(input);
        ReceiptRenderer.Rendered rendered = ReceiptRenderer.render(
                n.toStyle(), ReceiptRenderer.Kind.RECEIPT, sampleContent(), getEntity().getLogoPng());
        ReceiptPreview preview = new ReceiptPreview().columns(rendered.columns());
        for (ReceiptRenderer.Line l : rendered.lines()) {
            preview.addLinesItem(new ReceiptPreviewLine(l.text(), l.center(), l.bold(), l.size(), l.fontB()));
        }
        if (rendered.logo() != null) {
            preview.logoPngBase64(rendered.logo().toPngBase64()).logoWidthDots(rendered.logo().widthDots());
        }
        return preview;
    }

    /** The saved layout on the sample order, through the normal print queue. */
    @Transactional
    public PrintAttemptResult testPrint() {
        Optional<PrinterEntity> printer = printService.findActivePrinter(PrinterDepartment.CASHIER);
        if (printer.isEmpty()) {
            return new PrintAttemptResult(false);
        }
        ReceiptSettingsEntity entity = getEntity();
        ReceiptRenderer.Rendered rendered = ReceiptRenderer.render(
                ReceiptRenderer.Style.from(entity), ReceiptRenderer.Kind.RECEIPT, sampleContent(), entity.getLogoPng());
        byte[] payload = ReceiptRenderer.toEscPos(rendered, printer.get().getCodepage());
        PrintJobEntity job = printService.queueAndAttempt(
                printer.get(), PrintDocumentType.GUEST_RECEIPT, "Receipt layout test", payload);
        return new PrintAttemptResult(true).job(printJobMapper.toDto(job));
    }

    private ReceiptRenderer.Content sampleContent() {
        BigDecimal total = new BigDecimal("410.00");
        BigDecimal vatRate = vatSettingsService.currentRate();
        return new ReceiptRenderer.Content(
                "Restaurant - T5",
                "Order #1042 (ref 7F3A92C1)",
                "Time: " + TimestampFormat.readableHotel(TimestampFormat.nowUtc(clock), clock),
                List.of(
                        new ReceiptRenderer.Row("2x Pad Thai with shrimp", "320.00"),
                        new ReceiptRenderer.Row("1x Singha beer", "90.00")),
                PriceFormat.asDecimalString(total),
                "incl. VAT " + vatRate.stripTrailingZeros().toPlainString() + "%",
                PriceFormat.asDecimalString(RevenueClassification.vatInside(total, vatRate)),
                List.of("Payment: Cash"));
    }

    private record Normalised(
            String paperWidth, String bodyFont, String receiptTitle, String prebillTitle, int titleSize, int totalSize,
            boolean totalBold, String headerText, boolean headerBold, String footerText, boolean showOrderNumber,
            boolean showTime, boolean showVatLine, boolean showPaymentMethod, boolean showLogo, int logoWidthPercent) {

        ReceiptRenderer.Style toStyle() {
            return new ReceiptRenderer.Style(
                    "MM80".equals(paperWidth), "B".equals(bodyFont), receiptTitle, prebillTitle, titleSize, totalSize,
                    totalBold, headerText, headerBold, footerText, showOrderNumber, showTime, showVatLine,
                    showPaymentMethod, showLogo, logoWidthPercent);
        }
    }

    private static Normalised validate(ReceiptSettingsInput in) {
        String receiptTitle = title("receiptTitle", in.getReceiptTitle());
        String prebillTitle = title("prebillTitle", in.getPrebillTitle());
        String header = freeText("headerText", in.getHeaderText());
        String footer = freeText("footerText", in.getFooterText());
        int percent = in.getLogoWidthPercent();
        if (percent < 20 || percent > 100) {
            throw ValidationException.field("logoWidthPercent", "Between 20 and 100");
        }
        return new Normalised(
                in.getPaperWidth().getValue(),
                in.getBodyFont().getValue(),
                receiptTitle,
                prebillTitle,
                in.getTitleSize().getValue(),
                in.getTotalSize().getValue(),
                in.getTotalBold(),
                header,
                in.getHeaderBold(),
                footer,
                in.getShowOrderNumber(),
                in.getShowTime(),
                in.getShowVatLine(),
                in.getShowPaymentMethod(),
                in.getShowLogo(),
                percent);
    }

    private static String title(String field, String raw) {
        String t = raw == null ? "" : raw.strip();
        if (t.isEmpty() || t.length() > MAX_TITLE) {
            throw ValidationException.field(field, "1 to " + MAX_TITLE + " characters");
        }
        if (hasControlCharacters(t)) {
            throw ValidationException.field(field, "No line breaks or control characters");
        }
        return t;
    }

    /** Blank lines are dropped, each line trimmed - what is stored is what prints. */
    private static String freeText(String field, String raw) {
        List<String> kept = new ArrayList<>();
        if (raw != null) {
            for (String line : raw.split("\\R", -1)) {
                String t = line.strip();
                if (t.isEmpty()) {
                    continue;
                }
                if (hasControlCharacters(t)) {
                    throw ValidationException.field(field, "No control characters");
                }
                if (t.length() > MAX_TEXT_LINE_LENGTH) {
                    throw ValidationException.field(field, "Each line at most " + MAX_TEXT_LINE_LENGTH + " characters");
                }
                kept.add(t);
            }
        }
        if (kept.size() > MAX_TEXT_LINES) {
            throw ValidationException.field(field, "At most " + MAX_TEXT_LINES + " lines");
        }
        return String.join("\n", kept);
    }

    private static boolean hasControlCharacters(String s) {
        return s.chars().anyMatch(Character::isISOControl);
    }

    private static List<String> diff(String name, Object before, Object after) {
        return java.util.Objects.equals(before, after) ? List.of() : List.of(name);
    }

    private static ReceiptSettings toDto(ReceiptSettingsEntity e) {
        return new ReceiptSettings(
                ReceiptPaperWidth.fromValue(e.getPaperWidth()),
                ReceiptBodyFont.fromValue(e.getBodyFont()),
                e.getReceiptTitle(),
                e.getPrebillTitle(),
                ReceiptSettings.TitleSizeEnum.fromValue(e.getTitleSize()),
                ReceiptSettings.TotalSizeEnum.fromValue(e.getTotalSize()),
                e.isTotalBold(),
                e.getHeaderText(),
                e.isHeaderBold(),
                e.getFooterText(),
                e.isShowOrderNumber(),
                e.isShowTime(),
                e.isShowVatLine(),
                e.isShowPaymentMethod(),
                e.isShowLogo(),
                e.getLogoWidthPercent(),
                e.getLogoPng() != null,
                TimestampFormat.toUtc(e.getUpdatedAt()));
    }
}
