package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.PunchSource;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.User;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.time.LocalDate;
import java.util.ArrayList;
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
 * {@code PATCH /users/{id}/name}: a rename is retroactive wherever {@code name} is read live (a
 * punch recorded before the rename comes back under the new name), while audit {@code summary}
 * text written before it keeps the old one. Blank-after-trim is rejected by the service itself,
 * not only by the schema's {@code minLength}. NOT {@code @Transactional} - see
 * {@link BookingAuditLogTests}'s javadoc (AuditLogService.record commits independently).
 */
@SpringBootTest
class UserRenameTests extends AbstractIntegrationTest {

    private static final LocalDate PUNCH_DATE = LocalDate.of(2026, 3, 10);

    @Autowired
    private UserService userService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendancePunchRepository attendancePunchRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setUpSecurityContext() {
        StaffPrincipal principal = new StaffPrincipal("admin-actor", "rename-admin-test@example.com", Role.ADMIN);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String userId : createdUserIds) {
            auditLogRepository.deleteAll(auditEntriesFor(userId));
            attendancePunchRepository.deleteAll(attendancePunchRepository.findAll().stream()
                    .filter(p -> userId.equals(p.getEmployeeUserId()))
                    .toList());
            userRepository.deleteById(userId);
        }
    }

    private List<AuditLogEntity> auditEntriesFor(String userId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.USER && userId.equals(e.getEntityId()))
                .toList();
    }

    private UserEntity persistUser(String name) {
        UserEntity entity = new UserEntity();
        entity.setName(name);
        entity.setFullName("Full Legal Name");
        entity.setRole(Role.WAITER);
        UserEntity saved = userRepository.saveAndFlush(entity);
        createdUserIds.add(saved.getId());
        return saved;
    }

    /** A manual punch's recorder is always a login account - AttendanceService#list resolves its email. */
    private UserEntity persistRecorder() {
        UserEntity entity = new UserEntity();
        entity.setName("Recorder " + UUID.randomUUID());
        entity.setEmail("rename-recorder-" + UUID.randomUUID() + "@example.com");
        entity.setPasswordHash("irrelevant");
        entity.setRole(Role.MANAGER);
        UserEntity saved = userRepository.saveAndFlush(entity);
        createdUserIds.add(saved.getId());
        return saved;
    }

    @Test
    void updateName_trimsAndLeavesEveryOtherFieldUntouched() {
        UserEntity user = persistUser("Nok " + UUID.randomUUID());

        User result = userService.updateName(user.getId(), "  Noknoi  ");

        assertThat(result.getName()).isEqualTo("Noknoi");
        UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Noknoi");
        assertThat(reloaded.getFullName()).isEqualTo("Full Legal Name");
        assertThat(reloaded.getRole()).isEqualTo(Role.WAITER);
        assertThat(reloaded.getEmail()).isNull();
        assertThat(reloaded.getTokenVersion()).isZero();
        assertThat(reloaded.isActive()).isEqualTo(user.isActive());
    }

    @Test
    void updateName_blankOrWhitespaceOnly_isRejectedAndNothingChanges() {
        String original = "Keep " + UUID.randomUUID();
        UserEntity user = persistUser(original);

        assertThatThrownBy(() -> userService.updateName(user.getId(), "   ")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> userService.updateName(user.getId(), "")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> userService.updateName(user.getId(), null)).isInstanceOf(BadRequestException.class);

        assertThat(userRepository.findById(user.getId()).orElseThrow().getName()).isEqualTo(original);
    }

    @Test
    void updateName_relabelsEarlierPunchesButNotEarlierAuditSummaries() {
        String oldName = "Old " + UUID.randomUUID();
        UserEntity user = persistUser(oldName);
        userService.updateFullName(user.getId(), org.openapitools.jackson.nullable.JsonNullable.of("Before Rename"));

        AttendancePunchEntity punch = new AttendancePunchEntity();
        punch.setEmployeeUserId(user.getId());
        punch.setPunchAt(PUNCH_DATE.atTime(9, 0));
        punch.setDirection(PunchDirection.IN);
        punch.setSource(PunchSource.MANUAL);
        punch.setRecordedByUserId(persistRecorder().getId());
        attendancePunchRepository.saveAndFlush(punch);

        String newName = "New " + UUID.randomUUID();
        userService.updateName(user.getId(), newName);

        List<AttendancePunch> punches = attendanceService.list(user.getId(), PUNCH_DATE, PUNCH_DATE);
        assertThat(punches).singleElement().satisfies(p -> assertThat(p.getEmployeeName()).isEqualTo(newName));

        List<AuditLogEntity> entries = auditEntriesFor(user.getId());
        assertThat(entries).filteredOn(e -> e.getAction() == AuditAction.USER_FULL_NAME_CHANGED)
                .singleElement()
                .satisfies(e -> assertThat(e.getSummary()).contains(oldName).doesNotContain(newName));
        assertThat(entries).filteredOn(e -> e.getAction() == AuditAction.USER_NAME_CHANGED)
                .singleElement()
                .satisfies(e -> assertThat(e.getSummary()).isEqualTo("Renamed " + oldName + " to " + newName));
    }
}
