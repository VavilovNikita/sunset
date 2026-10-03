package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.RosterCopyInput;
import com.sunsetbeach.model.RosterCopyResult;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code POST /roster/copy}: March 2027 (starts on a Monday) is copied into April 2027 (starts on a
 * Thursday). Copying by weekday is the point - a Sunday off in March must still be a Sunday off in
 * April, which a day-of-month copy would break. No security context is set, so the audit entry
 * fails open and writes nothing to clean up; everything else rolls back.
 */
@SpringBootTest
@Transactional
class RosterCopyTests extends AbstractIntegrationTest {

    @Autowired private RosterService rosterService;
    @Autowired private ShiftCodeService shiftCodeService;
    @Autowired private RosterEntryRepository rosterEntryRepository;
    @Autowired private UserRepository userRepository;

    private UserEntity manager;
    private UserEntity employee;
    private String codeText;
    private ShiftCode codeV1;

    @BeforeEach
    void setUp() {
        manager = user(Role.MANAGER, true);
        employee = user(Role.WAITER, true);
        codeText = "K" + UUID.randomUUID().toString().substring(0, 5);
        codeV1 = code("2020-01-01");

        // Every day of March except Sundays.
        for (LocalDate d = LocalDate.of(2027, 3, 1); !d.isAfter(LocalDate.of(2027, 3, 31)); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                entry(employee, d, codeV1.getId());
            }
        }
    }

    @Test
    void defaultSource_isThePreviousMonth_cutToWholeWeeks_andCopiedByWeekday() {
        RosterCopyResult result = rosterService.copyMonth(new RosterCopyInput(2027, 4), manager.getId());

        assertThat(result.getSourceFrom()).isEqualTo("2027-03-01");
        assertThat(result.getSourceTo()).isEqualTo("2027-03-28");
        Map<LocalDate, RosterEntryEntity> april = aprilOf(employee);
        for (LocalDate d = LocalDate.of(2027, 4, 1); !d.isAfter(LocalDate.of(2027, 4, 30)); d = d.plusDays(1)) {
            assertThat(april.containsKey(d)).as(d + " " + d.getDayOfWeek()).isEqualTo(d.getDayOfWeek() != DayOfWeek.SUNDAY);
        }
        assertThat(april).hasSize(26);
        assertThat(result.getCreated()).isGreaterThanOrEqualTo(26);
    }

    @Test
    void existingEntries_areNeverOverwritten_andRerunningCreatesNothing() {
        ShiftCode other = code2();
        entry(employee, LocalDate.of(2027, 4, 5), other.getId());

        RosterCopyResult first = rosterService.copyMonth(new RosterCopyInput(2027, 4), manager.getId());
        RosterCopyResult second = rosterService.copyMonth(new RosterCopyInput(2027, 4), manager.getId());

        assertThat(aprilOf(employee).get(LocalDate.of(2027, 4, 5)).getShiftCodeId()).isEqualTo(other.getId());
        assertThat(first.getSkippedExisting()).isEqualTo(1);
        assertThat(second.getCreated()).isZero();
    }

    @Test
    void copiedEntries_pointAtTheCodeVersionInForceOnTheirNewDate() {
        ShiftCode codeV2 = code("2027-04-15");

        rosterService.copyMonth(new RosterCopyInput(2027, 4), manager.getId());

        Map<LocalDate, RosterEntryEntity> april = aprilOf(employee);
        assertThat(april.get(LocalDate.of(2027, 4, 14)).getShiftCodeId()).isEqualTo(codeV1.getId());
        assertThat(april.get(LocalDate.of(2027, 4, 15)).getShiftCodeId()).isEqualTo(codeV2.getId());
    }

    @Test
    void inactiveEmployees_areSkipped() {
        UserEntity departed = user(Role.WAITER, true);
        entry(departed, LocalDate.of(2027, 3, 1), codeV1.getId());
        departed.setActive(false);
        userRepository.saveAndFlush(departed);

        rosterService.copyMonth(new RosterCopyInput(2027, 4), manager.getId());

        assertThat(aprilOf(departed)).isEmpty();
    }

    @Test
    void explicitSourceRange_isUsed_andMustCoverAWeek() {
        RosterCopyResult result = rosterService.copyMonth(
                new RosterCopyInput(2027, 4).sourceFrom("2027-03-08").sourceTo("2027-03-14"), manager.getId());
        assertThat(result.getSourceTo()).isEqualTo("2027-03-14");
        assertThat(aprilOf(employee)).hasSize(26);

        assertThatThrownBy(() -> rosterService.copyMonth(new RosterCopyInput(2027, 5).sourceFrom("2027-03-08").sourceTo("2027-03-13"), manager.getId()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> rosterService.copyMonth(new RosterCopyInput(2027, 5).sourceFrom("2027-03-08"), manager.getId()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> rosterService.copyMonth(new RosterCopyInput(2027, 3).sourceFrom("2027-03-01").sourceTo("2027-03-14"), manager.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    private Map<LocalDate, RosterEntryEntity> aprilOf(UserEntity user) {
        return rosterEntryRepository.findByEmployeeUserIdAndDateBetween(user.getId(), LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 30))
                .stream().collect(Collectors.toMap(RosterEntryEntity::getDate, e -> e));
    }

    private UserEntity user(Role role, boolean active) {
        UserEntity user = new UserEntity();
        user.setEmail("roster-copy-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(role);
        user.setActive(active);
        return userRepository.saveAndFlush(user);
    }

    private ShiftCode code(String effectiveFrom) {
        return shiftCodeService.create(
                new ShiftCodeCreateInput(codeText, ShiftCodeKind.MORNING, true, true, effectiveFrom)
                        .staffArea(StaffArea.RESTAURANT).startTime1("09:00").endTime1("17:00"),
                manager.getId());
    }

    private ShiftCode code2() {
        return shiftCodeService.create(
                new ShiftCodeCreateInput("O" + UUID.randomUUID().toString().substring(0, 5), ShiftCodeKind.EVENING, true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT).startTime1("14:00").endTime1("22:00"),
                manager.getId());
    }

    private void entry(UserEntity user, LocalDate date, String shiftCodeId) {
        RosterEntryEntity e = new RosterEntryEntity();
        e.setEmployeeUserId(user.getId());
        e.setDate(date);
        e.setShiftCodeId(shiftCodeId);
        e.setCreatedByUserId(manager.getId());
        rosterEntryRepository.saveAndFlush(e);
    }
}
