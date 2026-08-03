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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        List<DayBlock> blocks = computeBlocks(room, monthParam);

        List<AvailabilityDay> days = blocks.stream()
                .map(block -> {
                    var source = block.isBooked()
                            ? AvailabilityDay.SourceEnum.BOOKING
                            : block.manual() ? AvailabilityDay.SourceEnum.MANUAL : null;
                    return new AvailabilityDay(block.date().toString(), block.isBooked() || block.manual(), source);
                })
                .toList();

        return new AvailabilityResponse(days);
    }

    /**
     * Public counterpart of {@link #getAvailability} - same day-by-day computation, but never
     * reveals whether a blocked day is a real booking or a manual staff block.
     */
    @Transactional(readOnly = true)
    public PublicAvailabilityResponse getPublicAvailability(String roomId, String monthParam) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        List<DayBlock> blocks = computeBlocks(room, monthParam);

        List<PublicAvailabilityDay> days = blocks.stream()
                .map(block -> new PublicAvailabilityDay(block.date().toString(), block.isBooked() || block.manual()))
                .toList();

        return new PublicAvailabilityResponse(days);
    }

    private record DayBlock(LocalDate date, boolean isBooked, boolean manual) {
    }

    private List<DayBlock> computeBlocks(RoomEntity room, String monthParam) {
        YearMonth month = DateRangeUtil.parseMonthOrCurrent(monthParam);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        Map<LocalDate, Boolean> manualByDate = new HashMap<>();
        for (AvailabilityEntity block : availabilityRepository.findByRoomIdAndDateBetween(room.getId(), monthStart, monthEnd)) {
            manualByDate.put(block.getDate(), block.isBlocked());
        }

        Set<LocalDate> bookedDates = new HashSet<>();
        for (BookingEntity booking : bookingRepository.findByRoomIdAndStatusNotAndCheckInLessThanEqualAndCheckOutGreaterThan(
                room.getId(), BookingStatus.CANCELLED, monthEnd, monthStart)) {
            bookedDates.addAll(DateRangeUtil.getNights(booking.getCheckIn(), booking.getCheckOut()));
        }

        return DateRangeUtil.eachDateInRange(monthStart, monthEnd).stream()
                .map(date -> new DayBlock(date, bookedDates.contains(date), manualByDate.getOrDefault(date, false)))
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

        List<LocalDate> dates = DateRangeUtil.eachDateInRange(from, to);
        if (dates.size() > DateRangeUtil.MAX_RANGE_DAYS) {
            throw new BadRequestException("Range too large (max " + DateRangeUtil.MAX_RANGE_DAYS + " days)");
        }

        for (LocalDate date : dates) {
            AvailabilityEntity block = availabilityRepository.findByRoomIdAndDate(room.getId(), date)
                    .orElseGet(() -> {
                        AvailabilityEntity created = new AvailabilityEntity();
                        created.setRoomId(room.getId());
                        created.setDate(date);
                        return created;
                    });
            block.setBlocked(input.getIsBlocked());
            availabilityRepository.save(block);
        }

        return dates.size();
    }
}
