package com.sunsetbeach.service;

import com.sunsetbeach.error.NotFoundException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

/**
 * Serves files written by {@code RoomService.uploadImages} under
 * {@code UPLOADS_ROOT/rooms/{roomId}/...}. Mirrors static serving of the Next.js app's
 * public/uploads/rooms/{id}/ folder - doesn't check whether {@code roomId} still exists as a
 * Room row, only that the resolved file exists under that room's own upload directory.
 */
@Service
public class RoomImageService {

    private final Path roomsRoot;

    public RoomImageService(@Value("${app.uploads.root}") String uploadsRoot) {
        this.roomsRoot = Path.of(uploadsRoot).toAbsolutePath().normalize().resolve("rooms");
    }

    public Resource resolve(String roomId, String filename) {
        // Path variables can carry an encoded ".." or absolute path; reject any segment
        // separator outright before even touching the filesystem.
        if (containsSeparator(roomId) || containsSeparator(filename)) {
            throw new NotFoundException("Not found");
        }

        Path roomDir = roomsRoot.resolve(roomId).normalize();
        if (!roomDir.startsWith(roomsRoot)) {
            throw new NotFoundException("Not found");
        }

        Path file = roomDir.resolve(filename).normalize();
        if (!file.startsWith(roomDir) || !Files.isRegularFile(file)) {
            throw new NotFoundException("Not found");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("Not found");
        }
    }

    private static boolean containsSeparator(String segment) {
        return segment.contains("/") || segment.contains("\\");
    }
}
