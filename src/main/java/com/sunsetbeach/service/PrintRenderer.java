package com.sunsetbeach.service;

import com.sunsetbeach.entity.PrintLayoutEntity;
import com.sunsetbeach.model.PrinterCodepage;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays out every configurable printed document - guest receipt, pre-bill, kitchen/bar ticket (and
 * its void ticket) and the Z-report. One class decides every line - what is printed, in which font
 * and size, wrapped where - and both the ESC/POS bytes ({@link #toEscPos}) and the settings page's
 * preview are made from that same list, so the preview wraps and truncates exactly where the paper
 * will. Pure: no database, no clock, so it is tested on its own.
 *
 * <p>Column counts are the printer's real ones for the roll, not the full dot width: 80mm is 42
 * (Font A) / 56 (Font B), 58mm is 32 / 42 - the 42 matches what every document printed before this
 * was configurable, so the seeded defaults change nothing on paper. A 2x line holds half as many.
 *
 * <p>Each document reads the part of {@link Style} that applies to it (a ticket has no VAT line, a
 * Z-report has no items); the rest is stored but ignored - see {@code PrintLayoutInput} in
 * openapi.yaml for the list.
 */
public final class PrintRenderer {

    private PrintRenderer() {}

    public enum ReceiptKind { RECEIPT, RECEIPT_COPY, PREBILL }

    /** The look. Built from the stored row or from an unsaved editor state (preview). */
    public record Style(
            boolean wide, // 80mm
            boolean fontB,
            String title,
            int titleSize,
            String headerText,
            boolean headerBold,
            String footerText,
            boolean showLogo,
            int logoWidthPercent,
            boolean showTime,
            int itemSize,
            boolean showLocation,
            int locationSize,
            boolean showOrderNumber,
            boolean showWaiter,
            boolean showItemNotes,
            boolean showVatLine,
            boolean showPaymentMethod,
            int totalSize,
            boolean totalBold) {

        public static Style from(PrintLayoutEntity e) {
            return new Style(
                    "MM80".equals(e.getPaperWidth()),
                    "B".equals(e.getBodyFont()),
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
                    e.isTotalBold());
        }

        public int columns() {
            return wide ? (fontB ? 56 : 42) : (fontB ? 42 : 32);
        }

        public int paperDots() {
            return wide ? 576 : 384;
        }
    }

    public record Row(String label, String amount) {}

    /** The location / order / time lines every order document opens with. Money and ids are already formatted. */
    public record OrderHead(String location, String orderLine, String timeLine) {}

    public record ReceiptContent(
            OrderHead head,
            List<Row> items,
            String total,
            String vatLabel,
            String vatAmount,
            /** Null on a pre-bill; otherwise the payment block's lines. */
            List<String> paymentLines,
            /** Samples only: a last line saying this is not a real document. */
            String sampleNote) {}

    public record TicketItem(String label, String note) {}

    public record TicketContent(OrderHead head, String waiterLine, List<TicketItem> items, String sampleNote) {}

    public record VoidContent(OrderHead head, String itemLabel, String note, String reason) {}

    /** A Z-report row: a label and a value, or a divider when {@code label} is null. */
    public record ReportRow(String label, String value) {
        public static ReportRow divider() {
            return new ReportRow(null, null);
        }
    }

    public record ReportContent(String identityLine, String closedLine, List<ReportRow> rows, String sampleNote) {}

    public record Line(String text, boolean center, boolean bold, int size, boolean fontB) {}

    public record Rendered(int columns, List<Line> lines, ReceiptLogo.Bitmap logo) {}

    // ---- documents -------------------------------------------------------------------------

    public static Rendered receipt(Style style, ReceiptKind kind, ReceiptContent c, byte[] logoPng) {
        Doc d = new Doc(style);
        d.logo(logoPng);
        d.header();
        d.title(kind == ReceiptKind.RECEIPT_COPY ? style.title() + " - COPY" : style.title());
        d.orderHead(c.head());
        d.divider();
        for (Row item : c.items()) {
            d.twoColumn(item.label(), item.amount(), style.itemSize(), false);
        }
        d.divider();
        d.twoColumn("TOTAL", c.total(), style.totalSize(), style.totalBold());
        if (style.showVatLine()) {
            d.twoColumn(c.vatLabel(), c.vatAmount(), 1, false);
        }
        if (c.paymentLines() != null && style.showPaymentMethod()) {
            d.divider();
            for (String p : c.paymentLines()) {
                d.plain(p, 1);
            }
        }
        d.footer();
        d.sample(c.sampleNote());
        return d.done();
    }

    public static Rendered ticket(Style style, TicketContent c) {
        Doc d = new Doc(style);
        d.header();
        d.title(style.title());
        d.orderHead(c.head());
        if (style.showWaiter()) {
            d.plain(c.waiterLine(), 1);
        }
        d.divider();
        for (TicketItem item : c.items()) {
            d.plain(item.label(), style.itemSize());
            if (style.showItemNotes() && item.note() != null && !item.note().isBlank()) {
                d.plain("   note: " + item.note(), style.itemSize());
            }
        }
        d.footer();
        d.sample(c.sampleNote());
        return d.done();
    }

    /** "*** VOID ***" is fixed - what makes it unmistakable on the pass - everything else follows the station's layout. */
    public static Rendered voided(Style style, VoidContent c) {
        Doc d = new Doc(style);
        d.title("*** VOID ***");
        d.orderHead(c.head());
        d.divider();
        d.plain(c.itemLabel(), style.itemSize());
        if (c.note() != null && !c.note().isBlank()) {
            d.plain("   note: " + c.note(), style.itemSize());
        }
        d.plain("Reason: " + c.reason(), style.itemSize());
        return d.done();
    }

    public static Rendered report(Style style, ReportContent c) {
        Doc d = new Doc(style);
        d.header();
        d.title(style.title());
        d.plain(c.identityLine(), 1);
        if (style.showTime()) {
            d.plain(c.closedLine(), 1);
        }
        d.divider();
        for (ReportRow r : c.rows()) {
            if (r.label() == null) {
                d.divider();
            } else {
                d.twoColumn(r.label(), r.value(), 1, false);
            }
        }
        d.footer();
        d.sample(c.sampleNote());
        return d.done();
    }

    // ---- shared layout ---------------------------------------------------------------------

    private static final class Doc {
        private final Style style;
        private final int columns;
        private final boolean fontB;
        private final List<Line> lines = new ArrayList<>();
        private ReceiptLogo.Bitmap logo;

        Doc(Style style) {
            this.style = style;
            this.columns = style.columns();
            this.fontB = style.fontB();
        }

        /** Only the guest-facing documents print the logo; the layout's showLogo is ignored elsewhere. */
        void logo(byte[] logoPng) {
            if (style.showLogo() && logoPng != null) {
                logo = ReceiptLogo.toBitmap(logoPng, style.paperDots(), style.logoWidthPercent());
            }
        }

        void header() {
            for (String h : splitLines(style.headerText())) {
                for (String part : wrap(h, columns)) {
                    lines.add(new Line(part, true, style.headerBold(), 1, fontB));
                }
            }
        }

        void title(String title) {
            for (String part : wrap(title, columns / style.titleSize())) {
                lines.add(new Line(part, true, true, style.titleSize(), fontB));
            }
        }

        void orderHead(OrderHead h) {
            if (style.showLocation()) {
                plain(h.location(), style.locationSize());
            }
            if (style.showOrderNumber()) {
                plain(h.orderLine(), 1);
            }
            if (style.showTime()) {
                plain(h.timeLine(), 1);
            }
        }

        void plain(String text, int size) {
            for (String part : wrap(text, columns / size)) {
                lines.add(new Line(part, false, false, size, fontB));
            }
        }

        void divider() {
            lines.add(new Line("-".repeat(columns), false, false, 1, fontB));
        }

        /** Label left, value right, label cut to fit - the same rule as {@link EscPosBuilder#twoColumn}. */
        void twoColumn(String left, String right, int size, boolean bold) {
            lines.add(new Line(PrintRenderer.twoColumn(left, right, columns / size), false, bold, size, fontB));
        }

        void footer() {
            List<String> footer = splitLines(style.footerText());
            if (footer.isEmpty()) {
                return;
            }
            lines.add(new Line("", false, false, 1, fontB));
            for (String f : footer) {
                for (String part : wrap(f, columns)) {
                    lines.add(new Line(part, true, false, 1, fontB));
                }
            }
        }

        void sample(String note) {
            if (note == null) {
                return;
            }
            lines.add(new Line("", false, false, 1, fontB));
            for (String part : wrap(note, columns)) {
                lines.add(new Line(part, true, false, 1, fontB));
            }
        }

        Rendered done() {
            List<Line> cleaned = new ArrayList<>(lines.size());
            for (Line l : lines) {
                cleaned.add(new Line(EscPosBuilder.asciiPunctuation(l.text()), l.center(), l.bold(), l.size(), l.fontB()));
            }
            return new Rendered(columns, cleaned, logo);
        }
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
            String t = line.strip();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * Word wrap at {@code width}; a word longer than the line is cut. Always returns at least one
     * line. A leading indent (the "   note:" lines) is kept on every line it wraps to.
     */
    static List<String> wrap(String text, int width) {
        int indent = Math.min(text.length() - text.stripLeading().length(), Math.max(0, width - 1));
        if (indent > 0) {
            String pad = " ".repeat(indent);
            return wrap(text.stripLeading(), width - indent).stream().map(l -> pad + l).toList();
        }
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
