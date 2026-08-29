package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.repository.BookingRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auto-cancels unconfirmed public bookings that have sat in {@code NEW} for too long, the same
 * way a hotel's own hold policy would release an unconfirmed reservation back to inventory. This
 * is the real bound on the "flood POST /bookings with fake reservations" business risk:
 * {@link com.sunsetbeach.security.BookingRateLimiter} only slows a single unproxied source down,
 * but it can be bypassed (rotating IPs, or a deployment with no trusted reverse proxy in front of
 * it). Without this sweep, a bypassed limiter could hold every room for months, since nothing
 * else in this codebase ever expires a booking - this is the only place that happens.
 *
 * <p>Deliberately scoped to {@link BookingSource#PUBLIC} only - a {@link BookingSource#STAFF}
 * booking from {@code POST /bookings/staff} was confirmed by a person at the front desk at
 * creation time and must never be silently cancelled out from under them.
 *
 * <p>Reuses {@link PrintService}'s {@code @Scheduled} background-sweep shape (a fixed-delay job
 * over a small filtered query, re-run periodically) rather than introducing a second scheduling
 * mechanism for what is the same kind of problem: a durable row that needs periodic follow-up
 * with no request driving it.
 */
@Service
public class BookingExpiryService {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryService.class);

    private final BookingRepository bookingRepository;
    private final int expiryHours;

    public BookingExpiryService(
            BookingRepository bookingRepository,
            // 24 hours: long enough that a genuine guest deciding overnight whether to confirm
            // isn't punished, short enough that a flood of fake NEW bookings self-heals within a
            // day instead of holding inventory for months. This is the standard "hold without a
            // deposit" window hotels already use for unconfirmed reservations - not a novel
            // policy invented for this fix.
            @Value("${app.booking.new-booking-expiry-hours:24}") int expiryHours) {
        this.bookingRepository = bookingRepository;
        this.expiryHours = expiryHours;
    }

    /**
     * Every 15 minutes by default - frequent enough that the attack window this closes stays
     * measured in minutes, not hours, and cheap enough (one indexed query, normally an empty
     * result) that running it often costs nothing.
     */
    @Scheduled(fixedDelayString = "${app.booking.expiry-sweep-interval-ms:900000}")
    @Transactional
    public void cancelExpiredPublicBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(expiryHours);
        List<BookingEntity> stale =
                bookingRepository.findByStatusAndSourceAndCreatedAtBefore(BookingStatus.NEW, BookingSource.PUBLIC, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        for (BookingEntity booking : stale) {
            booking.setStatus(BookingStatus.CANCELLED);
        }
        bookingRepository.saveAll(stale);
        log.info("Auto-cancelled {} unconfirmed public booking(s) older than {}h", stale.size(), expiryHours);
    }
}
