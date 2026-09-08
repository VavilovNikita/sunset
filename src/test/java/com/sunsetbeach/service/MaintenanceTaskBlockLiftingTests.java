package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AvailabilityDay;
import com.sunsetbeach.model.AvailabilityResponse;
import com.sunsetbeach.model.MaintenanceTask;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import com.sunsetbeach.model.RoomUnitAvailability;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * PATCH /maintenance-tasks/{id}/status, transitioning to DONE with a linked block - see
 * MaintenanceTaskService#liftBlock. Block dates are inclusive, so the room must not return to
 * sale until the day *after* the room is actually fixed: closing today moves the block's toDate
 * to yesterday, not today, which would still cover tonight. Every test here asserts on what
 * AvailabilityService actually reports afterwards, not just the stored toDate column - that's
 * the whole point of the change (a test that only checked the date column would pass while the
 * room stayed off sale).
 */
@SpringBootTest
@Transactional
class MaintenanceTaskBlockLiftingTests extends AbstractIntegrationTest {

    @Autowired
    private MaintenanceTaskService maintenanceTaskService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private RoomUnitBlockRepository roomUnitBlockRepository;

    @Autowired
    private UserRepository userRepository;

    private RoomUnitEntity unit;
    private String reporterId;

    @BeforeEach
    void setUp() {
        RoomEntity room = new RoomEntity();
        room.setName("Block Lifting Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by MaintenanceTaskBlockLiftingTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        RoomUnitEntity newUnit = new RoomUnitEntity();
        newUnit.setRoomId(savedRoom.getId());
        newUnit.setLabel("Block Lifting Test Unit " + UUID.randomUUID());
        newUnit.setActive(true);
        unit = roomUnitRepository.saveAndFlush(newUnit);

        UserEntity reporter = new UserEntity();
        reporter.setEmail("reporter-" + UUID.randomUUID() + "@example.com");
        reporter.setPasswordHash("irrelevant-for-this-test");
        reporter = userRepository.saveAndFlush(reporter);
        reporterId = reporter.getId();
    }

    private MaintenanceTask createTaskWithBlock(LocalDate fromDate, LocalDate toDate) {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "AC is broken", List.of(), reporterId);
        return maintenanceTaskService.addBlock(task.getId(), new RoomUnitBlockInput(fromDate.toString(), toDate.toString(), "AC repair")).getTask();
    }

    private boolean isBlockedToday(LocalDate today) {
        AvailabilityResponse response = availabilityService.getAvailability(unit.getRoomId(), null);
        AvailabilityDay todayEntry =
                response.getDays().stream().filter(d -> d.getDate().equals(today.toString())).findFirst().orElseThrow();
        RoomUnitAvailability unitEntry =
                todayEntry.getUnits().stream().filter(u -> u.getRoomUnitId().equals(unit.getId())).findFirst().orElseThrow();
        return unitEntry.getIsBlocked();
    }

    @Test
    void ordinaryCase_closingToday_shortensToDateToYesterday_andRoomIsAvailableTodayPerAvailabilityEngine() {
        LocalDate today = LocalDate.now();
        MaintenanceTask task = createTaskWithBlock(today.minusDays(3), today.plusDays(5));
        assertThat(isBlockedToday(today)).isTrue(); // sanity: blocked before closing

        MaintenanceTask closed = maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.DONE);

        assertThat(closed.getBlockId().get()).isNotNull();
        String blockId = closed.getBlockId().get();
        assertThat(roomUnitBlockRepository.findById(blockId)).isPresent();
        assertThat(roomUnitBlockRepository.findById(blockId).get().getToDate()).isEqualTo(today.minusDays(1));
        assertThat(isBlockedToday(today)).isFalse();
    }

    @Test
    void sameDayCase_blockCreatedAndClosedTheSameDay_isDeletedOutright_andRoomIsAvailableTodayPerAvailabilityEngine() {
        LocalDate today = LocalDate.now();
        // fromDate = today: yesterday (the computed new toDate) falls before fromDate, so the
        // block never covered a usable night once shortened - delete, don't write a backwards range.
        MaintenanceTask task = createTaskWithBlock(today, today.plusDays(7));
        assertThat(isBlockedToday(today)).isTrue(); // sanity: blocked before closing
        String blockId = task.getBlockId().get();

        MaintenanceTask closed = maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.DONE);

        assertThat(closed.getBlockId().get()).isNull();
        assertThat(roomUnitBlockRepository.findById(blockId)).isEmpty();
        assertThat(isBlockedToday(today)).isFalse();
    }

    @Test
    void blockAlreadyDeletedManually_closingStillSucceeds_andTouchesNoBlock() {
        LocalDate today = LocalDate.now();
        MaintenanceTask task = createTaskWithBlock(today.minusDays(1), today.plusDays(5));
        String blockId = task.getBlockId().get();
        roomUnitBlockRepository.deleteById(blockId);
        roomUnitBlockRepository.flush();

        MaintenanceTask closed = maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.DONE);

        assertThat(closed.getStatus()).isEqualTo(MaintenanceTaskStatus.DONE);
        assertThat(closed.getBlockId().get()).isNull();
        assertThat(roomUnitBlockRepository.findById(blockId)).isEmpty();
    }
}
