package com.sunsetbeach.controller;

import com.sunsetbeach.api.GuestAccountApi;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.mapper.GuestAccountMapper;
import com.sunsetbeach.model.GuestAccount;
import com.sunsetbeach.model.GuestAccountAuthResponse;
import com.sunsetbeach.model.GuestBookingView;
import com.sunsetbeach.model.GuestPasswordChangeInput;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.security.CurrentGuest;
import com.sunsetbeach.security.GuestJwtService;
import com.sunsetbeach.security.GuestPrincipal;
import com.sunsetbeach.service.GuestAccountService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated {@code /guest/**} - implements the generated {@link GuestAccountApi}, unlike {@link GuestAccountAuthController} (see that class's own comment for why). */
@RestController
public class GuestAccountController implements GuestAccountApi {

    private final GuestAccountService guestAccountService;
    private final GuestAccountRepository guestAccountRepository;
    private final GuestAccountMapper guestAccountMapper;
    private final GuestJwtService guestJwtService;

    public GuestAccountController(
            GuestAccountService guestAccountService,
            GuestAccountRepository guestAccountRepository,
            GuestAccountMapper guestAccountMapper,
            GuestJwtService guestJwtService) {
        this.guestAccountService = guestAccountService;
        this.guestAccountRepository = guestAccountRepository;
        this.guestAccountMapper = guestAccountMapper;
        this.guestJwtService = guestJwtService;
    }

    @Override
    public ResponseEntity<GuestAccount> getCurrentGuestAccount() {
        GuestAccountEntity entity = guestAccountRepository.findById(CurrentGuest.id())
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return ResponseEntity.ok(guestAccountMapper.toDto(entity));
    }

    @Override
    public ResponseEntity<List<GuestBookingView>> listCurrentGuestBookings() {
        return ResponseEntity.ok(guestAccountService.listBookings(CurrentGuest.email()));
    }

    @Override
    public ResponseEntity<GuestAccountAuthResponse> changeGuestAccountPassword(GuestPasswordChangeInput input) {
        GuestAccountEntity updated = guestAccountService.changePassword(CurrentGuest.id(), input.getCurrentPassword(), input.getNewPassword());
        GuestPrincipal refreshed = new GuestPrincipal(updated.getId(), updated.getEmail());
        String token = guestJwtService.issue(refreshed, updated.getTokenVersion());
        return ResponseEntity.ok(new GuestAccountAuthResponse(token, guestAccountMapper.toDto(updated)));
    }
}
