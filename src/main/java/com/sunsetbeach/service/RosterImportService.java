package com.sunsetbeach.service;

import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.RosterImportNameMappingEntity;
import com.sunsetbeach.entity.RosterImportShiftColorMappingEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterImportCodeEntry;
import com.sunsetbeach.model.RosterImportCollision;
import com.sunsetbeach.model.RosterImportColorMappingInput;
import com.sunsetbeach.model.RosterImportColorMappingResult;
import com.sunsetbeach.model.RosterImportIssue;
import com.sunsetbeach.model.RosterImportNameEntry;
import com.sunsetbeach.model.RosterImportNameMappingInput;
import com.sunsetbeach.model.RosterImportNameMappingResult;
import com.sunsetbeach.model.RosterImportPreview;
import com.sunsetbeach.model.RosterImportResult;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.RosterImportNameMappingRepository;
import com.sunsetbeach.repository.RosterImportShiftColorMappingRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.rosterimport.ParseIssue;
import com.sunsetbeach.rosterimport.ParsedCell;
import com.sunsetbeach.rosterimport.ParsedSchedule;
import com.sunsetbeach.rosterimport.ScheduleParseException;
import com.sunsetbeach.rosterimport.ScheduleWorkbookParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Imports one month of the hotel's hand-built Excel schedule - see {@code
 * ScheduleWorkbookParser}'s own javadoc for what gets read and why. This class is the seam
 * between that pure parsing and actually writing {@code RosterEntry} rows: {@link #preview}
 * parses and resolves but never writes anything, not even a mapping; {@link #createNameMapping}
 * and {@link #createColorMapping} are the only two calls that record a mapping (used repeatedly,
 * as many times as there are unresolved names/colours, independent of any one import); {@link
 * #commit} is the only call that writes {@code RosterEntry} rows, and it re-derives everything
 * fresh from the staged file rather than trusting an earlier preview's word for what's resolved.
 *
 * <p><b>Names are never matched automatically.</b> {@link #resolveName} only ever looks up an
 * exact, previously-confirmed {@code (trimmed, whitespace-collapsed)} spelling - never a fuzzy or
 * cross-month guess. A name with no mapping yet contributes zero {@code RosterEntry} rows and
 * surfaces in {@code RosterImportPreview#names} with {@code mapped: false} until a person resolves
 * it.
 *
 * <p><b>The "9" ambiguity resolves through a remembered mapping, not a hardcoded substitution -
 * and not one scoped by area.</b> Which code text a colour means is the same fact everywhere in
 * this file (confirmed against the hotel's own legend, which lists exactly two "9" entries, not
 * one pair per department, and against how the codes were actually set up) - {@link
 * #createColorMapping} records it once per {@code (rawCode, fillColor)}, full stop. What *is*
 * resolved per area, separately, at read time, is which actual {@code ShiftCode} row that code
 * text points to for a given cell ({@link ShiftCodeService#resolveActive}) - conflating that
 * area-scoped lookup with the colour-to-text mapping itself was an earlier mistake here (see
 * V81__roster_import_color_mapping_global.sql's own comment), not a fact about the file.
 * {@link #createColorMapping} validates, at mapping time - not later, on every future resolution -
 * that the chosen {@code ShiftCode}'s own shape (a single interval or two) actually matches what
 * the colour means, so a wrong pick is refused immediately rather than silently importing the
 * wrong hours.
 *
 * <p><b>Existing entries are never overwritten.</b> A cell whose {@code (employee, date)} already
 * has a {@code RosterEntry} is skipped and reported as a {@link RosterImportCollision}, exactly
 * once, whether during a dry run or a real commit - resolving it, if the imported value should
 * actually win, means clearing or editing that entry by hand first and re-running the import. This
 * also makes {@link #commit} safe to retry after a failure or a lost response: anything already
 * written the first time shows up as a collision the second time, never a duplicate.
 *
 * <p><b>Comments are never read.</b> See {@code ScheduleWorkbookParser}'s own javadoc for why
 * "do not move", specifically, turned out not to have a reliable general mapping onto {@code
 * RosterEntry#locked} in this hotel's own files.
 */
@Service
public class RosterImportService {

    /** How long a staged upload stays resolvable before a stale one is swept - an import is a once-a-month, few-minutes-long admin task, not a long-lived session. */
    private static final Duration STAGING_TTL = Duration.ofHours(1);

    private record StagedImport(byte[] fileBytes, int year, int month, Instant stagedAt) {
    }

    private final Map<String, StagedImport> stagedImports = new ConcurrentHashMap<>();

    private final ScheduleWorkbookParser parser;
    private final ShiftCodeService shiftCodeService;
    private final ShiftCodeRepository shiftCodeRepository;
    private final RosterEntryRepository rosterEntryRepository;
    private final RosterImportNameMappingRepository nameMappingRepository;
    private final RosterImportShiftColorMappingRepository colorMappingRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public RosterImportService(
            ScheduleWorkbookParser parser,
            ShiftCodeService shiftCodeService,
            ShiftCodeRepository shiftCodeRepository,
            RosterEntryRepository rosterEntryRepository,
            RosterImportNameMappingRepository nameMappingRepository,
            RosterImportShiftColorMappingRepository colorMappingRepository,
            UserRepository userRepository,
            UserService userService,
            AuditLogService auditLogService) {
        this.parser = parser;
        this.shiftCodeService = shiftCodeService;
        this.shiftCodeRepository = shiftCodeRepository;
        this.rosterEntryRepository = rosterEntryRepository;
        this.nameMappingRepository = nameMappingRepository;
        this.colorMappingRepository = colorMappingRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public RosterImportPreview preview(MultipartFile file, int year, int month) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        ParsedSchedule schedule = parseOrBadRequest(bytes, year, month);

        sweepExpiredStagedImports();
        String importId = UUID.randomUUID().toString();
        stagedImports.put(importId, new StagedImport(bytes, year, month, Instant.now()));

        Resolution resolution = resolve(schedule);
        return toPreview(importId, year, month, resolution);
    }

    @Transactional
    public RosterImportNameMappingResult createNameMapping(RosterImportNameMappingInput input, String actorUserId) {
        boolean hasExisting = input.getEmployeeUserId() != null;
        boolean hasNew = input.getNewEmployeeName() != null && !input.getNewEmployeeName().isBlank();
        if (hasExisting == hasNew) {
            throw new BadRequestException("Exactly one of employeeUserId or newEmployeeName is required");
        }
        String rawName = normalizeName(input.getRawName());

        UserEntity employee;
        if (hasExisting) {
            employee = userRepository.findById(input.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        } else {
            // staffArea here is a fact about the person, set immediately - unlike the rest of an
            // EmployeePattern (workDaysPerWeek/weeklyDayOff), which one month of attendance can't
            // reliably establish and so is still never fabricated. See RosterImportNameEntry's own
            // suggestedStaffArea description - this is that same value, sent back by the caller.
            UserCreateInput createInput = new UserCreateInput(input.getNewEmployeeName().trim()).role(Role.WAITER);
            if (input.getStaffArea() != null) {
                createInput.staffArea(input.getStaffArea());
            }
            employee = userRepository.findById(userService.create(createInput).getId()).orElseThrow();
        }

        RosterImportNameMappingEntity mapping =
                nameMappingRepository.findByRawName(rawName).orElseGet(RosterImportNameMappingEntity::new);
        mapping.setRawName(rawName);
        mapping.setEmployeeUserId(employee.getId());
        mapping.setCreatedByUserId(actorUserId);
        nameMappingRepository.saveAndFlush(mapping);

        RosterImportNameMappingResult result = new RosterImportNameMappingResult(rawName, employee.getId(), employee.getName());
        return result;
    }

    @Transactional
    public RosterImportColorMappingResult createColorMapping(RosterImportColorMappingInput input, String actorUserId) {
        ShiftCodeEntity shiftCode =
                shiftCodeRepository.findById(input.getShiftCodeId()).orElseThrow(() -> new NotFoundException("Shift code not found"));
        boolean isSplit = shiftCode.getStartTime2() != null;
        boolean expectSplit = input.getFillColor() == FillColor.BLUE;
        if (isSplit != expectSplit) {
            throw new BadRequestException(
                    "Shift code \"" + shiftCode.getCode() + "\" has a " + (isSplit ? "split" : "single") + " schedule, but "
                            + (expectSplit ? "light blue" : "yellow") + " means a " + (expectSplit ? "split" : "single") + " one - this looks like "
                            + "the wrong code for this colour");
        }

        RosterImportShiftColorMappingEntity mapping = colorMappingRepository
                .findByRawCodeAndFillColor(input.getRawCode(), input.getFillColor())
                .orElseGet(RosterImportShiftColorMappingEntity::new);
        mapping.setRawCode(input.getRawCode());
        mapping.setFillColor(input.getFillColor());
        mapping.setResolvedCode(shiftCode.getCode());
        mapping.setCreatedByUserId(actorUserId);
        colorMappingRepository.saveAndFlush(mapping);

        return new RosterImportColorMappingResult(input.getRawCode(), input.getFillColor(), shiftCode.getCode());
    }

    @Transactional
    public RosterImportResult commit(String importId, String actorUserId) {
        StagedImport staged = stagedImports.get(importId);
        if (staged == null) {
            throw new NotFoundException("This import has expired or was never uploaded - upload the file again");
        }

        ParsedSchedule schedule = parseOrBadRequest(staged.fileBytes(), staged.year(), staged.month());
        Resolution resolution = resolve(schedule);
        if (!resolution.issues.isEmpty() || resolution.hasUnmappedNames() || resolution.hasUnresolvedColors()) {
            throw new BadRequestException(
                    "Cannot commit: " + resolution.issues.size() + " unreadable cell(s), " + resolution.unmappedNameCount()
                            + " unmapped name(s), " + resolution.unresolvedColorCount() + " unresolved \"9\" colour(s) - call preview again to see "
                            + "what's left");
        }

        int created = 0;
        for (ResolvedCell cell : resolution.resolvedCells) {
            if (resolution.existingEntryDates.contains(cell.employeeUserId() + "|" + cell.parsed().date())) {
                continue;
            }
            RosterEntryEntity entity = new RosterEntryEntity();
            entity.setEmployeeUserId(cell.employeeUserId());
            entity.setDate(cell.parsed().date());
            entity.setShiftCodeId(cell.shiftCode().getId());
            entity.setCreatedByUserId(actorUserId);
            rosterEntryRepository.save(entity);
            created++;
        }
        rosterEntryRepository.flush();

        int skipped = resolution.collisions.size();
        YearMonth ym = YearMonth.of(staged.year(), staged.month());
        auditLogService.record(
                AuditAction.ROSTER_MONTH_IMPORTED, AuditEntityType.ROSTER_ENTRY, null,
                "Imported " + ym + " schedule: " + created + " entr" + (created == 1 ? "y" : "ies") + " created, " + skipped
                        + " skipped (already scheduled)");

        return new RosterImportResult(staged.year(), staged.month(), created, skipped);
    }

    private ParsedSchedule parseOrBadRequest(byte[] bytes, int year, int month) {
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

    private record ResolvedCell(ParsedCell parsed, String employeeUserId, ShiftCodeEntity shiftCode) {
    }

    private final class Resolution {
        final List<ParseIssue> issues = new ArrayList<>();
        final List<ResolvedCell> resolvedCells = new ArrayList<>();
        final Map<String, NameStatus> namesByRawName = new LinkedHashMap<>();
        final Map<String, CodeStatus> codesByKey = new LinkedHashMap<>();
        final List<RosterImportCollision> collisions = new ArrayList<>();
        java.util.Set<String> existingEntryDates = java.util.Set.of();

        boolean hasUnmappedNames() {
            return namesByRawName.values().stream().anyMatch(n -> !n.mapped);
        }

        long unmappedNameCount() {
            return namesByRawName.values().stream().filter(n -> !n.mapped).count();
        }

        boolean hasUnresolvedColors() {
            return codesByKey.values().stream().anyMatch(c -> c.fillColor != null && !c.resolved);
        }

        long unresolvedColorCount() {
            return codesByKey.values().stream().filter(c -> c.fillColor != null && !c.resolved).count();
        }
    }

    private static final class NameStatus {
        long occurrences;
        boolean mapped;
        String employeeUserId;
        String employeeName;
        StaffArea suggestedStaffArea;
    }

    private static final class CodeStatus {
        String rawCode;
        FillColor fillColor;
        long occurrences;
        boolean resolved;
        String shiftCodeDescription;
    }

    private Resolution resolve(ParsedSchedule schedule) {
        Resolution resolution = new Resolution();
        resolution.issues.addAll(schedule.issues());

        Map<String, Optional<UserEntity>> nameLookupCache = new java.util.HashMap<>();
        Map<String, Optional<ShiftCodeEntity>> shiftCodeLookupCache = new java.util.HashMap<>();
        Map<String, Optional<RosterImportShiftColorMappingEntity>> colorLookupCache = new java.util.HashMap<>();

        for (ParsedCell cell : schedule.cells()) {
            NameStatus nameStatus =
                    resolution.namesByRawName.computeIfAbsent(cell.rawName(), n -> new NameStatus());
            nameStatus.occurrences++;
            if (nameStatus.suggestedStaffArea == null) {
                nameStatus.suggestedStaffArea = cell.staffArea();
            }
            if (nameStatus.employeeUserId == null) {
                UserEntity employee = nameLookupCache
                        .computeIfAbsent(cell.rawName(), n -> nameMappingRepository.findByRawName(n).flatMap(m -> userRepository.findById(m.getEmployeeUserId())))
                        .orElse(null);
                if (employee != null) {
                    nameStatus.mapped = true;
                    nameStatus.employeeUserId = employee.getId();
                    nameStatus.employeeName = employee.getName();
                }
            }

            // Grouped by text alone (plus colour for "9") - never by area. Which code text a
            // colour means is the same fact everywhere in the file; see this class's own javadoc
            // for why area belongs to the separate ShiftCode lookup below, not to this key.
            String codeKey = cell.rawCode() + "|" + cell.fillColor();
            CodeStatus codeStatus = resolution.codesByKey.computeIfAbsent(codeKey, k -> {
                CodeStatus s = new CodeStatus();
                s.rawCode = cell.rawCode();
                s.fillColor = cell.fillColor();
                return s;
            });
            codeStatus.occurrences++;

            String effectiveCode;
            if (cell.fillColor() != null) {
                String colorCacheKey = cell.rawCode() + "|" + cell.fillColor();
                RosterImportShiftColorMappingEntity mapping = colorLookupCache
                        .computeIfAbsent(colorCacheKey, k -> colorMappingRepository.findByRawCodeAndFillColor(cell.rawCode(), cell.fillColor()))
                        .orElse(null);
                if (mapping == null) {
                    continue; // awaiting POST /roster/import/color-mappings - not an issue, see this class's own javadoc
                }
                effectiveCode = mapping.getResolvedCode();
            } else {
                effectiveCode = cell.rawCode();
            }

            String shiftCacheKey = cell.staffArea() + "|" + effectiveCode;
            String effectiveCodeFinal = effectiveCode;
            ShiftCodeEntity shiftCode = shiftCodeLookupCache
                    .computeIfAbsent(shiftCacheKey, k -> shiftCodeService.resolveActive(cell.staffArea(), effectiveCodeFinal))
                    .orElse(null);
            if (shiftCode == null) {
                resolution.issues.add(new ParseIssue(
                        cell.cellRef(), "Unknown code \"" + effectiveCode + "\" for " + cell.staffArea().getValue() + " - no active shift code "
                                + "defined for it yet"));
                continue;
            }
            if (!codeStatus.resolved) {
                codeStatus.resolved = true;
                codeStatus.shiftCodeDescription = describeHours(shiftCode);
            }

            if (nameStatus.employeeUserId != null) {
                resolution.resolvedCells.add(new ResolvedCell(cell, nameStatus.employeeUserId, shiftCode));
            }
        }

        // Collisions: batched over the whole month, not one query per cell.
        if (!resolution.resolvedCells.isEmpty()) {
            LocalDate anyDate = resolution.resolvedCells.get(0).parsed().date();
            YearMonth ym = YearMonth.from(anyDate);
            List<RosterEntryEntity> existing = rosterEntryRepository.findByDateBetween(ym.atDay(1), ym.atEndOfMonth());
            Map<String, RosterEntryEntity> existingByKey =
                    existing.stream().collect(Collectors.toMap(e -> e.getEmployeeUserId() + "|" + e.getDate(), e -> e, (a, b) -> a));
            resolution.existingEntryDates = existingByKey.keySet();

            Map<String, ShiftCodeEntity> existingShiftCodes = shiftCodeRepository
                    .findAllById(existing.stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList()).stream()
                    .collect(Collectors.toMap(ShiftCodeEntity::getId, s -> s));
            Map<String, UserEntity> employeesById = userRepository
                    .findAllById(resolution.resolvedCells.stream().map(ResolvedCell::employeeUserId).distinct().toList()).stream()
                    .collect(Collectors.toMap(UserEntity::getId, u -> u));

            for (ResolvedCell cell : resolution.resolvedCells) {
                String key = cell.employeeUserId() + "|" + cell.parsed().date();
                RosterEntryEntity existingEntry = existingByKey.get(key);
                if (existingEntry != null) {
                    UserEntity employee = employeesById.get(cell.employeeUserId());
                    ShiftCodeEntity existingShiftCode = existingShiftCodes.get(existingEntry.getShiftCodeId());
                    resolution.collisions.add(new RosterImportCollision(
                            employee.getName(), cell.parsed().date().toString(),
                            existingShiftCode != null ? describeHours(existingShiftCode) : "(unknown)", describeHours(cell.shiftCode())));
                }
            }
        }

        return resolution;
    }

    private RosterImportPreview toPreview(String importId, int year, int month, Resolution resolution) {
        List<RosterImportNameEntry> names = resolution.namesByRawName.entrySet().stream()
                .map(e -> {
                    NameStatus s = e.getValue();
                    RosterImportNameEntry dto = new RosterImportNameEntry(e.getKey(), (int) s.occurrences, s.mapped);
                    if (s.employeeUserId != null) {
                        dto.setEmployeeUserId(org.openapitools.jackson.nullable.JsonNullable.of(s.employeeUserId));
                        dto.setEmployeeName(org.openapitools.jackson.nullable.JsonNullable.of(s.employeeName));
                    }
                    if (s.suggestedStaffArea != null) {
                        dto.setSuggestedStaffArea(org.openapitools.jackson.nullable.JsonNullable.of(s.suggestedStaffArea));
                    }
                    return dto;
                })
                .toList();

        List<RosterImportCodeEntry> codes = resolution.codesByKey.values().stream()
                .map(s -> {
                    RosterImportCodeEntry dto = new RosterImportCodeEntry(s.rawCode, (int) s.occurrences, s.resolved);
                    if (s.fillColor != null) {
                        dto.setFillColor(org.openapitools.jackson.nullable.JsonNullable.of(s.fillColor));
                    }
                    if (s.shiftCodeDescription != null) {
                        dto.setShiftCodeDescription(org.openapitools.jackson.nullable.JsonNullable.of(s.shiftCodeDescription));
                    }
                    return dto;
                })
                .toList();

        long entriesToCreate = resolution.resolvedCells.stream()
                .filter(c -> !resolution.existingEntryDates.contains(c.employeeUserId() + "|" + c.parsed().date()))
                .count();

        boolean canCommit = resolution.issues.isEmpty() && !resolution.hasUnmappedNames() && !resolution.hasUnresolvedColors();

        return new RosterImportPreview(
                importId, year, month, names, codes, resolution.issues.stream().map(i -> {
                    RosterImportIssue dto = new RosterImportIssue(i.message());
                    if (i.cellRef() != null) {
                        dto.setCellRef(org.openapitools.jackson.nullable.JsonNullable.of(i.cellRef()));
                    }
                    return dto;
                }).toList(),
                resolution.collisions, (int) entriesToCreate, canCommit);
    }

    private static String normalizeName(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }

    /** Informal, human-facing only - not used for any resolution logic. */
    private static String describeHours(ShiftCodeEntity shiftCode) {
        if (shiftCode.getStartTime1() == null) {
            return "No fixed hours";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(shiftCode.getStartTime1()).append('-').append(shiftCode.getEndTime1());
        if (shiftCode.getStartTime2() != null) {
            sb.append(" / ").append(shiftCode.getStartTime2()).append('-').append(shiftCode.getEndTime2());
        }
        return sb.toString();
    }
}
