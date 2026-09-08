package com.sunsetbeach.service;

import com.sunsetbeach.entity.MaintenanceTaskEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.MaintenanceTask;
import com.sunsetbeach.model.MaintenanceTaskBlockResult;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.model.RoomUnitBlockResult;
import com.sunsetbeach.repository.MaintenanceTaskRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * A task always names a physical room; a {@code RoomUnitBlock} is optional and set separately
 * (see {@link #addBlock}) - a dead light bulb needs a task, not a block. Any authenticated staff
 * role may file/read a task; only MANAGER+ may block the room ({@link #addBlock}); only the
 * ENGINEER job function or MANAGER+ may move it through its statuses ({@link #updateStatus}) -
 * all enforced by {@code SecurityConfig}, not re-checked here (same division of concerns as
 * {@link RoomUnitService}).
 */
@Service
public class MaintenanceTaskService {

    private final MaintenanceTaskRepository maintenanceTaskRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomUnitBlockRepository roomUnitBlockRepository;
    private final RoomUnitService roomUnitService;
    private final ImageUploadValidator imageUploadValidator;
    private final AuditLogService auditLogService;
    private final Path uploadsRoot;

    public MaintenanceTaskService(
            MaintenanceTaskRepository maintenanceTaskRepository,
            RoomUnitRepository roomUnitRepository,
            RoomRepository roomRepository,
            UserRepository userRepository,
            RoomUnitBlockRepository roomUnitBlockRepository,
            RoomUnitService roomUnitService,
            ImageUploadValidator imageUploadValidator,
            AuditLogService auditLogService,
            @Value("${app.uploads.root}") String uploadsRoot) {
        this.maintenanceTaskRepository = maintenanceTaskRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomUnitBlockRepository = roomUnitBlockRepository;
        this.roomUnitService = roomUnitService;
        this.imageUploadValidator = imageUploadValidator;
        this.auditLogService = auditLogService;
        this.uploadsRoot = Path.of(uploadsRoot);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTask> list(MaintenanceTaskStatus status, String roomUnitId) {
        List<MaintenanceTaskEntity> entities;
        if (status != null && roomUnitId != null) {
            entities = maintenanceTaskRepository.findByRoomUnitIdAndStatusOrderByCreatedAtDesc(roomUnitId, status);
        } else if (status != null) {
            entities = maintenanceTaskRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (roomUnitId != null) {
            entities = maintenanceTaskRepository.findByRoomUnitIdOrderByCreatedAtDesc(roomUnitId);
        } else {
            entities = maintenanceTaskRepository.findAllByOrderByCreatedAtDesc();
        }
        return toDtos(entities);
    }

    @Transactional(readOnly = true)
    public MaintenanceTask getById(String id) {
        return toDtos(List.of(findEntity(id))).get(0);
    }

    /**
     * All photos are validated before any are written - the first violation aborts the whole
     * request, same all-or-nothing approach as {@code RoomService#uploadImages}. The task is
     * saved first (so its generated id names the upload directory), then updated once with the
     * stored photo paths - mirrors {@code PropertyMapService#uploadImage}'s own two-step save.
     */
    @Transactional
    public MaintenanceTask create(String roomUnitId, String description, List<MultipartFile> photos, String reportedByUserId) {
        RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
        String trimmedDescription = description == null ? "" : description.trim();
        if (trimmedDescription.isEmpty()) {
            throw new BadRequestException("description is required");
        }

        List<ImageUploadValidator.ValidatedImage> validated = new ArrayList<>();
        for (MultipartFile file : photos == null ? List.<MultipartFile>of() : photos) {
            validated.add(imageUploadValidator.validate(file));
        }

        MaintenanceTaskEntity entity = new MaintenanceTaskEntity();
        entity.setRoomUnitId(roomUnitId);
        entity.setDescription(trimmedDescription);
        entity.setReportedByUserId(reportedByUserId);
        MaintenanceTaskEntity saved = maintenanceTaskRepository.saveAndFlush(entity);

        if (!validated.isEmpty()) {
            Path dir = uploadsRoot.resolve("maintenance-tasks").resolve(saved.getId());
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new IllegalStateException("Could not create upload directory", e);
            }
            List<String> photoPaths = new ArrayList<>();
            for (ImageUploadValidator.ValidatedImage image : validated) {
                String filename = imageUploadValidator.randomFilename(image.extension());
                try {
                    Files.write(dir.resolve(filename), image.bytes());
                } catch (IOException e) {
                    throw new IllegalStateException("Could not write uploaded file", e);
                }
                photoPaths.add("/maintenance-tasks/" + saved.getId() + "/photos/" + filename);
            }
            saved.setPhotos(photoPaths.toArray(new String[0]));
            saved = maintenanceTaskRepository.saveAndFlush(saved);
        }

        auditLogService.record(
                AuditAction.MAINTENANCE_TASK_CREATED,
                AuditEntityType.MAINTENANCE_TASK,
                saved.getId(),
                "Task filed on room " + unit.getLabel() + ": " + trimmedDescription);

        return toDtos(List.of(saved)).get(0);
    }

    /** Path-traversal guarded the same way as {@code PropertyMapService#resolveImage}, plus confirmed against this task's own recorded photo list - a file sitting in the directory that was never recorded isn't servable. */
    @Transactional(readOnly = true)
    public Resource resolvePhoto(String taskId, String filename) {
        MaintenanceTaskEntity task = findEntity(taskId);
        String servedPath = "/maintenance-tasks/" + taskId + "/photos/" + filename;
        if (Arrays.stream(task.getPhotos()).noneMatch(servedPath::equals)) {
            throw new NotFoundException("Photo not found");
        }

        Path dir = uploadsRoot.resolve("maintenance-tasks").resolve(taskId).toAbsolutePath().normalize();
        Path file = dir.resolve(filename).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            throw new NotFoundException("Photo not found");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Photo not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("Photo not found");
        }
    }

    /**
     * Reuses {@link RoomUnitService#createBlock} as-is - same overlap warning, same validation -
     * rather than reimplementing it. Rejected if this task already has a block.
     */
    @Transactional
    public MaintenanceTaskBlockResult addBlock(String taskId, RoomUnitBlockInput input) {
        MaintenanceTaskEntity task = findEntity(taskId);
        if (task.getBlockId() != null) {
            throw new BadRequestException("This task already has a block");
        }

        RoomUnitBlockResult blockResult = roomUnitService.createBlock(task.getRoomUnitId(), input);
        task.setBlockId(blockResult.getBlock().getId());
        MaintenanceTaskEntity saved = maintenanceTaskRepository.saveAndFlush(task);

        auditLogService.record(
                AuditAction.MAINTENANCE_TASK_BLOCKED,
                AuditEntityType.MAINTENANCE_TASK,
                taskId,
                "Block linked to this task (" + blockResult.getBlock().getFromDate() + " to " + blockResult.getBlock().getToDate() + ")");

        return new MaintenanceTaskBlockResult(toDtos(List.of(saved)).get(0), blockResult);
    }

    /**
     * Forward-only (see {@code MaintenanceTaskStatus}'s own description) - checked by name, not
     * {@code Enum#ordinal()}, for the same reason {@code Role} comparisons never use ordinal():
     * this enum is generated from openapi.yaml and a future reordering there would silently
     * invert an ordinal check. Closing a task with a linked block shortens or removes it - see
     * {@link #liftBlock}.
     */
    @Transactional
    public MaintenanceTask updateStatus(String taskId, MaintenanceTaskStatus newStatus) {
        MaintenanceTaskEntity task = findEntity(taskId);
        MaintenanceTaskStatus oldStatus = task.getStatus();
        if (!isForwardTransition(oldStatus, newStatus)) {
            throw new ConflictException("Can't move a task from " + oldStatus.getValue() + " to " + newStatus.getValue());
        }

        task.setStatus(newStatus);
        if (newStatus == MaintenanceTaskStatus.DONE) {
            task.setClosedAt(LocalDateTime.now());
            liftBlock(task);
        }
        MaintenanceTaskEntity saved = maintenanceTaskRepository.saveAndFlush(task);

        auditLogService.record(
                AuditAction.MAINTENANCE_TASK_STATUS_CHANGED,
                AuditEntityType.MAINTENANCE_TASK,
                taskId,
                "Task status changed from " + oldStatus.getValue() + " to " + newStatus.getValue());

        return toDtos(List.of(saved)).get(0);
    }

    private static boolean isForwardTransition(MaintenanceTaskStatus from, MaintenanceTaskStatus to) {
        return switch (from) {
            case OPEN -> to == MaintenanceTaskStatus.IN_PROGRESS || to == MaintenanceTaskStatus.DONE;
            case IN_PROGRESS -> to == MaintenanceTaskStatus.DONE;
            case DONE -> false;
        };
    }

    /**
     * Block dates are inclusive, so a {@code toDate} of today would still cover tonight - the
     * room must not go back on sale until the day after it's actually fixed, so this moves
     * {@code toDate} to yesterday, not today. If yesterday falls before the block's own {@code
     * fromDate}, the block was created and closed the same day and never covered a usable night -
     * delete it outright instead of writing a backwards range. If the block has already been
     * removed by other means (e.g. a manager deleting it directly), this does nothing - closing
     * still succeeds.
     */
    private void liftBlock(MaintenanceTaskEntity task) {
        if (task.getBlockId() == null) {
            return;
        }
        Optional<RoomUnitBlockEntity> blockOpt = roomUnitBlockRepository.findById(task.getBlockId());
        if (blockOpt.isEmpty()) {
            task.setBlockId(null);
            return;
        }

        RoomUnitBlockEntity block = blockOpt.get();
        LocalDate newToDate = LocalDate.now().minusDays(1);
        if (newToDate.isBefore(block.getFromDate())) {
            roomUnitBlockRepository.delete(block);
            task.setBlockId(null);
            auditLogService.record(
                    AuditAction.MAINTENANCE_TASK_BLOCK_LIFTED,
                    AuditEntityType.MAINTENANCE_TASK,
                    task.getId(),
                    "Block removed - task closed the same day the block was created, so it never covered a usable night");
        } else {
            block.setToDate(newToDate);
            roomUnitBlockRepository.save(block);
            auditLogService.record(
                    AuditAction.MAINTENANCE_TASK_BLOCK_LIFTED,
                    AuditEntityType.MAINTENANCE_TASK,
                    task.getId(),
                    "Block shortened to end " + newToDate + " so the room can be sold again");
        }
    }

    private MaintenanceTaskEntity findEntity(String id) {
        return maintenanceTaskRepository.findById(id).orElseThrow(() -> new NotFoundException("Task not found"));
    }

    private List<MaintenanceTask> toDtos(List<MaintenanceTaskEntity> entities) {
        List<String> unitIds = entities.stream().map(MaintenanceTaskEntity::getRoomUnitId).distinct().toList();
        Map<String, RoomUnitEntity> unitsById = roomUnitRepository.findAllById(unitIds).stream().collect(Collectors.toMap(RoomUnitEntity::getId, u -> u));

        List<String> roomIds = unitsById.values().stream().map(RoomUnitEntity::getRoomId).distinct().toList();
        Map<String, String> roomNamesById = roomIds.isEmpty()
                ? Map.of()
                : roomRepository.findAllById(roomIds).stream().collect(Collectors.toMap(RoomEntity::getId, RoomEntity::getName));

        List<String> userIds = entities.stream().map(MaintenanceTaskEntity::getReportedByUserId).distinct().toList();
        Map<String, String> emailsByUserId =
                userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));

        return entities.stream().map(e -> toDto(e, unitsById, roomNamesById, emailsByUserId)).toList();
    }

    private MaintenanceTask toDto(
            MaintenanceTaskEntity e, Map<String, RoomUnitEntity> unitsById, Map<String, String> roomNamesById, Map<String, String> emailsByUserId) {
        RoomUnitEntity unit = unitsById.get(e.getRoomUnitId());
        String roomId = unit != null ? unit.getRoomId() : "";
        String roomName = unit != null ? roomNamesById.getOrDefault(unit.getRoomId(), "") : "";
        String unitLabel = unit != null ? unit.getLabel() : "";

        return new MaintenanceTask(
                e.getId(),
                e.getRoomUnitId(),
                roomId,
                roomName,
                unitLabel,
                e.getDescription(),
                e.getStatus(),
                e.getBlockId(),
                e.getReportedByUserId(),
                emailsByUserId.getOrDefault(e.getReportedByUserId(), ""),
                List.of(e.getPhotos()),
                TimestampFormat.toUtc(e.getCreatedAt()),
                e.getClosedAt() != null ? TimestampFormat.toUtc(e.getClosedAt()) : null);
    }
}
