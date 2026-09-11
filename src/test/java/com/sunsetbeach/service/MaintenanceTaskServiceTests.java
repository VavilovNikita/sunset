package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.MaintenanceTask;
import com.sunsetbeach.model.MaintenanceTaskBlockResult;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * A task always names a physical room; a block is optional and set separately (see
 * MaintenanceTaskBlockLiftingTests for the close-time block-shortening behaviour). Any
 * authenticated staff role may file/read a task (enforced by SecurityConfig, not here) - this
 * covers the service-level behaviour: photo validation/storage (reusing ImageUploadValidator,
 * same as RoomService/PropertyMapService), the block link being optional and reused
 * (RoomUnitService#createBlock, same overlap warning), and status transitions being forward-only.
 */
@SpringBootTest
@Transactional
class MaintenanceTaskServiceTests extends AbstractIntegrationTest {

    // Minimal valid 1x1 PNG - same bytes RoomServiceUploadTests/PropertyMapServiceTests use.
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

    // A dedicated uploads root, separate from every other @SpringBootTest class - photo files
    // are real disk writes that @Transactional rollback does not undo (same note as
    // PropertyMapServiceTests's own uploadsRoot).
    @TempDir
    static Path uploadsRoot;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.uploads.root", () -> uploadsRoot.toString());
    }

    @Autowired
    private MaintenanceTaskService maintenanceTaskService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private UserRepository userRepository;

    private RoomUnitEntity unit;
    private String reporterId;

    @BeforeEach
    void setUp() {
        RoomEntity room = new RoomEntity();
        room.setName("Maintenance Task Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by MaintenanceTaskServiceTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        RoomUnitEntity newUnit = new RoomUnitEntity();
        newUnit.setRoomId(savedRoom.getId());
        newUnit.setLabel("Maintenance Task Test Unit " + UUID.randomUUID());
        newUnit.setActive(true);
        unit = roomUnitRepository.saveAndFlush(newUnit);

        UserEntity reporter = new UserEntity();
        reporter.setEmail("reporter-" + UUID.randomUUID() + "@example.com");
        reporter.setPasswordHash("irrelevant-for-this-test");
        reporter = userRepository.saveAndFlush(reporter);
        reporterId = reporter.getId();
    }

    // --- create -----------------------------------------------------------------------------

    @Test
    void create_withPhoto_storesFileAndReturnsItsServedPath() {
        MockMultipartFile photo = new MockMultipartFile("photos", "leak.jpg", "image/jpeg", PNG_BYTES);

        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "AC is leaking", List.of(photo), reporterId);

        assertThat(task.getStatus()).isEqualTo(MaintenanceTaskStatus.OPEN);
        assertThat(task.getBlockId().get()).isNull();
        assertThat(task.getRoomUnitId()).isEqualTo(unit.getId());
        assertThat(task.getUnitLabel()).isEqualTo(unit.getLabel());
        assertThat(task.getReportedByUserId()).isEqualTo(reporterId);
        assertThat(task.getPhotos()).hasSize(1);
        String servedPath = task.getPhotos().get(0);
        assertThat(servedPath).isEqualTo("/maintenance-tasks/" + task.getId() + "/photos/" + servedPath.substring(servedPath.lastIndexOf('/') + 1));

        String filename = servedPath.substring(servedPath.lastIndexOf('/') + 1);
        assertThat(Files.exists(uploadsRoot.resolve("maintenance-tasks").resolve(task.getId()).resolve(filename))).isTrue();
    }

    @Test
    void create_withoutPhotos_succeedsWithEmptyPhotoList() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "Dead lightbulb", List.of(), reporterId);

        assertThat(task.getPhotos()).isEmpty();
    }

    @Test
    void create_blankDescription_isRejected() {
        assertThatThrownBy(() -> maintenanceTaskService.create(unit.getId(), "   ", List.of(), reporterId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_unknownRoomUnit_isNotFound() {
        assertThatThrownBy(() -> maintenanceTaskService.create("no-such-unit", "Broken thing", List.of(), reporterId))
                .isInstanceOf(NotFoundException.class);
    }

    // --- resolvePhoto -------------------------------------------------------------------------

    @Test
    void resolvePhoto_ordinaryPhoto_isServed() {
        MockMultipartFile photo = new MockMultipartFile("photos", "leak.jpg", "image/jpeg", PNG_BYTES);
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "AC is leaking", List.of(photo), reporterId);
        String servedPath = task.getPhotos().get(0);
        String filename = servedPath.substring(servedPath.lastIndexOf('/') + 1);

        assertThat(maintenanceTaskService.resolvePhoto(task.getId(), filename).exists()).isTrue();
    }

    @Test
    void resolvePhoto_unrecordedFilename_isNotFound() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "Dead lightbulb", List.of(), reporterId);

        assertThatThrownBy(() -> maintenanceTaskService.resolvePhoto(task.getId(), "not-a-real-file.jpg")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void resolvePhoto_pathTraversalFilename_isNotFound() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "Dead lightbulb", List.of(), reporterId);

        assertThatThrownBy(() -> maintenanceTaskService.resolvePhoto(task.getId(), "../../secret.txt")).isInstanceOf(NotFoundException.class);
    }

    // --- addBlock: reuses RoomUnitService#createBlock as-is -----------------------------------

    @Test
    void addBlock_linksBlockAndSurfacesTheSameOverlapWarning() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "AC is leaking", List.of(), reporterId);
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(unit.getRoomId(), "Blocked Room Guest", checkIn.toString(), checkOut.toString()).roomUnitId(unit.getId()));

        MaintenanceTaskBlockResult result =
                maintenanceTaskService.addBlock(task.getId(), new RoomUnitBlockInput(checkIn.toString(), checkOut.toString(), "AC repair"));

        assertThat(result.getTask().getBlockId().get()).isEqualTo(result.getBlockResult().getBlock().getId());
        assertThat(result.getBlockResult().getWarning().get()).isNotNull();
        assertThat(result.getBlockResult().getAffectedBookings()).extracting("bookingId").containsExactly(booking.getId());

        MaintenanceTask reloaded =
                maintenanceTaskService.list().stream().filter(t -> t.getId().equals(task.getId())).findFirst().orElseThrow();
        assertThat(reloaded.getBlockId().get()).isEqualTo(result.getBlockResult().getBlock().getId());
    }

    @Test
    void addBlock_whenTaskAlreadyHasOne_isRejected() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "AC is leaking", List.of(), reporterId);
        LocalDate from = LocalDate.now().plusDays(5);
        LocalDate to = from.plusDays(1);
        maintenanceTaskService.addBlock(task.getId(), new RoomUnitBlockInput(from.toString(), to.toString(), "AC repair"));

        assertThatThrownBy(() -> maintenanceTaskService.addBlock(task.getId(), new RoomUnitBlockInput(from.toString(), to.toString(), "Second block")))
                .isInstanceOf(BadRequestException.class);
    }

    // --- updateStatus: forward-only -------------------------------------------------------------

    @Test
    void updateStatus_openToInProgress_succeeds() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "Dead lightbulb", List.of(), reporterId);

        MaintenanceTask updated = maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.IN_PROGRESS);

        assertThat(updated.getStatus()).isEqualTo(MaintenanceTaskStatus.IN_PROGRESS);
        assertThat(updated.getClosedAt().get()).isNull();
    }

    @Test
    void updateStatus_openDirectlyToDone_skipsInProgress_andSucceeds() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "Dead lightbulb", List.of(), reporterId);

        MaintenanceTask updated = maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.DONE);

        assertThat(updated.getStatus()).isEqualTo(MaintenanceTaskStatus.DONE);
        assertThat(updated.getClosedAt().get()).isNotNull();
    }

    @Test
    void updateStatus_repeatingCurrentStatus_isRejected() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "Dead lightbulb", List.of(), reporterId);

        assertThatThrownBy(() -> maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.OPEN)).isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_backwardFromInProgressToOpen_isRejected() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "Dead lightbulb", List.of(), reporterId);
        maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.IN_PROGRESS);

        assertThatThrownBy(() -> maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.OPEN)).isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_fromDone_isAlwaysRejected() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "Dead lightbulb", List.of(), reporterId);
        maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.DONE);

        assertThatThrownBy(() -> maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.IN_PROGRESS)).isInstanceOf(ConflictException.class);
    }

}
