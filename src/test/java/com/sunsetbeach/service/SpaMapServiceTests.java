package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaMap;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.TablePositionInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mirrors {@code PropertyMapServiceTests}' own upload-image coverage - the same mechanism
 * ({@link ImageUploadValidator}), applied to a separate singleton row (see {@link SpaMapEntity}'s
 * own javadoc for why this isn't a reuse of {@code PropertyMapEntity}). The one guarantee worth
 * testing twice: replacing the spa map's image must never disturb a SPA-zone table's own
 * position, same as the property map's image never disturbs a RoomUnit's.
 */
@SpringBootTest
@Transactional
class SpaMapServiceTests extends AbstractIntegrationTest {

    // Minimal valid 1x1 PNG - same bytes PropertyMapServiceTests/RoomServiceUploadTests use.
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

    // A dedicated uploads root, separate from every other @SpringBootTest class - real disk
    // writes that @Transactional rollback does not undo, same note as PropertyMapServiceTests'.
    @TempDir
    static Path uploadsRoot;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.uploads.root", () -> uploadsRoot.toString());
    }

    @Autowired
    private SpaMapService spaMapService;

    @Autowired
    private TableService tableService;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        UserEntity staffUser = new UserEntity();
        staffUser.setEmail("manager-" + UUID.randomUUID() + "@example.com");
        staffUser.setPasswordHash("irrelevant-for-this-test");
        staffUser.setRole(Role.MANAGER);
        staffUser = userRepository.saveAndFlush(staffUser);

        // uploadImage reads the acting user off the security context - same stub as
        // PropertyMapServiceTests, no MockMvc/JWT layer here.
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new StaffPrincipal(staffUser.getId(), staffUser.getEmail(), Role.MANAGER), null, List.of()));
    }

    private TableEntity persistSpaTable(String label) {
        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel(label + "-" + UUID.randomUUID());
        table.setCapacity(1);
        table.setActive(true);
        return tableRepository.saveAndFlush(table);
    }

    @Test
    void get_withNoImageUploaded_returnsNullImagePath() {
        SpaMap map = spaMapService.get();

        assertThat(map.getImagePath().get()).isNull();
        assertThat(map.getImageUpdatedAt().get()).isNull();
    }

    @Test
    void resolveImage_withNoImageUploaded_isNotFound() {
        assertThatThrownBy(() -> spaMapService.resolveImage()).isInstanceOf(NotFoundException.class);
    }

    @Test
    void uploadImage_thenGet_roundTripsANonNullImagePath() {
        MockMultipartFile file = new MockMultipartFile("file", "spa-plan.jpg", "image/jpeg", PNG_BYTES);

        SpaMap uploaded = spaMapService.uploadImage(file);

        assertThat(uploaded.getImagePath().get()).isNotNull();
        assertThat(uploaded.getImageUpdatedAt().get()).isNotNull();
        assertThat(spaMapService.get().getImagePath().get()).isEqualTo(uploaded.getImagePath().get());
    }

    @Test
    void uploadImage_thenResolveImage_isReadable() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "spa-plan.png", "image/png", PNG_BYTES);
        spaMapService.uploadImage(file);

        var resource = spaMapService.resolveImage();

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    /** The guarantee worth testing twice - see this class's own javadoc. */
    @Test
    void uploadImage_doesNotChangeAnySpaTablePosition() {
        TableEntity table = persistSpaTable("Table 1");
        tableService.savePositions(List.of(new TablePositionInput(table.getId()).positionX(new BigDecimal("0.42")).positionY(new BigDecimal("0.58"))));

        MockMultipartFile first = new MockMultipartFile("file", "plan.jpg", "image/jpeg", PNG_BYTES);
        spaMapService.uploadImage(first);
        // Replace again with a second upload - still must not disturb the position.
        MockMultipartFile second = new MockMultipartFile("file", "plan2.jpg", "image/jpeg", PNG_BYTES);
        spaMapService.uploadImage(second);

        Table reloaded = findTable(table.getId());
        assertThat(reloaded.getPositionX().get()).isEqualByComparingTo("0.42");
        assertThat(reloaded.getPositionY().get()).isEqualByComparingTo("0.58");
    }

    private Table findTable(String tableId) {
        return tableService.list().stream().filter(t -> t.getId().equals(tableId)).findFirst().orElseThrow();
    }
}
