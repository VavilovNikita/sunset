package com.sunsetbeach.service;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.RoomMapper;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RoomService {

    private static final long MAX_FILE_BYTES = 8L * 1024 * 1024;
    // Keyed by the type Tika detects from actual file content (magic bytes), not the
    // client-supplied Content-Type header - this is what decides both acceptance and the
    // stored file's extension, so a relabeled .svg can't slip through as a "photo".
    private static final Map<String, String> ALLOWED_TYPE_EXTENSIONS =
            Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private static final Tika TIKA = new Tika();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomMapper roomMapper;
    private final Path uploadsRoot;

    public RoomService(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomMapper roomMapper,
            @Value("${app.uploads.root}") String uploadsRoot) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomMapper = roomMapper;
        this.uploadsRoot = Path.of(uploadsRoot);
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
        roomMapper.applyInput(entity, input);
        return roomMapper.toDto(roomRepository.save(entity));
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
        List<byte[]> contents = new ArrayList<>();
        List<String> extensions = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_BYTES) {
                throw new BadRequestException(file.getOriginalFilename() + " exceeds the 8MB limit");
            }
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Could not read uploaded file", e);
            }
            String detectedType = TIKA.detect(bytes);
            String extension = ALLOWED_TYPE_EXTENSIONS.get(detectedType);
            if (extension == null) {
                throw new BadRequestException("Unsupported file type: " + detectedType);
            }
            contents.add(bytes);
            extensions.add(extension);
        }

        Path dir = uploadsRoot.resolve("rooms").resolve(room.getId());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory", e);
        }

        List<String> newPaths = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            String filename = randomFilename(extensions.get(i));
            try {
                Files.write(dir.resolve(filename), contents.get(i));
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

    private static String randomFilename(String extension) {
        StringBuilder random = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            random.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return System.currentTimeMillis() + "-" + random + extension;
    }
}
