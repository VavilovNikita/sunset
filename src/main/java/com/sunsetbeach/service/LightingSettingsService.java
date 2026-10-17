package com.sunsetbeach.service;

import com.sunsetbeach.entity.LightingDeviceEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.SqlStates;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.lighting.LanAddressGuard;
import com.sunsetbeach.lighting.WledClient;
import com.sunsetbeach.lighting.WledClient.WledInfo;
import com.sunsetbeach.lighting.WledException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.LightingDeviceCheckInput;
import com.sunsetbeach.model.LightingDeviceCheckResult;
import com.sunsetbeach.model.LightingDeviceConfig;
import com.sunsetbeach.model.LightingDeviceCreateInput;
import com.sunsetbeach.model.LightingDeviceInfo;
import com.sunsetbeach.model.LightingDeviceUpdateInput;
import com.sunsetbeach.repository.LightingDeviceRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ADMIN side of the lighting module: which controllers appear on the page, at which address, in
 * what order, and the "test before saving" call. An address is checked here when it is typed (so the
 * admin gets a 400 at once) and again by the client on every request - see {@link LanAddressGuard}.
 */
@Service
public class LightingSettingsService {

    private static final String NAME_TAKEN = "Another controller already has this name";

    private final LightingDeviceRepository devices;
    private final LightingService lightingService;
    private final LanAddressGuard guard;
    private final WledClient wled;
    private final AuditLogService auditLogService;

    public LightingSettingsService(
            LightingDeviceRepository devices,
            LightingService lightingService,
            LanAddressGuard guard,
            WledClient wled,
            AuditLogService auditLogService) {
        this.devices = devices;
        this.lightingService = lightingService;
        this.guard = guard;
        this.wled = wled;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<LightingDeviceConfig> list() {
        return devices.findAllByOrderBySortOrderAscNameAsc().stream().map(LightingSettingsService::toDto).toList();
    }

    @Transactional
    public LightingDeviceConfig create(LightingDeviceCreateInput input) {
        requireAllowedHost(input.getHost());
        requireNameFree(input.getName(), null);
        LightingDeviceEntity entity = new LightingDeviceEntity();
        entity.setName(input.getName());
        entity.setHost(input.getHost().trim());
        entity.setPort(input.getPort() == null ? 80 : input.getPort());
        entity.setLocation(blankToNull(input.getLocation()));
        entity.setActive(input.getActive() == null || input.getActive());
        entity.setSortOrder(devices.findAll().stream().mapToInt(LightingDeviceEntity::getSortOrder).max().orElse(-1) + 1);
        LightingDeviceEntity saved = save(entity);
        auditLogService.record(AuditAction.LIGHTING_DEVICE_CREATED, AuditEntityType.LIGHTING_DEVICE, saved.getId(),
                "Lighting controller \"" + saved.getName() + "\" added (" + saved.getHost() + ":" + saved.getPort() + ")");
        return toDto(saved);
    }

    @Transactional
    public LightingDeviceConfig update(String id, LightingDeviceUpdateInput input) {
        LightingDeviceEntity entity = find(id);
        String oldName = entity.getName();
        String oldHost = entity.getHost();
        int oldPort = entity.getPort();
        boolean oldActive = entity.isActive();
        if (input.getHost() != null) {
            requireAllowedHost(input.getHost());
            entity.setHost(input.getHost().trim());
        }
        if (input.getName() != null) {
            requireNameFree(input.getName(), id);
            entity.setName(input.getName());
        }
        if (input.getPort() != null) {
            entity.setPort(input.getPort());
        }
        if (input.getLocation() != null) {
            entity.setLocation(blankToNull(input.getLocation()));
        }
        if (input.getActive() != null) {
            entity.setActive(input.getActive());
        }
        LightingDeviceEntity saved = save(entity);
        StringBuilder summary = new StringBuilder("Lighting controller \"").append(oldName).append("\" updated");
        if (!oldName.equals(saved.getName())) {
            summary.append(" (renamed to \"").append(saved.getName()).append("\")");
        }
        if (!oldHost.equals(saved.getHost()) || oldPort != saved.getPort()) {
            summary.append("; address ").append(oldHost).append(':').append(oldPort)
                    .append(" -> ").append(saved.getHost()).append(':').append(saved.getPort());
        }
        if (oldActive != saved.isActive()) {
            summary.append(saved.isActive() ? "; enabled" : "; disabled");
        }
        lightingService.clearStateCache();
        auditLogService.record(AuditAction.LIGHTING_DEVICE_UPDATED, AuditEntityType.LIGHTING_DEVICE, saved.getId(), summary.toString());
        return toDto(saved);
    }

    @Transactional
    public List<LightingDeviceConfig> reorder(List<String> ids) {
        List<LightingDeviceEntity> all = devices.findAll();
        Map<String, LightingDeviceEntity> byId = new HashMap<>();
        all.forEach(d -> byId.put(d.getId(), d));
        Set<String> requested = new HashSet<>(ids);
        if (ids.size() != byId.size() || !byId.keySet().equals(requested)) {
            throw new BadRequestException("The list must contain every configured controller exactly once");
        }
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).setSortOrder(i);
        }
        devices.saveAll(all);
        if (!ids.isEmpty()) {
            auditLogService.record(AuditAction.LIGHTING_DEVICE_UPDATED, AuditEntityType.LIGHTING_DEVICE, ids.get(0), "Lighting controller order changed");
        }
        return list();
    }

    @Transactional
    public void delete(String id) {
        LightingDeviceEntity entity = find(id);
        devices.delete(entity);
        lightingService.clearStateCache();
        auditLogService.record(AuditAction.LIGHTING_DEVICE_DELETED, AuditEntityType.LIGHTING_DEVICE, id,
                "Lighting controller \"" + entity.getName() + "\" deleted");
    }

    /**
     * Asks {@code host:port} for {@code /json/info} and saves nothing. An address that is not allowed is
     * a 400 (as on save); a controller that does not answer is a normal 200 with {@code reachable:false}.
     * Not transactional: it is a network call.
     */
    public LightingDeviceCheckResult check(LightingDeviceCheckInput input) {
        requireAllowedHost(input.getHost());
        int port = input.getPort() == null ? 80 : input.getPort();
        try {
            WledInfo info = wled.info(input.getHost().trim(), port);
            LightingDeviceCheckResult result = new LightingDeviceCheckResult(true);
            result.info(new LightingDeviceInfo(info.name(), info.version(), info.ledCount(), info.mac()));
            return result;
        } catch (WledException e) {
            LightingDeviceCheckResult result = new LightingDeviceCheckResult(false);
            result.error("The controller " + e.getMessage());
            return result;
        }
    }

    // ---- helpers ---------------------------------------------------------------------------

    private void requireAllowedHost(String host) {
        try {
            guard.resolve(host);
        } catch (LanAddressGuard.RefusedHostException e) {
            throw ValidationException.field("host", e.getMessage());
        }
    }

    /** Unique ignoring case and spaces; V146's index holds it under a race. */
    private void requireNameFree(String name, String excludeId) {
        if (devices.existsByNormalizedName(name, excludeId)) {
            throw new ConflictException(NAME_TAKEN);
        }
    }

    private LightingDeviceEntity save(LightingDeviceEntity entity) {
        try {
            return devices.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (SqlStates.is(e, "23505")) {
                throw new ConflictException(NAME_TAKEN);
            }
            throw e;
        }
    }

    private LightingDeviceEntity find(String id) {
        return devices.findById(id).orElseThrow(() -> new NotFoundException("Controller not found"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static LightingDeviceConfig toDto(LightingDeviceEntity d) {
        LightingDeviceConfig dto = new LightingDeviceConfig(d.getId(), d.getName(), d.getHost(), d.getPort(), d.isActive(), d.getSortOrder());
        dto.location(d.getLocation());
        return dto;
    }
}
