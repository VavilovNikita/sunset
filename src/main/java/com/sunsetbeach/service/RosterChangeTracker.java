package com.sunsetbeach.service;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.repository.RosterChangeMarkRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remembers which employee-days a manager's roster gesture touched, so that {@link StaffBotScanService}
 * can tell the employee <em>once</em>, with the final state, after the dragging stops. Called from
 * {@code RosterService} inside the gesture's own transaction (the mark commits or rolls back with it).
 *
 * <p>Changes made by an approved shift request are not recorded ({@link #suppressed}): the employee already
 * got the decision, and a second "your schedule changed" would only repeat it. Only the manual gestures call
 * this - the month generator, the copy and the Excel imports are bulk set-up, not a change to announce.
 */
@Service
public class RosterChangeTracker {

    private static final ThreadLocal<Boolean> SUPPRESSED = new ThreadLocal<>();

    private final RosterChangeMarkRepository markRepository;
    private final StaffBotSettingsService settingsService;
    private final Clock clock;

    public RosterChangeTracker(RosterChangeMarkRepository markRepository, StaffBotSettingsService settingsService, Clock clock) {
        this.markRepository = markRepository;
        this.settingsService = settingsService;
        this.clock = clock;
    }

    /** Runs {@code work} without recording anything (on this thread). */
    public <T> T suppressed(Supplier<T> work) {
        Boolean before = SUPPRESSED.get();
        SUPPRESSED.set(Boolean.TRUE);
        try {
            return work.get();
        } finally {
            if (before == null) {
                SUPPRESSED.remove();
            } else {
                SUPPRESSED.set(before);
            }
        }
    }

    /**
     * @param beforeShiftCodeId the shift code the employee had on {@code date} <em>before</em> this change,
     *                          null if the day was off. Only the first call of a burst keeps it.
     */
    @Transactional
    public void record(String employeeUserId, LocalDate date, String beforeShiftCodeId) {
        if (Boolean.TRUE.equals(SUPPRESSED.get())) {
            return;
        }
        StaffBotSettingsEntity settings = settingsService.getEntity();
        if (!settings.isRosterChangeEnabled()) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || date.isAfter(today.plusDays(settings.getRosterChangeHorizonDays()))) {
            return;
        }
        markRepository.touch(UUID.randomUUID().toString(), employeeUserId, date, beforeShiftCodeId, TimestampFormat.nowUtc(clock));
    }
}
