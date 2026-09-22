package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.GuestAccountRegisterInput;
import com.sunsetbeach.model.GuestBookingView;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real Postgres, rolled back after each test - see AbstractIntegrationTest) - covers
 * the guest-account lifecycle (register/verify/resend/login/change-password) and the "booking
 * history is a live email match, never a stored link" mechanism, both of which depend on real
 * unique-constraint and case-insensitive-query behavior a mocked repository can't exercise.
 */
@SpringBootTest
@Transactional
class GuestAccountServiceTests extends AbstractIntegrationTest {

    @Autowired
    private GuestAccountService guestAccountService;

    @Autowired
    private GuestAccountRepository guestAccountRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private EmailService emailService;

    private RoomEntity room;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Guest Account Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by GuestAccountServiceTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1500.00"));
        room = roomRepository.saveAndFlush(newRoom);
    }

    private GuestAccountRegisterInput registerInput(String email, String password) {
        GuestAccountRegisterInput input = new GuestAccountRegisterInput(email, password);
        input.name("Test Guest");
        return input;
    }

    @Test
    void register_newEmail_createsUnverifiedAccount_andSendsVerificationEmail() {
        String email = "new-guest-" + UUID.randomUUID() + "@example.com";

        guestAccountService.register(registerInput(email, "a-good-password1"));

        GuestAccountEntity account = guestAccountRepository.findByEmail(email).orElseThrow();
        assertThat(account.getEmailVerifiedAt()).isNull();
        assertThat(account.getEmailVerificationToken()).isNotBlank();
        assertThat(account.getEmailVerificationExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(passwordEncoder.matches("a-good-password1", account.getPasswordHash())).isTrue();

        verify(emailService).sendGuestVerificationEmail(eq(email), eq("Test Guest"), eq(account.getEmailVerificationToken()));
    }

    @Test
    void register_alreadyVerifiedEmail_doesNothingAndSendsNoEmail() {
        String email = "verified-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity existing = persistAccount(email, "original-password1", LocalDateTime.now());

        guestAccountService.register(registerInput(email, "attacker-password1"));

        GuestAccountEntity reloaded = guestAccountRepository.findById(existing.getId()).orElseThrow();
        // Untouched: still the original password, still verified, no token appeared.
        assertThat(passwordEncoder.matches("original-password1", reloaded.getPasswordHash())).isTrue();
        assertThat(reloaded.getEmailVerifiedAt()).isNotNull();
        assertThat(reloaded.getEmailVerificationToken()).isNull();
        assertThat(guestAccountRepository.findAll().stream().filter(a -> a.getEmail().equals(email)).count()).isEqualTo(1);
        verify(emailService, never()).sendGuestVerificationEmail(anyString(), any(), anyString());
    }

    @Test
    void register_alreadyUnverifiedEmail_regeneratesTokenWithoutDuplicating() {
        String email = "pending-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity existing = persistAccount(email, "original-password1", null);
        String firstToken = existing.getEmailVerificationToken();

        guestAccountService.register(registerInput(email, "second-attempt-password1"));

        List<GuestAccountEntity> matches = guestAccountRepository.findAll().stream().filter(a -> a.getEmail().equals(email)).toList();
        assertThat(matches).hasSize(1);
        GuestAccountEntity reloaded = matches.get(0);
        assertThat(reloaded.getId()).isEqualTo(existing.getId());
        assertThat(reloaded.getEmailVerificationToken()).isNotEqualTo(firstToken);
        verify(emailService).sendGuestVerificationEmail(eq(email), any(), eq(reloaded.getEmailVerificationToken()));
    }

    @Test
    void resendVerification_unverifiedAccount_regeneratesToken() {
        String email = "resend-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity existing = persistAccount(email, "password1234", null);
        String firstToken = existing.getEmailVerificationToken();

        guestAccountService.resendVerification(email.toUpperCase());

        GuestAccountEntity reloaded = guestAccountRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getEmailVerificationToken()).isNotEqualTo(firstToken);
        verify(emailService).sendGuestVerificationEmail(eq(email), any(), eq(reloaded.getEmailVerificationToken()));
    }

    @Test
    void resendVerification_alreadyVerifiedOrUnknownEmail_doesNothing() {
        String verifiedEmail = "already-verified-" + UUID.randomUUID() + "@example.com";
        persistAccount(verifiedEmail, "password1234", LocalDateTime.now());

        guestAccountService.resendVerification(verifiedEmail);
        guestAccountService.resendVerification("unknown-" + UUID.randomUUID() + "@example.com");

        verify(emailService, never()).sendGuestVerificationEmail(anyString(), any(), anyString());
    }

    @Test
    void verify_validToken_setsVerifiedAndClearsTokenAndReturnsWorkingAccount() {
        String email = "verify-me-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity existing = persistAccount(email, "password1234", null);
        String token = existing.getEmailVerificationToken();

        GuestAccountEntity verified = guestAccountService.verify(token);

        assertThat(verified.getEmailVerifiedAt()).isNotNull();
        assertThat(verified.getEmailVerificationToken()).isNull();
        assertThat(verified.getEmailVerificationExpiresAt()).isNull();
    }

    @Test
    void verify_expiredToken_isRejected() {
        String email = "expired-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity existing = persistAccount(email, "password1234", null);
        existing.setEmailVerificationExpiresAt(LocalDateTime.now().minusHours(1));
        guestAccountRepository.saveAndFlush(existing);

        assertThatThrownBy(() -> guestAccountService.verify(existing.getEmailVerificationToken()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired verification token");
    }

    @Test
    void verify_wrongToken_isRejected() {
        assertThatThrownBy(() -> guestAccountService.verify("not-a-real-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired verification token");
    }

    @Test
    void login_wrongPassword_isUnauthorized() {
        String email = "login-wrong-pw-" + UUID.randomUUID() + "@example.com";
        persistAccount(email, "correct-password1", LocalDateTime.now());

        assertThatThrownBy(() -> guestAccountService.login(email, "wrong-password1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_unknownEmail_isUnauthorized() {
        assertThatThrownBy(() -> guestAccountService.login("nobody-" + UUID.randomUUID() + "@example.com", "whatever12"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_correctPasswordButUnverified_isForbiddenNotUnauthorized() {
        String email = "login-unverified-" + UUID.randomUUID() + "@example.com";
        persistAccount(email, "correct-password1", null);

        assertThatThrownBy(() -> guestAccountService.login(email, "correct-password1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Please verify your email before logging in.");
    }

    @Test
    void login_correctPasswordAndVerified_succeeds() {
        String email = "login-ok-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity existing = persistAccount(email, "correct-password1", LocalDateTime.now());

        GuestAccountEntity result = guestAccountService.login(email, "correct-password1");

        assertThat(result.getId()).isEqualTo(existing.getId());
    }

    @Test
    void changePassword_bumpsTokenVersion_oldPasswordStopsMatching_newPasswordMatches() {
        String email = "change-pw-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity existing = persistAccount(email, "old-password1", LocalDateTime.now());
        int originalVersion = existing.getTokenVersion();

        GuestAccountEntity updated = guestAccountService.changePassword(existing.getId(), "old-password1", "brand-new-password1");

        assertThat(updated.getTokenVersion()).isEqualTo(originalVersion + 1);
        assertThat(passwordEncoder.matches("old-password1", updated.getPasswordHash())).isFalse();
        assertThat(passwordEncoder.matches("brand-new-password1", updated.getPasswordHash())).isTrue();
    }

    @Test
    void changePassword_wrongCurrentPassword_isUnauthorized() {
        String email = "change-pw-wrong-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity existing = persistAccount(email, "old-password1", LocalDateTime.now());

        assertThatThrownBy(() -> guestAccountService.changePassword(existing.getId(), "not-the-current-password", "brand-new-password1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    void listBookings_matchesCaseInsensitively_newestFirst_excludesOtherEmails() {
        String email = "guest-history-" + UUID.randomUUID() + "@example.com";
        String otherEmail = "someone-else-" + UUID.randomUUID() + "@example.com";

        BookingEntity older = persistBooking(email.toUpperCase(), LocalDateTime.now().minusDays(2));
        BookingEntity newer = persistBooking(email, LocalDateTime.now());
        persistBooking(otherEmail, LocalDateTime.now());

        List<GuestBookingView> history = guestAccountService.listBookings(email);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getId()).isEqualTo(newer.getId());
        assertThat(history.get(1).getId()).isEqualTo(older.getId());
        assertThat(history).extracting(GuestBookingView::getRoomName).containsOnly(room.getName());
    }

    @Test
    void listBookings_noMatches_returnsEmpty() {
        assertThat(guestAccountService.listBookings("nobody-" + UUID.randomUUID() + "@example.com")).isEmpty();
    }

    private GuestAccountEntity persistAccount(String email, String rawPassword, LocalDateTime emailVerifiedAt) {
        GuestAccountEntity entity = new GuestAccountEntity();
        entity.setEmail(email.toLowerCase());
        entity.setPasswordHash(passwordEncoder.encode(rawPassword));
        entity.setName("Test Guest");
        entity.setEmailVerifiedAt(emailVerifiedAt);
        if (emailVerifiedAt == null) {
            entity.setEmailVerificationToken(UUID.randomUUID().toString());
            entity.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));
        }
        return guestAccountRepository.saveAndFlush(entity);
    }

    private BookingEntity persistBooking(String guestEmail, LocalDateTime createdAt) {
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName("Guest");
        booking.setGuestEmail(guestEmail);
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.now().plusDays(30));
        booking.setCheckOut(LocalDate.now().plusDays(31));
        booking.setTotalPrice(new BigDecimal("1500.00"));
        booking.setStatus(BookingStatus.NEW);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        // @CreationTimestamp is a Hibernate insert-time value generator - backdating it needs a
        // native UPDATE plus clearing the persistence context, same as BookingExpiryServiceTests's
        // own persistBooking helper (see that class's own comment for why).
        entityManager.createNativeQuery("UPDATE \"Booking\" SET \"createdAt\" = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", saved.getId())
                .executeUpdate();
        entityManager.clear();
        return bookingRepository.findById(saved.getId()).orElseThrow();
    }
}
