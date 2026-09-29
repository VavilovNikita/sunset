package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.RevenueStatisticReport;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.VatSettingsUpdateInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
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
 * Same shape as {@link LifecycleEmailSettingsServiceTests}: {@code @Transactional} rolls the rate
 * back after each test, and the audit rows {@link AuditLogService#record} commits on its own are
 * removed {@code @AfterTransaction}.
 */
@SpringBootTest
@Transactional
class VatSettingsServiceTests extends AbstractIntegrationTest {

    private static final String ACTOR_EMAIL = "vat-settings-test@example.com";

    @Autowired
    private VatSettingsService service;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void authenticateAsAdmin() {
        StaffPrincipal principal = new StaffPrincipal("admin-actor", ACTOR_EMAIL, Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterTransaction
    void deleteAuditRows() {
        auditLogRepository.deleteAll(vatAuditRows());
    }

    private List<AuditLogEntity> vatAuditRows() {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.VAT_SETTINGS_UPDATED)
                .filter(e -> ACTOR_EMAIL.equals(e.getActorEmail()))
                .toList();
    }

    @Test
    void seededDefault_isSevenPercent() {
        assertThat(service.get().getVatRate()).isEqualTo("7.00");
    }

    @Test
    void update_storesTheRate_andAuditsOldAndNew() {
        assertThat(service.update(new VatSettingsUpdateInput(new BigDecimal("10"))).getVatRate()).isEqualTo("10.00");
        assertThat(service.get().getVatRate()).isEqualTo("10.00");

        assertThat(vatAuditRows()).singleElement().satisfies(row -> {
            assertThat(row.getEntityType()).isEqualTo(AuditEntityType.SETTINGS);
            assertThat(row.getSummary()).isEqualTo("VAT rate 7.00% -> 10.00%");
        });
    }

    @Test
    void update_withTheSameRate_recordsNoAuditRow() {
        service.update(new VatSettingsUpdateInput(new BigDecimal("7.0")));

        assertThat(vatAuditRows()).isEmpty();
    }

    @Test
    void update_withMoreThanTwoDecimals_isRejected() {
        assertThatThrownBy(() -> service.update(new VatSettingsUpdateInput(new BigDecimal("7.125"))))
                .isInstanceOf(ValidationException.class);
        assertThat(service.get().getVatRate()).isEqualTo("7.00");
    }

    /** The report reads the stored rate on every request - a change applies to the very next call, no restart. */
    @Test
    void changingTheRate_changesTheNextRevenueStatistic() {
        RevenueStatisticReport before = reportService.revenueStatistic("2033-12-01", "2033-12-31");
        service.update(new VatSettingsUpdateInput(new BigDecimal("10.00")));
        RevenueStatisticReport after = reportService.revenueStatistic("2033-12-01", "2033-12-31");

        assertThat(before.getVatRate()).isEqualTo("7.00");
        assertThat(after.getVatRate()).isEqualTo("10.00");
    }
}
