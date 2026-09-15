package com.sunsetbeach.service;

import com.sunsetbeach.attendance.AttendanceDeviceException;
import com.sunsetbeach.attendance.RawAttendancePunch;
import com.sunsetbeach.attendance.ZkTerminalClient;
import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import java.time.Duration;
import java.time.LocalDateTime;
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
 * <p><b>Full read every time, not a watermark.</b> The ZK protocol's own attendance-log read has
 * no "since a timestamp" parameter - a poll always gets the device's *entire* stored log, and
 * with the log never cleared (see {@code ZkTerminalClientImpl}'s own javadoc), that log only
 * grows. A client-side watermark (skip anything at or before the newest punch this device has
 * already given us) would cut the number of {@link AttendanceService#ingestDevicePunch} calls
 * each poll makes, but it would have to get the *ordering* question right to be safe - and this is
 * exactly where a watermark breaks: a device that gets factory-reset (or physically swapped for a
 * replacement re-registered under the same row) can restart its own clock or record numbering
 * from a point *behind* the watermark, and a record that arrives out of chronological order (this
 * protocol doesn't guarantee delivery order) can sit *before* the watermark's own cutoff despite
 * being genuinely new. Either would make a watermark silently skip real punches - precisely the
 * "looks exactly like nobody worked" failure this whole feature exists to prevent. Reading
 * everything and leaning on Phase 1's own idempotent ingestion (the {@code (deviceId,
 * enrollmentNumber, punchAt)} unique triple) has none of those failure modes: every record is
 * checked on its own terms, every time, so a reset device or a late-arriving record is simply
 * ingested (or recognised as already-ingested) correctly, with no special-case recovery logic
 * anywhere. The cost - re-checking the device's full history on every poll - is small at this
 * hotel's actual volume (low thousands of rows even after the ~2-year retention this hardware
 * holds), and buying correctness with it is the right trade for payroll data.
 */
@Service
public class AttendanceDevicePollService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceDevicePollService.class);

    private final AttendanceDeviceRepository attendanceDeviceRepository;
    private final AttendanceService attendanceService;
    private final ZkTerminalClient zkTerminalClient;
    private final Duration silenceWarningThreshold;

    public AttendanceDevicePollService(
            AttendanceDeviceRepository attendanceDeviceRepository,
            AttendanceService attendanceService,
            ZkTerminalClient zkTerminalClient,
            @Value("${app.attendance.device-silence-warning-hours:24}") long silenceWarningHours) {
        this.attendanceDeviceRepository = attendanceDeviceRepository;
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
            pollOneDevice(device);
        }
    }

    private void pollOneDevice(AttendanceDeviceEntity device) {
        List<RawAttendancePunch> punches;
        try {
            punches = zkTerminalClient.poll(device);
        } catch (AttendanceDeviceException e) {
            log.warn("Poll failed for device {} ({}): {}", device.getName(), device.getId(), e.getMessage());
            warnIfLongSilence(device);
            return;
        }

        markSeen(device);

        int ingested = 0;
        int duplicate = 0;
        int unknown = 0;
        for (RawAttendancePunch punch : punches) {
            try {
                DeviceIngestResult result =
                        attendanceService.ingestDevicePunch(device, punch.enrollmentNumber(), punch.deviceTimestamp(), punch.direction());
                switch (result) {
                    case INGESTED -> ingested++;
                    case DUPLICATE -> duplicate++;
                    case UNKNOWN_ENROLLMENT_NUMBER -> unknown++;
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
        log.info("Polled device {}: {} new punch(es), {} already seen, {} unattributable", device.getName(), ingested, duplicate, unknown);
    }

    @Transactional
    void markSeen(AttendanceDeviceEntity device) {
        device.setLastSeenAt(LocalDateTime.now());
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
