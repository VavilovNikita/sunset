package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftSummary;
import com.sunsetbeach.model.ShiftTotals;
import org.springframework.stereotype.Component;

@Component
public class ShiftMapper {

    public Shift toDto(ShiftEntity entity) {
        return new Shift(
                entity.getId(),
                entity.getOpenedByUserId(),
                TimestampFormat.toUtc(entity.getOpenedAt()),
                entity.getClosedByUserId(),
                entity.getClosedAt() != null ? TimestampFormat.toUtc(entity.getClosedAt()) : null,
                entity.getOpeningCashFloat() != null ? PriceFormat.asDecimalString(entity.getOpeningCashFloat()) : null,
                entity.getClosingCashCounted() != null ? PriceFormat.asDecimalString(entity.getClosingCashCounted()) : null,
                entity.getStatus(),
                entity.getNotes());
    }

    public ShiftSummary toSummaryDto(ShiftEntity entity, ShiftTotals totals) {
        return new ShiftSummary(
                entity.getId(),
                entity.getOpenedByUserId(),
                TimestampFormat.toUtc(entity.getOpenedAt()),
                entity.getClosedByUserId(),
                entity.getClosedAt() != null ? TimestampFormat.toUtc(entity.getClosedAt()) : null,
                entity.getOpeningCashFloat() != null ? PriceFormat.asDecimalString(entity.getOpeningCashFloat()) : null,
                entity.getClosingCashCounted() != null ? PriceFormat.asDecimalString(entity.getClosingCashCounted()) : null,
                entity.getStatus(),
                entity.getNotes(),
                totals);
    }
}
