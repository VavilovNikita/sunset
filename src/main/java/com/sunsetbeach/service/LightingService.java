package com.sunsetbeach.service;

import com.sunsetbeach.entity.LightingDeviceEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.lighting.WledClient;
import com.sunsetbeach.lighting.WledClient.WledPreset;
import com.sunsetbeach.lighting.WledClient.WledState;
import com.sunsetbeach.lighting.WledClient.WledStateChange;
import com.sunsetbeach.lighting.WledException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.LightingDevice;
import com.sunsetbeach.model.LightingDeviceList;
import com.sunsetbeach.model.LightingPreset;
import com.sunsetbeach.model.LightingStateInput;
import com.sunsetbeach.repository.LightingDeviceRepository;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reading and controlling the WLED controllers (the configuration side is {@link
 * LightingSettingsService}). Deliberately not {@code @Transactional}: a call to a controller is a
 * network round trip and must not hold a database transaction open.
 *
 * <p>Nothing polls. A controller is contacted when the page asks for the list, and after a command.
 * The list reads all active controllers in parallel, each bounded by the client's hard deadline, so
 * one dead controller costs one timeout and the rest still return; a state (with the controller's
 * preset list) is reused for {@code app.lighting.state-cache-seconds} so several people opening the
 * page together cost one read each controller. Failed reads are not cached. Every command is audited
 * with its outcome, success or failure.
 */
@Service
public class LightingService {

    private static final Logger log = LoggerFactory.getLogger(LightingService.class);

    private record CachedRead(WledState state, List<WledPreset> presets, Instant readAt) {}

    private final LightingDeviceRepository devices;
    private final WledClient wled;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final long cacheMillis;
    private final long confirmDelayMillis;

    private final Map<String, CachedRead> cache = new ConcurrentHashMap<>();
    private final ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor();

    public LightingService(
            LightingDeviceRepository devices,
            WledClient wled,
            AuditLogService auditLogService,
            Clock clock,
            @Value("${app.lighting.state-cache-seconds:10}") long cacheSeconds,
            @Value("${app.lighting.confirm-delay-ms:300}") long confirmDelayMillis) {
        this.devices = devices;
        this.wled = wled;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.cacheMillis = cacheSeconds * 1000L;
        this.confirmDelayMillis = confirmDelayMillis;
    }

    @PreDestroy
    void shutdown() {
        readers.shutdownNow();
    }

    /** Active controllers with live state, read in parallel. A controller that cannot be read is {@code online:false} with {@code stateError}. */
    public LightingDeviceList list(boolean refresh) {
        List<LightingDeviceEntity> active = devices.findByActiveTrueOrderBySortOrderAscNameAsc();
        List<CompletableFuture<LightingDevice>> futures = new ArrayList<>();
        for (LightingDeviceEntity device : active) {
            futures.add(CompletableFuture.supplyAsync(() -> read(device, refresh), readers));
        }
        List<LightingDevice> result = futures.stream().map(CompletableFuture::join).toList();
        return new LightingDeviceList(result, OffsetDateTime.now(clock));
    }

    /** Forget every cached read - after the configuration changed, so no read outlives the controller it was for. */
    public void clearStateCache() {
        cache.clear();
    }

    public LightingDevice setState(String id, LightingStateInput input) {
        if (input.getIsOn() == null && input.getBrightness() == null && input.getPresetId() == null) {
            throw ValidationException.field("isOn", "Specify at least one of isOn, brightness and presetId");
        }
        LightingDeviceEntity device = devices.findById(id).orElseThrow(() -> new NotFoundException("Controller not found"));
        if (!device.isActive()) {
            throw new ConflictException("The controller is disabled in settings");
        }
        String description = describe(input);
        try {
            wled.setState(device.getHost(), device.getPort(),
                    new WledStateChange(input.getIsOn(), input.getBrightness(), input.getPresetId()));
        } catch (WledException e) {
            WledException named = named(device, e);
            audit(device, "Could not set \"" + device.getName() + "\" (" + description + "): " + named.getMessage());
            throw named;
        }
        audit(device, "Set \"" + device.getName() + "\": " + description);
        CachedRead before = cache.remove(device.getId());
        pause(confirmDelayMillis);
        try {
            WledState state = wled.state(device.getHost(), device.getPort());
            List<WledPreset> presets = before != null ? before.presets() : readPresets(device, List.of());
            CachedRead read = new CachedRead(state, presets, clock.instant());
            cache.put(device.getId(), read);
            return toDto(device, read, null);
        } catch (WledException e) {
            // The command went through; only the confirming read failed. Say so rather than failing the whole action.
            LightingDevice dto = base(device, true, before != null ? before.presets() : List.of());
            dto.stateError("The command was sent, but the state could not be confirmed: controller " + e.getMessage());
            return dto;
        }
    }

    // ---- state -----------------------------------------------------------------------------

    private LightingDevice read(LightingDeviceEntity device, boolean refresh) {
        CachedRead cached = cache.get(device.getId());
        if (!refresh && cached != null && clock.instant().toEpochMilli() < cached.readAt().toEpochMilli() + cacheMillis) {
            return toDto(device, cached, null);
        }
        try {
            WledState state = wled.state(device.getHost(), device.getPort());
            List<WledPreset> presets = readPresets(device, cached != null ? cached.presets() : List.of());
            CachedRead read = new CachedRead(state, presets, clock.instant());
            cache.put(device.getId(), read);
            return toDto(device, read, null);
        } catch (WledException e) {
            cache.remove(device.getId());
            return toDto(device, null, named(device, e).getMessage());
        } catch (RuntimeException e) {
            // Whatever else goes wrong with one controller must not take the whole list down.
            log.warn("Unexpected failure reading lighting controller \"{}\"", device.getName(), e);
            cache.remove(device.getId());
            return toDto(device, null, "Controller \"" + device.getName() + "\" could not be read");
        }
    }

    /** The controller's presets; when they cannot be read the last known list (or none) stands - the state matters more. */
    private List<WledPreset> readPresets(LightingDeviceEntity device, List<WledPreset> fallback) {
        try {
            return wled.presets(device.getHost(), device.getPort());
        } catch (WledException e) {
            log.warn("Presets of lighting controller \"{}\" could not be read: {}", device.getName(), e.getMessage());
            return fallback;
        }
    }

    private LightingDevice base(LightingDeviceEntity d, boolean online, List<WledPreset> presets) {
        List<LightingPreset> dtoPresets = presets.stream().map(p -> new LightingPreset(p.id(), p.name())).toList();
        LightingDevice dto = new LightingDevice(d.getId(), d.getName(), online, dtoPresets);
        dto.location(d.getLocation());
        dto.sortOrder(d.getSortOrder());
        return dto;
    }

    private LightingDevice toDto(LightingDeviceEntity d, CachedRead read, String error) {
        if (read == null) {
            LightingDevice dto = base(d, false, List.of());
            dto.stateError(error);
            return dto;
        }
        LightingDevice dto = base(d, true, read.presets());
        dto.isOn(read.state().on());
        dto.brightness(read.state().brightness());
        dto.presetId(read.state().presetId());
        dto.stateReadAt(read.readAt().atZone(clock.getZone()).toOffsetDateTime());
        return dto;
    }

    // ---- helpers ---------------------------------------------------------------------------

    /** The failure with the controller's name in front - what the person at the screen reads. */
    private static WledException named(LightingDeviceEntity device, WledException e) {
        return new WledException(e.getKind(), "Controller \"" + device.getName() + "\" " + e.getMessage());
    }

    private static String describe(LightingStateInput input) {
        List<String> parts = new ArrayList<>();
        if (input.getIsOn() != null) {
            parts.add(input.getIsOn() ? "on" : "off");
        }
        if (input.getBrightness() != null) {
            parts.add("brightness " + input.getBrightness());
        }
        if (input.getPresetId() != null) {
            parts.add("preset " + input.getPresetId());
        }
        return String.join(", ", parts);
    }

    private void audit(LightingDeviceEntity device, String summary) {
        auditLogService.record(AuditAction.LIGHTING_DEVICE_COMMAND, AuditEntityType.LIGHTING_DEVICE, device.getId(), summary);
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
}
