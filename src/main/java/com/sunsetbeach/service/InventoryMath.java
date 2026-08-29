package com.sunsetbeach.service;

/**
 * The one implementation of "how many units of a room type remain free on a given day" -
 * {@code activeUnitCount - blockedUnits - bookedUnits} - shared by {@link AvailabilityService}
 * (per room type, per month) and {@link BookingCalendarService} (every room type at once, an
 * arbitrary date range), so the two never drift into computing this differently. Deliberately
 * not clamped at zero: a negative result is a real signal (e.g. more active bookings/blocks than
 * currently-active units, after a unit was deactivated), not noise to hide from staff.
 */
final class InventoryMath {

    private InventoryMath() {
    }

    static int availableCount(int unitCount, int blockedCount, int bookedCount) {
        return unitCount - blockedCount - bookedCount;
    }
}
