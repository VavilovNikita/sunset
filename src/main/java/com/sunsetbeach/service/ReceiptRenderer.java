package com.sunsetbeach.service;

import com.sunsetbeach.entity.ReceiptSettingsEntity;
import com.sunsetbeach.model.PrinterCodepage;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays out the guest receipt and the pre-bill. One method decides every line - what is printed,
 * in which font and size, wrapped where - and both the ESC/POS bytes ({@link #toEscPos}) and the
 * settings page's preview are made from that same list, so the preview wraps and truncates exactly
 * where the paper will. Pure: no database, no clock, so it is tested on its own.
 *
 * <p>Column counts are the printer's real ones for the roll, not the full dot width: 80mm is 42
 * (Font A) / 56 (Font B), 58mm is 32 / 42 - the 42 matches what every receipt printed before this
 * was configurable, so the seeded defaults change nothing on paper. A 2x line holds half as many.
 */
public final class ReceiptRenderer {

    private ReceiptRenderer() {}

    public enum Kind { PREBILL, RECEIPT, RECEIPT_COPY }

    /** The look. Built from the stored row or from an unsaved editor state (preview). */
    public record Style(
            boolean wide, // 80mm
            boolean fontB,
            String receiptTitle,
            String prebillTitle,
            int titleSize,
            int totalSize,
            boolean totalBold,
            String headerText,
            boolean headerBold,
            String footerText,
            boolean showOrderNumber,
            boolean showTime,
            boolean showVatLine,
            boolean showPaymentMethod,
            boolean showLogo,
            int logoWidthPercent) {

        public static Style from(ReceiptSettingsEntity e) {
            return new Style(
                    "MM80".equals(e.getPaperWidth()),
                    "B".equals(e.getBodyFont()),
                    e.getReceiptTitle(),
                    e.getPrebillTitle(),
                    e.getTitleSize(),
                    e.getTotalSize(),
                    e.isTotalBold(),
                    e.getHeaderText(),
                    e.isHeaderBold(),
                    e.getFooterText(),
                    e.isShowOrderNumber(),
                    e.isShowTime(),
                    e.isShowVatLine(),
                    e.isShowPaymentMethod(),
                    e.isShowLogo(),
                    e.getLogoWidthPercent());
        }

        public int columns() {
            return wide ? (fontB ? 56 : 42) : (fontB ? 42 : 32);
        }

        public int paperDots() {
            return wide ? 576 : 384;
        }
    }

    public record Row(String label, String amount) {}

    /** What the document says, independent of how it looks. Money strings come from the server already formatted. */
    public record Content(
            String location,
            String orderLine,
            String timeLine,
            List<Row> items,
            String total,
            String vatLabel,
            String vatAmount,
            /** Null on a pre-bill; otherwise the payment block's lines. */
            List<String> paymentLines) {}

    public record Line(String text, boolean center, boolean bold, int size, boolean fontB) {}

    public record Rendered(int columns, List<Line> lines, ReceiptLogo.Bitmap logo) {}

    public static Rendered render(Style style, Kind kind, Content content, byte[] logoPng) {
        int columns = style.columns();
        boolean fontB = style.fontB();
        List<Line> lines = new ArrayList<>();

        ReceiptLogo.Bitmap logo = style.showLogo() && logoPng != null
                ? ReceiptLogo.toBitmap(logoPng, style.paperDots(), style.logoWidthPercent())
                : null;

        for (String h : splitLines(style.headerText())) {
            for (String part : wrap(h, columns)) {
                lines.add(new Line(part, true, style.headerBold(), 1, fontB));
            }
        }

        String title = kind == Kind.PREBILL ? style.prebillTitle() : style.receiptTitle();
        if (kind == Kind.RECEIPT_COPY) {
            title = title + " - COPY";
        }
        for (String part : wrap(title, columns / style.titleSize())) {
            lines.add(new Line(part, true, true, style.titleSize(), fontB));
        }

        for (String part : wrap(content.location(), columns)) {
            lines.add(plain(part, fontB));
        }
        if (style.showOrderNumber()) {
            for (String part : wrap(content.orderLine(), columns)) {
                lines.add(plain(part, fontB));
            }
        }
        if (style.showTime()) {
            for (String part : wrap(content.timeLine(), columns)) {
                lines.add(plain(part, fontB));
            }
        }
        lines.add(divider(columns, fontB));
        for (Row item : content.items()) {
            lines.add(plain(twoColumn(item.label(), item.amount(), columns), fontB));
        }
        lines.add(divider(columns, fontB));
        lines.add(new Line(twoColumn("TOTAL", content.total(), columns / style.totalSize()), false, style.totalBold(), style.totalSize(), fontB));
        if (style.showVatLine()) {
            lines.add(plain(twoColumn(content.vatLabel(), content.vatAmount(), columns), fontB));
        }
        if (content.paymentLines() != null && style.showPaymentMethod()) {
            lines.add(divider(columns, fontB));
            for (String p : content.paymentLines()) {
                for (String part : wrap(p, columns)) {
                    lines.add(plain(part, fontB));
                }
            }
        }
        List<String> footer = splitLines(style.footerText());
        if (!footer.isEmpty()) {
            lines.add(plain("", fontB));
            for (String f : footer) {
                for (String part : wrap(f, columns)) {
                    lines.add(new Line(part, true, false, 1, fontB));
                }
            }
        }

        List<Line> cleaned = new ArrayList<>(lines.size());
        for (Line l : lines) {
            cleaned.add(new Line(EscPosBuilder.asciiPunctuation(l.text()), l.center(), l.bold(), l.size(), l.fontB()));
        }
        return new Rendered(columns, cleaned, logo);
    }

    public static byte[] toEscPos(Rendered r, PrinterCodepage codepage) {
        EscPosBuilder b = new EscPosBuilder(codepage);
        boolean center = false;
        boolean bold = false;
        int size = 1;
        boolean fontB = false;
        if (r.logo() != null) {
            b.center(true);
            center = true;
            b.raster(r.logo().widthBytes(), r.logo().heightDots(), r.logo().packed());
        }
        for (Line l : r.lines()) {
            // Turn things off before on, so a line never prints in the previous line's leftovers.
            if (bold && !l.bold()) { b.bold(false); bold = false; }
            if (center != l.center()) { b.center(l.center()); center = l.center(); }
            if (size != l.size()) { b.size(l.size()); size = l.size(); }
            if (fontB != l.fontB()) { b.fontB(l.fontB()); fontB = l.fontB(); }
            if (!bold && l.bold()) { b.bold(true); bold = true; }
            b.line(l.text());
        }
        if (bold) b.bold(false);
        if (size != 1) b.size(1);
        if (fontB) b.fontB(false);
        if (center) b.center(false);
        return b.cutAndBuild();
    }

    private static Line plain(String text, boolean fontB) {
        return new Line(text, false, false, 1, fontB);
    }

    private static Line divider(int columns, boolean fontB) {
        return plain("-".repeat(columns), fontB);
    }

    /** Label left, value right, label cut to fit - the same rule as {@link EscPosBuilder#twoColumn}. */
    static String twoColumn(String left, String right, int width) {
        int leftWidth = Math.max(1, width - right.length());
        String l = left.length() > leftWidth ? left.substring(0, leftWidth) : left;
        return l + " ".repeat(Math.max(0, width - l.length() - right.length())) + right;
    }

    private static List<String> splitLines(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String line : text.split("\\R", -1)) {
            String t = line.stripTrailing();
            if (!t.isBlank()) {
                out.add(t.strip());
            }
        }
        return out;
    }

    /** Word wrap at {@code width}; a word longer than the line is cut. Always returns at least one line. */
    static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        int w = Math.max(1, width);
        StringBuilder current = new StringBuilder();
        for (String word : text.strip().split("\\s+")) {
            while (word.length() > w) {
                if (current.length() > 0) {
                    out.add(current.toString());
                    current.setLength(0);
                }
                out.add(word.substring(0, w));
                word = word.substring(w);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= w) {
                current.append(' ').append(word);
            } else {
                out.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0 || out.isEmpty()) {
            out.add(current.toString());
        }
        return out;
    }
}
