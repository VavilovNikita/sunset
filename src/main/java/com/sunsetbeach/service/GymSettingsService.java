package com.sunsetbeach.service;

import com.sunsetbeach.entity.GymDeviceEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.GymDeviceConfig;
import com.sunsetbeach.model.GymDeviceConfigInput;
import com.sunsetbeach.model.GymDeviceType;
import com.sunsetbeach.model.GymTuyaUsage;
import com.sunsetbeach.model.TuyaDiscoveredDevice;
import com.sunsetbeach.model.TuyaRemote;
import com.sunsetbeach.repository.GymDeviceRepository;
import com.sunsetbeach.tuya.TuyaClient;
import com.sunsetbeach.tuya.TuyaUsageCounter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ADMIN side of the gym module: which Tuya devices appear on the page, in what order, the list
 * of devices Tuya offers for adding, and the monthly call counter. Device ids are runtime data an
 * admin picks from the Tuya project - never a migration.
 */
@Service
public class GymSettingsService {

    private final GymDeviceRepository devices;
    private final GymDeviceService gymDeviceService;
    private final TuyaClient tuya;
    private final TuyaUsageCounter usage;
    private final AuditLogService auditLogService;
    private final int quotaHint;

    public GymSettingsService(
            GymDeviceRepository devices,
            GymDeviceService gymDeviceService,
            TuyaClient tuya,
            TuyaUsageCounter usage,
            AuditLogService auditLogService,
            @Value("${app.tuya.monthly-quota-hint:26000}") int quotaHint) {
        this.devices = devices;
        this.gymDeviceService = gymDeviceService;
        this.tuya = tuya;
        this.usage = usage;
        this.auditLogService = auditLogService;
        this.quotaHint = quotaHint;
    }

    @Transactional(readOnly = true)
    public List<GymDeviceConfig> list() {
        return devices.findAllByOrderBySortOrderAscNameAsc().stream().map(GymSettingsService::toDto).toList();
    }

    @Transactional
    public GymDeviceConfig create(GymDeviceConfigInput input) {
        GymDeviceEntity entity = new GymDeviceEntity();
        apply(entity, input);
        if (devices.existsByTuyaDeviceId(entity.getTuyaDeviceId())) {
            throw new ConflictException("This Tuya device is already added");
        }
        entity.setSortOrder(devices.findAll().stream().mapToInt(GymDeviceEntity::getSortOrder).max().orElse(-1) + 1);
        GymDeviceEntity saved;
        try {
            saved = devices.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This Tuya device is already added");
        }
        auditLogService.record(
                AuditAction.GYM_DEVICE_CREATED, AuditEntityType.GYM_DEVICE, saved.getId(), "Gym device \"" + saved.getName() + "\" added");
        return toDto(saved);
    }

    @Transactional
    public GymDeviceConfig update(String id, GymDeviceConfigInput input) {
        GymDeviceEntity entity = find(id);
        String oldName = entity.getName();
        boolean oldActive = entity.isActive();
        boolean oldInBookings = entity.isInBookings();
        apply(entity, input);
        if (devices.existsByTuyaDeviceIdAndIdNot(entity.getTuyaDeviceId(), id)) {
            throw new ConflictException("This Tuya device is already added");
        }
        GymDeviceEntity saved;
        try {
            saved = devices.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This Tuya device is already added");
        }
        StringBuilder summary = new StringBuilder("Gym device \"").append(oldName).append("\" updated");
        if (!oldName.equals(saved.getName())) {
            summary.append(" (renamed to \"").append(saved.getName()).append("\")");
        }
        if (oldActive != saved.isActive()) {
            summary.append(saved.isActive() ? "; enabled" : "; disabled");
        }
        if (oldInBookings != saved.isInBookings()) {
            summary.append(saved.isInBookings() ? "; now switched by gym reservations" : "; no longer switched by gym reservations");
        }
        gymDeviceService.clearStateCache();
        auditLogService.record(AuditAction.GYM_DEVICE_UPDATED, AuditEntityType.GYM_DEVICE, saved.getId(), summary.toString());
        return toDto(saved);
    }

    @Transactional
    public List<GymDeviceConfig> reorder(List<String> ids) {
        List<GymDeviceEntity> all = devices.findAll();
        Map<String, GymDeviceEntity> byId = new HashMap<>();
        all.forEach(d -> byId.put(d.getId(), d));
        Set<String> requested = new HashSet<>(ids);
        if (ids.size() != byId.size() || !byId.keySet().equals(requested)) {
            throw new BadRequestException("The list must contain every configured device exactly once");
        }
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).setSortOrder(i);
        }
        devices.saveAll(all);
        if (!ids.isEmpty()) {
            auditLogService.record(AuditAction.GYM_DEVICE_UPDATED, AuditEntityType.GYM_DEVICE, ids.get(0), "Gym device order changed");
        }
        return list();
    }

    @Transactional
    public void delete(String id) {
        GymDeviceEntity entity = find(id);
        devices.delete(entity);
        gymDeviceService.clearStateCache();
        auditLogService.record(
                AuditAction.GYM_DEVICE_DELETED, AuditEntityType.GYM_DEVICE, id, "Gym device \"" + entity.getName() + "\" deleted");
    }

    /** Live from Tuya (costs calls). Only what the add screen shows - the client never copies out {@code localKey}. */
    public List<TuyaDiscoveredDevice> listTuyaDevices() {
        return tuya.listDevices().stream()
                .map(d -> {
                    TuyaDiscoveredDevice dto = new TuyaDiscoveredDevice(d.id(), d.name(), d.category(), d.online(), d.sub());
                    dto.productName(d.productName());
                    return dto;
                })
                .toList();
    }

    public List<String> listSwitchCodes(String tuyaDeviceId) {
        return tuya.deviceState(tuyaDeviceId).status().entrySet().stream()
                .filter(e -> e.getValue().isBoolean())
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<TuyaRemote> listRemotes(String infraredId) {
        return tuya.listRemotes(infraredId).stream().map(r -> new TuyaRemote(r.remoteId(), r.name())).toList();
    }

    public GymTuyaUsage usage() {
        return new GymTuyaUsage(usage.currentMonth(), usage.callsThisMonth(), quotaHint);
    }

    private GymDeviceEntity find(String id) {
        return devices.findById(id).orElseThrow(() -> new NotFoundException("Device not found"));
    }

    /** Full replacement; the type-specific fields are checked here because Bean Validation cannot see across fields. */
    private static void apply(GymDeviceEntity entity, GymDeviceConfigInput input) {
        String tuyaId = input.getTuyaDeviceId().trim();
        if (tuyaId.isEmpty()) {
            throw ValidationException.field("tuyaDeviceId", "Specify the Tuya device");
        }
        entity.setName(input.getName());
        entity.setType(input.getType().getValue());
        entity.setTuyaDeviceId(tuyaId);
        entity.setActive(input.getActive() == null || input.getActive());
        if (input.getInBookings() != null) {
            entity.setInBookings(input.getInBookings());
        }
        String switchCode = trimToNull(input.getSwitchCode());
        String infraredId = trimToNull(input.getInfraredId());
        String remoteId = trimToNull(input.getRemoteId());
        if (input.getType() == GymDeviceType.RELAY) {
            if (switchCode == null) {
                throw ValidationException.field("switchCode", "A relay needs a switch code (for example switch_1)");
            }
            entity.setSwitchCode(switchCode);
            entity.setInfraredId(null);
            entity.setRemoteId(null);
        } else {
            if (infraredId == null) {
                throw ValidationException.field("infraredId", "An air conditioner needs an IR hub");
            }
            entity.setInfraredId(infraredId);
            // A virtual IR remote's device id is its remote id; used when the admin does not state one.
            entity.setRemoteId(remoteId != null ? remoteId : tuyaId);
            entity.setSwitchCode(null);
        }
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static GymDeviceConfig toDto(GymDeviceEntity d) {
        GymDeviceConfig dto = new GymDeviceConfig(
                d.getId(), d.getName(), GymDeviceType.fromValue(d.getType()), d.isInBookings(), d.getTuyaDeviceId(), d.getSortOrder(), d.isActive());
        dto.infraredId(d.getInfraredId());
        dto.remoteId(d.getRemoteId());
        dto.switchCode(d.getSwitchCode());
        return dto;
    }
}
