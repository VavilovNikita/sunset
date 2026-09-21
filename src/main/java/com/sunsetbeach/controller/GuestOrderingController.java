package com.sunsetbeach.controller;

import com.sunsetbeach.api.PublicOrderingApi;
import com.sunsetbeach.model.GuestOrderView;
import com.sunsetbeach.model.MenuItem;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.security.GuestOrderRateLimiter;
import com.sunsetbeach.service.MenuService;
import com.sunsetbeach.service.OrderService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dine-in QR ordering: a guest's own phone, gated by {@code token} alone - see
 * {@link OrderService#requireGuestAccess}. Deliberately not folded into {@link PublicController}
 * - a separate tag/generated interface ({@code PublicOrderingApi}) keeps this its own small,
 * focused controller, same one-controller-per-tag convention as everywhere else in this API.
 */
@RestController
public class GuestOrderingController implements PublicOrderingApi {

    private final MenuService menuService;
    private final OrderService orderService;
    private final GuestOrderRateLimiter guestOrderRateLimiter;

    public GuestOrderingController(MenuService menuService, OrderService orderService, GuestOrderRateLimiter guestOrderRateLimiter) {
        this.menuService = menuService;
        this.orderService = orderService;
        this.guestOrderRateLimiter = guestOrderRateLimiter;
    }

    @Override
    public ResponseEntity<List<MenuItem>> listPublicMenu() {
        return ResponseEntity.ok(menuService.listPublic());
    }

    @Override
    public ResponseEntity<GuestOrderView> getPublicOrder(String id, String token) {
        return ResponseEntity.ok(orderService.getGuestOrderView(id, token));
    }

    @Override
    public ResponseEntity<GuestOrderView> addPublicOrderItems(String id, String token, List<OrderItemInput> orderItemInput) {
        // Keyed on the token, not the caller's IP - see GuestOrderRateLimiter's own javadoc for
        // why. Checked before requireGuestAccess so a flood of wrong-token guesses is throttled
        // too, not just successful adds.
        guestOrderRateLimiter.checkAllowedAndRecord(token);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.addGuestItems(id, token, orderItemInput));
    }
}
