package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
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
import com.sunsetbeach.repository.BookingRepository;
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
     * A month-scale UI grid has no reason to ask for more than this at once.
     * {@link DateRangeUtil#MAX_RANGE_DAYS} (366, used by {@link PricingService}) is a much
     * larger cap for a coarser-grained, single-room-type endpoint - not reused here on purpose.
     */
    public static final int MAX_CALENDAR_RANGE_DAYS = 92;

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomUnitBlockRepository roomUnitBlockRepository;
    private final BookingRepository bookingRepository;
    private final RoomUnitMapper roomUnitMapper;

    public BookingCalendarService(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomUnitBlockRepository roomUnitBlockRepository,
            BookingRepository bookingRepository,
            RoomUnitMapper roomUnitMapper) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomUnitBlockRepository = roomUnitBlockRepository;
        this.bookingRepository = bookingRepository;
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

        List<BookingEntity> bookings = bookingRepository.findByStatusNotAndCheckInLessThanAndCheckOutGreaterThan(BookingStatus.CANCELLED, to, from);
        Map<String, List<BookingEntity>> bookingsByRoomId = bookings.stream().collect(Collectors.groupingBy(BookingEntity::getRoomId));

        List<LocalDate> days = DateRangeUtil.eachDateInRange(from, to.minusDays(1));

        List<RoomTypeCalendar> roomTypes = rooms.stream()
                .sorted(Comparator.comparing(RoomEntity::getName))
                .map(room -> toRoomTypeCalendar(room, unitsByRoomId.getOrDefault(room.getId(), List.of()),
                        bookingsByRoomId.getOrDefault(room.getId(), List.of()), blocksByUnitId, days))
                .toList();

        List<CalendarBooking> calendarBookings = bookings.stream()
                .sorted(Comparator.comparing(BookingEntity::getCheckIn))
                .map(BookingCalendarService::toCalendarBooking)
                .toList();

        List<RoomUnitBlock> blockDtos = blocks.stream().map(roomUnitMapper::toDto).toList();

        return new BookingCalendarResponse(from.toString(), to.toString(), roomTypes, calendarBookings, blockDtos);
    }

    private RoomTypeCalendar toRoomTypeCalendar(
            RoomEntity room,
            List<RoomUnitEntity> units,
            List<BookingEntity> roomBookings,
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
                            roomBookings.stream().filter(b -> !date.isBefore(b.getCheckIn()) && date.isBefore(b.getCheckOut())).count();
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

    private static CalendarBooking toCalendarBooking(BookingEntity b) {
        return new CalendarBooking(
                b.getId(),
                b.getRoomId(),
                b.getRoomUnitId(),
                b.getGuestName(),
                b.getCheckIn().toString(),
                b.getCheckOut().toString(),
                b.getStatus(),
                PriceFormat.asDecimalString(b.getTotalPrice()));
    }
}
