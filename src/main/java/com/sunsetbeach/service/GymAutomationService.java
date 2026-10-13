package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GymDeviceEntity;
import com.sunsetbeach.entity.GymHallSettingsEntity;
import com.sunsetbeach.entity.GymReservationActionEntity;
import com.sunsetbeach.entity.GymReservationEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.GymAcCommandInput;
import com.sunsetbeach.model.GymDeviceType;
import com.sunsetbeach.model.GymReservationStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.GymDeviceRepository;
import com.sunsetbeach.repository.GymReservationActionRepository;
import com.sunsetbeach.repository.GymReservationRepository;
import com.sunsetbeach.tuya.TuyaException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Switches the gym equipment around reservations: every device that takes part in bookings goes
 * on {@code leadMinutes} before a session starts and off when it ends, unless the next session
 * begins within {@code leadMinutes} - then it stays on. Air conditioners get the one scene command
 * from {@link GymHallSettingsEntity} (cool, 16 degrees, strongest fan by default) on switch-on,
 * the same setting with power 0 on switch-off.
 *
 * <p>Same shape as the other periodic work here: a plain {@code @Scheduled} fixed-delay sweep
 * ({@link #sweep}), not a job queue. Its memory is the {@code GymReservationAction} table - one
 * row per reservation, device and phase - so:
 * <ul>
 *   <li><b>No duplicates, restart-safe.</b> A command is sent only for a row that is still
 *       {@code PENDING}; a sent one is {@code OK} and never touched again. A backend restart in the
 *       middle of a session just reads the table. (A crash between Tuya accepting a command and the
 *       row being saved can repeat that one command; on/off and the scene are idempotent.)
 *   <li><b>Catching up.</b> A transition is attempted whenever the sweep finds it due and still
 *       relevant: a switch-on is sent late if the session has not ended yet, never after
 *       ({@code MISSED}); a switch-off is always sent.
 *   <li><b>One device failing does not stop the others.</b> Each device is its own row with its own
 *       attempt counter. A failed attempt is retried on the next sweep (about a minute later, see
 *       {@code retry-pause-seconds}) up to {@code max-attempts} (3) times, then the row is {@code
 *       FAILED}. The outcome per device is kept on the row and one system audit entry per phase
 *       says what went on and what did not.
 *   <li><b>No polling, no fighting with hands.</b> Nothing here reads device state (the Tuya quota
 *       is finite) and commands go out only at the transitions above - a fan someone switches off
 *       by hand mid-session stays off until the next transition.
 * </ul>
 *
 * <p>Chains of sessions: a session whose switch-on moment falls inside the previous session's
 * window finds the equipment already on ({@code COVERED_BY_PREVIOUS}) and sends nothing; the
 * previous session's switch-off then sees the next one coming ({@code NEXT_BOOKING_SOON}) and
 * sends nothing either. A cancelled session that had been switched on is switched off (or kept
 * on, if another session needs it). Deliberately not {@code @Transactional}: a Tuya call is a
 * network round trip and must not hold a database transaction open.
 */
@Service
public class GymAutomationService {

    private static final Logger log = LoggerFactory.getLogger(GymAutomationService.class);
    private static final String SENT_BY = "Gym reservation automation";

    private final GymReservationRepository reservations;
    private final GymReservationActionRepository actions;
    private final GymDeviceRepository devices;
    private final GymDeviceService deviceService;
    private final GymHallSettingsService hallSettings;
    private final BookingRepository bookings;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final int maxAttempts;
    private final long retryPauseSeconds;

    public GymAutomationService(
            GymReservationRepository reservations,
            GymReservationActionRepository actions,
            GymDeviceRepository devices,
            GymDeviceService deviceService,
            GymHallSettingsService hallSettings,
            BookingRepository bookings,
            AuditLogService auditLogService,
            Clock clock,
            @Value("${app.gym.automation.max-attempts:3}") int maxAttempts,
            @Value("${app.gym.automation.retry-pause-seconds:45}") long retryPauseSeconds) {
        this.reservations = reservations;
        this.actions = actions;
        this.devices = devices;
        this.deviceService = deviceService;
        this.hallSettings = hallSettings;
        this.bookings = bookings;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryPauseSeconds = Math.max(0, retryPauseSeconds);
    }

    /** The scheduled entry point; a failure of one run must never stop the next. */
    @Scheduled(fixedDelayString = "${app.gym.automation.interval-ms:60000}")
    public void sweep() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            log.error("Gym automation sweep failed", e);
        }
    }

    /** One pass over the unfinished reservations: switch-ons first, then switch-offs, then bookkeeping. */
    public void runOnce() {
        LocalDateTime now = LocalDateTime.now(clock);
        // A reservation that ended long ago is still unfinished if the backend was down for it, so there is no lower bound.
        List<String> ids = reservations
                .findByAutomationDoneFalseAndDateLessThanEqualOrderByDateAscStartTimeAsc(now.toLocalDate().plusDays(1))
                .stream()
                .map(GymReservationEntity::getId)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        GymHallSettingsEntity settings = hallSettings.entity();
        // On before off: when a session ends exactly as the next one's switch-on becomes due, the
        // switch-off has to find that next session already registered (and keep the equipment on).
        for (String id : ids) {
            guarded(id, "switch-on", () -> processOn(id, now, settings));
        }
        for (String id : ids) {
            guarded(id, "switch-off", () -> processOff(id, now, settings));
        }
        for (String id : ids) {
            guarded(id, "bookkeeping", () -> finish(id, now));
        }
    }

    private void guarded(String reservationId, String what, Runnable work) {
        try {
            work.run();
        } catch (RuntimeException e) {
            log.error("Gym automation {} failed for reservation {}", what, reservationId, e);
        }
    }

    // ---- switch-on --------------------------------------------------------------------------

    private void processOn(String id, LocalDateTime now, GymHallSettingsEntity settings) {
        GymReservationEntity r = reservations.findById(id).orElse(null);
        if (r == null) {
            return;
        }
        List<GymReservationActionEntity> rows = actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(id, GymReservationActionEntity.ON);
        if (r.getStatus() == GymReservationStatus.CANCELLED) {
            // Cancelled: whatever was still being retried is moot. What already went out is switched off by processOff.
            for (GymReservationActionEntity row : rows) {
                if (row.isPending()) {
                    skip(row, GymReservationActionEntity.CANCELLED, now);
                }
            }
            return;
        }
        int lead = settings.getLeadMinutes();
        if (rows.isEmpty()) {
            if (now.isBefore(r.start().minusMinutes(lead))) {
                return;
            }
            List<GymDeviceEntity> participating = devices.findByActiveTrueAndInBookingsTrueOrderBySortOrderAscNameAsc();
            if (participating.isEmpty()) {
                return;
            }
            if (!now.isBefore(r.end())) {
                // The session is over and nothing was sent (backend down, or booked at the last minute): do not switch on now.
                rows = createRows(r, GymReservationActionEntity.ON, participating, GymReservationActionEntity.SKIPPED, GymReservationActionEntity.MISSED, now);
                auditPhase(r, GymReservationActionEntity.ON, rows);
                return;
            }
            rows = createRows(r, GymReservationActionEntity.ON, participating, GymReservationActionEntity.PENDING, null, now);
            GymReservationEntity previous = coveringPredecessor(r, lead);
            if (previous != null) {
                List<GymReservationActionEntity> previousOn = actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(previous.getId(), GymReservationActionEntity.ON);
                for (GymReservationActionEntity row : rows) {
                    boolean alreadyOn = previousOn.stream().anyMatch(p -> row.getDeviceId() != null && row.getDeviceId().equals(p.getDeviceId())
                            && (GymReservationActionEntity.OK.equals(p.getStatus())
                                    || (GymReservationActionEntity.SKIPPED.equals(p.getStatus())
                                            && GymReservationActionEntity.COVERED_BY_PREVIOUS.equals(p.getSkipReason()))));
                    if (alreadyOn) {
                        skip(row, GymReservationActionEntity.COVERED_BY_PREVIOUS, now);
                    }
                }
            }
            rows = actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(id, GymReservationActionEntity.ON);
        }

        boolean hadPending = rows.stream().anyMatch(GymReservationActionEntity::isPending);
        for (GymReservationActionEntity row : rows) {
            if (!row.isPending()) {
                continue;
            }
            if (!now.isBefore(r.end())) {
                skip(row, GymReservationActionEntity.MISSED, now);
                continue;
            }
            attempt(row, true, settings);
        }
        if (hadPending) {
            List<GymReservationActionEntity> after = actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(id, GymReservationActionEntity.ON);
            if (after.stream().noneMatch(GymReservationActionEntity::isPending)) {
                auditPhase(r, GymReservationActionEntity.ON, after);
            }
        }
    }

    /**
     * The earlier reservation whose equipment-on window still reaches this one's switch-on moment
     * and has not started switching off - the one that has the equipment on for us.
     */
    private GymReservationEntity coveringPredecessor(GymReservationEntity r, int lead) {
        LocalDateTime onAt = r.start().minusMinutes(lead);
        return reservations
                .findByDateBetweenAndStatusInOrderByDateAscStartTimeAsc(
                        r.getDate().minusDays(1), r.getDate(), List.of(GymReservationStatus.BOOKED, GymReservationStatus.COMPLETED))
                .stream()
                .filter(p -> !p.getId().equals(r.getId()) && p.start().isBefore(r.start()) && !p.end().isBefore(onAt))
                .filter(p -> actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(p.getId(), GymReservationActionEntity.OFF).stream()
                        .allMatch(o -> GymReservationActionEntity.SKIPPED.equals(o.getStatus())))
                .max(Comparator.comparing(GymReservationEntity::start))
                .orElse(null);
    }

    // ---- switch-off -------------------------------------------------------------------------

    private void processOff(String id, LocalDateTime now, GymHallSettingsEntity settings) {
        GymReservationEntity r = reservations.findById(id).orElse(null);
        if (r == null) {
            return;
        }
        boolean cancelled = r.getStatus() == GymReservationStatus.CANCELLED;
        if (!cancelled && now.isBefore(r.end())) {
            return;
        }
        List<GymReservationActionEntity> onRows = actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(id, GymReservationActionEntity.ON);
        if (onRows.stream().anyMatch(GymReservationActionEntity::isPending)) {
            return;
        }
        List<GymReservationActionEntity> wasOn = onRows.stream().filter(GymReservationActionEntity::isEffectivelyOn).toList();
        if (wasOn.isEmpty()) {
            return;
        }
        int lead = settings.getLeadMinutes();
        List<GymReservationActionEntity> offRows = actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(id, GymReservationActionEntity.OFF);
        if (offRows.isEmpty()) {
            if (needsEquipmentOn(r, now, lead)) {
                createOffRows(r, wasOn, true, now);
                auditPhase(r, GymReservationActionEntity.OFF, actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(id, GymReservationActionEntity.OFF));
                return;
            }
            createOffRows(r, wasOn, false, now);
            offRows = actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(id, GymReservationActionEntity.OFF);
        }

        boolean hadPending = offRows.stream().anyMatch(GymReservationActionEntity::isPending);
        for (GymReservationActionEntity row : offRows) {
            if (!row.isPending()) {
                continue;
            }
            // A retry made moot: another session has meanwhile come within reach and needs the equipment on.
            if (needsEquipmentOn(r, now, lead)) {
                skip(row, GymReservationActionEntity.NEXT_BOOKING_SOON, now);
                continue;
            }
            attempt(row, false, settings);
        }
        if (hadPending) {
            List<GymReservationActionEntity> after = actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(id, GymReservationActionEntity.OFF);
            if (after.stream().noneMatch(GymReservationActionEntity::isPending)) {
                auditPhase(r, GymReservationActionEntity.OFF, after);
            }
        }
    }

    /**
     * Another session that has not ended needs the equipment on: it starts within the lead time of
     * this one's end (of "now", for a cancellation), or its own switch-on has already gone out -
     * switching off now would be undone straight away, or would cut its warm-up short.
     */
    private boolean needsEquipmentOn(GymReservationEntity r, LocalDateTime now, int lead) {
        LocalDateTime reference = r.getStatus() == GymReservationStatus.CANCELLED ? now : r.end();
        return reservations
                .findByDateBetweenAndStatusInOrderByDateAscStartTimeAsc(
                        now.toLocalDate().minusDays(1), now.toLocalDate().plusDays(1),
                        List.of(GymReservationStatus.BOOKED, GymReservationStatus.COMPLETED))
                .stream()
                .filter(q -> !q.getId().equals(r.getId()) && q.end().isAfter(now))
                .anyMatch(q -> !q.start().isAfter(reference.plusMinutes(lead))
                        || actions.existsByReservationIdAndPhase(q.getId(), GymReservationActionEntity.ON));
    }

    private void createOffRows(GymReservationEntity r, List<GymReservationActionEntity> wasOn, boolean keepOn, LocalDateTime now) {
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        for (GymReservationActionEntity on : wasOn) {
            GymReservationActionEntity row = newRow(r.getId(), GymReservationActionEntity.OFF, on.getDeviceId(), on.getDeviceName());
            if (keepOn) {
                row.setStatus(GymReservationActionEntity.SKIPPED);
                row.setSkipReason(GymReservationActionEntity.NEXT_BOOKING_SOON);
                row.setCompletedAt(nowUtc);
            } else if (on.getDeviceId() == null) {
                row.setStatus(GymReservationActionEntity.SKIPPED);
                row.setSkipReason(GymReservationActionEntity.DEVICE_REMOVED);
                row.setCompletedAt(nowUtc);
            }
            saveRow(row);
        }
    }

    // ---- sending ----------------------------------------------------------------------------

    /** One attempt for a pending row; the row ends {@code OK}, stays {@code PENDING} for the next sweep, or turns {@code FAILED} at the limit. */
    private void attempt(GymReservationActionEntity row, boolean on, GymHallSettingsEntity settings) {
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        if (row.getLastAttemptAt() != null && Duration.between(row.getLastAttemptAt(), nowUtc).getSeconds() < retryPauseSeconds) {
            return;
        }
        GymDeviceEntity device = row.getDeviceId() == null ? null : devices.findById(row.getDeviceId()).orElse(null);
        if (device == null || !device.isActive()) {
            skip(row, GymReservationActionEntity.DEVICE_REMOVED, LocalDateTime.now(clock));
            return;
        }
        row.setAttempts(row.getAttempts() + 1);
        row.setLastAttemptAt(nowUtc);
        try {
            if (GymDeviceType.RELAY.getValue().equals(device.getType())) {
                deviceService.commandRelayForAutomation(device, on);
            } else {
                deviceService.commandAcForAutomation(
                        device,
                        new GymAcCommandInput(on ? 1 : 0, settings.getAcMode(), settings.getAcTemp(), settings.getAcWind()),
                        SENT_BY);
            }
            row.setStatus(GymReservationActionEntity.OK);
            row.setLastError(null);
            row.setCompletedAt(nowUtc);
        } catch (RuntimeException e) {
            String message = e instanceof TuyaException || e instanceof com.sunsetbeach.error.ConflictException
                    ? e.getMessage()
                    : "Unexpected error: " + e.getClass().getSimpleName();
            if (!(e instanceof TuyaException)) {
                log.warn("Gym automation could not send to \"{}\"", device.getName(), e);
            }
            row.setLastError(message);
            if (row.getAttempts() >= maxAttempts) {
                row.setStatus(GymReservationActionEntity.FAILED);
                row.setCompletedAt(nowUtc);
            }
        }
        actions.save(row);
    }

    private void skip(GymReservationActionEntity row, String reason, LocalDateTime ignored) {
        row.setStatus(GymReservationActionEntity.SKIPPED);
        row.setSkipReason(reason);
        row.setCompletedAt(TimestampFormat.nowUtc(clock));
        actions.save(row);
    }

    // ---- rows -------------------------------------------------------------------------------

    private List<GymReservationActionEntity> createRows(
            GymReservationEntity r, String phase, List<GymDeviceEntity> targets, String status, String skipReason, LocalDateTime now) {
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        for (GymDeviceEntity device : targets) {
            GymReservationActionEntity row = newRow(r.getId(), phase, device.getId(), device.getName());
            row.setStatus(status);
            row.setSkipReason(skipReason);
            if (!GymReservationActionEntity.PENDING.equals(status)) {
                row.setCompletedAt(nowUtc);
            }
            saveRow(row);
        }
        return actions.findByReservationIdAndPhaseOrderByDeviceNameAsc(r.getId(), phase);
    }

    private GymReservationActionEntity newRow(String reservationId, String phase, String deviceId, String deviceName) {
        GymReservationActionEntity row = new GymReservationActionEntity();
        row.setReservationId(reservationId);
        row.setPhase(phase);
        row.setDeviceId(deviceId);
        row.setDeviceName(deviceName);
        return row;
    }

    /** The unique index is the guard against two runs creating the same row: the loser just moves on. */
    private void saveRow(GymReservationActionEntity row) {
        try {
            actions.saveAndFlush(row);
        } catch (DataIntegrityViolationException e) {
            log.debug("Gym automation row for reservation {} / {} already exists", row.getReservationId(), row.getDeviceName());
        }
    }

    // ---- bookkeeping ------------------------------------------------------------------------

    private void finish(String id, LocalDateTime now) {
        GymReservationEntity r = reservations.findById(id).orElse(null);
        if (r == null || r.isAutomationDone()) {
            return;
        }
        if (r.getStatus() == GymReservationStatus.BOOKED && !now.isBefore(r.end())) {
            reservations.changeStatus(id, GymReservationStatus.BOOKED, GymReservationStatus.COMPLETED);
        }
        boolean over = r.getStatus() == GymReservationStatus.CANCELLED || !now.isBefore(r.end());
        if (!over) {
            return;
        }
        List<GymReservationActionEntity> rows = actions.findByReservationIdIn(List.of(id));
        if (rows.stream().anyMatch(GymReservationActionEntity::isPending)) {
            return;
        }
        boolean wasOn = rows.stream().anyMatch(a -> GymReservationActionEntity.ON.equals(a.getPhase()) && a.isEffectivelyOn());
        boolean switchedOff = rows.stream().anyMatch(a -> GymReservationActionEntity.OFF.equals(a.getPhase()));
        if (wasOn && !switchedOff) {
            return;
        }
        reservations.markAutomationDone(id);
    }

    // ---- audit ------------------------------------------------------------------------------

    private void auditPhase(GymReservationEntity r, String phase, List<GymReservationActionEntity> rows) {
        String guest = bookings.findById(r.getBookingId()).map(BookingEntity::getGuestName).orElse("a guest");
        String session = guest + "'s gym session " + r.getDate() + " " + r.getStartTime().format(GymHallSettingsService.HHMM)
                + "–" + r.end().toLocalTime().format(GymHallSettingsService.HHMM);
        boolean on = GymReservationActionEntity.ON.equals(phase);
        List<String> ok = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (GymReservationActionEntity row : rows) {
            switch (row.getStatus()) {
                case GymReservationActionEntity.OK -> ok.add(row.getDeviceName());
                case GymReservationActionEntity.FAILED -> failed.add(row.getDeviceName() + " (" + row.getLastError() + ")");
                default -> skipped.add(row.getDeviceName() + " (" + describeSkip(row.getSkipReason()) + ")");
            }
        }
        StringBuilder summary = new StringBuilder("Equipment ").append(on ? "switch-on" : "switch-off").append(" for ").append(session).append(": ");
        summary.append(on ? "switched on " : "switched off ").append(ok.size()).append(" of ").append(rows.size());
        if (!ok.isEmpty()) {
            summary.append(" [").append(String.join(", ", ok)).append("]");
        }
        if (!failed.isEmpty()) {
            summary.append("; FAILED: ").append(String.join("; ", failed));
        }
        if (!skipped.isEmpty()) {
            summary.append("; not sent: ").append(skipped.stream().collect(Collectors.joining("; ")));
        }
        if (!failed.isEmpty()) {
            log.warn(summary.toString());
        }
        auditLogService.recordSystemAction(AuditAction.GYM_AUTOMATION_RESULT, AuditEntityType.GYM_RESERVATION, r.getId(), summary.toString());
    }

    private static String describeSkip(String reason) {
        if (reason == null) {
            return "skipped";
        }
        return switch (reason) {
            case GymReservationActionEntity.COVERED_BY_PREVIOUS -> "already on from the previous session";
            case GymReservationActionEntity.NEXT_BOOKING_SOON -> "kept on for the next session";
            case GymReservationActionEntity.MISSED -> "session was over before it could be switched on";
            case GymReservationActionEntity.CANCELLED -> "session cancelled";
            case GymReservationActionEntity.DEVICE_REMOVED -> "device removed or disabled";
            default -> reason;
        };
    }
}
