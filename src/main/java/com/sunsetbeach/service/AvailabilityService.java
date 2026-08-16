package com.sunsetbeach.service;

import com.sunsetbeach.entity.AvailabilityEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AvailabilityDay;
import com.sunsetbeach.model.AvailabilityRangeInput;
import com.sunsetbeach.model.AvailabilityResponse;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.PublicAvailabilityDay;
import com.sunsetbeach.model.PublicAvailabilityResponse;
import com.sunsetbeach.repository.AvailabilityRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private final RoomRepository roomRepository;
    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;

    public AvailabilityService(
            RoomRepository roomRepository, AvailabilityRepository availabilityRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(String roomId, String monthParam) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        List<DayInventory> inventory = computeInventory(room, monthParam);

        List<AvailabilityDay> days = inventory.stream()
                .map(d -> new AvailabilityDay(d.date().toString(), d.quantity(), d.blockedCount(), d.bookedCount(), d.availableCount()))
                .toList();

        return new AvailabilityResponse(days);
    }

    /**
     * Public counterpart of {@link #getAvailability} - same day-by-day computation, but
     * collapsed to a yes/no signal: never reveals whether a blocked day is a real booking or a
     * manual staff block, and never reveals the exact remaining count (that would let anyone
     * read the hotel's occupancy/load straight off the public site).
     */
    @Transactional(readOnly = true)
    public PublicAvailabilityResponse getPublicAvailability(String roomId, String monthParam) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        List<DayInventory> inventory = computeInventory(room, monthParam);

        List<PublicAvailabilityDay> days =
                inventory.stream().map(d -> new PublicAvailabilityDay(d.date().toString(), d.availableCount() <= 0)).toList();

        return new PublicAvailabilityResponse(days);
    }

    /** availableCount is deliberately not clamped at 0 - a negative remainder is a real signal (e.g. after a quantity cut), not noise to hide. */
    private record DayInventory(LocalDate date, int quantity, int blockedCount, int bookedCount) {
        int availableCount() {
            return quantity - blockedCount - bookedCount;
        }
    }

    private List<DayInventory> computeInventory(RoomEntity room, String monthParam) {
        YearMonth month = DateRangeUtil.parseMonthOrCurrent(monthParam);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        Map<LocalDate, Integer> blockedByDate = new HashMap<>();
        for (AvailabilityEntity block : availabilityRepository.findByRoomIdAndDateBetween(room.getId(), monthStart, monthEnd)) {
            blockedByDate.put(block.getDate(), block.getBlockedCount());
        }

        List<BookingEntity> bookings = bookingRepository.findByRoomIdAndStatusNotAndCheckInLessThanEqualAndCheckOutGreaterThan(
                room.getId(), BookingStatus.CANCELLED, monthEnd, monthStart);

        return DateRangeUtil.eachDateInRange(monthStart, monthEnd).stream()
                .map(date -> {
                    int booked = (int) bookings.stream()
                            .filter(b -> !date.isBefore(b.getCheckIn()) && date.isBefore(b.getCheckOut()))
                            .count();
                    return new DayInventory(date, room.getQuantity(), blockedByDate.getOrDefault(date, 0), booked);
                })
                .toList();
    }

    @Transactional
    public int setAvailability(String roomId, AvailabilityRangeInput input) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));

        LocalDate from = LocalDate.parse(input.getFrom());
        LocalDate to = LocalDate.parse(input.getTo());
        if (from.isAfter(to)) {
            throw ValidationException.field("to", "from must be on or before to");
        }

        int blockedCount = input.getBlockedCount();
        if (blockedCount > room.getQuantity()) {
            throw new BadRequestException("blockedCount cannot exceed the room's quantity (" + room.getQuantity() + ")");
        }

        List<LocalDate> dates = DateRangeUtil.eachDateInRange(from, to);
        if (dates.size() > DateRangeUtil.MAX_RANGE_DAYS) {
            throw new BadRequestException("Range too large (max " + DateRangeUtil.MAX_RANGE_DAYS + " days)");
        }

        for (LocalDate date : dates) {
            var existing = availabilityRepository.findByRoomIdAndDate(room.getId(), date);
            if (blockedCount == 0) {
                // Zero is equivalent to no row existing - drop it instead of carrying a dead row.
                existing.ifPresent(availabilityRepository::delete);
                continue;
            }
            AvailabilityEntity block = existing.orElseGet(() -> {
                AvailabilityEntity created = new AvailabilityEntity();
                created.setRoomId(room.getId());
                created.setDate(date);
                return created;
            });
            block.setBlockedCount(blockedCount);
            availabilityRepository.save(block);
        }

        return dates.size();
    }
}
