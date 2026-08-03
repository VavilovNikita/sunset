package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.error.NotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

class RoomImageServiceTest {

    @TempDir
    Path uploadsRoot;

    private RoomImageService service;
    private Path roomDir;

    @BeforeEach
    void setUp() throws IOException {
        service = new RoomImageService(uploadsRoot.toString());
        roomDir = uploadsRoot.resolve("rooms").resolve("room-1");
        Files.createDirectories(roomDir);
        Files.writeString(roomDir.resolve("photo.jpg"), "fake-jpeg-bytes");
    }

    @Test
    void resolvesAnExistingFile() {
        Resource resource = service.resolve("room-1", "photo.jpg");
        assertThat(resource.exists()).isTrue();
    }

    @Test
    void missingFile_throwsNotFound() {
        assertThatThrownBy(() -> service.resolve("room-1", "does-not-exist.jpg")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void missingRoom_throwsNotFound() {
        assertThatThrownBy(() -> service.resolve("no-such-room", "photo.jpg")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void dotDotFilename_cannotEscapeRoomDirectory() throws IOException {
        // A file that genuinely exists one level up from the room dir must stay unreachable.
        Files.writeString(uploadsRoot.resolve("rooms").resolve("secret.txt"), "top secret");
        assertThatThrownBy(() -> service.resolve("room-1", "..%2Fsecret.txt".replace("%2F", "/")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.resolve("room-1", "..\\secret.txt")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void dotDotRoomId_cannotEscapeUploadsRoot() throws IOException {
        Files.writeString(uploadsRoot.resolve("outside.txt"), "outside uploads root");
        assertThatThrownBy(() -> service.resolve("..", "outside.txt")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.resolve("../..", "outside.txt")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void directoryIsNotServedAsAFile() {
        assertThatThrownBy(() -> service.resolve("room-1", ".")).isInstanceOf(NotFoundException.class);
    }
}
