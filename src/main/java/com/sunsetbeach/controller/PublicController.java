package com.sunsetbeach.controller;

import com.sunsetbeach.api.PublicApi;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.PricingResponse;
import com.sunsetbeach.model.PublicAvailabilityResponse;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.security.ClientIpResolver;
import com.sunsetbeach.security.PublicQuoteRateLimiter;
import com.sunsetbeach.service.AvailabilityService;
import com.sunsetbeach.service.BookingService;
import com.sunsetbeach.service.PricingService;
import com.sunsetbeach.service.RoomImageService;
import com.sunsetbeach.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final BookingService bookingService;
    private final PublicQuoteRateLimiter quoteRateLimiter;
    private final HttpServletRequest request;

    public PublicController(
            RoomService roomService,
            PricingService pricingService,
            AvailabilityService availabilityService,
            RoomImageService roomImageService,
            BookingService bookingService,
            PublicQuoteRateLimiter quoteRateLimiter,
            HttpServletRequest request) {
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.availabilityService = availabilityService;
        this.roomImageService = roomImageService;
        this.bookingService = bookingService;
        this.quoteRateLimiter = quoteRateLimiter;
        this.request = request;
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
    public ResponseEntity<BookingScheduleQuote> getPublicRoomQuote(String id, String checkIn, String checkOut) {
        quoteRateLimiter.checkAllowedAndRecord(ClientIpResolver.resolve(request));
        return ResponseEntity.ok(bookingService.quotePublicBooking(id, checkIn, checkOut));
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
