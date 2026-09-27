package com.sunsetbeach.contract;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AuditEntityType;
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
 * {@code Guest.vip}/{@code dateOfBirth}/{@code tags} over real HTTP, through the real security
 * chain and Jackson converter - same {@code @AutoConfigureMockMvc} setup and reasoning as
 * {@link DateOnlyFieldsContractTests}. The service-level behaviour (normalization, audit wording)
 * is covered in {@code GuestServiceTests}; this class covers what only the wire can show: that an
 * omitted field really defaults on create, that a bad date is a 400 and not a 500, that an update
 * body predating these fields is rejected rather than silently clearing them, and that a
 * {@code Booking} embedding its guest actually carries the new fields in its JSON.
 *
 * <p>Dates are fixed far-past/far-future values, never derived from the wall clock - see
 * CLAUDE.md's Tests section.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GuestProfileFieldsContractTests extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSegmentRepository bookingSegmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdGuestIds = new ArrayList<>();
    private final List<String> createdBookingIds = new ArrayList<>();
    private final List<String> createdRoomIds = new ArrayList<>();
    private String testUserId;
    private String authHeader;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setEmail("guest-profile-test-" + UUID.randomUUID() + "@test.local");
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
            deleteAuditEntries(AuditEntityType.BOOKING, bookingId);
            bookingSegmentRepository.deleteAll(bookingSegmentRepository.findByBookingIdOrderByCheckInAsc(bookingId));
            bookingRepository.findById(bookingId).ifPresent(bookingRepository::delete);
        }
        for (String guestId : createdGuestIds) {
            deleteAuditEntries(AuditEntityType.GUEST, guestId);
            guestRepository.findById(guestId).ifPresent(guestRepository::delete);
        }
        for (String roomId : createdRoomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
        if (testUserId != null) {
            userRepository.deleteById(testUserId);
        }
    }

    private void deleteAuditEntries(AuditEntityType entityType, String entityId) {
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == entityType && entityId.equals(e.getEntityId()))
                .toList());
    }

    private JsonNode createGuestOverHttp(String jsonBody) throws Exception {
        String body = mockMvc.perform(post("/guests")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode guest = objectMapper.readTree(body);
        createdGuestIds.add(guest.get("id").asText());
        return guest;
    }

    @Test
    void create_omittingProfileFields_defaultsThemInTheResponse() throws Exception {
        JsonNode guest = createGuestOverHttp("{\"name\":\"Wire Defaults " + UUID.randomUUID() + "\"}");

        assertThat(guest.get("vip").asBoolean()).isFalse();
        assertThat(guest.get("vip").isBoolean()).isTrue();
        assertThat(guest.get("dateOfBirth").isNull()).isTrue();
        assertThat(guest.get("tags").isArray()).isTrue();
        assertThat(guest.get("tags")).isEmpty();
    }

    @Test
    void create_dateOfBirthInTheFuture_is400() throws Exception {
        mockMvc.perform(post("/guests")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wire Future " + UUID.randomUUID() + "\",\"dateOfBirth\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_dateOfBirthNotARealDate_is400() throws Exception {
        mockMvc.perform(post("/guests")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wire Bad Date " + UUID.randomUUID() + "\",\"dateOfBirth\":\"1990-02-30\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * The reason {@code vip} is required on update: a body written before these fields existed
     * must be refused, not treated as "unmark VIP, clear tags" - see {@code GuestUpdateInput}'s
     * own description.
     */
    @Test
    void update_bodyWithoutVip_is400_andLeavesTheGuestUntouched() throws Exception {
        JsonNode guest = createGuestOverHttp(
                "{\"name\":\"Wire Legacy " + UUID.randomUUID() + "\",\"vip\":true,\"tags\":[\"honeymoon\"]}");
        String id = guest.get("id").asText();

        mockMvc.perform(patch("/guests/" + id)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isBadRequest());

        var entity = guestRepository.findById(id).orElseThrow();
        assertThat(entity.isVip()).isTrue();
        assertThat(entity.getTags()).containsExactly("honeymoon");
    }

    @Test
    void bookingEmbeddingItsGuest_carriesVipDateOfBirthAndTags() throws Exception {
        JsonNode guest = createGuestOverHttp("{\"name\":\"Wire Embedded " + UUID.randomUUID()
                + "\",\"vip\":true,\"dateOfBirth\":\"1987-04-12\",\"tags\":[\"honeymoon\",\"late checkout\"]}");
        String bookingId = createBookingOverHttp();

        mockMvc.perform(put("/bookings/" + bookingId + "/guest")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestId\":\"" + guest.get("id").asText() + "\"}"))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/bookings/" + bookingId).header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode embedded = objectMapper.readTree(body).get("guest");

        assertThat(embedded.get("vip").asBoolean()).isTrue();
        assertThat(embedded.get("dateOfBirth").asText()).isEqualTo("1987-04-12");
        assertThat(embedded.get("tags")).extracting(JsonNode::asText).containsExactly("honeymoon", "late checkout");
    }

    private String createBookingOverHttp() throws Exception {
        RoomEntity room = new RoomEntity();
        room.setName("Guest Profile Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by GuestProfileFieldsContractTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Guest Profile Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        RoomUnitEntity savedUnit = roomUnitRepository.saveAndFlush(unit);

        String body = mockMvc.perform(post("/bookings/staff")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + savedRoom.getId() + "\",\"roomUnitId\":\"" + savedUnit.getId()
                                + "\",\"guestName\":\"Embedded Guest Booking\",\"checkIn\":\"2098-06-01\",\"checkOut\":\"2098-06-03\",\"channel\":\"WALK_IN\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String bookingId = objectMapper.readTree(body).get("id").asText();
        createdBookingIds.add(bookingId);
        return bookingId;
    }
}
