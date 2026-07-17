package com.sunsetbeach.controller;

import com.sunsetbeach.api.PricingApi;
import com.sunsetbeach.model.OkUpdated;
import com.sunsetbeach.model.PriceRangeInput;
import com.sunsetbeach.model.PricingResponse;
import com.sunsetbeach.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PricingController implements PricingApi {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Override
    public ResponseEntity<PricingResponse> getRoomPricing(String roomId, String month) {
        return ResponseEntity.ok(pricingService.getPricing(roomId, month));
    }

    @Override
    public ResponseEntity<OkUpdated> setRoomPricing(String roomId, PriceRangeInput priceRangeInput) {
        int updated = pricingService.setPricing(roomId, priceRangeInput);
        return ResponseEntity.ok(new OkUpdated(true, updated));
    }
}
