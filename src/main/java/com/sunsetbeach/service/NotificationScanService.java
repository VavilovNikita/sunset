package com.sunsetbeach.service;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.entity.NotificationSettingsEntity;
import com.sunsetbeach.entity.TerminalSilenceEpisodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.model.TodayShiftState;
import com.sunsetbeach.model.TodayShiftStatus;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.entity.NotificationEntity;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.NotificationRepository;
import com.sunsetbeach.repository.TerminalSilenceEpisodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The once-a-minute scanner behind the late / absence / terminal / block-reminder notifications -
 * the project's usual {@code @Scheduled} fixed-delay sweep. It holds no state of its own: every
 * condition is re-derived from the data each pass and an alert is raised through
 * {@link NotificationService#raise} under a {@code dedupKey} the database makes unique, so a repeated
 * (or racing) pass cannot duplicate anything, and a backend that was down catches up on its own.
 *
 * <p><b>Who is late is not decided here.</b> {@link AttendanceService#getTodayShiftBoard} is the one
 * definition ({@code LATE}: the interval has started, no complete punch pair covers it yet, and it
 * has not ended; {@code MISSED}: it has ended). This class only adds the configured thresholds on top
 * of the interval's planned start.
 *
 * <p><b>A silent terminal suppresses the roster alerts.</b> A terminal that has not been reached for
 * {@code terminalSilenceMinutes} cannot have reported anyone's punch, so "late" would be a false
 * accusation. While any active terminal is silent - or when there is no active terminal at all -
 * nothing about lateness or absence is raised; the directors get one "not responding" per episode and
 * one "back online" when it ends. This is the guard the attendance module already needed (see
 * {@code AttendanceDevicePollService}'s own javadoc on silence), now with a voice.
 */
@Service
public class NotificationScanService {

    private static final Logger log = LoggerFactory.getLogger(NotificationScanService.class);

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String ATTENDANCE_PATH = "/admin/today";
    private static final String DEVICES_PATH = "/admin/attendance-devices";

    private final NotificationSettingsService settingsService;
    private final NotificationService notificationService;
    private final BlockNotificationService blockNotificationService;
    private final StaffBotScanService staffBotScanService;
    private final AttendanceService attendanceService;
    private final AttendanceDeviceRepository deviceRepository;
    private final TerminalSilenceEpisodeRepository episodeRepository;
    private final UserRepository userRepository;
    private final AttendancePunchRepository punchRepository;
    private final NotificationRepository notificationRepository;
    private final Clock clock;
    private final TransactionTemplate tx;

    public NotificationScanService(
            NotificationSettingsService settingsService,
            NotificationService notificationService,
            BlockNotificationService blockNotificationService,
            StaffBotScanService staffBotScanService,
            AttendanceService attendanceService,
            AttendanceDeviceRepository deviceRepository,
            TerminalSilenceEpisodeRepository episodeRepository,
            UserRepository userRepository,
            AttendancePunchRepository punchRepository,
            NotificationRepository notificationRepository,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.settingsService = settingsService;
        this.notificationService = notificationService;
        this.blockNotificationService = blockNotificationService;
        this.staffBotScanService = staffBotScanService;
        this.attendanceService = attendanceService;
        this.deviceRepository = deviceRepository;
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
        this.punchRepository = punchRepository;
        this.notificationRepository = notificationRepository;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${app.notifications.scan-interval-ms:60000}", initialDelayString = "${app.notifications.initial-delay-ms:10000}")
    public void scan() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            log.error("Notification scan failed", e);
        }
    }

    /** One pass. The three parts are independent: one failing doesn't skip the others. */
    public void runOnce() {
        NotificationSettingsEntity settings = settingsService.getEntity();
        boolean terminalsHealthy = false;
        try {
            terminalsHealthy = scanTerminals(settings);
        } catch (RuntimeException e) {
            log.error("Terminal health scan failed", e);
        }
        if (terminalsHealthy) {
            try {
                scanRoster(settings);
            } catch (RuntimeException e) {
                log.error("Late/absence scan failed", e);
            }
        }
        try {
            blockNotificationService.sendMorningReminders();
        } catch (RuntimeException e) {
            log.error("Block reminder scan failed", e);
        }
        // Shift requests, evening reminders, roster-change messages: independent of the terminal guard above.
        staffBotScanService.runOnce();
    }

    // ---- terminals ----------------------------------------------------------------------------

    /**
     * Raises "not responding" / "back online" and returns whether late/absence alerts may be raised:
     * true only when there is at least one active terminal and none of them is silent.
     */
    boolean scanTerminals(NotificationSettingsEntity settings) {
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        Duration threshold = Duration.ofMinutes(settings.getTerminalSilenceMinutes());
        List<AttendanceDeviceEntity> devices = deviceRepository.findByActiveTrue();
        boolean anySilent = false;
        for (AttendanceDeviceEntity device : devices) {
            boolean silent = TerminalHealthService.isSilent(device, nowUtc, threshold);
            anySilent |= silent;
            if (!settings.isTerminalAlertsEnabled()) {
                continue;
            }
            try {
                if (silent) {
                    openEpisode(device, settings, nowUtc);
                } else {
                    closeEpisode(device, settings);
                }
            } catch (RuntimeException e) {
                log.error("Terminal alert for {} failed", device.getId(), e);
            }
        }
        // An episode of a terminal that was switched off is not "back online" - it is just gone.
        if (settings.isTerminalAlertsEnabled()) {
            List<String> activeIds = devices.stream().map(AttendanceDeviceEntity::getId).toList();
            tx.executeWithoutResult(status -> {
                for (TerminalSilenceEpisodeEntity episode : episodeRepository.findAll()) {
                    if (!activeIds.contains(episode.getDeviceId())) {
                        episodeRepository.delete(episode);
                    }
                }
            });
        }
        return !devices.isEmpty() && !anySilent;
    }

    private void openEpisode(AttendanceDeviceEntity device, NotificationSettingsEntity settings, LocalDateTime nowUtc) {
        LocalDateTime anchor = device.getLastSeenAt() != null ? device.getLastSeenAt() : device.getCreatedAt();
        tx.executeWithoutResult(status -> {
            // Only the pass that opens the episode raises it; the dedup key (device + when it went silent) is the backstop.
            if (episodeRepository.openIfAbsent(device.getId(), anchor, nowUtc) == 0) {
                return;
            }
            notificationService.raise(
                    NotificationType.TERMINAL_OFFLINE,
                    NotificationMessages.Key.TERMINAL_OFFLINE,
                    Arrays.asList(settings.getDirectorUserIds()),
                    Map.of("device", device.getName(), "time", hotelTime(anchor), NotificationService.PATH, DEVICES_PATH),
                    "TERMINAL_OFFLINE:" + device.getId() + ":" + epoch(anchor));
        });
    }

    private void closeEpisode(AttendanceDeviceEntity device, NotificationSettingsEntity settings) {
        tx.executeWithoutResult(status -> {
            TerminalSilenceEpisodeEntity episode = episodeRepository.findById(device.getId()).orElse(null);
            if (episode == null || episodeRepository.close(device.getId()) == 0) {
                return;
            }
            notificationService.raise(
                    NotificationType.TERMINAL_ONLINE,
                    NotificationMessages.Key.TERMINAL_ONLINE,
                    Arrays.asList(settings.getDirectorUserIds()),
                    Map.of("device", device.getName(), NotificationService.PATH, DEVICES_PATH),
                    "TERMINAL_ONLINE:" + device.getId() + ":" + epoch(episode.getAnchorLastSeenAt()));
        });
    }

    private static long epoch(LocalDateTime utc) {
        return utc == null ? 0 : utc.toEpochSecond(ZoneOffset.UTC);
    }

    private String hotelTime(LocalDateTime utc) {
        return utc == null ? "" : TimestampFormat.inHotelZone(utc, clock).format(HHMM);
    }

    // ---- late / absent --------------------------------------------------------------------------

    void scanRoster(NotificationSettingsEntity settings) {
        if (!settings.isLateEnabled() && !settings.isAbsenceEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();
        try {
            scanArrivals(board, today);
        } catch (RuntimeException e) {
            log.error("Arrival-after-absence scan failed", e);
        }
        for (TodayShiftStatus status : board) {
            // Someone punching with no plan has no start time to be late for.
            if (Boolean.TRUE.equals(status.getUnscheduled()) || !status.getReferenceTime().isPresent()) {
                continue;
            }
            if (status.getState() != TodayShiftState.LATE && status.getState() != TodayShiftState.MISSED) {
                continue;
            }
            LocalDateTime intervalStart = status.getReferenceTime().get().atZoneSameInstant(clock.getZone()).toLocalDateTime();
            long minutes = Duration.between(intervalStart, now).toMinutes();
            int interval = intervalNumber(status, intervalStart.toLocalTime());
            String employeeId = status.getEmployeeUserId();
            String key = employeeId + ":" + today + ":" + interval;
            Map<String, String> params = Map.of(
                    "name", status.getEmployeeName(), "time", intervalStart.format(HHMM), NotificationService.PATH, ATTENDANCE_PATH);
            try {
                // LATE only while the interval is still running - a message "you are late" after it ended is noise (ABSENT covers that).
                if (settings.isLateEnabled() && status.getState() == TodayShiftState.LATE && minutes >= settings.getLateGraceMinutes()) {
                    raiseLate(settings, employeeId, params, key);
                }
                if (settings.isAbsenceEnabled() && minutes >= settings.getAbsenceMinutes()) {
                    List<String> directors = new ArrayList<>(Arrays.asList(settings.getDirectorUserIds()));
                    directors.remove(employeeId);
                    tx.executeWithoutResult(s -> notificationService.raise(
                            NotificationType.ABSENT, NotificationMessages.Key.ABSENT, directors, params, "ABSENT:" + key));
                }
            } catch (RuntimeException e) {
                log.error("Late/absence alert for {} failed", employeeId, e);
            }
        }
    }

    /**
     * Someone the directors were told is absent has now clocked in: the same recipients get one
     * "arrived at HH:mm (N min late)". Nothing is sent if no absence message was ever raised for that
     * interval - the directors never heard they were missing. The arrival is the earliest live IN at or
     * after the interval's planned start (an absence means no punch covered it until now), and its own
     * dedupKey makes a repeated pass silent. Quiet hours apply as for any notification.
     */
    private void scanArrivals(List<TodayShiftStatus> board, LocalDate today) {
        for (TodayShiftStatus status : board) {
            if (Boolean.TRUE.equals(status.getUnscheduled())
                    || (status.getState() != TodayShiftState.ON_SHIFT && status.getState() != TodayShiftState.FINISHED)) {
                continue;
            }
            String employeeId = status.getEmployeeUserId();
            String prefix = "ABSENT:" + employeeId + ":" + today + ":";
            List<NotificationEntity> absences = notificationRepository.findByDedupKeyStartingWith(prefix);
            if (absences.isEmpty()) {
                continue;
            }
            var code = status.getShiftCode();
            List<AttendancePunchEntity> punches = punchRepository
                    .findByPunchAtBetweenAndVoidedAtIsNullOrderByEmployeeUserIdAscPunchAtAsc(today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                    .stream()
                    .filter(p -> p.getEmployeeUserId().equals(employeeId))
                    .toList();
            for (String interval : absences.stream().map(a -> a.getDedupKey().substring(prefix.length())).distinct().toList()) {
                LocalTime plannedStart = null;
                if (code != null) {
                    var field = "2".equals(interval) ? code.getStartTime2() : code.getStartTime1();
                    plannedStart = field.isPresent() ? LocalTime.parse(field.get()) : null;
                }
                if (plannedStart == null) {
                    continue;
                }
                LocalDateTime start = today.atTime(plannedStart);
                var plannedEnd = "2".equals(interval) ? code.getEndTime2() : code.getEndTime1();
                if (!plannedEnd.isPresent()) {
                    continue;
                }
                // "Arrived" is the shared presence rule, not a second reading of the punches.
                var arrival = AttendancePunchPairing.presenceIn(punches, start, today.atTime(LocalTime.parse(plannedEnd.get())), LocalDateTime.now(clock));
                if (arrival.isEmpty()) {
                    continue;
                }
                LocalDateTime at = arrival.get().arrivedAt();
                List<String> recipients = absences.stream()
                        .filter(a -> a.getDedupKey().endsWith(":" + interval))
                        .map(NotificationEntity::getRecipientUserId)
                        .toList();
                Map<String, String> params = Map.of(
                        "name", status.getEmployeeName(),
                        "time", at.format(HHMM),
                        "minutes", String.valueOf(Math.max(0, Duration.between(start, at).toMinutes())),
                        NotificationService.PATH, ATTENDANCE_PATH);
                try {
                    String key = "ARRIVED:" + employeeId + ":" + today + ":" + interval;
                    tx.executeWithoutResult(s -> notificationService.raise(
                            NotificationType.ABSENT, NotificationMessages.Key.ARRIVED_AFTER_ABSENT, recipients, params, key));
                } catch (RuntimeException e) {
                    log.error("Arrival message for {} failed", employeeId, e);
                }
            }
        }
    }

    private void raiseLate(NotificationSettingsEntity settings, String employeeId, Map<String, String> params, String key) {
        UserEntity employee = userRepository.findById(employeeId).orElse(null);
        if (employee == null || !employee.isActive()) {
            return;
        }
        List<String> others = new ArrayList<>(Arrays.asList(settings.getLateRecipientUserIds()));
        others.remove(employeeId);
        tx.executeWithoutResult(s -> {
            // One dedup key, two wordings: the person is told "you are late", the others "X is late".
            notificationService.raise(NotificationType.LATE, NotificationMessages.Key.LATE_SELF, List.of(employeeId), params, "LATE:" + key);
            notificationService.raise(NotificationType.LATE, NotificationMessages.Key.LATE_STAFF, others, params, "LATE:" + key);
        });
    }

    /** 1 or 2: which of the shift code's intervals this start time belongs to. */
    private static int intervalNumber(TodayShiftStatus status, LocalTime start) {
        var code = status.getShiftCode();
        if (code != null && code.getStartTime2().isPresent() && LocalTime.parse(code.getStartTime2().get()).equals(start)) {
            return 2;
        }
        return 1;
    }
}
