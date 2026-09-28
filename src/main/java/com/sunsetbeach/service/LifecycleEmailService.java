package com.sunsetbeach.service;

import com.sunsetbeach.entity.GuestEmailLogEntity;
import com.sunsetbeach.entity.LifecycleEmailSettingsEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.LifecycleEmailType;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.GuestEmailLogRepository;
import com.sunsetbeach.repository.StayCandidate;
import com.sunsetbeach.repository.WinBackCandidate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * The daily sweep behind the automated guest lifecycle emails: pre-arrival, post-stay and
 * win-back. Which types run, and their day/month thresholds, come from
 * {@link LifecycleEmailSettingsService} (ADMIN-only); the wording is fixed in
 * {@link EmailService}.
 *
 * <p><b>Audience: a verified, not-opted-out {@code GuestAccount}, never a bare {@code Guest}
 * card.</b> Every email the system sent before this was one the recipient caused themselves
 * (verification, a booking status change). These aren't, so they go only to people who created an
 * account and confirmed their email - the one population with any consent basis. A card staff
 * typed in at the front desk never qualifies. The email goes to the account's own verified
 * address, not whatever is on the booking. Every email carries an unsubscribe link
 * ({@code GET /guest-auth/unsubscribe}), and one opt-out stops all three types.
 *
 * <p><b>Idempotency is the {@code GuestEmailLog} table</b> - the role {@code expiryReminderSent}
 * plays for {@link BookingExpiryService}, as a log because one guest can legitimately get several
 * of these over time. The candidate queries exclude anything already logged, so running the sweep
 * twice in a day sends nothing the second time. A row is written only after {@link EmailService}
 * reports the email actually went out. A failed win-back is picked up again by the next day's
 * sweep; a failed pre-arrival/post-stay is not, since "exactly N days" no longer matches the next
 * day - same no-retry, best-effort contract as the rest of {@link EmailService}.
 *
 * <p><b>Not {@code @Transactional}</b>, deliberately: each log row commits on its own, right after
 * its own send. One transaction around the whole sweep would roll back every earlier row if a
 * later one failed, while those emails had already gone out - and the next run would send them
 * again.
 *
 * <p>Same {@code @Scheduled} mechanism as the other sweeps (see CLAUDE.md, "Failure handling"),
 * but a daily cron rather than a fixed delay: nothing here is time-critical, and a fixed delay
 * would fire at whatever hour the server last started. 10:00 hotel time, so guests get these at a
 * reasonable hour.
 */
@Service
public class LifecycleEmailService {

    private static final Logger log = LoggerFactory.getLogger(LifecycleEmailService.class);

    private final LifecycleEmailSettingsService settingsService;
    private final GuestEmailLogRepository guestEmailLogRepository;
    private final EmailService emailService;
    private final Clock clock;

    public LifecycleEmailService(
            LifecycleEmailSettingsService settingsService,
            GuestEmailLogRepository guestEmailLogRepository,
            EmailService emailService,
            Clock clock) {
        this.settingsService = settingsService;
        this.guestEmailLogRepository = guestEmailLogRepository;
        this.emailService = emailService;
        this.clock = clock;
    }

    /** How many emails of each type one sweep sent - for the log line and for tests. */
    public record SweepResult(int preArrival, int postStay, int winBack) {
    }

    // The zone is a literal rather than read from the Clock bean (an annotation attribute must be
    // a constant) - it's the same Asia/Bangkok as ClockConfig.
    @Scheduled(cron = "${app.lifecycle-email.cron:0 0 10 * * *}", zone = "Asia/Bangkok")
    public void runDailySweep() {
        SweepResult result = sweep();
        log.info("Lifecycle email sweep sent {} pre-arrival, {} post-stay, {} win-back email(s)",
                result.preArrival(), result.postStay(), result.winBack());
    }

    public SweepResult sweep() {
        LifecycleEmailSettingsEntity settings = settingsService.getEntity();
        LocalDate today = LocalDate.now(clock);

        int preArrival = 0;
        if (settings.isPreArrivalEnabled()) {
            // NO_SHOW as the excluded occupancy is a no-op here (nobody is a no-show before
            // arriving) - it just lets both stay queries share one shape.
            List<StayCandidate> candidates = guestEmailLogRepository.findStayCandidatesByCheckIn(
                    today.plusDays(settings.getPreArrivalDaysBefore()), BookingStatus.CANCELLED, OccupancyStatus.NO_SHOW,
                    LifecycleEmailType.PRE_ARRIVAL);
            for (StayCandidate c : candidates) {
                if (sendAndLog(c.guestId(), c.bookingId(), LifecycleEmailType.PRE_ARRIVAL, EmailService.PRE_ARRIVAL_SUBJECT,
                        () -> emailService.sendPreArrivalEmail(
                                c.email(), c.guestName(), c.roomName(), c.checkIn(), c.checkOut(), c.unsubscribeToken()))) {
                    preArrival++;
                }
            }
        }

        int postStay = 0;
        if (settings.isPostStayEnabled()) {
            // A no-show never stayed - "thank you for staying" would be wrong.
            List<StayCandidate> candidates = guestEmailLogRepository.findStayCandidatesByCheckOut(
                    today.minusDays(settings.getPostStayDaysAfter()), BookingStatus.CANCELLED, OccupancyStatus.NO_SHOW,
                    LifecycleEmailType.POST_STAY);
            String reviewUrl = settings.getPostStayReviewUrl();
            for (StayCandidate c : candidates) {
                if (sendAndLog(c.guestId(), c.bookingId(), LifecycleEmailType.POST_STAY, EmailService.POST_STAY_SUBJECT,
                        () -> emailService.sendPostStayEmail(
                                c.email(), c.guestName(), c.roomName(), c.checkIn(), c.checkOut(), reviewUrl, c.unsubscribeToken()))) {
                    postStay++;
                }
            }
        }

        int winBack = 0;
        if (settings.isWinBackEnabled()) {
            // Both cutoffs are the same N months: a guest qualifies once their last stay is older
            // than that, and gets another nudge only once their last win-back email is too.
            LocalDate cutoff = today.minusMonths(settings.getWinBackMonthsSinceStay());
            List<WinBackCandidate> candidates = guestEmailLogRepository.findWinBackCandidates(
                    cutoff, cutoff.atStartOfDay(), BookingStatus.CANCELLED, LifecycleEmailType.WIN_BACK);
            for (WinBackCandidate c : candidates) {
                if (sendAndLog(c.guestId(), null, LifecycleEmailType.WIN_BACK, EmailService.WIN_BACK_SUBJECT,
                        () -> emailService.sendWinBackEmail(c.email(), c.guestName(), c.unsubscribeToken()))) {
                    winBack++;
                }
            }
        }

        return new SweepResult(preArrival, postStay, winBack);
    }

    /** One candidate's failure is logged and skipped - it never stops the rest of the sweep. */
    private boolean sendAndLog(String guestId, String bookingId, LifecycleEmailType type, String subject, BooleanSupplier send) {
        try {
            if (!send.getAsBoolean()) {
                return false;
            }
            GuestEmailLogEntity entry = new GuestEmailLogEntity();
            entry.setGuestId(guestId);
            entry.setBookingId(bookingId);
            entry.setType(type);
            entry.setSubject(subject);
            entry.setSentAt(LocalDateTime.now(clock));
            guestEmailLogRepository.save(entry);
            return true;
        } catch (Exception e) {
            log.error("Lifecycle email {} for guest {} (booking {}) failed:", type, guestId, bookingId, e);
            return false;
        }
    }
}
