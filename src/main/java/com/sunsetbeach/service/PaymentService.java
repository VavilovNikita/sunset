package com.sunsetbeach.service;

import com.sunsetbeach.entity.FolioPaymentEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.model.PaymentsSummary;
import com.sunsetbeach.repository.FolioPaymentRepository;
import com.sunsetbeach.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final FolioPaymentRepository folioPaymentRepository;

    public PaymentService(PaymentRepository paymentRepository, FolioPaymentRepository folioPaymentRepository) {
        this.paymentRepository = paymentRepository;
        this.folioPaymentRepository = folioPaymentRepository;
    }

    @Transactional(readOnly = true)
    public PaymentsSummary getSummary(String from, String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        if (fromDate.isAfter(toDate)) {
            throw ValidationException.field("to", "must be on or after from");
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime endExclusive = toDate.plusDays(1).atStartOfDay();
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
