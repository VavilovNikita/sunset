package com.sunsetbeach.contract;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.BookingService;
import com.sunsetbeach.service.RoomUnitService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * A single field-name-driven check, run against the real controller/security/serialization
 * stack over the real dev DB: every JSON field that names a *calendar day* - {@code checkIn},
 * {@code checkOut}, {@code date}, {@code fromDate}, {@code toDate}, {@code from}, {@code to},
 * {@code effectiveDate}, {@code splitDate} - must be a plain {@code YYYY-MM-DD} string, never a
 * datetime. Timestamp fields ({@code createdAt}, {@code updatedAt}, {@code paidAt},
 * {@code openedAt}, ...) are correctly excluded by name, not asserted on at all: this is not a
 * blanket "no datetimes" check, only the narrower claim that a field naming a stay date can never
 * regress into one.
 *
 * <p>This exists because {@code Booking.checkIn}/{@code checkOut} spent a long time serializing
 * as a datetime with a legacy {@code T00:00:00.000Z} suffix while every sibling schema
 * ({@code BookingSegment}, {@code CalendarBooking}, {@code RoomUnitBlock},
 * {@code RoomTypeDailyAvailability}, {@code AvailabilityDay}) already used a plain date - a
 * mismatch that caused three separate incidents (one shipped: a dashboard revenue/occupancy bug;
 * two caught in review while building the booking calendar) before the format was unified. A
 * warning in a schema's description didn't prevent any of the three - nobody reads a field's doc
 * comment before writing {@code booking.checkIn}, and openapi-generator silently drops hand
 * edits on every regeneration anyway. A name-keyed assertion over the *actual* response body
 * does not depend on anyone reading anything, and - critically - it is keyed by field *name*,
 * not by schema or endpoint, so it keeps catching the same mistake even inside a brand new
 * schema introduced by a future task, as long as that schema reuses one of these names (which
 * every stay-date field in this API always has, deliberately, for exactly this reason).
 *
 * <p>Endpoints covered, chosen to hit every schema the checkIn/checkOut-adjacent family
 * currently reaches: {@code GET /bookings/{id}} ({@code Booking} plus its nested
 * {@code BookingSegment[]} - a relocated, two-segment booking exercises both the top-level and
 * nested cases in one call), {@code GET /bookings/calendar} ({@code BookingCalendarResponse}'s
 * own {@code from}/{@code to}, its {@code bookings[]} ({@code CalendarBooking}), its
 * {@code blocks[]} ({@code RoomUnitBlock}), and its {@code roomTypes[].dailyAvailable[]}
 * ({@code RoomTypeDailyAvailability}) all in one response), {@code GET /availability/{roomId}}
 * ({@code AvailabilityDay}), and {@code GET /room-units/{id}/blocks} ({@code RoomUnitBlock},
 * hit directly as its own endpoint too, not just via the calendar). Every other endpoint that
 * returns one of these same schemas (e.g. {@code POST /bookings/{id}/relocate} also returning
 * {@code Booking}) is covered by construction, since it's built by the same mapper.
 *
 * <p>DB-backed against the real dev Postgres, same convention as {@code BookingRelocationTests}/
 * {@code BookingAvailabilityEngineTests} - disposable rows tracked and deleted in
 * {@link #cleanUp()}, manual dev-DB data never touched. Unlike those, this drives the endpoints
 * through {@link MockMvc} rather than calling the service layer directly - {@code
 * @AutoConfigureMockMvc} on a full {@code @SpringBootTest} (not a {@code @WebMvcTest} slice)
 * builds it against the real application context, so the request passes through the real
 * security filter chain and the response body is produced by the real controller and the real
 * Jackson {@code HttpMessageConverter} - byte-for-byte the same JSON a real HTTP client would
 * receive. Only the actual TCP socket is skipped, which is irrelevant to what this test checks.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DateOnlyFieldsContractTests extends AbstractIntegrationTest {

    // Field names that mean "a calendar day" anywhere in this API - see the class javadoc. Not
    // "date" fields in general: createdAt/updatedAt/paidAt/openedAt/closedAt are deliberately not
    // in this set, since those are real timestamps and are supposed to stay datetimes.
    private static final Set<String> CALENDAR_DAY_FIELD_NAMES =
            Set.of("checkIn", "checkOut", "date", "fromDate", "toDate", "from", "to", "effectiveDate", "splitDate");
    private static final Pattern DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingService bookingService;

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
    private RatePlanRepository ratePlanRepository;

    private final List<String> createdRoomIds = new ArrayList<>();
    private String testUserId;
    private String authHeader;

    @BeforeEach
    void setUp() {
        // Issued via the real JwtService bean and backed by a real, disposable User row (rather
        // than a full password login) - the same "issue a token directly" idiom
        // PosRoleHierarchyTests uses for its @WebMvcTest slices, applied here to a real
        // @SpringBootTest server whose JwtAuthFilter genuinely re-checks active/tokenVersion
        // against the DB on every request, so a real row is required. MANAGER covers every
        // endpoint under test (GET /room-units/**'s blocks sub-resource is MANAGER-only; the
        // rest are CASHIER+, and ROLE_MANAGER > ROLE_CASHIER in the configured hierarchy).
        UserEntity user = new UserEntity();
        user.setEmail("contract-test-" + UUID.randomUUID() + "@test.local");
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
        List<String> bookingIdsToDelete = createdRoomIds.isEmpty()
                ? List.of()
                : bookingRepository.findAll().stream()
                        .filter(b -> createdRoomIds.contains(b.getRoomId()))
                        .map(com.sunsetbeach.entity.BookingEntity::getId)
                        .distinct()
                        .toList();
        if (!bookingIdsToDelete.isEmpty()) {
            bookingRepository.deleteAllById(bookingIdsToDelete);
        }
        for (String roomId : createdRoomIds) {
            ratePlanRepository.deleteAll(
                    ratePlanRepository.findByRoomIdAndDateBetween(roomId, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1)));
            for (RoomUnitEntity unit : roomUnitRepository.findByRoomId(roomId)) {
                roomUnitBlockRepository.deleteAll(roomUnitBlockRepository.findByRoomUnitId(unit.getId()));
            }
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
        createdRoomIds.clear();
        if (testUserId != null) {
            userRepository.deleteById(testUserId);
        }
    }

    private RoomEntity createRoom(int activeUnitCount) {
        RoomEntity room = new RoomEntity();
        room.setName("Contract Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by DateOnlyFieldsContractTests");
        room.setCapacity(2);
        room.setBasePrice(new java.math.BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        createdRoomIds.add(saved.getId());
        for (int i = 0; i < activeUnitCount; i++) {
            RoomUnitEntity unit = new RoomUnitEntity();
            unit.setRoomId(saved.getId());
            unit.setLabel("Contract Test Unit " + UUID.randomUUID());
            unit.setActive(true);
            roomUnitRepository.saveAndFlush(unit);
        }
        return saved;
    }

    private JsonNode getJson(String path) throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.get(path).header("Authorization", authHeader))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    /**
     * Recursively walks every field of the response tree; any field whose *name* is in
     * {@link #CALENDAR_DAY_FIELD_NAMES} must be a plain {@code YYYY-MM-DD} string. Matching is by
     * field name alone, not by path or schema, so a newly regenerated nested schema that reuses
     * one of these names is covered automatically, without this test needing to know it exists.
     * Returns how many such fields were found, so callers can assert that at least one was
     * actually exercised - an empty result here would otherwise let this test pass vacuously if
     * the endpoint's response shape ever changed out from under it.
     */
    private static int checkCalendarDayFields(JsonNode node, String path, List<String> violations) {
        int checked = 0;
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                String childPath = path + "." + entry.getKey();
                JsonNode value = entry.getValue();
                if (CALENDAR_DAY_FIELD_NAMES.contains(entry.getKey()) && value.isTextual()) {
                    checked++;
                    if (!DATE_ONLY.matcher(value.asText()).matches()) {
                        violations.add(childPath + " = \"" + value.asText() + "\" (expected YYYY-MM-DD)");
                    }
                }
                checked += checkCalendarDayFields(value, childPath, violations);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                checked += checkCalendarDayFields(node.get(i), path + "[" + i + "]", violations);
            }
        }
        return checked;
    }

    private static void assertDateOnly(JsonNode root, int minimumExpected) {
        List<String> violations = new ArrayList<>();
        int checked = checkCalendarDayFields(root, "$", violations);
        assertThat(violations).as("calendar-day fields must be plain YYYY-MM-DD, found violations: %s", violations).isEmpty();
        assertThat(checked)
                .as("expected to find at least %d calendar-day field(s) in the response, found %d - the response shape may have "
                        + "changed and this test's coverage silently dropped; check test setup and the field-name allowlist", minimumExpected, checked)
                .isGreaterThanOrEqualTo(minimumExpected);
    }

    @Test
    void booking_topLevelAndSegments_areDateOnly() throws Exception {
        RoomEntity room = createRoom(2);
        LocalDate checkIn = LocalDate.now().plusDays(300);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        Booking booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), "Contract Guest", checkIn.toString(), checkOut.toString())
                        .roomUnitId(unitA.getId()));
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), room.getId()).roomUnitId(unitB.getId()));

        JsonNode json = getJson("/bookings/" + booking.getId());
        // Booking.checkIn/checkOut (2) + two segments' own checkIn/checkOut (4) = 6.
        assertDateOnly(json, 6);
    }

    @Test
    void calendar_bookingsBlocksAndDailyAvailable_areDateOnly() throws Exception {
        RoomEntity room = createRoom(2);
        LocalDate checkIn = LocalDate.now().plusDays(305);
        LocalDate checkOut = checkIn.plusDays(3);
        RoomUnitEntity unitA = roomUnitRepository.findByRoomId(room.getId()).get(0);
        RoomUnitEntity unitB = roomUnitRepository.findByRoomId(room.getId()).get(1);

        bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), "Contract Guest", checkIn.toString(), checkOut.toString())
                        .roomUnitId(unitA.getId()));
        roomUnitService.createBlock(
                unitB.getId(), new RoomUnitBlockInput(checkIn.toString(), checkOut.toString(), "Contract test block"));

        String from = checkIn.minusDays(1).toString();
        String to = checkOut.plusDays(1).toString();
        JsonNode json = getJson("/bookings/calendar?from=" + from + "&to=" + to);
        // from/to (2) + booking checkIn/checkOut (2) + block fromDate/toDate (2) + at least one
        // dailyAvailable.date entry for the range (>=1) = 7 at minimum.
        assertDateOnly(json, 7);
    }

    @Test
    void availability_perDay_isDateOnly() throws Exception {
        RoomEntity room = createRoom(1);
        String month = LocalDate.now().plusMonths(3).toString().substring(0, 7);

        JsonNode json = getJson("/availability/" + room.getId() + "?month=" + month);
        assertDateOnly(json, 1);
    }

    @Test
    void roomUnitBlocks_areDateOnly() throws Exception {
        RoomEntity room = createRoom(1);
        RoomUnitEntity unit = roomUnitRepository.findByRoomId(room.getId()).get(0);
        LocalDate from = LocalDate.now().plusDays(310);
        LocalDate to = from.plusDays(2);
        roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(from.toString(), to.toString(), "Contract test block"));

        JsonNode json = getJson("/room-units/" + unit.getId() + "/blocks");
        assertDateOnly(json, 1);
    }
}
