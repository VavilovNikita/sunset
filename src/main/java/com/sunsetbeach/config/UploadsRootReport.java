package com.sunsetbeach.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Says at every start where uploaded files (room photos, floor plans, spa and restaurant maps,
 * maintenance photos) are read from and written to, as an absolute path, and whether anything is
 * there. {@code app.uploads.root} defaults to a path relative to the working directory
 * ({@code ./uploads}), so the same code looks in a different place when started from the IDE,
 * from {@code target/} or from the container's {@code /app} - and "the uploads are gone" was
 * really "the app is looking somewhere else" (or at a fresh, unmounted container directory). The
 * database keeps the image paths, so a missing file only shows up later as a 404 on a map. One
 * line at startup makes that visible before anyone uploads a second plan.
 *
 * <p>Only reports; never creates, moves or deletes anything, and never stops the application.
 */
@Component
public class UploadsRootReport implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UploadsRootReport.class);

    private final Path root;

    public UploadsRootReport(@Value("${app.uploads.root}") String uploadsRoot) {
        this.root = Path.of(uploadsRoot).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Files.isDirectory(root)) {
            log.warn("Uploads root {} does not exist yet - uploaded images the database already references will 404 until it is mounted/restored. "
                    + "In Docker it must be the persistent volume mounted at /app/uploads (see DEPLOY.md, \"Uploads\").", root);
            return;
        }
        try (Stream<Path> files = Files.walk(root)) {
            long count = files.filter(Files::isRegularFile).count();
            if (count == 0) {
                log.warn("Uploads root {} exists but holds no files. If images were uploaded before, this is not where they were stored "
                        + "(relative UPLOADS_ROOT started from another directory, or the Docker volume is not mounted). See DEPLOY.md, \"Uploads\".", root);
            } else {
                log.info("Uploads root {} holds {} file(s).", root, count);
            }
        } catch (IOException e) {
            log.warn("Could not read the uploads root {}: {}", root, e.getMessage());
        }
    }
}
