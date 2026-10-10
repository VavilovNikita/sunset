package com.sunsetbeach.service;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.SiteMinderRoomTypeMappingEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.SiteMinderRoomTypeMapping;
import com.sunsetbeach.model.SiteMinderRoomTypeMappingInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.SiteMinderRoomTypeMappingRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for the SiteMinder room-type mapping (MANAGER+), and the lookup the import uses. Same shape
 * as {@code AttendanceDeviceService}: a unique name enforced by the database and translated to 409,
 * every write audited.
 */
@Service
public class SiteMinderRoomTypeMappingService {

    private static final String DUPLICATE_MESSAGE = "This SiteMinder room type is already mapped";

    private final SiteMinderRoomTypeMappingRepository mappingRepository;
    private final RoomRepository roomRepository;
    private final AuditLogService auditLogService;

    public SiteMinderRoomTypeMappingService(
            SiteMinderRoomTypeMappingRepository mappingRepository, RoomRepository roomRepository, AuditLogService auditLogService) {
        this.mappingRepository = mappingRepository;
        this.roomRepository = roomRepository;
        this.auditLogService = auditLogService;
    }

    /** Trimmed, with internal runs of whitespace collapsed - scraped text isn't reliably spaced. Case is left alone; lookups ignore it. */
    static String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }

    /** The room type a SiteMinder room type name is mapped to, if any. */
    @Transactional(readOnly = true)
    public Optional<RoomEntity> findMappedRoom(String siteMinderRoomType) {
        return mappingRepository.findByName(normalizeName(siteMinderRoomType)).flatMap(m -> roomRepository.findById(m.getRoomId()));
    }

    /**
     * The room type a name sent by the import means: an explicit mapping first (a manager's alias wins,
     * even over a room type that happens to carry the same name), otherwise the room type whose own name
     * matches, ignoring case and runs of spaces - so a sender that already uses this system's names needs
     * no mapping rows at all. Empty if neither finds anything.
     *
     * <p>{@code Room.name} isn't unique in the database, so a name shared by two room types matches
     * neither of them: guessing which one a booking belongs to would put it, and its price, in the wrong
     * room. That is rejected with its own message; an explicit mapping resolves it.
     */
    @Transactional(readOnly = true)
    public Optional<RoomEntity> resolveRoom(String name) {
        Optional<RoomEntity> mapped = findMappedRoom(name);
        if (mapped.isPresent()) {
            return mapped;
        }
        String wanted = normalizeName(name);
        List<RoomEntity> sameName = roomRepository.findAll().stream()
                .filter(r -> r.getName() != null && normalizeName(r.getName()).equalsIgnoreCase(wanted))
                .toList();
        if (sameName.size() > 1) {
            throw ValidationException.field(
                    "roomTypeName",
                    "More than one room type is named \"" + wanted + "\", so it can't be matched by name - add it under SiteMinder room type mappings.");
        }
        return sameName.stream().findFirst();
    }

    @Transactional(readOnly = true)
    public List<SiteMinderRoomTypeMapping> list() {
        return mappingRepository.findAllByOrderBySiteMinderRoomTypeAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public SiteMinderRoomTypeMapping create(SiteMinderRoomTypeMappingInput input) {
        RoomEntity room = findRoom(input.getRoomId());
        SiteMinderRoomTypeMappingEntity entity = new SiteMinderRoomTypeMappingEntity();
        entity.setSiteMinderRoomType(normalizeName(input.getSiteMinderRoomType()));
        entity.setRoomId(room.getId());
        SiteMinderRoomTypeMappingEntity saved = save(entity);
        auditLogService.record(
                AuditAction.SITEMINDER_ROOM_TYPE_MAPPING_CREATED,
                AuditEntityType.SITEMINDER_ROOM_TYPE_MAPPING,
                saved.getId(),
                "SiteMinder room type \"" + saved.getSiteMinderRoomType() + "\" mapped to " + room.getName());
        return toDto(saved);
    }

    @Transactional
    public SiteMinderRoomTypeMapping update(String id, SiteMinderRoomTypeMappingInput input) {
        SiteMinderRoomTypeMappingEntity entity = findOrThrow(id);
        RoomEntity room = findRoom(input.getRoomId());
        String oldName = entity.getSiteMinderRoomType();
        String oldRoomId = entity.getRoomId();
        entity.setSiteMinderRoomType(normalizeName(input.getSiteMinderRoomType()));
        entity.setRoomId(room.getId());
        SiteMinderRoomTypeMappingEntity saved = save(entity);

        StringBuilder summary = new StringBuilder("SiteMinder room type mapping \"").append(oldName).append("\" updated");
        if (!oldName.equals(saved.getSiteMinderRoomType())) {
            summary.append("; renamed to \"").append(saved.getSiteMinderRoomType()).append("\"");
        }
        if (!Objects.equals(oldRoomId, saved.getRoomId())) {
            String oldRoomName = roomRepository.findById(oldRoomId).map(RoomEntity::getName).orElse("a deleted room type");
            summary.append("; room type ").append(oldRoomName).append(" → ").append(room.getName());
        }
        auditLogService.record(
                AuditAction.SITEMINDER_ROOM_TYPE_MAPPING_UPDATED, AuditEntityType.SITEMINDER_ROOM_TYPE_MAPPING, saved.getId(), summary.toString());
        return toDto(saved);
    }

    @Transactional
    public void delete(String id) {
        SiteMinderRoomTypeMappingEntity entity = findOrThrow(id);
        mappingRepository.delete(entity);
        auditLogService.record(
                AuditAction.SITEMINDER_ROOM_TYPE_MAPPING_DELETED,
                AuditEntityType.SITEMINDER_ROOM_TYPE_MAPPING,
                id,
                "SiteMinder room type mapping \"" + entity.getSiteMinderRoomType() + "\" deleted");
    }

    private SiteMinderRoomTypeMappingEntity save(SiteMinderRoomTypeMappingEntity entity) {
        try {
            return mappingRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(DUPLICATE_MESSAGE);
        }
    }

    private RoomEntity findRoom(String roomId) {
        return roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
    }

    private SiteMinderRoomTypeMappingEntity findOrThrow(String id) {
        return mappingRepository.findById(id).orElseThrow(() -> new NotFoundException("Mapping not found"));
    }

    private SiteMinderRoomTypeMapping toDto(SiteMinderRoomTypeMappingEntity entity) {
        String roomName = roomRepository.findById(entity.getRoomId()).map(RoomEntity::getName).orElse(null);
        return new SiteMinderRoomTypeMapping(
                entity.getId(),
                entity.getSiteMinderRoomType(),
                entity.getRoomId(),
                roomName,
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()));
    }
}
