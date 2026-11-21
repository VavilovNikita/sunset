package com.sunsetbeach.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.GuestBookingView;
import com.sunsetbeach.model.OccupancyStatus;
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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Tentative/Confirmed at creation, the stay's staff notes, and {@code GET /reports/future-bookings}. Dates are fixed
 * far-future values (CLAUDE.md, Tests: nothing here reads the wall clock). The report is always asked for an explicit
 * range on the test's own dates, so other tests' bookings can't leak in.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingNotesAndFutureBookingsContractTests extends AbstractIntegrationTest {

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

    private final List<String> bookingIds = new ArrayList<>();
    private final List<String> roomIds = new ArrayList<>();
    private final List<String> userIds = new ArrayList<>();
    private String managerAuth;
    private String cashierAuth;
    private String roomId;

    @BeforeEach
    void setUp() {
        managerAuth = authFor(Role.MANAGER);
        cashierAuth = authFor(Role.CASHIER);
        roomId = createRoom();
    }

    @AfterEach
    void cleanUp() {
        for (String bookingId : bookingIds) {
            bookingRepository.findById(bookingId).ifPresent(booking -> {
                String guestId = booking.getGuestId();
                bookingSegmentRepository.deleteAll(bookingSegmentRepository.findByBookingIdOrderByCheckInAsc(bookingId));
                bookingRepository.delete(booking);
                if (guestId != null) {
                    auditLogRepository.deleteAll(auditLogRepository.findAll().stream().filter(e -> guestId.equals(e.getEntityId())).toList());
                    guestRepository.findById(guestId).ifPresent(guestRepository::delete);
                }
            });
        }
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                .filter(e -> userIds.contains(e.getActorUserId()) || bookingIds.contains(e.getEntityId()))
                .toList());
        for (String id : roomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(id));
            roomRepository.deleteById(id);
        }
        userRepository.deleteAllById(userIds);
    }

    // --- status at creation ----------------------------------------------------------------------

    @Test
    void creation_withoutStatus_isTentativeNew_asBefore() throws Exception {
        JsonNode booking = create("2097-01-10", "2097-01-12", "");
        assertThat(booking.get("status").asText()).isEqualTo("NEW");
    }

    @Test
    void creation_withConfirmed_isConfirmed_andTheAuditEntryNamesIt() throws Exception {
        JsonNode booking = create("2097-01-10", "2097-01-12", ",\"status\":\"CONFIRMED\"");
        assertThat(booking.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(bookingRepository.findById(booking.get("id").asText()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(auditFor(booking.get("id").asText(), AuditAction.BOOKING_CREATED)).anyMatch(e -> e.getSummary().contains("status CONFIRMED"));
    }

    @Test
    void creation_withPaidOrCancelled_isRejected_andNothingIsWritten() throws Exception {
        long before = bookingRepository.count();
        for (String status : List.of("PAID", "CANCELLED")) {
            doPost(managerAuth, body("2097-01-10", "2097-01-12", ",\"status\":\"" + status + "\"")).andExpect(status().isBadRequest());
        }
        assertThat(bookingRepository.count()).isEqualTo(before);
    }

    // --- notes -----------------------------------------------------------------------------------

    @Test
    void notes_areSavedTrimmed_andBlankMeansNone() throws Exception {
        JsonNode withNotes = create("2097-02-10", "2097-02-12", ",\"notes\":\"  Airport pickup 14:00  \"");
        assertThat(withNotes.get("notes").asText()).isEqualTo("Airport pickup 14:00");

        JsonNode blank = create("2097-02-20", "2097-02-22", ",\"notes\":\"   \"");
        assertThat(blank.get("notes").isNull()).isTrue();
        assertThat(bookingRepository.findById(blank.get("id").asText()).orElseThrow().getNotes()).isNull();
    }

    @Test
    void notes_overAThousandCharacters_isA400_atCreationAndOnPatch() throws Exception {
        String tooLong = "x".repeat(1001);
        doPost(managerAuth, body("2097-02-10", "2097-02-12", ",\"notes\":\"" + tooLong + "\"")).andExpect(status().isBadRequest());

        JsonNode booking = create("2097-02-10", "2097-02-12", "");
        doPatch(cashierAuth, booking.get("id").asText(), "{\"status\":\"NEW\",\"notes\":\"" + tooLong + "\"}").andExpect(status().isBadRequest());

        // Exactly 1000 is fine.
        String max = "y".repeat(1000);
        JsonNode ok = objectMapper.readTree(doPatch(cashierAuth, booking.get("id").asText(), "{\"status\":\"NEW\",\"notes\":\"" + max + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(ok.get("notes").asText()).hasSize(1000);
    }

    @Test
    void notes_patch_setsKeepsAndClears() throws Exception {
        JsonNode booking = create("2097-03-10", "2097-03-12", "");
        String id = booking.get("id").asText();

        JsonNode set = patchOk(cashierAuth, id, "{\"status\":\"NEW\",\"notes\":\"Allergic to peanuts\"}");
        assertThat(set.get("notes").asText()).isEqualTo("Allergic to peanuts");

        // Not sent: untouched.
        JsonNode kept = patchOk(cashierAuth, id, "{\"status\":\"CONFIRMED\"}");
        assertThat(kept.get("notes").asText()).isEqualTo("Allergic to peanuts");

        // null clears.
        JsonNode cleared = patchOk(cashierAuth, id, "{\"status\":\"CONFIRMED\",\"notes\":null}");
        assertThat(cleared.get("notes").isNull()).isTrue();
        assertThat(bookingRepository.findById(id).orElseThrow().getNotes()).isNull();
    }

    @Test
    void notes_auditSaysTheyChanged_butNeverWhatTheySay() throws Exception {
        JsonNode booking = create("2097-03-10", "2097-03-12", ",\"notes\":\"Shellfish allergy SECRET-A\"");
        String id = booking.get("id").asText();
        patchOk(cashierAuth, id, "{\"status\":\"NEW\",\"notes\":\"Changed text SECRET-B\"}");
        patchOk(cashierAuth, id, "{\"status\":\"NEW\",\"notes\":null}");

        List<AuditLogEntity> entries = auditLogRepository.findAll().stream().filter(e -> id.equals(e.getEntityId())).toList();
        assertThat(entries).extracting(AuditLogEntity::getSummary).anyMatch(t -> t.contains("notes added"));
        assertThat(auditFor(id, AuditAction.BOOKING_STATUS_CHANGED))
                .extracting(AuditLogEntity::getSummary)
                .anyMatch(t -> t.toLowerCase().contains("notes changed"))
                .anyMatch(t -> t.toLowerCase().contains("notes removed"));
        assertThat(entries).extracting(AuditLogEntity::getSummary).noneMatch(t -> t.contains("SECRET") || t.contains("Shellfish"));
    }

    @Test
    void notes_areNotInAnyGuestFacingShape_andRedactedInToString() throws Exception {
        assertThat(GuestBookingView.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName).doesNotContain("notes");
        JsonNode booking = create("2097-03-10", "2097-03-12", ",\"notes\":\"Allergic to peanuts\"");
        com.sunsetbeach.model.StaffBookingCreateInput input = new com.sunsetbeach.model.StaffBookingCreateInput().notes("Allergic to peanuts");
        assertThat(input.toString()).doesNotContain("peanuts");
        com.sunsetbeach.model.BookingStatusInput statusInput = new com.sunsetbeach.model.BookingStatusInput().notes("Allergic to peanuts");
        assertThat(statusInput.toString()).doesNotContain("peanuts");
        assertThat(booking.get("notes").asText()).isEqualTo("Allergic to peanuts");
    }

    // --- GET /reports/future-bookings --------------------------------------------------------------

    @Test
    void report_defaultsToTentativeConfirmedAndPaid_withoutCancelledOrNoShow() throws Exception {
        String tentative = create("2097-05-10", "2097-05-12", "").get("id").asText();
        String confirmed = create("2097-05-11", "2097-05-13", ",\"status\":\"CONFIRMED\"").get("id").asText();
        String paid = create("2097-05-12", "2097-05-14", "").get("id").asText();
        // Set directly: going through PAID would post a payment and ledger entries a test can't clean up.
        BookingEntity paidEntity = bookingRepository.findById(paid).orElseThrow();
        paidEntity.setStatus(BookingStatus.PAID);
        bookingRepository.saveAndFlush(paidEntity);
        String cancelled = create("2097-05-13", "2097-05-15", "").get("id").asText();
        patchOk(managerAuth, cancelled, "{\"status\":\"CANCELLED\"}");
        String noShow = create("2097-05-14", "2097-05-16", "").get("id").asText();
        BookingEntity ns = bookingRepository.findById(noShow).orElseThrow();
        ns.setOccupancyStatus(OccupancyStatus.NO_SHOW);
        bookingRepository.saveAndFlush(ns);

        JsonNode report = report("from=2097-05-01&to=2097-05-31");
        assertThat(ids(report)).containsExactly(tentative, confirmed, paid);
        assertThat(report.get("statuses")).hasSize(3);
        assertThat(report.get("summary")).hasSize(3);
    }

    @Test
    void report_statusFilter_andCancelledIsRefused() throws Exception {
        String tentative = create("2097-06-10", "2097-06-12", "").get("id").asText();
        String confirmed = create("2097-06-11", "2097-06-13", ",\"status\":\"CONFIRMED\"").get("id").asText();

        assertThat(ids(report("from=2097-06-01&to=2097-06-30&status=NEW"))).containsExactly(tentative);
        assertThat(ids(report("from=2097-06-01&to=2097-06-30&status=CONFIRMED"))).containsExactly(confirmed);
        assertThat(ids(report("from=2097-06-01&to=2097-06-30&status=NEW&status=CONFIRMED"))).containsExactly(tentative, confirmed);
        mockMvc.perform(get("/reports/future-bookings?from=2097-06-01&to=2097-06-30&status=CANCELLED").header("Authorization", managerAuth))
                .andExpect(status().isBadRequest());
    }

    @Test
    void report_rangeIsOnCheckIn_inclusiveOnBothEnds() throws Exception {
        String before = create("2097-07-09", "2097-07-20", "").get("id").asText();
        String first = create("2097-07-10", "2097-07-12", "").get("id").asText();
        String last = create("2097-07-20", "2097-07-22", "").get("id").asText();
        String after = create("2097-07-21", "2097-07-23", "").get("id").asText();

        // A stay that began before `from` but is still in the house is not an arrival in the range.
        assertThat(ids(report("from=2097-07-10&to=2097-07-20"))).containsExactly(first, last).doesNotContain(before, after);
    }

    @Test
    void report_rowsAndSummary_carryTheBookingsOwnFigures() throws Exception {
        JsonNode created = create("2097-08-10", "2097-08-13", ",\"status\":\"CONFIRMED\",\"notes\":\"Airport pickup\",\"adults\":2,\"children\":1");
        String id = created.get("id").asText();

        JsonNode report = report("from=2097-08-01&to=2097-08-31");
        JsonNode row = report.get("rows").get(0);
        assertThat(row.get("bookingId").asText()).isEqualTo(id);
        assertThat(row.get("nights").asInt()).isEqualTo(3);
        assertThat(row.get("adults").asInt()).isEqualTo(2);
        assertThat(row.get("children").asInt()).isEqualTo(1);
        assertThat(row.get("notes").asText()).isEqualTo("Airport pickup");
        assertThat(row.get("roomName").asText()).startsWith("Future Test Room");
        assertThat(row.get("totalPrice").asText()).isEqualTo("3000.00");
        // balanceDue is the one the booking itself shows.
        JsonNode single = objectMapper.readTree(mockMvc.perform(get("/bookings/" + id).header("Authorization", managerAuth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(row.get("balanceDue").asText()).isEqualTo(single.get("balanceDue").asText());

        JsonNode confirmedSummary = report.get("summary").get(1);
        assertThat(confirmedSummary.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmedSummary.get("bookings").asInt()).isEqualTo(1);
        assertThat(confirmedSummary.get("nights").asInt()).isEqualTo(3);
        assertThat(confirmedSummary.get("totalPrice").asText()).isEqualTo("3000.00");
        assertThat(report.get("summary").get(0).get("bookings").asInt()).isZero();
    }

    @Test
    void report_isManagerPlus() throws Exception {
        mockMvc.perform(get("/reports/future-bookings?from=2097-09-01&to=2097-09-30").header("Authorization", cashierAuth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reports/future-bookings/export?from=2097-09-01&to=2097-09-30").header("Authorization", cashierAuth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reports/future-bookings")).andExpect(status().isUnauthorized());
    }

    @Test
    void export_usesTheLabels_putsNotesInTheirOwnColumn_andEscapesFormulas() throws Exception {
        create("2097-10-10", "2097-10-12", ",\"status\":\"CONFIRMED\",\"notes\":\"=HYPERLINK(\\\"http://x\\\")\"");

        String csv = mockMvc.perform(get("/reports/future-bookings/export?from=2097-10-01&to=2097-10-31").header("Authorization", managerAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("Notes").contains("Confirmed");
        // The channel is worded as on screen ("Phone"), not the stored PHONE; same BOM + sep=, opening as GET /bookings/export.
        assertThat(csv).contains(",Phone,").doesNotContain(",PHONE,");
        assertThat(csv).startsWith(String.valueOf((char) 0xFEFF) + "sep=,");
        assertThat(csv).contains("'=HYPERLINK").doesNotContain(",=HYPERLINK");
        assertThat(auditLogRepository.findAll()).anyMatch(e -> e.getAction() == AuditAction.BOOKINGS_EXPORTED && e.getSummary().contains("future booking"));
    }

    // --- helpers ---------------------------------------------------------------------------------

    private JsonNode report(String query) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/reports/future-bookings?" + query).header("Authorization", managerAuth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static List<String> ids(JsonNode report) {
        List<String> ids = new ArrayList<>();
        report.get("rows").forEach(r -> ids.add(r.get("bookingId").asText()));
        return ids;
    }

    private List<AuditLogEntity> auditFor(String bookingId, AuditAction action) {
        return auditLogRepository.findAll().stream().filter(e -> bookingId.equals(e.getEntityId()) && e.getAction() == action).toList();
    }

    private JsonNode create(String checkIn, String checkOut, String extra) throws Exception {
        String response = doPost(managerAuth, body(checkIn, checkOut, extra)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode booking = objectMapper.readTree(response);
        bookingIds.add(booking.get("id").asText());
        return booking;
    }

    private String body(String checkIn, String checkOut, String extra) {
        String guests = extra.contains("\"adults\"") ? "" : ",\"adults\":2";
        return "{\"roomId\":\"" + roomId + "\",\"guestName\":\"Future Guest\",\"guestEmail\":\"future-" + UUID.randomUUID() + "@test.local\","
                + "\"checkIn\":\"" + checkIn + "\",\"checkOut\":\"" + checkOut + "\",\"channel\":\"PHONE\"" + guests + extra + "}";
    }

    private ResultActions doPost(String auth, String json) throws Exception {
        return mockMvc.perform(post("/bookings/staff").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions doPatch(String auth, String id, String json) throws Exception {
        return mockMvc.perform(patch("/bookings/" + id).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private JsonNode patchOk(String auth, String id, String json) throws Exception {
        return objectMapper.readTree(doPatch(auth, id, json).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String authFor(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("future-bookings-" + role + "-" + UUID.randomUUID() + "@test.local");
        user.setName(user.getEmail());
        user.setPasswordHash("unused");
        user.setRole(role);
        user.setActive(true);
        user.setTokenVersion(0);
        UserEntity saved = userRepository.saveAndFlush(user);
        userIds.add(saved.getId());
        return "Bearer " + jwtService.issue(new StaffPrincipal(saved.getId(), saved.getEmail(), role));
    }

    private String createRoom() {
        RoomEntity room = new RoomEntity();
        room.setName("Future Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingNotesAndFutureBookingsContractTests");
        room.setCapacity(4);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        roomIds.add(savedRoom.getId());
        // Several units: the tests book overlapping dates in this one room type.
        for (int i = 0; i < 6; i++) {
            RoomUnitEntity unit = new RoomUnitEntity();
            unit.setRoomId(savedRoom.getId());
            unit.setLabel("Future Unit " + i + " " + UUID.randomUUID());
            unit.setActive(true);
            roomUnitRepository.saveAndFlush(unit);
        }
        return savedRoom.getId();
    }
}
