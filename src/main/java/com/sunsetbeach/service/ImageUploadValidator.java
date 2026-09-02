package com.sunsetbeach.service;

import com.sunsetbeach.error.BadRequestException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Shared image-upload validation, originally written for {@link RoomService#uploadImages} and
 * extracted here so {@code PropertyMapService} can reuse the exact same mechanism rather than
 * duplicate it - same 8MB cap, same content-sniffed (not client-header-trusted) type check, same
 * randomized filename scheme.
 */
@Component
public class ImageUploadValidator {

    private static final long MAX_FILE_BYTES = 8L * 1024 * 1024;
    // Keyed by the type Tika detects from actual file content (magic bytes), not the
    // client-supplied Content-Type header - this is what decides both acceptance and the
    // stored file's extension, so a relabeled .svg can't slip through as a photo/floor plan.
    private static final Map<String, String> ALLOWED_TYPE_EXTENSIONS =
            Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private static final Tika TIKA = new Tika();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";

    public record ValidatedImage(byte[] bytes, String extension) {}

    public ValidatedImage validate(MultipartFile file) {
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
        return new ValidatedImage(bytes, extension);
    }

    public String randomFilename(String extension) {
        StringBuilder random = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            random.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return System.currentTimeMillis() + "-" + random + extension;
    }
}
