package com.sunsetbeach.controller;

import com.sunsetbeach.api.PublicApi;
import com.sunsetbeach.model.PricingResponse;
import com.sunsetbeach.model.PublicAvailabilityResponse;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.service.AvailabilityService;
import com.sunsetbeach.service.PricingService;
import com.sunsetbeach.service.RoomImageService;
import com.sunsetbeach.service.RoomService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController implements PublicApi {

    private final RoomService roomService;
    private final PricingService pricingService;
    private final AvailabilityService availabilityService;
    private final RoomImageService roomImageService;

    public PublicController(
            RoomService roomService,
            PricingService pricingService,
            AvailabilityService availabilityService,
            RoomImageService roomImageService) {
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.availabilityService = availabilityService;
        this.roomImageService = roomImageService;
    }

    @Override
    public ResponseEntity<List<Room>> listPublicRooms() {
        return ResponseEntity.ok(roomService.list());
    }

    @Override
    public ResponseEntity<Room> getPublicRoom(String id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    @Override
    public ResponseEntity<PricingResponse> getPublicRoomPricing(String id, String month) {
        return ResponseEntity.ok(pricingService.getPricing(id, month));
    }

    @Override
    public ResponseEntity<PublicAvailabilityResponse> getPublicRoomAvailability(String id, String month) {
        return ResponseEntity.ok(availabilityService.getPublicAvailability(id, month));
    }

    @Override
    public ResponseEntity<Resource> getRoomImage(String roomId, String filename) {
        Resource resource = roomImageService.resolve(roomId, filename);
        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(contentType)
                // Filenames are timestamp+random, never reused - safe to cache aggressively.
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(resource);
    }
}
