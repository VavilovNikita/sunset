package com.sunsetbeach.service;

import com.sunsetbeach.entity.AvailabilityEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.RoomMapper;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomInput;
import com.sunsetbeach.repository.AvailabilityRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;
    private final RoomMapper roomMapper;
    private final Path uploadsRoot;

    public RoomService(
            RoomRepository roomRepository,
            BookingRepository bookingRepository,
            AvailabilityRepository availabilityRepository,
            RoomMapper roomMapper,
            @Value("${app.uploads.root}") String uploadsRoot) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
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

        if (input.getQuantity() < entity.getQuantity()) {
            int peak = peakCommittedUnits(id, LocalDate.now());
            if (input.getQuantity() < peak) {
                throw new ConflictException("Cannot reduce quantity to " + input.getQuantity() + ": " + peak
                        + " units are already committed on at least one future date");
            }
        }

        roomMapper.applyInput(entity, input);
        return roomMapper.toDto(roomRepository.save(entity));
    }

    /**
     * The most units of this room type committed (booked or manually blocked) on any single
     * date from {@code from} onward - a decreasing quantity edit is rejected below this, so the
     * hotel never ends up with more commitments on a date than units to honor them. A sweep-line
     * over booking/block interval boundaries instead of a day-by-day scan, since bookings can
     * span an unbounded horizon: each booking contributes +1 at its (clamped) start and -1 at
     * checkOut, each manual block contributes +blockedCount at its date and -blockedCount the
     * day after; the running total at any event date is exactly bookedCount + blockedCount for
     * every date up to the next event, so its max is the true peak.
     */
    private int peakCommittedUnits(String roomId, LocalDate from) {
        TreeMap<LocalDate, Integer> deltas = new TreeMap<>();

        for (BookingEntity booking : bookingRepository.findByRoomIdAndStatusNotAndCheckOutGreaterThan(roomId, BookingStatus.CANCELLED, from)) {
            LocalDate start = booking.getCheckIn().isBefore(from) ? from : booking.getCheckIn();
            deltas.merge(start, 1, Integer::sum);
            deltas.merge(booking.getCheckOut(), -1, Integer::sum);
        }
        for (AvailabilityEntity block : availabilityRepository.findByRoomIdAndDateGreaterThanEqual(roomId, from)) {
            if (block.getBlockedCount() == 0) {
                continue;
            }
            deltas.merge(block.getDate(), block.getBlockedCount(), Integer::sum);
            deltas.merge(block.getDate().plusDays(1), -block.getBlockedCount(), Integer::sum);
        }

        int running = 0;
        int peak = 0;
        for (int delta : deltas.values()) {
            running += delta;
            peak = Math.max(peak, running);
        }
        return peak;
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

    private static String randomFilename(String extension) {
        StringBuilder random = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            random.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return System.currentTimeMillis() + "-" + random + extension;
    }
}
