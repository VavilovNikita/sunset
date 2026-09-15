package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterImportColorMappingInput;
import com.sunsetbeach.model.RosterImportNameMappingInput;
import com.sunsetbeach.model.RosterImportPreview;
import com.sunsetbeach.model.RosterImportResult;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.RosterImportNameMappingRepository;
import com.sunsetbeach.repository.RosterImportShiftColorMappingRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.rosterimport.ScheduleWorkbookBuilder;
import com.sunsetbeach.security.StaffPrincipal;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@link RosterImportService} against small, controlled synthetic workbooks built with {@link
 * ScheduleWorkbookBuilder} - parsing fidelity against the hotel's own real file is {@code
 * ScheduleWorkbookParserTests}' job; this covers the orchestration around it: mapping, the "9"
 * colour resolution (including the shape check at mapping time), commit, collision-skipping, and
 * commit's own idempotent-retry safety.
 */
@SpringBootTest
class RosterImportServiceTests extends AbstractIntegrationTest {

    @Autowired
    private RosterImportService rosterImportService;

    @Autowired
    private ShiftCodeService shiftCodeService;

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private RosterEntryRepository rosterEntryRepository;

    @Autowired
    private RosterImportNameMappingRepository nameMappingRepository;

    @Autowired
    private RosterImportShiftColorMappingRepository colorMappingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdShiftCodeIds = new ArrayList<>();
    private UserEntity admin;

    @BeforeEach
    void setUpAdminAndSecurityContext() {
        admin = createUser(Role.ADMIN);
        StaffPrincipal principal = new StaffPrincipal(admin.getId(), admin.getEmail(), Role.ADMIN);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream().filter(a -> createdUserIds.contains(a.getActorUserId())).toList());
        nameMappingRepository.deleteAll();
        colorMappingRepository.deleteAll();
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("roster-import-test-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(role);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private ShiftCode createCode(StaffArea area, String code, boolean countsAsWorked, String start1, String end1, String start2, String end2) {
        ShiftCodeCreateInput input = new ShiftCodeCreateInput(code, countsAsWorked, true, "2020-01-01").staffArea(area);
        if (start1 != null) {
            input.startTime1(start1).endTime1(end1);
        }
        if (start2 != null) {
            input.startTime2(start2).endTime2(end2);
        }
        ShiftCode saved = shiftCodeService.create(input, admin.getId());
        createdShiftCodeIds.add(saved.getId());
        return saved;
    }

    private MockMultipartFile syntheticFile() throws IOException {
        InputStream in = new ScheduleWorkbookBuilder("Jan26", 1, 2)
                .department("Front Office")
                .person("Alice", "9", "PH")
                .nineFill("Alice", 0, true)
                .department("Kitchen")
                .person("Bob", "9", null)
                .nineFill("Bob", 0, false)
                .stopMarker()
                .build();
        return new MockMultipartFile("file", "schedule.xlsx", "application/vnd.openxmlformats", in.readAllBytes());
    }

    @Test
    void preview_freshUpload_nothingMappedYet() throws IOException {
        createCode(StaffArea.FRONT_OFFICE, "PH", false, null, null, null, null);

        RosterImportPreview preview = rosterImportService.preview(syntheticFile(), 2026, 1);

        assertThat(preview.getIssues()).isEmpty();
        assertThat(preview.getNames()).extracting(n -> n.getRawName()).containsExactlyInAnyOrder("Alice", "Bob");
        assertThat(preview.getNames()).allMatch(n -> !n.getMapped());
        assertThat(preview.getCodes()).anySatisfy(c -> {
            assertThat(c.getRawCode()).isEqualTo("PH");
            assertThat(c.getResolved()).isTrue(); // PH's own ShiftCode already exists, no ambiguity to resolve
        });
        assertThat(preview.getCodes()).anySatisfy(c -> {
            assertThat(c.getRawCode()).isEqualTo("9");
            assertThat(c.getFillColor().get()).isEqualTo(FillColor.YELLOW);
            assertThat(c.getResolved()).isFalse();
        });
        assertThat(preview.getEntriesToCreate()).isEqualTo(0);
        assertThat(preview.getCanCommit()).isFalse();
    }

    @Test
    void colorMapping_shapeMismatch_isRejectedAtMappingTime() throws IOException {
        // A single-interval shift code offered for the YELLOW (single-shift) colour is fine...
        ShiftCode singleShift = createCode(StaffArea.FRONT_OFFICE, "9", true, "09:00", "18:00", null, null);
        rosterImportService.createColorMapping(
                new RosterImportColorMappingInput(StaffArea.FRONT_OFFICE, "9", FillColor.YELLOW, singleShift.getId()), admin.getId());

        // ...but offering that same single-interval code for BLUE (which means split) must be refused, not silently accepted.
        assertThatThrownBy(() -> rosterImportService.createColorMapping(
                new RosterImportColorMappingInput(StaffArea.FRONT_OFFICE, "9", FillColor.BLUE, singleShift.getId()), admin.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("split");
    }

    @Test
    void fullFlow_previewMapCommit_thenRetryIsIdempotentViaCollisions() throws IOException {
        createCode(StaffArea.FRONT_OFFICE, "PH", false, null, null, null, null);
        ShiftCode frontOfficeNine = createCode(StaffArea.FRONT_OFFICE, "9", true, "09:00", "18:00", null, null);
        ShiftCode kitchenSplitNine = createCode(StaffArea.KITCHEN, "9B", true, "09:00", "13:00", "16:00", "21:00");
        UserEntity existingAlice = createUser(Role.WAITER);

        RosterImportPreview firstPreview = rosterImportService.preview(syntheticFile(), 2026, 1);
        String importId = firstPreview.getImportId();

        rosterImportService.createNameMapping(new RosterImportNameMappingInput("Alice").employeeUserId(existingAlice.getId()), admin.getId());
        var bobMapping = rosterImportService.createNameMapping(new RosterImportNameMappingInput("Bob").newEmployeeName("Bob Created On The Spot"), admin.getId());
        createdUserIds.add(bobMapping.getEmployeeUserId());
        rosterImportService.createColorMapping(
                new RosterImportColorMappingInput(StaffArea.FRONT_OFFICE, "9", FillColor.YELLOW, frontOfficeNine.getId()), admin.getId());
        rosterImportService.createColorMapping(
                new RosterImportColorMappingInput(StaffArea.KITCHEN, "9", FillColor.BLUE, kitchenSplitNine.getId()), admin.getId());

        RosterImportPreview resolvedPreview = rosterImportService.preview(syntheticFile(), 2026, 1);
        assertThat(resolvedPreview.getCanCommit()).isTrue();
        assertThat(resolvedPreview.getEntriesToCreate()).isEqualTo(3); // Alice's 2 cells, Bob's 1 (his day 2 is a day off)

        RosterImportResult result = rosterImportService.commit(importId, admin.getId());
        assertThat(result.getCreated()).isEqualTo(3);
        assertThat(result.getSkippedCollisions()).isEqualTo(0);

        List<RosterEntryEntity> aliceEntries = rosterEntryRepository.findByEmployeeUserIdAndDateBetween(
                existingAlice.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        assertThat(aliceEntries).hasSize(2);
        assertThat(aliceEntries).extracting(e -> e.getShiftCodeId()).contains(frontOfficeNine.getId());

        List<AuditLogEntity> auditEntries =
                auditLogRepository.findAll().stream().filter(a -> a.getAction() == AuditAction.ROSTER_MONTH_IMPORTED && a.getActorUserId().equals(admin.getId())).toList();
        assertThat(auditEntries).hasSize(1);
        assertThat(auditEntries.get(0).getSummary()).contains("2026-01").contains("3");

        // Re-committing the same staged file (a retry after a lost response, or just running it
        // again by mistake) must not duplicate anything - every cell now collides instead.
        RosterImportResult retryResult = rosterImportService.commit(importId, admin.getId());
        assertThat(retryResult.getCreated()).isEqualTo(0);
        assertThat(retryResult.getSkippedCollisions()).isEqualTo(3);
        List<RosterEntryEntity> aliceEntriesAfterRetry = rosterEntryRepository.findByEmployeeUserIdAndDateBetween(
                existingAlice.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        assertThat(aliceEntriesAfterRetry).hasSize(2); // still 2, not 4
    }

    @Test
    void commit_stillUnresolved_refuses() throws IOException {
        RosterImportPreview preview = rosterImportService.preview(syntheticFile(), 2026, 1);

        assertThatThrownBy(() -> rosterImportService.commit(preview.getImportId(), admin.getId())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void commit_unknownImportId_isNotFound() {
        assertThatThrownBy(() -> rosterImportService.commit("does-not-exist", admin.getId())).isInstanceOf(NotFoundException.class);
    }
}
