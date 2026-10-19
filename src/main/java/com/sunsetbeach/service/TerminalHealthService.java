package com.sunsetbeach.service;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Whether the attendance terminals can be trusted right now - the one definition of "silent" that both the
 * late/absence scanner ({@link NotificationScanService}) and the staff bot's "Who is on shift" screen and morning
 * summary ask. A terminal that has not been reached for {@code terminalSilenceMinutes} (by {@code lastSeenAt};
 * one nobody has reached yet counts from its registration) cannot have reported anyone's punch, and with no active
 * terminal at all nobody's punch can be seen either - so "late" and "absent" would be accusations, and a board
 * built on them must say so instead of listing everybody as late. Reads only; raising the "not responding"
 * messages stays with the scanner.
 */
@Service
public class TerminalHealthService {

    /** @param healthy true only with at least one active terminal and none silent */
    public record Health(boolean healthy, boolean noActiveTerminal, List<String> silentTerminalNames) {
    }

    private final AttendanceDeviceRepository deviceRepository;
    private final NotificationSettingsService settingsService;
    private final Clock clock;

    public TerminalHealthService(AttendanceDeviceRepository deviceRepository, NotificationSettingsService settingsService, Clock clock) {
        this.deviceRepository = deviceRepository;
        this.settingsService = settingsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Health current() {
        Duration threshold = Duration.ofMinutes(settingsService.getEntity().getTerminalSilenceMinutes());
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        List<AttendanceDeviceEntity> devices = deviceRepository.findByActiveTrue();
        List<String> silent = devices.stream().filter(d -> isSilent(d, nowUtc, threshold)).map(AttendanceDeviceEntity::getName).toList();
        return new Health(!devices.isEmpty() && silent.isEmpty(), devices.isEmpty(), silent);
    }

    /** A terminal nobody has reached yet is silent once it has been registered longer than the threshold. */
    public static boolean isSilent(AttendanceDeviceEntity device, LocalDateTime nowUtc, Duration threshold) {
        LocalDateTime reference = device.getLastSeenAt() != null ? device.getLastSeenAt() : device.getCreatedAt();
        return reference == null || Duration.between(reference, nowUtc).compareTo(threshold) > 0;
    }
}
