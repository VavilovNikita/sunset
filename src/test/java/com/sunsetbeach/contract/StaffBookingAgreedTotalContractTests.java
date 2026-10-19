package com.sunsetbeach.contract;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.BookingSegmentNightlyRateEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentNightlyRateRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.BookingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * {@code StaffBookingCreateInput.agreedTotal} over real HTTP: a manager-entered price for the
 * whole stay, spread over the nights and frozen like a SiteMinder price; absent, the rate plan
 * prices as before; a cashier who sends it is refused. Dates are fixed far-future values.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StaffBookingAgreedTotalContractTests extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private GuestRepository guestRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private BookingSegmentNightlyRateRepository nightlyRateRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private BookingService bookingService;

    private final List<String> createdBookingIds = new ArrayList<>();
    private final List<String> createdRoomIds = new ArrayList<>();
    private final List<String> createdUserIds = new ArrayList<>();
    private String managerAuth;
    private String cashierAuth;

    @BeforeEach
    void setUp() {
        managerAuth = authFor(Role.MANAGER);
        cashierAuth = authFor(Role.CASHIER);
    }

    @AfterEach
    void cleanUp() {
        for (String bookingId : createdBookingIds) {
            auditLogRepository.deleteAll(auditEntries(bookingId));
            bookingRepository.findById(bookingId).ifPresent(booking -> {
                String guestId = booking.getGuestId();
                segmentRepository.deleteAll(segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId));
                bookingRepository.delete(booking);
                if (guestId != null) {
                    auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                            .filter(e -> e.getEntityType() == AuditEntityType.GUEST && guestId.equals(e.getEntityId()))
                            .toList());
                    guestRepository.findById(guestId).ifPresent(guestRepository::delete);
                }
            });
        }
        for (String roomId : createdRoomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
        userRepository.deleteAllById(createdUserIds);
    }

    @Test
    void managerPrice_isSpreadOverTheNightsAndSumsToExactlyTheEnteredTotal() throws Exception {
        // 10 000 / 3 nights does not divide: 3333.33 + 3333.33 + 3333.34.
        JsonNode booking = createAsManager(createRoom("1200.00"), "2097-08-01", "2097-08-04", "\"agreedTotal\":\"10000\"");

        assertThat(new BigDecimal(booking.get("totalPrice").asText())).isEqualByComparingTo("10000.00");
        Map<LocalDate, BigDecimal> rates = nightlyRatesOf(booking.get("id").asText());
        assertThat(rates).hasSize(3);
        assertThat(rates.get(LocalDate.parse("2097-08-01"))).isEqualByComparingTo("3333.33");
        assertThat(rates.get(LocalDate.parse("2097-08-02"))).isEqualByComparingTo("3333.33");
        assertThat(rates.get(LocalDate.parse("2097-08-03"))).isEqualByComparingTo("3333.34");
        assertThat(rates.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("10000.00");
    }

    @Test
    void withoutTheField_theRatePlanPrices() throws Exception {
        JsonNode booking = createAsManager(createRoom("1200.00"), "2097-08-01", "2097-08-04", "\"purpose\":\"STANDARD\"");
        assertThat(new BigDecimal(booking.get("totalPrice").asText())).isEqualByComparingTo("3600.00");

        // An explicit null means the same as leaving it out.
        JsonNode withNull = createAsManager(createRoom("1000.00"), "2097-08-01", "2097-08-03", "\"agreedTotal\":null");
        assertThat(new BigDecimal(withNull.get("totalPrice").asText())).isEqualByComparingTo("2000.00");
    }

    @Test
    void cashier_withAgreedTotal_isRefused_andWithoutItCreatesAsBefore() throws Exception {
        String roomId = createRoom("1000.00");
        postStaff(cashierAuth, roomId, "2097-08-01", "2097-08-03", "\"agreedTotal\":\"500\"").andExpect(status().isForbidden());
        assertThat(bookingRepository.findAll().stream().filter(b -> roomId.equals(b.getRoomId()))).isEmpty();

        ResultActions ok = postStaff(cashierAuth, roomId, "2097-08-01", "2097-08-03", "\"purpose\":\"STANDARD\"").andExpect(status().isCreated());
        createdBookingIds.add(objectMapper.readTree(ok.andReturn().getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void invalidAmount_isA400OnAgreedTotal_andNothingIsCreated() throws Exception {
        String roomId = createRoom("1000.00");
        for (String bad : new String[] {"\"-1\"", "\"12.345\"", "\"abc\"", "\"\"", "\"1e3\""}) {
            String response = postStaff(managerAuth, roomId, "2097-08-01", "2097-08-03", "\"agreedTotal\":" + bad)
                    .andExpect(status().isBadRequest())
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).as("amount %s", bad).contains("agreedTotal");
        }
        assertThat(bookingRepository.findAll().stream().filter(b -> roomId.equals(b.getRoomId()))).isEmpty();
    }

    @Test
    void extendingAManualPriceBooking_keepsTheOldNightsAndPricesTheNewOneAtTheRatePlan() throws Exception {
        RoomEntity room = roomRepository.findById(createRoom("1000.00")).orElseThrow();
        JsonNode booking = createAsManager(room.getId(), "2097-08-01", "2097-08-04", "\"agreedTotal\":\"10000\"");
        String bookingId = booking.get("id").asText();

        room.setBasePrice(new BigDecimal("2500.00"));
        roomRepository.saveAndFlush(room);
        bookingService.updateSchedule(bookingId, new BookingScheduleInput("2097-08-01", "2097-08-05").roomUnitId(null));

        Map<LocalDate, BigDecimal> rates = nightlyRatesOf(bookingId);
        assertThat(rates).hasSize(4);
        assertThat(rates.get(LocalDate.parse("2097-08-01"))).isEqualByComparingTo("3333.33");
        assertThat(rates.get(LocalDate.parse("2097-08-02"))).isEqualByComparingTo("3333.33");
        assertThat(rates.get(LocalDate.parse("2097-08-03"))).isEqualByComparingTo("3333.34");
        assertThat(rates.get(LocalDate.parse("2097-08-04"))).isEqualByComparingTo("2500.00");
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getTotalPrice()).isEqualByComparingTo("12500.00");
    }

    @Test
    void audit_recordsThatThePriceWasManual_withBothFigures() throws Exception {
        JsonNode booking = createAsManager(createRoom("1200.00"), "2097-08-01", "2097-08-04", "\"agreedTotal\":\"3000.50\"");

        List<AuditLogEntity> created = auditEntries(booking.get("id").asText()).stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_CREATED)
                .toList();
        assertThat(created).hasSize(1);
        assertThat(created.get(0).getSummary()).contains("price set manually").contains("3000.50").contains("rate plan: 3600.00");

        // An ordinary booking's entry says nothing about a manual price.
        JsonNode plain = createAsManager(createRoom("1000.00"), "2097-08-01", "2097-08-03", "\"purpose\":\"STANDARD\"");
        assertThat(auditEntries(plain.get("id").asText()).get(0).getSummary()).doesNotContain("manually");
    }

    // --- helpers -------------------------------------------------------------------------------

    private String authFor(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("agreed-total-" + role + "-" + UUID.randomUUID() + "@test.local");
        user.setName(user.getEmail());
        user.setPasswordHash("unused");
        user.setRole(role);
        user.setActive(true);
        user.setTokenVersion(0);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return "Bearer " + jwtService.issue(new StaffPrincipal(saved.getId(), saved.getEmail(), role));
    }

    private ResultActions postStaff(String auth, String roomId, String checkIn, String checkOut, String extra) throws Exception {
        return mockMvc.perform(post("/bookings/staff")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomId\":\"" + roomId + "\",\"guestName\":\"Agreed Total Guest\",\"channel\":\"WALK_IN\",\"adults\":1,"
                        + "\"checkIn\":\"" + checkIn + "\",\"checkOut\":\"" + checkOut + "\"," + extra + "}"));
    }

    private JsonNode createAsManager(String roomId, String checkIn, String checkOut, String extra) throws Exception {
        String body = postStaff(managerAuth, roomId, checkIn, checkOut, extra)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode booking = objectMapper.readTree(body);
        createdBookingIds.add(booking.get("id").asText());
        return booking;
    }

    private Map<LocalDate, BigDecimal> nightlyRatesOf(String bookingId) {
        BookingSegmentEntity segment = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId).get(0);
        return nightlyRateRepository.findBySegmentIdOrderByDateAsc(segment.getId()).stream()
                .collect(Collectors.toMap(BookingSegmentNightlyRateEntity::getDate, BookingSegmentNightlyRateEntity::getPrice));
    }

    private List<AuditLogEntity> auditEntries(String bookingId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.BOOKING && bookingId.equals(e.getEntityId()))
                .toList();
    }

    private String createRoom(String basePrice) {
        RoomEntity room = new RoomEntity();
        room.setName("Agreed Total Room " + UUID.randomUUID());
        room.setDescription("Room used only by StaffBookingAgreedTotalContractTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal(basePrice));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Agreed Total Unit " + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);
        return savedRoom.getId();
    }
}
