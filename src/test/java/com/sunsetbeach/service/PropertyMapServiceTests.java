package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.PropertyMap;
import com.sunsetbeach.model.PropertyMapUnit;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitPositionInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
 * Covers the property map's core arithmetic: a room without a position stays visible (not
 * dropped from the response), a background-image replacement never touches any room's
 * position, and - the bug caught in plan review before this was built - a permanently
 * deactivated room (RoomUnitEntity.isActive=false) and a room temporarily blocked for today
 * (RoomUnitBlockEntity covering today, isActive still true) are reported as two independent
 * facts, never collapsed into one flag. See PropertyMapUnit's own javadoc.
 */
@SpringBootTest
@Transactional
class PropertyMapServiceTests extends AbstractIntegrationTest {

    // Minimal valid 1x1 PNG - same bytes RoomServiceUploadTests uses, enough for Tika to detect image/png.
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

    // A dedicated uploads root, separate from every other @SpringBootTest class (which all share
    // the default app.uploads.root=./uploads) - PropertyMapService.uploadImage really does write
    // files, and rollback-on-@Transactional only undoes the DB row, not the disk write.
    @TempDir
    static Path uploadsRoot;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.uploads.root", () -> uploadsRoot.toString());
    }

    @Autowired
    private PropertyMapService propertyMapService;

    @Autowired
    private RoomUnitService roomUnitService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private RoomUnitBlockRepository roomUnitBlockRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private RoomEntity room;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Property Map Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by PropertyMapServiceTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);

        UserEntity staffUser = new UserEntity();
        staffUser.setEmail("manager-" + UUID.randomUUID() + "@example.com");
        staffUser.setPasswordHash("irrelevant-for-this-test");
        staffUser.setRole(Role.MANAGER);
        staffUser = userRepository.saveAndFlush(staffUser);

        // uploadImage reads the acting user off the security context, same pattern as
        // BookingFolioPaymentTests stubs for recordFolioPayment - no MockMvc/JWT layer here.
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new StaffPrincipal(staffUser.getId(), staffUser.getEmail(), Role.MANAGER), null, List.of()));
    }

    private RoomUnitEntity persistUnit(String label) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel(label + "-" + UUID.randomUUID());
        unit.setActive(true);
        return roomUnitRepository.saveAndFlush(unit);
    }

    // --- Position save/read, including the "not placed" state -----------------------------------

    @Test
    void unitWithNoPosition_appearsInTheMap_withNullCoordinates() {
        persistUnit("203");

        PropertyMap map = propertyMapService.get();

        assertThat(map.getUnits()).isNotEmpty();
        assertThat(map.getUnits()).allSatisfy(u -> {
            assertThat(u.getPositionX().isPresent()).isTrue();
            assertThat(u.getPositionX().get()).isNull();
            assertThat(u.getPositionY().get()).isNull();
        });
    }

    @Test
    void savePositions_thenRead_roundTripsTheCoordinates() {
        RoomUnitEntity unit = persistUnit("204");

        roomUnitService.savePositions(List.of(new RoomUnitPositionInput(unit.getId(), new BigDecimal("0.25"), new BigDecimal("0.75"))));

        PropertyMapUnit dto = findUnit(unit.getId());
        assertThat(dto.getPositionX().get()).isEqualByComparingTo("0.25");
        assertThat(dto.getPositionY().get()).isEqualByComparingTo("0.75");
    }

    @Test
    void savePositions_isBatch_appliesSeveralUnitsInOneCall() {
        RoomUnitEntity a = persistUnit("A");
        RoomUnitEntity b = persistUnit("B");

        roomUnitService.savePositions(List.of(
                new RoomUnitPositionInput(a.getId(), new BigDecimal("0.1"), new BigDecimal("0.2")),
                new RoomUnitPositionInput(b.getId(), new BigDecimal("0.8"), new BigDecimal("0.9"))));

        assertThat(findUnit(a.getId()).getPositionX().get()).isEqualByComparingTo("0.1");
        assertThat(findUnit(b.getId()).getPositionX().get()).isEqualByComparingTo("0.8");
    }

    @Test
    void savePositions_bothNull_resetsAnAlreadyPlacedUnit() {
        RoomUnitEntity unit = persistUnit("205");
        roomUnitService.savePositions(List.of(new RoomUnitPositionInput(unit.getId(), new BigDecimal("0.5"), new BigDecimal("0.5"))));

        roomUnitService.savePositions(List.of(new RoomUnitPositionInput(unit.getId(), null, null)));

        PropertyMapUnit dto = findUnit(unit.getId());
        assertThat(dto.getPositionX().get()).isNull();
        assertThat(dto.getPositionY().get()).isNull();
    }

    @Test
    void savePositions_onlyOneCoordinateSet_isRejected() {
        RoomUnitEntity unit = persistUnit("206");

        assertThatCode(() -> roomUnitService.savePositions(List.of(new RoomUnitPositionInput(unit.getId(), new BigDecimal("0.5"), null))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void savePositions_outOfRange_isRejected() {
        RoomUnitEntity unit = persistUnit("207");

        assertThatCode(() -> roomUnitService.savePositions(List.of(new RoomUnitPositionInput(unit.getId(), new BigDecimal("1.5"), new BigDecimal("0.5")))))
                .isInstanceOf(ValidationException.class);
        assertThatCode(() -> roomUnitService.savePositions(List.of(new RoomUnitPositionInput(unit.getId(), new BigDecimal("-0.1"), new BigDecimal("0.5")))))
                .isInstanceOf(ValidationException.class);
    }

    // --- Replacing the background image must never move a room -----------------------------------

    @Test
    void uploadImage_doesNotChangeAnyUnitPosition() throws IOException {
        RoomUnitEntity unit = persistUnit("208");
        roomUnitService.savePositions(List.of(new RoomUnitPositionInput(unit.getId(), new BigDecimal("0.42"), new BigDecimal("0.58"))));

        MockMultipartFile file = new MockMultipartFile("file", "plan.jpg", "image/jpeg", PNG_BYTES);
        PropertyMap first = propertyMapService.uploadImage(file);
        assertThat(first.getImagePath().get()).isNotNull();

        // Replace again with a second upload - still must not disturb the position.
        MockMultipartFile secondFile = new MockMultipartFile("file", "plan2.jpg", "image/jpeg", PNG_BYTES);
        propertyMapService.uploadImage(secondFile);

        PropertyMapUnit dto = findUnit(unit.getId());
        assertThat(dto.getPositionX().get()).isEqualByComparingTo("0.42");
        assertThat(dto.getPositionY().get()).isEqualByComparingTo("0.58");
    }

    // --- The bug from plan review: isActive and a today-covering RoomUnitBlock are independent --

    @Test
    void deactivatedUnit_and_blockedTodayUnit_areReportedAsDifferentFacts() {
        RoomUnitEntity deactivated = persistUnit("209");
        deactivated.setActive(false);
        roomUnitRepository.saveAndFlush(deactivated);

        RoomUnitEntity blocked = persistUnit("210");
        RoomUnitBlockEntity block = new RoomUnitBlockEntity();
        block.setRoomUnitId(blocked.getId());
        block.setFromDate(LocalDate.now().minusDays(1));
        block.setToDate(LocalDate.now().plusDays(3));
        block.setReason("AC is leaking");
        roomUnitBlockRepository.saveAndFlush(block);

        PropertyMapUnit deactivatedDto = findUnit(deactivated.getId());
        assertThat(deactivatedDto.getIsActive()).isFalse();
        assertThat(deactivatedDto.getActiveBlock()).isNull();

        PropertyMapUnit blockedDto = findUnit(blocked.getId());
        assertThat(blockedDto.getIsActive()).isTrue();
        assertThat(blockedDto.getActiveBlock()).isNotNull();
        assertThat(blockedDto.getActiveBlock().getReason()).isEqualTo("AC is leaking");
    }

    @Test
    void unitThatIsBothDeactivatedAndBlocked_stillReportsBothFactsIndependently() {
        RoomUnitEntity unit = persistUnit("211");
        unit.setActive(false);
        roomUnitRepository.saveAndFlush(unit);

        RoomUnitBlockEntity block = new RoomUnitBlockEntity();
        block.setRoomUnitId(unit.getId());
        block.setFromDate(LocalDate.now());
        block.setToDate(LocalDate.now());
        block.setReason("Flooded");
        roomUnitBlockRepository.saveAndFlush(block);

        PropertyMapUnit dto = findUnit(unit.getId());
        assertThat(dto.getIsActive()).isFalse();
        assertThat(dto.getActiveBlock()).isNotNull();
        assertThat(dto.getActiveBlock().getReason()).isEqualTo("Flooded");
    }

    @Test
    void blockNotCoveringToday_doesNotAppearAsActiveBlock() {
        RoomUnitEntity unit = persistUnit("212");
        RoomUnitBlockEntity pastBlock = new RoomUnitBlockEntity();
        pastBlock.setRoomUnitId(unit.getId());
        pastBlock.setFromDate(LocalDate.now().minusDays(10));
        pastBlock.setToDate(LocalDate.now().minusDays(5));
        pastBlock.setReason("Past maintenance");
        roomUnitBlockRepository.saveAndFlush(pastBlock);

        assertThat(findUnit(unit.getId()).getActiveBlock()).isNull();
    }

    // --- Occupancy/debt reuse BookingService's own outstanding-balance arithmetic -----------------

    @Test
    void checkedInGuest_showsAsCurrentBooking_withOutstandingBalance() {
        RoomUnitEntity unit = persistUnit("213");
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setRoomUnitId(unit.getId());
        booking.setGuestName("Somchai");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.now().minusDays(1));
        booking.setCheckOut(LocalDate.now().plusDays(2));
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setOccupancyStatus(OccupancyStatus.CHECKED_IN);
        bookingRepository.saveAndFlush(booking);

        PropertyMapUnit dto = findUnit(unit.getId());
        assertThat(dto.getCurrentBooking()).isNotNull();
        assertThat(dto.getCurrentBooking().getGuestName()).isEqualTo("Somchai");
        assertThat(dto.getCurrentBooking().getOccupancyStatus()).isEqualTo(OccupancyStatus.CHECKED_IN);
        assertThat(new BigDecimal(dto.getCurrentBooking().getOutstandingBalance())).isEqualByComparingTo("1000.00");
    }

    @Test
    void vacantUnit_hasNoCurrentBooking() {
        RoomUnitEntity unit = persistUnit("214");
        assertThat(findUnit(unit.getId()).getCurrentBooking()).isNull();
    }

    private PropertyMapUnit findUnit(String roomUnitId) {
        Optional<PropertyMapUnit> found =
                propertyMapService.get().getUnits().stream().filter(u -> u.getRoomUnitId().equals(roomUnitId)).findFirst();
        assertThat(found).isPresent();
        return found.get();
    }
}
