package com.sunsetbeach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.BookingOccupancyService;
import com.sunsetbeach.service.BookingService;
import com.sunsetbeach.service.OrderService;
import com.sunsetbeach.service.ShiftService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The money and booking-integrity rules the owner fixed after the QA report, each driven through
 * the real controller/security/validation stack and asserted on the HTTP status it gives:
 * <ul>
 *   <li>a debt is visible whatever the booking's status ({@code balanceDue}, night audit);</li>
 *   <li>{@code PAID} cannot write money off - it needs a payment for exactly what is owed;</li>
 *   <li>check-in, check-out and room charges are refused from states they don't belong to (409);</li>
 *   <li>a shift is blocked only by its own open orders;</li>
 *   <li>names are trimmed and a blank one is a 400; a POS line is 1..99; a guest can only order
 *       from the public menu.</li>
 * </ul>
 * "Today" is pinned to {@link #TODAY} so the check-in-date rule never depends on the real date.
 * {@code @Transactional}: every row, including the users, is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MoneyAndIntegrityRulesTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final LocalDate TODAY = LocalDate.parse("2033-05-10");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2033-05-10T05:00:00Z"), BANGKOK);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private OrderService orderService;
    @Autowired private ShiftService shiftService;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    private UserEntity admin;
    private String adminAuth;
    private UserEntity cashierA;
    private String cashierAAuth;
    private UserEntity cashierB;
    private RoomEntity room;
    private RoomUnitEntity unit;
    private MenuItemEntity beer;

    @BeforeEach
    void setUp() {
        admin = saveUser(Role.ADMIN);
        adminAuth = tokenFor(admin);
        cashierA = saveUser(Role.CASHIER);
        cashierAAuth = tokenFor(cashierA);
        cashierB = saveUser(Role.CASHIER);

        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Rules Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by MoneyAndIntegrityRulesTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);
        RoomUnitEntity newUnit = new RoomUnitEntity();
        newUnit.setRoomId(room.getId());
        newUnit.setLabel("Rules Unit " + UUID.randomUUID());
        newUnit.setActive(true);
        unit = roomUnitRepository.saveAndFlush(newUnit);

        beer = menuItem("Rules Beer", MenuDepartment.BAR, true, "100.00");
    }

    // --- Fixtures --------------------------------------------------------------------------------

    private UserEntity saveUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.local");
        user.setName(user.getEmail());
        user.setPasswordHash("unused");
        user.setRole(role);
        user.setActive(true);
        user.setTokenVersion(0);
        return userRepository.saveAndFlush(user);
    }

    private String tokenFor(UserEntity user) {
        return "Bearer " + jwtService.issue(new StaffPrincipal(user.getId(), user.getEmail(), user.getRole()));
    }

    private MenuItemEntity menuItem(String name, MenuDepartment department, boolean available, String price) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName(name + " " + UUID.randomUUID());
        item.setDescription("rules test item");
        item.setCategory("Rules");
        item.setDepartment(department);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(available);
        return menuItemRepository.saveAndFlush(item);
    }

    /** A staff booking with a physical room, arriving {@code TODAY + arrivesInDays}, two nights (฿2,000). */
    private Booking booking(int arrivesInDays) {
        LocalDate checkIn = TODAY.plusDays(arrivesInDays);
        return bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), "Rules Guest", checkIn.toString(), checkIn.plusDays(2).toString(), BookingChannel.WALK_IN, 1)
                        .roomUnitId(unit.getId()));
    }

    private ResultActions patchStatus(String bookingId, String json) throws Exception {
        return mockMvc.perform(patch("/bookings/" + bookingId).header("Authorization", adminAuth).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions postJson(String url, String auth, String json) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(json);
        if (auth != null) {
            request = request.header("Authorization", auth);
        }
        return mockMvc.perform(request);
    }

    /** The unauthenticated public booking form: a fresh client address per call, so its per-address rate limit never trips. */
    private ResultActions postPublicBooking(String json) throws Exception {
        return mockMvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(json).with(request -> {
            request.setRemoteAddr("198.51.100." + (1 + (int) (Math.random() * 250)));
            return request;
        }));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** Writes everything and empties the persistence context, so a list/export request re-reads rows (and their lazy associations) from the database. */
    private void flushAndForget() {
        entityManager.flush();
        entityManager.clear();
    }

    private BookingEntity reload(String bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow();
    }

    private Order orderWithBeer(UserEntity opener, int quantity) {
        return orderService.create(new OrderCreateInput().items(java.util.List.of(new OrderItemInput(beer.getId(), quantity))), opener.getId());
    }

    // --- 1. A debt is visible whatever the status --------------------------------------------------

    @Test
    void balanceDue_isOnTheBooking_forACheckedOutGuestWhoNeverPaid() throws Exception {
        Booking b = booking(0);
        occupancyService.checkIn(b.getId());
        occupancyService.checkOut(b.getId(), null);

        JsonNode single = body(mockMvc.perform(get("/bookings/" + b.getId()).header("Authorization", adminAuth)).andExpect(status().isOk()));
        assertThat(single.get("occupancyStatus").asText()).isEqualTo("CHECKED_OUT");
        assertThat(single.get("status").asText()).isEqualTo("NEW");
        assertThat(single.get("balanceDue").asText()).isEqualTo("2000.00");

        flushAndForget(); // the list reads the bookings back with their lazy room, as a real request would
        JsonNode search = body(mockMvc.perform(get("/bookings/search").param("q", "Rules Guest").header("Authorization", adminAuth)).andExpect(status().isOk()));
        JsonNode row = null;
        for (JsonNode item : search.get("items")) {
            if (item.get("id").asText().equals(b.getId())) {
                row = item;
            }
        }
        assertThat(row).as("the booking is in the search results").isNotNull();
        assertThat(row.get("balanceDue").asText()).isEqualTo("2000.00");
    }

    @Test
    void balanceDue_forACancelledBooking_isItsPosChargesOnly() throws Exception {
        Booking b = booking(0);
        occupancyService.checkIn(b.getId());
        shiftService.open(cashierA.getId(), new ShiftOpenInput());
        Order order = orderWithBeer(cashierA, 3);
        mockMvc.perform(post("/orders/" + order.getId() + "/close").header("Authorization", cashierAAuth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"ROOM_CHARGE\",\"bookingId\":\"" + b.getId() + "\"}"))
                .andExpect(status().isOk());
        // A guest who has arrived can no longer be cancelled through the API (see
        // cancelling_aCheckedInBooking_is409 below). A booking cancelled after check-in, from
        // before that rule, is the legacy shape whose balance is asserted here - so it is written
        // the way that data was, directly.
        BookingEntity legacy = reload(b.getId());
        legacy.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(legacy);

        JsonNode single = body(mockMvc.perform(get("/bookings/" + b.getId()).header("Authorization", adminAuth)).andExpect(status().isOk()));
        assertThat(single.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(single.get("balanceDue").asText()).isEqualTo("300.00"); // the room is not charged; the bar tab is
    }

    // --- 1b. A guest who has arrived is not cancelled ------------------------------------------------

    @Test
    void cancelling_aCheckedInBooking_is409_andChangesNothing() throws Exception {
        Booking b = booking(0);
        occupancyService.checkIn(b.getId());

        JsonNode error = body(patchStatus(b.getId(), "{\"status\":\"CANCELLED\"}").andExpect(status().isConflict()));

        assertThat(error.get("error").asText()).contains("checked in");
        assertThat(reload(b.getId()).getStatus()).isEqualTo(BookingStatus.NEW);
    }

    @Test
    void cancelling_aCheckedOutBooking_is409() throws Exception {
        Booking b = booking(0);
        occupancyService.checkIn(b.getId());
        occupancyService.checkOut(b.getId(), null);

        JsonNode error = body(patchStatus(b.getId(), "{\"status\":\"CANCELLED\"}").andExpect(status().isConflict()));

        assertThat(error.get("error").asText()).contains("checked out");
        assertThat(reload(b.getId()).getStatus()).isEqualTo(BookingStatus.NEW);
    }

    @Test
    void cancelling_aBookingThatHasNotArrived_stillWorks_andReSendingCancelledToACancelledOneIsANoOp() throws Exception {
        Booking b = booking(3);

        patchStatus(b.getId(), "{\"status\":\"CANCELLED\"}").andExpect(status().isOk());
        patchStatus(b.getId(), "{\"status\":\"CANCELLED\"}").andExpect(status().isOk());

        assertThat(reload(b.getId()).getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void otherChangesToACheckedInBooking_stillWork() throws Exception {
        Booking b = booking(0);
        occupancyService.checkIn(b.getId());

        patchStatus(b.getId(), "{\"status\":\"CONFIRMED\"}").andExpect(status().isOk());

        assertThat(reload(b.getId()).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void nightAudit_listsAFinishedStayThatStillOwes_underUnpaidBalances_andNotAPaidOne() throws Exception {
        Booking owing = booking(0);
        occupancyService.checkIn(owing.getId());
        occupancyService.checkOut(owing.getId(), null);

        JsonNode audit = body(mockMvc.perform(get("/night-audit").param("date", TODAY.toString()).header("Authorization", adminAuth)).andExpect(status().isOk()));

        JsonNode found = null;
        for (JsonNode row : audit.get("unpaidBalances")) {
            if (row.get("id").asText().equals(owing.getId())) {
                found = row;
            }
        }
        assertThat(found).as("checked-out and unpaid is listed").isNotNull();
        assertThat(found.get("balanceDue").asText()).isEqualTo("2000.00");
        assertThat(found.get("status").asText()).isEqualTo("NEW");
    }

    // --- 2. PAID cannot write money off ------------------------------------------------------------

    @Test
    void markingPaidWithoutPayment_whileMoneyIsOwed_is409_andChangesNothing() throws Exception {
        Booking b = booking(2);

        patchStatus(b.getId(), "{\"status\":\"PAID\"}").andExpect(status().isConflict());

        assertThat(reload(b.getId()).getStatus()).isEqualTo(BookingStatus.NEW);
        assertThat(bookingService.listFolioPayments(b.getId())).isEmpty();
    }

    @Test
    void markingPaidWithAPaymentOfADifferentAmount_is409_andRecordsNothing() throws Exception {
        Booking b = booking(2);

        patchStatus(b.getId(), "{\"status\":\"PAID\",\"payment\":{\"method\":\"CASH\",\"amount\":\"1999.99\"}}").andExpect(status().isConflict());
        patchStatus(b.getId(), "{\"status\":\"PAID\",\"payment\":{\"method\":\"CASH\",\"amount\":\"2000.01\"}}").andExpect(status().isConflict());

        assertThat(reload(b.getId()).getStatus()).isEqualTo(BookingStatus.NEW);
        assertThat(bookingService.listFolioPayments(b.getId())).isEmpty();
    }

    @Test
    void markingPaidWithTheExactPayment_recordsItAndSetsPaid_andTheBalanceIsZero() throws Exception {
        Booking b = booking(2);

        JsonNode paid = body(patchStatus(b.getId(), "{\"status\":\"PAID\",\"payment\":{\"method\":\"CARD\",\"amount\":\"2000.00\"}}").andExpect(status().isOk()));

        assertThat(paid.get("status").asText()).isEqualTo("PAID");
        assertThat(paid.get("balanceDue").asText()).isEqualTo("0.00");
        assertThat(bookingService.listFolioPayments(b.getId())).singleElement()
                .satisfies(p -> {
                    assertThat(p.getAmount()).isEqualTo("2000.00");
                    assertThat(p.getMethod().getValue()).isEqualTo("CARD");
                });
    }

    @Test
    void markingPaidWithoutPayment_isFine_onceNothingIsOwedAnyMore() throws Exception {
        Booking b = booking(2);
        patchStatus(b.getId(), "{\"status\":\"PAID\",\"payment\":{\"method\":\"CASH\",\"amount\":\"2000.00\"}}").andExpect(status().isOk());
        patchStatus(b.getId(), "{\"status\":\"CONFIRMED\"}").andExpect(status().isOk());

        patchStatus(b.getId(), "{\"status\":\"PAID\"}").andExpect(status().isOk());

        assertThat(bookingService.listFolioPayments(b.getId())).hasSize(1); // the first payment still covers it
    }

    @Test
    void aCancelledBooking_isNeverReinstated_itsRoomMayHaveBeenSoldAgain() throws Exception {
        Booking b = booking(2);
        patchStatus(b.getId(), "{\"status\":\"CANCELLED\"}").andExpect(status().isOk());

        patchStatus(b.getId(), "{\"status\":\"CONFIRMED\"}").andExpect(status().isConflict());
        patchStatus(b.getId(), "{\"status\":\"PAID\",\"payment\":{\"method\":\"OTHER\",\"amount\":\"2000.00\"}}").andExpect(status().isConflict());
        patchStatus(b.getId(), "{\"status\":\"CANCELLED\",\"paymentNote\":\"refund ref 42\"}").andExpect(status().isOk());

        assertThat(reload(b.getId()).getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookingService.listFolioPayments(b.getId())).isEmpty();
    }

    // --- 3. Transitions the UI used to be the only guard for ---------------------------------------

    @Test
    void checkIn_ofACancelledBooking_is409() throws Exception {
        Booking b = booking(0);
        patchStatus(b.getId(), "{\"status\":\"CANCELLED\"}").andExpect(status().isOk());

        mockMvc.perform(post("/bookings/" + b.getId() + "/check-in").header("Authorization", adminAuth)).andExpect(status().isConflict());

        assertThat(reload(b.getId()).getOccupancyStatus()).isEqualTo(OccupancyStatus.EXPECTED);
    }

    @Test
    void checkIn_beforeTheCheckInDate_is409() throws Exception {
        Booking b = booking(1); // arrives tomorrow

        mockMvc.perform(post("/bookings/" + b.getId() + "/check-in").header("Authorization", adminAuth)).andExpect(status().isConflict());

        assertThat(reload(b.getId()).getOccupancyStatus()).isEqualTo(OccupancyStatus.EXPECTED);
    }

    @Test
    void checkIn_onTheDay_succeeds_andRepeatedIs409() throws Exception {
        Booking b = booking(0);

        mockMvc.perform(post("/bookings/" + b.getId() + "/check-in").header("Authorization", adminAuth)).andExpect(status().isOk());
        mockMvc.perform(post("/bookings/" + b.getId() + "/check-in").header("Authorization", adminAuth)).andExpect(status().isConflict());
    }

    @Test
    void checkOut_ofAGuestWhoWasNeverCheckedIn_is409_andOfOneAlreadyCheckedOutToo() throws Exception {
        Booking b = booking(0);
        mockMvc.perform(post("/bookings/" + b.getId() + "/check-out").header("Authorization", adminAuth)).andExpect(status().isConflict());

        occupancyService.checkIn(b.getId());
        mockMvc.perform(post("/bookings/" + b.getId() + "/check-out").header("Authorization", adminAuth)).andExpect(status().isOk());
        mockMvc.perform(post("/bookings/" + b.getId() + "/check-out").header("Authorization", adminAuth)).andExpect(status().isConflict());
    }

    @Test
    void roomCharge_toAGuestWhoIsNotCheckedIn_is409_beforeArrival_andAfterCheckOut() throws Exception {
        shiftService.open(cashierA.getId(), new ShiftOpenInput());
        Booking b = booking(0);

        Order beforeArrival = orderWithBeer(cashierA, 1);
        roomChargeClose(beforeArrival, b).andExpect(status().isConflict());

        occupancyService.checkIn(b.getId());
        roomChargeClose(beforeArrival, b).andExpect(status().isOk());

        occupancyService.checkOut(b.getId(), null);
        Order afterCheckOut = orderWithBeer(cashierA, 1);
        roomChargeClose(afterCheckOut, b).andExpect(status().isConflict());
        assertThat(orderRepository.findById(afterCheckOut.getId()).orElseThrow().getStatus().getValue()).isEqualTo("OPEN");
    }

    private ResultActions roomChargeClose(Order order, Booking booking) throws Exception {
        return postJson("/orders/" + order.getId() + "/close", cashierAAuth, "{\"method\":\"ROOM_CHARGE\",\"bookingId\":\"" + booking.getId() + "\"}");
    }

    @Test
    void changingTheStayOfAGuestWhoHasCheckedOut_is409() throws Exception {
        Booking b = booking(0);
        occupancyService.checkIn(b.getId());
        occupancyService.checkOut(b.getId(), null);

        String extended = "{\"checkIn\":\"" + b.getCheckIn() + "\",\"checkOut\":\"" + TODAY.plusDays(5) + "\",\"roomUnitId\":\"" + unit.getId() + "\"}";
        mockMvc.perform(patch("/bookings/" + b.getId() + "/schedule").header("Authorization", adminAuth).contentType(MediaType.APPLICATION_JSON).content(extended))
                .andExpect(status().isConflict());
    }

    // --- 4. A shift is blocked only by its own orders ----------------------------------------------

    @Test
    void closingAShift_withTheCashiersOwnOpenOrder_is409() throws Exception {
        Shift shift = shiftService.open(cashierA.getId(), new ShiftOpenInput());
        orderWithBeer(cashierA, 1);

        postJson("/shifts/" + shift.getId() + "/close", cashierAAuth, "{}").andExpect(status().isConflict());
    }

    /** Audit finding H7: a cashier can't close a colleague's drawer, and the closer is who really closed it. */
    @Test
    void closingAShift_isTheOpenersOrAManagers_andRecordsWhoClosedIt() throws Exception {
        Shift shift = shiftService.open(cashierA.getId(), new ShiftOpenInput());

        postJson("/shifts/" + shift.getId() + "/close", tokenFor(cashierB), "{}").andExpect(status().isNotFound());

        JsonNode closed = body(postJson("/shifts/" + shift.getId() + "/close", adminAuth, "{}").andExpect(status().isOk()));
        assertThat(closed.get("closedByUserId").asText()).isEqualTo(admin.getId());
        assertThat(closed.get("openedByUserId").asText()).isEqualTo(cashierA.getId());
    }

    @Test
    void closingAShift_withOnlySomeoneElsesOpenOrder_succeeds_andWarnsAboutIt() throws Exception {
        Shift shift = shiftService.open(cashierA.getId(), new ShiftOpenInput());
        Order othersOrder = orderWithBeer(cashierB, 1);

        JsonNode closed = body(postJson("/shifts/" + shift.getId() + "/close", cashierAAuth, "{}").andExpect(status().isOk()));

        assertThat(closed.get("status").asText()).isEqualTo("CLOSED");
        assertThat(closed.get("warning").asText()).contains("other staff").contains("#" + orderRepository.findById(othersOrder.getId()).orElseThrow().getNumber());
        assertThat(orderRepository.findById(othersOrder.getId()).orElseThrow().getStatus().getValue()).isEqualTo("OPEN"); // left alone
    }

    @Test
    void closingAShift_cancelsEmptyOpenOrders_insteadOfBlocking() throws Exception {
        Shift shift = shiftService.open(cashierA.getId(), new ShiftOpenInput());
        Order empty = orderService.create(new OrderCreateInput(), cashierA.getId()); // no table, no items: allowed, and abandoned

        JsonNode closed = body(postJson("/shifts/" + shift.getId() + "/close", cashierAAuth, "{}").andExpect(status().isOk()));

        assertThat(closed.get("status").asText()).isEqualTo("CLOSED");
        assertThat(orderRepository.findById(empty.getId()).orElseThrow().getStatus().getValue()).isEqualTo("CANCELLED");
    }

    // --- 6. Names -----------------------------------------------------------------------------------

    @Test
    void aGuestNameOfOnlySpaces_isA400_onTheStaffAndThePublicBookingForms() throws Exception {
        String dates = "\"checkIn\":\"" + TODAY.plusDays(3) + "\",\"checkOut\":\"" + TODAY.plusDays(5) + "\"";
        postJson("/bookings/staff", adminAuth,
                        "{\"roomId\":\"" + room.getId() + "\",\"guestName\":\"     \"," + dates + ",\"adults\":1,\"channel\":\"WALK_IN\"}")
                .andExpect(status().isBadRequest());
        postPublicBooking(
                        "{\"roomId\":\"" + room.getId() + "\",\"guestName\":\"     \",\"guestEmail\":\"a@test.local\",\"guestPhone\":\"+66 1234 5678\"," + dates + ",\"adults\":1}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void aGuestName_isStoredTrimmed_onTheStaffAndThePublicBookingForms() throws Exception {
        String dates = "\"checkIn\":\"" + TODAY.plusDays(3) + "\",\"checkOut\":\"" + TODAY.plusDays(5) + "\"";
        JsonNode staff = body(postJson("/bookings/staff", adminAuth,
                        "{\"roomId\":\"" + room.getId() + "\",\"guestName\":\"   Jane Doe \"," + dates + ",\"adults\":1,\"channel\":\"WALK_IN\"}")
                .andExpect(status().isCreated()));
        assertThat(staff.get("guestName").asText()).isEqualTo("Jane Doe");

        // The only unit is taken for those nights by the staff booking above - the public one asks for later ones.
        String laterDates = "\"checkIn\":\"" + TODAY.plusDays(10) + "\",\"checkOut\":\"" + TODAY.plusDays(12) + "\"";
        JsonNode pub = body(postPublicBooking(
                        "{\"roomId\":\"" + room.getId() + "\",\"guestName\":\"  Anna Smith  \",\"guestEmail\":\"a@test.local\",\"guestPhone\":\"+66 1234 5678\"," + laterDates + ",\"adults\":1}")
                .andExpect(status().isCreated()));
        assertThat(pub.get("guestName").asText()).isEqualTo("Anna Smith");
    }

    @Test
    void aRoomTypeName_andAGuestCardName_ofOnlySpaces_areA400() throws Exception {
        postJson("/rooms", adminAuth, "{\"name\":\"      \",\"description\":\"A description that is long enough\",\"capacity\":2,\"basePrice\":1500}")
                .andExpect(status().isBadRequest());
        postJson("/guests", adminAuth, "{\"name\":\"   \"}").andExpect(status().isBadRequest());

        JsonNode trimmed = body(postJson("/rooms", adminAuth, "{\"name\":\"  Padded Room Name  \",\"description\":\"A description that is long enough\",\"capacity\":2,\"basePrice\":1500}")
                .andExpect(status().isCreated()));
        assertThat(trimmed.get("name").asText()).isEqualTo("Padded Room Name");
    }

    @Test
    void aNameOfNoBreakSpacesOrZeroWidthCharacters_isA400_andTheyAreTrimmedAtTheEdges() throws Exception {
        // U+00A0, U+2003 (em space), U+200B (zero width space), U+FEFF (BOM) - none are Character.isWhitespace.
        String invisible = "\u00a0\u2003\u200b\ufeff";
        postJson("/rooms", adminAuth, "{\"name\":\"" + invisible + "\",\"description\":\"A description that is long enough\",\"capacity\":2,\"basePrice\":1500}")
                .andExpect(status().isBadRequest());
        postJson("/guests", adminAuth, "{\"name\":\"" + invisible + "\"}").andExpect(status().isBadRequest());

        JsonNode trimmed = body(postJson("/rooms", adminAuth, "{\"name\":\"" + invisible + "Padded Unicode Room" + invisible + "\",\"description\":\"A description that is long enough\",\"capacity\":2,\"basePrice\":1500}")
                .andExpect(status().isCreated()));
        assertThat(trimmed.get("name").asText()).isEqualTo("Padded Unicode Room");
    }

    // --- 7. Quantities ---------------------------------------------------------------------------------

    @Test
    void aPosLine_isOneToNinetyNine_onAdd_onMerge_andOnUpdate() throws Exception {
        Order order = orderService.create(new OrderCreateInput(), cashierA.getId());
        String items = "/orders/" + order.getId() + "/items";

        postJson(items, cashierAAuth, "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":100}]").andExpect(status().isBadRequest());
        postJson(items, cashierAAuth, "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":0}]").andExpect(status().isBadRequest());

        postJson(items, cashierAAuth, "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":99}]").andExpect(status().isCreated());
        // 99 + 1 would merge into one line of 100.
        postJson(items, cashierAAuth, "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":1}]").andExpect(status().isBadRequest());
        // Two entries in one request that merge past 99 are refused too.
        Order other = orderService.create(new OrderCreateInput(), cashierA.getId());
        postJson("/orders/" + other.getId() + "/items", cashierAAuth,
                        "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":60},{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":60}]")
                .andExpect(status().isBadRequest());
    }

    @Test
    void anOrderCreatedWithItsFirstLine_alsoHoldsTheLimit() throws Exception {
        postJson("/orders", cashierAAuth, "{\"items\":[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":100}]}").andExpect(status().isBadRequest());
    }

    // --- 8. The QR order: public menu only, same limits -------------------------------------------------

    @Test
    void aGuest_canOnlyOrderWhatIsOnThePublicMenu() throws Exception {
        Order order = orderService.create(new OrderCreateInput(), cashierA.getId());
        String token = orderRepository.findById(order.getId()).orElseThrow().getGuestAccessToken();
        MenuItemEntity hidden = menuItem("Rules Hidden", MenuDepartment.BAR, false, "90.00");
        MenuItemEntity treatment = menuItem("Rules Treatment", MenuDepartment.SPA, true, "1500.00");
        String url = "/public/orders/" + order.getId() + "/items?token=" + token;

        postJson(url, null, "[{\"menuItemId\":\"" + hidden.getId() + "\",\"quantity\":1}]").andExpect(status().isBadRequest());
        postJson(url, null, "[{\"menuItemId\":\"" + treatment.getId() + "\",\"quantity\":1}]").andExpect(status().isBadRequest());
        // One bad line in a request refuses all of it - nothing is added.
        postJson(url, null, "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":1},{\"menuItemId\":\"" + hidden.getId() + "\",\"quantity\":1}]")
                .andExpect(status().isBadRequest());
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getTotal()).isEqualByComparingTo("0.00");

        postJson(url, null, "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":2}]").andExpect(status().isCreated());
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void aGuest_cannotOrderMoreThanNinetyNineOfOneLine() throws Exception {
        Order order = orderService.create(new OrderCreateInput(), cashierA.getId());
        String token = orderRepository.findById(order.getId()).orElseThrow().getGuestAccessToken();
        String url = "/public/orders/" + order.getId() + "/items?token=" + token;

        postJson(url, null, "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":100}]").andExpect(status().isBadRequest());
        postJson(url, null, "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":99}]").andExpect(status().isCreated());
        postJson(url, null, "[{\"menuItemId\":\"" + beer.getId() + "\",\"quantity\":1}]").andExpect(status().isBadRequest());
    }

    // --- 5. Exports ---------------------------------------------------------------------------------

    @Test
    void theBookingsCsv_escapesAFormulaInAGuestName() throws Exception {
        String dates = "\"checkIn\":\"" + TODAY.plusDays(3) + "\",\"checkOut\":\"" + TODAY.plusDays(5) + "\"";
        postJson("/bookings/staff", adminAuth,
                        "{\"roomId\":\"" + room.getId() + "\",\"guestName\":\"=HYPERLINK(\\\"http://evil\\\",\\\"x\\\")\"," + dates + ",\"adults\":1,\"channel\":\"WALK_IN\"}")
                .andExpect(status().isCreated());

        flushAndForget();
        String csv = mockMvc.perform(get("/bookings/export").param("from", TODAY.toString()).param("to", TODAY.plusDays(30).toString()).header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(csv).contains("'=HYPERLINK(");
        assertThat(csv).doesNotContain(",=HYPERLINK(").doesNotContain("\r\n=HYPERLINK(");
    }

    @Test
    void theBookingsCsv_carriesTheRoomUnitTheBalanceAndAZonedTimestamp() throws Exception {
        String dates = "\"checkIn\":\"" + TODAY.plusDays(3) + "\",\"checkOut\":\"" + TODAY.plusDays(5) + "\"";
        postJson("/bookings/staff", adminAuth,
                        "{\"roomId\":\"" + room.getId() + "\",\"guestName\":\"Csv Columns Guest\"," + dates + ",\"adults\":1,\"channel\":\"WALK_IN\"}")
                .andExpect(status().isCreated());

        flushAndForget();
        String csv = mockMvc.perform(get("/bookings/export").param("from", TODAY.toString()).param("to", TODAY.plusDays(30).toString()).header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(csv).contains("Room unit").contains("Balance due").contains("Created at (Asia/Bangkok)");
        String row = csv.lines().filter(l -> l.contains("Csv Columns Guest")).findFirst().orElseThrow();
        // An unpaid two-night stay owes its whole price, and the timestamp names its offset.
        assertThat(row).containsPattern("[0-9]+[.][0-9]{2},[0-9]+[.][0-9]{2},NEW,");
        assertThat(row).endsWith("+07:00");
    }
}
