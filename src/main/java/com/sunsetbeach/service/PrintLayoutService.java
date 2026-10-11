package com.sunsetbeach.service;

import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.entity.PrintLayoutEntity;
import com.sunsetbeach.entity.PrintLogoEntity;
import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.PrintJobMapper;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.PrintBodyFont;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintLayout;
import com.sunsetbeach.model.PrintLayoutDocument;
import com.sunsetbeach.model.PrintLayoutInput;
import com.sunsetbeach.model.PrintLayoutPreview;
import com.sunsetbeach.model.PrintLayouts;
import com.sunsetbeach.model.PrintPaperWidth;
import com.sunsetbeach.model.PrintPreviewLine;
import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.repository.PrintLayoutRepository;
import com.sunsetbeach.repository.PrintLogoRepository;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code /settings/print-layouts/**} - one {@code PrintLayout} row per printed document (V137),
 * ADMIN only at the route level like every {@code /settings/**}. {@link OrderPrintingService} and
 * {@link ShiftService} read {@link #style} / {@link #logoPng} on every document they print, never
 * cached, so a change applies to the next print without a restart.
 */
@Service
public class PrintLayoutService {

    static final int MAX_TITLE = 24;
    static final int MAX_TEXT_LINES = 6;
    static final int MAX_TEXT_LINE_LENGTH = 48;
    private static final String SAMPLE_NOTE = "- sample, not a real order -";

    private final PrintLayoutRepository repository;
    private final PrintLogoRepository logoRepository;
    private final AuditLogService auditLogService;
    private final ImageUploadValidator imageUploadValidator;
    private final PrintService printService;
    private final PrintJobMapper printJobMapper;
    private final VatSettingsService vatSettingsService;
    private final Clock clock;

    public PrintLayoutService(
            PrintLayoutRepository repository,
            PrintLogoRepository logoRepository,
            AuditLogService auditLogService,
            ImageUploadValidator imageUploadValidator,
            PrintService printService,
            PrintJobMapper printJobMapper,
            VatSettingsService vatSettingsService,
            Clock clock) {
        this.repository = repository;
        this.logoRepository = logoRepository;
        this.auditLogService = auditLogService;
        this.imageUploadValidator = imageUploadValidator;
        this.printService = printService;
        this.printJobMapper = printJobMapper;
        this.vatSettingsService = vatSettingsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PrintLayouts list() {
        return listDto();
    }

    private PrintLayouts listDto() {
        List<PrintLayout> layouts = new ArrayList<>();
        for (PrintLayoutDocument doc : PrintLayoutDocument.values()) {
            layouts.add(toDto(getEntity(doc)));
        }
        return new PrintLayouts(logoEntity().getLogoPng() != null, layouts);
    }

    @Transactional(readOnly = true)
    public PrintRenderer.Style style(PrintLayoutDocument doc) {
        return PrintRenderer.Style.from(getEntity(doc));
    }

    /** The stored logo, or null - whether a document prints it is its layout's {@code showLogo}. */
    @Transactional(readOnly = true)
    public byte[] logoPng() {
        return logoEntity().getLogoPng();
    }

    /** The rows always exist - V137 seeds all of them and nothing deletes one - so a missing one is a broken database, not a 404. */
    private PrintLayoutEntity getEntity(PrintLayoutDocument doc) {
        return repository.findById(doc.getValue())
                .orElseThrow(() -> new IllegalStateException("PrintLayout row for " + doc + " is missing - V137 seeds it"));
    }

    private PrintLogoEntity logoEntity() {
        return logoRepository.findById(PrintLogoEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("PrintLogo row is missing - V137 seeds it"));
    }

    @Transactional
    public PrintLayout update(PrintLayoutDocument doc, PrintLayoutInput input) {
        Normalised n = validate(input);
        PrintLayoutEntity entity = getEntity(doc);

        List<String> changed = changedFields(Normalised.of(entity), n);

        entity.setPaperWidth(n.paperWidth);
        entity.setBodyFont(n.bodyFont);
        entity.setTitle(n.title);
        entity.setTitleSize(n.titleSize);
        entity.setHeaderText(n.headerText);
        entity.setHeaderBold(n.headerBold);
        entity.setFooterText(n.footerText);
        entity.setShowLogo(n.showLogo);
        entity.setLogoWidthPercent(n.logoWidthPercent);
        entity.setShowTime(n.showTime);
        entity.setItemSize(n.itemSize);
        entity.setShowLocation(n.showLocation);
        entity.setLocationSize(n.locationSize);
        entity.setShowOrderNumber(n.showOrderNumber);
        entity.setShowWaiter(n.showWaiter);
        entity.setShowItemNotes(n.showItemNotes);
        entity.setShowVatLine(n.showVatLine);
        entity.setShowPaymentMethod(n.showPaymentMethod);
        entity.setTotalSize(n.totalSize);
        entity.setTotalBold(n.totalBold);
        PrintLayoutEntity saved = repository.saveAndFlush(entity);

        // An unchanged PUT records nothing - same as VatSettingsService#update.
        if (!changed.isEmpty()) {
            auditLogService.record(
                    AuditAction.RECEIPT_SETTINGS_UPDATED,
                    AuditEntityType.SETTINGS,
                    doc.getValue(),
                    "Print layout " + doc.getValue() + " changed: " + String.join(", ", changed));
        }
        return toDto(saved);
    }

    @Transactional
    public PrintLayouts setLogo(MultipartFile file) {
        ImageUploadValidator.ValidatedImage image = imageUploadValidator.validate(file);
        if (".webp".equals(image.extension())) {
            throw new BadRequestException("WebP is not supported for the receipt logo - use PNG or JPEG");
        }
        byte[] stored = ReceiptLogo.normalize(image.bytes());
        PrintLogoEntity logo = logoEntity();
        logo.setLogoPng(stored);
        logoRepository.saveAndFlush(logo);
        auditLogService.record(AuditAction.RECEIPT_LOGO_CHANGED, AuditEntityType.SETTINGS, null, "Receipt logo uploaded");
        return listDto();
    }

    @Transactional
    public PrintLayouts removeLogo() {
        PrintLogoEntity logo = logoEntity();
        if (logo.getLogoPng() == null) {
            return listDto();
        }
        logo.setLogoPng(null);
        logoRepository.saveAndFlush(logo);
        // A layout can't keep "print the logo" switched on with nothing to print.
        for (PrintLayoutDocument doc : PrintLayoutDocument.values()) {
            PrintLayoutEntity layout = getEntity(doc);
            if (layout.isShowLogo()) {
                layout.setShowLogo(false);
                repository.save(layout);
            }
        }
        repository.flush();
        auditLogService.record(AuditAction.RECEIPT_LOGO_CHANGED, AuditEntityType.SETTINGS, null, "Receipt logo removed");
        return listDto();
    }

    /** The unsaved editor state rendered on the document's sample, with the stored logo. Writes nothing. */
    @Transactional(readOnly = true)
    public PrintLayoutPreview preview(PrintLayoutDocument doc, PrintLayoutInput input) {
        PrintRenderer.Rendered rendered = renderSample(doc, validate(input).toStyle(), logoEntity().getLogoPng());
        PrintLayoutPreview preview = new PrintLayoutPreview().columns(rendered.columns());
        for (PrintRenderer.Line l : rendered.lines()) {
            preview.addLinesItem(new PrintPreviewLine(l.text(), l.center(), l.bold(), l.size(), l.fontB()));
        }
        if (rendered.logo() != null) {
            preview.logoPngBase64(rendered.logo().toPngBase64()).logoWidthDots(rendered.logo().widthDots());
        }
        return preview;
    }

    /** The saved layout on the document's sample, on its department's printer, through the normal print queue. */
    @Transactional
    public PrintAttemptResult testPrint(PrintLayoutDocument doc) {
        Optional<PrinterEntity> printer = printService.findActivePrinter(departmentOf(doc));
        if (printer.isEmpty()) {
            return new PrintAttemptResult(false);
        }
        PrintRenderer.Rendered rendered = renderSample(doc, style(doc), logoEntity().getLogoPng());
        PrinterCodepage codepage = printer.get().getCodepage();
        PrintJobEntity job = printService.queueAndAttempt(
                printer.get(), documentTypeOf(doc), "Layout test — " + doc.getValue(), PrintRenderer.toEscPos(rendered, codepage));
        return new PrintAttemptResult(true).job(printJobMapper.toDto(job));
    }

    private static PrinterDepartment departmentOf(PrintLayoutDocument doc) {
        return switch (doc) {
            case KITCHEN_TICKET -> PrinterDepartment.KITCHEN;
            case BAR_TICKET -> PrinterDepartment.BAR;
            case RECEIPT, PREBILL, Z_REPORT -> PrinterDepartment.CASHIER;
        };
    }

    private static PrintDocumentType documentTypeOf(PrintLayoutDocument doc) {
        return switch (doc) {
            case RECEIPT -> PrintDocumentType.GUEST_RECEIPT;
            case PREBILL -> PrintDocumentType.PREBILL;
            case KITCHEN_TICKET -> PrintDocumentType.KITCHEN_TICKET;
            case BAR_TICKET -> PrintDocumentType.BAR_TICKET;
            case Z_REPORT -> PrintDocumentType.Z_REPORT;
        };
    }

    private PrintRenderer.Rendered renderSample(PrintLayoutDocument doc, PrintRenderer.Style style, byte[] logoPng) {
        String time = "Time: " + TimestampFormat.readableHotel(TimestampFormat.nowUtc(clock), clock);
        PrintRenderer.OrderHead head = new PrintRenderer.OrderHead("Restaurant - T5", "Order #1042 (ref 7F3A92C1)", time);
        return switch (doc) {
            case RECEIPT, PREBILL -> {
                BigDecimal total = new BigDecimal("410.00");
                BigDecimal vatRate = vatSettingsService.currentRate();
                yield PrintRenderer.receipt(
                        style,
                        doc == PrintLayoutDocument.PREBILL ? PrintRenderer.ReceiptKind.PREBILL : PrintRenderer.ReceiptKind.RECEIPT,
                        new PrintRenderer.ReceiptContent(
                                head,
                                List.of(
                                        new PrintRenderer.Row("2x Pad Thai with shrimp", "320.00"),
                                        new PrintRenderer.Row("1x Singha beer", "90.00")),
                                PriceFormat.asDecimalString(total),
                                "incl. VAT " + vatRate.stripTrailingZeros().toPlainString() + "%",
                                PriceFormat.asDecimalString(RevenueClassification.vatInside(total, vatRate)),
                                doc == PrintLayoutDocument.PREBILL ? null : List.of("Payment: Cash"),
                                SAMPLE_NOTE),
                        logoPng);
            }
            case KITCHEN_TICKET -> PrintRenderer.ticket(
                    style,
                    new PrintRenderer.TicketContent(
                            new PrintRenderer.OrderHead(head.location(), "Order #1042", time),
                            "Waiter: anna@example.com",
                            List.of(
                                    new PrintRenderer.TicketItem("2x Pad Thai with shrimp", "no peanuts"),
                                    new PrintRenderer.TicketItem("1x Green curry", null)),
                            SAMPLE_NOTE));
            case BAR_TICKET -> PrintRenderer.ticket(
                    style,
                    new PrintRenderer.TicketContent(
                            new PrintRenderer.OrderHead(head.location(), "Order #1042", time),
                            "Waiter: anna@example.com",
                            List.of(
                                    new PrintRenderer.TicketItem("1x Singha beer", null),
                                    new PrintRenderer.TicketItem("2x Mojito", "less sugar")),
                            SAMPLE_NOTE));
            case Z_REPORT -> PrintRenderer.report(
                    style,
                    ZReportContent.build(
                            "7F3A92C1",
                            TimestampFormat.readableHotel(TimestampFormat.nowUtc(clock), clock),
                            "12450.00", "8300.00", "0.00", "1500.00", "20750.00", "31",
                            new ZReportContent.Folio("2000.00", "0.00", "0.00"),
                            "1000.00", "15450.00", "15450.00", "0.00",
                            SAMPLE_NOTE));
        };
    }

    private record Normalised(
            String paperWidth, String bodyFont, String title, int titleSize, String headerText, boolean headerBold,
            String footerText, boolean showLogo, int logoWidthPercent, boolean showTime, int itemSize,
            boolean showLocation, int locationSize, boolean showOrderNumber, boolean showWaiter,
            boolean showItemNotes, boolean showVatLine, boolean showPaymentMethod, int totalSize, boolean totalBold) {

        static Normalised of(PrintLayoutEntity e) {
            return new Normalised(
                    e.getPaperWidth(), e.getBodyFont(), e.getTitle(), e.getTitleSize(), e.getHeaderText(),
                    e.isHeaderBold(), e.getFooterText(), e.isShowLogo(), e.getLogoWidthPercent(), e.isShowTime(),
                    e.getItemSize(), e.isShowLocation(), e.getLocationSize(), e.isShowOrderNumber(), e.isShowWaiter(),
                    e.isShowItemNotes(), e.isShowVatLine(), e.isShowPaymentMethod(), e.getTotalSize(), e.isTotalBold());
        }

        PrintRenderer.Style toStyle() {
            return new PrintRenderer.Style(
                    "MM80".equals(paperWidth), "B".equals(bodyFont), title, titleSize, headerText, headerBold,
                    footerText, showLogo, logoWidthPercent, showTime, itemSize, showLocation, locationSize,
                    showOrderNumber, showWaiter, showItemNotes, showVatLine, showPaymentMethod, totalSize, totalBold);
        }
    }

    /** Names only, never values - a header can be a phone number or a tax id. */
    private static List<String> changedFields(Normalised before, Normalised after) {
        return Arrays.stream(Normalised.class.getRecordComponents())
                .filter(c -> !Objects.equals(read(before, c), read(after, c)))
                .map(RecordComponent::getName)
                .toList();
    }

    private static Object read(Normalised n, RecordComponent c) {
        try {
            return c.getAccessor().invoke(n);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Normalised validate(PrintLayoutInput in) {
        String title = title(in.getTitle());
        String header = freeText("headerText", in.getHeaderText());
        String footer = freeText("footerText", in.getFooterText());
        int percent = in.getLogoWidthPercent();
        if (percent < 20 || percent > 100) {
            throw ValidationException.field("logoWidthPercent", "Between 20 and 100");
        }
        return new Normalised(
                in.getPaperWidth().getValue(),
                in.getBodyFont().getValue(),
                title,
                size("titleSize", in.getTitleSize()),
                header,
                in.getHeaderBold(),
                footer,
                in.getShowLogo(),
                percent,
                in.getShowTime(),
                size("itemSize", in.getItemSize()),
                in.getShowLocation(),
                size("locationSize", in.getLocationSize()),
                in.getShowOrderNumber(),
                in.getShowWaiter(),
                in.getShowItemNotes(),
                in.getShowVatLine(),
                in.getShowPaymentMethod(),
                size("totalSize", in.getTotalSize()),
                in.getTotalBold());
    }

    private static int size(String field, int value) {
        if (value != 1 && value != 2) {
            throw ValidationException.field(field, "1 or 2");
        }
        return value;
    }

    private static String title(String raw) {
        String t = raw == null ? "" : raw.strip();
        if (t.isEmpty() || t.length() > MAX_TITLE) {
            throw ValidationException.field("title", "1 to " + MAX_TITLE + " characters");
        }
        if (hasControlCharacters(t)) {
            throw ValidationException.field("title", "No line breaks or control characters");
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

    private static PrintLayout toDto(PrintLayoutEntity e) {
        return new PrintLayout(
                PrintPaperWidth.fromValue(e.getPaperWidth()),
                PrintBodyFont.fromValue(e.getBodyFont()),
                e.getTitle(),
                e.getTitleSize(),
                e.getHeaderText(),
                e.isHeaderBold(),
                e.getFooterText(),
                e.isShowLogo(),
                e.getLogoWidthPercent(),
                e.isShowTime(),
                e.getItemSize(),
                e.isShowLocation(),
                e.getLocationSize(),
                e.isShowOrderNumber(),
                e.isShowWaiter(),
                e.isShowItemNotes(),
                e.isShowVatLine(),
                e.isShowPaymentMethod(),
                e.getTotalSize(),
                e.isTotalBold(),
                PrintLayoutDocument.fromValue(e.getDocumentType()),
                TimestampFormat.toUtc(e.getUpdatedAt()));
    }
}
