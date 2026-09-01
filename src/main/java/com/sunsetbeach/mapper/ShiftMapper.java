package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftListItem;
import com.sunsetbeach.model.ShiftSummary;
import com.sunsetbeach.model.ShiftTotals;
import java.math.BigDecimal;
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

    /**
     * {@code expectedCash}/{@code discrepancy} are pre-computed by the caller (same
     * {@code openingCashFloat + totals.cash} / {@code closingCashCounted - expectedCash}
     * arithmetic as {@link com.sunsetbeach.service.ShiftService#describeShiftClose}/
     * {@code buildZReportPayload}/{@code appendSummary} - this mapper only shapes the DTO, it
     * doesn't own that formula. {@code discrepancy} is null exactly when {@code closingCashCounted}
     * is (nothing counted yet, or the shift is still open).
     */
    public ShiftListItem toListItemDto(
            ShiftEntity entity,
            String openedByEmail,
            String closedByEmail,
            ShiftTotals totals,
            BigDecimal expectedCash,
            BigDecimal discrepancy) {
        return new ShiftListItem(
                entity.getId(),
                entity.getOpenedByUserId(),
                openedByEmail,
                TimestampFormat.toUtc(entity.getOpenedAt()),
                entity.getClosedByUserId(),
                closedByEmail,
                entity.getClosedAt() != null ? TimestampFormat.toUtc(entity.getClosedAt()) : null,
                entity.getOpeningCashFloat() != null ? PriceFormat.asDecimalString(entity.getOpeningCashFloat()) : null,
                entity.getClosingCashCounted() != null ? PriceFormat.asDecimalString(entity.getClosingCashCounted()) : null,
                entity.getStatus(),
                totals,
                PriceFormat.asDecimalString(expectedCash),
                discrepancy != null ? PriceFormat.asDecimalString(discrepancy) : null);
    }
}
