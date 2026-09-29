package com.sunsetbeach.service;

import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.RevenueCode;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The two rules the revenue statistic (Z410, {@link ReportService#revenueStatistic}) and the
 * ledger's automatic postings ({@link LedgerService}) must agree on - kept in one place so a POS
 * line can never be FNB in one and SPA in the other, or carry a different VAT.
 */
final class RevenueClassification {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private RevenueClassification() {
    }

    /** Which revenue code a POS line belongs to, by its menu item's department. */
    static RevenueCode of(MenuDepartment department) {
        return switch (department) {
            case KITCHEN, BAR -> RevenueCode.FNB;
            case SPA -> RevenueCode.SPA;
        };
    }

    /**
     * The VAT already inside a VAT-inclusive {@code gross} (every price in this system is one):
     * {@code gross × rate / (100 + rate)}, half-up to two decimals. Net is {@code gross - vat},
     * so the two always add back to gross exactly.
     */
    static BigDecimal vatInside(BigDecimal gross, BigDecimal ratePercent) {
        return gross.multiply(ratePercent).divide(HUNDRED.add(ratePercent), 2, RoundingMode.HALF_UP);
    }
}
