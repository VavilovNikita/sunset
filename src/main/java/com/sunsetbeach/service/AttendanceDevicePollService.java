package com.sunsetbeach.service;

import com.sunsetbeach.attendance.AttendanceDeviceException;
import com.sunsetbeach.attendance.RawAttendancePunch;
import com.sunsetbeach.attendance.TerminalPollResult;
import com.sunsetbeach.attendance.ZkTerminalClient;
import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AttendancePunchRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reuses {@code PrintService}'s own {@code @Scheduled} background-sweep shape (a fixed-delay job
 * over a small filtered query, each unit of work independent) rather than introducing a second
 * scheduling mechanism - see {@code BookingExpiryService}'s own javadoc, which made the same
 * choice for the same reason: a durable row that needs periodic follow-up with no request driving
 * it. This is the one thing above {@link ZkTerminalClient} that talks to the seam directly -
 * everything in this class is exercised in tests against a fake {@code ZkTerminalClient}, with no
 * real hardware or even a real socket involved.
 *
 * <p><b>Why not wrap the whole sweep in one {@code @Transactional}</b>, unlike {@code
 * PrintService#retryPendingJobs}: that method's own per-job network call (a local ESC/POS send) is
 * fast enough that holding a DB transaction open for it is a reasonable trade. A poll's network
 * call is a real TCP round trip to a device that might be slow or unreachable, with a ten-second
 * timeout - holding one DB connection open across that, times however many devices exist, is a
 * cost the print queue never pays. Each device's own DB work ({@link #markSeen}, {@link
 * AttendanceService#ingestDevicePunch}) is its own short transaction instead, called from this
 * (deliberately non-transactional) sweep method.
 *
 * <p><b>Poll interval: 5 minutes by default</b> ({@code app.attendance.device-poll-interval-ms}).
 * Attendance isn't real-time - nobody is blocked waiting on a punch to register - but a manager
 * checking "who's actually here" shouldn't be looking at hours-old data either, and setting the
 * device's own clock (see {@link ZkTerminalClient#poll}) more often than that buys little: clock
 * drift on a small embedded device accumulates over days, not minutes. Five minutes sits between
 * {@code PrintService}'s 60 seconds (a guest is plausibly waiting on that one) and {@code
 * BookingExpiryService}'s 15 minutes (a background administrative sweep with no one watching).
 *
 * <p><b>A windowed read, not a full read, every five minutes.</b> A full read was the original,
 * simpler design - safe, but wasteful at scale: at this hotel's ~2,000 punches/month, a device
 * polled every five minutes (288 times a day) would be dragging the *entire* growing history off
 * a small embedded box on every single poll, worse every month the log never gets cleared (see
 * {@code ZkTerminalClientImpl}'s own javadoc for why it never is). {@link #sinceWatermark} instead
 * reads from just past the newest punch already ingested from that device
 * ({@link AttendancePunchRepository#findMaxPunchAtByDeviceId}), overlapping backward by {@link
 * #WATERMARK_OVERLAP} rather than starting exactly at it - see that field's own javadoc for why an
 * exact cutoff isn't safe. Phase 1's idempotent ingestion ({@code (deviceId, enrollmentNumber,
 * punchAt)}) is what makes the overlap free: anything the window re-reads that's already been
 * ingested lands as a no-op {@code DUPLICATE}, not a second row.
 *
 * <p><b>The first poll of a device that's never been read is a full read</b> - there is no
 * watermark to window from yet ({@link #sinceWatermark} returns {@code null} until at least one
 * punch has been ingested from that device), so {@link ZkTerminalClient#poll} is called with
 * {@code since=null}, which also detects and persists {@code AttendanceDeviceEntity
 * #attendanceRecordSize} for every windowed read after. The same full read is available on demand
 * afterward too - {@link #resyncNow} forces one regardless of any existing watermark, for a
 * device an operator suspects has drifted out of what its normal window would ever see again (a
 * factory reset, or a clock that jumped backward hard enough that new records now fall before the
 * watermark rather than after it - the overlap absorbs ordinary jitter, not that). Automatic
 * detection of that specific failure mode isn't attempted here: {@code lastSeenAt} keeps updating
 * normally even when a device's clock is wrong, since it records when *we* successfully polled,
 * not what the device's own clock said - so this is a case for an operator who notices something
 * looks off to resolve with a manual resync, not something the sweep silently self-heals.
 *
 * <p><b>A device that doesn't support windowed reads says so, visibly.</b> {@link #markSeen}
 * persists {@code result.windowedReadUnsupported()} onto {@code AttendanceDeviceEntity
 * #windowedReadUnsupported} whenever a poll actually tested the windowed command - {@code true} if
 * the device rejected it and this poll fell back to a full read, {@code false} if the device
 * honored it, left untouched (never guessed) when this poll never attempted a windowed read at
 * all. Without this, a K60 that turns out not to support {@code CMD_ATTLOG_TIME_RRQ} would read
 * its entire, ever-growing log every five minutes for as long as it's deployed, and nothing would
 * ever say why - exactly the cost this whole windowing feature exists to remove, reintroduced
 * silently. The devices screen shows it next to {@code lastSeenAt} for exactly that reason.
 */
@Service
public class AttendanceDevicePollService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceDevicePollService.class);

    /**
     * How far past the newest already-ingested punch a windowed read starts from, instead of
     * starting exactly at the watermark. Generous relative to the jitter it exists to absorb -
     * the device's clock is corrected on every successful poll (see {@link ZkTerminalClient#poll}),
     * so between two successful polls drift is bounded to whatever a cheap RTC accumulates in five
     * minutes (negligible); a record written slightly out of chronological order near a poll
     * boundary is the realistic case this covers, not a long outage - an outage of any length is
     * already safe on its own, because every punch recorded during it still gets a timestamp after
     * the last watermark and before "now", both inside the window regardless of how long the gap
     * was. An hour is a few punches out of roughly two thousand a month - re-checking that many
     * for a duplicate is free next to what a full read every five minutes would cost.
     */
    static final Duration WATERMARK_OVERLAP = Duration.ofHours(1);

    private final AttendanceDeviceRepository attendanceDeviceRepository;
    private final AttendancePunchRepository attendancePunchRepository;
    private final AttendanceService attendanceService;
    private final ZkTerminalClient zkTerminalClient;
    private final Duration silenceWarningThreshold;

    public AttendanceDevicePollService(
            AttendanceDeviceRepository attendanceDeviceRepository,
            AttendancePunchRepository attendancePunchRepository,
            AttendanceService attendanceService,
            ZkTerminalClient zkTerminalClient,
            @Value("${app.attendance.device-silence-warning-hours:24}") long silenceWarningHours) {
        this.attendanceDeviceRepository = attendanceDeviceRepository;
        this.attendancePunchRepository = attendancePunchRepository;
        this.attendanceService = attendanceService;
        this.zkTerminalClient = zkTerminalClient;
        this.silenceWarningThreshold = Duration.ofHours(silenceWarningHours);
    }

    /**
     * A failed poll is normal - the thing is on a wall in a hotel - so a single unreachable or
     * misbehaving device is caught and logged here, never allowed to stop the sweep for every
     * other device, and never raised as something a person has to dismiss. What *is* raised (at
     * WARN, so ordinary log-based monitoring picks it up) is a device that has gone unusually long
     * without a successful poll - see {@link #warnIfLongSilence} - because that silence is
     * otherwise indistinguishable, in the attendance data itself, from a stretch where genuinely
     * nobody worked.
     */
    @Scheduled(fixedDelayString = "${app.attendance.device-poll-interval-ms:300000}")
    public void pollDevices() {
        for (AttendanceDeviceEntity device : attendanceDeviceRepository.findByActiveTrue()) {
            pollOneDevice(device, false);
        }
    }

    /**
     * Forces a full read of one device regardless of any existing watermark - the "manual
     * re-sync" this class's own javadoc describes, for an operator who suspects a device has
     * drifted somewhere its normal windowed poll can no longer see (see {@code
     * AttendanceDeviceController}). Runs synchronously and returns the (possibly unchanged, if the
     * poll failed) device - a failed resync is reported the same "didn't work this time" way a
     * failed scheduled poll is, not as a request error, matching {@code PrinterService#testPrint}'s
     * own "attempt now, report what actually happened" convention.
     */
    public AttendanceDeviceEntity resyncNow(String deviceId) {
        AttendanceDeviceEntity device = attendanceDeviceRepository.findById(deviceId).orElseThrow(() -> new NotFoundException("Device not found"));
        pollOneDevice(device, true);
        return device;
    }

    private void pollOneDevice(AttendanceDeviceEntity device, boolean forceFullRead) {
        LocalDateTime since = forceFullRead ? null : sinceWatermark(device);
        TerminalPollResult result;
        try {
            result = zkTerminalClient.poll(device, since, device.getAttendanceRecordSize());
        } catch (AttendanceDeviceException e) {
            log.warn("Poll failed for device {} ({}): {}", device.getName(), device.getId(), e.getMessage());
            warnIfLongSilence(device);
            return;
        }

        markSeen(device, result.recordSize(), result.windowedReadUnsupported());

        // Sorted so a per-employee prior-count parity and the debounce check both see this
        // employee's punches in true chronological order, even if the device (or a windowed-read
        // merge across a poll boundary) ever returns records slightly out of order across
        // employees - see AttendanceService#ingestDevicePunch's own javadoc.
        List<RawAttendancePunch> punches = result.punches().stream().sorted(Comparator.comparing(RawAttendancePunch::deviceTimestamp)).toList();

        int ingested = 0;
        int duplicate = 0;
        int unknown = 0;
        int ignoredDoubleScan = 0;
        for (RawAttendancePunch punch : punches) {
            try {
                DeviceIngestResult ingestResult =
                        attendanceService.ingestDevicePunch(device, punch.enrollmentNumber(), punch.deviceTimestamp(), punch.direction());
                switch (ingestResult) {
                    case INGESTED -> ingested++;
                    case DUPLICATE -> duplicate++;
                    case UNKNOWN_ENROLLMENT_NUMBER -> unknown++;
                    case IGNORED_DUPLICATE_SCAN -> ignoredDoubleScan++;
                }
            } catch (RuntimeException e) {
                // One bad record must not lose the rest of an otherwise-good batch - same
                // independence PrintService's own sweep already gives each job.
                log.error(
                        "Failed to ingest a punch from device {} (enrollment {}, at {}): {}", device.getName(), punch.enrollmentNumber(),
                        punch.deviceTimestamp(), e.getMessage(), e);
            }
        }
        if (unknown > 0) {
            log.warn("Device {} reported {} punch(es) whose enrollment number matches no employee", device.getName(), unknown);
        }
        log.info(
                "Polled device {} ({}): {} new punch(es), {} already seen, {} unattributable, {} ignored as likely double-scans", device.getName(),
                since == null ? "full read" : "windowed from " + since, ingested, duplicate, unknown, ignoredDoubleScan);
    }

    /**
     * Null (full read) until this device has ingested at least one punch - see this class's own
     * javadoc. Deliberately re-derived from what's actually stored every poll, rather than a
     * separately maintained field, so it can never drift from the data it describes.
     */
    private LocalDateTime sinceWatermark(AttendanceDeviceEntity device) {
        return attendancePunchRepository.findMaxPunchAtByDeviceId(device.getId()).map(watermark -> watermark.minus(WATERMARK_OVERLAP)).orElse(null);
    }

    @Transactional
    void markSeen(AttendanceDeviceEntity device, Integer recordSize, Boolean windowedReadUnsupported) {
        device.setLastSeenAt(LocalDateTime.now());
        if (recordSize != null) {
            device.setAttendanceRecordSize(recordSize);
        }
        // Null means this poll never actually tested the windowed command (no watermark yet, or
        // an empty log) - leave whatever the last real test found alone rather than guess. See
        // TerminalPollResult#windowedReadUnsupported's own javadoc.
        if (windowedReadUnsupported != null) {
            device.setWindowedReadUnsupported(windowedReadUnsupported);
        }
        attendanceDeviceRepository.save(device);
    }

    /**
     * A device that keeps failing to poll produces no punches at all - and a month with no
     * punches looks, in the data alone, exactly like a month in which nobody worked. This is the
     * one signal that tells the difference: logged at WARN specifically so it reaches whatever
     * log-based monitoring already exists, without this codebase inventing a new alerting path
     * for one warning.
     */
    private void warnIfLongSilence(AttendanceDeviceEntity device) {
        LocalDateTime lastSeen = device.getLastSeenAt();
        if (lastSeen == null) {
            return;
        }
        Duration silence = Duration.between(lastSeen, LocalDateTime.now());
        if (silence.compareTo(silenceWarningThreshold) >= 0) {
            log.warn(
                    "Device {} ({}) has not been reached in {} hours - a silence this long is indistinguishable, in the attendance data "
                            + "alone, from a stretch where nobody worked. Check the device and the network path to it.",
                    device.getName(), device.getId(), silence.toHours());
        }
    }
}
