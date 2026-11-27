package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.support.FixedHotelClockConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * An arrival checking into a room while an overstaying guest is being moved into the same room: never both. Only the
 * occupancy check stands between them (the overdue guest's agreed nights are past, so no availability check sees the
 * arrival), and the two writes run at different isolation levels - hence a real race.
 *
 * <p>Not {@code @Transactional}: the race needs two real transactions. Cleans up what it wrote.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
class CheckInVersusMoveRaceTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private JdbcTemplate jdbc;

    private final List<String> bookingIds = new ArrayList<>();
    private final List<String> unitIds = new ArrayList<>();
    private final List<String> roomIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String id : bookingIds) {
            String charges = "SELECT id FROM \"OverstayCharge\" WHERE \"bookingId\" = ?";
            // The charge rows point at their ledger entries, so they go first.
            List<String> chargeIds = jdbc.queryForList(charges, String.class, id);
            jdbc.update("DELETE FROM \"OverstayCharge\" WHERE \"bookingId\" = ?", id);
            for (String chargeId : chargeIds) {
                jdbc.update("DELETE FROM \"JournalLine\" WHERE \"entryId\" IN (SELECT id FROM \"JournalEntry\" WHERE \"sourceId\" = ?)", chargeId);
                jdbc.update("DELETE FROM \"JournalEntry\" WHERE \"sourceId\" = ? AND \"reversesEntryId\" IS NOT NULL", chargeId);
                jdbc.update("DELETE FROM \"JournalEntry\" WHERE \"sourceId\" = ?", chargeId);
            }
            jdbc.update("DELETE FROM \"AuditLog\" WHERE \"entityId\" = ?", id);
            jdbc.update("DELETE FROM \"Booking\" WHERE id = ?", id);
        }
        unitIds.forEach(roomUnitRepository::deleteById);
        roomIds.forEach(roomRepository::deleteById);
        bookingIds.clear();
        unitIds.clear();
        roomIds.clear();
    }

    @Test
    void anArrivalCheckingIn_andAnOverdueGuestBeingMovedIn_neverBothEndUpInTheRoom() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 12; round++) {
                RoomEntity room = room();
                RoomUnitEntity from = unit(room);
                RoomUnitEntity target = unit(room);
                Booking overdue = booking(room, from, "Overdue " + round, TODAY.minusDays(3), TODAY.minusDays(1));
                occupancyService.checkIn(overdue.getId());
                Booking arrival = booking(room, target, "Arrival " + round, TODAY, TODAY.plusDays(2));

                CyclicBarrier together = new CyclicBarrier(2);
                Future<Boolean> checkIn = pool.submit(attempt(together, () -> occupancyService.checkIn(arrival.getId())));
                Future<Boolean> move = pool.submit(attempt(together,
                        () -> bookingService.assignRoomUnit(overdue.getId(), new RoomUnitAssignmentInput().roomUnitId(target.getId()))));
                boolean checkedIn = checkIn.get(30, TimeUnit.SECONDS);
                boolean moved = move.get(30, TimeUnit.SECONDS);

                Integer inTarget = jdbc.queryForObject(
                        "SELECT count(*) FROM \"Booking\" WHERE \"roomUnitId\" = ? AND \"occupancyStatus\" = 'CHECKED_IN'", Integer.class, target.getId());
                assertThat(inTarget).as("round %s: check-in %s, move %s", round, checkedIn, moved).isLessThanOrEqualTo(1);
                assertThat(checkedIn && moved).as("round %s", round).isFalse();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /** True when it went through; false when it was refused with the 409 the desk is given. Anything else fails the test. */
    private static Callable<Boolean> attempt(CyclicBarrier together, Runnable write) {
        return () -> {
            together.await(10, TimeUnit.SECONDS);
            try {
                write.run();
                return true;
            } catch (ConflictException refused) {
                return false;
            }
        };
    }

    private RoomEntity room() {
        RoomEntity r = new RoomEntity();
        r.setName("Race Move Room " + UUID.randomUUID());
        r.setDescription("Used only by CheckInVersusMoveRaceTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(r);
        roomIds.add(saved.getId());
        return saved;
    }

    private RoomUnitEntity unit(RoomEntity room) {
        RoomUnitEntity u = new RoomUnitEntity();
        u.setRoomId(room.getId());
        u.setLabel("RM-" + UUID.randomUUID().toString().substring(0, 8));
        u.setActive(true);
        u.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        RoomUnitEntity saved = roomUnitRepository.saveAndFlush(u);
        unitIds.add(saved.getId());
        return saved;
    }

    private Booking booking(RoomEntity room, RoomUnitEntity unit, String guest, LocalDate checkIn, LocalDate checkOut) {
        Booking b = bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), guest, checkIn.toString(), checkOut.toString(), BookingChannel.WALK_IN, 1));
        bookingIds.add(b.getId());
        return bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
    }
}
