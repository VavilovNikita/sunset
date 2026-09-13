package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.SpaAppointmentTreatmentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaMap;
import com.sunsetbeach.model.SpaMapTable;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.TablePositionInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.SpaAppointmentTreatmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
 *
 * <p><b>Time is fixed, not real.</b> {@link SpaMapService#get()} reads {@code Clock} to decide
 * "is this appointment upcoming" - this class overrides that bean with a fixed one ({@link
 * FixedClockConfig}) instead of building appointment times off {@code LocalTime.now()}. An
 * earlier version of this test did exactly that ({@code LocalTime.now().plusHours(2)}, with
 * {@code persistAppointment} always dating the row to {@code LocalDate.now()}), and it was wrong:
 * adding hours to a {@code LocalTime} wraps at midnight without ever rolling the date over, so
 * whenever the suite happened to run within about two hours of midnight, the "2 hours from now"
 * appointment silently landed on *this morning*, hours in the past, and
 * {@code nextAppointmentStartTime} correctly reported nothing. That was chased down as a
 * suspected production bug first (per this project's own "confirm before changing" rule) -
 * {@link SpaMapService}'s date/time comparisons turned out to be correct for every appointment
 * the real API can actually produce ({@code SpaAppointmentService#validateWithinOpeningHours}
 * keeps every real appointment's time comfortably inside business hours, nowhere near midnight,
 * and always carries its own explicit, correct date) - the bug was entirely in this test
 * constructing a same-day time that {@code validateWithinOpeningHours} would have rejected had it
 * gone through the real service instead of writing the row directly. Fixed at the root instead of
 * patched around: this class now controls "now" completely, so no fact it asserts can ever depend
 * on when the suite happens to run.
 */
@SpringBootTest
@Transactional
class SpaMapServiceTests extends AbstractIntegrationTest {

    // A fixed, arbitrary weekday afternoon - safely mid-day so every offset used below (-3h to
    // +4h) stays on the same calendar date in both directions, with no midnight wraparound to
    // reason about. FIXED_DATE is what persistAppointment dates every row to; FIXED_TIME is the
    // anchor every test builds its own appointment's startTime from.
    private static final LocalDate FIXED_DATE = LocalDate.of(2027, 6, 15);
    private static final LocalTime FIXED_TIME = LocalTime.of(14, 0);

    // Registered automatically by @SpringBootTest as a nested @TestConfiguration - @Primary wins
    // the by-type injection into SpaMapService's constructor over ClockConfig's own real-clock
    // bean, without needing to name this bean "clock" (which would collide with it instead).
    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            Instant fixedInstant = LocalDateTime.of(FIXED_DATE, FIXED_TIME).atZone(ZoneId.systemDefault()).toInstant();
            return Clock.fixed(fixedInstant, ZoneId.systemDefault());
        }
    }

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

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private SpaAppointmentRepository spaAppointmentRepository;

    @Autowired
    private SpaAppointmentTreatmentRepository spaAppointmentTreatmentRepository;

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

    private Booking createBooking() {
        RoomEntity room = new RoomEntity();
        room.setName("Spa Map Test Room " + UUID.randomUUID());
        room.setDescription("Used only by SpaMapServiceTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Spa Map Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);
        LocalDate checkIn = LocalDate.now();
        return bookingService.createStaffBooking(
                new StaffBookingCreateInput(savedRoom.getId(), "Spa Map Test Guest", checkIn.toString(), checkIn.plusDays(3).toString())
                        .roomUnitId(savedUnit.getId()));
    }

    private MenuItemEntity createTreatment(int durationMinutes) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Spa Map Treatment " + UUID.randomUUID());
        item.setDescription("Used only by SpaMapServiceTests");
        item.setCategory("Massage");
        item.setDepartment(MenuDepartment.SPA);
        item.setPrice(new BigDecimal("1500.00"));
        item.setAvailable(true);
        item.setDurationMinutes(durationMinutes);
        return menuItemRepository.saveAndFlush(item);
    }

    private UserEntity createTherapist() {
        UserEntity user = new UserEntity();
        user.setEmail("spa-map-therapist-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant");
        user.setRole(Role.WAITER);
        user.setActive(true);
        user.setJobFunctions(new String[] {JobFunction.THERAPIST.getValue()});
        return userRepository.saveAndFlush(user);
    }

    /**
     * Persisted directly (bypassing SpaAppointmentService's opening-hours validation, same
     * reasoning as OrderSpaAppointmentLinkTests) so startTime can be built relative to
     * FIXED_TIME - a real appointment could never land at some of the times these tests use
     * (well outside business hours by design, see this class's own javadoc), but SpaMapService
     * itself doesn't care where an appointment came from, only what its date/time say.
     */
    private void persistAppointment(
            TableEntity table, Booking booking, MenuItemEntity treatment, UserEntity therapist, LocalTime startTime, int durationMinutes, SpaAppointmentStatus status) {
        SpaAppointmentEntity entity = new SpaAppointmentEntity();
        entity.setTableId(table.getId());
        entity.setBookingId(booking.getId());
        entity.setTherapistUserId(therapist.getId());
        entity.setCreatedByUserId(therapist.getId()); // any real User id - actor isn't under test here
        entity.setDate(FIXED_DATE);
        entity.setStartTime(startTime);
        entity.setDurationMinutes(durationMinutes);
        entity.setStatus(status);
        SpaAppointmentEntity saved = spaAppointmentRepository.saveAndFlush(entity);

        SpaAppointmentTreatmentEntity treatmentRow = new SpaAppointmentTreatmentEntity();
        treatmentRow.setSpaAppointmentId(saved.getId());
        treatmentRow.setTreatmentMenuItemId(treatment.getId());
        treatmentRow.setDurationMinutes(durationMinutes);
        spaAppointmentTreatmentRepository.saveAndFlush(treatmentRow);
    }

    private SpaMapTable findMapTable(SpaMap map, String tableId) {
        return map.getTables().stream().filter(t -> t.getTableId().equals(tableId)).findFirst().orElseThrow();
    }

    // --- Table enrichment: busy/free/next/inactive/appointments -------------------------------

    @Test
    void get_tableWithNoAppointments_isFreeWithNoAppointments() {
        TableEntity table = persistSpaTable("Free Table");

        SpaMapTable dto = findMapTable(spaMapService.get(), table.getId());

        assertThat(dto.getBusy()).isFalse();
        assertThat(dto.getNextAppointmentStartTime().get()).isNull();
        assertThat(dto.getAppointments()).isEmpty();
        assertThat(dto.getIsActive()).isTrue();
    }

    @Test
    void get_appointmentCoveringThisMoment_isBusy() {
        TableEntity table = persistSpaTable("Busy Table");
        Booking booking = createBooking();
        MenuItemEntity treatment = createTreatment(60);
        UserEntity therapist = createTherapist();
        // Started 10 minutes ago, runs 60 minutes - covers right now regardless of when the suite runs.
        persistAppointment(table, booking, treatment, therapist, FIXED_TIME.minusMinutes(10), 60, SpaAppointmentStatus.BOOKED);

        SpaMapTable dto = findMapTable(spaMapService.get(), table.getId());

        assertThat(dto.getBusy()).isTrue();
    }

    /** CORRECTION 2's own rule, reused here: COMPLETED still occupies its slot (V43). */
    @Test
    void get_completedAppointmentCoveringThisMoment_isStillBusy() {
        TableEntity table = persistSpaTable("Busy Completed Table");
        Booking booking = createBooking();
        MenuItemEntity treatment = createTreatment(60);
        UserEntity therapist = createTherapist();
        persistAppointment(table, booking, treatment, therapist, FIXED_TIME.minusMinutes(10), 60, SpaAppointmentStatus.COMPLETED);

        SpaMapTable dto = findMapTable(spaMapService.get(), table.getId());

        assertThat(dto.getBusy()).isTrue();
    }

    @Test
    void get_cancelledAppointmentCoveringThisMoment_isNotBusy() {
        TableEntity table = persistSpaTable("Cancelled Table");
        Booking booking = createBooking();
        MenuItemEntity treatment = createTreatment(60);
        UserEntity therapist = createTherapist();
        persistAppointment(table, booking, treatment, therapist, FIXED_TIME.minusMinutes(10), 60, SpaAppointmentStatus.CANCELLED);

        SpaMapTable dto = findMapTable(spaMapService.get(), table.getId());

        assertThat(dto.getBusy()).isFalse();
    }

    @Test
    void get_futureBookedAppointment_isFreeWithNextStartTime() {
        TableEntity table = persistSpaTable("Free With Next Table");
        Booking booking = createBooking();
        MenuItemEntity treatment = createTreatment(30);
        UserEntity therapist = createTherapist();
        LocalTime start = FIXED_TIME.plusHours(2).withSecond(0).withNano(0);
        persistAppointment(table, booking, treatment, therapist, start, 30, SpaAppointmentStatus.BOOKED);

        SpaMapTable dto = findMapTable(spaMapService.get(), table.getId());

        assertThat(dto.getBusy()).isFalse();
        assertThat(dto.getNextAppointmentStartTime().get()).isEqualTo(start.toString().substring(0, 5));
    }

    @Test
    void get_pastAppointment_isNotCountedAsNext() {
        TableEntity table = persistSpaTable("Past Only Table");
        Booking booking = createBooking();
        MenuItemEntity treatment = createTreatment(30);
        UserEntity therapist = createTherapist();
        // Ended in the past, well clear of "covers this moment" too.
        persistAppointment(table, booking, treatment, therapist, FIXED_TIME.minusHours(3), 30, SpaAppointmentStatus.COMPLETED);

        SpaMapTable dto = findMapTable(spaMapService.get(), table.getId());

        assertThat(dto.getBusy()).isFalse();
        assertThat(dto.getNextAppointmentStartTime().get()).isNull();
    }

    @Test
    void get_inactiveTable_stillListedWithRealFactsNotBlanked() {
        TableEntity table = persistSpaTable("Inactive Table");
        table.setActive(false);
        tableRepository.saveAndFlush(table);
        Booking booking = createBooking();
        MenuItemEntity treatment = createTreatment(60);
        UserEntity therapist = createTherapist();
        persistAppointment(table, booking, treatment, therapist, FIXED_TIME.minusMinutes(10), 60, SpaAppointmentStatus.BOOKED);

        SpaMapTable dto = findMapTable(spaMapService.get(), table.getId());

        assertThat(dto.getIsActive()).isFalse();
        // isActive is independent of the day's own facts - a deactivated table's real occupancy
        // still shows, it isn't zeroed out just because the table is out of service.
        assertThat(dto.getBusy()).isTrue();
    }

    @Test
    void get_appointmentsList_includesEveryStatusWithTreatmentNames() {
        TableEntity table = persistSpaTable("Full List Table");
        Booking booking = createBooking();
        MenuItemEntity treatment = createTreatment(45);
        UserEntity therapist = createTherapist();
        persistAppointment(table, booking, treatment, therapist, FIXED_TIME.plusHours(4), 45, SpaAppointmentStatus.CANCELLED);

        SpaMapTable dto = findMapTable(spaMapService.get(), table.getId());

        assertThat(dto.getAppointments()).hasSize(1);
        assertThat(dto.getAppointments().get(0).getStatus()).isEqualTo(SpaAppointmentStatus.CANCELLED);
        assertThat(dto.getAppointments().get(0).getGuestName()).isEqualTo(booking.getGuestName());
        assertThat(dto.getAppointments().get(0).getTreatmentNames()).containsExactly(treatment.getName());
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
