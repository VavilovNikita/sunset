package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.entity.RosterImportNameMappingEntity;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.RosterGridImportCommitInput;
import com.sunsetbeach.model.RosterGridImportPreview;
import com.sunsetbeach.model.RosterGridImportResult;
import com.sunsetbeach.model.RosterGridImportRetiredCodeResolution;
import com.sunsetbeach.model.RosterGridImportUnknownCodeResolution;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.RosterImportNameMappingRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.rosterimport.RosterGridImportFormat;
import com.sunsetbeach.security.StaffPrincipal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
 * {@link RosterGridImportService} re-importing a file {@link RosterExportService#exportGrid}
 * actually produced. Most tests build the file via a real export (so the hidden metadata is
 * exactly what production writes) and then simulate a hand edit by mutating the workbook bytes
 * directly - either the visible cell only (a retyped code, which must NOT still trust the now-stale
 * hidden pointer) or the hidden employee-id column only (a row whose metadata was lost), never
 * both, so each test isolates exactly one fallback trigger.
 */
@SpringBootTest
class RosterGridImportServiceTests extends AbstractIntegrationTest {

    @Autowired
    private RosterGridImportService rosterGridImportService;

    @Autowired
    private RosterExportService rosterExportService;

    @Autowired
    private RosterService rosterService;

    @Autowired
    private ShiftCodeService shiftCodeService;

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private RosterEntryRepository rosterEntryRepository;

    @Autowired
    private RosterImportNameMappingRepository nameMappingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdShiftCodeIds = new ArrayList<>();
    private UserEntity admin;

    @BeforeEach
    void setUpAdminAndSecurityContext() {
        admin = createUser(Role.ADMIN, null);
        StaffPrincipal principal = new StaffPrincipal(admin.getId(), admin.getEmail(), Role.ADMIN);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        nameMappingRepository.deleteAll(nameMappingRepository.findAll().stream()
                .filter(m -> createdUserIds.contains(m.getEmployeeUserId())).toList());
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createUser(Role role, StaffArea area) {
        UserEntity user = new UserEntity();
        user.setEmail("grid-import-test-" + UUID.randomUUID() + "@example.com");
        user.setName("GridImport " + UUID.randomUUID().toString().substring(0, 8));
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(role);
        user.setActive(true);
        user.setStaffArea(area);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private ShiftCode createCode(StaffArea area, String code, String start1, String end1) {
        return createCode(area, code, start1, end1, "2020-01-01");
    }

    /**
     * Explicit {@code effectiveFrom} for the "retire and redefine the same code" tests - the DB's
     * own {@code (staffArea, code, effectiveFrom)} uniqueness (see CLAUDE.md's Migrations section)
     * means two versions of the same code can never share a date, even though {@code
     * ShiftCodeService#create} itself deactivates the first automatically.
     */
    private ShiftCode createCode(StaffArea area, String code, String start1, String end1, String effectiveFrom) {
        ShiftCodeCreateInput input = new ShiftCodeCreateInput(code, ShiftCodeKind.MORNING, true, true, effectiveFrom).staffArea(area);
        if (start1 != null) input.startTime1(start1).endTime1(end1);
        ShiftCode saved = shiftCodeService.create(input, admin.getId());
        createdShiftCodeIds.add(saved.getId());
        return saved;
    }

    private RosterEntryEntity entryFor(String employeeUserId, LocalDate date) {
        return rosterEntryRepository.findByEmployeeUserIdAndDate(employeeUserId, date).orElseThrow();
    }

    @SuppressWarnings("resource")
    private Row findEmployeeRow(Sheet sheet, String employeeName) {
        for (Row row : sheet) {
            Cell first = row.getCell(0);
            if (first != null && first.getCellType() == CellType.STRING && employeeName.equals(first.getStringCellValue())) {
                return row;
            }
        }
        throw new AssertionError("Employee row not found: " + employeeName);
    }

    /** Retypes only the visible cell's text - the hidden shiftCodeId companion column is left exactly as export wrote it, simulating a hand edit. */
    private byte[] retypeVisibleCode(byte[] bytes, int year, int month, String employeeName, int day, String newText) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet(RosterGridImportFormat.visibleSheetName(year, month));
            findEmployeeRow(sheet, employeeName).createCell(day).setCellValue(newText);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Blanks only the hidden employee-id column for this row - simulating a row whose export metadata was lost/never existed. */
    private byte[] clearHiddenEmployeeId(byte[] bytes, int year, int month, String employeeName, int days) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet(RosterGridImportFormat.visibleSheetName(year, month));
            Row row = findEmployeeRow(sheet, employeeName);
            Cell cell = row.getCell(RosterGridImportFormat.employeeIdColumn(days));
            if (cell != null) row.removeCell(cell);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private MockMultipartFile multipart(byte[] bytes) {
        return new MockMultipartFile("file", "roster.xlsx", "application/vnd.openxmlformats", bytes);
    }

    @Test
    void preview_unmodifiedRoundTrip_everythingUnchangedAndCommitIsANoOp() throws IOException {
        UserEntity emp = createUser(Role.WAITER, StaffArea.RESTAURANT);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "RT" + UUID.randomUUID().toString().substring(0, 4), "09:00", "17:00");
        LocalDate date = LocalDate.of(2028, 3, 10);
        rosterService.createEntry(new RosterEntryCreateInput(emp.getId(), date.toString(), code.getId()), admin.getId());

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId());
        RosterGridImportPreview preview = rosterGridImportService.preview(multipart(bytes), 2028, 3);

        assertThat(preview.getDiffRows()).noneMatch(r -> r.getEmployeeUserId().equals(emp.getId()) && r.getDate().equals(date.toString()));
        assertThat(preview.getCanCommit()).isTrue();

        RosterGridImportResult result = rosterGridImportService.commit(new RosterGridImportCommitInput(preview.getImportId()), admin.getId());
        assertThat(result.getCreated()).isZero();
        assertThat(result.getChanged()).isZero();
        assertThat(result.getRemoved()).isZero();
        assertThat(entryFor(emp.getId(), date).getShiftCodeId()).isEqualTo(code.getId());
    }

    @Test
    void commit_add_recreatesAnEntryDeletedAfterExport() throws IOException {
        UserEntity emp = createUser(Role.WAITER, StaffArea.RESTAURANT);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "AD" + UUID.randomUUID().toString().substring(0, 4), "09:00", "17:00");
        LocalDate date = LocalDate.of(2028, 3, 11);
        rosterService.createEntry(new RosterEntryCreateInput(emp.getId(), date.toString(), code.getId()), admin.getId());

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId());
        rosterEntryRepository.delete(entryFor(emp.getId(), date));

        RosterGridImportPreview preview = rosterGridImportService.preview(multipart(bytes), 2028, 3);
        assertThat(preview.getDiffRows()).anySatisfy(r -> {
            assertThat(r.getEmployeeUserId()).isEqualTo(emp.getId());
            assertThat(r.getDate()).isEqualTo(date.toString());
            assertThat(r.getChangeType().getValue()).isEqualTo("ADD");
            assertThat(r.getNewCode().get()).isEqualTo(code.getCode());
        });
        assertThat(preview.getCanCommit()).isTrue();

        RosterGridImportResult result = rosterGridImportService.commit(new RosterGridImportCommitInput(preview.getImportId()), admin.getId());
        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(entryFor(emp.getId(), date).getShiftCodeId()).isEqualTo(code.getId());
    }

    @Test
    void commit_remove_deletesAnEntryAddedAfterExportAndFlagsItStale() throws IOException {
        UserEntity emp = createUser(Role.WAITER, StaffArea.RESTAURANT);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "RM" + UUID.randomUUID().toString().substring(0, 4), "09:00", "17:00");
        LocalDate date = LocalDate.of(2028, 3, 12);

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId()); // file has a blank cell here
        rosterService.createEntry(new RosterEntryCreateInput(emp.getId(), date.toString(), code.getId()), admin.getId()); // added after export

        RosterGridImportPreview preview = rosterGridImportService.preview(multipart(bytes), 2028, 3);
        assertThat(preview.getDiffRows()).anySatisfy(r -> {
            assertThat(r.getEmployeeUserId()).isEqualTo(emp.getId());
            assertThat(r.getChangeType().getValue()).isEqualTo("REMOVE");
            assertThat(r.getPreviousCode().get()).isEqualTo(code.getCode());
            assertThat(r.getStaleSinceExport()).isTrue();
        });

        RosterGridImportResult result = rosterGridImportService.commit(new RosterGridImportCommitInput(preview.getImportId()), admin.getId());
        assertThat(result.getRemoved()).isEqualTo(1);
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(emp.getId(), date)).isEmpty();
    }

    @Test
    void commit_change_restoresTheFilesCodeOverADirectDbEdit() throws IOException {
        UserEntity emp = createUser(Role.WAITER, StaffArea.RESTAURANT);
        ShiftCode codeA = createCode(StaffArea.RESTAURANT, "CA" + UUID.randomUUID().toString().substring(0, 4), "09:00", "17:00");
        ShiftCode codeB = createCode(StaffArea.RESTAURANT, "CB" + UUID.randomUUID().toString().substring(0, 4), "10:00", "18:00");
        LocalDate date = LocalDate.of(2028, 3, 13);
        rosterService.createEntry(new RosterEntryCreateInput(emp.getId(), date.toString(), codeA.getId()), admin.getId());

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId());
        RosterEntryEntity entry = entryFor(emp.getId(), date);
        entry.setShiftCodeId(codeB.getId());
        rosterEntryRepository.saveAndFlush(entry);

        RosterGridImportPreview preview = rosterGridImportService.preview(multipart(bytes), 2028, 3);
        assertThat(preview.getDiffRows()).anySatisfy(r -> {
            assertThat(r.getChangeType().getValue()).isEqualTo("CHANGE");
            assertThat(r.getPreviousCode().get()).isEqualTo(codeB.getCode());
            assertThat(r.getNewCode().get()).isEqualTo(codeA.getCode());
            assertThat(r.getResolution().getValue()).isEqualTo("POSITIONAL");
        });

        rosterGridImportService.commit(new RosterGridImportCommitInput(preview.getImportId()), admin.getId());
        assertThat(entryFor(emp.getId(), date).getShiftCodeId()).isEqualTo(codeA.getId());
    }

    @Test
    void commit_lockedEntry_neverOverwrittenAndCountedSeparately() throws IOException {
        UserEntity emp = createUser(Role.WAITER, StaffArea.RESTAURANT);
        ShiftCode codeA = createCode(StaffArea.RESTAURANT, "LA" + UUID.randomUUID().toString().substring(0, 4), "09:00", "17:00");
        ShiftCode codeB = createCode(StaffArea.RESTAURANT, "LB" + UUID.randomUUID().toString().substring(0, 4), "10:00", "18:00");
        LocalDate date = LocalDate.of(2028, 3, 14);
        rosterService.createEntry(new RosterEntryCreateInput(emp.getId(), date.toString(), codeA.getId()), admin.getId());

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId());
        RosterEntryEntity entry = entryFor(emp.getId(), date);
        entry.setShiftCodeId(codeB.getId());
        entry.setLocked(true);
        rosterEntryRepository.saveAndFlush(entry);

        RosterGridImportPreview preview = rosterGridImportService.preview(multipart(bytes), 2028, 3);
        assertThat(preview.getDiffRows()).anySatisfy(r -> {
            assertThat(r.getChangeType().getValue()).isEqualTo("CHANGE");
            assertThat(r.getLockedConflict()).isTrue();
        });
        assertThat(preview.getCanCommit()).isTrue(); // a locked conflict never blocks commit, only skips itself

        RosterGridImportResult result = rosterGridImportService.commit(new RosterGridImportCommitInput(preview.getImportId()), admin.getId());
        assertThat(result.getSkippedLocked()).isEqualTo(1);
        assertThat(result.getChanged()).isZero();
        assertThat(entryFor(emp.getId(), date).getShiftCodeId()).isEqualTo(codeB.getId()); // untouched
    }

    /**
     * The direct regression test for keying cell resolution positionally rather than by code text:
     * two departments share the same {@code code} string on two different {@code ShiftCode} rows.
     * A resolution that conflated them would either spuriously flag both as changed or, worse,
     * silently swap one employee's entry onto the other department's code.
     */
    @Test
    void resolution_sameCodeTextInTwoAreas_neverConflated() throws IOException {
        ShiftCode codeFrontOffice = createCode(StaffArea.FRONT_OFFICE, "SAME", "09:00", "17:00");
        ShiftCode codeKitchen = createCode(StaffArea.KITCHEN, "SAME", "10:00", "18:00");
        ShiftCode differentKitchenCode = createCode(StaffArea.KITCHEN, "DIFF" + UUID.randomUUID().toString().substring(0, 4), "11:00", "19:00");
        UserEntity empFrontOffice = createUser(Role.WAITER, StaffArea.FRONT_OFFICE);
        UserEntity empKitchen = createUser(Role.WAITER, StaffArea.KITCHEN);
        LocalDate date = LocalDate.of(2028, 3, 15);
        rosterService.createEntry(new RosterEntryCreateInput(empFrontOffice.getId(), date.toString(), codeFrontOffice.getId()), admin.getId());
        rosterService.createEntry(new RosterEntryCreateInput(empKitchen.getId(), date.toString(), codeKitchen.getId()), admin.getId());

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId());

        RosterGridImportPreview unmodifiedPreview = rosterGridImportService.preview(multipart(bytes), 2028, 3);
        assertThat(unmodifiedPreview.getDiffRows())
                .noneMatch(r -> r.getEmployeeUserId().equals(empFrontOffice.getId()) || r.getEmployeeUserId().equals(empKitchen.getId()));

        // Now change only the kitchen employee's entry directly in the DB - the front office
        // employee's identically-texted "SAME" cell must stay unaffected.
        RosterEntryEntity kitchenEntry = entryFor(empKitchen.getId(), date);
        kitchenEntry.setShiftCodeId(differentKitchenCode.getId());
        rosterEntryRepository.saveAndFlush(kitchenEntry);

        RosterGridImportPreview afterPreview = rosterGridImportService.preview(multipart(bytes), 2028, 3);
        assertThat(afterPreview.getDiffRows()).noneMatch(r -> r.getEmployeeUserId().equals(empFrontOffice.getId()));
        assertThat(afterPreview.getDiffRows()).anySatisfy(r -> {
            assertThat(r.getEmployeeUserId()).isEqualTo(empKitchen.getId());
            assertThat(r.getChangeType().getValue()).isEqualTo("CHANGE");
            assertThat(r.getPreviousCode().get()).isEqualTo(differentKitchenCode.getCode());
            assertThat(r.getNewCode().get()).isEqualTo("SAME");
        });
    }

    @Test
    void preview_thenCommit_retiredCodeIsRecreatedWithItsOwnHistoricalHours() throws IOException {
        UserEntity emp = createUser(Role.WAITER, StaffArea.MAINTENANCE);
        String codeText = "RET" + UUID.randomUUID().toString().substring(0, 4);
        ShiftCode original = createCode(StaffArea.MAINTENANCE, codeText, "09:00", "17:00");
        LocalDate date = LocalDate.of(2028, 3, 16);
        rosterService.createEntry(new RosterEntryCreateInput(emp.getId(), date.toString(), original.getId()), admin.getId());

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId());

        // Retires `original` by defining a new version of the same (area, code) with different
        // hours, then deletes the entry itself - so restoring from the file actually needs to
        // WRITE a cell pointing at the now-retired code, not just confirm one that's already
        // there. (If the DB entry still pointed at `original` untouched, there'd be nothing to
        // apply regardless of its retirement - see resolve()'s own "nothing to change" shortcut.)
        ShiftCode replacement = createCode(StaffArea.MAINTENANCE, codeText, "11:00", "19:00", "2021-01-01");
        assertThat(shiftCodeRepository.findById(original.getId()).orElseThrow().isActive()).isFalse();
        rosterEntryRepository.delete(entryFor(emp.getId(), date));

        RosterGridImportPreview preview = rosterGridImportService.preview(multipart(bytes), 2028, 3);
        assertThat(preview.getCanCommit()).isFalse();
        assertThat(preview.getRetiredCodes()).anySatisfy(rc -> {
            assertThat(rc.getShiftCodeId()).isEqualTo(original.getId());
            assertThat(rc.getStartTime1().get()).isEqualTo("09:00"); // the ORIGINAL hours, not the replacement's
        });

        RosterGridImportCommitInput commitInput = new RosterGridImportCommitInput(preview.getImportId());
        commitInput.addRetiredCodeResolutionsItem(new RosterGridImportRetiredCodeResolution(
                        original.getId(), codeText, ShiftCodeKind.MORNING, true, true, LocalDate.now().toString())
                .staffArea(StaffArea.MAINTENANCE).startTime1("09:00").endTime1("17:00"));

        RosterGridImportResult result = rosterGridImportService.commit(commitInput, admin.getId());
        assertThat(result.getCreatedShiftCodes()).isEqualTo(1);

        RosterEntryEntity updatedEntry = entryFor(emp.getId(), date);
        ShiftCodeEntity finalCode = shiftCodeRepository.findById(updatedEntry.getShiftCodeId()).orElseThrow();
        createdShiftCodeIds.add(finalCode.getId());
        assertThat(finalCode.getId()).isNotEqualTo(original.getId()).isNotEqualTo(replacement.getId());
        assertThat(finalCode.getCode()).isEqualTo(codeText);
        assertThat(finalCode.getStartTime1().toString()).isEqualTo("09:00");
    }

    @Test
    void preview_thenCommit_handRetypedCodeWithNoMetadataIsDefinedFromScratch() throws IOException {
        UserEntity emp = createUser(Role.WAITER, StaffArea.KITCHEN);
        ShiftCode original = createCode(StaffArea.KITCHEN, "OR" + UUID.randomUUID().toString().substring(0, 4), "09:00", "17:00");
        LocalDate date = LocalDate.of(2028, 3, 17);
        rosterService.createEntry(new RosterEntryCreateInput(emp.getId(), date.toString(), original.getId()), admin.getId());

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId());
        String handTypedCode = "HT" + UUID.randomUUID().toString().substring(0, 4);
        byte[] handEdited = retypeVisibleCode(bytes, 2028, 3, emp.getName(), date.getDayOfMonth(), handTypedCode);

        RosterGridImportPreview preview = rosterGridImportService.preview(multipart(handEdited), 2028, 3);
        assertThat(preview.getCanCommit()).isFalse();
        assertThat(preview.getUnknownCodes()).anySatisfy(uc -> assertThat(uc.getRawCode()).isEqualTo(handTypedCode));
        assertThat(preview.getDiffRows()).anySatisfy(r -> {
            assertThat(r.getChangeType().getValue()).isEqualTo("CHANGE");
            assertThat(r.getResolution().getValue()).isEqualTo("FALLBACK_CODE");
            assertThat(r.getPendingUnknownCode()).isTrue();
        });

        RosterGridImportCommitInput commitInput = new RosterGridImportCommitInput(preview.getImportId());
        commitInput.addUnknownCodeResolutionsItem(new RosterGridImportUnknownCodeResolution(
                        handTypedCode, handTypedCode, ShiftCodeKind.EVENING, true, true, LocalDate.now().toString())
                .staffArea(StaffArea.KITCHEN).startTime1("14:00").endTime1("22:00"));

        RosterGridImportResult result = rosterGridImportService.commit(commitInput, admin.getId());
        assertThat(result.getCreatedShiftCodes()).isEqualTo(1);

        RosterEntryEntity updatedEntry = entryFor(emp.getId(), date);
        ShiftCodeEntity finalCode = shiftCodeRepository.findById(updatedEntry.getShiftCodeId()).orElseThrow();
        createdShiftCodeIds.add(finalCode.getId());
        assertThat(finalCode.getCode()).isEqualTo(handTypedCode);
    }

    @Test
    void preview_rowWithNoMetadataAndNoNameMapping_isReportedAsUnmatchedAndBlocksCommit() throws IOException {
        UserEntity emp = createUser(Role.WAITER, StaffArea.HOUSEKEEPING);
        ShiftCode code = createCode(StaffArea.HOUSEKEEPING, "UM" + UUID.randomUUID().toString().substring(0, 4), "09:00", "17:00");
        LocalDate date = LocalDate.of(2028, 3, 18);
        rosterService.createEntry(new RosterEntryCreateInput(emp.getId(), date.toString(), code.getId()), admin.getId());

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId());
        byte[] withoutMetadata = clearHiddenEmployeeId(bytes, 2028, 3, emp.getName(), LocalDate.of(2028, 3, 1).lengthOfMonth());

        RosterGridImportPreview preview = rosterGridImportService.preview(multipart(withoutMetadata), 2028, 3);
        assertThat(preview.getUnmatchedEmployees()).anySatisfy(n -> assertThat(n.getRawName()).isEqualTo(emp.getName()));
        assertThat(preview.getCanCommit()).isFalse();

        assertThatThrownBy(() -> rosterGridImportService.commit(new RosterGridImportCommitInput(preview.getImportId()), admin.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("unmatched");
    }

    @Test
    void preview_rowWithNoMetadataButAnExistingNameMapping_resolvesViaFallbackName() throws IOException {
        UserEntity emp = createUser(Role.WAITER, StaffArea.HOUSEKEEPING);
        ShiftCode code = createCode(StaffArea.HOUSEKEEPING, "FN" + UUID.randomUUID().toString().substring(0, 4), "09:00", "17:00");
        LocalDate date = LocalDate.of(2028, 3, 19);
        rosterService.createEntry(new RosterEntryCreateInput(emp.getId(), date.toString(), code.getId()), admin.getId());

        RosterImportNameMappingEntity mapping = new RosterImportNameMappingEntity();
        mapping.setRawName(emp.getName());
        mapping.setEmployeeUserId(emp.getId());
        mapping.setCreatedByUserId(admin.getId());
        nameMappingRepository.saveAndFlush(mapping);

        byte[] bytes = rosterExportService.exportGrid(2028, 3, admin.getId());
        byte[] withoutMetadata = clearHiddenEmployeeId(bytes, 2028, 3, emp.getName(), LocalDate.of(2028, 3, 1).lengthOfMonth());
        // Also retype the visible code so the entry actually differs from the DB - proving both
        // the employee AND the code resolved via fallback (FALLBACK_BOTH), not just one of them.
        String newCode = "FN2" + UUID.randomUUID().toString().substring(0, 4);
        ShiftCode secondCode = createCode(StaffArea.HOUSEKEEPING, newCode, "10:00", "18:00");
        byte[] handEdited = retypeVisibleCode(withoutMetadata, 2028, 3, emp.getName(), date.getDayOfMonth(), newCode);

        RosterGridImportPreview preview = rosterGridImportService.preview(multipart(handEdited), 2028, 3);
        assertThat(preview.getUnmatchedEmployees()).isEmpty();
        assertThat(preview.getCanCommit()).isTrue();
        assertThat(preview.getDiffRows()).anySatisfy(r -> {
            assertThat(r.getEmployeeUserId()).isEqualTo(emp.getId());
            assertThat(r.getResolution().getValue()).isEqualTo("FALLBACK_BOTH");
            assertThat(r.getNewCode().get()).isEqualTo(newCode);
        });

        rosterGridImportService.commit(new RosterGridImportCommitInput(preview.getImportId()), admin.getId());
        assertThat(entryFor(emp.getId(), date).getShiftCodeId()).isEqualTo(secondCode.getId());
    }
}
