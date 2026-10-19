package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftChangeRequestEntity;
import com.sunsetbeach.entity.StaffAreaCoverageRuleEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.RosterMoveInput;
import com.sunsetbeach.model.ShiftChangeRequestChannel;
import com.sunsetbeach.model.ShiftChangeRequestType;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.UserFunctionsUpdateInput;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.TelegramClient.TelegramUpdate;
import com.sunsetbeach.repository.StaffAreaCoverageRuleRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.ShiftChangeRequestService.DecisionResult;
import com.sunsetbeach.service.ShiftChangeRequestService.Rejection;
import com.sunsetbeach.service.ShiftChangeRequestService.ShiftRequestException;
import com.sunsetbeach.support.FakeTelegramClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The staff bot and shift requests, over the real services and database with a Telegram that never touches
 * the network and a clock the test moves (2033-05-17, hotel time). The tests are the things that would
 * silently give a wrong answer: a double approval, an approval of a shift that changed, the notice period
 * at the hotel's midnight, a button pressed by the wrong person, the wrong message count after a drag series.
 */
class StaffBotTests extends NotificationTestBase {

    private static final LocalDate TODAY_ = TODAY;
    private static final LocalDate SHIFT_DAY = TODAY.plusDays(3);
    private static final LocalDate TARGET_DAY = TODAY.plusDays(5);

    @Autowired private ShiftChangeRequestService requestService;
    @Autowired private ShiftRequestDeciderService deciderService;
    @Autowired private UserService userService;
    @Autowired private StaffBotSettingsService botSettingsService;
    @Autowired private StaffAreaCoverageRuleRepository coverageRuleRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    private UserEntity manager;
    private UserEntity employee;
    private UserEntity hr;
    private String employeeChat;
    private String hrChat;
    private ShiftCode code;
    private long nextUpdate = 1;

    @BeforeEach
    void people() {
        botSettings(s -> s.setRosterChangeEnabled(true));
        manager = user("Manager", Role.MANAGER);
        employee = user("Eve Employee", Role.WAITER);
        employee.setStaffArea(StaffArea.RESTAURANT);
        userRepository.saveAndFlush(employee);
        hr = user("Hana HR", Role.WAITER, "HR");
        employeeChat = link(employee);
        hrChat = link(hr);
        code = shiftCode("09:00", "18:00", manager);
    }

    @AfterEach
    void removeAuditRows() {
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream().filter(a -> userIds.contains(a.getActorUserId())).toList());
    }

    // ---- helpers -----------------------------------------------------------------------------------------

    private RosterEntryEntity entry(UserEntity who, LocalDate date) {
        actAs(manager);
        rosterService.createEntry(new RosterEntryCreateInput(who.getId(), date.toString(), code.getId()), manager.getId());
        SecurityContextHolderClear.clear();
        // Setting the scene is not the manager's "change" under test: drop the mark the creation left.
        rosterChangeMarkRepository.deleteAll(rosterChangeMarkRepository.findAll().stream().filter(m -> m.getEmployeeUserId().equals(who.getId())).toList());
        return rosterEntryRepository.findByEmployeeUserIdAndDate(who.getId(), date).orElseThrow();
    }

    private ShiftChangeRequestEntity file(RosterEntryEntity entry, ShiftChangeRequestType type, LocalDate target, String reason) {
        return requestService.submit(employee, entry.getId(), type, target, reason, ShiftChangeRequestChannel.TELEGRAM);
    }

    private void say(String chat, String text) {
        telegram.incoming.add(new TelegramUpdate(nextUpdate++, chat, text));
        pollService.pollOnce();
    }

    private void press(String chat, String callbackData) {
        telegram.incoming.add(new TelegramUpdate(nextUpdate++, chat, null, "cq" + nextUpdate, callbackData, "1"));
        pollService.pollOnce();
    }

    private ShiftChangeRequestEntity reload(ShiftChangeRequestEntity r) {
        return shiftRequestRepository.findById(r.getId()).orElseThrow();
    }

    private UserEntity admin(String name) {
        UserEntity admin = user(name, Role.ADMIN);
        link(admin);
        return admin;
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, UserEntity who) {
        return request.header("Authorization", "Bearer " + jwtService.issue(new StaffPrincipal(who.getId(), who.getEmail(), who.getRole())));
    }

    /** No HR: the administrators decide. */
    private void withoutHr() {
        hr.setJobFunctions(new String[0]);
        userRepository.saveAndFlush(hr);
    }

    // ---- who holds HR -------------------------------------------------------------------------------------

    @Test
    void aSecondActiveHrHolder_isRefused_andRemovingTheFirstMakesRoom() {
        UserEntity other = user("Other", Role.WAITER);

        assertThatThrownBy(() -> userService.updateFunctions(other.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.HR))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Hana HR");

        userService.updateFunctions(hr.getId(), new UserFunctionsUpdateInput(List.of()));
        userService.updateFunctions(other.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.HR)));
        assertThat(deciderService.activeHr()).map(UserEntity::getId).contains(other.getId());
    }

    @Test
    void twoSimultaneousHrAssignments_leaveExactlyOneHolder() throws Exception {
        withoutHr();
        UserEntity a = user("A", Role.WAITER);
        UserEntity b = user("B", Role.WAITER);
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (UserEntity who : List.of(a, b)) {
                results.add(pool.submit(() -> {
                    barrier.await();
                    try {
                        userService.updateFunctions(who.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.HR)));
                        return true;
                    } catch (ConflictException e) {
                        return false;
                    }
                }));
            }
            long succeeded = 0;
            for (Future<Boolean> f : results) {
                succeeded += f.get() ? 1 : 0;
            }
            assertThat(succeeded).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(userRepository.findActiveHrHolders().stream().filter(u -> userIds.contains(u.getId()))).hasSize(1);
    }

    @Test
    void reactivatingAnHrHolder_whileSomeoneElseHoldsIt_isRefused() {
        userService.setActive(hr.getId(), manager.getId(), false);
        UserEntity other = user("Other", Role.WAITER);
        userService.updateFunctions(other.getId(), new UserFunctionsUpdateInput(List.of(JobFunction.HR)));

        assertThatThrownBy(() -> userService.setActive(hr.getId(), manager.getId(), true)).isInstanceOf(ConflictException.class);
    }

    // ---- the decision is applied by the roster, once --------------------------------------------------------

    @Test
    void twoDecidersApprovingAtOnce_applyTheMoveOnce() throws Exception {
        withoutHr();
        UserEntity a1 = admin("Admin One");
        UserEntity a2 = admin("Admin Two");
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.MOVE, TARGET_DAY, "family");

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<DecisionResult> results = new ArrayList<>();
        try {
            List<Future<DecisionResult>> futures = new ArrayList<>();
            for (UserEntity decider : List.of(a1, a2)) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    return requestService.decide(request.getId(), decider, true, null, ShiftChangeRequestChannel.TELEGRAM);
                }));
            }
            for (Future<DecisionResult> f : futures) {
                results.add(f.get());
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(results).filteredOn(DecisionResult::changedNow).hasSize(1);
        assertThat(results).allSatisfy(r -> assertThat(r.request().getStatus()).isEqualTo("APPROVED"));
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), TARGET_DAY)).isPresent();
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), SHIFT_DAY)).isEmpty();
        assertThat(auditLogRepository.findAll().stream()
                .filter(a -> a.getAction() == AuditAction.ROSTER_ENTRY_MOVED && entry.getId().equals(a.getEntityId()))).hasSize(1);
    }

    @Test
    void approvingARequest_whoseEntryWasMovedMeanwhile_expiresItAndAppliesNothing() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "sick");
        actAs(manager);
        rosterService.moveEntry(entry.getId(), new RosterMoveInput(TODAY.plusDays(4).toString()), manager.getId());
        SecurityContextHolderClear.clear();

        DecisionResult result = requestService.decide(request.getId(), hr, true, null, ShiftChangeRequestChannel.TELEGRAM);

        assertThat(result.request().getStatus()).isEqualTo("EXPIRED");
        assertThat(result.request().getExpiredReason()).isEqualTo(ShiftRequestExpiry.ENTRY_CHANGED.name());
        // Nothing was applied: the entry is where the manager left it, not deleted.
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), TODAY.plusDays(4))).isPresent();
        // The employee is told in words.
        assertThat(notificationsOf(employee, "SHIFT_REQUEST_EXPIRED")).hasSize(1);
    }

    @Test
    void aLockedEntry_expiresTheApproval_andADeletedOneToo() {
        RosterEntryEntity locked = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(locked, ShiftChangeRequestType.DELETE, null, "x");
        actAs(manager);
        rosterService.setLocked(locked.getId(), true);
        SecurityContextHolderClear.clear();
        assertThat(requestService.decide(request.getId(), hr, true, null, ShiftChangeRequestChannel.TELEGRAM).request().getExpiredReason())
                .isEqualTo(ShiftRequestExpiry.LOCKED.name());
        assertThat(rosterEntryRepository.findById(locked.getId())).isPresent();

        // A deleted entry: the request outlives it (FK set null) and expires as "gone".
        RosterEntryEntity doomed = entry(employee, TODAY.plusDays(6));
        ShiftChangeRequestEntity second = file(doomed, ShiftChangeRequestType.MOVE, TODAY.plusDays(7), "y");
        actAs(manager);
        rosterService.deleteEntry(doomed.getId());
        SecurityContextHolderClear.clear();
        assertThat(requestService.decide(second.getId(), hr, true, null, ShiftChangeRequestChannel.TELEGRAM).request().getExpiredReason())
                .isEqualTo(ShiftRequestExpiry.ENTRY_GONE.name());
    }

    @Test
    void secondDecisionOnASettledRequest_changesNothing_andReportsTheCurrentStatus() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "x");

        DecisionResult first = requestService.decide(request.getId(), hr, true, null, ShiftChangeRequestChannel.TELEGRAM);
        DecisionResult second = requestService.decide(request.getId(), hr, false, "no", ShiftChangeRequestChannel.TELEGRAM);

        assertThat(first.changedNow()).isTrue();
        assertThat(second.changedNow()).isFalse();
        assertThat(second.request().getStatus()).isEqualTo("APPROVED");
        // An approved "delete" removed the entry through the roster's own code.
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), SHIFT_DAY)).isEmpty();
    }

    // ---- when a request may be filed, by the hotel's clock --------------------------------------------------------

    @Test
    void noticePeriod_isMeasuredToTheShiftStart_inclusive() {
        RosterEntryEntity entry = entry(employee, TODAY.plusDays(1)); // starts 09:00 tomorrow

        clock.set(DAY + "T09:00");
        assertThat(requestService.check(employee, entry, ShiftChangeRequestType.DELETE)).isEmpty();
        clock.set(DAY + "T09:01");
        assertThat(requestService.check(employee, entry, ShiftChangeRequestType.DELETE)).contains(Rejection.TOO_LATE);
    }

    @Test
    void today_isTheHotelDay_notTheUtcDay_andIsOffUnlessAllowed() {
        RosterEntryEntity entry = entry(employee, TODAY);
        botSettings(s -> s.setRequestMinHoursBefore(9));

        // 23:55 the evening before in Bangkok is already the next day in UTC+7... but not yet "today" for the shift.
        clock.set(TODAY.minusDays(1) + "T23:55");
        assertThat(requestService.check(employee, entry, ShiftChangeRequestType.DELETE)).isEmpty(); // 23:55 + 9 h = 08:55, before 09:00
        // Ten minutes later it is the hotel's today: today's shift can't be changed here.
        clock.set(DAY + "T00:05");
        assertThat(requestService.check(employee, entry, ShiftChangeRequestType.DELETE)).contains(Rejection.TODAY);

        // Allowed by setting (and no notice): fine until the shift starts, not a minute after.
        botSettings(s -> {
            s.setAllowTodayRequests(true);
            s.setRequestMinHoursBefore(0);
        });
        clock.set(DAY + "T09:00");
        assertThat(requestService.check(employee, entry, ShiftChangeRequestType.DELETE)).isEmpty();
        clock.set(DAY + "T09:01");
        assertThat(requestService.check(employee, entry, ShiftChangeRequestType.DELETE)).contains(Rejection.TOO_LATE);
        clock.set(DAY + "T10:00");
        assertThat(requestService.check(employee, rosterEntryRepository.findById(entry.getId()).orElseThrow(), ShiftChangeRequestType.DELETE)).isPresent();
    }

    @Test
    void aLockedShift_aPastShift_andAShiftThatAlreadyHasARequest_cannotBeRequested() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        actAs(manager);
        rosterService.setLocked(entry.getId(), true);
        SecurityContextHolderClear.clear();
        assertThat(requestService.check(employee, rosterEntryRepository.findById(entry.getId()).orElseThrow(), ShiftChangeRequestType.DELETE)).contains(Rejection.LOCKED);

        actAs(manager);
        rosterService.setLocked(entry.getId(), false);
        SecurityContextHolderClear.clear();
        file(entry, ShiftChangeRequestType.DELETE, null, "x");
        assertThatThrownBy(() -> file(entry, ShiftChangeRequestType.MOVE, TARGET_DAY, "y"))
                .isInstanceOfSatisfying(ShiftRequestException.class, e -> assertThat(e.reason()).isEqualTo(Rejection.PENDING));

        clock.set(TODAY.plusDays(4) + "T10:00");
        assertThat(requestService.check(employee, entry, ShiftChangeRequestType.DELETE)).contains(Rejection.PAST);
    }

    @Test
    void aMoveGoesOnlyToAFreeDayInsideTheWindow() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        entry(employee, TARGET_DAY);

        List<LocalDate> targets = requestService.eligibleTargets(employee, entry);
        // Tomorrow's 09:00 is less than the 24 h notice away from 10:00 today; the day after is not.
        assertThat(targets).doesNotContain(SHIFT_DAY, TARGET_DAY, TODAY, TODAY.plusDays(1)).contains(TODAY.plusDays(2));
        assertThat(targets).allSatisfy(d -> assertThat(d).isBetween(TODAY, TODAY.plusDays(14)));
        assertThatThrownBy(() -> file(entry, ShiftChangeRequestType.MOVE, TARGET_DAY, "x"))
                .isInstanceOfSatisfying(ShiftRequestException.class, e -> assertThat(e.reason()).isEqualTo(Rejection.TARGET_BUSY));
        assertThatThrownBy(() -> file(entry, ShiftChangeRequestType.MOVE, TODAY.plusDays(40), "x"))
                .isInstanceOfSatisfying(ShiftRequestException.class, e -> assertThat(e.reason()).isEqualTo(Rejection.TARGET));
    }

    @Test
    void theReason_isRequiredByDefault_andBoundedTo300Characters() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);

        assertThatThrownBy(() -> file(entry, ShiftChangeRequestType.DELETE, null, "  "))
                .isInstanceOfSatisfying(ShiftRequestException.class, e -> assertThat(e.reason()).isEqualTo(Rejection.REASON_REQUIRED));
        assertThatThrownBy(() -> file(entry, ShiftChangeRequestType.DELETE, null, "x".repeat(301)))
                .isInstanceOfSatisfying(ShiftRequestException.class, e -> assertThat(e.reason()).isEqualTo(Rejection.REASON_TOO_LONG));

        botSettings(s -> s.setReasonRequired(false));
        assertThat(file(entry, ShiftChangeRequestType.DELETE, null, null).getReason()).isNull();
    }

    // ---- coverage warns, it does not block ----------------------------------------------------------------------

    @Test
    void coverageBelowTheMinimum_isShownToTheDecider_butApprovalStillWorks() {
        UserEntity colleague = user("Colleague", Role.WAITER);
        colleague.setStaffArea(StaffArea.RESTAURANT);
        userRepository.saveAndFlush(colleague);
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        entry(colleague, SHIFT_DAY);
        var previous = coverageRuleRepository.findById(StaffArea.RESTAURANT);
        StaffAreaCoverageRuleEntity rule = previous.orElseGet(StaffAreaCoverageRuleEntity::new);
        int previousMinimum = rule.getMinimumWorking();
        rule.setStaffArea(StaffArea.RESTAURANT);
        rule.setMinimumWorking(2);
        rule.setUpdatedByUserId(manager.getId());
        coverageRuleRepository.saveAndFlush(rule);
        try {
            ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "sick");

            var dto = requestService.toDto(request, hr);
            assertThat(dto.getCoverage()).isNotNull();
            assertThat(dto.getCoverage().getSourceWorkingAfter()).isEqualTo(1);
            assertThat(dto.getCoverage().getMinimumWorking()).isEqualTo(2);
            assertThat(dto.getCoverage().getBelowMinimum()).isTrue();
            assertThat(notificationsOf(hr, "SHIFT_REQUEST_NEW")).singleElement().satisfies(n -> assertThat(n.getText()).contains("! "));

            DecisionResult result = requestService.decide(request.getId(), hr, true, null, ShiftChangeRequestChannel.TELEGRAM);
            assertThat(result.request().getStatus()).isEqualTo("APPROVED");
            assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), SHIFT_DAY)).isEmpty();
        } finally {
            if (previous.isPresent()) {
                rule.setMinimumWorking(previousMinimum);
                coverageRuleRepository.saveAndFlush(rule);
            } else {
                coverageRuleRepository.delete(rule);
            }
        }
    }

    // ---- who may press a button ------------------------------------------------------------------------------------

    @Test
    void aButtonPressedFromAnotherChat_doesNothing() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "x");
        deliveryService.runOnce();
        String approve = FakeTelegramClient.callbackOf(telegram.lastTo(hrChat), "Approve");
        assertThat(approve).as("the announcement carries an Approve button").startsWith("a:");

        // The employee has the callback data (a forwarded message, a leaked id) and presses it from their own chat.
        press(employeeChat, approve);

        assertThat(reload(request).getStatus()).isEqualTo("PENDING");
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), SHIFT_DAY)).isPresent();
    }

    @Test
    void aButtonPressedAfterTheFunctionWasTakenAway_doesNothing() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "x");
        deliveryService.runOnce();
        String approve = FakeTelegramClient.callbackOf(telegram.lastTo(hrChat), "Approve");
        userService.updateFunctions(hr.getId(), new UserFunctionsUpdateInput(List.of()));

        press(hrChat, approve);

        assertThat(reload(request).getStatus()).isEqualTo("PENDING");
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), SHIFT_DAY)).isPresent();
        assertThat(telegram.answers).last().isEqualTo(NotificationMessages.render(NotificationMessages.Key.BOT_NOT_ALLOWED,
                com.sunsetbeach.model.NotificationLanguage.EN, java.util.Map.of()));
    }

    @Test
    void noHr_meansTheAdministratorsDecide_andOnlyTheyHaveTheirOwnChatTold() {
        withoutHr();
        UserEntity linkedAdmin = admin("Linked Admin");
        UserEntity unlinkedAdmin = user("Unlinked Admin", Role.ADMIN);
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);

        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "x");

        assertThat(notificationsOf(linkedAdmin, "SHIFT_REQUEST_NEW")).hasSize(1);
        assertThat(notificationsOf(unlinkedAdmin, "SHIFT_REQUEST_NEW")).isEmpty();
        assertThat(deciderService.mayDecide(unlinkedAdmin, TODAY_)).isTrue(); // may decide (e.g. in the admin UI), just wasn't messaged
        assertThat(deciderService.mayDecide(employee, TODAY_)).isFalse();

        botSettings(s -> s.setFallbackToAdmin(false));
        assertThat(deciderService.mayDecide(linkedAdmin, TODAY_)).isFalse();
        assertThatThrownBy(() -> requestService.decide(request.getId(), linkedAdmin, true, null, ShiftChangeRequestChannel.ADMIN_UI))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void whileTheHrDirectorIsOnLeave_theAdministratorsDecideInsteadOfThem() {
        UserEntity admin = admin("Admin");
        deciderService.setVacation(hr, TODAY.minusDays(1), TODAY.plusDays(2), "Telegram");
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);

        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "x");

        assertThat(notificationsOf(hr, "SHIFT_REQUEST_NEW")).isEmpty();
        assertThat(notificationsOf(admin, "SHIFT_REQUEST_NEW")).hasSize(1);
        assertThatThrownBy(() -> requestService.decide(request.getId(), hr, true, null, ShiftChangeRequestChannel.TELEGRAM)).isInstanceOf(ForbiddenException.class);

        clock.set(TODAY.plusDays(3) + "T10:00"); // the leave is over
        assertThat(deciderService.mayDecide(hr, TODAY.plusDays(3))).isTrue();
    }

    // ---- the whole bot, through updates ----------------------------------------------------------------------------

    @Test
    void anEmployeeFilesAMoveInTheBot_theHrDirectorApproves_andBothSidesSeeTheOutcome() {
        entry(employee, SHIFT_DAY);

        say(employeeChat, "/shifts");
        FakeTelegramClient.Sent list = telegram.lastTo(employeeChat);
        assertThat(list.text()).contains(BotText.date(SHIFT_DAY, com.sunsetbeach.model.NotificationLanguage.EN));
        String move = FakeTelegramClient.callbackOf(list, "Move " + BotText.date(SHIFT_DAY, com.sunsetbeach.model.NotificationLanguage.EN));
        assertThat(move).isNotNull();

        press(employeeChat, move);
        String pick = FakeTelegramClient.callbackOf(telegram.lastTo(employeeChat), BotText.date(TARGET_DAY, com.sunsetbeach.model.NotificationLanguage.EN));
        assertThat(pick).isNotNull();

        press(employeeChat, pick);
        assertThat(botConversationRepository.findById(employeeChat)).isPresent();
        say(employeeChat, "Family event");

        assertThat(botConversationRepository.findById(employeeChat)).isEmpty();
        ShiftChangeRequestEntity request = shiftRequestRepository.findByEmployeeUserIdOrderByCreatedAtDesc(employee.getId(), org.springframework.data.domain.PageRequest.of(0, 5)).get(0);
        assertThat(request.getStatus()).isEqualTo("PENDING");
        assertThat(request.getReason()).isEqualTo("Family event");
        assertThat(request.getChannel()).isEqualTo("TELEGRAM");

        deliveryService.runOnce();
        FakeTelegramClient.Sent announcement = telegram.lastTo(hrChat);
        assertThat(announcement.text()).contains("Eve Employee").contains("Family event");
        press(hrChat, FakeTelegramClient.callbackOf(announcement, "Approve"));

        assertThat(reload(request).getStatus()).isEqualTo("APPROVED");
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), TARGET_DAY)).isPresent();
        // The announcement is rewritten to the outcome, its buttons gone.
        assertThat(telegram.edits).anySatisfy(e -> {
            assertThat(e.chatId()).isEqualTo(hrChat);
            assertThat(e.messageId()).isEqualTo(announcement.messageId());
            assertThat(e.text()).contains("Approved by Hana HR");
            assertThat(e.buttons()).isNullOrEmpty();
        });
        // The employee hears the decision - and, because the roster change came from the approval, nothing more.
        deliveryService.runOnce();
        assertThat(telegram.textsTo(employeeChat)).anyMatch(t -> t.startsWith("Approved:"));
        clock.set(DAY + "T10:30");
        scanService.runOnce();
        assertThat(notificationsOf(employee, "ROSTER_CHANGED")).isEmpty();
        // Audited as the HR director, marked as Telegram.
        assertThat(auditLogRepository.findAll()).anySatisfy(a -> {
            assertThat(a.getAction()).isEqualTo(AuditAction.ROSTER_ENTRY_MOVED);
            assertThat(a.getActorUserId()).isEqualTo(hr.getId());
            assertThat(a.getSummary()).endsWith("(via Telegram)");
        });
        assertThat(auditLogRepository.findAll()).anySatisfy(a -> {
            assertThat(a.getAction()).isEqualTo(AuditAction.SHIFT_CHANGE_APPROVED);
            assertThat(a.getActorUserId()).isEqualTo(hr.getId());
        });
    }

    @Test
    void aCommandTypedMidDialogue_discardsIt_andTheNextTextIsNotAReason() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        say(employeeChat, "/shifts");
        press(employeeChat, FakeTelegramClient.callbackOf(telegram.lastTo(employeeChat), "Day off " + BotText.date(SHIFT_DAY, com.sunsetbeach.model.NotificationLanguage.EN)));
        assertThat(botConversationRepository.findById(employeeChat)).isPresent();

        say(employeeChat, "/month");
        assertThat(botConversationRepository.findById(employeeChat)).isEmpty();
        say(employeeChat, "this is not a reason");

        assertThat(shiftRequestRepository.findAll().stream().filter(r -> r.getEmployeeUserId().equals(employee.getId()))).isEmpty();
        assertThat(rosterEntryRepository.findById(entry.getId())).isPresent();
    }

    @Test
    void aDialogueIsForgottenAfterFifteenMinutes() {
        entry(employee, SHIFT_DAY);
        say(employeeChat, "/shifts");
        press(employeeChat, FakeTelegramClient.callbackOf(telegram.lastTo(employeeChat), "Day off " + BotText.date(SHIFT_DAY, com.sunsetbeach.model.NotificationLanguage.EN)));

        clock.set(DAY + "T10:16");
        say(employeeChat, "late reason");

        assertThat(shiftRequestRepository.findAll().stream().filter(r -> r.getEmployeeUserId().equals(employee.getId()))).isEmpty();
    }

    @Test
    void aFunctionSwitchedOff_isRefusedAndLeftOutOfTheMenu() {
        botSettings(s -> s.setMyMonthEnabled(false));

        say(employeeChat, "/month");
        assertThat(telegram.lastTo(employeeChat).text()).isEqualTo("This function is turned off.");
        say(employeeChat, "/menu");
        assertThat(telegram.commandSets.stream().filter(c -> c.startsWith(employeeChat)).toList()).isNotEmpty();
    }

    @Test
    void myDay_showsTheBoardStateAndTheEmployeesOwnPunches() {
        entry(employee, TODAY);
        clock.set(DAY + "T09:30");
        punchIn(employee, "09:05", manager);

        say(employeeChat, "/today");

        String text = telegram.lastTo(employeeChat).text();
        assertThat(text).contains("09:05 in").contains("on shift").contains("09:00-18:00");
    }

    @Test
    void theHrDirectorSetsTheLeaveInTheBot_andTheAdministratorsTakeOver() {
        UserEntity admin = admin("Admin");
        say(hrChat, "/vacation");
        press(hrChat, FakeTelegramClient.callbackOf(telegram.lastTo(hrChat), "Set leave"));
        assertThat(botConversationRepository.findById(hrChat)).isPresent();

        say(hrChat, "not a date");
        assertThat(telegram.lastTo(hrChat).text()).contains("not a date like");
        say(hrChat, TODAY.minusDays(1).toString());
        say(hrChat, TODAY.plusDays(2).toString());

        UserEntity reloaded = userRepository.findById(hr.getId()).orElseThrow();
        assertThat(reloaded.getHrVacationFrom()).isEqualTo(TODAY.minusDays(1));
        assertThat(reloaded.getHrVacationTo()).isEqualTo(TODAY.plusDays(2));
        assertThat(deciderService.mayDecide(admin, TODAY_)).isTrue();
        assertThat(deciderService.mayDecide(hr, TODAY_)).isFalse();
        assertThat(auditLogRepository.findAll()).anySatisfy(a -> {
            assertThat(a.getAction()).isEqualTo(AuditAction.HR_VACATION_CHANGED);
            assertThat(a.getActorUserId()).isEqualTo(hr.getId());
            assertThat(a.getSummary()).endsWith("(via Telegram)");
        });

        // Someone who is not the HR director has no such command.
        say(employeeChat, "/vacation");
        assertThat(telegram.lastTo(employeeChat).text()).isEqualTo("You can't do that.");
    }

    @Test
    void anEmployeeChangesTheirLanguageAndTheEveningReminder_inTheBot_auditedAsThemselves() {
        say(employeeChat, "/settings");
        press(employeeChat, FakeTelegramClient.callbackOf(telegram.lastTo(employeeChat), "Русский"));

        assertThat(userRepository.findById(employee.getId()).orElseThrow().getNotificationLanguage()).isEqualTo("RU");
        // The screen is rewritten in place, now in Russian, with the reminder switch.
        String eve = callbackInLastEdit("Включить вечернее напоминание");
        press(employeeChat, eve);
        assertThat(userRepository.findById(employee.getId()).orElseThrow().getEveReminderEnabled()).isTrue();
        assertThat(auditLogRepository.findAll().stream().filter(a -> employee.getId().equals(a.getActorUserId()))
                .map(a -> a.getAction()).toList())
                .containsExactlyInAnyOrder(AuditAction.NOTIFICATION_LANGUAGE_UPDATED, AuditAction.STAFF_EVE_REMINDER_CHANGED);

        // The same choices again change nothing and write nothing.
        long before = auditLogRepository.findAll().stream().filter(a -> employee.getId().equals(a.getActorUserId())).count();
        press(employeeChat, callbackInLastEdit("Русский"));
        press(employeeChat, eve);
        assertThat(auditLogRepository.findAll().stream().filter(a -> employee.getId().equals(a.getActorUserId())).count()).isEqualTo(before);
    }

    private String callbackInLastEdit(String label) {
        FakeTelegramClient.Edit last = telegram.edits.get(telegram.edits.size() - 1);
        return last.buttons().stream().flatMap(List::stream).filter(b -> b.text().equals(label)).map(b -> b.callbackData()).findFirst().orElseThrow();
    }

    // ---- reminders and the roster-changed message ---------------------------------------------------------------------

    @Test
    void theEveningBeforeReminder_goesOnlyToThoseWhoAskedForIt_onceAndNotBeforeTheTime() {
        UserEntity opted = user("Opted In", Role.WAITER);
        opted.setEveReminderEnabled(true);
        userRepository.saveAndFlush(opted);
        link(opted);
        entry(opted, TODAY.plusDays(1));
        entry(employee, TODAY.plusDays(1)); // default is off: no message

        clock.set(DAY + "T17:59");
        scanService.runOnce();
        assertThat(notificationsOf(opted, "SHIFT_EVE_REMINDER")).isEmpty();

        clock.set(DAY + "T18:00");
        scanService.runOnce();
        scanService.runOnce();
        assertThat(notificationsOf(opted, "SHIFT_EVE_REMINDER")).hasSize(1);
        assertThat(notificationsOf(opted, "SHIFT_EVE_REMINDER").get(0).getText()).contains("09:00-18:00");
        assertThat(notificationsOf(employee, "SHIFT_EVE_REMINDER")).isEmpty();

        botSettings(s -> s.setEveReminderDefaultOn(true));
        scanService.runOnce();
        assertThat(notificationsOf(employee, "SHIFT_EVE_REMINDER")).hasSize(1);
    }

    @Test
    void aSeriesOfDrags_isOneMessagePerDay_withTheFinalState_afterTheDelay() {
        RosterEntryEntity original = entry(employee, TODAY.plusDays(2));
        // The creation above is itself a change of that day; start from a clean slate, as if it was made long ago.
        rosterChangeMarkRepository.deleteAll();
        actAs(manager);
        LocalDate d2 = TODAY.plusDays(3);
        LocalDate d3 = TODAY.plusDays(4);
        LocalDate d4 = TODAY.plusDays(5);
        rosterService.moveEntry(original.getId(), new RosterMoveInput(d2.toString()), manager.getId());
        rosterService.moveEntry(original.getId(), new RosterMoveInput(d3.toString()), manager.getId());
        rosterService.moveEntry(original.getId(), new RosterMoveInput(d4.toString()), manager.getId());
        SecurityContextHolderClear.clear();

        clock.set(DAY + "T10:05");
        scanService.runOnce();
        assertThat(notificationsOf(employee, "ROSTER_CHANGED")).as("not before the delay").isEmpty();

        clock.set(DAY + "T10:11");
        scanService.runOnce();
        List<com.sunsetbeach.entity.NotificationEntity> sent = notificationsOf(employee, "ROSTER_CHANGED");
        // Two days ended up different (the one it left, the one it landed on); the two it only passed through
        // are back to what they were and say nothing.
        assertThat(sent).hasSize(2);
        assertThat(sent).anySatisfy(n -> assertThat(n.getText()).contains(BotText.date(TODAY.plusDays(2), com.sunsetbeach.model.NotificationLanguage.EN)).contains("day off"));
        assertThat(sent).anySatisfy(n -> assertThat(n.getText()).contains(BotText.date(d4, com.sunsetbeach.model.NotificationLanguage.EN)).contains("day off ->"));
        scanService.runOnce();
        assertThat(notificationsOf(employee, "ROSTER_CHANGED")).hasSize(2);
        assertThat(rosterChangeMarkRepository.findAll().stream().filter(m -> m.getEmployeeUserId().equals(employee.getId()))).isEmpty();
    }

    @Test
    void anUnchangedDay_afterTheDragsStop_sendsNothing() {
        RosterEntryEntity original = entry(employee, TODAY.plusDays(2));
        rosterChangeMarkRepository.deleteAll();
        actAs(manager);
        rosterService.moveEntry(original.getId(), new RosterMoveInput(TODAY.plusDays(3).toString()), manager.getId());
        rosterService.moveEntry(original.getId(), new RosterMoveInput(TODAY.plusDays(2).toString()), manager.getId());
        SecurityContextHolderClear.clear();

        clock.set(DAY + "T10:30");
        scanService.runOnce();

        assertThat(notificationsOf(employee, "ROSTER_CHANGED")).isEmpty();
    }

    @Test
    void aRequestWhoseShiftStartedUndecided_expires_andAWaitingOneIsRemindedOncePerDay() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "x");
        jdbc.update("UPDATE \"ShiftChangeRequest\" SET \"createdAt\" = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(DAY + "T00:00")), request.getId());

        // Created at 00:00 UTC; at 20:00 hotel time (13:00 UTC) it has waited 13 h, over the 12 h default.
        clock.set(DAY + "T20:00");
        scanService.runOnce();
        scanService.runOnce();
        assertThat(notificationsOf(hr, "SHIFT_REQUEST_REMINDER")).hasSize(1);

        clock.set(TODAY.plusDays(1) + "T20:00");
        scanService.runOnce();
        assertThat(notificationsOf(hr, "SHIFT_REQUEST_REMINDER")).hasSize(2);

        clock.set(SHIFT_DAY + "T09:00");
        scanService.runOnce();
        assertThat(reload(request).getStatus()).isEqualTo("EXPIRED");
        assertThat(reload(request).getExpiredReason()).isEqualTo(ShiftRequestExpiry.SHIFT_STARTED.name());
        assertThat(notificationsOf(employee, "SHIFT_REQUEST_EXPIRED")).hasSize(1);
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), SHIFT_DAY)).isPresent();
    }

    @Test
    void theEmployeeCanWithdrawTheirOwnWaitingRequest_butNotSomeoneElses() {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "x");
        UserEntity stranger = user("Stranger", Role.WAITER);

        assertThatThrownBy(() -> requestService.cancel(request.getId(), stranger, ShiftChangeRequestChannel.TELEGRAM)).isInstanceOf(ForbiddenException.class);
        assertThat(requestService.cancel(request.getId(), employee, ShiftChangeRequestChannel.TELEGRAM).changedNow()).isTrue();
        assertThat(reload(request).getStatus()).isEqualTo("CANCELLED");
        assertThat(requestService.decide(request.getId(), hr, true, null, ShiftChangeRequestChannel.TELEGRAM).changedNow()).isFalse();
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), SHIFT_DAY)).isPresent();
    }

    // ---- settings --------------------------------------------------------------------------------------------------------

    @Test
    void anUnchangedSettingsPut_writesNoAuditEntry_aChangedOneWritesOne() {
        actAs(admin("Settings Admin"));
        var current = botSettingsService.get();
        var input = new com.sunsetbeach.model.StaffBotSettingsUpdateInput(
                current.getMyShiftsEnabled(), current.getMyDayEnabled(), current.getMyMonthEnabled(), current.getMyRequestsEnabled(),
                current.getBotSettingsEnabled(), current.getRequestsEnabled(), current.getMoveRequestsEnabled(), current.getDeleteRequestsEnabled(),
                current.getRequestMinHoursBefore(), current.getAllowTodayRequests(), current.getMoveWindowDays(), current.getReasonRequired(),
                current.getMyShiftsDays(), current.getFallbackToAdmin(), current.getPendingReminderEnabled(), current.getPendingReminderHours(),
                current.getEveReminderEnabled(), current.getEveReminderTime(), current.getEveReminderDefaultOn(),
                current.getRosterChangeEnabled(), current.getRosterChangeHorizonDays(), current.getRosterChangeDelayMinutes());
        long before = countSettingsAudit();

        botSettingsService.update(input);
        assertThat(countSettingsAudit()).isEqualTo(before);

        input.setRequestMinHoursBefore(36);
        assertThat(botSettingsService.update(input).getRequestMinHoursBefore()).isEqualTo(36);
        assertThat(countSettingsAudit()).isEqualTo(before + 1);

        input.setEveReminderTime("not a time");
        assertThatThrownBy(() -> botSettingsService.update(input)).isInstanceOf(com.sunsetbeach.error.ValidationException.class);
    }

    private long countSettingsAudit() {
        return auditLogRepository.findAll().stream().filter(a -> a.getAction() == AuditAction.STAFF_BOT_SETTINGS_UPDATED).count();
    }

    // ---- over HTTP ---------------------------------------------------------------------------------------------------------

    @Test
    void theHrDirector_readsTheRosterAndTheRequests_butCannotEditTheGrid_andAPlainManagerCannotDecide() throws Exception {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.DELETE, null, "x");
        String month = "?year=" + SHIFT_DAY.getYear() + "&month=" + SHIFT_DAY.getMonthValue();

        mockMvc.perform(as(get("/roster" + month), hr)).andExpect(status().isOk());
        mockMvc.perform(as(get("/roster/employees"), hr)).andExpect(status().isOk());
        mockMvc.perform(as(get("/shift-change-requests"), hr)).andExpect(status().isOk());
        mockMvc.perform(as(get("/shift-change-requests/deciders"), hr)).andExpect(status().isOk()).andExpect(jsonPath("$.callerMayDecide").value(true));
        // Every write to the grid stays MANAGER+.
        mockMvc.perform(as(post("/roster/entries").contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeUserId\":\"" + employee.getId() + "\",\"date\":\"" + TARGET_DAY + "\",\"shiftCodeId\":\"" + code.getId() + "\"}"), hr))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(post("/roster/generate").contentType(MediaType.APPLICATION_JSON).content("{}"), hr)).andExpect(status().isForbidden());
        // A manager reads the requests but is not a decider (no HR function, not an administrator).
        mockMvc.perform(as(get("/shift-change-requests"), manager)).andExpect(status().isOk());
        mockMvc.perform(as(post("/shift-change-requests/" + request.getId() + "/decision").contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVE\"}"), manager)).andExpect(status().isForbidden());
        // A waiter without the function reads nothing here.
        mockMvc.perform(as(get("/shift-change-requests"), employee)).andExpect(status().isForbidden());

        mockMvc.perform(as(post("/shift-change-requests/" + request.getId() + "/decision").contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVE\",\"comment\":\"ok\"}"), hr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionComment").value("ok"));
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), SHIFT_DAY)).isEmpty();
    }

    @Test
    void theRosterGrid_marksAnEntryThatHasAWaitingRequest() throws Exception {
        RosterEntryEntity entry = entry(employee, SHIFT_DAY);
        ShiftChangeRequestEntity request = file(entry, ShiftChangeRequestType.MOVE, TARGET_DAY, "x");

        mockMvc.perform(as(get("/roster?year=" + SHIFT_DAY.getYear() + "&month=" + SHIFT_DAY.getMonthValue()), manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[?(@.id == '" + entry.getId() + "')].pendingShiftRequestId").value(request.getId()))
                .andExpect(jsonPath("$.entries[?(@.id == '" + entry.getId() + "')].pendingShiftRequestType").value("MOVE"));
    }

    /** The tests set a SecurityContext only to let the roster's own audit calls write; clear it again. */
    private static final class SecurityContextHolderClear {
        static void clear() {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
