package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.model.RoomUnitBlockResult;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * POST /room-units/{id}/blocks warns rather than refuses when the blocked range overlaps a
 * non-CANCELLED booking on the unit - see RoomUnitService#createBlock and
 * RoomUnitBlockResult's own description for the overlap rule (matches how the calendar/
 * availability engine already compare a booking against a block).
 */
@SpringBootTest
@Transactional
class RoomUnitBlockOverlapWarningTests extends AbstractIntegrationTest {

    @Autowired
    private RoomUnitService roomUnitService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private RoomUnitBlockRepository roomUnitBlockRepository;

    @Autowired
    private com.sunsetbeach.repository.UserRepository userRepository;

    @Autowired
    private MaintenanceTaskService maintenanceTaskService;

    private RoomUnitEntity createUnit() {
        RoomEntity room = new RoomEntity();
        room.setName("Block Overlap Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by RoomUnitBlockOverlapWarningTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Block Overlap Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        return roomUnitRepository.saveAndFlush(unit);
    }

    private RoomUnitEntity createUnit(String roomId) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(roomId);
        unit.setLabel("Block Overlap Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        return roomUnitRepository.saveAndFlush(unit);
    }

    private Booking bookUnit(RoomUnitEntity unit, LocalDate checkIn, LocalDate checkOut) {
        return bookingService.createStaffBooking(new StaffBookingCreateInput(unit.getRoomId(), "Overlap Test Guest", checkIn.toString(), checkOut.toString())
                .roomUnitId(unit.getId()));
    }

    /** No {@code roomUnitId} - occupies the type's pool without pinning a specific physical room. */
    private Booking bookUnassigned(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return bookingService.createStaffBooking(new StaffBookingCreateInput(roomId, "Unassigned Overlap Guest", checkIn.toString(), checkOut.toString()));
    }

    @Test
    void blockOverlappingBooking_setsWarningAndListsBooking_butStillCreatesTheBlock() {
        RoomUnitEntity unit = createUnit();
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(3);
        Booking booking = bookUnit(unit, checkIn, checkOut);

        RoomUnitBlockResult result = roomUnitService.createBlock(
                unit.getId(), new RoomUnitBlockInput(checkIn.plusDays(1).toString(), checkOut.toString(), "Maintenance"), null);

        assertThat(result.getWarning().get()).isNotNull();
        assertThat(result.getAffectedBookings()).hasSize(1);
        assertThat(result.getAffectedBookings().get(0).getBookingId()).isEqualTo(booking.getId());
        assertThat(result.getAffectedBookings().get(0).getGuestName()).isEqualTo("Overlap Test Guest");
        assertThat(result.getAffectedBookings().get(0).getCheckIn()).isEqualTo(checkIn.toString());
        assertThat(result.getAffectedBookings().get(0).getCheckOut()).isEqualTo(checkOut.toString());
        assertThat(result.getAffectedBookings().get(0).getStatus()).isEqualTo(BookingStatus.NEW);
        assertThat(result.getAffectedUnassignedBookings()).isEmpty();

        assertThat(roomUnitBlockRepository.findById(result.getBlock().getId())).isPresent();
    }

    @Test
    void blockOnFreeRange_hasNullWarningAndNoAffectedBookings() {
        RoomUnitEntity unit = createUnit();
        LocalDate from = LocalDate.now().plusDays(60);
        LocalDate to = from.plusDays(2);

        RoomUnitBlockResult result = roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(from.toString(), to.toString(), "Maintenance"), null);

        assertThat(result.getWarning().get()).isNull();
        assertThat(result.getAffectedBookings()).isEmpty();
        assertThat(result.getAffectedUnassignedBookings()).isEmpty();
        assertThat(roomUnitBlockRepository.findById(result.getBlock().getId())).isPresent();
    }

    @Test
    void blockStartingOnBookingsCheckOutDay_doesNotWarn() {
        RoomUnitEntity unit = createUnit();
        LocalDate checkIn = LocalDate.now().plusDays(90);
        LocalDate checkOut = checkIn.plusDays(4);
        bookUnit(unit, checkIn, checkOut);

        RoomUnitBlockResult result =
                roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(checkOut.toString(), checkOut.plusDays(2).toString(), "Maintenance"), null);

        assertThat(result.getWarning().get()).isNull();
        assertThat(result.getAffectedBookings()).isEmpty();
        assertThat(result.getAffectedUnassignedBookings()).isEmpty();
    }

    @Test
    void blockOverlappingUnassignedBookingOfSameType_warnsAndListsItSeparatelyFromAssignedBookings() {
        RoomUnitEntity unit = createUnit();
        // A second unit of the same type - the unassigned booking below belongs to the type as a
        // whole (either physical unit could end up serving it), not to `unit` specifically.
        createUnit(unit.getRoomId());
        LocalDate checkIn = LocalDate.now().plusDays(120);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking unassigned = bookUnassigned(unit.getRoomId(), checkIn, checkOut);

        RoomUnitBlockResult result = roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(checkIn.toString(), checkOut.toString(), "Maintenance"), null);

        assertThat(result.getWarning().get()).isNotNull();
        assertThat(result.getAffectedBookings()).isEmpty();
        assertThat(result.getAffectedUnassignedBookings()).hasSize(1);
        assertThat(result.getAffectedUnassignedBookings().get(0).getBookingId()).isEqualTo(unassigned.getId());
        assertThat(result.getAffectedUnassignedBookings().get(0).getGuestName()).isEqualTo("Unassigned Overlap Guest");
    }

    @Test
    void blockOverlappingBothAssignedAndUnassignedBookings_listsBothDistinctly() {
        RoomUnitEntity unit = createUnit();
        createUnit(unit.getRoomId()); // a second unit of the type, so both bookings below actually fit
        LocalDate checkIn = LocalDate.now().plusDays(150);
        LocalDate checkOut = checkIn.plusDays(3);
        Booking assigned = bookUnit(unit, checkIn, checkOut);
        Booking unassigned = bookUnassigned(unit.getRoomId(), checkIn, checkOut);

        RoomUnitBlockResult result = roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(checkIn.toString(), checkOut.toString(), "Maintenance"), null);

        assertThat(result.getAffectedBookings()).extracting("bookingId").containsExactly(assigned.getId());
        assertThat(result.getAffectedUnassignedBookings()).extracting("bookingId").containsExactly(unassigned.getId());
    }

    @Test
    void unassignedBookingOnADifferentRoomType_doesNotWarn() {
        RoomUnitEntity unit = createUnit();
        RoomUnitEntity otherTypeUnit = createUnit(); // createUnit() with no arg makes its own fresh Room (type)
        LocalDate checkIn = LocalDate.now().plusDays(180);
        LocalDate checkOut = checkIn.plusDays(2);
        bookUnassigned(otherTypeUnit.getRoomId(), checkIn, checkOut);

        RoomUnitBlockResult result = roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(checkIn.toString(), checkOut.toString(), "Maintenance"), null);

        assertThat(result.getWarning().get()).isNull();
        assertThat(result.getAffectedUnassignedBookings()).isEmpty();
    }

    // --- createdByEmail / maintenanceTask --------------------------------------------------------

    @Test
    void createBlock_recordsAndDenormalizesTheCreatingUsersEmail() {
        RoomUnitEntity unit = createUnit();
        com.sunsetbeach.entity.UserEntity user = new com.sunsetbeach.entity.UserEntity();
        user.setEmail("block-creator-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user = userRepository.saveAndFlush(user);
        LocalDate from = LocalDate.now().plusDays(200);

        RoomUnitBlockResult result =
                roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(from.toString(), from.plusDays(1).toString(), "Maintenance"), user.getId());

        assertThat(result.getBlock().getCreatedByEmail().get()).isEqualTo(user.getEmail());
        // Same fact, read back through the list endpoint, not just the create response.
        assertThat(roomUnitService.listBlocks(unit.getId()).get(0).getCreatedByEmail().get()).isEqualTo(user.getEmail());
    }

    @Test
    void createBlock_withNoActor_hasNullCreatedByEmail_notAnErrorOrAPlaceholder() {
        RoomUnitEntity unit = createUnit();
        LocalDate from = LocalDate.now().plusDays(201);

        RoomUnitBlockResult result =
                roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(from.toString(), from.plusDays(1).toString(), "Maintenance"), null);

        assertThat(result.getBlock().getCreatedByEmail().get()).isNull();
        assertThat(roomUnitService.listBlocks(unit.getId()).get(0).getCreatedByEmail().get()).isNull();
    }

    @Test
    void listBlocks_reportsTheMaintenanceTaskLinkedToIt() {
        RoomUnitEntity unit = createUnit();
        com.sunsetbeach.entity.UserEntity reporter = new com.sunsetbeach.entity.UserEntity();
        reporter.setEmail("block-reporter-" + UUID.randomUUID() + "@example.com");
        reporter.setPasswordHash("irrelevant-for-this-test");
        reporter = userRepository.saveAndFlush(reporter);
        com.sunsetbeach.model.MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "AC is leaking", java.util.List.of(), reporter.getId());
        LocalDate from = LocalDate.now().plusDays(202);

        com.sunsetbeach.model.MaintenanceTaskBlockResult blockResult =
                maintenanceTaskService.addBlock(task.getId(), new RoomUnitBlockInput(from.toString(), from.plusDays(1).toString(), "AC repair"), reporter.getId());

        com.sunsetbeach.model.RoomUnitBlockMaintenanceTask taskDto =
                roomUnitService.listBlocks(unit.getId()).get(0).getMaintenanceTask();
        assertThat(taskDto.getTaskId()).isEqualTo(task.getId());
        assertThat(taskDto.getDescription()).isEqualTo("AC is leaking");
        assertThat(taskDto.getStatus()).isEqualTo(com.sunsetbeach.model.MaintenanceTaskStatus.OPEN);
        // The create response itself never has one yet - see RoomUnitMapper#toDto's own comment.
        assertThat(blockResult.getBlockResult().getBlock().getMaintenanceTask()).isNull();
    }

    @Test
    void listBlocks_withNoLinkedTask_hasNullMaintenanceTask() {
        RoomUnitEntity unit = createUnit();
        LocalDate from = LocalDate.now().plusDays(203);
        roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(from.toString(), from.plusDays(1).toString(), "Maintenance"), null);

        assertThat(roomUnitService.listBlocks(unit.getId()).get(0).getMaintenanceTask()).isNull();
    }
}
