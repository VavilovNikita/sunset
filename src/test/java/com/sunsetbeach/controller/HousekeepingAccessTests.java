package com.sunsetbeach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.BookingOccupancyService;
import com.sunsetbeach.service.BookingService;
import com.sunsetbeach.support.FixedHotelClockConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * What the HOUSEKEEPER job function opens to someone below CASHIER: the Housekeeping board and marking a room
 * clean or dirty - no guest, no money, nothing else. Through the real filter chain and the real controllers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedHotelClockConfig.class)
@Transactional
class HousekeepingAccessTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;

    private String token(Role role, String... functions) {
        UserEntity user = new UserEntity();
        user.setEmail("housekeeping-" + UUID.randomUUID() + "@test.local");
        user.setName(user.getEmail());
        user.setPasswordHash("unused");
        user.setRole(role);
        user.setActive(true);
        user.setTokenVersion(0);
        user.setJobFunctions(functions);
        UserEntity saved = userRepository.saveAndFlush(user);
        return "Bearer " + jwtService.issue(new StaffPrincipal(saved.getId(), saved.getEmail(), role));
    }

    private RoomEntity roomType() {
        RoomEntity r = new RoomEntity();
        r.setName("HK Room " + UUID.randomUUID());
        r.setDescription("Used only by HousekeepingAccessTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        return roomRepository.saveAndFlush(r);
    }

    private RoomUnitEntity unit(RoomEntity type) {
        RoomUnitEntity u = new RoomUnitEntity();
        u.setRoomId(type.getId());
        u.setLabel("HK-" + UUID.randomUUID().toString().substring(0, 8));
        u.setActive(true);
        u.setHousekeepingStatus(HousekeepingStatus.DIRTY);
        return roomUnitRepository.saveAndFlush(u);
    }

    private Booking booking(RoomEntity type, RoomUnitEntity unit, String guest, LocalDate checkIn, LocalDate checkOut) {
        Booking b = bookingService.createStaffBooking(
                new StaffBookingCreateInput(type.getId(), guest, checkIn.toString(), checkOut.toString(), BookingChannel.WALK_IN, 1));
        return bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
    }

    private int statusOf(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header("Authorization", token).contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void aWaiterWhoIsAHousekeeper_readsTheBoardAndMarksRooms_andSeesNoGuestAndNoMoney() throws Exception {
        RoomEntity type = roomType();
        RoomUnitEntity turnover = unit(type);
        Booking leaving = booking(type, turnover, "Secret Departing Guest", TODAY.minusDays(2), TODAY);
        booking(type, turnover, "Secret Arriving Guest", TODAY, TODAY.plusDays(2));
        occupancyService.checkIn(leaving.getId());
        RoomUnitEntity empty = unit(type);
        String housekeeper = token(Role.WAITER, "HOUSEKEEPER");

        String body = mockMvc.perform(get("/housekeeping/rooms").header("Authorization", housekeeper))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("Secret").doesNotContain(leaving.getId()).doesNotContain("1000.00").doesNotContain("2000.00");
        List<String> fields = new ArrayList<>();
        JsonNode occupied = null;
        JsonNode vacant = null;
        for (JsonNode node : objectMapper.readTree(body)) {
            if (node.get("roomUnitId").asString().equals(turnover.getId())) occupied = node;
            if (node.get("roomUnitId").asString().equals(empty.getId())) vacant = node;
        }
        fields.addAll(occupied.propertyNames());
        assertThat(fields).containsExactlyInAnyOrder("roomUnitId", "roomId", "roomName", "unitLabel", "isActive", "housekeepingStatus",
                "occupied", "arrivingToday", "departingToday", "overdueDays", "outOfOrder");
        assertThat(occupied.get("occupied").asBoolean()).isTrue();
        assertThat(occupied.get("departingToday").asBoolean()).isTrue();
        // The turnover room: the next guest is already waiting for it.
        assertThat(occupied.get("arrivingToday").asBoolean()).isTrue();
        assertThat(vacant.get("occupied").asBoolean()).isFalse();
        assertThat(vacant.get("arrivingToday").asBoolean()).isFalse();

        mockMvc.perform(patch("/room-units/" + empty.getId() + "/housekeeping").header("Authorization", housekeeper)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"housekeepingStatus\":\"CLEAN\"}"))
                .andExpect(status().isOk());
        assertThat(roomUnitRepository.findById(empty.getId()).orElseThrow().getHousekeepingStatus()).isEqualTo(HousekeepingStatus.CLEAN);
    }

    @Test
    void theFunctionOpensNothingElse() throws Exception {
        String housekeeper = token(Role.WAITER, "HOUSEKEEPER");
        RoomUnitEntity unit = unit(roomType());

        for (MockHttpServletRequestBuilder request : List.of(
                get("/property-map"), get("/property-map/image"), get("/rooms"), get("/bookings"), get("/bookings/today"),
                get("/bookings/calendar?from=2034-03-01&to=2034-03-08"), get("/night-audit"), get("/room-units/" + unit.getId() + "/blocks"),
                delete("/room-units/" + unit.getId()), patch("/room-units/" + unit.getId()).content("{}"),
                post("/room-units").content("{}"), get("/spa-appointments?date=2034-03-10"), get("/roster?month=2034-03"), get("/users"))) {
            assertThat(statusOf(request, housekeeper)).as(request.buildRequest(null).getRequestURI()).isEqualTo(403);
        }
    }

    @Test
    void withoutTheFunction_aWaiterIsRefused_andACashierStillHasBoth() throws Exception {
        RoomUnitEntity unit = unit(roomType());
        MockHttpServletRequestBuilder mark =
                patch("/room-units/" + unit.getId() + "/housekeeping").content("{\"housekeepingStatus\":\"CLEAN\"}");
        String waiter = token(Role.WAITER);
        String engineer = token(Role.WAITER, "ENGINEER");

        for (String refused : List.of(waiter, engineer)) {
            assertThat(statusOf(get("/housekeeping/rooms"), refused)).isEqualTo(403);
            assertThat(statusOf(patch("/room-units/" + unit.getId() + "/housekeeping").content("{\"housekeepingStatus\":\"CLEAN\"}"), refused))
                    .isEqualTo(403);
        }
        String cashier = token(Role.CASHIER);
        assertThat(statusOf(get("/housekeeping/rooms"), cashier)).isEqualTo(200);
        assertThat(statusOf(mark, cashier)).isEqualTo(200);
    }
}
