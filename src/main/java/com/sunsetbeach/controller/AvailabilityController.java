package com.sunsetbeach.controller;

import com.sunsetbeach.api.AvailabilityApi;
import com.sunsetbeach.model.AvailabilityRangeInput;
import com.sunsetbeach.model.AvailabilityResponse;
import com.sunsetbeach.model.OkUpdated;
import com.sunsetbeach.service.AvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvailabilityController implements AvailabilityApi {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @Override
    public ResponseEntity<AvailabilityResponse> getRoomAvailability(String roomId, String month) {
        return ResponseEntity.ok(availabilityService.getAvailability(roomId, month));
    }

    @Override
    public ResponseEntity<OkUpdated> setRoomAvailability(String roomId, AvailabilityRangeInput availabilityRangeInput) {
        int updated = availabilityService.setAvailability(roomId, availabilityRangeInput);
        return ResponseEntity.ok(new OkUpdated(true, updated));
    }
}
