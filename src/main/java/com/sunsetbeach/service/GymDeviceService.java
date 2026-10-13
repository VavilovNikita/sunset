package com.sunsetbeach.service;

import com.sunsetbeach.entity.GymDeviceEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.GymAcCommand;
import com.sunsetbeach.model.GymAcCommandInput;
import com.sunsetbeach.model.GymDevice;
import com.sunsetbeach.model.GymDeviceList;
import com.sunsetbeach.model.GymDeviceType;
import com.sunsetbeach.repository.GymDeviceRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.CurrentUser;
import com.sunsetbeach.tuya.TuyaClient;
import com.sunsetbeach.tuya.TuyaException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reading and controlling the gym equipment through Tuya (the configuration side is {@link
 * GymSettingsService}). Deliberately not {@code @Transactional}: a call to Tuya is a network round
 * trip and must not hold a database transaction open; each repository call is its own.
 *
 * <p>Nothing here polls. A relay's state is read when the page asks for the list and after a
 * command, and reused for {@code app.tuya.state-cache-seconds} so a few people opening the page
 * together cost one read, not one each (the trial quota is finite). An air conditioner has no
 * readable state - the IR link is one-way - so the list carries the last command this server sent
 * and nothing else. Every command is audited with its outcome, success or failure.
 */
@Service
public class GymDeviceService {

    private static final String TYPE_RELAY = "RELAY";
    private static final String TYPE_AC = "AC_IR";

    private record CachedRead(Boolean on, boolean online, Instant readAt) {}

    private record RelayRead(Boolean on, Boolean online, Instant readAt, String error) {}

    private final GymDeviceRepository devices;
    private final TuyaClient tuya;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final Clock clock;
    private final long cacheMillis;
    private final long confirmDelayMillis;

    private final Map<String, CachedRead> cache = new ConcurrentHashMap<>();

    public GymDeviceService(
            GymDeviceRepository devices,
            TuyaClient tuya,
            AuditLogService auditLogService,
            UserRepository userRepository,
            Clock clock,
            @Value("${app.tuya.state-cache-seconds:30}") long cacheSeconds,
            @Value("${app.tuya.confirm-delay-ms:1500}") long confirmDelayMillis) {
        this.devices = devices;
        this.tuya = tuya;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.clock = clock;
        this.cacheMillis = cacheSeconds * 1000L;
        this.confirmDelayMillis = confirmDelayMillis;
    }

    /** Active devices with relay state. A relay that cannot be read carries {@code stateError}; the list still returns. */
    public GymDeviceList list(boolean refresh) {
        List<GymDevice> result = new ArrayList<>();
        TuyaException integrationDown = null;
        for (GymDeviceEntity device : devices.findByActiveTrueOrderBySortOrderAscNameAsc()) {
            if (!TYPE_RELAY.equals(device.getType())) {
                result.add(toDto(device, null));
                continue;
            }
            if (integrationDown != null) {
                // The whole integration is down: do not spend a call per relay finding that out again.
                result.add(toDto(device, new RelayRead(null, null, null, integrationDown.getMessage())));
                continue;
            }
            try {
                result.add(toDto(device, readRelay(device, refresh)));
            } catch (TuyaException e) {
                if (e.isIntegrationDown()) {
                    integrationDown = e;
                }
                result.add(toDto(device, new RelayRead(null, null, null, e.getMessage())));
            }
        }
        return new GymDeviceList(result, OffsetDateTime.now(clock));
    }

    /** Forget every cached relay read - after the configuration changed, so no read outlives the device it was for. */
    public void clearStateCache() {
        cache.clear();
    }

    public GymDevice setRelay(String id, boolean on) {
        GymDeviceEntity device = requireControllable(id, TYPE_RELAY, "This device is not a relay");
        String action = on ? "on" : "off";
        try {
            tuya.sendCommand(device.getTuyaDeviceId(), device.getSwitchCode(), on);
        } catch (TuyaException e) {
            audit(device, "Could not switch " + action + " \"" + device.getName() + "\": " + e.getMessage());
            throw e;
        }
        audit(device, "Switched " + action + " \"" + device.getName() + "\"");
        cache.remove(device.getTuyaDeviceId());
        // Tuya's cloud takes up to about a second to show what the relay did (measured on the gym's relays:
        // an immediate read returned the old value), so wait a moment, read once, and never present a
        // not-yet-updated read as the truth.
        pause(confirmDelayMillis);
        try {
            RelayRead read = readRelay(device, true);
            if (read.on() != null && read.on() != on) {
                cache.remove(device.getTuyaDeviceId());
                return toDto(device, new RelayRead(read.on(), read.online(), read.readAt(),
                        "The command was sent, but the device has not confirmed the new state yet — press Refresh in a couple of seconds"));
            }
            return toDto(device, read);
        } catch (TuyaException e) {
            // The command went through; only the confirming read failed. Say so rather than failing the whole action.
            return toDto(device, new RelayRead(on, null, null, "The command was sent, but the state could not be confirmed: " + e.getMessage()));
        }
    }

    public GymDevice sendAcCommand(String id, GymAcCommandInput input) {
        GymDeviceEntity device = requireControllable(id, TYPE_AC, "This device is not an air conditioner");
        if (blank(device.getInfraredId()) || blank(device.getRemoteId())) {
            throw new ConflictException("The air conditioner has no IR hub or remote set — fill them in under settings");
        }
        String description = describe(input);
        try {
            tuya.sendAirConditionerCommand(
                    device.getInfraredId(), device.getRemoteId(), input.getPower(), input.getMode(), input.getTemp(), input.getWind());
        } catch (TuyaException e) {
            audit(device, "Could not send to \"" + device.getName() + "\" (" + description + "): " + e.getMessage());
            throw e;
        }
        device.setLastAcPower(input.getPower());
        device.setLastAcMode(input.getMode());
        device.setLastAcTemp(input.getTemp());
        device.setLastAcWind(input.getWind());
        device.setLastAcSentAt(TimestampFormat.nowUtc(clock));
        device.setLastAcSentBy(currentUserName());
        GymDeviceEntity saved = devices.save(device);
        audit(saved, "Sent to \"" + saved.getName() + "\": " + description);
        return toDto(saved, null);
    }

    // ---- state -----------------------------------------------------------------------------

    private RelayRead readRelay(GymDeviceEntity device, boolean refresh) {
        Instant now = clock.instant();
        CachedRead cached = cache.get(device.getTuyaDeviceId());
        if (!refresh && cached != null && now.toEpochMilli() < cached.readAt().toEpochMilli() + cacheMillis) {
            return new RelayRead(cached.on(), cached.online(), cached.readAt(), null);
        }
        TuyaClient.TuyaDeviceState state = tuya.deviceState(device.getTuyaDeviceId());
        Boolean on = state.booleanStatus(device.getSwitchCode());
        if (on == null) {
            return new RelayRead(null, state.online(), now, "The relay does not report the state of switch \"" + device.getSwitchCode() + "\"");
        }
        cache.put(device.getTuyaDeviceId(), new CachedRead(on, state.online(), now));
        return new RelayRead(on, state.online(), now, null);
    }

    private GymDevice toDto(GymDeviceEntity d, RelayRead read) {
        GymDevice dto = new GymDevice(d.getId(), d.getName(), GymDeviceType.fromValue(d.getType()), d.getSortOrder());
        if (read != null) {
            if (read.on() != null) {
                dto.isOn(read.on());
            }
            if (read.online() != null) {
                dto.online(read.online());
            }
            if (read.readAt() != null) {
                dto.stateReadAt(read.readAt().atZone(clock.getZone()).toOffsetDateTime());
            }
            if (read.error() != null) {
                dto.stateError(read.error());
            }
        }
        if (d.getLastAcSentAt() != null && d.getLastAcPower() != null) {
            dto.lastCommand(new GymAcCommand(
                    d.getLastAcPower(), d.getLastAcMode(), d.getLastAcTemp(), d.getLastAcWind(),
                    TimestampFormat.toUtc(d.getLastAcSentAt()), d.getLastAcSentBy() == null ? "" : d.getLastAcSentBy()));
        }
        return dto;
    }

    // ---- helpers ---------------------------------------------------------------------------

    private GymDeviceEntity requireControllable(String id, String type, String wrongTypeMessage) {
        GymDeviceEntity device = devices.findById(id).orElseThrow(() -> new NotFoundException("Device not found"));
        if (!type.equals(device.getType())) {
            throw new ConflictException(wrongTypeMessage);
        }
        if (!device.isActive()) {
            throw new ConflictException("The device is disabled in settings");
        }
        return device;
    }

    private void audit(GymDeviceEntity device, String summary) {
        auditLogService.record(AuditAction.GYM_DEVICE_COMMAND, AuditEntityType.GYM_DEVICE, device.getId(), summary);
    }

    private String currentUserName() {
        try {
            UserEntity user = userRepository.findById(CurrentUser.id()).orElse(null);
            if (user != null) {
                if (user.getFullName() != null && !user.getFullName().isBlank()) {
                    return user.getFullName().trim();
                }
                if (user.getName() != null && !user.getName().isBlank()) {
                    return user.getName().trim();
                }
                return user.getEmail();
            }
        } catch (RuntimeException e) {
            // No staff principal or no row: a neutral label rather than failing an already-sent command.
        }
        return "—";
    }

    private static void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String describe(GymAcCommandInput in) {
        if (in.getPower() == 0) {
            return "off";
        }
        String[] modes = {"cool", "heat", "auto", "fan", "dry"};
        String wind = in.getWind() == 0 ? "fan auto" : "fan " + in.getWind();
        return "on, " + modes[in.getMode()] + ", " + in.getTemp() + "°, " + wind;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
