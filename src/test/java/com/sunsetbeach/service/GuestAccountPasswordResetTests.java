package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.GuestAccountRegisterInput;
import com.sunsetbeach.repository.GuestAccountRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest password reset ({@code POST /guest-auth/forgot-password} + {@code /reset-password})
 * against the real database: what is stored (a hash, never the emailed token), single use,
 * expiry, sign-out of earlier tokens, and the deliberate side effect on an unverified account -
 * the mailbox owner takes back an address someone else registered first (audit finding H3).
 */
@SpringBootTest
@Transactional
class GuestAccountPasswordResetTests extends AbstractIntegrationTest {

    @Autowired
    private GuestAccountService guestAccountService;

    @Autowired
    private GuestAccountRepository guestAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @MockitoSpyBean
    private EmailService emailService;

    private GuestAccountEntity persistAccount(String email, String rawPassword, boolean verified) {
        GuestAccountEntity entity = new GuestAccountEntity();
        entity.setEmail(email.toLowerCase());
        entity.setPasswordHash(passwordEncoder.encode(rawPassword));
        entity.setName("Reset Guest");
        if (verified) {
            entity.setEmailVerifiedAt(LocalDateTime.now(clock));
        } else {
            entity.setEmailVerificationToken(UUID.randomUUID().toString());
            entity.setEmailVerificationExpiresAt(LocalDateTime.now(clock).plusHours(24));
        }
        return guestAccountRepository.saveAndFlush(entity);
    }

    /** Requests a reset and returns the raw token that went out in the email. */
    private String requestAndCaptureToken(String email) {
        guestAccountService.requestPasswordReset(email);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailService, org.mockito.Mockito.atLeastOnce()).sendGuestPasswordResetEmail(eq(email.toLowerCase()), any(), token.capture());
        List<String> all = token.getAllValues();
        return all.get(all.size() - 1);
    }

    @Test
    void unknownEmail_sendsNothing_andDoesNotThrow() {
        guestAccountService.requestPasswordReset("nobody-" + UUID.randomUUID() + "@example.com");
        verify(emailService, never()).sendGuestPasswordResetEmail(anyString(), any(), anyString());
    }

    @Test
    void request_storesOnlyAHashOfTheEmailedToken_validForAnHour() {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity account = persistAccount(email, "old-password-1", true);

        String token = requestAndCaptureToken(email);

        GuestAccountEntity reloaded = guestAccountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getPasswordResetTokenHash()).isNotBlank().isNotEqualTo(token).isEqualTo(GuestAccountService.sha256Hex(token));
        assertThat(reloaded.getPasswordResetExpiresAt())
                .isAfter(LocalDateTime.now(clock).plusMinutes(55))
                .isBefore(LocalDateTime.now(clock).plusMinutes(65));
    }

    @Test
    void reset_setsThePassword_signsOutEarlierTokens_andCannotBeReused() {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity account = persistAccount(email, "old-password-1", true);
        int versionBefore = account.getTokenVersion();
        String token = requestAndCaptureToken(email);

        guestAccountService.resetPassword(token, "brand-new-password-1");

        GuestAccountEntity reloaded = guestAccountRepository.findById(account.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brand-new-password-1", reloaded.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("old-password-1", reloaded.getPasswordHash())).isFalse();
        assertThat(reloaded.getTokenVersion()).isEqualTo(versionBefore + 1);
        assertThat(reloaded.getPasswordResetTokenHash()).isNull();
        assertThat(reloaded.getPasswordResetExpiresAt()).isNull();

        assertThatThrownBy(() -> guestAccountService.resetPassword(token, "another-password-1")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void expiredToken_isRejected_andChangesNothing() {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        GuestAccountEntity account = persistAccount(email, "old-password-1", true);
        String token = requestAndCaptureToken(email);
        GuestAccountEntity pending = guestAccountRepository.findById(account.getId()).orElseThrow();
        pending.setPasswordResetExpiresAt(LocalDateTime.now(clock).minusMinutes(1));
        guestAccountRepository.saveAndFlush(pending);

        assertThatThrownBy(() -> guestAccountService.resetPassword(token, "brand-new-password-1")).isInstanceOf(BadRequestException.class);
        assertThat(passwordEncoder.matches("old-password-1", guestAccountRepository.findById(account.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void aNewerRequest_invalidatesTheEarlierLink() {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        persistAccount(email, "old-password-1", true);
        String first = requestAndCaptureToken(email);
        String second = requestAndCaptureToken(email);
        assertThat(second).isNotEqualTo(first);

        assertThatThrownBy(() -> guestAccountService.resetPassword(first, "brand-new-password-1")).isInstanceOf(BadRequestException.class);
        guestAccountService.resetPassword(second, "brand-new-password-1");
        verify(emailService, times(2)).sendGuestPasswordResetEmail(eq(email), any(), anyString());
    }

    @Test
    void unknownOrBlankToken_isRejected() {
        assertThatThrownBy(() -> guestAccountService.resetPassword("not-a-real-token", "brand-new-password-1")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> guestAccountService.resetPassword("", "brand-new-password-1")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void mailboxOwner_takesBackAnAddressSomeoneElseRegisteredButNeverVerified() {
        String email = "victim-" + UUID.randomUUID() + "@example.com";
        // Someone else registers the address first, with their own password, and never verifies.
        guestAccountService.register(new GuestAccountRegisterInput(email, "attacker-password-1"));

        String token = requestAndCaptureToken(email);
        GuestAccountEntity reset = guestAccountService.resetPassword(token, "owners-password-1");

        assertThat(reset.getEmailVerifiedAt()).isNotNull();
        assertThat(reset.getEmailVerificationToken()).isNull();
        assertThat(passwordEncoder.matches("owners-password-1", reset.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("attacker-password-1", reset.getPasswordHash())).isFalse();
        assertThat(guestAccountService.login(email, "owners-password-1").getId()).isEqualTo(reset.getId());
    }
}
