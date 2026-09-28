package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.LifecycleEmailSettings;
import com.sunsetbeach.model.LifecycleEmailSettingsUpdateInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code @Transactional} rolls the settings row back after each test; the audit rows
 * {@link AuditLogService#record} commits on its own (REQUIRES_NEW) are removed in
 * {@link #deleteAuditRows()} - see CLAUDE.md, "Tests". That runs {@code @AfterTransaction}, not
 * {@code @AfterEach}: an {@code @AfterEach} runs inside the test's transaction, so its deletes
 * would be rolled back along with everything else.
 */
@SpringBootTest
@Transactional
class LifecycleEmailSettingsServiceTests extends AbstractIntegrationTest {

    @Autowired
    private LifecycleEmailSettingsService service;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void authenticateAsAdmin() {
        StaffPrincipal principal = new StaffPrincipal("admin-actor", "lifecycle-settings-test@example.com", Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterTransaction
    void deleteAuditRows() {
        auditLogRepository.deleteAll(settingsAuditRows());
    }

    private List<AuditLogEntity> settingsAuditRows() {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.LIFECYCLE_EMAIL_SETTINGS_UPDATED)
                .filter(e -> "lifecycle-settings-test@example.com".equals(e.getActorEmail()))
                .toList();
    }

    private static LifecycleEmailSettingsUpdateInput input(String reviewUrl) {
        return new LifecycleEmailSettingsUpdateInput(true, 5, true, 2, false, 18).postStayReviewUrl(reviewUrl);
    }

    @Test
    void seededDefaults_haveEveryTypeDisabled() {
        LifecycleEmailSettings settings = service.get();

        assertThat(settings.getPreArrivalEnabled()).isFalse();
        assertThat(settings.getPostStayEnabled()).isFalse();
        assertThat(settings.getWinBackEnabled()).isFalse();
    }

    @Test
    void update_replacesEveryField_trimsTheUrl_andAuditsWhatChanged() {
        LifecycleEmailSettings updated = service.update(input("  https://reviews.example.com/sunset  "));

        assertThat(updated.getPreArrivalEnabled()).isTrue();
        assertThat(updated.getPreArrivalDaysBefore()).isEqualTo(5);
        assertThat(updated.getPostStayDaysAfter()).isEqualTo(2);
        assertThat(updated.getPostStayReviewUrl().get()).isEqualTo("https://reviews.example.com/sunset");
        assertThat(updated.getWinBackMonthsSinceStay()).isEqualTo(18);
        assertThat(service.get().getPreArrivalDaysBefore()).isEqualTo(5);

        assertThat(settingsAuditRows()).singleElement().satisfies(row -> {
            assertThat(row.getEntityType()).isEqualTo(AuditEntityType.SETTINGS);
            assertThat(row.getSummary()).contains("pre-arrival enabled").contains("review URL set to https://reviews.example.com/sunset");
        });
    }

    @Test
    void update_withNothingChanged_recordsNoAuditRow() {
        service.update(input(null));
        auditLogRepository.deleteAll(settingsAuditRows());

        service.update(input(null));

        assertThat(settingsAuditRows()).isEmpty();
    }

    @Test
    void blankReviewUrl_meansNone() {
        assertThat(service.update(input("   ")).getPostStayReviewUrl().get()).isNull();
    }

    @Test
    void nonHttpReviewUrl_isRejected() {
        for (String bad : List.of("javascript:alert(1)", "reviews.example.com", "/relative", "ftp://example.com/x")) {
            assertThatThrownBy(() -> service.update(input(bad))).as(bad).isInstanceOf(ValidationException.class);
        }
    }
}
