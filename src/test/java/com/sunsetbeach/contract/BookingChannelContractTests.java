package com.sunsetbeach.contract;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingChannel;
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
import com.sunsetbeach.service.EmailService;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * {@code Booking.channel} over real HTTP: DIRECT for the public form (set server-side, not a guest
 * input), required on {@code POST /bookings/staff}, and present-updates/absent-leaves-alone on
 * {@code PATCH /bookings/{id}} - with the audit summary naming a channel change only when one
 * actually happened. Also pins that channel never touches the internal {@code BookingSource}.
 *
 * <p>Dates are fixed far-future values, never derived from the wall clock - see CLAUDE.md's Tests
 * section.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingChannelContractTests extends AbstractIntegrationTest {

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

    @MockitoSpyBean
    private EmailService emailService;

    private final List<String> createdBookingIds = new ArrayList<>();
    private final List<String> createdRoomIds = new ArrayList<>();
    private String testUserId;
    private String authHeader;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setEmail("booking-channel-test-" + UUID.randomUUID() + "@test.local");
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

    @Test
    void publicBooking_isAlwaysDirect_andStaysPublicSource() throws Exception {
        String roomId = createRoom();
        String body = mockMvc.perform(post("/bookings")
                        // Unique client address so the public form's per-IP rate limiter never
                        // sees this test as a repeat caller.
                        .with(request -> {
                            request.setRemoteAddr("198.51.100." + (1 + (int) (Math.random() * 250)));
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + roomId + "\",\"guestName\":\"Public Channel Guest\","
                                + "\"guestEmail\":\"channel-" + UUID.randomUUID() + "@test.local\",\"guestPhone\":\"+66 1234 5678\","
                                + "\"checkIn\":\"2097-03-01\",\"checkOut\":\"2097-03-03\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode booking = objectMapper.readTree(body);
        createdBookingIds.add(booking.get("id").asText());

        assertThat(booking.get("channel").asText()).isEqualTo("DIRECT");
        var entity = bookingRepository.findById(booking.get("id").asText()).orElseThrow();
        assertThat(entity.getChannel()).isEqualTo(BookingChannel.DIRECT);
        assertThat(entity.getSource()).isEqualTo(BookingSource.PUBLIC);
    }

    @Test
    void staffBooking_withoutChannel_isRejected() throws Exception {
        String roomId = createRoom();
        mockMvc.perform(post("/bookings/staff")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + roomId + "\",\"guestName\":\"No Channel Guest\","
                                + "\"checkIn\":\"2097-04-01\",\"checkOut\":\"2097-04-03\"}"))
                .andExpect(status().isBadRequest());

        assertThat(bookingRepository.findAll().stream().filter(b -> roomId.equals(b.getRoomId()))).isEmpty();
    }

    @Test
    void staffBooking_storesGivenChannel_andStaysStaffSource() throws Exception {
        String bookingId = createStaffBooking("PHONE");

        var entity = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(entity.getChannel()).isEqualTo(BookingChannel.PHONE);
        assertThat(entity.getSource()).isEqualTo(BookingSource.STAFF);
    }

    @Test
    void patch_withoutChannel_leavesItAlone_andAuditsNothingAboutChannel() throws Exception {
        String bookingId = createStaffBooking("PHONE");

        JsonNode updated = patchBooking(bookingId, "{\"status\":\"CONFIRMED\"}");

        assertThat(updated.get("channel").asText()).isEqualTo("PHONE");
        List<AuditLogEntity> entries = statusEntries(bookingId);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).contains("NEW").contains("CONFIRMED").doesNotContainIgnoringCase("channel changed");
    }

    @Test
    void patch_channelOnly_updatesIt_andAuditSummaryNamesTheChange() throws Exception {
        String bookingId = createStaffBooking("PHONE");

        JsonNode updated = patchBooking(bookingId, "{\"status\":\"NEW\",\"channel\":\"AIRBNB\"}");

        assertThat(updated.get("channel").asText()).isEqualTo("AIRBNB");
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getSource()).isEqualTo(BookingSource.STAFF);
        List<AuditLogEntity> entries = statusEntries(bookingId);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).startsWith("Channel changed from PHONE to AIRBNB");
    }

    @Test
    void patch_statusAndChannel_bothNamedInOneEntry() throws Exception {
        String bookingId = createStaffBooking("WALK_IN");

        patchBooking(bookingId, "{\"status\":\"CONFIRMED\",\"channel\":\"BOOKING_COM\"}");

        List<AuditLogEntity> entries = statusEntries(bookingId);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary())
                .contains("Status changed from NEW to CONFIRMED")
                .contains("channel changed from WALK_IN to BOOKING_COM");
    }

    @Test
    void patch_sameChannelAgain_isNotRecordedAsAChange() throws Exception {
        String bookingId = createStaffBooking("AGODA");

        patchBooking(bookingId, "{\"status\":\"NEW\",\"channel\":\"AGODA\"}");

        assertThat(statusEntries(bookingId)).isEmpty();
    }

    @Test
    void patch_channelOnPaidBooking_doesNotResendGuestStatusEmail() throws Exception {
        String bookingId = createStaffBooking("PHONE");
        patchBooking(bookingId, "{\"status\":\"PAID\"}");
        verify(emailService).sendGuestStatusEmail(any(), any());
        clearInvocations(emailService);

        patchBooking(bookingId, "{\"status\":\"PAID\",\"channel\":\"OTHER\"}");

        verify(emailService, never()).sendGuestStatusEmail(any(), any());
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

    private String createStaffBooking(String channel) throws Exception {
        String roomId = createRoom();
        String body = mockMvc.perform(post("/bookings/staff")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + roomId + "\",\"guestName\":\"Channel Staff Guest\","
                                + "\"guestEmail\":\"channel-" + UUID.randomUUID() + "@test.local\","
                                + "\"checkIn\":\"2097-05-01\",\"checkOut\":\"2097-05-03\",\"channel\":\"" + channel + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode booking = objectMapper.readTree(body);
        createdBookingIds.add(booking.get("id").asText());
        assertThat(booking.get("channel").asText()).isEqualTo(channel);
        return booking.get("id").asText();
    }

    private String createRoom() {
        RoomEntity room = new RoomEntity();
        room.setName("Channel Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingChannelContractTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Channel Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);
        return savedRoom.getId();
    }
}
