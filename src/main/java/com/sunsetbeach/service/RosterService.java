package com.sunsetbeach.service;

import com.sunsetbeach.entity.EmployeePatternEntity;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.RosterEmployee;
import com.sunsetbeach.model.RosterEntry;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.RosterMonth;
import com.sunsetbeach.model.RosterMoveInput;
import com.sunsetbeach.model.RosterReassignInput;
import com.sunsetbeach.model.RosterSwapInput;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The roster grid and its editing surface. A day off is the absence of a {@code RosterEntry},
 * never a row of its own - see that model's own description.
 *
 * <p>Coverage warnings are always empty for now - {@link #buildMonth} returns {@code List.of()}
 * until the coverage-rule commit adds the real computation on top of this one, the same
 * "one working slice at a time" staging the shift-code/pattern commit before this one used.
 */
@Service
public class RosterService {

    private final RosterEntryRepository rosterEntryRepository;
    private final EmployeePatternRepository employeePatternRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public RosterService(
            RosterEntryRepository rosterEntryRepository,
            EmployeePatternRepository employeePatternRepository,
            ShiftCodeRepository shiftCodeRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.rosterEntryRepository = rosterEntryRepository;
        this.employeePatternRepository = employeePatternRepository;
        this.shiftCodeRepository = shiftCodeRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<RosterEmployee> listEmployees() {
        Map<String, EmployeePatternEntity> patternsByEmployeeId =
                employeePatternRepository.findAll().stream().collect(Collectors.toMap(EmployeePatternEntity::getEmployeeUserId, p -> p));
        return userRepository.findAll().stream()
                .filter(UserEntity::isActive)
                .map(u -> {
                    RosterEmployee dto = new RosterEmployee(u.getId(), u.getEmail(), u.isActive());
                    EmployeePatternEntity pattern = patternsByEmployeeId.get(u.getId());
                    if (pattern != null) {
                        dto.staffArea(pattern.getStaffArea());
                    }
                    return dto;
                })
                .sorted((a, b) -> a.getEmail().compareTo(b.getEmail()))
                .toList();
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
        Map<String, String> emails = resolveEmails(entities.stream().map(RosterEntryEntity::getEmployeeUserId).distinct().toList());
        Map<String, ShiftCodeEntity> shiftCodes = resolveShiftCodes(entities);
        return entities.stream().map(e -> toDto(e, emails.get(e.getEmployeeUserId()), shiftCodes.get(e.getShiftCodeId()), null)).toList();
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

        int created = 0;
        for (EmployeePatternEntity pattern : patterns) {
            if (pattern.getDefaultShiftCodeId() == null) {
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

        auditLogService.record(
                AuditAction.ROSTER_ENTRY_CREATED, AuditEntityType.ROSTER_ENTRY, saved.getId(),
                "Scheduled " + employee.getEmail() + " for " + shiftCode.getCode() + " on " + date);

        return toDto(saved, employee.getEmail(), shiftCode, null);
    }

    @Transactional
    public void deleteEntry(String id) {
        RosterEntryEntity entity = rosterEntryRepository.findById(id).orElseThrow(() -> new NotFoundException("Entry not found"));
        if (entity.isLocked()) {
            throw new BadRequestException("This entry is locked");
        }
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
            rosterEntryRepository.findByEmployeeUserIdAndDate(entity.getEmployeeUserId(), targetDate).ifPresent(e -> {
                throw new BadRequestException("This employee already has an entry on the target date");
            });
        }
        LocalDate originalDate = entity.getDate();
        entity.setDate(targetDate);
        RosterEntryEntity saved = rosterEntryRepository.saveAndFlush(entity);

        UserEntity employee = userRepository.findById(saved.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        ShiftCodeEntity shiftCode = shiftCodeRepository.findById(saved.getShiftCodeId()).orElseThrow(() -> new NotFoundException("Shift code not found"));
        auditLogService.record(
                AuditAction.ROSTER_ENTRY_MOVED, AuditEntityType.ROSTER_ENTRY, saved.getId(),
                "Moved " + employee.getEmail() + "'s " + shiftCode.getCode() + " shift from " + originalDate + " to " + targetDate);

        return toDto(saved, employee.getEmail(), shiftCode, null);
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
            rosterEntryRepository.findByEmployeeUserIdAndDate(targetEmployee.getId(), entity.getDate()).ifPresent(e -> {
                throw new BadRequestException("The target employee already has an entry on this date");
            });
        }

        UserEntity originalEmployee = userRepository.findById(entity.getEmployeeUserId()).orElseThrow(() -> new NotFoundException("Employee not found"));
        entity.setEmployeeUserId(targetEmployee.getId());
        RosterEntryEntity saved = rosterEntryRepository.saveAndFlush(entity);

        ShiftCodeEntity shiftCode = shiftCodeRepository.findById(saved.getShiftCodeId()).orElseThrow(() -> new NotFoundException("Shift code not found"));
        auditLogService.record(
                AuditAction.ROSTER_ENTRY_REASSIGNED, AuditEntityType.ROSTER_ENTRY, saved.getId(),
                "Gave " + originalEmployee.getEmail() + "'s " + shiftCode.getCode() + " shift on " + saved.getDate() + " to "
                        + targetEmployee.getEmail());

        return toDto(saved, targetEmployee.getEmail(), shiftCode, null);
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
        if (rosterEntryRepository.findByEmployeeUserIdAndDate(entity.getEmployeeUserId(), other.getDate()).filter(e -> !e.getId().equals(entity.getId())).isPresent()
                || rosterEntryRepository.findByEmployeeUserIdAndDate(other.getEmployeeUserId(), entity.getDate()).filter(e -> !e.getId().equals(other.getId())).isPresent()) {
            throw new BadRequestException("One of these employees already has an entry on the other's date");
        }

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
                "Swapped days with " + otherEmployee.getEmail() + ": now " + entity.getDate());
        auditLogService.record(
                AuditAction.ROSTER_ENTRIES_SWAPPED, AuditEntityType.ROSTER_ENTRY, other.getId(),
                "Swapped days with " + entityEmployee.getEmail() + ": now " + other.getDate());

        ShiftCodeEntity shiftCode = shiftCodeRepository.findById(entity.getShiftCodeId()).orElseThrow(() -> new NotFoundException("Shift code not found"));
        return toDto(entity, entityEmployee.getEmail(), shiftCode, null);
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
                (locked ? "Locked" : "Unlocked") + " " + employee.getEmail() + "'s " + shiftCode.getCode() + " shift on " + saved.getDate());

        return toDto(saved, employee.getEmail(), shiftCode, null);
    }

    private RosterMonth buildMonth(int year, int month, List<RosterEntryEntity> entities) {
        Map<String, String> emails = resolveEmails(entities.stream().map(RosterEntryEntity::getEmployeeUserId).distinct().toList());
        Map<String, ShiftCodeEntity> shiftCodes = resolveShiftCodes(entities);

        List<RosterEntry> entryDtos = entities.stream().map(e -> toDto(e, emails.get(e.getEmployeeUserId()), shiftCodes.get(e.getShiftCodeId()), null)).toList();
        List<RosterEmployee> employeeDtos = listEmployees();

        return new RosterMonth(year, month, entryDtos, employeeDtos, List.of());
    }

    private Map<String, String> resolveEmails(List<String> userIds) {
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
    }

    private Map<String, ShiftCodeEntity> resolveShiftCodes(List<RosterEntryEntity> entities) {
        List<String> ids = entities.stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList();
        return shiftCodeRepository.findAllById(ids).stream().collect(Collectors.toMap(ShiftCodeEntity::getId, s -> s));
    }

    private RosterEntry toDto(RosterEntryEntity e, String employeeEmail, ShiftCodeEntity shiftCode, String shiftCodeCreatedByEmail) {
        RosterEntry dto = new RosterEntry(
                e.getId(), e.getEmployeeUserId(), employeeEmail, e.getDate().toString(), ShiftCodeService.toDto(shiftCode, shiftCodeCreatedByEmail),
                e.isLocked(), com.sunsetbeach.mapper.TimestampFormat.toUtc(e.getCreatedAt()), com.sunsetbeach.mapper.TimestampFormat.toUtc(e.getUpdatedAt()));
        if (e.getNote() != null) {
            dto.note(e.getNote());
        }
        return dto;
    }
}
