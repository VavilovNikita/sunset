package com.sunsetbeach.controller;

import com.sunsetbeach.api.GuestOrderApi;
import com.sunsetbeach.model.GuestOrderSubmitInput;
import com.sunsetbeach.model.GuestOrderView;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.security.CurrentGuest;
import com.sunsetbeach.security.RoomServiceOrderRateLimiter;
import com.sunsetbeach.service.RoomServiceOrderingService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Room-service ordering for an authenticated, {@code CHECKED_IN} guest - see {@link RoomServiceOrderingService}'s own class javadoc. */
@RestController
public class GuestOrderController implements GuestOrderApi {

    private final RoomServiceOrderingService roomServiceOrderingService;
    private final RoomServiceOrderRateLimiter rateLimiter;

    public GuestOrderController(RoomServiceOrderingService roomServiceOrderingService, RoomServiceOrderRateLimiter rateLimiter) {
        this.roomServiceOrderingService = roomServiceOrderingService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public ResponseEntity<GuestOrderView> submitGuestOrder(GuestOrderSubmitInput guestOrderSubmitInput) {
        rateLimiter.checkAllowedAndRecord(CurrentGuest.id());
        GuestOrderView view = roomServiceOrderingService.submit(
                CurrentGuest.email(), guestOrderSubmitInput.getBookingId(), guestOrderSubmitInput.getItems());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @Override
    public ResponseEntity<GuestOrderView> addGuestOrderItems(String id, List<OrderItemInput> orderItemInput) {
        rateLimiter.checkAllowedAndRecord(CurrentGuest.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(roomServiceOrderingService.addItems(CurrentGuest.email(), id, orderItemInput));
    }

    @Override
    public ResponseEntity<GuestOrderView> getGuestOrder(String id) {
        return ResponseEntity.ok(roomServiceOrderingService.getById(CurrentGuest.email(), id));
    }
}
