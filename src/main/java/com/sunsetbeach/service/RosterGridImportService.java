package com.sunsetbeach.service;

import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.RosterGridImportCommitInput;
import com.sunsetbeach.model.RosterGridImportDiffRow;
import com.sunsetbeach.model.RosterGridImportPreview;
import com.sunsetbeach.model.RosterGridImportResult;
import com.sunsetbeach.model.RosterGridImportRetiredCode;
import com.sunsetbeach.model.RosterGridImportRetiredCodeResolution;
import com.sunsetbeach.model.RosterGridImportUnknownCode;
import com.sunsetbeach.model.RosterGridImportUnknownCodeResolution;
import com.sunsetbeach.model.RosterImportNameEntry;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.RosterImportNameMappingRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.rosterimport.ParsedGridImport;
import com.sunsetbeach.rosterimport.ParsedGridImportCell;
import com.sunsetbeach.rosterimport.ParsedGridImportRow;
import com.sunsetbeach.rosterimport.RosterGridImportParser;
import com.sunsetbeach.rosterimport.ScheduleParseException;
import com.sunsetbeach.rosterimport.ShiftCodeSnapshot;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Re-imports a file previously produced by {@code RosterExportService#exportGrid} - restoring or
 * syncing a month's {@code RosterEntry} rows from this app's own data, not the hotel's hand-built
 * schedule ({@link RosterImportService} is that one). See {@link RosterGridImportParser} and
 * {@code RosterGridImportFormat} for the hidden metadata this reads and why it's shaped the way it
 * is.
 *
 * <p><b>Sync, not replace.</b> {@link #preview} diffs every {@code (employeeUserId, date)} cell the
 * file covers against the current {@code RosterEntry} state - {@code ADD} (blank in the DB, coded
 * in the file), {@code REMOVE} (coded in the DB, blank in the file), or {@code CHANGE} (coded
 * differently in each). An employee whose entire row was deleted from the file, rather than left
 * with blank cells, is out of scope for this sync - their existing entries are untouched, since
 * there is no per-day cell left to diff against.
 *
 * <p><b>Positional resolution, never a code-text dictionary.</b> {@code ShiftCode.code} is unique
 * only per {@code (staffArea, code, effectiveFrom)}, not globally, so two departments can show the
 * same text for two different rows - resolving a cell by matching its text against a remembered
 * mapping would reintroduce exactly that ambiguity. Every cell's hidden companion column instead
 * names the exact {@code ShiftCode} id the export meant, cross-checked against the visible text
 * (see {@link #resolve}'s own cell loop) so a hand-retyped cell can never resolve to a stale
 * pointer - a mismatch there means the id is ignored and the text is resolved the ordinary way
 * instead, against whatever's active for this employee's own area today.
 *
 * <p><b>Employee resolution reuses the hand-built importer's own remembered mapping.</b> A row with
 * no (or an unresolvable) hidden {@code User.id} falls back to exactly the same {@code
 * RosterImportNameMapping} lookup {@link RosterImportService} uses - resolving a name here also
 * resolves it there, and vice versa, since it's the same underlying fact either way.
 *
 * <p><b>Retired and unknown codes are resolved inline at commit, never as a separate remembered
 * mapping.</b> Unlike a name or a legacy "9" colour, a {@code ShiftCode} id superseded since export
 * (or a hand-typed code with no export metadata at all) is a one-time fact about this specific
 * file, not a recurring ambiguity worth remembering across future imports - see {@link
 * RosterGridImportCommitInput}'s own description.
 */
@Service
public class RosterGridImportService {

    /** Same "once-a-month, few-minutes-long admin task" staging lifetime as {@link RosterImportService}. */
    private static final Duration STAGING_TTL = Duration.ofHours(1);

    private record StagedGridImport(byte[] fileBytes, int year, int month, Instant stagedAt) {
    }

    private final Map<String, StagedGridImport> stagedImports = new ConcurrentHashMap<>();

    private final RosterGridImportParser parser;
    private final ShiftCodeService shiftCodeService;
    private final ShiftCodeRepository shiftCodeRepository;
    private final RosterEntryRepository rosterEntryRepository;
    private final RosterImportNameMappingRepository nameMappingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public RosterGridImportService(
            RosterGridImportParser parser,
            ShiftCodeService shiftCodeService,
            ShiftCodeRepository shiftCodeRepository,
            RosterEntryRepository rosterEntryRepository,
            RosterImportNameMappingRepository nameMappingRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.parser = parser;
        this.shiftCodeService = shiftCodeService;
        this.shiftCodeRepository = shiftCodeRepository;
        this.rosterEntryRepository = rosterEntryRepository;
        this.nameMappingRepository = nameMappingRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public RosterGridImportPreview preview(MultipartFile file, int year, int month) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        ParsedGridImport parsed = parseOrBadRequest(bytes, year, month);

        sweepExpiredStagedImports();
        String importId = UUID.randomUUID().toString();
        stagedImports.put(importId, new StagedGridImport(bytes, year, month, Instant.now()));

        GridResolution resolution = resolve(parsed, year, month);
        return toPreview(importId, year, month, resolution);
    }

    @Transactional
    public RosterGridImportResult commit(RosterGridImportCommitInput input, String actorUserId) {
        StagedGridImport staged = stagedImports.get(input.getImportId());
        if (staged == null) {
            throw new NotFoundException("This import has expired or was never uploaded - upload the file again");
        }

        ParsedGridImport parsed = parseOrBadRequest(staged.fileBytes(), staged.year(), staged.month());
        GridResolution resolution = resolve(parsed, staged.year(), staged.month());

        if (!resolution.unmatchedEmployees.isEmpty()) {
            throw new BadRequestException(
                    "Cannot commit: " + resolution.unmatchedEmployees.size() + " unmatched employee name(s) remain - resolve via "
                            + "POST /roster/import/name-mappings, then call preview again");
        }

        Map<String, RosterGridImportRetiredCodeResolution> retiredResolutionsByOldId = new HashMap<>();
        for (RosterGridImportRetiredCodeResolution r : nullToEmpty(input.getRetiredCodeResolutions())) {
            retiredResolutionsByOldId.put(r.getShiftCodeId(), r);
        }
        List<String> missingRetired = resolution.retiredCodes.keySet().stream().filter(id -> !retiredResolutionsByOldId.containsKey(id)).toList();

        Map<String, RosterGridImportUnknownCodeResolution> unknownResolutionsByKey = new HashMap<>();
        for (RosterGridImportUnknownCodeResolution r : nullToEmpty(input.getUnknownCodeResolutions())) {
            unknownResolutionsByKey.put(unknownCodeKey(r.getStaffArea(), r.getRawCode()), r);
        }
        List<String> missingUnknown = resolution.unknownCodes.keySet().stream().filter(k -> !unknownResolutionsByKey.containsKey(k)).toList();

        if (!missingRetired.isEmpty() || !missingUnknown.isEmpty()) {
            throw new BadRequestException(
                    "Cannot commit: " + missingRetired.size() + " retired code(s) and " + missingUnknown.size() + " unknown code(s) still need a "
                            + "resolution - call preview again to see what's left");
        }

        Map<String, ShiftCodeEntity> recreatedByOldId = new HashMap<>();
        Map<String, ShiftCodeEntity> recreatedByUnknownKey = new HashMap<>();
        int createdShiftCodes = 0;
        for (RosterGridImportRetiredCodeResolution r : nullToEmpty(input.getRetiredCodeResolutions())) {
            if (!resolution.retiredCodes.containsKey(r.getShiftCodeId())) {
                continue; // not actually pending any more (re-derived fresh) - ignore rather than error
            }
            recreatedByOldId.put(r.getShiftCodeId(), createShiftCode(
                    r.getStaffArea(), r.getCode(), r.getKind(), orNull(r.getStartTime1()), orNull(r.getEndTime1()), orNull(r.getStartTime2()),
                    orNull(r.getEndTime2()), r.getCountsAsWorked(), r.getIsPaid(), orNull(r.getDisplayColor()), actorUserId));
            createdShiftCodes++;
        }
        for (RosterGridImportUnknownCodeResolution r : nullToEmpty(input.getUnknownCodeResolutions())) {
            String key = unknownCodeKey(r.getStaffArea(), r.getRawCode());
            if (!resolution.unknownCodes.containsKey(key)) {
                continue;
            }
            recreatedByUnknownKey.put(key, createShiftCode(
                    r.getStaffArea(), r.getCode(), r.getKind(), orNull(r.getStartTime1()), orNull(r.getEndTime1()), orNull(r.getStartTime2()),
                    orNull(r.getEndTime2()), r.getCountsAsWorked(), r.getIsPaid(), orNull(r.getDisplayColor()), actorUserId));
            createdShiftCodes++;
        }

        Set<String> excluded = nullToEmpty(input.getExcludeCells()).stream()
                .map(c -> c.getEmployeeUserId() + "|" + c.getDate())
                .collect(Collectors.toSet());

        int created = 0;
        int removed = 0;
        int changed = 0;
        int skippedLocked = 0;
        int skippedExcluded = 0;
        for (GridDiffRowInternal diff : resolution.diffRows) {
            if (diff.lockedConflict) {
                skippedLocked++;
                continue;
            }
            if (excluded.contains(diff.employeeUserId + "|" + diff.date)) {
                skippedExcluded++;
                continue;
            }

            ShiftCodeEntity finalCode = diff.resolvedShiftCode;
            if (finalCode == null && diff.pendingRetiredCodeId != null) {
                finalCode = recreatedByOldId.get(diff.pendingRetiredCodeId);
            }
            if (finalCode == null && diff.pendingUnknownRawCode != null) {
                finalCode = recreatedByUnknownKey.get(unknownCodeKey(diff.pendingUnknownStaffArea, diff.pendingUnknownRawCode));
            }

            switch (diff.changeType) {
                case "REMOVE" -> {
                    rosterEntryRepository.findByEmployeeUserIdAndDate(diff.employeeUserId, diff.date).ifPresent(rosterEntryRepository::delete);
                    removed++;
                }
                case "ADD" -> {
                    RosterEntryEntity entity = new RosterEntryEntity();
                    entity.setEmployeeUserId(diff.employeeUserId);
                    entity.setDate(diff.date);
                    entity.setShiftCodeId(finalCode.getId());
                    entity.setCreatedByUserId(actorUserId);
                    rosterEntryRepository.save(entity);
                    created++;
                }
                case "CHANGE" -> {
                    ShiftCodeEntity toApply = finalCode;
                    rosterEntryRepository.findByEmployeeUserIdAndDate(diff.employeeUserId, diff.date).ifPresent(existing -> {
                        existing.setShiftCodeId(toApply.getId());
                        rosterEntryRepository.save(existing);
                    });
                    changed++;
                }
                default -> throw new IllegalStateException("Unexpected changeType " + diff.changeType);
            }
        }
        rosterEntryRepository.flush();

        YearMonth ym = YearMonth.of(staged.year(), staged.month());
        auditLogService.record(
                AuditAction.ROSTER_GRID_IMPORTED, AuditEntityType.ROSTER_ENTRY, null,
                "Re-imported the " + ym + " roster grid: " + created + " created, " + changed + " changed, " + removed + " removed, "
                        + skippedLocked + " skipped (locked), " + skippedExcluded + " skipped (excluded), " + createdShiftCodes
                        + " shift code(s) created");

        return new RosterGridImportResult(staged.year(), staged.month(), created, removed, changed, skippedLocked, skippedExcluded, createdShiftCodes);
    }

    private ShiftCodeEntity createShiftCode(
            StaffArea staffArea, String code, ShiftCodeKind kind, String startTime1, String endTime1, String startTime2, String endTime2,
            Boolean countsAsWorked, Boolean isPaid, String displayColor, String actorUserId) {
        ShiftCodeCreateInput createInput = new ShiftCodeCreateInput(code, kind, countsAsWorked, isPaid, LocalDate.now().toString());
        createInput.staffArea(staffArea);
        createInput.startTime1(startTime1);
        createInput.endTime1(endTime1);
        createInput.startTime2(startTime2);
        createInput.endTime2(endTime2);
        ShiftCode created = shiftCodeService.create(createInput, actorUserId);
        if (displayColor != null) {
            shiftCodeService.updateDisplayColor(created.getId(), displayColor, actorUserId);
        }
        return shiftCodeRepository.findById(created.getId()).orElseThrow();
    }

    private ParsedGridImport parseOrBadRequest(byte[] bytes, int year, int month) {
        try {
            return parser.parse(new ByteArrayInputStream(bytes), year, month);
        } catch (ScheduleParseException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private void sweepExpiredStagedImports() {
        Instant cutoff = Instant.now().minus(STAGING_TTL);
        stagedImports.entrySet().removeIf(e -> e.getValue().stagedAt().isBefore(cutoff));
    }

    // --- Shared resolution, used by both preview() and commit() ---

    private static final class GridDiffRowInternal {
        String employeeUserId;
        String employeeName;
        LocalDate date;
        String changeType; // ADD/REMOVE/CHANGE
        String previousCode;
        String newCode;
        boolean employeeFallback;
        boolean codeFallback;
        boolean lockedConflict;
        boolean staleSinceExport;
        ShiftCodeEntity resolvedShiftCode; // null while pending a retired/unknown-code resolution
        String pendingRetiredCodeId;
        String pendingUnknownRawCode;
        StaffArea pendingUnknownStaffArea;
    }

    private static final class RetiredCodeIssue {
        ShiftCodeSnapshot snapshot;
        int occurrences;
    }

    private static final class UnknownCodeIssue {
        String rawCode;
        StaffArea staffArea;
        int occurrences;
    }

    private static final class EmployeeIssue {
        String rawName;
        int occurrences;
    }

    private static final class EmployeeRowResolution {
        String employeeUserId;
        String employeeName;
        StaffArea staffArea;
        boolean viaFallback;
    }

    private static final class GridResolution {
        Instant exportedAt;
        List<GridDiffRowInternal> diffRows = new ArrayList<>();
        int unchangedCount;
        Map<String, EmployeeIssue> unmatchedEmployees = new LinkedHashMap<>();
        Map<String, RetiredCodeIssue> retiredCodes = new LinkedHashMap<>();
        Map<String, UnknownCodeIssue> unknownCodes = new LinkedHashMap<>();
    }

    private GridResolution resolve(ParsedGridImport parsed, int year, int month) {
        GridResolution resolution = new GridResolution();
        resolution.exportedAt = parsed.exportedAt();

        Map<Integer, List<ParsedGridImportCell>> cellsByRow = new HashMap<>();
        for (ParsedGridImportCell cell : parsed.cells()) {
            cellsByRow.computeIfAbsent(cell.rowIndex(), k -> new ArrayList<>()).add(cell);
        }

        Map<Integer, EmployeeRowResolution> rowResolutions = new LinkedHashMap<>();
        Set<String> employeeIdsInFile = new LinkedHashSet<>();
        for (ParsedGridImportRow row : parsed.rows()) {
            UserEntity employee = null;
            boolean viaFallback = false;
            if (row.metadataEmployeeUserId() != null) {
                employee = userRepository.findById(row.metadataEmployeeUserId()).orElse(null);
            }
            if (employee == null) {
                viaFallback = true;
                String normalized = normalizeName(row.rawName());
                employee = nameMappingRepository.findByRawName(normalized)
                        .flatMap(m -> userRepository.findById(m.getEmployeeUserId()))
                        .orElse(null);
            }
            if (employee == null) {
                String key = normalizeName(row.rawName());
                EmployeeIssue issue = resolution.unmatchedEmployees.computeIfAbsent(key, k -> {
                    EmployeeIssue i = new EmployeeIssue();
                    i.rawName = k;
                    return i;
                });
                issue.occurrences += cellsByRow.getOrDefault(row.rowIndex(), List.of()).size();
                continue; // this row's cells cannot be diffed until the employee is resolved
            }

            EmployeeRowResolution rr = new EmployeeRowResolution();
            rr.employeeUserId = employee.getId();
            rr.employeeName = employee.getName();
            rr.staffArea = employee.getStaffArea();
            rr.viaFallback = viaFallback;
            rowResolutions.put(row.rowIndex(), rr);
            employeeIdsInFile.add(employee.getId());
        }

        YearMonth ym = YearMonth.of(year, month);
        int days = ym.lengthOfMonth();

        Map<String, RosterEntryEntity> existingByKey = new HashMap<>();
        for (RosterEntryEntity e : rosterEntryRepository.findByDateBetween(ym.atDay(1), ym.atEndOfMonth())) {
            if (employeeIdsInFile.contains(e.getEmployeeUserId())) {
                existingByKey.put(e.getEmployeeUserId() + "|" + e.getDate(), e);
            }
        }
        Map<String, ShiftCodeEntity> existingShiftCodesById = shiftCodeRepository
                .findAllById(existingByKey.values().stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList()).stream()
                .collect(Collectors.toMap(ShiftCodeEntity::getId, s -> s));

        for (Map.Entry<Integer, EmployeeRowResolution> rowEntry : rowResolutions.entrySet()) {
            EmployeeRowResolution rr = rowEntry.getValue();
            Map<Integer, ParsedGridImportCell> dayCells = cellsByRow.getOrDefault(rowEntry.getKey(), List.of()).stream()
                    .collect(Collectors.toMap(ParsedGridImportCell::day, c -> c));

            for (int day = 1; day <= days; day++) {
                LocalDate date = ym.atDay(day);
                ParsedGridImportCell fileCell = dayCells.get(day);
                RosterEntryEntity existing = existingByKey.get(rr.employeeUserId + "|" + date);

                if (fileCell == null) {
                    if (existing == null) {
                        resolution.unchangedCount++;
                    } else {
                        resolution.diffRows.add(buildRemoveRow(rr, date, existing, existingShiftCodesById, resolution.exportedAt));
                    }
                    continue;
                }

                CellResolution cr = resolveCell(fileCell, rr.staffArea, parsed.shiftCodeSnapshots());
                if (existing != null && cr.resolvedShiftCode != null && cr.resolvedShiftCode.getId().equals(existing.getShiftCodeId())) {
                    resolution.unchangedCount++;
                    continue;
                }
                if (existing != null && cr.resolvedShiftCode == null && cr.pendingRetiredCodeId != null
                        && cr.pendingRetiredCodeId.equals(existing.getShiftCodeId())) {
                    // The file still points at exactly the (now-retired) code this entry already has - nothing to change.
                    resolution.unchangedCount++;
                    continue;
                }

                GridDiffRowInternal diff = new GridDiffRowInternal();
                diff.employeeUserId = rr.employeeUserId;
                diff.employeeName = rr.employeeName;
                diff.date = date;
                diff.changeType = existing == null ? "ADD" : "CHANGE";
                if (existing != null) {
                    ShiftCodeEntity existingCode = existingShiftCodesById.get(existing.getShiftCodeId());
                    diff.previousCode = existingCode != null ? existingCode.getCode() : null;
                }
                diff.newCode = fileCell.rawCode();
                diff.employeeFallback = rr.viaFallback;
                diff.codeFallback = cr.viaFallback;
                diff.lockedConflict = existing != null && existing.isLocked();
                diff.staleSinceExport = existing != null && existing.getUpdatedAt().toInstant(ZoneOffset.UTC).isAfter(resolution.exportedAt);
                diff.resolvedShiftCode = cr.resolvedShiftCode;

                if (!diff.lockedConflict) {
                    if (cr.pendingRetiredCodeId != null) {
                        diff.pendingRetiredCodeId = cr.pendingRetiredCodeId;
                        RetiredCodeIssue issue = resolution.retiredCodes.computeIfAbsent(cr.pendingRetiredCodeId, k -> {
                            RetiredCodeIssue i = new RetiredCodeIssue();
                            i.snapshot = parsed.shiftCodeSnapshots().get(k);
                            return i;
                        });
                        issue.occurrences++;
                    } else if (cr.pendingUnknownRawCode != null) {
                        diff.pendingUnknownRawCode = cr.pendingUnknownRawCode;
                        diff.pendingUnknownStaffArea = rr.staffArea;
                        String key = unknownCodeKey(rr.staffArea, cr.pendingUnknownRawCode);
                        UnknownCodeIssue issue = resolution.unknownCodes.computeIfAbsent(key, k -> {
                            UnknownCodeIssue i = new UnknownCodeIssue();
                            i.rawCode = cr.pendingUnknownRawCode;
                            i.staffArea = rr.staffArea;
                            return i;
                        });
                        issue.occurrences++;
                    }
                }

                resolution.diffRows.add(diff);
            }
        }

        return resolution;
    }

    private GridDiffRowInternal buildRemoveRow(
            EmployeeRowResolution rr, LocalDate date, RosterEntryEntity existing, Map<String, ShiftCodeEntity> existingShiftCodesById,
            Instant exportedAt) {
        GridDiffRowInternal diff = new GridDiffRowInternal();
        diff.employeeUserId = rr.employeeUserId;
        diff.employeeName = rr.employeeName;
        diff.date = date;
        diff.changeType = "REMOVE";
        ShiftCodeEntity existingCode = existingShiftCodesById.get(existing.getShiftCodeId());
        diff.previousCode = existingCode != null ? existingCode.getCode() : null;
        diff.employeeFallback = rr.viaFallback;
        diff.lockedConflict = existing.isLocked();
        diff.staleSinceExport = existing.getUpdatedAt().toInstant(ZoneOffset.UTC).isAfter(exportedAt);
        return diff;
    }

    private record CellResolution(ShiftCodeEntity resolvedShiftCode, boolean viaFallback, String pendingRetiredCodeId, String pendingUnknownRawCode) {
    }

    /**
     * One cell's code resolution - positional (trusting the hidden {@code metadataShiftCodeId})
     * only when that id's own snapshot still names exactly this cell's visible text; otherwise
     * falls back to resolving the visible text the ordinary way. See this class's own javadoc for
     * why a stale hidden id is never trusted blindly.
     */
    private CellResolution resolveCell(ParsedGridImportCell cell, StaffArea staffArea, Map<String, ShiftCodeSnapshot> snapshots) {
        ShiftCodeSnapshot snapshot = cell.metadataShiftCodeId() != null ? snapshots.get(cell.metadataShiftCodeId()) : null;
        boolean positionalTrusted = snapshot != null && snapshot.code().trim().equals(cell.rawCode().trim());

        if (positionalTrusted) {
            ShiftCodeEntity entity = shiftCodeRepository.findById(cell.metadataShiftCodeId()).orElse(null);
            if (entity != null && entity.isActive()) {
                return new CellResolution(entity, false, null, null);
            }
            return new CellResolution(null, false, cell.metadataShiftCodeId(), null);
        }

        ShiftCodeEntity resolved = shiftCodeService.resolveActive(staffArea, cell.rawCode()).orElse(null);
        if (resolved != null) {
            return new CellResolution(resolved, true, null, null);
        }
        return new CellResolution(null, true, null, cell.rawCode());
    }

    // --- DTO assembly ---

    private RosterGridImportPreview toPreview(String importId, int year, int month, GridResolution resolution) {
        List<RosterGridImportDiffRow> diffRows = resolution.diffRows.stream().map(this::toDiffRowDto).toList();

        int addCount = 0;
        int removeCount = 0;
        int changeCount = 0;
        int staleCount = 0;
        int lockedConflictCount = 0;
        for (GridDiffRowInternal d : resolution.diffRows) {
            switch (d.changeType) {
                case "ADD" -> addCount++;
                case "REMOVE" -> removeCount++;
                case "CHANGE" -> changeCount++;
                default -> { }
            }
            if (d.staleSinceExport) staleCount++;
            if (d.lockedConflict) lockedConflictCount++;
        }

        List<RosterImportNameEntry> unmatchedEmployees = resolution.unmatchedEmployees.values().stream()
                .map(i -> new RosterImportNameEntry(i.rawName, i.occurrences, false))
                .toList();
        List<RosterGridImportRetiredCode> retiredCodes = resolution.retiredCodes.entrySet().stream()
                .map(e -> toRetiredCodeDto(e.getKey(), e.getValue()))
                .toList();
        List<RosterGridImportUnknownCode> unknownCodes = resolution.unknownCodes.values().stream()
                .map(this::toUnknownCodeDto)
                .toList();

        boolean canCommit = unmatchedEmployees.isEmpty() && retiredCodes.isEmpty() && unknownCodes.isEmpty();

        return new RosterGridImportPreview(
                importId, year, month, OffsetDateTime.ofInstant(resolution.exportedAt, ZoneOffset.UTC), diffRows, addCount, removeCount,
                changeCount, resolution.unchangedCount, staleCount, lockedConflictCount, unmatchedEmployees, retiredCodes, unknownCodes, canCommit);
    }

    private RosterGridImportDiffRow toDiffRowDto(GridDiffRowInternal d) {
        RosterGridImportDiffRow.ChangeTypeEnum changeType = RosterGridImportDiffRow.ChangeTypeEnum.fromValue(d.changeType);
        RosterGridImportDiffRow.ResolutionEnum resolutionEnum = resolutionEnumFor(d.employeeFallback, d.codeFallback);
        RosterGridImportDiffRow dto =
                new RosterGridImportDiffRow(d.employeeUserId, d.employeeName, d.date.toString(), changeType, resolutionEnum, d.lockedConflict, d.staleSinceExport);
        if (d.previousCode != null) dto.setPreviousCode(JsonNullable.of(d.previousCode));
        if (d.newCode != null) dto.setNewCode(JsonNullable.of(d.newCode));
        if (d.pendingRetiredCodeId != null) dto.setPendingRetiredCodeId(JsonNullable.of(d.pendingRetiredCodeId));
        dto.pendingUnknownCode(d.pendingUnknownRawCode != null);
        return dto;
    }

    private static RosterGridImportDiffRow.ResolutionEnum resolutionEnumFor(boolean employeeFallback, boolean codeFallback) {
        if (employeeFallback && codeFallback) return RosterGridImportDiffRow.ResolutionEnum.FALLBACK_BOTH;
        if (employeeFallback) return RosterGridImportDiffRow.ResolutionEnum.FALLBACK_NAME;
        if (codeFallback) return RosterGridImportDiffRow.ResolutionEnum.FALLBACK_CODE;
        return RosterGridImportDiffRow.ResolutionEnum.POSITIONAL;
    }

    private RosterGridImportRetiredCode toRetiredCodeDto(String shiftCodeId, RetiredCodeIssue issue) {
        ShiftCodeSnapshot s = issue.snapshot;
        RosterGridImportRetiredCode dto =
                new RosterGridImportRetiredCode(shiftCodeId, s.code(), s.kind(), s.countsAsWorked(), s.isPaid(), s.effectiveFrom(), issue.occurrences);
        dto.staffArea(s.staffArea());
        if (s.startTime1() != null) dto.setStartTime1(JsonNullable.of(s.startTime1()));
        if (s.endTime1() != null) dto.setEndTime1(JsonNullable.of(s.endTime1()));
        if (s.startTime2() != null) dto.setStartTime2(JsonNullable.of(s.startTime2()));
        if (s.endTime2() != null) dto.setEndTime2(JsonNullable.of(s.endTime2()));
        if (s.displayColor() != null) dto.setDisplayColor(JsonNullable.of(s.displayColor()));
        return dto;
    }

    private RosterGridImportUnknownCode toUnknownCodeDto(UnknownCodeIssue issue) {
        RosterGridImportUnknownCode dto = new RosterGridImportUnknownCode(issue.rawCode, issue.occurrences);
        if (issue.staffArea != null) dto.setStaffArea(JsonNullable.of(issue.staffArea));
        return dto;
    }

    private static String unknownCodeKey(StaffArea staffArea, String rawCode) {
        return (staffArea != null ? staffArea.getValue() : "") + "|" + rawCode;
    }

    private static String orNull(JsonNullable<String> value) {
        return value != null && value.isPresent() ? value.get() : null;
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list != null ? list : List.of();
    }

    /** Trim and collapse internal whitespace only - never a fuzzy match, same convention as {@code RosterImportService}. */
    private static String normalizeName(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }
}
