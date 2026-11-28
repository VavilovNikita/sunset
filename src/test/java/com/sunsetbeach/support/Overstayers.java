package com.sunsetbeach.support;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.service.OverstayChargeService;

/**
 * A guest who is overdue arrived during the stay and was never checked out. Check-in itself refuses a stay whose
 * dates are over, so a test that needs such a guest writes what that earlier check-in left behind and then lets the
 * nights accrue, as the sweep does.
 */
public final class Overstayers {

    private Overstayers() {}

    public static void leftCheckedIn(BookingRepository bookings, OverstayChargeService charges, String bookingId) {
        BookingEntity booking = bookings.findById(bookingId).orElseThrow();
        booking.setOccupancyStatus(OccupancyStatus.CHECKED_IN);
        // Early on the arrival day in UTC - when the guest came, as far as any report is concerned.
        booking.setCheckedInAt(booking.getCheckIn().atTime(7, 0));
        bookings.saveAndFlush(booking);
        charges.reconcile(bookingId);
    }
}
