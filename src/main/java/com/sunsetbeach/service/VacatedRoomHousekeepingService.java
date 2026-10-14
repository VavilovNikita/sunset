package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marks dirty the rooms that a planned room move releases on the day it takes effect. A move made
 * for today (or earlier) marks the room at once (see {@code BookingService#relocate}); a move planned
 * for a later day has nobody leaving the room until then, and nothing else runs at that moment - this
 * is the daily job that does, at the start of the hotel's day.
 *
 * <p>For every booking that is {@code CHECKED_IN} and has a segment starting today, the room of the
 * segment that ended today is marked dirty (unless it already is, or another booking holds it tonight
 * - it was cleaned and re-let). One run per hotel day: {@code HousekeepingSweep} (V145) keeps the day,
 * so a repeat run - the hourly catch-up after a restart, a second instance - changes nothing, and a
 * room housekeeping cleaned meanwhile is not dirtied again. Same {@code @Scheduled} shape as the other
 * sweeps (CLAUDE.md, "Failure handling").
 */
@Service
public class VacatedRoomHousekeepingService {

    private static final Logger log = LoggerFactory.getLogger(VacatedRoomHousekeepingService.class);

    private final BookingSegmentRepository segmentRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public VacatedRoomHousekeepingService(
            BookingSegmentRepository segmentRepository,
            RoomUnitRepository roomUnitRepository,
            BookingRepository bookingRepository,
            AuditLogService auditLogService,
            JdbcTemplate jdbc,
            Clock clock) {
        this.segmentRepository = segmentRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.bookingRepository = bookingRepository;
        this.auditLogService = auditLogService;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    // 00:05 hotel time, right after the day turns; the hourly line is the catch-up for a server that was
    // down at 00:05 (a no-op once today has run). The zone is a literal, as in LifecycleEmailService.
    @Scheduled(cron = "${app.housekeeping.vacated-rooms-cron:0 5 * * * *}", zone = "Asia/Bangkok")
    public void runScheduled() {
        try {
            int marked = runForToday();
            if (marked > 0) {
                log.info("Marked {} room(s) dirty after room moves that took effect today", marked);
            }
        } catch (Exception e) {
            log.error("Vacated-room housekeeping failed", e);
        }
    }

    /** Runs once per hotel day; returns how many rooms were marked now (0 on a repeat run). */
    @Transactional
    public int runForToday() {
        LocalDate today = LocalDate.now(clock);
        int claimed = jdbc.update("insert into \"HousekeepingSweep\" (\"day\") values (?) on conflict do nothing", today);
        if (claimed == 0) {
            return 0;
        }

        List<BookingSegmentEntity> starting = segmentRepository.findByCheckInEquals(today);
        if (starting.isEmpty()) {
            return 0;
        }
        Map<String, List<BookingSegmentEntity>> byBooking = segmentRepository
                .findByBookingIdIn(starting.stream().map(BookingSegmentEntity::getBookingId).distinct().toList()).stream()
                .collect(Collectors.groupingBy(BookingSegmentEntity::getBookingId));

        Map<String, BookingEntity> bookings = bookingRepository.findAllById(byBooking.keySet()).stream()
                .collect(Collectors.toMap(BookingEntity::getId, b -> b));
        List<String> marked = new ArrayList<>();
        for (BookingSegmentEntity segment : starting) {
            BookingEntity booking = bookings.get(segment.getBookingId());
            if (booking == null || booking.getStatus() == BookingStatus.CANCELLED || booking.getOccupancyStatus() != OccupancyStatus.CHECKED_IN) {
                continue;
            }
            byBooking.getOrDefault(segment.getBookingId(), List.of()).stream()
                    .filter(prev -> prev.getCheckOut().equals(today) && prev.getRoomUnitId() != null)
                    .filter(prev -> !prev.getRoomUnitId().equals(segment.getRoomUnitId()))
                    .forEach(prev -> {
                        if (markDirty(prev.getRoomUnitId(), segment.getBookingId(), today)) {
                            marked.add(prev.getRoomUnitId());
                        }
                    });
        }
        if (!marked.isEmpty()) {
            auditLogService.recordSystemAction(
                    com.sunsetbeach.model.AuditAction.ROOM_UNIT_UPDATED,
                    com.sunsetbeach.model.AuditEntityType.ROOM_UNIT,
                    marked.get(0),
                    marked.size() + " room(s) marked dirty: a guest moved out of them today");
        }
        return marked.size();
    }

    private boolean markDirty(String unitId, String bookingId, LocalDate today) {
        RoomUnitEntity unit = roomUnitRepository.findById(unitId).orElse(null);
        if (unit == null || unit.getHousekeepingStatus() == HousekeepingStatus.DIRTY) {
            return false;
        }
        boolean heldByAnother = !segmentRepository
                .findByRoomUnitIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThanAndBookingIdNot(
                        unitId, BookingStatus.CANCELLED, today.plusDays(1), today, bookingId)
                .isEmpty();
        if (heldByAnother) {
            return false;
        }
        unit.setHousekeepingStatus(HousekeepingStatus.DIRTY);
        roomUnitRepository.saveAndFlush(unit);
        return true;
    }
}
