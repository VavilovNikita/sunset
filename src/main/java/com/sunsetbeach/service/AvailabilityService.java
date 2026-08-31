package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AvailabilityDay;
import com.sunsetbeach.model.AvailabilityResponse;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.PublicAvailabilityDay;
import com.sunsetbeach.model.PublicAvailabilityResponse;
import com.sunsetbeach.model.RoomUnitAvailability;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomUnitBlockRepository roomUnitBlockRepository;
    private final BookingSegmentRepository segmentRepository;

    public AvailabilityService(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomUnitBlockRepository roomUnitBlockRepository,
            BookingSegmentRepository segmentRepository) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomUnitBlockRepository = roomUnitBlockRepository;
        this.segmentRepository = segmentRepository;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(String roomId, String monthParam) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        List<DayInventory> inventory = computeInventory(room, monthParam);

        List<AvailabilityDay> days = inventory.stream()
                .map(d -> new AvailabilityDay(d.date().toString(), d.unitCount(), d.blockedCount(), d.bookedCount(), d.availableCount(), d.units()))
                .toList();

        return new AvailabilityResponse(days);
    }

    /**
     * Public counterpart of {@link #getAvailability} - same day-by-day computation, but
     * collapsed to a yes/no signal: never reveals whether a blocked day is a real booking or a
     * manual staff block, never reveals the exact remaining count, and never reveals anything
     * about individual physical rooms (that would let anyone read the hotel's occupancy/layout
     * straight off the public site).
     */
    @Transactional(readOnly = true)
    public PublicAvailabilityResponse getPublicAvailability(String roomId, String monthParam) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        List<DayInventory> inventory = computeInventory(room, monthParam);

        List<PublicAvailabilityDay> days =
                inventory.stream().map(d -> new PublicAvailabilityDay(d.date().toString(), d.availableCount() <= 0)).toList();

        return new PublicAvailabilityResponse(days);
    }

    /** availableCount is deliberately not clamped at 0 - a negative remainder is a real signal (e.g. after units are deactivated), not noise to hide. See {@link InventoryMath}, the one shared implementation of this formula. */
    private record DayInventory(LocalDate date, int unitCount, int blockedCount, int bookedCount, List<RoomUnitAvailability> units) {
        int availableCount() {
            return InventoryMath.availableCount(unitCount, blockedCount, bookedCount);
        }
    }

    private List<DayInventory> computeInventory(RoomEntity room, String monthParam) {
        YearMonth month = DateRangeUtil.parseMonthOrCurrent(monthParam);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        List<RoomUnitEntity> activeUnits = roomUnitRepository.findByRoomIdAndIsActiveTrue(room.getId());
        List<String> unitIds = activeUnits.stream().map(RoomUnitEntity::getId).toList();

        List<RoomUnitBlockEntity> blocks = unitIds.isEmpty()
                ? List.of()
                : roomUnitBlockRepository.findByRoomUnitIdInAndFromDateLessThanEqualAndToDateGreaterThanEqual(unitIds, monthEnd, monthStart);

        List<BookingSegmentEntity> segments = segmentRepository.findByRoomIdAndBooking_StatusNotAndCheckInLessThanEqualAndCheckOutGreaterThan(
                room.getId(), BookingStatus.CANCELLED, monthEnd, monthStart);

        return DateRangeUtil.eachDateInRange(monthStart, monthEnd).stream()
                .map(date -> {
                    List<RoomUnitBlockEntity> blockedToday =
                            blocks.stream().filter(b -> !date.isBefore(b.getFromDate()) && !date.isAfter(b.getToDate())).toList();
                    Set<String> blockedUnitIds = blockedToday.stream().map(RoomUnitBlockEntity::getRoomUnitId).collect(Collectors.toSet());
                    Map<String, String> blockReasonByUnitId = blockedToday.stream()
                            .collect(Collectors.toMap(RoomUnitBlockEntity::getRoomUnitId, RoomUnitBlockEntity::getReason, (a, b) -> a));

                    List<BookingSegmentEntity> bookedToday =
                            segments.stream().filter(s -> !date.isBefore(s.getCheckIn()) && date.isBefore(s.getCheckOut())).toList();
                    Map<String, BookingSegmentEntity> segmentByUnitId = bookedToday.stream()
                            .filter(s -> s.getRoomUnitId() != null)
                            .collect(Collectors.toMap(BookingSegmentEntity::getRoomUnitId, s -> s, (a, b) -> a));

                    List<RoomUnitAvailability> units = activeUnits.stream()
                            .map(unit -> {
                                boolean blocked = blockedUnitIds.contains(unit.getId());
                                BookingSegmentEntity segment = segmentByUnitId.get(unit.getId());
                                boolean booked = segment != null;
                                return new RoomUnitAvailability(unit.getId(), unit.getLabel(), blocked, booked, !blocked && !booked,
                                        segment != null ? segment.getBookingId() : null, blocked ? blockReasonByUnitId.get(unit.getId()) : null);
                            })
                            .toList();

                    return new DayInventory(date, activeUnits.size(), blockedUnitIds.size(), bookedToday.size(), units);
                })
                .toList();
    }
}
