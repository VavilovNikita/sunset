package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.model.PrinterCodepage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Pure layout tests - the page that decides what lands on paper has no database and no clock. */
class ReceiptRendererTests {

    private static ReceiptRenderer.Style defaults() {
        // Exactly what V136 seeds: the receipt as it printed before it was configurable.
        return new ReceiptRenderer.Style(true, false, "GUEST RECEIPT", "PRE-BILL", 1, 1, false, "", true, "", true, true, true, true, false, 60);
    }

    private static ReceiptRenderer.Content content(List<String> payment) {
        return new ReceiptRenderer.Content(
                "Restaurant - T5",
                "Order #1042 (ref 7F3A92C1)",
                "Time: 11 Oct 2026, 14:05",
                List.of(new ReceiptRenderer.Row("2x Pad Thai", "320.00"), new ReceiptRenderer.Row("1x Singha beer", "90.00")),
                "410.00",
                "incl. VAT 7%",
                "26.82",
                payment);
    }

    private static List<String> texts(ReceiptRenderer.Rendered r) {
        return r.lines().stream().map(ReceiptRenderer.Line::text).toList();
    }

    /** The regression that matters most: with the seeded settings the bytes are what the old hard-coded builder produced. */
    @Test
    void seededDefaults_produceByteForByteTheOldReceipt() {
        EscPosBuilder old = new EscPosBuilder(PrinterCodepage.PC437);
        old.center(true).bold(true).line("GUEST RECEIPT").bold(false).center(false);
        old.line("Restaurant - T5");
        old.line("Order #1042 (ref 7F3A92C1)");
        old.line("Time: 11 Oct 2026, 14:05");
        old.divider();
        old.twoColumn("2x Pad Thai", "320.00");
        old.twoColumn("1x Singha beer", "90.00");
        old.divider();
        old.twoColumn("TOTAL", "410.00");
        old.twoColumn("incl. VAT 7%", "26.82");
        old.divider();
        old.line("Payment: Cash");

        byte[] now = ReceiptRenderer.toEscPos(
                ReceiptRenderer.render(defaults(), ReceiptRenderer.Kind.RECEIPT, content(List.of("Payment: Cash")), null),
                PrinterCodepage.PC437);

        assertThat(now).isEqualTo(old.cutAndBuild());
    }

    @Test
    void preBill_usesItsOwnTitle_andHasNoPaymentBlock() {
        ReceiptRenderer.Rendered r = ReceiptRenderer.render(defaults(), ReceiptRenderer.Kind.PREBILL, content(null), null);

        assertThat(texts(r)).startsWith("PRE-BILL").noneMatch(t -> t.startsWith("Payment"));
    }

    @Test
    void reprint_appendsCopyToTheConfiguredTitle() {
        ReceiptRenderer.Style s = new ReceiptRenderer.Style(true, false, "THANK YOU", "PRE-BILL", 1, 1, false, "", true, "", true, true, true, true, false, 60);

        assertThat(texts(ReceiptRenderer.render(s, ReceiptRenderer.Kind.RECEIPT_COPY, content(List.of("Payment: Cash")), null)).get(0))
                .isEqualTo("THANK YOU - COPY");
    }

    @Test
    void columnsFollowPaperAndFont() {
        assertThat(new ReceiptRenderer.Style(true, false, "T", "T", 1, 1, false, "", true, "", true, true, true, true, false, 60).columns()).isEqualTo(42);
        assertThat(new ReceiptRenderer.Style(true, true, "T", "T", 1, 1, false, "", true, "", true, true, true, true, false, 60).columns()).isEqualTo(56);
        assertThat(new ReceiptRenderer.Style(false, false, "T", "T", 1, 1, false, "", true, "", true, true, true, true, false, 60).columns()).isEqualTo(32);
        assertThat(new ReceiptRenderer.Style(false, true, "T", "T", 1, 1, false, "", true, "", true, true, true, true, false, 60).columns()).isEqualTo(42);
    }

    @Test
    void narrowPaper_keepsEveryLineWithinTheColumns_andKeepsTheAmount() {
        ReceiptRenderer.Style s = new ReceiptRenderer.Style(false, false, "GUEST RECEIPT", "PRE-BILL", 1, 1, false, "", true, "", true, true, true, true, false, 60);
        ReceiptRenderer.Content c = new ReceiptRenderer.Content(
                "Restaurant - T5", "Order #1042 (ref 7F3A92C1)", "Time: 11 Oct 2026, 14:05",
                List.of(new ReceiptRenderer.Row("3x Grilled seafood platter for two with lemon", "1250.00")),
                "1250.00", "incl. VAT 7%", "81.78", List.of("Payment: Card"));

        ReceiptRenderer.Rendered r = ReceiptRenderer.render(s, ReceiptRenderer.Kind.RECEIPT, c, null);

        assertThat(r.columns()).isEqualTo(32);
        assertThat(texts(r)).allSatisfy(t -> assertThat(t.length()).isLessThanOrEqualTo(32));
        assertThat(texts(r)).anyMatch(t -> t.startsWith("3x Grilled") && t.endsWith("1250.00"));
    }

    @Test
    void doubleSizeTotal_isLaidOutForHalfTheColumns() {
        ReceiptRenderer.Style s = new ReceiptRenderer.Style(true, false, "GUEST RECEIPT", "PRE-BILL", 1, 2, true, "", true, "", true, true, true, true, false, 60);

        ReceiptRenderer.Rendered r = ReceiptRenderer.render(s, ReceiptRenderer.Kind.RECEIPT, content(null), null);

        ReceiptRenderer.Line total = r.lines().stream().filter(l -> l.text().startsWith("TOTAL")).findFirst().orElseThrow();
        assertThat(total.size()).isEqualTo(2);
        assertThat(total.bold()).isTrue();
        assertThat(total.text()).hasSize(21).endsWith("410.00");
    }

    @Test
    void longTitleAtDoubleSize_wrapsInsteadOfOverflowing() {
        ReceiptRenderer.Style s = new ReceiptRenderer.Style(false, false, "SUNSET BEACH RESORT RECEIPT", "PRE-BILL", 2, 1, false, "", true, "", true, true, true, true, false, 60);

        List<ReceiptRenderer.Line> titleLines = ReceiptRenderer.render(s, ReceiptRenderer.Kind.RECEIPT, content(null), null)
                .lines().stream().filter(l -> l.size() == 2).toList();

        assertThat(titleLines.size()).isGreaterThan(1);
        assertThat(titleLines).allSatisfy(l -> assertThat(l.text().length()).isLessThanOrEqualTo(16));
    }

    @Test
    void headerAndFooter_areCentred_headerBoldIsConfigurable_blankLinesDropped() {
        ReceiptRenderer.Style s = new ReceiptRenderer.Style(true, false, "GUEST RECEIPT", "PRE-BILL", 1, 1, false,
                "Sunset Beach Resort\n\n12 Beach Rd", false, "Thank you!\nSee you soon", true, true, true, true, false, 60);

        ReceiptRenderer.Rendered r = ReceiptRenderer.render(s, ReceiptRenderer.Kind.RECEIPT, content(List.of("Payment: Cash")), null);

        assertThat(texts(r)).containsSequence("Sunset Beach Resort", "12 Beach Rd", "GUEST RECEIPT");
        assertThat(r.lines().get(0).center()).isTrue();
        assertThat(r.lines().get(0).bold()).isFalse();
        assertThat(texts(r).subList(texts(r).size() - 3, texts(r).size())).containsExactly("", "Thank you!", "See you soon");
    }

    @Test
    void toggles_removeTheirLines() {
        ReceiptRenderer.Style s = new ReceiptRenderer.Style(true, false, "GUEST RECEIPT", "PRE-BILL", 1, 1, false, "", true, "", false, false, false, false, false, 60);

        List<String> t = texts(ReceiptRenderer.render(s, ReceiptRenderer.Kind.RECEIPT, content(List.of("Payment: Cash")), null));

        assertThat(t).noneMatch(x -> x.startsWith("Order #") || x.startsWith("Time:") || x.contains("VAT") || x.startsWith("Payment"));
        assertThat(t).anyMatch(x -> x.startsWith("TOTAL"));
    }

    @Test
    void typographicPunctuation_isReplacedBeforeLayout_soPreviewMatchesPaper() {
        ReceiptRenderer.Content c = new ReceiptRenderer.Content(
                "Restaurant – T5", "o", "t", List.of(), "0.00", "v", "0.00", null);

        assertThat(texts(ReceiptRenderer.render(defaults(), ReceiptRenderer.Kind.PREBILL, c, null))).contains("Restaurant - T5");
    }

    @Test
    void fontB_andDoubleSize_emitTheirControlCodes_andResetBeforeTheCut() {
        ReceiptRenderer.Style s = new ReceiptRenderer.Style(true, true, "GUEST RECEIPT", "PRE-BILL", 2, 1, false, "", true, "", true, true, true, true, false, 60);

        byte[] bytes = ReceiptRenderer.toEscPos(ReceiptRenderer.render(s, ReceiptRenderer.Kind.RECEIPT, content(null), null), PrinterCodepage.PC437);

        assertThat(indexOf(bytes, new byte[] {0x1B, 0x4D, 0x01})).isPositive();
        assertThat(indexOf(bytes, new byte[] {0x1D, 0x21, 0x11})).isPositive();
        assertThat(lastIndexOf(bytes, new byte[] {0x1D, 0x21, 0x00})).isGreaterThan(indexOf(bytes, new byte[] {0x1D, 0x21, 0x11}));
        assertThat(lastIndexOf(bytes, new byte[] {0x1B, 0x4D, 0x00})).isGreaterThan(indexOf(bytes, new byte[] {0x1B, 0x4D, 0x01}));
    }

    @Test
    void logo_isPrintedAsRaster_onlyWhenShown_andOnlyWhenOneExists() throws Exception {
        byte[] logo = png(200, 100, 0x000000);
        ReceiptRenderer.Style shown = new ReceiptRenderer.Style(true, false, "GUEST RECEIPT", "PRE-BILL", 1, 1, false, "", true, "", true, true, true, true, true, 50);
        ReceiptRenderer.Style hidden = new ReceiptRenderer.Style(true, false, "GUEST RECEIPT", "PRE-BILL", 1, 1, false, "", true, "", true, true, true, true, false, 50);

        ReceiptRenderer.Rendered with = ReceiptRenderer.render(shown, ReceiptRenderer.Kind.RECEIPT, content(null), ReceiptLogo.normalize(logo));
        assertThat(with.logo().widthDots()).isEqualTo(288); // 50% of 576
        assertThat(indexOf(ReceiptRenderer.toEscPos(with, PrinterCodepage.PC437), new byte[] {0x1D, 0x76, 0x30, 0x00})).isPositive();

        assertThat(ReceiptRenderer.render(hidden, ReceiptRenderer.Kind.RECEIPT, content(null), ReceiptLogo.normalize(logo)).logo()).isNull();
        assertThat(ReceiptRenderer.render(shown, ReceiptRenderer.Kind.RECEIPT, content(null), null).logo()).isNull();
    }

    static byte[] png(int w, int h, int rgb) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setRGB(x, y, rgb);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static int indexOf(byte[] hay, byte[] needle) {
        outer:
        for (int i = 0; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) if (hay[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }

    private static int lastIndexOf(byte[] hay, byte[] needle) {
        outer:
        for (int i = hay.length - needle.length; i >= 0; i--) {
            for (int j = 0; j < needle.length; j++) if (hay[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }
}
