package com.sunsetbeach.service;

import com.sunsetbeach.entity.SpaMapEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.SpaMap;
import com.sunsetbeach.repository.SpaMapRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The spa's own floor-plan background image - see {@link SpaMap}'s own openapi.yaml description
 * for why this is a separate image from the property map's, not a reuse of it. Mirrors {@link
 * PropertyMapService}'s upload/serve mechanism exactly ({@link ImageUploadValidator}, the same
 * staff-only (not {@code /uploads/**}) serving path, the same replace-and-best-effort-delete-the-
 * old-file behaviour) - deliberately not a shared component between the two, since the two
 * images have nothing else in common (no rooms/units, no booking/occupancy data) and forcing a
 * shared abstraction over just the upload/serve plumbing would cost more than it saves for two
 * call sites this small.
 */
@Service
public class SpaMapService {

    // Single row - see V47__spa_map.sql.
    private static final String SINGLETON_ID = "default";

    private final SpaMapRepository spaMapRepository;
    private final ImageUploadValidator imageUploadValidator;
    private final AuditLogService auditLogService;
    private final Path uploadsRoot;

    public SpaMapService(
            SpaMapRepository spaMapRepository,
            ImageUploadValidator imageUploadValidator,
            AuditLogService auditLogService,
            @Value("${app.uploads.root}") String uploadsRoot) {
        this.spaMapRepository = spaMapRepository;
        this.imageUploadValidator = imageUploadValidator;
        this.auditLogService = auditLogService;
        this.uploadsRoot = Path.of(uploadsRoot);
    }

    @Transactional(readOnly = true)
    public SpaMap get() {
        SpaMapEntity map = spaMapRepository.findById(SINGLETON_ID).orElse(null);
        String imagePath = map != null ? map.getImagePath() : null;
        return new SpaMap(imagePath, map != null ? TimestampFormat.toUtc(map.getUpdatedAt()) : null);
    }

    @Transactional
    public SpaMap uploadImage(MultipartFile file) {
        ImageUploadValidator.ValidatedImage validated = imageUploadValidator.validate(file);

        Path dir = uploadsRoot.resolve("spa-map");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory", e);
        }
        String filename = imageUploadValidator.randomFilename(validated.extension());
        try {
            Files.write(dir.resolve(filename), validated.bytes());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write uploaded file", e);
        }

        SpaMapEntity entity = spaMapRepository.findById(SINGLETON_ID).orElseGet(SpaMapEntity::new);
        entity.setId(SINGLETON_ID);
        String oldFilename = entity.getImagePath();
        StaffPrincipal actor = (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        entity.setImagePath(filename);
        entity.setUpdatedByUserId(actor.id());
        spaMapRepository.saveAndFlush(entity);

        // Best-effort: replacing the plan must never fail because the old file couldn't be
        // removed - matches PropertyMapService#uploadImage/RoomService.deleteImage's own
        // best-effort disk cleanup.
        if (oldFilename != null) {
            try {
                Files.deleteIfExists(dir.resolve(oldFilename));
            } catch (IOException ignored) {
                // best-effort
            }
        }

        auditLogService.record(AuditAction.SPA_MAP_IMAGE_UPDATED, AuditEntityType.SPA_MAP, SINGLETON_ID, "Spa map background image replaced");

        return get();
    }

    @Transactional(readOnly = true)
    public Resource resolveImage() {
        SpaMapEntity entity = spaMapRepository.findById(SINGLETON_ID).orElseThrow(() -> new NotFoundException("No spa map image has been uploaded yet"));

        Path dir = uploadsRoot.resolve("spa-map").toAbsolutePath().normalize();
        Path file = dir.resolve(entity.getImagePath()).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            throw new NotFoundException("No spa map image has been uploaded yet");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("No spa map image has been uploaded yet");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("No spa map image has been uploaded yet");
        }
    }
}
