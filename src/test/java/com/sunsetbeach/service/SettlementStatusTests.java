package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.SettlementStatus;
import com.sunsetbeach.service.BookingFolioCalculator.FolioBreakdown;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** {@code settlement} is read off the folio's own balance - PAID only says the room was paid. */
class SettlementStatusTests {

    private static final BigDecimal STAY = new BigDecimal("1000.00");

    private static FolioBreakdown folio(BookingStatus status, BigDecimal paid, int overstayNights, String overstayAmount) {
        return new FolioBreakdown(STAY, BigDecimal.ZERO, BigDecimal.ZERO, 0, paid, status, OccupancyStatus.CHECKED_IN, false, overstayNights,
                new BigDecimal(overstayAmount), overstayNights > 0);
    }

    @Test
    void aPaidBookingThatStillOwesIsOwes() {
        // The room is paid (and PAID), the guest stayed 3 more nights: 4500 is owed.
        FolioBreakdown f = folio(BookingStatus.PAID, STAY, 3, "4500.00");
        assertThat(f.balanceDue()).isEqualByComparingTo("4500.00");
        assertThat(f.settlement()).isEqualTo(SettlementStatus.OWES);
    }

    @Test
    void aPaidBookingWithNothingLeftIsSettled() {
        assertThat(folio(BookingStatus.PAID, STAY, 0, "0.00").settlement()).isEqualTo(SettlementStatus.SETTLED);
    }

    @Test
    void collectingMoreThanWasChargedIsCredit() {
        FolioBreakdown f = folio(BookingStatus.CONFIRMED, new BigDecimal("1500.00"), 0, "0.00");
        assertThat(f.balanceDue()).isEqualByComparingTo("0.00");
        assertThat(f.settlement()).isEqualTo(SettlementStatus.CREDIT);
    }

    @Test
    void anUnpaidBookingOwes() {
        assertThat(folio(BookingStatus.CONFIRMED, BigDecimal.ZERO, 0, "0.00").settlement()).isEqualTo(SettlementStatus.OWES);
    }
}
