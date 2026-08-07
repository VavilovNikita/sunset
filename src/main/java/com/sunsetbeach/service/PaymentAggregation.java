package com.sunsetbeach.service;

import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.model.ShiftTotals;
import java.math.BigDecimal;
import java.util.List;

/**
 * The one place Payment rows get summed by method - shared by ShiftService (one shift's
 * payments) and PaymentService (a date range's payments across all shifts), so the per-method
 * switch only exists once.
 */
public final class PaymentAggregation {

    private PaymentAggregation() {
    }

    public static Totals aggregate(List<PaymentEntity> payments) {
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;
        BigDecimal roomCharge = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        for (PaymentEntity payment : payments) {
            switch (payment.getMethod()) {
                case CASH -> cash = cash.add(payment.getAmount());
                case CARD -> card = card.add(payment.getAmount());
                case ROOM_CHARGE -> roomCharge = roomCharge.add(payment.getAmount());
                case OTHER -> other = other.add(payment.getAmount());
            }
        }
        return new Totals(cash, card, roomCharge, other, payments.size());
    }

    public static ShiftTotals toShiftTotals(Totals totals) {
        return new ShiftTotals(
                PriceFormat.asDecimalString(totals.cash()),
                PriceFormat.asDecimalString(totals.card()),
                PriceFormat.asDecimalString(totals.roomCharge()),
                PriceFormat.asDecimalString(totals.other()),
                totals.paymentCount());
    }

    /**
     * Raw (unformatted) per-method sums. {@code roomCharge} is intentionally excluded from
     * {@link #receivedTotal()} - it's money moved onto a room folio, not received revenue; see
     * GET /payments/summary's openapi.yaml description for the full reasoning.
     */
    public record Totals(BigDecimal cash, BigDecimal card, BigDecimal roomCharge, BigDecimal other, int paymentCount) {
        public BigDecimal receivedTotal() {
            return cash.add(card).add(other);
        }
    }
}
