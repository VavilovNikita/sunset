package com.sunsetbeach.repository;

import java.time.LocalDate;

/** One booking the lifecycle email sweep may email about, with everything the email itself needs - see {@link GuestEmailLogRepository}. */
public record StayCandidate(
        String guestId,
        String bookingId,
        String email,
        String unsubscribeToken,
        String guestName,
        String roomName,
        LocalDate checkIn,
        LocalDate checkOut) {
}
