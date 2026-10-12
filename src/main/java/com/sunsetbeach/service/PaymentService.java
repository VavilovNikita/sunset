package com.sunsetbeach.service;

import com.sunsetbeach.entity.FolioPaymentEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.model.PaymentsSummary;
import com.sunsetbeach.repository.FolioPaymentRepository;
import com.sunsetbeach.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final FolioPaymentRepository folioPaymentRepository;

    private final Clock clock;

    public PaymentService(PaymentRepository paymentRepository, FolioPaymentRepository folioPaymentRepository, Clock clock) {
        this.paymentRepository = paymentRepository;
        this.folioPaymentRepository = folioPaymentRepository;
        this.clock = clock;
    }

    /**
     * {@code from}/{@code to} are hotel-local days (Asia/Bangkok), but {@code createdAt} holds UTC
     * wall-clock - so the window is Bangkok midnight to Bangkok midnight expressed in UTC
     * ({@link ReportDateRange}, the same boundaries every {@code /reports/*} endpoint uses). Cut
     * at UTC midnight instead, a payment taken at 02:00 Bangkok (19:00 UTC the day before) landed
     * in yesterday's figures.
     */
    @Transactional(readOnly = true)
    public PaymentsSummary getSummary(String from, String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        if (fromDate.isAfter(toDate)) {
            throw ValidationException.field("to", "must be on or after from");
        }

        ReportDateRange range = new ReportDateRange(fromDate, toDate);
        LocalDateTime start = range.startUtc(clock.getZone());
        LocalDateTime endExclusive = range.endUtcExclusive(clock.getZone());
        List<PaymentEntity> payments = paymentRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, endExclusive);

        PaymentAggregation.Totals totals = PaymentAggregation.aggregate(payments);
        // Desk payments against bookings' folios, same window as the POS rows above. The dashboard's
        // room revenue is this - money actually recorded - never a sum over bookings by status.
        BigDecimal folioTotal = folioPaymentRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, endExclusive).stream()
                .map(FolioPaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PaymentsSummary(
                from,
                to,
                PaymentAggregation.toShiftTotals(totals),
                PriceFormat.asDecimalString(totals.receivedTotal()),
                PriceFormat.asDecimalString(folioTotal));
    }
}
