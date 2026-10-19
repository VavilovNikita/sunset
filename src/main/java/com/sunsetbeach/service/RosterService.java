package com.sunsetbeach.service;

import com.sunsetbeach.entity.EmployeePatternEntity;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.RosterCopyInput;
import com.sunsetbeach.model.RosterCopyResult;
import com.sunsetbeach.model.RosterCoverageWarning;
import com.sunsetbeach.model.RosterEmployee;
import com.sunsetbeach.model.RosterEntry;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.RosterMonth;
import com.sunsetbeach.model.RosterMoveInput;
import com.sunsetbeach.model.RosterReassignInput;
import com.sunsetbeach.model.RosterSwapInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftChangeRequestRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.StaffAreaCoverageRuleRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The roster grid and its editing surface. A day off is the absence of a {@code RosterEntry},
 * never a row of its own - see that model's own description.
 */
@Service
public class RosterService {

    private final RosterEntryRepository rosterEntryRepository;
    private final EmployeePatternRepository employeePatternRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final StaffAreaCoverageRuleRepository staffAreaCoverageRuleRepository;
    private final UserRepository userRepository;
    private final AttendancePunchRepository attendancePunchRepository;
    private final AuditLogService auditLogService;
    private final RosterChangeTracker changeTracker;
    private final ShiftChangeRequestRepository shiftChangeRequestRepository;

    public RosterService(
            RosterEntryRepository rosterEntryRepository,
            EmployeePatternRepository employeePatternRepository,
            ShiftCodeRepository shiftCodeRepository,
            StaffAreaCoverageRuleRepository staffAreaCoverageRuleRepository,
            UserRepository userRepository,
            AttendancePunchRepository attendancePunchRepository,
            AuditLogService auditLogService,
            RosterChangeTracker changeTracker,
            ShiftChangeRequestRepository shiftChangeRequestRepository) {
        this.rosterEntryRepository = rosterEntryRepository;
        this.employeePatternRepository = employeePatternRepository;
        this.shiftCodeRepository = shiftCodeRepository;
        this.staffAreaCoverageRuleRepository = staffAreaCoverageRuleRepository;
        this.userRepository = userRepository;
        this.attendancePunchRepository = attendancePunchRepository;
        this.auditLogService = auditLogService;
        this.changeTracker = changeTracker;
        this.shiftChangeRequestRepository = shiftChangeRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<RosterEmployee> listEmployees() {
        // staffArea is read straight off User now - see that field's own openapi.yaml
        // description - not joined from EmployeePattern, which no longer carries its own copy.
        return userRepository.findAll().stream()
                .filter(UserEntity::isActive)
                .map(RosterService::toEmployeeDto)
                // name, not email: the roster's whole reason for existing is a grid with
                // someone to point at, and a no-login account (see UserCreateInput) has no
                // email to sort by at all.
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();
    }

    /**
     * The people on a month's grid: the active ones plus anyone deactivated who still has an entry
     * or a live punch in the month - the same set both exports list ({@code
     * RosterExportService#exportEmployeeIds}), so the screen and the files never disagree about
     * who worked. Those rows come back {@code active = false}; the screen shows them greyed out
     * and offers no way to assign them a shift ({@link #requireActiveEmployee} refuses it anyway).
     */
    private List<RosterEmployee> listEmployeesForMonth(List<RosterEntryEntity> monthEntries, YearMonth ym) {
        java.util.Set<String> extraIds = monthEntries.stream().map(RosterEntryEntity::getEmployeeUserId).collect(Collectors.toCollection(java.util.HashSet::new));
        attendancePunchRepository
                .findByPunchAtBetweenAndVoidedAtIsNullOrderByEmployeeUserIdAscPunchAtAsc(ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay())
                .forEach(p -> extraIds.add(p.getEmployeeUserId()));
        return userRepository.findAll().stream()
                .filter(u -> u.isActive() || extraIds.contains(u.getId()))
                .map(RosterService::toEmployeeDto)
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();
    }

    /**
     * A shift is never assigned to a deactivated employee, whichever door it comes through - the
     * manual gestures here, {@link #generateMonth}, and both Excel imports. Their past entries stay
     * (the history is still true) and can still be deleted; nothing new is written for them.
     */
    static void requireActiveEmployee(UserEntity employee) {
        if (!employee.isActive()) {
            throw new BadRequestException(employee.getName() + " is deactivated - a shift can't be assigned to them");
        }
    }

    /**
     * The one definition of "this entry counts toward its department's coverage": a worked shift code of an
     * active employee who has a department. The month grid's warnings and the shift-request coverage figures
     * both use it, so the two can never disagree about who is working.
     */
    public static boolean countsTowardCoverage(ShiftCodeEntity shiftCode, UserEntity employee) {
        return shiftCode != null && shiftCode.isCountsAsWorked() && employee != null && employee.isActive() && employee.getStaffArea() != null;
    }

    static RosterEmployee toEmployeeDto(UserEntity u) {
        RosterEmployee dto = new RosterEmployee(u.getId(), u.getName(), u.isActive());
        dto.setEmail(u.getEmail());
        if (u.getStaffArea() != null) {
            dto.staffArea(u.getStaffArea());
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public RosterMonth getMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<RosterEntryEntity> entities = rosterEntryRepository.findByDateBetween(ym.atDay(1), ym.atEndOfMonth());
        return buildMonth(year, month, entities);
    }

    @Transactional(readOnly = true)
    public List<RosterEntry> getMyRoster(String employeeUserId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<RosterEntryEntity> entities = rosterEntryRepository.findByEmployeeUserIdAndDateBetween(employeeUserId, ym.atDay(1), ym.atEndOfMonth());
        Map<String, UserEntity> employees = resolveUsers(entities.stream().map(RosterEntryEntity::getEmployeeUserId).distinct().toList());
        Map<String, ShiftCodeEntity> shiftCodes = resolveShiftCodes(entities);
        return entities.stream().map(e -> toDto(e, employees.get(e.getEmployeeUserId()), shiftCodes.get(e.getShiftCodeId()), null)).toList();
    }

    /**
     * Fills every blank date in the month for every employee who has a pattern - never
     * overwrites an existing entry, which is what makes this safe to run again (after adding a
     * new employee's pattern partway through setting a month up) without disturbing anything
     * already hand-edited.
     */
    @Transactional
    public RosterMonth generateMonth(int year, int month, String actorUserId) {
        YearMonth ym = YearMonth.of(year, month);
        List<EmployeePatternEntity> patterns = employeePatternRepository.findAll();
        List<RosterEntryEntity> existing = rosterEntryRepository.findByDateBetween(ym.atDay(1), ym.atEndOfMonth());
        java.util.Set<String> occupied =
                existing.stream().map(e -> e.getEmployeeUserId() + "|" + e.getDate()).collect(Collectors.toSet());

        Map<String, UserEntity> patternEmployees = resolveUsers(patterns.stream().map(EmployeePatternEntity::getEmployeeUserId).distinct().toList());
        int created = 0;
        for (EmployeePatternEntity pattern : patterns) {
            UserEntity patternEmployee = patternEmployees.get(pattern.getEmployeeUserId());
            if (pattern.getDefaultShiftCodeId() == null || patternEmployee == null || !patternEmployee.isActive()) {
                continue;
            }
            for (LocalDate date = ym.atDay(1); !date.isAfter(ym.atEndOfMonth()); date = date.plusDays(1)) {
                if (date.getDayOfWeek().name().equals(pattern.getWeeklyDayOff().name())) {
                    continue;
                }
                String key = pattern.getEmployeeUserId() + "|" + date;
                if (occupied.contains(key)) {
                    continue;
                }
                RosterEntryEntity entry = new RosterEntryEntity();
                entry.setEmployeeUserId(pattern.getEmployeeUserId());
                entry.setDate(date);
                entry.setShiftCodeId(pattern.getDefaultShiftCodeId());
                entry.setCreatedByUserId(actorUserId);
                rosterEntryRepository.save(entry);
                occupied.add(key);
                created++;
            }
        }
        rosterEntryRepository.flush();

        auditLogService.record(
                AuditAction.ROSTER_MONTH_GENERATED, AuditEntityType.ROSTER_ENTRY, null,
                "Generated " + ym + " roster from patterns: " + created + " entr" + (created == 1 ? "y" : "ies") + " created");

        return getMonth(year, month);
    }

    @Transactional
    public RosterEntry createEntry(RosterEntryCreateInput input, String actorUserId) {
        UserEntity employee = userRepository.findById(input.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        requireActiveEmployee(employee);
        ShiftCodeEntity shiftCode = shiftCodeRepository.findById(input.getShiftCodeId()).orElseThrow(() -> new NotFoundException("Shift code not found"));
        LocalDate date = LocalDate.parse(input.getDate());
        rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), date).ifPresent(e -> {
            throw new BadRequestException("This employee already has an entry on this date");
        });

        RosterEntryEntity entity = new RosterEntryEntity();
        entity.setEmployeeUserId(employee.getId());
        entity.setDate(date);
        entity.setShiftCodeId(shiftCode.getId());
        entity.setNote(input.getNote().orElse(null));
        entity.setCreatedByUserId(actorUserId);
        RosterEntryEntity saved = rosterEntryRepository.saveAndFlush(entity);
        changeTracker.record(saved.getEmployeeUserId(), saved.getDate(), null);

        auditLogService.record(
                AuditAction.ROSTER_ENTRY_CREATED, AuditEntityType.ROSTER_ENTRY, saved.getId(),
                "Scheduled " + employee.getName() + " for " + shiftCode.getCode() + " on " + date);

        return toDto(saved, employee, shiftCode, null);
    }

    @Transactional
    public void deleteEntry(String id) {
        RosterEntryEntity entity = rosterEntryRepository.findById(id).orElseThrow(() -> new NotFoundException("Entry not found"));
        if (entity.isLocked()) {
            throw new BadRequestException("This entry is locked");
        }
        changeTracker.record(entity.getEmployeeUserId(), entity.getDate(), entity.getShiftCodeId());
        rosterEntryRepository.delete(entity);
        auditLogService.record(AuditAction.ROSTER_ENTRY_DELETED, AuditEntityType.ROSTER_ENTRY, id, "Removed a roster entry");
    }

    /** The drag-to-another-date gesture - same employee, new date. */
    @Transactional
    public RosterEntry moveEntry(String id, RosterMoveInput input, String actorUserId) {
        RosterEntryEntity entity = rosterEntryRepository.findById(id).orElseThrow(() -> new NotFoundException("Entry not found"));
        if (entity.isLocked()) {
            throw new BadRequestException("This entry is locked");
        }
        LocalDate targetDate = LocalDate.parse(input.getDate());
        if (!targetDate.equals(entity.getDate())) {
            requireActiveEmployee(userRepository.findById(entity.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found")));
            rosterEntryRepository.findByEmployeeUserIdAndDate(entity.getEmployeeUserId(), targetDate).ifPresent(e -> {
                throw new BadRequestException("This employee already has an entry on the target date");
            });
        }
        LocalDate originalDate = entity.getDate();
        if (!targetDate.equals(originalDate)) {
            changeTracker.record(entity.getEmployeeUserId(), originalDate, entity.getShiftCodeId());
            changeTracker.record(entity.getEmployeeUserId(), targetDate, null);
        }
        entity.setDate(targetDate);
        RosterEntryEntity saved = rosterEntryRepository.saveAndFlush(entity);

        UserEntity employee = userRepository.findById(saved.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        ShiftCodeEntity shiftCode = shiftCodeRepository.findById(saved.getShiftCodeId()).orElseThrow(() -> new NotFoundException("Shift code not found"));
        auditLogService.record(
                AuditAction.ROSTER_ENTRY_MOVED, AuditEntityType.ROSTER_ENTRY, saved.getId(),
                "Moved " + employee.getName() + "'s " + shiftCode.getCode() + " shift from " + originalDate + " to " + targetDate);

        return toDto(saved, employee, shiftCode, null);
    }

    /**
     * The drag-onto-a-different-person's-empty-cell gesture. Crosses {@code StaffArea} freely -
     * covering another area for a day is a normal, wanted operation here (the source spreadsheet
     * itself notes a Restaurant employee covering Housekeeping for two days), unlike the
     * booking/spa swaps' same-type restriction, which exists only because of a frozen price or
     * duration that has no equivalent here.
     */
    @Transactional
    public RosterEntry reassignEntry(String id, RosterReassignInput input, String actorUserId) {
        RosterEntryEntity entity = rosterEntryRepository.findById(id).orElseThrow(() -> new NotFoundException("Entry not found"));
        if (entity.isLocked()) {
            throw new BadRequestException("This entry is locked");
        }
        UserEntity targetEmployee = userRepository.findById(input.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        if (!targetEmployee.getId().equals(entity.getEmployeeUserId())) {
            requireActiveEmployee(targetEmployee);
            rosterEntryRepository.findByEmployeeUserIdAndDate(targetEmployee.getId(), entity.getDate()).ifPresent(e -> {
                throw new BadRequestException("The target employee already has an entry on this date");
            });
        }

        UserEntity originalEmployee = userRepository.findById(entity.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        if (!targetEmployee.getId().equals(originalEmployee.getId())) {
            changeTracker.record(originalEmployee.getId(), entity.getDate(), entity.getShiftCodeId());
            changeTracker.record(targetEmployee.getId(), entity.getDate(), null);
        }
        entity.setEmployeeUserId(targetEmployee.getId());
        RosterEntryEntity saved = rosterEntryRepository.saveAndFlush(entity);

        ShiftCodeEntity shiftCode = shiftCodeRepository.findById(saved.getShiftCodeId()).orElseThrow(() -> new NotFoundException("Shift code not found"));
        auditLogService.record(
                AuditAction.ROSTER_ENTRY_REASSIGNED, AuditEntityType.ROSTER_ENTRY, saved.getId(),
                "Gave " + originalEmployee.getName() + "'s " + shiftCode.getCode() + " shift on " + saved.getDate() + " to "
                        + targetEmployee.getName());

        return toDto(saved, targetEmployee, shiftCode, null);
    }

    /**
     * The drag-onto-another-person's-occupied-cell gesture: this entry's {@code (date,
     * shiftCodeId)} trades with the other one's. Unlike the booking room swap or the spa table
     * swap, this needs no SERIALIZABLE write and no deferred constraint - a person's own
     * calendar dates aren't a resource a third party can be contending for the way a room unit
     * or a spa table is (nobody else is fighting to be scheduled on "Tuesday for employee A"),
     * so an ordinary two-row update inside one transaction is enough. Do not add SERIALIZABLE or
     * a deferred-constraint dance here "to match the other two swaps" - the resource shape that
     * required it there doesn't exist here, and adding it back would be solving a problem this
     * table doesn't have.
     */
    @Transactional
    public RosterEntry swapEntries(String id, RosterSwapInput input, String actorUserId) {
        if (id.equals(input.getOtherEntryId())) {
            throw new BadRequestException("Can't swap an entry with itself");
        }
        RosterEntryEntity entity = rosterEntryRepository.findById(id).orElseThrow(() -> new NotFoundException("Entry not found"));
        RosterEntryEntity other = rosterEntryRepository.findById(input.getOtherEntryId()).orElseThrow(() -> new NotFoundException("Other entry not found"));
        if (entity.isLocked() || other.isLocked()) {
            throw new BadRequestException("One of these entries is locked");
        }
        // Each person ends up with the other's date and code, so both have to be assignable.
        if (!entity.getEmployeeUserId().equals(other.getEmployeeUserId())) {
            requireActiveEmployee(userRepository.findById(entity.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found")));
            requireActiveEmployee(userRepository.findById(other.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found")));
        }
        if (rosterEntryRepository.findByEmployeeUserIdAndDate(entity.getEmployeeUserId(), other.getDate()).filter(e -> !e.getId().equals(entity.getId())).isPresent()
                || rosterEntryRepository.findByEmployeeUserIdAndDate(other.getEmployeeUserId(), entity.getDate()).filter(e -> !e.getId().equals(other.getId())).isPresent()) {
            throw new BadRequestException("One of these employees already has an entry on the other's date");
        }

        // Four employee-days change hands; each remembers what it was. When both entries are one person's, the
        // two dates simply trade codes.
        boolean samePerson = entity.getEmployeeUserId().equals(other.getEmployeeUserId());
        changeTracker.record(entity.getEmployeeUserId(), entity.getDate(), entity.getShiftCodeId());
        changeTracker.record(entity.getEmployeeUserId(), other.getDate(), samePerson ? other.getShiftCodeId() : null);
        changeTracker.record(other.getEmployeeUserId(), other.getDate(), other.getShiftCodeId());
        changeTracker.record(other.getEmployeeUserId(), entity.getDate(), samePerson ? entity.getShiftCodeId() : null);

        LocalDate entityOriginalDate = entity.getDate();
        String entityOriginalShiftCodeId = entity.getShiftCodeId();
        String entityOriginalNote = entity.getNote();
        LocalDate otherOriginalDate = other.getDate();
        String otherOriginalShiftCodeId = other.getShiftCodeId();
        String otherOriginalNote = other.getNote();

        entity.setDate(otherOriginalDate);
        entity.setShiftCodeId(otherOriginalShiftCodeId);
        entity.setNote(otherOriginalNote);
        rosterEntryRepository.saveAndFlush(entity);

        other.setDate(entityOriginalDate);
        other.setShiftCodeId(entityOriginalShiftCodeId);
        other.setNote(entityOriginalNote);
        rosterEntryRepository.saveAndFlush(other);

        UserEntity entityEmployee = userRepository.findById(entity.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        UserEntity otherEmployee = userRepository.findById(other.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        auditLogService.record(
                AuditAction.ROSTER_ENTRIES_SWAPPED, AuditEntityType.ROSTER_ENTRY, entity.getId(),
                "Swapped days with " + otherEmployee.getName() + ": now " + entity.getDate());
        auditLogService.record(
                AuditAction.ROSTER_ENTRIES_SWAPPED, AuditEntityType.ROSTER_ENTRY, other.getId(),
                "Swapped days with " + entityEmployee.getName() + ": now " + other.getDate());

        ShiftCodeEntity shiftCode = shiftCodeRepository.findById(entity.getShiftCodeId()).orElseThrow(() -> new NotFoundException("Shift code not found"));
        return toDto(entity, entityEmployee, shiftCode, null);
    }

    @Transactional
    public RosterEntry setLocked(String id, boolean locked) {
        RosterEntryEntity entity = rosterEntryRepository.findById(id).orElseThrow(() -> new NotFoundException("Entry not found"));
        entity.setLocked(locked);
        RosterEntryEntity saved = rosterEntryRepository.saveAndFlush(entity);

        UserEntity employee = userRepository.findById(saved.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        ShiftCodeEntity shiftCode = shiftCodeRepository.findById(saved.getShiftCodeId()).orElseThrow(() -> new NotFoundException("Shift code not found"));
        auditLogService.record(
                AuditAction.ROSTER_ENTRY_LOCKED_CHANGED, AuditEntityType.ROSTER_ENTRY, saved.getId(),
                (locked ? "Locked" : "Unlocked") + " " + employee.getName() + "'s " + shiftCode.getCode() + " shift on " + saved.getDate());

        return toDto(saved, employee, shiftCode, null);
    }

    /**
     * {@code POST /roster/copy} - see that operation's description for the rules. The working
     * path for a month nobody has patterns for: {@link #generateMonth} needs an
     * {@code EmployeePattern} per person, this only needs a roster that was already worked.
     *
     * <p>Copied by weekday: the source is cut to whole weeks ({@code period}) and each target date
     * reads {@code sourceFrom + ((target - sourceFrom) mod period)}, which always lands on the same
     * weekday. Copying by day of the month instead would move everyone's weekly day off to a
     * different weekday every month.
     */
    @Transactional
    public RosterCopyResult copyMonth(RosterCopyInput input, String actorUserId) {
        YearMonth target = YearMonth.of(input.getYear(), input.getMonth());
        LocalDate targetStart = target.atDay(1);
        LocalDate targetEnd = target.atEndOfMonth();

        LocalDate sourceFrom;
        LocalDate sourceTo;
        if (input.getSourceFrom() == null && input.getSourceTo() == null) {
            YearMonth previous = target.minusMonths(1);
            sourceFrom = previous.atDay(1);
            sourceTo = previous.atEndOfMonth();
        } else if (input.getSourceFrom() == null || input.getSourceTo() == null) {
            throw new BadRequestException("Give both sourceFrom and sourceTo, or neither");
        } else {
            sourceFrom = LocalDate.parse(input.getSourceFrom());
            sourceTo = LocalDate.parse(input.getSourceTo());
        }
        long sourceDays = ChronoUnit.DAYS.between(sourceFrom, sourceTo) + 1;
        if (sourceDays < 7) {
            throw new BadRequestException("The source range must cover at least 7 days, so every weekday has something to copy from");
        }
        if (!sourceFrom.isAfter(targetEnd) && !sourceTo.isBefore(targetStart)) {
            throw new BadRequestException("The source range can't overlap the month being filled");
        }
        int period = (int) (sourceDays / 7) * 7;
        LocalDate usedSourceTo = sourceFrom.plusDays(period - 1L);

        Map<String, UserEntity> activeEmployees = userRepository.findAll().stream()
                .filter(UserEntity::isActive)
                .collect(Collectors.toMap(UserEntity::getId, u -> u));
        Map<String, Map<LocalDate, RosterEntryEntity>> sourceByEmployee = rosterEntryRepository.findByDateBetween(sourceFrom, usedSourceTo).stream()
                .filter(e -> activeEmployees.containsKey(e.getEmployeeUserId()))
                .collect(Collectors.groupingBy(RosterEntryEntity::getEmployeeUserId, Collectors.toMap(RosterEntryEntity::getDate, e -> e, (a, b) -> a)));
        java.util.Set<String> occupied = rosterEntryRepository.findByDateBetween(targetStart, targetEnd).stream()
                .map(e -> e.getEmployeeUserId() + "|" + e.getDate())
                .collect(Collectors.toSet());

        // Every version of every code, so a copied entry can be re-pointed to the version in force
        // on its new date (ShiftCodeService#createVersion) rather than keep last month's terms.
        Map<String, ShiftCodeEntity> codesById = shiftCodeRepository.findAll().stream().collect(Collectors.toMap(ShiftCodeEntity::getId, c -> c));
        Map<String, List<ShiftCodeEntity>> versionsByKey =
                codesById.values().stream().collect(Collectors.groupingBy(RosterService::versionKey));

        int created = 0;
        int skippedExisting = 0;
        int skippedNoCurrentCode = 0;
        for (Map.Entry<String, Map<LocalDate, RosterEntryEntity>> employee : sourceByEmployee.entrySet()) {
            for (LocalDate date = targetStart; !date.isAfter(targetEnd); date = date.plusDays(1)) {
                LocalDate sourceDate = sourceFrom.plusDays(Math.floorMod(ChronoUnit.DAYS.between(sourceFrom, date), period));
                RosterEntryEntity source = employee.getValue().get(sourceDate);
                if (source == null) {
                    continue; // a day off stays a day off
                }
                if (occupied.contains(employee.getKey() + "|" + date)) {
                    skippedExisting++;
                    continue;
                }
                ShiftCodeEntity sourceCode = codesById.get(source.getShiftCodeId());
                ShiftCodeEntity code = sourceCode == null ? null : versionInForce(versionsByKey.get(versionKey(sourceCode)), date);
                if (code == null) {
                    skippedNoCurrentCode++;
                    continue;
                }
                RosterEntryEntity entry = new RosterEntryEntity();
                entry.setEmployeeUserId(employee.getKey());
                entry.setDate(date);
                entry.setShiftCodeId(code.getId());
                entry.setCreatedByUserId(actorUserId);
                rosterEntryRepository.save(entry);
                occupied.add(employee.getKey() + "|" + date);
                created++;
            }
        }
        rosterEntryRepository.flush();

        auditLogService.record(
                AuditAction.ROSTER_MONTH_COPIED, AuditEntityType.ROSTER_ENTRY, null,
                "Copied " + sourceFrom + " to " + usedSourceTo + " into the " + target + " roster: " + created + " entr" + (created == 1 ? "y" : "ies")
                        + " created, " + skippedExisting + " existing left alone, " + skippedNoCurrentCode + " without a current code skipped");

        return new RosterCopyResult(
                getMonth(target.getYear(), target.getMonthValue()), sourceFrom.toString(), usedSourceTo.toString(), created, skippedExisting, skippedNoCurrentCode);
    }

    private static String versionKey(ShiftCodeEntity code) {
        return (code.getStaffArea() == null ? "" : code.getStaffArea().getValue()) + "|" + code.getCode();
    }

    /** The version starting latest on or before {@code date}, or {@code null} if every version starts after it. */
    private static ShiftCodeEntity versionInForce(List<ShiftCodeEntity> versions, LocalDate date) {
        return versions.stream()
                .filter(v -> !v.getEffectiveFrom().isAfter(date))
                .max(java.util.Comparator.comparing(ShiftCodeEntity::getEffectiveFrom))
                .orElse(null);
    }

    private RosterMonth buildMonth(int year, int month, List<RosterEntryEntity> entities) {
        Map<String, UserEntity> employees = resolveUsers(entities.stream().map(RosterEntryEntity::getEmployeeUserId).distinct().toList());
        Map<String, ShiftCodeEntity> shiftCodes = resolveShiftCodes(entities);

        // At most one waiting shift request per entry (a partial unique index); the grid shows a marker on it.
        Map<String, com.sunsetbeach.entity.ShiftChangeRequestEntity> pendingByEntry = entities.isEmpty()
                ? Map.of()
                : shiftChangeRequestRepository.findByRosterEntryIdInAndStatus(entities.stream().map(RosterEntryEntity::getId).toList(), "PENDING").stream()
                        .collect(Collectors.toMap(com.sunsetbeach.entity.ShiftChangeRequestEntity::getRosterEntryId, r -> r, (a, b) -> a));
        List<RosterEntry> entryDtos = entities.stream().map(e -> {
            RosterEntry dto = toDto(e, employees.get(e.getEmployeeUserId()), shiftCodes.get(e.getShiftCodeId()), null);
            com.sunsetbeach.entity.ShiftChangeRequestEntity pending = pendingByEntry.get(e.getId());
            if (pending != null) {
                dto.pendingShiftRequestId(pending.getId()).pendingShiftRequestType(com.sunsetbeach.model.ShiftChangeRequestType.fromValue(pending.getType()));
            }
            return dto;
        }).toList();
        List<RosterEmployee> employeeDtos = listEmployeesForMonth(entities, YearMonth.of(year, month));

        // Coverage: a countsAsWorked entry counts toward the area of the *employee* it belongs to
        // (User.staffArea), the same department the grid groups that person's row under - and,
        // like the grid's own Working total, only for active employees. It used to read the area
        // off the shift code instead, which a shared code (OP, PH, 7, 12, ... - staffArea null,
        // see ShiftCode's own description) doesn't have: every entry using one was excluded from
        // every area, so a roster built on shared codes showed 0/N coverage on every day. A code's
        // area only says where that code is defined; the person is who's actually working where.
        Map<String, Integer> workingCountByAreaDate = new HashMap<>();
        for (RosterEntryEntity e : entities) {
            ShiftCodeEntity shiftCode = shiftCodes.get(e.getShiftCodeId());
            UserEntity employee = employees.get(e.getEmployeeUserId());
            if (countsTowardCoverage(shiftCode, employee)) {
                String key = employee.getStaffArea().name() + "|" + e.getDate();
                workingCountByAreaDate.merge(key, 1, Integer::sum);
            }
        }
        Map<StaffArea, Integer> minimumByArea = staffAreaCoverageRuleRepository.findAll().stream()
                .collect(Collectors.toMap(r -> r.getStaffArea(), r -> r.getMinimumWorking()));

        List<RosterCoverageWarning> warnings = new ArrayList<>();
        YearMonth ym = YearMonth.of(year, month);
        for (Map.Entry<StaffArea, Integer> ruleEntry : minimumByArea.entrySet()) {
            StaffArea area = ruleEntry.getKey();
            int minimum = ruleEntry.getValue();
            for (LocalDate date = ym.atDay(1); !date.isAfter(ym.atEndOfMonth()); date = date.plusDays(1)) {
                int working = workingCountByAreaDate.getOrDefault(area.name() + "|" + date, 0);
                if (working < minimum) {
                    warnings.add(new RosterCoverageWarning(area, date.toString(), working, minimum));
                }
            }
        }

        return new RosterMonth(year, month, entryDtos, employeeDtos, warnings);
    }

    private Map<String, UserEntity> resolveUsers(List<String> userIds) {
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, u -> u));
    }

    private Map<String, ShiftCodeEntity> resolveShiftCodes(List<RosterEntryEntity> entities) {
        List<String> ids = entities.stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList();
        return shiftCodeRepository.findAllById(ids).stream().collect(Collectors.toMap(ShiftCodeEntity::getId, s -> s));
    }

    private RosterEntry toDto(RosterEntryEntity e, UserEntity employee, ShiftCodeEntity shiftCode, String shiftCodeCreatedByEmail) {
        RosterEntry dto = new RosterEntry(
                e.getId(), e.getEmployeeUserId(), employee.getName(), e.getDate().toString(), ShiftCodeService.toDto(shiftCode, shiftCodeCreatedByEmail),
                e.isLocked(), com.sunsetbeach.mapper.TimestampFormat.toUtc(e.getCreatedAt()), com.sunsetbeach.mapper.TimestampFormat.toUtc(e.getUpdatedAt()));
        dto.setEmployeeEmail(employee.getEmail());
        if (e.getNote() != null) {
            dto.note(e.getNote());
        }
        return dto;
    }
}
