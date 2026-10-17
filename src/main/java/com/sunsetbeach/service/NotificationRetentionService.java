package com.sunsetbeach.service;

import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.repository.NotificationRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Housekeeping, not business history: notifications older than {@code app.notifications.retention-days}
 * (90) are deleted every night at 03:30 hotel time, their deliveries with them (ON DELETE CASCADE).
 * Not audited. The cron is a property so tests can switch it off ("-"): a sweep in another test's
 * context with a different fixed clock would otherwise delete rows that test just wrote.
 */
@Service
public class NotificationRetentionService {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetentionService.class);

    private final NotificationRepository notificationRepository;
    private final Clock clock;
    private final int retentionDays;

    public NotificationRetentionService(
            NotificationRepository notificationRepository, Clock clock, @Value("${app.notifications.retention-days:90}") int retentionDays) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${app.notifications.retention-cron:0 30 3 * * *}", zone = "Asia/Bangkok")
    public void sweep() {
        try {
            int removed = purge();
            if (removed > 0) {
                log.info("Removed {} notification(s) older than {} days", removed, retentionDays);
            }
        } catch (RuntimeException e) {
            log.error("Notification retention sweep failed", e);
        }
    }

    /** Deletes what is older than the retention window; returns how many notifications went. */
    @Transactional
    public int purge() {
        return notificationRepository.deleteOlderThan(TimestampFormat.nowUtc(clock).minusDays(retentionDays));
    }
}
