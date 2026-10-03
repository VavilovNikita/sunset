package com.sunsetbeach.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RatePlanEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.service.EmailService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * {@code GET /public/rooms/{id}/quote} over real HTTP with no credentials: the figure it returns is
 * the figure {@code POST /bookings} then stores (so a guest client never has to add nightly prices
 * up itself - audit finding M14), availability is reported rather than thrown, bad input is a 400,
 * and quoting runs on its own rate-limit bucket so it never uses up the booking attempts.
 *
 * <p>Dates are fixed far-future values, never derived from the wall clock - see CLAUDE.md's Tests
 * section. Each test uses its own client address, because the limiters are singletons shared by
 * every test in the suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PublicRoomQuoteContractTests extends AbstractIntegrationTest {

    private static final AtomicInteger ADDRESS = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private RatePlanRepository ratePlanRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSegmentRepository bookingSegmentRepository;

    @Autowired
    private GuestRepository guestRepository;

    @MockitoSpyBean
    private EmailService emailService;

    private String roomId;
    private String clientAddress;
    private final List<String> createdBookingIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clientAddress = "203.0.113." + ADDRESS.getAndIncrement();
        RoomEntity room = new RoomEntity();
        room.setName("Public Quote Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by PublicRoomQuoteContractTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1500.00"));
        roomId = roomRepository.saveAndFlush(room).getId();

        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(roomId);
        unit.setLabel("Quote Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);

        RatePlanEntity override = new RatePlanEntity();
        override.setRoomId(roomId);
        override.setDate(LocalDate.of(2096, 5, 2));
        override.setPrice(new BigDecimal("2000.00"));
        ratePlanRepository.saveAndFlush(override);
    }

    @AfterEach
    void cleanUp() {
        for (String bookingId : createdBookingIds) {
            bookingRepository.findById(bookingId).ifPresent(booking -> {
                String guestId = booking.getGuestId();
                bookingSegmentRepository.deleteAll(bookingSegmentRepository.findByBookingIdOrderByCheckInAsc(bookingId));
                bookingRepository.delete(booking);
                if (guestId != null) {
                    guestRepository.findById(guestId).ifPresent(guestRepository::delete);
                }
            });
        }
        ratePlanRepository.deleteAll(ratePlanRepository.findAll().stream().filter(p -> roomId.equals(p.getRoomId())).toList());
        roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
        roomRepository.deleteById(roomId);
    }

    private MockHttpServletRequestBuilder fromClient(MockHttpServletRequestBuilder builder) {
        return builder.with(request -> {
            request.setRemoteAddr(clientAddress);
            return request;
        });
    }

    private MockHttpServletRequestBuilder quote(String room, String checkIn, String checkOut) {
        return fromClient(get("/public/rooms/" + room + "/quote").param("checkIn", checkIn).param("checkOut", checkOut));
    }

    private JsonNode book(String checkIn, String checkOut) throws Exception {
        String body = mockMvc.perform(fromClient(post("/bookings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + roomId + "\",\"guestName\":\"Quote Guest\","
                                + "\"guestEmail\":\"quote-" + UUID.randomUUID() + "@test.local\",\"guestPhone\":\"+66 1234 5678\","
                                + "\"checkIn\":\"" + checkIn + "\",\"checkOut\":\"" + checkOut + "\",\"adults\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode booking = objectMapper.readTree(body);
        createdBookingIds.add(booking.get("id").asText());
        return booking;
    }

    @Test
    void quote_needsNoLogin_andPricesEachNightAtTheCurrentRate() throws Exception {
        mockMvc.perform(quote(roomId, "2096-05-01", "2096-05-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value("5000.00"))
                .andExpect(jsonPath("$.nights").value(3))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.reason").doesNotExist());
    }

    @Test
    void quotedTotal_isExactlyWhatThePublicBookingStores() throws Exception {
        String quoted = objectMapper.readTree(mockMvc.perform(quote(roomId, "2096-05-01", "2096-05-04"))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("totalPrice").asText();

        JsonNode booking = book("2096-05-01", "2096-05-04");

        assertThat(new BigDecimal(booking.get("totalPrice").asText())).isEqualByComparingTo(new BigDecimal(quoted));
    }

    @Test
    void soldOutDates_areReportedAsUnavailable_notAnError() throws Exception {
        book("2096-06-01", "2096-06-03");

        mockMvc.perform(quote(roomId, "2096-06-02", "2096-06-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.totalPrice").value("3000.00"));
    }

    @Test
    void checkOutNotAfterCheckIn_isA400() throws Exception {
        mockMvc.perform(quote(roomId, "2096-05-04", "2096-05-04")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.checkOut").exists());
    }

    @Test
    void impossibleDate_isA400_notA500() throws Exception {
        mockMvc.perform(quote(roomId, "2096-02-30", "2096-03-02")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.checkIn").exists());
    }

    @Test
    void stayLongerThan90Nights_isA400() throws Exception {
        mockMvc.perform(quote(roomId, "2096-01-01", "2096-04-01")).andExpect(status().isBadRequest());
        mockMvc.perform(quote(roomId, "2096-01-01", "2096-03-31")).andExpect(status().isOk());
    }

    @Test
    void unknownRoom_isA404() throws Exception {
        mockMvc.perform(quote("no-such-room", "2096-05-01", "2096-05-02")).andExpect(status().isNotFound());
    }

    @Test
    void quoting_neverUsesUpTheBookingAttempts() throws Exception {
        // BookingRateLimiter allows 8 booking attempts per address per hour - quote more than
        // that from the same address, then book: the booking must still go through.
        for (int i = 0; i < 12; i++) {
            mockMvc.perform(quote(roomId, "2096-07-01", "2096-07-03")).andExpect(status().isOk());
        }
        book("2096-07-01", "2096-07-03");
    }

    @Test
    void quoting_isRateLimitedPerAddress() throws Exception {
        for (int i = 0; i < 120; i++) {
            mockMvc.perform(quote(roomId, "2096-08-01", "2096-08-02")).andExpect(status().isOk());
        }
        mockMvc.perform(quote(roomId, "2096-08-01", "2096-08-02")).andExpect(status().isTooManyRequests());
    }
}
