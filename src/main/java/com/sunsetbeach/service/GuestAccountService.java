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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
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
    private final Clock clock;

    /** How long a password-reset link stays usable. Short on purpose - it is a credential for a live account. */
    static final Duration PASSWORD_RESET_TTL = Duration.ofHours(1);

    public GuestAccountService(
            GuestAccountRepository guestAccountRepository,
            BookingRepository bookingRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            GuestAccountMapper guestAccountMapper,
            GuestLinkService guestLinkService,
            @Value("${app.guest-account.verification-ttl-hours}") long verificationTtlHours,
            Clock clock) {
        this.guestAccountRepository = guestAccountRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.guestAccountMapper = guestAccountMapper;
        this.guestLinkService = guestLinkService;
        this.verificationTtlHours = verificationTtlHours;
        this.clock = clock;
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
     * {@code GET /guest-auth/unsubscribe} - opts the account out of every lifecycle email (see
     * {@link LifecycleEmailService}). Idempotent; the token is never consumed, so an old email's
     * link keeps working.
     */
    @Transactional
    public void unsubscribe(String token) {
        GuestAccountEntity account = guestAccountRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new BadRequestException("This unsubscribe link isn't valid."));
        if (!account.isMarketingEmailsOptOut()) {
            account.setMarketingEmailsOptOut(true);
            guestAccountRepository.save(account);
        }
    }

    /**
     * Wrong email or wrong password both throw the same {@link UnauthorizedException} (see this
     * operation's own openapi.yaml description) - a correct password against an unverified
     * account throws {@link ForbiddenException} instead, since a correct password already proves
     * legitimate possession of the account and there's nothing left to hide.
     */
    /**
     * {@code POST /guest-auth/forgot-password}. Silent about whether the email has an account - the
     * controller returns the same message either way. Issues a fresh single-use token (replacing any
     * earlier one), stores only its SHA-256, and emails the raw token as a link.
     */
    @Transactional
    public void requestPasswordReset(String rawEmail) {
        String email = normalize(rawEmail);
        guestAccountRepository.findByEmail(email).ifPresent(account -> {
            String token = generateToken();
            account.setPasswordResetTokenHash(sha256Hex(token));
            account.setPasswordResetExpiresAt(LocalDateTime.now(clock).plus(PASSWORD_RESET_TTL));
            guestAccountRepository.save(account);
            emailService.sendGuestPasswordResetEmail(account.getEmail(), account.getName(), token);
        });
    }

    /**
     * {@code POST /guest-auth/reset-password}. Unknown, used and expired tokens are one 400. A
     * successful reset sets the password, clears the token, bumps {@code tokenVersion} (signing out
     * every earlier guest token) and - because holding the emailed token proves control of the
     * mailbox - verifies a not-yet-verified account, clearing its pending verification token. That
     * last part is deliberate: it hands an address someone else registered but never verified back
     * to the mailbox's real owner.
     */
    @Transactional
    public GuestAccountEntity resetPassword(String token, String newPassword) {
        LocalDateTime now = LocalDateTime.now(clock);
        GuestAccountEntity account = (token == null || token.isBlank() ? java.util.Optional.<GuestAccountEntity>empty()
                        : guestAccountRepository.findByPasswordResetTokenHash(sha256Hex(token)))
                .filter(a -> a.getPasswordResetExpiresAt() != null && a.getPasswordResetExpiresAt().isAfter(now))
                .orElseThrow(() -> new BadRequestException("This reset link is invalid or has expired."));

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setTokenVersion(account.getTokenVersion() + 1);
        account.setPasswordResetTokenHash(null);
        account.setPasswordResetExpiresAt(null);
        if (account.getEmailVerifiedAt() == null) {
            account.setEmailVerifiedAt(now);
            account.setEmailVerificationToken(null);
            account.setEmailVerificationExpiresAt(null);
            guestLinkService.linkAccount(account, true);
        }
        return guestAccountRepository.save(account);
    }

    static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

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
