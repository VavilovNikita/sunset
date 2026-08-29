package com.sunsetbeach.entity;

/**
 * Internal-only distinction, never exposed via the API: was this booking submitted by a guest
 * through the public {@code POST /bookings} form, or entered by staff through
 * {@code POST /bookings/staff}? Drives {@link com.sunsetbeach.service.BookingExpiryService},
 * which only auto-cancels unconfirmed {@code PUBLIC} bookings - a {@code STAFF} booking is
 * confirmed by definition, by the person who created it.
 */
public enum BookingSource {
    PUBLIC,
    STAFF
}
