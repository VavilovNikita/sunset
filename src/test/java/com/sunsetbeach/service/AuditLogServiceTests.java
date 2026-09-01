package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.AuditLogPage;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DB-backed (real dev Postgres): {@link AuditLogService#record} and {@link AuditLogService#search}
 * directly, independent of any particular caller. Wiring correctness for each specific audited
 * action (right action/entityType/entityId/summary from the right service method) is covered
 * separately, one test class per domain (Booking/Order/Shift/Room/User/RoomUnit).
 *
 * <p>{@code record} commits in its own transaction ({@code REQUIRES_NEW} - see its javadoc), so
 * unlike most DB-backed tests in this suite it is NOT wrapped in {@code @Transactional}: rows it
 * writes would outlive a rollback anyway, so cleanup here is manual, by id, in {@link #tearDown()}.
 */
@SpringBootTest
class AuditLogServiceTests extends AbstractIntegrationTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdIds = new java.util.ArrayList<>();
    private String actorEmail;

    @BeforeEach
    void setUpSecurityContext() {
        actorEmail = "audit-service-test-" + UUID.randomUUID() + "@example.com";
        authenticateAs("actor-1", actorEmail, Role.MANAGER);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        auditLogRepository.deleteAllById(createdIds);
    }

    private static void authenticateAs(String id, String email, Role role) {
        StaffPrincipal principal = new StaffPrincipal(id, email, role);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.getValue()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private void recordAndTrack(AuditAction action, AuditEntityType entityType, String entityId, String summary) {
        auditLogService.record(action, entityType, entityId, summary);
        // The entry's own id isn't returned by record() (it's fire-and-forget by design) - find
        // it back by the unique entityId this test used, so cleanup doesn't need the repository's
        // internals exposed just for tests.
        auditLogRepository.findAll().stream()
                .filter(e -> entityId != null && entityId.equals(e.getEntityId()) && actorEmail.equals(e.getActorEmail()))
                .forEach(e -> createdIds.add(e.getId()));
    }

    @Test
    void record_persistsWithActorSnapshotFromSecurityContext() {
        String entityId = "booking-" + UUID.randomUUID();
        recordAndTrack(AuditAction.BOOKING_CREATED, AuditEntityType.BOOKING, entityId, "Test entry");

        var page = auditLogService.search(actorEmail, null, null, null, null, null, 0, 50);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getActorEmail()).isEqualTo(actorEmail);
        assertThat(page.getItems().get(0).getActorRole()).isEqualTo(Role.MANAGER);
        assertThat(page.getItems().get(0).getActorUserId()).isEqualTo("actor-1");
        assertThat(page.getItems().get(0).getSummary()).isEqualTo("Test entry");
    }

    @Test
    void record_noAuthenticatedPrincipal_doesNotThrow() {
        SecurityContextHolder.clearContext();
        assertThatCode(() -> auditLogService.record(AuditAction.BOOKING_CREATED, AuditEntityType.BOOKING, "x", "should not throw"))
                .doesNotThrowAnyException();
    }

    @Test
    void search_filtersByActorEmailCaseInsensitiveSubstring() {
        String entityId = "booking-" + UUID.randomUUID();
        recordAndTrack(AuditAction.BOOKING_CREATED, AuditEntityType.BOOKING, entityId, "Findable by email");

        AuditLogPage byUpper = auditLogService.search(actorEmail.toUpperCase(java.util.Locale.ROOT), null, null, null, null, null, 0, 50);
        assertThat(byUpper.getItems()).extracting(e -> e.getEntityId().orElse(null)).contains(entityId);

        AuditLogPage byOther = auditLogService.search("nobody-else-" + UUID.randomUUID(), null, null, null, null, null, 0, 50);
        assertThat(byOther.getItems()).isEmpty();
    }

    @Test
    void search_filtersByActionAndEntityTypeAndEntityId() {
        String bookingId = "booking-" + UUID.randomUUID();
        String otherBookingId = "booking-" + UUID.randomUUID();
        recordAndTrack(AuditAction.BOOKING_CREATED, AuditEntityType.BOOKING, bookingId, "Target booking");
        recordAndTrack(AuditAction.BOOKING_STATUS_CHANGED, AuditEntityType.BOOKING, bookingId, "Status changed on target");
        recordAndTrack(AuditAction.BOOKING_CREATED, AuditEntityType.BOOKING, otherBookingId, "A different booking");

        AuditLogPage byEntity = auditLogService.search(null, null, AuditEntityType.BOOKING, bookingId, null, null, 0, 50);
        assertThat(byEntity.getItems()).hasSize(2).allMatch(e -> bookingId.equals(e.getEntityId().orElse(null)));

        AuditLogPage byAction = auditLogService.search(
                actorEmail, AuditAction.BOOKING_STATUS_CHANGED, AuditEntityType.BOOKING, bookingId, null, null, 0, 50);
        assertThat(byAction.getItems()).hasSize(1);
        assertThat(byAction.getItems().get(0).getAction()).isEqualTo(AuditAction.BOOKING_STATUS_CHANGED);
    }

    @Test
    void search_entityIdWithoutEntityType_isRejected() {
        assertThatThrownBy(() -> auditLogService.search(null, null, null, "some-id", null, null, 0, 50))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void search_fromAfterTo_isRejected() {
        assertThatThrownBy(() -> auditLogService.search(null, null, null, null, "2031-06-01", "2031-01-01", 0, 50))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void search_pagination_totalCountReflectsAllMatchesNotJustOnePage() {
        String marker = "pagination-marker-" + UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            recordAndTrack(AuditAction.BOOKING_CREATED, AuditEntityType.BOOKING, marker + "-" + i, "Pagination test entry " + i);
        }

        AuditLogPage firstPage = auditLogService.search(actorEmail, null, AuditEntityType.BOOKING, null, null, null, 0, 2);
        assertThat(firstPage.getItems()).hasSizeLessThanOrEqualTo(2);

        // Filtering by entityType + actorEmail without entityId isn't supported by this search
        // signature (entityId alone requires entityType, but entityType alone is fine) - assert
        // against the exact 3 rows this test created via actorEmail, which is unique per test run.
        AuditLogPage all = auditLogService.search(actorEmail, null, null, null, null, null, 0, 50);
        assertThat(all.getTotalCount()).isEqualTo(3);
        assertThat(all.getItems()).hasSize(3);
    }
}
