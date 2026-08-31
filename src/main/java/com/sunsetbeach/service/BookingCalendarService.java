package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.RoomUnitMapper;
import com.sunsetbeach.model.BookingCalendarResponse;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.CalendarBooking;
import com.sunsetbeach.model.RoomTypeCalendar;
import com.sunsetbeach.model.RoomTypeDailyAvailability;
import com.sunsetbeach.model.RoomUnitBlock;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read model behind {@code GET /bookings/calendar} - everything the booking calendar grid needs
 * for an arbitrary {@code [from, to)} range, across every room type at once. Deliberately
 * separate from {@link AvailabilityService}: that service answers a per-room-type, per-month
 * occupancy/pricing question with booleans and counts; this one is a booking-identity read model
 * (guest name, status, exact booking id per stay) for rendering and driving a drag/resize grid.
 * The per-day "remaining units" count both services expose is still the same single formula -
 * see {@link InventoryMath}.
 */
@Service
public class BookingCalendarService {

    /**
     * Up to a year on one screen (the calendar's own zoom-out requirement), same cap as
     * {@link DateRangeUtil#MAX_RANGE_DAYS} - still bounded, not unbounded: an arbitrarily large
     * request is rejected with a clear error rather than made to time out. Was 92 days before
     * the grid grew day/week/month zoom levels; reusing the shared constant now rather than
     * keeping a second, smaller one that would need to track it.
     */
    public static final int MAX_CALENDAR_RANGE_DAYS = DateRangeUtil.MAX_RANGE_DAYS;

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomUnitBlockRepository roomUnitBlockRepository;
    private final BookingSegmentRepository segmentRepository;
    private final RoomUnitMapper roomUnitMapper;

    public BookingCalendarService(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomUnitBlockRepository roomUnitBlockRepository,
            BookingSegmentRepository segmentRepository,
            RoomUnitMapper roomUnitMapper) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomUnitBlockRepository = roomUnitBlockRepository;
        this.segmentRepository = segmentRepository;
        this.roomUnitMapper = roomUnitMapper;
    }

    @Transactional(readOnly = true)
    public BookingCalendarResponse getCalendar(LocalDate from, LocalDate to) {
        if (!from.isBefore(to)) {
            throw new BadRequestException("from must be before to");
        }
        long rangeDays = ChronoUnit.DAYS.between(from, to);
        if (rangeDays > MAX_CALENDAR_RANGE_DAYS) {
            throw new BadRequestException("Range too large (max " + MAX_CALENDAR_RANGE_DAYS + " days)");
        }

        List<RoomEntity> rooms = roomRepository.findAll();
        List<RoomUnitEntity> allUnits = roomUnitRepository.findAll();
        Map<String, List<RoomUnitEntity>> unitsByRoomId = allUnits.stream().collect(Collectors.groupingBy(RoomUnitEntity::getRoomId));

        List<String> allUnitIds = allUnits.stream().map(RoomUnitEntity::getId).toList();
        List<RoomUnitBlockEntity> blocks = allUnitIds.isEmpty()
                ? List.of()
                : roomUnitBlockRepository.findByFromDateLessThanEqualAndToDateGreaterThanEqual(to.minusDays(1), from);
        Map<String, List<RoomUnitBlockEntity>> blocksByUnitId = blocks.stream().collect(Collectors.groupingBy(RoomUnitBlockEntity::getRoomUnitId));

        List<BookingSegmentEntity> segments = segmentRepository.findByBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(BookingStatus.CANCELLED, to, from);
        Map<String, List<BookingSegmentEntity>> segmentsByRoomId = segments.stream().collect(Collectors.groupingBy(BookingSegmentEntity::getRoomId));

        // segmentCount on each CalendarBooking is the *whole* booking's segment count, not just
        // how many fall inside [from, to) - the grid needs to know "has this booking ever been
        // relocated" (to decide whether drag-resize/move is offered) even when only one of its
        // segments is visible in the current view.
        List<String> bookingIds = segments.stream().map(BookingSegmentEntity::getBookingId).distinct().toList();
        Map<String, Long> segmentCountByBookingId = bookingIds.isEmpty()
                ? Map.of()
                : segmentRepository.findByBookingIdIn(bookingIds).stream()
                        .collect(Collectors.groupingBy(BookingSegmentEntity::getBookingId, Collectors.counting()));

        List<LocalDate> days = DateRangeUtil.eachDateInRange(from, to.minusDays(1));

        List<RoomTypeCalendar> roomTypes = rooms.stream()
                .sorted(Comparator.comparing(RoomEntity::getName))
                .map(room -> toRoomTypeCalendar(room, unitsByRoomId.getOrDefault(room.getId(), List.of()),
                        segmentsByRoomId.getOrDefault(room.getId(), List.of()), blocksByUnitId, days))
                .toList();

        List<CalendarBooking> calendarBookings = segments.stream()
                .sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn))
                .map(s -> toCalendarBooking(s, segmentCountByBookingId.getOrDefault(s.getBookingId(), 1L).intValue()))
                .toList();

        List<RoomUnitBlock> blockDtos = blocks.stream().map(roomUnitMapper::toDto).toList();

        return new BookingCalendarResponse(from.toString(), to.toString(), roomTypes, calendarBookings, blockDtos);
    }

    private RoomTypeCalendar toRoomTypeCalendar(
            RoomEntity room,
            List<RoomUnitEntity> units,
            List<BookingSegmentEntity> roomSegments,
            Map<String, List<RoomUnitBlockEntity>> blocksByUnitId,
            List<LocalDate> days) {
        List<RoomUnitEntity> activeUnits = units.stream().filter(RoomUnitEntity::isActive).toList();

        List<RoomTypeDailyAvailability> dailyAvailable = days.stream()
                .map(date -> {
                    long blockedUnitsToday = activeUnits.stream()
                            .filter(u -> blocksByUnitId.getOrDefault(u.getId(), List.of()).stream()
                                    .anyMatch(b -> !date.isBefore(b.getFromDate()) && !date.isAfter(b.getToDate())))
                            .count();
                    long bookedToday =
                            roomSegments.stream().filter(s -> !date.isBefore(s.getCheckIn()) && date.isBefore(s.getCheckOut())).count();
                    int available = InventoryMath.availableCount(activeUnits.size(), (int) blockedUnitsToday, (int) bookedToday);
                    return new RoomTypeDailyAvailability(date.toString(), available);
                })
                .toList();

        List<com.sunsetbeach.model.RoomUnit> roomUnitDtos = units.stream()
                .sorted(Comparator.comparing(RoomUnitEntity::getLabel))
                .map(roomUnitMapper::toDto)
                .toList();

        return new RoomTypeCalendar(room.getId(), room.getName(), roomUnitDtos, dailyAvailable);
    }

    /** One bar per *segment* - see {@code CalendarBooking}'s schema description for why. */
    private static CalendarBooking toCalendarBooking(BookingSegmentEntity s, int segmentCount) {
        return new CalendarBooking(
                s.getId(),
                s.getBookingId(),
                s.getRoomId(),
                s.getRoomUnitId(),
                s.getBooking().getGuestName(),
                s.getCheckIn().toString(),
                s.getCheckOut().toString(),
                s.getBooking().getStatus(),
                PriceFormat.asDecimalString(s.getTotalPrice()),
                segmentCount);
    }
}
