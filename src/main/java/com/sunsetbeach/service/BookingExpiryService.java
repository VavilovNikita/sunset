package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import java.time.LocalDate;
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
 * <p><b>Business-day window, not a flat calendar duration.</b> This hotel confirms bookings by a
 * person looking at each request - there's no deposit, no automatic confirmation, and nobody
 * works weekends. A flat 24-48h clock would auto-cancel a Friday-evening inquiry sometime over
 * the weekend, before anyone is back at a desk to look at it. Counting only weekdays
 * ({@link BusinessDayCounter}) means a request submitted just before a closed period gets the
 * same amount of *working* attention time as one submitted on a Monday morning.
 *
 * <p><b>A reminder before the cancellation, not just the cancellation itself.</b> Two silent
 * failure modes were both rejected: cancelling with no guest-facing email means a guest can show
 * up to a reservation that quietly stopped existing days ago; cancelling *with* one means an
 * automated "your booking has been cancelled" notice can reach a guest whose request simply
 * wasn't reviewed yet because it arrived right before a closed period - which reads as a
 * considered rejection, not what actually happened. The fix is to make the auto-cancellation a
 * rare event in practice: {@link EmailService#sendBookingExpiringReminderEmail} nudges ADMIN/
 * MANAGER one business day before the deadline, so a human has a real chance to act (confirm,
 * decline, or extend the hold by touching the booking) before it ever reaches the cutoff. Given
 * that safety net, the actual auto-cancellation - now the exception, not the norm - deliberately
 * does NOT notify the guest (see {@link #sweepUnconfirmedPublicBookings()}): an automated
 * rejection is the wrong tone for "staff missed the reminder too", and {@link BookingService#updateStatus}
 * (the real, human-decided cancellation path) still emails the guest exactly as before.
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
    private final RoomRepository roomRepository;
    private final EmailService emailService;
    private final int expiryBusinessDays;

    public BookingExpiryService(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            EmailService emailService,
            // 2 business days: a request that arrives Monday gets a staff reminder Tuesday and
            // expires Wednesday if still untouched; one that arrives Friday evening gets its
            // reminder Monday and expires Tuesday - the weekend costs it nothing. Long enough
            // that a genuine guest isn't punished for the hotel being closed, short enough that a
            // flood of fake NEW bookings self-heals within a couple of working days instead of
            // holding inventory for months.
            @Value("${app.booking.new-booking-expiry-business-days:2}") int expiryBusinessDays) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.emailService = emailService;
        this.expiryBusinessDays = expiryBusinessDays;
    }

    /**
     * Every 15 minutes by default - frequent enough that both the reminder and the eventual
     * cancellation land within the same working day they're due, and cheap enough (one indexed
     * query over what's normally a tiny or empty NEW/PUBLIC set) that running it often costs
     * nothing.
     */
    @Scheduled(fixedDelayString = "${app.booking.expiry-sweep-interval-ms:900000}")
    @Transactional
    public void sweepUnconfirmedPublicBookings() {
        List<BookingEntity> candidates = bookingRepository.findByStatusAndSource(BookingStatus.NEW, BookingSource.PUBLIC);
        if (candidates.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        for (BookingEntity booking : candidates) {
            int businessDaysWaiting = BusinessDayCounter.countBusinessDaysBetween(booking.getCreatedAt().toLocalDate(), today);

            if (businessDaysWaiting >= expiryBusinessDays) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                log.info(
                        "Auto-cancelled unconfirmed public booking {} ({} business day(s) unconfirmed) - guest not notified, "
                                + "see BookingExpiryService's javadoc",
                        booking.getId(),
                        businessDaysWaiting);
                // Deliberately NOT emailService.sendGuestStatusEmail(...) here - see the class
                // javadoc for why an automated guest-facing cancellation notice is the wrong
                // move for a request the hotel simply never got to, as opposed to one a human
                // actually declined via BookingService.updateStatus.
            } else if (businessDaysWaiting >= expiryBusinessDays - 1 && !booking.isExpiryReminderSent()) {
                roomRepository.findById(booking.getRoomId()).ifPresent(room -> {
                    emailService.sendBookingExpiringReminderEmail(booking, room);
                    booking.setExpiryReminderSent(true);
                    bookingRepository.save(booking);
                });
            }
        }
    }
}
