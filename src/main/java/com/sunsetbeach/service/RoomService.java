package com.sunsetbeach.service;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.RoomMapper;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomMapper roomMapper;
    private final Path uploadsRoot;
    private final AuditLogService auditLogService;
    private final ImageUploadValidator imageUploadValidator;

    public RoomService(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomMapper roomMapper,
            @Value("${app.uploads.root}") String uploadsRoot,
            AuditLogService auditLogService,
            ImageUploadValidator imageUploadValidator) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomMapper = roomMapper;
        this.uploadsRoot = Path.of(uploadsRoot);
        this.auditLogService = auditLogService;
        this.imageUploadValidator = imageUploadValidator;
    }

    @Transactional(readOnly = true)
    public List<Room> list() {
        Map<String, Long> activeUnitCounts = roomUnitRepository.countActiveGroupedByRoom().stream()
                .collect(Collectors.toMap(RoomUnitRepository.RoomActiveUnitCount::getRoomId, RoomUnitRepository.RoomActiveUnitCount::getActiveCount));
        return roomRepository.findAll().stream()
                .map(room -> roomMapper.toDto(room, activeUnitCounts.getOrDefault(room.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Room getById(String id) {
        RoomEntity entity = roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Room not found"));
        return roomMapper.toDto(entity);
    }

    @Transactional
    public Room create(RoomInput input) {
        RoomEntity entity = new RoomEntity();
        roomMapper.applyInput(entity, input);
        // flush so @CreationTimestamp (populated at insert time) is on the object before mapping
        return roomMapper.toDto(roomRepository.saveAndFlush(entity));
    }

    @Transactional
    public Room update(String id, RoomInput input) {
        RoomEntity entity = roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Room not found"));
        BigDecimal oldBasePrice = entity.getBasePrice();
        roomMapper.applyInput(entity, input);
        RoomEntity saved = roomRepository.save(entity);

        if (oldBasePrice.compareTo(saved.getBasePrice()) != 0) {
            // Both sides scaled to 2 decimal places before formatting: oldBasePrice comes off the
            // entity (DB numeric(_,2), always "1550.00"), but saved.getBasePrice() at this point is
            // still the raw value applyInput() copied from the request body (e.g. a bare "1500"
            // from JSON) - without normalizing, the summary read "changed from 1550.00 to 1500".
            auditLogService.record(
                    AuditAction.ROOM_PRICE_CHANGED,
                    AuditEntityType.ROOM,
                    saved.getId(),
                    "Base price for " + saved.getName() + " changed from "
                            + oldBasePrice.setScale(2, RoundingMode.HALF_UP) + " to "
                            + saved.getBasePrice().setScale(2, RoundingMode.HALF_UP));
        }

        return roomMapper.toDto(saved);
    }

    @Transactional
    public void delete(String id) {
        // Modern Spring Data JPA's deleteById() silently no-ops on a missing row instead of
        // throwing EmptyResultDataAccessException, so existence has to be checked explicitly.
        if (!roomRepository.existsById(id)) {
            throw new NotFoundException("Room not found");
        }
        try {
            roomRepository.deleteById(id);
            roomRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This room has existing bookings or room units and can't be deleted.");
        }
    }

    @Transactional
    public Room uploadImages(String id, List<MultipartFile> files) {
        RoomEntity room = roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Room not found"));

        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new BadRequestException("No files provided");
        }

        // Validate every file before writing any of them - the first violation aborts the
        // whole request, so nothing should be left on disk from a rejected upload.
        List<ImageUploadValidator.ValidatedImage> validated = new ArrayList<>();
        for (MultipartFile file : files) {
            validated.add(imageUploadValidator.validate(file));
        }

        Path dir = uploadsRoot.resolve("rooms").resolve(room.getId());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory", e);
        }

        List<String> newPaths = new ArrayList<>();
        for (ImageUploadValidator.ValidatedImage image : validated) {
            String filename = imageUploadValidator.randomFilename(image.extension());
            try {
                Files.write(dir.resolve(filename), image.bytes());
            } catch (IOException e) {
                throw new IllegalStateException("Could not write uploaded file", e);
            }
            newPaths.add("/uploads/rooms/" + room.getId() + "/" + filename);
        }

        List<String> images = new ArrayList<>(List.of(room.getImages()));
        images.addAll(newPaths);
        room.setImages(images.toArray(new String[0]));

        return roomMapper.toDto(roomRepository.save(room));
    }

    @Transactional
    public Room deleteImage(String id, String imagePath) {
        RoomEntity room = roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Room not found"));

        List<String> remaining = new ArrayList<>();
        for (String path : room.getImages()) {
            if (!path.equals(imagePath)) {
                remaining.add(path);
            }
        }
        room.setImages(remaining.toArray(new String[0]));
        RoomEntity saved = roomRepository.save(room);

        deleteFileIfWithinRoomDirectory(room.getId(), imagePath);

        return roomMapper.toDto(saved);
    }

    /**
     * Best-effort disk cleanup for an image path just removed from {@code Room.images}. Mirrors
     * {@link RoomImageService#resolve}'s containment check (reject any path segment separator in
     * the filename outright, then require the resolved, normalized path to still live under this
     * room's own directory) rather than the plain string-prefix check this used to do, which a
     * path like {@code /uploads/rooms/{id}/../../../some-other-file} would pass without ever
     * actually resolving under this room's directory.
     */
    private void deleteFileIfWithinRoomDirectory(String roomId, String imagePath) {
        String prefix = "/uploads/rooms/" + roomId + "/";
        if (!imagePath.startsWith(prefix)) {
            return;
        }
        String filename = imagePath.substring(prefix.length());
        if (filename.contains("/") || filename.contains("\\")) {
            return;
        }

        Path roomDir = uploadsRoot.resolve("rooms").resolve(roomId).normalize();
        Path file = roomDir.resolve(filename).normalize();
        if (!file.startsWith(roomDir)) {
            return;
        }

        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // best-effort, matches the JS route's .catch(() => {})
        }
    }
}
