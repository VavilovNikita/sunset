package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.model.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toDto(PaymentEntity entity) {
        return new Payment(
                entity.getId(),
                entity.getOrderId(),
                entity.getMethod(),
                PriceFormat.asDecimalString(entity.getAmount()),
                entity.getBookingId(),
                entity.getRecordedByUserId(),
                entity.getShiftId(),
                TimestampFormat.toUtc(entity.getCreatedAt()));
    }
}
