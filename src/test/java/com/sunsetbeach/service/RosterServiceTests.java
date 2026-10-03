package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.EmployeePatternInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntry;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.RosterMonth;
import com.sunsetbeach.model.RosterMoveInput;
import com.sunsetbeach.model.RosterReassignInput;
import com.sunsetbeach.model.RosterSwapInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.StaffAreaCoverageRuleInput;
import com.sunsetbeach.model.Weekday;
import com.sunsetbeach.repository.EmployeePatternRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.StaffAreaCoverageRuleRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The roster grid and its editing surface - move (same employee, new date), reassign (different
 * employee, same date), swap (trade date+code between two entries). See
 * RosterService#swapEntries's own comment for why the swap needs no SERIALIZABLE/deferred-
 * constraint machinery the room and table swaps needed - a person's own dates aren't a resource
 * a third party can contend for.
 */
@SpringBootTest
class RosterServiceTests extends AbstractIntegrationTest {

    @Autowired
    private RosterService rosterService;

    @Autowired
    private ShiftCodeService shiftCodeService;

    @Autowired
    private EmployeePatternService employeePatternService;

    @Autowired
    private StaffAreaCoverageRuleService staffAreaCoverageRuleService;

    @Autowired
    private RosterEntryRepository rosterEntryRepository;

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private EmployeePatternRepository employeePatternRepository;

    @Autowired
    private StaffAreaCoverageRuleRepository staffAreaCoverageRuleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdShiftCodeIds = new ArrayList<>();
    private UserEntity manager;

    @AfterEach
    void cleanUp() {
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> createdUserIds.contains(e.getEmployeeUserId())).toList());
        createdUserIds.forEach(employeePatternRepository::deleteById);
        staffAreaCoverageRuleRepository.deleteAll();
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("roster-test-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(role);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private UserEntity createUserInArea(StaffArea area) {
        UserEntity user = createUser(Role.WAITER);
        user.setStaffArea(area);
        return userRepository.saveAndFlush(user);
    }

    private ShiftCode createCode(StaffArea area, String start, String end) {
        UserEntity actor = manager != null ? manager : createUser(Role.MANAGER);
        manager = actor;
        ShiftCodeKind kind = start.compareTo("12:00") < 0 ? ShiftCodeKind.MORNING : ShiftCodeKind.EVENING;
        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput("C" + UUID.randomUUID().toString().substring(0, 6), kind, true, true, "2020-01-01")
                        .staffArea(area)
                        .startTime1(start)
                        .endTime1(end),
                actor.getId());
        createdShiftCodeIds.add(code.getId());
        return code;
    }

    @Test
    void createEntry_thenAppearsInMonth() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        LocalDate date = LocalDate.of(2027, 3, 10);

        RosterEntry created = rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), code.getId()), mgr.getId());
        assertThat(created.getShiftCode().getId()).isEqualTo(code.getId());

        RosterMonth month = rosterService.getMonth(2027, 3);
        assertThat(month.getEntries()).extracting(RosterEntry::getId).contains(created.getId());
    }

    @Test
    void createEntry_duplicateDateForSameEmployee_isRejected() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        LocalDate date = LocalDate.of(2027, 3, 11);
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), code.getId()), mgr.getId());

        assertThatThrownBy(() -> rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), date.toString(), code.getId()), mgr.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void moveEntry_toAFreeDate_succeeds() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        RosterEntry entry =
                rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-03-12", code.getId()), mgr.getId());

        RosterEntry moved = rosterService.moveEntry(entry.getId(), new RosterMoveInput("2027-03-13"), mgr.getId());
        assertThat(moved.getDate()).isEqualTo("2027-03-13");
    }

    @Test
    void moveEntry_locked_isRejected() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        RosterEntry entry =
                rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-03-14", code.getId()), mgr.getId());
        rosterService.setLocked(entry.getId(), true);

        assertThatThrownBy(() -> rosterService.moveEntry(entry.getId(), new RosterMoveInput("2027-03-15"), mgr.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void moveEntry_targetDateAlreadyOccupied_isRejected() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        RosterEntry entry =
                rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-03-16", code.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-03-17", code.getId()), mgr.getId());

        assertThatThrownBy(() -> rosterService.moveEntry(entry.getId(), new RosterMoveInput("2027-03-17"), mgr.getId()))
                .isInstanceOf(BadRequestException.class);
        // Rejected - the original entry must not have moved.
        assertThat(rosterEntryRepository.findById(entry.getId()).orElseThrow().getDate()).isEqualTo(LocalDate.of(2027, 3, 16));
    }

    @Test
    void reassignEntry_givesTheDayToADifferentEmployee() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity original = createUser(Role.WAITER);
        UserEntity target = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        RosterEntry entry =
                rosterService.createEntry(new RosterEntryCreateInput(original.getId(), "2027-03-18", code.getId()), mgr.getId());

        RosterEntry reassigned = rosterService.reassignEntry(entry.getId(), new RosterReassignInput(target.getId()), mgr.getId());
        assertThat(reassigned.getEmployeeUserId()).isEqualTo(target.getId());
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(original.getId(), LocalDate.of(2027, 3, 18))).isEmpty();
    }

    /** The source spreadsheet's own note - "Tey to help housekeeping" - a Restaurant employee covering Housekeeping is a real, wanted operation. */
    @Test
    void reassignEntry_toADifferentStaffArea_isAllowed() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity restaurantEmployee = createUser(Role.WAITER);
        UserEntity housekeepingEmployee = createUser(Role.WAITER);
        ShiftCode housekeepingCode = createCode(StaffArea.HOUSEKEEPING, "08:00", "17:00");
        RosterEntry entry = rosterService.createEntry(
                new RosterEntryCreateInput(restaurantEmployee.getId(), "2027-03-19", housekeepingCode.getId()), mgr.getId());

        RosterEntry reassigned = rosterService.reassignEntry(entry.getId(), new RosterReassignInput(housekeepingEmployee.getId()), mgr.getId());
        assertThat(reassigned.getEmployeeUserId()).isEqualTo(housekeepingEmployee.getId());
        assertThat(reassigned.getShiftCode().getStaffArea().get()).isEqualTo(StaffArea.HOUSEKEEPING);
    }

    @Test
    void swapEntries_tradesDateAndShiftCodeBetweenTwoEmployees() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employeeA = createUser(Role.WAITER);
        UserEntity employeeB = createUser(Role.WAITER);
        ShiftCode codeA = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        ShiftCode codeB = createCode(StaffArea.RESTAURANT, "12:00", "21:00");
        RosterEntry entryA =
                rosterService.createEntry(new RosterEntryCreateInput(employeeA.getId(), "2027-03-20", codeA.getId()), mgr.getId());
        RosterEntry entryB =
                rosterService.createEntry(new RosterEntryCreateInput(employeeB.getId(), "2027-03-21", codeB.getId()), mgr.getId());

        rosterService.swapEntries(entryA.getId(), new RosterSwapInput(entryB.getId()), mgr.getId());

        RosterEntryEntity refreshedA = rosterEntryRepository.findById(entryA.getId()).orElseThrow();
        RosterEntryEntity refreshedB = rosterEntryRepository.findById(entryB.getId()).orElseThrow();
        assertThat(refreshedA.getDate()).isEqualTo(LocalDate.of(2027, 3, 21));
        assertThat(refreshedA.getShiftCodeId()).isEqualTo(codeB.getId());
        assertThat(refreshedB.getDate()).isEqualTo(LocalDate.of(2027, 3, 20));
        assertThat(refreshedB.getShiftCodeId()).isEqualTo(codeA.getId());
        // Employee identity never moves in a swap - only date+code trade.
        assertThat(refreshedA.getEmployeeUserId()).isEqualTo(employeeA.getId());
        assertThat(refreshedB.getEmployeeUserId()).isEqualTo(employeeB.getId());
    }

    @Test
    void swapEntries_whenTargetEmployeeAlreadyHasAnEntryOnTheOtherDate_isRejected() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employeeA = createUser(Role.WAITER);
        UserEntity employeeB = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        RosterEntry entryA = rosterService.createEntry(new RosterEntryCreateInput(employeeA.getId(), "2027-03-22", code.getId()), mgr.getId());
        RosterEntry entryB = rosterService.createEntry(new RosterEntryCreateInput(employeeB.getId(), "2027-03-23", code.getId()), mgr.getId());
        // employeeB already has something on 2027-03-22 too - the date entryA would move onto B for.
        rosterService.createEntry(new RosterEntryCreateInput(employeeB.getId(), "2027-03-22", code.getId()), mgr.getId());

        assertThatThrownBy(() -> rosterService.swapEntries(entryA.getId(), new RosterSwapInput(entryB.getId()), mgr.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void swapEntries_oneLocked_isRejected() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employeeA = createUser(Role.WAITER);
        UserEntity employeeB = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        RosterEntry entryA = rosterService.createEntry(new RosterEntryCreateInput(employeeA.getId(), "2027-03-24", code.getId()), mgr.getId());
        RosterEntry entryB = rosterService.createEntry(new RosterEntryCreateInput(employeeB.getId(), "2027-03-25", code.getId()), mgr.getId());
        rosterService.setLocked(entryB.getId(), true);

        assertThatThrownBy(() -> rosterService.swapEntries(entryA.getId(), new RosterSwapInput(entryB.getId()), mgr.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteEntry_locked_isRejected() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        RosterEntry entry = rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-03-26", code.getId()), mgr.getId());
        rosterService.setLocked(entry.getId(), true);

        assertThatThrownBy(() -> rosterService.deleteEntry(entry.getId())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void generateMonth_fillsFromPatternSkippingWeeklyDayOff_neverOverwritesExisting() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        ShiftCode code = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        ShiftCode overrideCode = createCode(StaffArea.RESTAURANT, "12:00", "21:00");
        employeePatternService.set(employee.getId(), new EmployeePatternInput(StaffArea.RESTAURANT, 6, Weekday.MONDAY).defaultShiftCodeId(code.getId()), mgr.getId());
        // Hand-edited before generation - must survive untouched.
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-04-06", overrideCode.getId()), mgr.getId()); // a Tuesday

        rosterService.generateMonth(2027, 4, mgr.getId());

        List<RosterEntryEntity> entries =
                rosterEntryRepository.findByEmployeeUserIdAndDateBetween(employee.getId(), LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 30));
        // Mondays in April 2027 are skipped (the weekly day off).
        assertThat(entries.stream().anyMatch(e -> e.getDate().getDayOfWeek().name().equals("MONDAY"))).isFalse();
        // The hand-edited Tuesday kept its override code, not the pattern's default.
        assertThat(entries.stream().filter(e -> e.getDate().equals(LocalDate.of(2027, 4, 6))).findFirst().orElseThrow().getShiftCodeId())
                .isEqualTo(overrideCode.getId());
        // An ordinary, untouched Wednesday got the default code.
        assertThat(entries.stream().filter(e -> e.getDate().equals(LocalDate.of(2027, 4, 7))).findFirst().orElseThrow().getShiftCodeId())
                .isEqualTo(code.getId());

        // Running it again does not duplicate or disturb anything.
        rosterService.generateMonth(2027, 4, mgr.getId());
        List<RosterEntryEntity> secondRun =
                rosterEntryRepository.findByEmployeeUserIdAndDateBetween(employee.getId(), LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 30));
        assertThat(secondRun).hasSameSizeAs(entries);
    }

    @Test
    void getMonth_belowMinimum_warnsForThatAreaAndDateOnly() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity employee = createUserInArea(StaffArea.KITCHEN);
        ShiftCode code = createCode(StaffArea.KITCHEN, "12:00", "21:00");
        staffAreaCoverageRuleService.set(StaffArea.KITCHEN, new StaffAreaCoverageRuleInput(2), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), "2027-05-02", code.getId()), mgr.getId());

        RosterMonth month = rosterService.getMonth(2027, 5);

        assertThat(month.getCoverageWarnings())
                .anyMatch(w -> w.getStaffArea() == StaffArea.KITCHEN && w.getDate().equals("2027-05-02") && w.getWorkingCount() == 1 && w.getMinimumWorking() == 2);
        // A day with nobody in Kitchen at all still warns (0 < 2).
        assertThat(month.getCoverageWarnings()).anyMatch(w -> w.getStaffArea() == StaffArea.KITCHEN && w.getDate().equals("2027-05-03") && w.getWorkingCount() == 0);
    }


    /**
     * The reported bug: coverage showed 0/N on every day while Working counted correctly, because
     * the numerator read the area off the shift code - and a shared code (staffArea null: OP, PH,
     * 7, 12, ...) has none, so every entry using one counted toward no area at all. Each entry now
     * counts toward its employee's own area, per area independently, and a code defined for a
     * different area still counts where the person actually works. Not equal to a whole-day
     * Working total either: an ABSENCE entry counts nowhere, an inactive employee counts nowhere.
     */
    @Test
    void getMonth_coverageCountsByEmployeeArea_includingSharedAndOtherAreaCodes() {
        UserEntity mgr = createUser(Role.MANAGER);
        ShiftCode shared = createSharedCode("S" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.OPEN_SCHEDULE, true);
        ShiftCode sharedAbsence = createSharedCode("A" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.ABSENCE, false);
        ShiftCode restaurantCode = createCode(StaffArea.RESTAURANT, "09:00", "17:00");
        staffAreaCoverageRuleService.set(StaffArea.KITCHEN, new StaffAreaCoverageRuleInput(50), mgr.getId());
        staffAreaCoverageRuleService.set(StaffArea.MAINTENANCE, new StaffAreaCoverageRuleInput(50), mgr.getId());
        String date = "2027-10-05";

        UserEntity cook1 = createUserInArea(StaffArea.KITCHEN);
        UserEntity cook2 = createUserInArea(StaffArea.KITCHEN);
        UserEntity cookOnHoliday = createUserInArea(StaffArea.KITCHEN);
        UserEntity cookInactive = createUserInArea(StaffArea.KITCHEN);
        UserEntity engineer = createUserInArea(StaffArea.MAINTENANCE);
        rosterService.createEntry(new RosterEntryCreateInput(cook1.getId(), date, shared.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(cook2.getId(), date, restaurantCode.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(cookOnHoliday.getId(), date, sharedAbsence.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(cookInactive.getId(), date, shared.getId()), mgr.getId());
        cookInactive.setActive(false);
        userRepository.saveAndFlush(cookInactive);
        rosterService.createEntry(new RosterEntryCreateInput(engineer.getId(), date, shared.getId()), mgr.getId());

        // Counted against whatever else the shared database already has rostered that day, so
        // compared as "everything else + what this test added", per area.
        int kitchenBefore = 0, maintenanceBefore = 0;
        for (RosterEntryEntity e : rosterEntryRepository.findByDateBetween(LocalDate.parse(date), LocalDate.parse(date))) {
            if (createdUserIds.contains(e.getEmployeeUserId())) continue;
            UserEntity u = userRepository.findById(e.getEmployeeUserId()).orElseThrow();
            boolean worked = shiftCodeRepository.findById(e.getShiftCodeId()).orElseThrow().isCountsAsWorked();
            if (!worked || !u.isActive()) continue;
            if (u.getStaffArea() == StaffArea.KITCHEN) kitchenBefore++;
            if (u.getStaffArea() == StaffArea.MAINTENANCE) maintenanceBefore++;
        }
        int expectedKitchen = kitchenBefore + 2;
        int expectedMaintenance = maintenanceBefore + 1;

        RosterMonth month = rosterService.getMonth(2027, 10);

        assertThat(month.getCoverageWarnings())
                .anyMatch(w -> w.getStaffArea() == StaffArea.KITCHEN && w.getDate().equals(date) && w.getWorkingCount() == expectedKitchen);
        assertThat(month.getCoverageWarnings())
                .anyMatch(w -> w.getStaffArea() == StaffArea.MAINTENANCE && w.getDate().equals(date) && w.getWorkingCount() == expectedMaintenance);
    }

    /**
     * Admin, one day - Mai has no entry at all, Pueng has the shared working code 7: 1/1, and with a
     * minimum of 2 it's 1/2, not 0/2 - coverage is "N of the area's active people on a
     * countsAsWorked code", not tied to any particular person. Front office, Joy's row: OP covers;
     * PH and any other countsAsWorked=false code (like an unpaid-leave one) doesn't, and neither
     * does a blank cell.
     */
    @Test
    void getMonth_coverage_oneOfTwoInAreaOnWorkingCode_isCovered_absenceIsNot() {
        UserEntity mgr = createUser(Role.MANAGER);
        UserEntity codeAuthor = manager != null ? manager : createUser(Role.MANAGER);
        manager = codeAuthor;
        ShiftCode seven = shiftCodeService.create(
                new ShiftCodeCreateInput("7" + UUID.randomUUID().toString().substring(0, 5), ShiftCodeKind.MORNING, true, true, "2020-01-01")
                        .startTime1("07:00")
                        .endTime1("16:00"),
                codeAuthor.getId());
        createdShiftCodeIds.add(seven.getId());
        ShiftCode op = createSharedCode("O" + UUID.randomUUID().toString().substring(0, 5), ShiftCodeKind.OPEN_SCHEDULE, true);
        ShiftCode ph = createSharedCode("P" + UUID.randomUUID().toString().substring(0, 5), ShiftCodeKind.ABSENCE, false);
        ShiftCode unpaid = createSharedCode("U" + UUID.randomUUID().toString().substring(0, 5), ShiftCodeKind.ABSENCE, false);
        staffAreaCoverageRuleService.set(StaffArea.ADMIN, new StaffAreaCoverageRuleInput(1), mgr.getId());
        staffAreaCoverageRuleService.set(StaffArea.FRONT_OFFICE, new StaffAreaCoverageRuleInput(1), mgr.getId());

        UserEntity mai = createUserInArea(StaffArea.ADMIN);
        UserEntity pueng = createUserInArea(StaffArea.ADMIN);
        UserEntity joy = createUserInArea(StaffArea.FRONT_OFFICE);
        rosterService.createEntry(new RosterEntryCreateInput(mai.getId(), "2031-03-09", seven.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(pueng.getId(), "2031-03-10", seven.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(joy.getId(), "2031-03-10", op.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(joy.getId(), "2031-03-11", ph.getId()), mgr.getId());
        rosterService.createEntry(new RosterEntryCreateInput(joy.getId(), "2031-03-12", unpaid.getId()), mgr.getId());
        // 2031-03-13: Joy's cell left blank.

        RosterMonth month = rosterService.getMonth(2031, 3);
        assertThat(month.getCoverageWarnings())
                .noneMatch(w -> w.getStaffArea() == StaffArea.ADMIN && w.getDate().equals("2031-03-10"))
                .noneMatch(w -> w.getStaffArea() == StaffArea.FRONT_OFFICE && w.getDate().equals("2031-03-10"))
                .anyMatch(w -> w.getStaffArea() == StaffArea.FRONT_OFFICE && w.getDate().equals("2031-03-11") && w.getWorkingCount() == 0)
                .anyMatch(w -> w.getStaffArea() == StaffArea.FRONT_OFFICE && w.getDate().equals("2031-03-12") && w.getWorkingCount() == 0)
                .anyMatch(w -> w.getStaffArea() == StaffArea.FRONT_OFFICE && w.getDate().equals("2031-03-13") && w.getWorkingCount() == 0);

        staffAreaCoverageRuleService.set(StaffArea.ADMIN, new StaffAreaCoverageRuleInput(2), mgr.getId());
        staffAreaCoverageRuleService.set(StaffArea.FRONT_OFFICE, new StaffAreaCoverageRuleInput(2), mgr.getId());
        assertThat(rosterService.getMonth(2031, 3).getCoverageWarnings())
                .anyMatch(w -> w.getStaffArea() == StaffArea.ADMIN && w.getDate().equals("2031-03-10") && w.getWorkingCount() == 1 && w.getMinimumWorking() == 2)
                .anyMatch(w -> w.getStaffArea() == StaffArea.FRONT_OFFICE && w.getDate().equals("2031-03-10") && w.getWorkingCount() == 1 && w.getMinimumWorking() == 2);
    }

    private ShiftCode createSharedCode(String code, ShiftCodeKind kind, boolean countsAsWorked) {
        UserEntity actor = manager != null ? manager : createUser(Role.MANAGER);
        manager = actor;
        ShiftCode created = shiftCodeService.create(new ShiftCodeCreateInput(code, kind, countsAsWorked, true, "2020-01-01"), actor.getId());
        createdShiftCodeIds.add(created.getId());
        return created;
    }
}
