package com.sunsetbeach.service;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.RoomMapper;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomInput;
import com.sunsetbeach.repository.RoomRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RoomService {

    private static final long MAX_FILE_BYTES = 8L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final Path uploadsRoot;

    public RoomService(RoomRepository roomRepository, RoomMapper roomMapper, @Value("${app.uploads.root}") String uploadsRoot) {
        this.roomRepository = roomRepository;
        this.roomMapper = roomMapper;
        this.uploadsRoot = Path.of(uploadsRoot);
    }

    @Transactional(readOnly = true)
    public List<Room> list() {
        return roomRepository.findAll().stream().map(roomMapper::toDto).toList();
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
            throw new ConflictException("This room has existing bookings and can't be deleted.");
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
        for (MultipartFile file : files) {
            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                throw new BadRequestException("Unsupported file type: " + file.getContentType());
            }
            if (file.getSize() > MAX_FILE_BYTES) {
                throw new BadRequestException(file.getOriginalFilename() + " exceeds the 8MB limit");
            }
        }

        Path dir = uploadsRoot.resolve("rooms").resolve(room.getId());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory", e);
        }

        List<String> newPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = sanitizeFilename(file.getOriginalFilename());
            try {
                file.transferTo(dir.resolve(filename));
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

        String prefix = "/uploads/rooms/" + room.getId() + "/";
        if (imagePath.startsWith(prefix)) {
            String filename = imagePath.substring(prefix.length());
            try {
                Files.deleteIfExists(uploadsRoot.resolve("rooms").resolve(room.getId()).resolve(filename));
            } catch (IOException ignored) {
                // best-effort, matches the JS route's .catch(() => {})
            }
        }

        return roomMapper.toDto(saved);
    }

    private static String sanitizeFilename(String originalName) {
        String ext = "";
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) {
                ext = originalName.substring(dot).toLowerCase(Locale.ROOT).replaceAll("[^.\\w]", "");
            }
        }
        StringBuilder random = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            random.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return System.currentTimeMillis() + "-" + random + ext;
    }
}
