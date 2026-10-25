package com.sunsetbeach.service;

import com.sunsetbeach.entity.RatePlanEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.repository.RatePlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The one function that says what a night of a room type costs: the price grid's manual price ({@code RatePlan})
 * for that date where one is set, {@code Room.basePrice} otherwise. A new booking's nights, an extension, a
 * relocation, a reprice and the overstay charge ({@link OverstayChargeService}) all ask here - a second copy of
 * this lookup is how two screens end up quoting different prices for the same night.
 */
@Component
public class RoomNightPricing {

    private final RatePlanRepository ratePlanRepository;

    public RoomNightPricing(RatePlanRepository ratePlanRepository) {
        this.ratePlanRepository = ratePlanRepository;
    }

    /** Every night in {@code nights} priced at today's rate, in the order given. Never reads what a night was already priced at. */
    public Map<LocalDate, BigDecimal> priceNights(RoomEntity room, List<LocalDate> nights) {
        if (nights.isEmpty()) {
            return Map.of();
        }
        Map<LocalDate, BigDecimal> overrides = new HashMap<>();
        for (RatePlanEntity plan : ratePlanRepository.findByRoomIdAndDateIn(room.getId(), nights)) {
            overrides.put(plan.getDate(), plan.getPrice());
        }
        Map<LocalDate, BigDecimal> prices = new LinkedHashMap<>();
        for (LocalDate night : nights) {
            prices.put(night, overrides.getOrDefault(night, room.getBasePrice()));
        }
        return prices;
    }
}
