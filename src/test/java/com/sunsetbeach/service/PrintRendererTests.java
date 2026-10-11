package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.service.PrintRenderer.OrderHead;
import com.sunsetbeach.service.PrintRenderer.Style;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Pure layout tests - the class that decides what lands on paper has no database and no clock. */
class PrintRendererTests {

    /** Mutable stand-in for the (immutable) Style record, starting from what V137 seeds. */
    static final class S {
        boolean wide = true, fontB = false, headerBold = true, showLogo = false, showTime = true, showLocation = true,
                showOrderNumber = true, showWaiter = true, showItemNotes = true, showVatLine = true,
                showPaymentMethod = true, totalBold = false;
        String title, headerText = "", footerText = "";
        int titleSize = 1, logoWidthPercent = 60, itemSize = 1, locationSize = 1, totalSize = 1;

        S(String title) {
            this.title = title;
        }

        Style build() {
            return new Style(wide, fontB, title, titleSize, headerText, headerBold, footerText, showLogo, logoWidthPercent,
                    showTime, itemSize, showLocation, locationSize, showOrderNumber, showWaiter, showItemNotes, showVatLine,
                    showPaymentMethod, totalSize, totalBold);
        }
    }

    private static S receiptStyle() {
        return new S("GUEST RECEIPT");
    }

    private static final OrderHead HEAD = new OrderHead("Restaurant - T5", "Order #1042 (ref 7F3A92C1)", "Time: 11 Oct 2026, 14:05");

    private static PrintRenderer.ReceiptContent receipt(List<String> payment) {
        return new PrintRenderer.ReceiptContent(
                HEAD,
                List.of(new PrintRenderer.Row("2x Pad Thai", "320.00"), new PrintRenderer.Row("1x Singha beer", "90.00")),
                "410.00", "incl. VAT 7%", "26.82", payment, null);
    }

    private static List<String> texts(PrintRenderer.Rendered r) {
        return r.lines().stream().map(PrintRenderer.Line::text).toList();
    }

    private static byte[] bytes(PrintRenderer.Rendered r) {
        return PrintRenderer.toEscPos(r, PrinterCodepage.PC437);
    }

    // ---- the seeded defaults print what each document printed before layouts existed ------------

    @Test
    void receiptDefaults_produceByteForByteTheOldReceipt() {
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

        assertThat(bytes(PrintRenderer.receipt(receiptStyle().build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(List.of("Payment: Cash")), null)))
                .isEqualTo(old.cutAndBuild());
    }

    @Test
    void ticketDefaults_produceByteForByteTheOldKitchenTicket() {
        EscPosBuilder old = new EscPosBuilder(PrinterCodepage.PC437);
        old.center(true).bold(true).line("KITCHEN TICKET").bold(false).center(false);
        old.line("Restaurant - T5");
        old.line("Order #1042");
        old.line("Time: 11 Oct 2026, 14:05");
        old.line("Waiter: anna@example.com");
        old.divider();
        old.line("2x Pad Thai");
        old.line("   note: no peanuts");
        old.line("1x Green curry");

        PrintRenderer.TicketContent c = new PrintRenderer.TicketContent(
                new OrderHead("Restaurant - T5", "Order #1042", "Time: 11 Oct 2026, 14:05"),
                "Waiter: anna@example.com",
                List.of(new PrintRenderer.TicketItem("2x Pad Thai", "no peanuts"), new PrintRenderer.TicketItem("1x Green curry", null)),
                null);

        assertThat(bytes(PrintRenderer.ticket(new S("KITCHEN TICKET").build(), c))).isEqualTo(old.cutAndBuild());
    }

    @Test
    void voidDefaults_produceByteForByteTheOldVoidTicket() {
        EscPosBuilder old = new EscPosBuilder(PrinterCodepage.PC437);
        old.center(true).bold(true).line("*** VOID ***").bold(false).center(false);
        old.line("Restaurant - T5");
        old.line("Order #1042");
        old.line("Time: 11 Oct 2026, 14:05");
        old.divider();
        old.line("1x Pad Thai");
        old.line("   note: no peanuts");
        old.line("Reason: guest changed mind");

        PrintRenderer.VoidContent c = new PrintRenderer.VoidContent(
                new OrderHead("Restaurant - T5", "Order #1042", "Time: 11 Oct 2026, 14:05"),
                "1x Pad Thai", "no peanuts", "guest changed mind");

        assertThat(bytes(PrintRenderer.voided(new S("KITCHEN TICKET").build(), c))).isEqualTo(old.cutAndBuild());
    }

    @Test
    void zReportDefaults_produceByteForByteTheOldReport() {
        EscPosBuilder old = new EscPosBuilder(PrinterCodepage.PC437);
        old.center(true).bold(true).line("Z-REPORT").bold(false).center(false);
        old.line("Shift #AB12CD34");
        old.line("Closed: 11 Oct 2026, 23:00");
        old.divider();
        old.twoColumn("Cash", "100.00");
        old.twoColumn("Card", "50.00");
        old.twoColumn("Other", "0.00");
        old.twoColumn("Room charge (folio, not received)", "20.00");
        old.divider();
        old.twoColumn("Received total", "150.00");
        old.twoColumn("Payments", "4");
        old.divider();
        old.twoColumn("Opening float", "10.00");
        old.twoColumn("Expected cash", "110.00");
        old.twoColumn("Counted cash", "—");
        old.twoColumn("Discrepancy", "—");

        PrintRenderer.ReportContent c = ZReportContent.build("AB12CD34", "11 Oct 2026, 23:00", "100.00", "50.00", "0.00",
                "20.00", "150.00", "4", null, "10.00", "110.00", "—", "—", null);

        assertThat(bytes(PrintRenderer.report(new S("Z-REPORT").build(), c))).isEqualTo(old.cutAndBuild());
    }

    // ---- receipt / pre-bill ---------------------------------------------------------------------

    @Test
    void preBill_hasItsOwnTitle_andNoPaymentBlock() {
        S s = new S("PRE-BILL");
        PrintRenderer.Rendered r = PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.PREBILL, receipt(null), null);

        assertThat(texts(r)).startsWith("PRE-BILL").noneMatch(t -> t.startsWith("Payment"));
    }

    @Test
    void reprint_appendsCopyToTheConfiguredTitle() {
        S s = receiptStyle();
        s.title = "THANK YOU";

        assertThat(texts(PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT_COPY, receipt(List.of("Payment: Cash")), null)).get(0))
                .isEqualTo("THANK YOU - COPY");
    }

    @Test
    void columnsFollowPaperAndFont() {
        S s = receiptStyle();
        assertThat(s.build().columns()).isEqualTo(42);
        s.fontB = true;
        assertThat(s.build().columns()).isEqualTo(56);
        s.wide = false;
        assertThat(s.build().columns()).isEqualTo(42);
        s.fontB = false;
        assertThat(s.build().columns()).isEqualTo(32);
    }

    @Test
    void narrowPaper_keepsEveryLineWithinTheColumns_andKeepsTheAmount() {
        S s = receiptStyle();
        s.wide = false;
        PrintRenderer.ReceiptContent c = new PrintRenderer.ReceiptContent(
                HEAD,
                List.of(new PrintRenderer.Row("3x Grilled seafood platter for two with lemon", "1250.00")),
                "1250.00", "incl. VAT 7%", "81.78", List.of("Payment: Card"), null);

        PrintRenderer.Rendered r = PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT, c, null);

        assertThat(r.columns()).isEqualTo(32);
        assertThat(texts(r)).allSatisfy(t -> assertThat(t.length()).isLessThanOrEqualTo(32));
        assertThat(texts(r)).anyMatch(t -> t.startsWith("3x Grilled") && t.endsWith("1250.00"));
    }

    @Test
    void doubleSizeTotal_isLaidOutForHalfTheColumns() {
        S s = receiptStyle();
        s.totalSize = 2;
        s.totalBold = true;

        PrintRenderer.Line total = PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(null), null)
                .lines().stream().filter(l -> l.text().startsWith("TOTAL")).findFirst().orElseThrow();

        assertThat(total.size()).isEqualTo(2);
        assertThat(total.bold()).isTrue();
        assertThat(total.text()).hasSize(21).endsWith("410.00");
    }

    @Test
    void doubleSizeItems_haveHalfTheColumnsForTheirName() {
        S s = receiptStyle();
        s.itemSize = 2;

        List<PrintRenderer.Line> items = PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(null), null)
                .lines().stream().filter(l -> l.text().contains("Pad Thai") || l.text().contains("Singha")).toList();

        assertThat(items).hasSize(2).allSatisfy(l -> {
            assertThat(l.size()).isEqualTo(2);
            assertThat(l.text()).hasSize(21);
        });
    }

    @Test
    void longTitleAtDoubleSize_wrapsInsteadOfOverflowing() {
        S s = receiptStyle();
        s.wide = false;
        s.title = "SUNSET BEACH RESORT RECEIPT";
        s.titleSize = 2;

        List<PrintRenderer.Line> titleLines = PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(null), null)
                .lines().stream().filter(l -> l.size() == 2).toList();

        assertThat(titleLines.size()).isGreaterThan(1);
        assertThat(titleLines).allSatisfy(l -> assertThat(l.text().length()).isLessThanOrEqualTo(16));
    }

    @Test
    void headerAndFooter_areCentred_headerBoldIsConfigurable_blankLinesDropped() {
        S s = receiptStyle();
        s.headerText = "Sunset Beach Resort\n\n12 Beach Rd";
        s.headerBold = false;
        s.footerText = "Thank you!\nSee you soon";

        PrintRenderer.Rendered r = PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(List.of("Payment: Cash")), null);

        assertThat(texts(r)).containsSequence("Sunset Beach Resort", "12 Beach Rd", "GUEST RECEIPT");
        assertThat(r.lines().get(0).center()).isTrue();
        assertThat(r.lines().get(0).bold()).isFalse();
        assertThat(texts(r).subList(texts(r).size() - 3, texts(r).size())).containsExactly("", "Thank you!", "See you soon");
    }

    @Test
    void receiptToggles_removeTheirLines() {
        S s = receiptStyle();
        s.showOrderNumber = false;
        s.showTime = false;
        s.showVatLine = false;
        s.showPaymentMethod = false;
        s.showLocation = false;

        List<String> t = texts(PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(List.of("Payment: Cash")), null));

        assertThat(t).noneMatch(x -> x.startsWith("Restaurant") || x.startsWith("Order #") || x.startsWith("Time:") || x.contains("VAT") || x.startsWith("Payment"));
        assertThat(t).anyMatch(x -> x.startsWith("TOTAL"));
    }

    @Test
    void typographicPunctuation_isReplacedBeforeLayout_soPreviewMatchesPaper() {
        PrintRenderer.ReceiptContent c = new PrintRenderer.ReceiptContent(
                new OrderHead("Restaurant – T5", "o", "t"), List.of(), "0.00", "v", "0.00", null, null);

        assertThat(texts(PrintRenderer.receipt(new S("PRE-BILL").build(), PrintRenderer.ReceiptKind.PREBILL, c, null))).contains("Restaurant - T5");
    }

    @Test
    void sampleNote_isAddedCentredAfterTheFooter_onlyForSamples() {
        S s = receiptStyle();
        s.footerText = "Thanks";
        PrintRenderer.ReceiptContent sample = new PrintRenderer.ReceiptContent(
                HEAD, List.of(), "0.00", "v", "0.00", null, "- sample -");

        List<String> t = texts(PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT, sample, null));

        assertThat(t.subList(t.size() - 4, t.size())).containsExactly("", "Thanks", "", "- sample -");
        assertThat(texts(PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(null), null))).doesNotContain("- sample -");
    }

    // ---- tickets, void, report -----------------------------------------------------------------

    private static PrintRenderer.TicketContent ticket() {
        return new PrintRenderer.TicketContent(
                new OrderHead("Restaurant - T5", "Order #1042", "Time: 11 Oct 2026, 14:05"),
                "Waiter: anna@example.com",
                List.of(new PrintRenderer.TicketItem("2x Pad Thai", "no peanuts"), new PrintRenderer.TicketItem("1x Green curry", null)),
                null);
    }

    @Test
    void ticketToggles_removeWaiterNotesLocationOrderAndTime() {
        S s = new S("KITCHEN TICKET");
        s.showWaiter = false;
        s.showItemNotes = false;
        s.showLocation = false;
        s.showOrderNumber = false;
        s.showTime = false;

        assertThat(texts(PrintRenderer.ticket(s.build(), ticket())))
                .containsExactly("KITCHEN TICKET", "-".repeat(42), "2x Pad Thai", "1x Green curry");
    }

    @Test
    void ticketItemsAndLocation_canBeDoubleSize_notesFollowTheItem() {
        S s = new S("KITCHEN TICKET");
        s.itemSize = 2;
        s.locationSize = 2;

        PrintRenderer.Rendered r = PrintRenderer.ticket(s.build(), ticket());

        assertThat(r.lines()).filteredOn(l -> l.text().equals("Restaurant - T5") || l.text().equals("2x Pad Thai") || l.text().trim().equals("note: no peanuts"))
                .hasSize(3).allSatisfy(l -> assertThat(l.size()).isEqualTo(2));
        // The order number and the time stay small: it is the table and the dishes the pass reads from a distance.
        assertThat(r.lines()).filteredOn(l -> l.text().startsWith("Order #")).allSatisfy(l -> assertThat(l.size()).isEqualTo(1));
    }

    @Test
    void longDishName_wrapsAtTheColumns_keepingTheNoteIndent() {
        S s = new S("KITCHEN TICKET");
        s.wide = false;
        PrintRenderer.TicketContent c = new PrintRenderer.TicketContent(
                HEAD, "w", List.of(new PrintRenderer.TicketItem("2x Crispy duck breast with tamarind sauce and jasmine rice", "extra sauce on the side please")), null);

        List<String> t = texts(PrintRenderer.ticket(s.build(), c));

        assertThat(t).allSatisfy(x -> assertThat(x.length()).isLessThanOrEqualTo(32));
        assertThat(t).filteredOn(x -> x.startsWith("   ")).isNotEmpty().allSatisfy(x -> assertThat(x).startsWith("   "));
    }

    @Test
    void voidTicket_keepsItsFixedTitle_andFollowsTheStationsFontAndSizes() {
        S s = new S("KITCHEN TICKET");
        s.title = "MY TITLE";
        s.fontB = true;
        s.itemSize = 2;
        s.showLocation = false;
        PrintRenderer.VoidContent c = new PrintRenderer.VoidContent(HEAD, "1x Pad Thai", null, "dropped");

        PrintRenderer.Rendered r = PrintRenderer.voided(s.build(), c);

        assertThat(texts(r)).startsWith("*** VOID ***").doesNotContain("MY TITLE").noneMatch(t -> t.startsWith("Restaurant"));
        assertThat(r.lines()).allSatisfy(l -> assertThat(l.fontB()).isTrue());
        assertThat(r.lines()).filteredOn(l -> l.text().startsWith("Reason:")).allSatisfy(l -> assertThat(l.size()).isEqualTo(2));
    }

    @Test
    void zReport_closedLineFollowsShowTime_identityAndFooterAreKept() {
        S s = new S("Z-REPORT");
        s.showTime = false;
        s.footerText = "Signed: ________";
        PrintRenderer.ReportContent c = ZReportContent.build("AB12CD34", "11 Oct 2026, 23:00", "1.00", "2.00", "0.00", "0.00",
                "3.00", "2", new ZReportContent.Folio("5.00", "0.00", "0.00"), "0.00", "1.00", "1.00", "0.00", null);

        List<String> t = texts(PrintRenderer.report(s.build(), c));

        assertThat(t).contains("Shift #AB12CD34").noneMatch(x -> x.startsWith("Closed:")).contains("Signed: ________");
        assertThat(t).anyMatch(x -> x.startsWith("Folio cash") && x.endsWith("5.00"));
    }

    // ---- bytes ----------------------------------------------------------------------------------

    @Test
    void fontB_andDoubleSize_emitTheirControlCodes_andResetBeforeTheCut() {
        S s = receiptStyle();
        s.fontB = true;
        s.titleSize = 2;

        byte[] bytes = bytes(PrintRenderer.receipt(s.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(null), null));

        assertThat(indexOf(bytes, new byte[] {0x1B, 0x4D, 0x01})).isPositive();
        assertThat(indexOf(bytes, new byte[] {0x1D, 0x21, 0x11})).isPositive();
        assertThat(lastIndexOf(bytes, new byte[] {0x1D, 0x21, 0x00})).isGreaterThan(indexOf(bytes, new byte[] {0x1D, 0x21, 0x11}));
        assertThat(lastIndexOf(bytes, new byte[] {0x1B, 0x4D, 0x00})).isGreaterThan(indexOf(bytes, new byte[] {0x1B, 0x4D, 0x01}));
    }

    @Test
    void logo_isPrintedAsRaster_onlyWhenShown_onlyWhenOneExists_andNeverOnATicket() throws Exception {
        byte[] logo = ReceiptLogo.normalize(png(200, 100, 0x000000));
        S shown = receiptStyle();
        shown.showLogo = true;
        shown.logoWidthPercent = 50;
        S hidden = receiptStyle();

        PrintRenderer.Rendered with = PrintRenderer.receipt(shown.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(null), logo);
        assertThat(with.logo().widthDots()).isEqualTo(288); // 50% of 576
        assertThat(indexOf(bytes(with), new byte[] {0x1D, 0x76, 0x30, 0x00})).isPositive();

        assertThat(PrintRenderer.receipt(hidden.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(null), logo).logo()).isNull();
        assertThat(PrintRenderer.receipt(shown.build(), PrintRenderer.ReceiptKind.RECEIPT, receipt(null), null).logo()).isNull();
        assertThat(PrintRenderer.ticket(shown.build(), ticket()).logo()).isNull();
        assertThat(PrintRenderer.report(shown.build(), ZReportContent.build("x", "y", "0", "0", "0", "0", "0", "0", null, "0", "0", "0", "0", null)).logo()).isNull();
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
