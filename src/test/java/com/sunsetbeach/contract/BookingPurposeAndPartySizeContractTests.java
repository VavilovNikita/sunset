package com.sunsetbeach.contract;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPurpose;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * {@code Booking.purpose}, {@code adults}/{@code children} and the {@code EXPEDIA} channel over
 * real HTTP: the public form requires {@code adults}, defaults {@code children} to 0 and is always
 * {@code STANDARD}; {@code POST /bookings/staff} defaults {@code purpose} to {@code STANDARD};
 * {@code PATCH /bookings/{id}} updates each of channel/purpose/adults/children independently and
 * leaves every omitted one alone.
 *
 * <p>Dates are fixed far-future values, never derived from the wall clock - see CLAUDE.md's Tests
 * section.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingPurposeAndPartySizeContractTests extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private GuestRepository guestRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository bookingSegmentRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private final List<String> createdBookingIds = new ArrayList<>();
    private final List<String> createdRoomIds = new ArrayList<>();
    private String testUserId;
    private String authHeader;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setEmail("booking-purpose-test-" + UUID.randomUUID() + "@test.local");
        user.setName(user.getEmail());
        user.setPasswordHash("unused");
        user.setRole(Role.MANAGER);
        user.setActive(true);
        user.setTokenVersion(0);
        UserEntity saved = userRepository.saveAndFlush(user);
        testUserId = saved.getId();
        authHeader = "Bearer " + jwtService.issue(new StaffPrincipal(saved.getId(), saved.getEmail(), Role.MANAGER));
    }

    @AfterEach
    void cleanUp() {
        for (String bookingId : createdBookingIds) {
            auditLogRepository.deleteAll(auditEntries(bookingId));
            bookingRepository.findById(bookingId).ifPresent(booking -> {
                String guestId = booking.getGuestId();
                bookingSegmentRepository.deleteAll(bookingSegmentRepository.findByBookingIdOrderByCheckInAsc(bookingId));
                bookingRepository.delete(booking);
                // Both create paths auto-link (or create) a Guest card from the booking's email.
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
        if (testUserId != null) {
            userRepository.deleteById(testUserId);
        }
    }

    // --- POST /bookings (public) ---------------------------------------------------------------

    @Test
    void publicBooking_withoutAdults_isRejected() throws Exception {
        String roomId = createRoom();
        postPublic(publicBody(roomId, "")).andExpect(status().isBadRequest());

        assertThat(bookingRepository.findAll().stream().filter(b -> roomId.equals(b.getRoomId()))).isEmpty();
    }

    @Test
    void publicBooking_withZeroAdults_isRejected() throws Exception {
        String roomId = createRoom();
        postPublic(publicBody(roomId, ",\"adults\":0")).andExpect(status().isBadRequest());
    }

    @Test
    void publicBooking_withoutChildren_defaultsToZero_andIsAlwaysStandard() throws Exception {
        String roomId = createRoom();
        // A purpose in the body is not a public input - it is ignored, not honoured.
        String body = postPublic(publicBody(roomId, ",\"adults\":2,\"purpose\":\"COMPLIMENTARY\""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode booking = objectMapper.readTree(body);
        createdBookingIds.add(booking.get("id").asText());

        assertThat(booking.get("adults").asInt()).isEqualTo(2);
        assertThat(booking.get("children").asInt()).isZero();
        assertThat(booking.get("purpose").asText()).isEqualTo("STANDARD");
        BookingEntity entity = bookingRepository.findById(booking.get("id").asText()).orElseThrow();
        assertThat(entity.getPurpose()).isEqualTo(BookingPurpose.STANDARD);
        assertThat(entity.getChildren()).isZero();
    }

    @Test
    void publicBooking_storesGivenChildren() throws Exception {
        String roomId = createRoom();
        String body = postPublic(publicBody(roomId, ",\"adults\":1,\"children\":1"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode booking = objectMapper.readTree(body);
        createdBookingIds.add(booking.get("id").asText());

        assertThat(booking.get("children").asInt()).isEqualTo(1);
    }

    // --- POST /bookings/staff ------------------------------------------------------------------

    @Test
    void staffBooking_withoutPurpose_defaultsToStandard() throws Exception {
        JsonNode booking = createStaffBooking("\"channel\":\"PHONE\",\"adults\":2");

        assertThat(booking.get("purpose").asText()).isEqualTo("STANDARD");
        assertThat(booking.get("adults").asInt()).isEqualTo(2);
        assertThat(booking.get("children").asInt()).isZero();
    }

    @Test
    void staffBooking_storesPurposeGuestCountsAndExpedia() throws Exception {
        JsonNode booking = createStaffBooking("\"channel\":\"EXPEDIA\",\"purpose\":\"COMPLIMENTARY\",\"adults\":2,\"children\":1");

        BookingEntity entity = bookingRepository.findById(booking.get("id").asText()).orElseThrow();
        assertThat(entity.getChannel()).isEqualTo(BookingChannel.EXPEDIA);
        assertThat(entity.getPurpose()).isEqualTo(BookingPurpose.COMPLIMENTARY);
        assertThat(entity.getAdults()).isEqualTo(2);
        assertThat(entity.getChildren()).isEqualTo(1);
    }

    @Test
    void staffBooking_withoutAdults_isRejected() throws Exception {
        String roomId = createRoom();
        mockMvc.perform(post("/bookings/staff")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + roomId + "\",\"guestName\":\"No Adults Guest\","
                                + "\"checkIn\":\"2097-07-01\",\"checkOut\":\"2097-07-03\",\"channel\":\"PHONE\"}"))
                .andExpect(status().isBadRequest());

        assertThat(bookingRepository.findAll().stream().filter(b -> roomId.equals(b.getRoomId()))).isEmpty();
    }

    // --- PATCH /bookings/{id} ------------------------------------------------------------------

    @Test
    void patch_withNoneOfTheFour_leavesThemAllUntouched() throws Exception {
        String id = createStaffBooking("\"channel\":\"AGODA\",\"purpose\":\"HOUSE_USE\",\"adults\":3,\"children\":2").get("id").asText();

        JsonNode updated = patchBooking(id, "{\"status\":\"CONFIRMED\"}");

        assertThat(updated.get("channel").asText()).isEqualTo("AGODA");
        assertThat(updated.get("purpose").asText()).isEqualTo("HOUSE_USE");
        assertThat(updated.get("adults").asInt()).isEqualTo(3);
        assertThat(updated.get("children").asInt()).isEqualTo(2);
        // Only the status change is named - nothing about the four untouched fields.
        assertThat(statusEntries(id)).singleElement()
                .satisfies(e -> assertThat(e.getSummary()).startsWith("Status changed from NEW to CONFIRMED for "));
    }

    @Test
    void patch_eachFieldUpdatesIndependently() throws Exception {
        String id = createStaffBooking("\"channel\":\"PHONE\",\"adults\":2,\"children\":1").get("id").asText();

        JsonNode afterPurpose = patchBooking(id, "{\"status\":\"NEW\",\"purpose\":\"COMPLIMENTARY\"}");
        assertThat(afterPurpose.get("purpose").asText()).isEqualTo("COMPLIMENTARY");
        assertThat(afterPurpose.get("channel").asText()).isEqualTo("PHONE");
        assertThat(afterPurpose.get("adults").asInt()).isEqualTo(2);
        assertThat(afterPurpose.get("children").asInt()).isEqualTo(1);

        JsonNode afterAdults = patchBooking(id, "{\"status\":\"NEW\",\"adults\":4}");
        assertThat(afterAdults.get("adults").asInt()).isEqualTo(4);
        assertThat(afterAdults.get("children").asInt()).isEqualTo(1);
        assertThat(afterAdults.get("purpose").asText()).isEqualTo("COMPLIMENTARY");

        JsonNode afterChildren = patchBooking(id, "{\"status\":\"NEW\",\"children\":0}");
        assertThat(afterChildren.get("children").asInt()).isZero();
        assertThat(afterChildren.get("adults").asInt()).isEqualTo(4);

        JsonNode afterChannel = patchBooking(id, "{\"status\":\"NEW\",\"channel\":\"EXPEDIA\"}");
        assertThat(afterChannel.get("channel").asText()).isEqualTo("EXPEDIA");
        assertThat(afterChannel.get("purpose").asText()).isEqualTo("COMPLIMENTARY");
        assertThat(afterChannel.get("adults").asInt()).isEqualTo(4);
        assertThat(afterChannel.get("children").asInt()).isZero();
    }

    @Test
    void patch_purposeAndGuestCountChanges_areNamedInTheAuditSummary() throws Exception {
        String id = createStaffBooking("\"channel\":\"WALK_IN\",\"adults\":1").get("id").asText();

        patchBooking(id, "{\"status\":\"NEW\",\"purpose\":\"HOUSE_USE\",\"adults\":2,\"children\":1}");

        assertThat(statusEntries(id)).singleElement().satisfies(e -> assertThat(e.getSummary())
                .startsWith("Purpose changed from STANDARD to HOUSE_USE; guests changed from 1 adult, 0 children to 2 adults, 1 child"));
    }

    @Test
    void patch_zeroAdults_isRejected() throws Exception {
        String id = createStaffBooking("\"channel\":\"WALK_IN\",\"adults\":1").get("id").asText();

        mockMvc.perform(patch("/bookings/" + id)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NEW\",\"adults\":0}"))
                .andExpect(status().isBadRequest());
        assertThat(bookingRepository.findById(id).orElseThrow().getAdults()).isEqualTo(1);
    }

    // --- helpers -------------------------------------------------------------------------------

    private String publicBody(String roomId, String extra) {
        return "{\"roomId\":\"" + roomId + "\",\"guestName\":\"Public Party Guest\","
                + "\"guestEmail\":\"party-" + UUID.randomUUID() + "@test.local\",\"guestPhone\":\"+66 1234 5678\","
                + "\"checkIn\":\"2097-06-01\",\"checkOut\":\"2097-06-03\"" + extra + "}";
    }

    private org.springframework.test.web.servlet.ResultActions postPublic(String json) throws Exception {
        return mockMvc.perform(post("/bookings")
                // Unique client address so the public form's per-IP rate limiter never sees this
                // test as a repeat caller.
                .with(request -> {
                    request.setRemoteAddr("198.51.100." + (1 + (int) (Math.random() * 250)));
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private JsonNode createStaffBooking(String fields) throws Exception {
        String roomId = createRoom();
        String body = mockMvc.perform(post("/bookings/staff")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + roomId + "\",\"guestName\":\"Party Staff Guest\","
                                + "\"guestEmail\":\"party-" + UUID.randomUUID() + "@test.local\","
                                + "\"checkIn\":\"2097-08-01\",\"checkOut\":\"2097-08-03\"," + fields + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode booking = objectMapper.readTree(body);
        createdBookingIds.add(booking.get("id").asText());
        return booking;
    }

    private JsonNode patchBooking(String bookingId, String json) throws Exception {
        String body = mockMvc.perform(patch("/bookings/" + bookingId)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private List<AuditLogEntity> statusEntries(String bookingId) {
        return auditEntries(bookingId).stream().filter(e -> e.getAction() == AuditAction.BOOKING_STATUS_CHANGED).toList();
    }

    private List<AuditLogEntity> auditEntries(String bookingId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.BOOKING && bookingId.equals(e.getEntityId()))
                .toList();
    }

    private String createRoom() {
        RoomEntity room = new RoomEntity();
        room.setName("Party Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingPurposeAndPartySizeContractTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Party Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);
        return savedRoom.getId();
    }
}
