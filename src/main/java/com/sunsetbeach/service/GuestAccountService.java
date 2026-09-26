package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.mapper.GuestAccountMapper;
import com.sunsetbeach.model.GuestAccountRegisterInput;
import com.sunsetbeach.model.GuestBookingView;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.security.PasswordTimingNormalization;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A guest's own persistent login - entirely separate from staff {@code User}/{@code Role} (see
 * {@code GuestAccount}'s own openapi.yaml description and CLAUDE.md's Authorization section), but
 * linked to the CRM-facing {@code Guest} card by {@code guestId} (see {@link GuestLinkService}), and
 * booking history follows that link - see {@link #listBookings}.
 */
@Service
public class GuestAccountService {

    private final GuestAccountRepository guestAccountRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final GuestAccountMapper guestAccountMapper;
    private final GuestLinkService guestLinkService;
    private final long verificationTtlHours;

    public GuestAccountService(
            GuestAccountRepository guestAccountRepository,
            BookingRepository bookingRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            GuestAccountMapper guestAccountMapper,
            GuestLinkService guestLinkService,
            @Value("${app.guest-account.verification-ttl-hours}") long verificationTtlHours) {
        this.guestAccountRepository = guestAccountRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.guestAccountMapper = guestAccountMapper;
        this.guestLinkService = guestLinkService;
        this.verificationTtlHours = verificationTtlHours;
    }

    /**
     * Always succeeds from the caller's point of view (see {@code POST /guest-auth/register}'s
     * own description) - the three internal cases (free email, already-verified email,
     * already-pending email) are handled here, never surfaced as a different response.
     */
    @Transactional
    public void register(GuestAccountRegisterInput input) {
        String email = normalize(input.getEmail());
        GuestAccountEntity entity = guestAccountRepository.findByEmail(email).orElse(null);

        if (entity != null && entity.getEmailVerifiedAt() != null) {
            // Already registered and usable - nothing to do, and no email to send. The controller
            // returns the same generic message regardless.
            return;
        }

        String name = input.getName() != null ? input.getName().orElse(null) : null;

        if (entity == null) {
            entity = new GuestAccountEntity();
            entity.setEmail(email);
            entity.setPasswordHash(passwordEncoder.encode(input.getPassword()));
            entity.setName(name);
        }
        // else: an abandoned, still-unverified signup - regenerate the token and resend, don't
        // create a second account (see this operation's own description). The original password
        // stays whatever it was first set to; a caller who forgot it has PATCH /guest/password
        // once verified, or (not yet built) a forgot-password flow - same as any other account.

        entity.setEmailVerificationToken(generateToken());
        entity.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(verificationTtlHours));
        // Links to an existing card only - see GuestLinkService#linkAccount's allowCreate.
        guestLinkService.linkAccount(entity, false);
        guestAccountRepository.save(entity);

        emailService.sendGuestVerificationEmail(email, name, entity.getEmailVerificationToken());
    }

    /** Same generic response regardless of match (see {@code POST /guest-auth/resend-verification}'s own description) - only an unverified account actually gets a new token/email. */
    @Transactional
    public void resendVerification(String rawEmail) {
        String email = normalize(rawEmail);
        guestAccountRepository.findByEmail(email)
                .filter(account -> account.getEmailVerifiedAt() == null)
                .ifPresent(account -> {
                    account.setEmailVerificationToken(generateToken());
                    account.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(verificationTtlHours));
                    guestAccountRepository.save(account);
                    emailService.sendGuestVerificationEmail(account.getEmail(), account.getName(), account.getEmailVerificationToken());
                });
    }

    @Transactional
    public GuestAccountEntity verify(String token) {
        GuestAccountEntity account = guestAccountRepository.findByEmailVerificationToken(token)
                .filter(a -> a.getEmailVerificationExpiresAt() != null && a.getEmailVerificationExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        account.setEmailVerifiedAt(LocalDateTime.now());
        account.setEmailVerificationToken(null);
        account.setEmailVerificationExpiresAt(null);
        guestLinkService.linkAccount(account, true);
        return guestAccountRepository.save(account);
    }

    /**
     * Wrong email or wrong password both throw the same {@link UnauthorizedException} (see this
     * operation's own openapi.yaml description) - a correct password against an unverified
     * account throws {@link ForbiddenException} instead, since a correct password already proves
     * legitimate possession of the account and there's nothing left to hide.
     */
    @Transactional(readOnly = true)
    public GuestAccountEntity login(String rawEmail, String password) {
        String email = normalize(rawEmail);
        GuestAccountEntity account = guestAccountRepository.findByEmail(email).orElse(null);
        // passwordOk is computed unconditionally, even when account is null, against a fixed dummy
        // hash in that case - see PasswordTimingNormalization's own javadoc for why a short-circuit
        // here would be a timing oracle for email enumeration.
        boolean passwordOk = passwordEncoder.matches(
                password,
                account != null ? account.getPasswordHash() : PasswordTimingNormalization.DUMMY_HASH);
        if (account == null || !passwordOk) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (account.getEmailVerifiedAt() == null) {
            throw new ForbiddenException("Please verify your email before logging in.");
        }
        return account;
    }

    @Transactional
    public GuestAccountEntity changePassword(String accountId, String currentPassword, String newPassword) {
        GuestAccountEntity account = guestAccountRepository.findById(accountId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setTokenVersion(account.getTokenVersion() + 1);
        return guestAccountRepository.save(account);
    }

    /**
     * Every booking linked to this account's Guest card - the same set staff see on that card's
     * stay history ({@code GET /guests/{id}}), including bookings staff linked by hand. An account
     * with no card yet (its email matched several cards, or the card was deleted) falls back to
     * matching {@code Booking.guestEmail} against the account's own email, case-insensitively -
     * the pre-V104 behaviour, kept only as a safety net.
     */
    @Transactional(readOnly = true)
    public List<GuestBookingView> listBookings(String accountId) {
        GuestAccountEntity account = guestAccountRepository.findById(accountId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        List<BookingEntity> bookings = account.getGuestId() != null
                ? bookingRepository.findByGuestIdOrderByCreatedAtDesc(account.getGuestId())
                : bookingRepository.findByGuestEmailIgnoreCaseOrderByCreatedAtDesc(account.getEmail());
        return bookings.stream().map(guestAccountMapper::toGuestBookingView).toList();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String generateToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
