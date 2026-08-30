package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.mapper.RoomMapper;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Covers the room-photo upload path's content-based type check: acceptance/extension must
 * follow what's actually in the file's bytes, not the client-supplied Content-Type or the
 * original filename - otherwise an SVG (which can carry a <script>) mislabeled as a JPEG
 * would be stored and served as one.
 */
class RoomServiceUploadTests {

    // Minimal valid 1x1 PNG (magic bytes + IHDR/IEND), enough for Tika to detect image/png.
    private static final byte[] PNG_BYTES = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
        (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
        0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00,
        0x00, 0x00, 0x03, 0x00, 0x01, 0x18, (byte) 0xDD, (byte) 0x8D,
        (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
        0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    private static final byte[] SVG_WITH_SCRIPT_BYTES =
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(document.domain)</script></svg>"
                    .getBytes(StandardCharsets.UTF_8);

    private RoomService newService(Path uploadsRoot, RoomRepository roomRepository) {
        return new RoomService(roomRepository, Mockito.mock(RoomUnitRepository.class), new RoomMapper(Mockito.mock(RoomUnitRepository.class)),
                uploadsRoot.toString(), Mockito.mock(AuditLogService.class));
    }

    @Test
    void uploadImages_realPng_isStoredWithPngExtensionRegardlessOfClaimedType(@TempDir Path uploadsRoot) throws IOException {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        RoomEntity room = new RoomEntity();
        room.setId("room-1");
        room.setBasePrice(BigDecimal.TEN);
        ReflectionTestUtils.setField(room, "createdAt", LocalDateTime.now());
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomService service = newService(uploadsRoot, roomRepository);

        MockMultipartFile file = new MockMultipartFile(
                "files", "photo.jpg", "image/jpeg", PNG_BYTES);

        Room result = service.uploadImages("room-1", List.of(file));

        assertThat(result.getImages()).hasSize(1);
        String storedPath = result.getImages().get(0);
        assertThat(storedPath).endsWith(".png");
        assertThat(Files.exists(uploadsRoot.resolve("rooms").resolve("room-1")
                .resolve(storedPath.substring(storedPath.lastIndexOf('/') + 1)))).isTrue();
    }

    @Test
    void uploadImages_svgMislabeledAsJpeg_isRejected(@TempDir Path uploadsRoot) {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        RoomEntity room = new RoomEntity();
        room.setId("room-1");
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));

        RoomService service = newService(uploadsRoot, roomRepository);

        MockMultipartFile file = new MockMultipartFile(
                "files", "photo.jpg", "image/jpeg", SVG_WITH_SCRIPT_BYTES);

        assertThatThrownBy(() -> service.uploadImages("room-1", List.of(file)))
                .isInstanceOf(BadRequestException.class);

        assertThat(Files.exists(uploadsRoot.resolve("rooms").resolve("room-1"))).isFalse();
    }

    // --- deleteImage: path traversal via a crafted `path` value ---

    @Test
    void deleteImage_pathTraversalOutsideRoomDirectory_doesNotDeleteTheEscapedFile(@TempDir Path uploadsRoot) throws IOException {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        RoomEntity room = new RoomEntity();
        room.setId("room-1");
        room.setBasePrice(BigDecimal.TEN);
        ReflectionTestUtils.setField(room, "createdAt", LocalDateTime.now());
        room.setImages(new String[] {"/uploads/rooms/room-1/../../../secret.txt"});
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // A file sitting just outside uploadsRoot - the traversal path's actual target once
        // resolved - must survive the delete call.
        Path secretFile = uploadsRoot.getParent().resolve("secret-" + System.nanoTime() + ".txt");
        Files.writeString(secretFile, "should not be touched");
        // Rewrite room.images to point at this run's actual escaped filename so the test isn't
        // sensitive to how many ".." segments happen to reach the temp dir's parent.
        String traversal = "/uploads/rooms/room-1/../../../" + secretFile.getFileName();
        room.setImages(new String[] {traversal});

        try {
            RoomService service = newService(uploadsRoot, roomRepository);

            service.deleteImage("room-1", traversal);

            assertThat(Files.exists(secretFile)).isTrue();
        } finally {
            Files.deleteIfExists(secretFile);
        }
    }

    @Test
    void deleteImage_ordinaryPath_deletesTheFileWithinTheRoomDirectory(@TempDir Path uploadsRoot) throws IOException {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        RoomEntity room = new RoomEntity();
        room.setId("room-1");
        room.setBasePrice(BigDecimal.TEN);
        ReflectionTestUtils.setField(room, "createdAt", LocalDateTime.now());
        String imagePath = "/uploads/rooms/room-1/photo.jpg";
        room.setImages(new String[] {imagePath});
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Path roomDir = uploadsRoot.resolve("rooms").resolve("room-1");
        Files.createDirectories(roomDir);
        Files.writeString(roomDir.resolve("photo.jpg"), "fake-jpeg-bytes");

        RoomService service = newService(uploadsRoot, roomRepository);
        service.deleteImage("room-1", imagePath);

        assertThat(Files.exists(roomDir.resolve("photo.jpg"))).isFalse();
    }
}
