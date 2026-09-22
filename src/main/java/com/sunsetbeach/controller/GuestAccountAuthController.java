package com.sunsetbeach.controller;

import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.mapper.GuestAccountMapper;
import com.sunsetbeach.model.GuestAccountAuthResponse;
import com.sunsetbeach.model.GuestAccountLoginInput;
import com.sunsetbeach.model.GuestAccountMessage;
import com.sunsetbeach.model.GuestAccountRegisterInput;
import com.sunsetbeach.model.GuestAccountResendVerificationInput;
import com.sunsetbeach.model.GuestAccountVerifyInput;
import com.sunsetbeach.security.ClientIpResolver;
import com.sunsetbeach.security.GuestAccountLoginRateLimiter;
import com.sunsetbeach.security.GuestAccountAuthRateLimiter;
import com.sunsetbeach.security.GuestJwtService;
import com.sunsetbeach.security.GuestPrincipal;
import com.sunsetbeach.service.GuestAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public {@code /guest-auth/**} - hand-rolled rather than implementing a generated
 * {@code GuestAccountAuthApi}, for the same structural reason {@code AuthController} hand-rolls
 * {@code /auth/*}: every operation here is IP/email rate-limited, which needs an
 * {@link HttpServletRequest} the generated interface has no way to carry - see
 * scripts/generate-api.sh's own comment.
 */
@RestController
public class GuestAccountAuthController {

    private final GuestAccountService guestAccountService;
    private final GuestAccountMapper guestAccountMapper;
    private final GuestJwtService guestJwtService;
    private final GuestAccountAuthRateLimiter authRateLimiter;
    private final GuestAccountLoginRateLimiter loginRateLimiter;

    public GuestAccountAuthController(
            GuestAccountService guestAccountService,
            GuestAccountMapper guestAccountMapper,
            GuestJwtService guestJwtService,
            GuestAccountAuthRateLimiter authRateLimiter,
            GuestAccountLoginRateLimiter loginRateLimiter) {
        this.guestAccountService = guestAccountService;
        this.guestAccountMapper = guestAccountMapper;
        this.guestJwtService = guestJwtService;
        this.authRateLimiter = authRateLimiter;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/guest-auth/register")
    public GuestAccountMessage register(@Valid @RequestBody GuestAccountRegisterInput input, HttpServletRequest httpRequest) {
        String ip = ClientIpResolver.resolve(httpRequest);
        authRateLimiter.checkAllowedAndRecordForEmail(ip, input.getEmail());
        guestAccountService.register(input);
        return new GuestAccountMessage("If this email isn't already registered, we've sent a verification link.");
    }

    @PostMapping("/guest-auth/verify")
    public GuestAccountAuthResponse verify(@Valid @RequestBody GuestAccountVerifyInput input, HttpServletRequest httpRequest) {
        String ip = ClientIpResolver.resolve(httpRequest);
        authRateLimiter.checkAllowedAndRecordForToken(ip);
        GuestAccountEntity account = guestAccountService.verify(input.getToken());
        return issueResponse(account);
    }

    @PostMapping("/guest-auth/resend-verification")
    public GuestAccountMessage resendVerification(@Valid @RequestBody GuestAccountResendVerificationInput input, HttpServletRequest httpRequest) {
        String ip = ClientIpResolver.resolve(httpRequest);
        authRateLimiter.checkAllowedAndRecordForEmail(ip, input.getEmail());
        guestAccountService.resendVerification(input.getEmail());
        return new GuestAccountMessage("If this email has a pending verification, we've sent a new link.");
    }

    @PostMapping("/guest-auth/login")
    public GuestAccountAuthResponse login(@Valid @RequestBody GuestAccountLoginInput input, HttpServletRequest httpRequest) {
        String email = input.getEmail().trim();
        String ip = ClientIpResolver.resolve(httpRequest);
        loginRateLimiter.checkAllowed(ip, email);

        GuestAccountEntity account;
        try {
            account = guestAccountService.login(email, input.getPassword());
        } catch (UnauthorizedException e) {
            // Only a genuine wrong-email/wrong-password guess counts toward the limit - a correct
            // password against an unverified account (ForbiddenException, uncaught here) already
            // proves legitimate possession, so it isn't a guessing attempt and shouldn't burn down
            // a real guest's own attempt budget while they go check their inbox.
            loginRateLimiter.recordFailure(ip, email);
            throw e;
        }
        loginRateLimiter.recordSuccess(ip, email);
        return issueResponse(account);
    }

    private GuestAccountAuthResponse issueResponse(GuestAccountEntity account) {
        GuestPrincipal principal = new GuestPrincipal(account.getId(), account.getEmail());
        String token = guestJwtService.issue(principal, account.getTokenVersion());
        return new GuestAccountAuthResponse(token, guestAccountMapper.toDto(account));
    }
}
