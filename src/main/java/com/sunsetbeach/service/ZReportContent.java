package com.sunsetbeach.service;

import com.sunsetbeach.service.PrintRenderer.ReportContent;
import com.sunsetbeach.service.PrintRenderer.ReportRow;
import java.util.ArrayList;
import java.util.List;

/**
 * The Z-report's rows, in one place so the real report ({@link ShiftService}) and the layout
 * editor's sample ({@link PrintLayoutService}) can't drift apart: same labels, same order, same
 * dividers. Takes strings already formatted by the caller - money is never computed here.
 */
final class ZReportContent {

    private ZReportContent() {}

    record Folio(String cash, String card, String other) {}

    /** {@code folio} is null when reception took no folio payments in the shift. */
    static ReportContent build(
            String shiftRef,
            String closedAt,
            String cash,
            String card,
            String other,
            String roomCharge,
            String receivedTotal,
            String paymentCount,
            Folio folio,
            String openingFloat,
            String expectedCash,
            String countedCash,
            String discrepancy,
            String sampleNote) {
        List<ReportRow> rows = new ArrayList<>();
        rows.add(new ReportRow("Cash", cash));
        rows.add(new ReportRow("Card", card));
        rows.add(new ReportRow("Other", other));
        rows.add(new ReportRow("Room charge (folio, not received)", roomCharge));
        rows.add(ReportRow.divider());
        rows.add(new ReportRow("Received total", receivedTotal));
        rows.add(new ReportRow("Payments", paymentCount));
        if (folio != null) {
            rows.add(ReportRow.divider());
            rows.add(new ReportRow("Folio cash (reception)", folio.cash()));
            rows.add(new ReportRow("Folio card", folio.card()));
            rows.add(new ReportRow("Folio other", folio.other()));
        }
        rows.add(ReportRow.divider());
        rows.add(new ReportRow("Opening float", openingFloat));
        rows.add(new ReportRow("Expected cash", expectedCash));
        rows.add(new ReportRow("Counted cash", countedCash));
        rows.add(new ReportRow("Discrepancy", discrepancy));
        return new ReportContent("Shift #" + shiftRef, "Closed: " + closedAt, rows, sampleNote);
    }
}
