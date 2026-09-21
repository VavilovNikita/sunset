package com.sunsetbeach.service;

import com.sunsetbeach.entity.RosterImportShiftColorMappingEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.RosterImportShiftColorMappingRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shift codes are data, not an enum - each one carries its own optional area, its own
 * interval(s), and whether it counts as worked and/or is paid, matching how the source
 * spreadsheet's legend behaves (a code's own hours drifting between one month's legend and the
 * next, and - the "9" case - the same code letter occasionally covering two genuinely different
 * shifts, told apart on paper by cell fill colour rather than by department; see
 * {@code ShiftCode}'s own openapi.yaml description). See V57__staff_area_and_shift_code.sql for
 * why a row here is never updated once any {@code RosterEntry} references it - "editing" hours is
 * always {@link #create} again, which retires the previous version for new use without touching
 * what already happened under it.
 */
@Service
public class ShiftCodeService {

    // Representative swatches, not the literal Excel value: YELLOW is exact (this workbook's own
    // pure-ARGB "Yellow", FFFFFF00 - see ScheduleWorkbookParser's own YELLOW_ARGB constant), but
    // BLUE is a theme colour ("Accent 4, Lighter 80%") whose actual RGB depends on the workbook's
    // own theme1.xml, which is never persisted anywhere - only this coarse YELLOW/BLUE
    // classification is (RosterImportShiftColorMapping.fillColor). Good enough for a suggestion
    // the admin can freely override (see computeSuggestedColorHex) - not a claim of exactness.
    private static final Map<FillColor, String> FILL_COLOR_HEX = Map.of(FillColor.YELLOW, "#FFFF00", FillColor.BLUE, "#ADD8E6");

    private final ShiftCodeRepository shiftCodeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final RosterImportShiftColorMappingRepository colorMappingRepository;

    public ShiftCodeService(
            ShiftCodeRepository shiftCodeRepository,
            UserRepository userRepository,
            AuditLogService auditLogService,
            RosterImportShiftColorMappingRepository colorMappingRepository) {
        this.shiftCodeRepository = shiftCodeRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.colorMappingRepository = colorMappingRepository;
    }

    /**
     * With {@code staffArea} given, this is a *resolved* view for that one area - every
     * area-scoped code for it, plus every shared code whose {@code code} string isn't also
     * defined for this area (the area-scoped version wins and the shared one is left out, never
     * both - see {@code ShiftCode}'s own openapi.yaml description). Without {@code staffArea},
     * every active row is returned exactly as stored, area-scoped and shared alike, for the
     * shift-code management screen itself, where seeing both rows of a collision is the point.
     */
    @Transactional(readOnly = true)
    public List<ShiftCode> list(StaffArea staffArea) {
        List<ShiftCodeEntity> entities = staffArea != null ? resolveForArea(staffArea) : shiftCodeRepository.findByActiveTrue();
        Map<String, String> emailsById = resolveCreatorEmails(entities);
        return entities.stream().map(e -> toDtoForApi(e, emailsById.get(e.getCreatedByUserId()))).toList();
    }

    private List<ShiftCodeEntity> resolveForArea(StaffArea staffArea) {
        List<ShiftCodeEntity> areaScoped = shiftCodeRepository.findByStaffAreaAndActiveTrue(staffArea);
        List<ShiftCodeEntity> shared = shiftCodeRepository.findByStaffAreaIsNullAndActiveTrue();
        Set<String> areaScopedCodes = areaScoped.stream().map(ShiftCodeEntity::getCode).collect(Collectors.toSet());
        return java.util.stream.Stream.concat(areaScoped.stream(), shared.stream().filter(s -> !areaScopedCodes.contains(s.getCode()))).toList();
    }

    /**
     * The single-code version of {@link #resolveForArea}'s own precedence (area-scoped wins over
     * shared) - what {@code RosterImportService} resolves every code (including a "9" already
     * substituted via its own colour mapping) against, the same way any other write path would.
     */
    java.util.Optional<ShiftCodeEntity> resolveActive(StaffArea staffArea, String code) {
        return shiftCodeRepository.findByStaffAreaAndCodeAndActiveTrue(staffArea, code)
                .or(() -> shiftCodeRepository.findByStaffAreaAndCodeAndActiveTrue(null, code));
    }

    @Transactional
    public ShiftCode create(ShiftCodeCreateInput input, String actorUserId) {
        LocalTime start1 = parseTime(input.getStartTime1().orElse(null));
        LocalTime end1 = parseTime(input.getEndTime1().orElse(null));
        LocalTime start2 = parseTime(input.getStartTime2().orElse(null));
        LocalTime end2 = parseTime(input.getEndTime2().orElse(null));
        validateIntervals(start1, end1, start2, end2);
        validateKindShape(input.getKind(), start1, start2, input.getCountsAsWorked());

        // Retires the current version of this exact (staffArea, code) pair, if one exists - the
        // new row takes over for anything created from here on, every RosterEntry already
        // pointing at the old one keeps meaning what it meant.
        shiftCodeRepository.findByStaffAreaAndCodeAndActiveTrue(input.getStaffArea(), input.getCode()).ifPresent(existing -> {
            existing.setActive(false);
            shiftCodeRepository.save(existing);
        });

        ShiftCodeEntity entity = new ShiftCodeEntity();
        entity.setStaffArea(input.getStaffArea());
        entity.setCode(input.getCode());
        entity.setKind(input.getKind());
        entity.setStartTime1(start1);
        entity.setEndTime1(end1);
        entity.setStartTime2(start2);
        entity.setEndTime2(end2);
        entity.setCountsAsWorked(input.getCountsAsWorked());
        entity.setPaid(input.getIsPaid());
        entity.setEffectiveFrom(LocalDate.parse(input.getEffectiveFrom()));
        entity.setCreatedByUserId(actorUserId);

        ShiftCodeEntity saved = shiftCodeRepository.saveAndFlush(entity);

        String creatorEmail = userRepository.findById(actorUserId).map(UserEntity::getEmail).orElse(null);
        String areaDescription = saved.getStaffArea() != null ? saved.getStaffArea().getValue() : "shared (every area)";
        auditLogService.record(
                AuditAction.SHIFT_CODE_CREATED,
                AuditEntityType.SHIFT_CODE,
                saved.getId(),
                "Defined " + areaDescription + " code \"" + saved.getCode() + "\" as " + saved.getKind().getValue()
                        + ", effective " + saved.getEffectiveFrom());

        return toDtoForApi(saved, creatorEmail);
    }

    /**
     * {@code PATCH /shift-codes/{id}/kind} - the one deliberate exception to a {@code ShiftCode}
     * row never being edited (see this class's own javadoc): {@code kind} classifies what a code
     * already is, not an agreed term a {@code RosterEntry} depends on staying frozen, so
     * correcting it later doesn't reinterpret what anyone actually worked or was paid. Exists
     * mainly to confirm {@link #computeSuggestedKind}'s guess for a code that predates this field.
     */
    @Transactional
    public ShiftCode updateKind(String id, ShiftCodeKind kind, String actorUserId) {
        ShiftCodeEntity entity = shiftCodeRepository.findById(id).orElseThrow(() -> new NotFoundException("Shift code not found"));
        validateKindShape(kind, entity.getStartTime1(), entity.getStartTime2(), entity.isCountsAsWorked());
        entity.setKind(kind);
        ShiftCodeEntity saved = shiftCodeRepository.saveAndFlush(entity);

        auditLogService.record(
                AuditAction.SHIFT_CODE_KIND_CHANGED,
                AuditEntityType.SHIFT_CODE,
                saved.getId(),
                "Kind for code \"" + saved.getCode() + "\" set to " + saved.getKind().getValue());

        String creatorEmail = userRepository.findById(saved.getCreatedByUserId()).map(UserEntity::getEmail).orElse(null);
        return toDtoForApi(saved, creatorEmail);
    }

    /**
     * {@code PATCH /shift-codes/{id}/display-color} - the second deliberate exception to a {@code
     * ShiftCode} row never being edited (see this class's own javadoc and {@link #updateKind}):
     * {@code displayColor} is presentation only, not an agreed term. Unlike {@code kind}, this may
     * also clear the row back to unset ({@code displayColor} null) - there is no "unconfirmed"
     * state to protect against reverting, so the caller is free to change their mind.
     */
    @Transactional
    public ShiftCode updateDisplayColor(String id, String displayColor, String actorUserId) {
        ShiftCodeEntity entity = shiftCodeRepository.findById(id).orElseThrow(() -> new NotFoundException("Shift code not found"));
        entity.setDisplayColor(displayColor);
        ShiftCodeEntity saved = shiftCodeRepository.saveAndFlush(entity);

        auditLogService.record(
                AuditAction.SHIFT_CODE_DISPLAY_COLOR_CHANGED,
                AuditEntityType.SHIFT_CODE,
                saved.getId(),
                "Display color for code \"" + saved.getCode() + "\" set to " + (displayColor != null ? displayColor : "unset"));

        String creatorEmail = userRepository.findById(saved.getCreatedByUserId()).map(UserEntity::getEmail).orElse(null);
        return toDtoForApi(saved, creatorEmail);
    }

    private static LocalTime parseTime(String value) {
        return value != null ? LocalTime.parse(value) : null;
    }

    private static void validateIntervals(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if ((start1 == null) != (end1 == null)) {
            throw new BadRequestException("startTime1 and endTime1 must both be set or both be null");
        }
        if ((start2 == null) != (end2 == null)) {
            throw new BadRequestException("startTime2 and endTime2 must both be set or both be null");
        }
        if (start2 != null && start1 == null) {
            throw new BadRequestException("A second interval needs a first one");
        }
        if (start1 != null && !end1.isAfter(start1)) {
            throw new BadRequestException("endTime1 must be after startTime1");
        }
        if (start2 != null && !end2.isAfter(start2)) {
            throw new BadRequestException("endTime2 must be after startTime2");
        }
    }

    /**
     * {@code kind} must match the code's own interval shape - it names what the shape already
     * says, so a mismatch is a data-entry mistake, not a legitimate choice. {@code MORNING}/
     * {@code EVENING} are otherwise interchangeable here (both mean "exactly one interval") - the
     * distinction between the two is a human judgment this method doesn't referee.
     */
    private static void validateKindShape(ShiftCodeKind kind, LocalTime start1, LocalTime start2, boolean countsAsWorked) {
        boolean isSplit = start2 != null;
        boolean isSingleInterval = start1 != null && !isSplit;
        boolean isZeroInterval = start1 == null;
        switch (kind) {
            case SPLIT -> {
                if (!isSplit) throw new BadRequestException("SPLIT needs two intervals - this code doesn't have two");
            }
            case MORNING, EVENING -> {
                if (!isSingleInterval) throw new BadRequestException(kind.getValue() + " needs exactly one interval");
            }
            case OPEN_SCHEDULE -> {
                if (!isZeroInterval) throw new BadRequestException("OPEN_SCHEDULE needs no fixed interval");
                if (!countsAsWorked) throw new BadRequestException("OPEN_SCHEDULE must count as worked");
            }
            case ABSENCE -> {
                if (!isZeroInterval) throw new BadRequestException("ABSENCE needs no fixed interval");
                if (countsAsWorked) throw new BadRequestException("ABSENCE must not count as worked");
            }
        }
    }

    /**
     * A default guessed from the code's own shape, for {@code PATCH /shift-codes/{id}/kind} to
     * offer - never applied on its own (see {@code ShiftCode.suggestedKind}'s own openapi.yaml
     * description). Mirrors {@link #validateKindShape}'s own shape rules exactly, so a confirmed
     * suggestion always passes that same validation. The MORNING/EVENING split for a single
     * interval is a guess (before noon reads as morning) - the one part of this a person actually
     * has to look at and confirm, not just accept.
     */
    private static ShiftCodeKind computeSuggestedKind(ShiftCodeEntity e) {
        if (e.getStartTime2() != null) return ShiftCodeKind.SPLIT;
        if (e.getStartTime1() != null) return e.getStartTime1().isBefore(LocalTime.NOON) ? ShiftCodeKind.MORNING : ShiftCodeKind.EVENING;
        return e.isCountsAsWorked() ? ShiftCodeKind.OPEN_SCHEDULE : ShiftCodeKind.ABSENCE;
    }

    /**
     * A default guessed for `"9"`/`"9S"` specifically, for {@code PATCH
     * /shift-codes/{id}/display-color} to offer - never applied on its own (see {@code
     * ShiftCode.suggestedColor}'s own openapi.yaml description). Every other code has nothing to
     * guess from: nothing else in the source spreadsheet was ever told apart by cell colour.
     */
    private String computeSuggestedColorHex(ShiftCodeEntity e) {
        if (!("9".equals(e.getCode()) || "9S".equals(e.getCode()))) {
            return null;
        }
        return colorMappingRepository.findByResolvedCode(e.getCode()).stream()
                .max(Comparator.comparing(RosterImportShiftColorMappingEntity::getCreatedAt))
                .map(m -> FILL_COLOR_HEX.get(m.getFillColor()))
                .orElse(null);
    }

    /**
     * {@link #toDto} plus {@code suggestedColor} - kept out of the shared static method since
     * computing it needs a repository lookup, and every other caller of {@code toDto}
     * (RosterService, AttendanceService) renders many {@code ShiftCode}s at once without wanting
     * one extra query per row for a suggestion only this service's own endpoints surface.
     */
    private ShiftCode toDtoForApi(ShiftCodeEntity e, String createdByEmail) {
        ShiftCode dto = toDto(e, createdByEmail);
        if (e.getDisplayColor() == null) {
            String suggested = computeSuggestedColorHex(e);
            if (suggested != null) {
                dto.suggestedColor(suggested);
            }
        }
        return dto;
    }

    private Map<String, String> resolveCreatorEmails(List<ShiftCodeEntity> entities) {
        List<String> ids = entities.stream().map(ShiftCodeEntity::getCreatedByUserId).distinct().toList();
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
    }

    /** Package-private so RosterService/EmployeePatternService can render a ShiftCode already loaded, without a second query. */
    static ShiftCode toDto(ShiftCodeEntity e, String createdByEmail) {
        ShiftCode dto = new ShiftCode(
                e.getId(), e.getCode(), e.isCountsAsWorked(), e.isPaid(), e.getEffectiveFrom().toString(), e.isActive(),
                createdByEmail, com.sunsetbeach.mapper.TimestampFormat.toUtc(e.getCreatedAt()));
        // Left undefined (not "present but null") when shared - same convention as the interval
        // fields below: JsonNullable.of(null) would mean "explicitly null" (isPresent() true),
        // which is a different JSON shape than omitting the key entirely.
        if (e.getStaffArea() != null) dto.staffArea(e.getStaffArea());
        if (e.getStartTime1() != null) dto.setStartTime1(org.openapitools.jackson.nullable.JsonNullable.of(e.getStartTime1().toString().substring(0, 5)));
        if (e.getEndTime1() != null) dto.setEndTime1(org.openapitools.jackson.nullable.JsonNullable.of(e.getEndTime1().toString().substring(0, 5)));
        if (e.getStartTime2() != null) dto.setStartTime2(org.openapitools.jackson.nullable.JsonNullable.of(e.getStartTime2().toString().substring(0, 5)));
        if (e.getEndTime2() != null) dto.setEndTime2(org.openapitools.jackson.nullable.JsonNullable.of(e.getEndTime2().toString().substring(0, 5)));
        if (e.getKind() != null) {
            dto.kind(e.getKind());
        } else {
            dto.suggestedKind(computeSuggestedKind(e));
        }
        if (e.getDisplayColor() != null) dto.displayColor(e.getDisplayColor());
        return dto;
    }
}
