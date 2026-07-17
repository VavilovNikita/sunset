package com.sunsetbeach.service;

import com.sunsetbeach.entity.RatePlanEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.model.PriceRangeInput;
import com.sunsetbeach.model.PricingDay;
import com.sunsetbeach.model.PricingResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;

@Service
public class PricingService {

    private final RoomRepository roomRepository;
    private final RatePlanRepository ratePlanRepository;

    public PricingService(RoomRepository roomRepository, RatePlanRepository ratePlanRepository) {
        this.roomRepository = roomRepository;
        this.ratePlanRepository = ratePlanRepository;
    }

    @Transactional(readOnly = true)
    public PricingResponse getPricing(String roomId, String monthParam) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));

        YearMonth month = DateRangeUtil.parseMonthOrCurrent(monthParam);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        Map<LocalDate, BigDecimal> overrides = new HashMap<>();
        for (RatePlanEntity plan : ratePlanRepository.findByRoomIdAndDateBetween(roomId, monthStart, monthEnd)) {
            overrides.put(plan.getDate(), plan.getPrice());
        }

        List<PricingDay> days = DateRangeUtil.eachDateInRange(monthStart, monthEnd).stream()
                .map(date -> {
                    BigDecimal override = overrides.get(date);
                    BigDecimal price = override != null ? override : room.getBasePrice();
                    return new PricingDay(date.toString(), PriceFormat.asRealNumber(price), override != null);
                })
                .toList();

        return new PricingResponse(PriceFormat.asRealNumber(room.getBasePrice()), days);
    }

    @Transactional
    public int setPricing(String roomId, PriceRangeInput input) {
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
            RatePlanEntity plan = ratePlanRepository.findByRoomIdAndDate(room.getId(), date)
                    .orElseGet(() -> {
                        RatePlanEntity created = new RatePlanEntity();
                        created.setRoomId(room.getId());
                        created.setDate(date);
                        return created;
                    });
            plan.setPrice(input.getPrice());
            ratePlanRepository.save(plan);
        }

        return dates.size();
    }
}
