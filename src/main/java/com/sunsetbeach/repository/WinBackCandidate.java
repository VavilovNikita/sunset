package com.sunsetbeach.repository;

import java.time.LocalDate;

/** One guest the win-back sweep may email, with the checkout that made them eligible - see {@link GuestEmailLogRepository#findWinBackCandidates}. */
public record WinBackCandidate(String guestId, String email, String unsubscribeToken, String guestName, LocalDate lastCheckOut) {
}
