package com.sunsetbeach.service;

import com.sunsetbeach.model.BookingTrackerMonth;
import com.sunsetbeach.repository.BookingTrackerSnapshotRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The weekly snapshots of the booking tracker: every Friday 23:55 (hotel time) the room nights, net revenue and
 * occupancy of every stay month of the current and the next season, as computed at that moment and frozen.
 *
 * <p>Same shape as the other sweeps - a plain {@code @Scheduled} method, no job queue. The cron is the real run; the
 * fixed-delay one is the catch-up after downtime (and the first run after this ships). Both ask {@link #runOnce},
 * which finds the latest Friday whose 23:55 has passed and writes whatever of its months is missing, so the database's
 * unique {@code (snapshotDate, stayMonth)} - not a check two threads can both pass - decides who wrote a row. A Friday
 * the server missed is written when it is back, as at that moment, and marked {@code late}; only the latest due Friday
 * is ever caught up, because an earlier one's figures can no longer be reconstructed honestly.
 */
@Service
public class BookingTrackerSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(BookingTrackerSnapshotService.class);

    static final LocalTime SNAPSHOT_TIME = LocalTime.of(23, 55);

    private final BookingTrackerService trackerService;
    private final BookingTrackerSnapshotRepository snapshotRepository;
    private final TransactionTemplate readOnly;
    private final Clock clock;
    private final boolean enabled;

    public BookingTrackerSnapshotService(
            BookingTrackerService trackerService,
            BookingTrackerSnapshotRepository snapshotRepository,
            PlatformTransactionManager transactionManager,
            Clock clock,
            @Value("${app.booking-tracker.snapshot-enabled:true}") boolean enabled) {
        this.trackerService = trackerService;
        this.snapshotRepository = snapshotRepository;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.clock = clock;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${app.booking-tracker.snapshot-cron:0 55 23 * * FRI}", zone = "Asia/Bangkok")
    public void sweepOnSchedule() {
        sweep();
    }

    @Scheduled(
            initialDelayString = "${app.booking-tracker.catch-up-initial-delay-ms:90000}",
            fixedDelayString = "${app.booking-tracker.catch-up-interval-ms:3600000}")
    public void sweepCatchUp() {
        sweep();
    }

    private void sweep() {
        if (!enabled) return;
        try {
            int written = runOnce();
            if (written > 0) {
                log.info("Booking tracker: wrote {} weekly snapshot row(s)", written);
            }
        } catch (RuntimeException e) {
            log.error("Booking tracker snapshot sweep failed", e);
        }
    }

    /** The latest Friday whose 23:55 is not after {@code now} (hotel time). */
    static LocalDate latestDueFriday(LocalDateTime now) {
        LocalDate friday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        return friday.atTime(SNAPSHOT_TIME).isAfter(now) ? friday.minusWeeks(1) : friday;
    }

    /** Writes the due Friday's missing rows; returns how many this call wrote (0 when everything is already there). */
    public int runOnce() {
        return runOnce(LocalDateTime.now(clock));
    }

    /** {@link #runOnce()} as at {@code now} (hotel time) - what the tests move instead of the clock. */
    int runOnce(LocalDateTime now) {
        LocalDate friday = latestDueFriday(now);
        LocalDate today = now.toLocalDate();
        boolean late = !friday.equals(today);

        BookingTrackerPeriod season = BookingTrackerPeriod.seasonFor(today);
        int written = 0;
        for (BookingTrackerPeriod period : List.of(season, season.nextSeason())) {
            List<String> months = period.months().stream().map(Object::toString).toList();
            if (months.stream().allMatch(m -> snapshotRepository.existsBySnapshotDateAndStayMonth(friday, m))) {
                continue;
            }
            BookingTrackerService.Computation computation = readOnly.execute(status -> trackerService.computation(period, today));
            for (BookingTrackerMonth month : computation.report().getMonths()) {
                String stayMonth = month.getMonth().get();
                int inserted = snapshotRepository.insertIfAbsent(
                        UUID.randomUUID().toString(),
                        friday,
                        stayMonth,
                        late,
                        month.getBookedRoomNights(),
                        new BigDecimal(month.getNetRoomRevenue()),
                        month.getOccupancyPercent().get() != null ? new BigDecimal(month.getOccupancyPercent().get()) : null);
                written += inserted;
            }
        }
        return written;
    }
}
