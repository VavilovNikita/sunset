package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftCode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Late / absence / terminal-silence notifications. "Late" itself is {@code AttendanceService}'s
 * definition (its own tests cover the states); what is tested here is what the scanner adds: the
 * configured thresholds, who is told, that a pass repeated or raced raises nothing twice, and that a
 * terminal which has gone quiet turns every roster alert off - a false "late" is worse than none.
 */
class NotificationScanTests extends NotificationTestBase {

    private UserEntity manager;
    private UserEntity director;
    private UserEntity supervisor;
    private UserEntity employee;
    private AttendanceDeviceEntity terminal;

    @BeforeEach
    void people() {
        manager = user("Mgr", Role.MANAGER);
        director = user("Director", Role.ADMIN);
        supervisor = user("Supervisor", Role.MANAGER);
        employee = user("Somchai", Role.WAITER);
        for (UserEntity u : List.of(employee, supervisor, director)) {
            link(u);
        }
        settings(s -> {
            s.setLateRecipientUserIds(new String[] {supervisor.getId()});
            s.setDirectorUserIds(new String[] {director.getId()});
            s.setQuietHoursEnabled(false);
        });
        terminal = healthyDevice("Front desk terminal");
    }

    /** The terminal answered at the given hotel time; keeps it from being "silent" as the clock moves. */
    private void terminalSeenAt(String time) {
        touchDevice(terminal, "2033-05-17T" + time);
    }

    private void at(String time) {
        clock.set("2033-05-17T" + time);
        terminalSeenAt(time);
    }

    private ShiftCode nineToFive() {
        return shiftCode("09:00", "17:00", manager);
    }

    @Test
    void nothingBeforeTheGrace_thenOneMessageToThePersonAndOneToEachConfiguredRecipient() {
        roster(employee, nineToFive(), manager);

        at("09:09");
        scanService.runOnce();
        assertThat(notificationsOf(employee, "LATE")).isEmpty();

        at("09:10");
        scanService.runOnce();
        assertThat(notificationsOf(employee, "LATE")).hasSize(1);
        assertThat(notificationsOf(employee, "LATE").get(0).getText()).startsWith("You are late: your shift started at 09:00");
        assertThat(notificationsOf(supervisor, "LATE")).hasSize(1);
        assertThat(notificationsOf(supervisor, "LATE").get(0).getText()).startsWith("Somchai is late: the shift started at 09:00");
        assertThat(notificationsOf(director, "LATE")).isEmpty(); // directors hear about absence, not lateness
    }

    @Test
    void aRepeatedPass_raisesNothingNew() {
        roster(employee, nineToFive(), manager);
        at("09:30");

        scanService.runOnce();
        scanService.runOnce();
        at("09:31");
        scanService.runOnce();

        assertThat(notificationsOf(employee, "LATE")).hasSize(1);
        assertThat(notificationsOf(supervisor, "LATE")).hasSize(1);
    }

    @Test
    void twoScannersRacingOnTheSameLateness_raiseItOnce() throws Exception {
        roster(employee, nineToFive(), manager);
        at("09:30");
        int threads = 6;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    scanService.runOnce();
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(notificationsOf(employee, "LATE")).hasSize(1);
        assertThat(notificationsOf(supervisor, "LATE")).hasSize(1);
        assertThat(notificationsOf(director, "ABSENT")).isEmpty(); // 30 minutes in, below the 60
    }

    @Test
    void someoneWhoClockedInInTime_isNeverLate() {
        roster(employee, nineToFive(), manager);
        punchIn(employee, "09:04", manager);

        at("09:40");
        scanService.runOnce();

        assertThat(notificationsOf(employee)).isEmpty();
        assertThat(notificationsOf(supervisor)).isEmpty();
    }

    @Test
    void absence_goesToTheDirectorsOnly_afterTheAbsenceThreshold_andNotToTheAbsentPerson() {
        roster(employee, nineToFive(), manager);

        at("09:59");
        scanService.runOnce();
        assertThat(notificationsOf(director, "ABSENT")).isEmpty();

        at("10:00");
        scanService.runOnce();
        assertThat(notificationsOf(director, "ABSENT")).hasSize(1);
        assertThat(notificationsOf(director, "ABSENT").get(0).getText()).startsWith("Somchai has not shown up");
        assertThat(notificationsOf(employee, "ABSENT")).isEmpty();
        assertThat(notificationsOf(supervisor, "ABSENT")).isEmpty();

        at("12:00");
        scanService.runOnce();
        assertThat(notificationsOf(director, "ABSENT")).hasSize(1);
    }

    @Test
    void aDirectorWhoIsTheAbsentOne_isNotToldAboutThemselves() {
        roster(director, nineToFive(), manager);
        at("10:30");

        scanService.runOnce();

        assertThat(notificationsOf(director, "ABSENT")).isEmpty();
    }

    @Test
    void afterTheIntervalEnded_noLateMessage_butAbsenceStillReaches() {
        roster(employee, shiftCode("06:00", "08:00", manager), manager);
        at("10:00"); // the interval is over: MISSED, not LATE

        scanService.runOnce();

        assertThat(notificationsOf(employee, "LATE")).isEmpty();
        assertThat(notificationsOf(director, "ABSENT")).hasSize(1);
    }

    @Test
    void theSettingsDecide_disabledAlertsAreNotRaised_andTheGraceIsConfigurable() {
        roster(employee, nineToFive(), manager);
        settings(s -> s.setLateGraceMinutes(25));
        at("09:24");
        scanService.runOnce();
        assertThat(notificationsOf(employee, "LATE")).isEmpty();

        at("09:25");
        scanService.runOnce();
        assertThat(notificationsOf(employee, "LATE")).hasSize(1);

        settings(s -> {
            s.setLateEnabled(false);
            s.setAbsenceEnabled(false);
        });
        at("11:00");
        scanService.runOnce();
        assertThat(notificationsOf(director, "ABSENT")).isEmpty();
    }

    @Test
    void aSecondIntervalOfASplitShift_isItsOwnLateness() {
        ShiftCode split = shiftCodeService.create(
                new com.sunsetbeach.model.ShiftCodeCreateInput("S" + java.util.UUID.randomUUID().toString().substring(0, 6), com.sunsetbeach.model.ShiftCodeKind.SPLIT, true, true, "2020-01-01")
                        .staffArea(com.sunsetbeach.model.StaffArea.RESTAURANT)
                        .startTime1("07:00")
                        .endTime1("11:00")
                        .startTime2("15:00")
                        .endTime2("19:00"),
                manager.getId());
        shiftCodeIds.add(split.getId());
        roster(employee, split, manager);
        at("15:20"); // a punch can't be recorded in the future, so the day is lived through first
        punchIn(employee, "07:00", manager);
        attendanceService.recordPunch(
                new com.sunsetbeach.model.AttendancePunchCreateInput(
                        employee.getId(), java.time.OffsetDateTime.parse("2033-05-17T11:00:00+07:00"), com.sunsetbeach.model.PunchDirection.OUT),
                manager.getId());

        scanService.runOnce();

        assertThat(notificationsOf(employee, "LATE")).hasSize(1);
    }

    // ---- arrival after an absence message ---------------------------------------------------------

    @Test
    void clockingInAfterTheAbsenceMessage_tellsTheSameRecipientsOnce_inTheirLanguage() {
        director.setNotificationLanguage("RU");
        userRepository.saveAndFlush(director);
        roster(employee, nineToFive(), manager);
        at("10:00");
        scanService.runOnce();
        assertThat(notificationsOf(director, "ABSENT")).hasSize(1);

        at("10:20");
        punchIn(employee, "10:12", manager);
        scanService.runOnce();
        scanService.runOnce();
        at("10:40");
        scanService.runOnce();

        var arrived = notificationsOf(director, "ABSENT").stream().filter(n -> n.getMessageKey().equals("ARRIVED_AFTER_ABSENT")).toList();
        assertThat(arrived).hasSize(1);
        assertThat(arrived.get(0).getText()).startsWith("Somchai пришёл в 10:12 (опоздание 72 мин)");
        assertThat(notificationsOf(supervisor, "ABSENT")).isEmpty(); // not an absence recipient
        assertThat(notificationsOf(employee, "ABSENT")).isEmpty();
    }

    @Test
    void clockingInWithNoAbsenceMessage_saysNothing() {
        roster(employee, nineToFive(), manager);
        at("09:40");
        punchIn(employee, "09:35", manager);

        scanService.runOnce();
        at("10:30");
        scanService.runOnce();

        assertThat(notificationsOf(director)).isEmpty();
        assertThat(notificationsOf(supervisor, "ABSENT")).isEmpty();
    }

    @Test
    void stillAbsent_isNotAnArrival() {
        roster(employee, nineToFive(), manager);
        at("10:30");
        scanService.runOnce();
        scanService.runOnce();

        assertThat(notificationsOf(director, "ABSENT")).hasSize(1).allSatisfy(n -> assertThat(n.getMessageKey()).isEqualTo("ABSENT"));
    }

    // ---- the terminal -------------------------------------------------------------------------------

    @Test
    void aSilentTerminal_turnsLateAndAbsenceOff_andTheDirectorsAreToldOnce() {
        roster(employee, nineToFive(), manager);
        touchDevice(terminal, "2033-05-17T09:00");
        clock.set("2033-05-17T10:30"); // 90 minutes of silence, threshold 15

        scanService.runOnce();
        scanService.runOnce();

        assertThat(notificationsOf(employee, "LATE")).isEmpty();
        assertThat(notificationsOf(supervisor, "LATE")).isEmpty();
        assertThat(notificationsOf(director, "ABSENT")).isEmpty();
        assertThat(notificationsOf(director, "TERMINAL_OFFLINE")).hasSize(1);
        assertThat(notificationsOf(director, "TERMINAL_OFFLINE").get(0).getText()).contains("Front desk terminal").contains("has not responded since 09:00");
    }

    @Test
    void justUnderTheSilenceThreshold_isStillHealthy() {
        roster(employee, nineToFive(), manager);
        touchDevice(terminal, "2033-05-17T09:15");
        clock.set("2033-05-17T09:30"); // exactly 15 minutes: not longer than the threshold

        scanService.runOnce();

        assertThat(notificationsOf(director, "TERMINAL_OFFLINE")).isEmpty();
        assertThat(notificationsOf(employee, "LATE")).hasSize(1);
    }

    @Test
    void whenTheTerminalIsBack_thereIsOneBackOnlineMessage_andLatenessResumes() {
        roster(employee, nineToFive(), manager);
        touchDevice(terminal, "2033-05-17T09:00");
        clock.set("2033-05-17T10:30");
        scanService.runOnce();
        assertThat(notificationsOf(employee, "LATE")).isEmpty();

        at("10:45");
        scanService.runOnce();
        scanService.runOnce();

        assertThat(notificationsOf(director, "TERMINAL_ONLINE")).hasSize(1);
        assertThat(notificationsOf(employee, "LATE")).hasSize(1); // the guard lifted
        assertThat(notificationsOf(director, "TERMINAL_OFFLINE")).hasSize(1);
    }

    @Test
    void aNewOutageIsANewEpisode_withItsOwnMessage() {
        touchDevice(terminal, "2033-05-17T09:00");
        clock.set("2033-05-17T10:00");
        scanService.runOnce();
        at("10:05");
        scanService.runOnce();
        touchDevice(terminal, "2033-05-17T10:05");
        clock.set("2033-05-17T11:00");
        scanService.runOnce();

        assertThat(notificationsOf(director, "TERMINAL_OFFLINE")).hasSize(2);
        assertThat(notificationsOf(director, "TERMINAL_ONLINE")).hasSize(1);
    }

    @Test
    void oneSilentTerminalOfTwo_isEnoughToSuppress() {
        roster(employee, nineToFive(), manager);
        AttendanceDeviceEntity second = healthyDevice("Kitchen terminal");
        touchDevice(second, "2033-05-17T08:00");
        at("09:40"); // the first is fresh, the second silent since 08:00

        scanService.runOnce();

        assertThat(notificationsOf(employee, "LATE")).isEmpty();
        assertThat(notificationsOf(director, "TERMINAL_OFFLINE")).hasSize(1);
        assertThat(notificationsOf(director, "TERMINAL_OFFLINE").get(0).getText()).contains("Kitchen terminal");
    }

    @Test
    void noActiveTerminalAtAll_meansNoRosterAlerts() {
        deviceRepository.deleteAllById(deviceIds);
        deviceIds.clear();
        roster(employee, nineToFive(), manager);
        clock.set("2033-05-17T11:00");

        scanService.runOnce();

        assertThat(notificationsOf(employee)).isEmpty();
        assertThat(notificationsOf(supervisor)).isEmpty();
        assertThat(notificationsOf(director)).isEmpty();
    }

    @Test
    void terminalAlertsOff_stillSuppressesTheRosterAlerts_butSaysNothing() {
        settings(s -> s.setTerminalAlertsEnabled(false));
        roster(employee, nineToFive(), manager);
        touchDevice(terminal, "2033-05-17T09:00");
        clock.set("2033-05-17T10:30");

        scanService.runOnce();

        assertThat(notificationsOf(employee, "LATE")).isEmpty();
        assertThat(notificationsOf(director)).isEmpty();
    }

    @Test
    void aTerminalThatWasSwitchedOff_isNotReportedAsBackOnline() {
        touchDevice(terminal, "2033-05-17T09:00");
        clock.set("2033-05-17T10:00");
        scanService.runOnce();
        assertThat(notificationsOf(director, "TERMINAL_OFFLINE")).hasSize(1);

        AttendanceDeviceEntity off = deviceRepository.findById(terminal.getId()).orElseThrow();
        off.setActive(false);
        deviceRepository.saveAndFlush(off);
        scanService.runOnce();

        assertThat(notificationsOf(director, "TERMINAL_ONLINE")).isEmpty();
        assertThat(episodeRepository.findAll()).isEmpty();
    }

    @Test
    void anAlertAtNight_isHeldUntilMorning() {
        settings(s -> s.setQuietHoursEnabled(true));
        touchDevice(terminal, "2033-05-17T20:00");
        clock.set("2033-05-17T23:00");

        scanService.runOnce();

        assertThat(notificationsOf(director, "TERMINAL_OFFLINE")).hasSize(1);
        var deliveries = deliveryRepository.findByNotificationIdIn(notificationsOf(director, "TERMINAL_OFFLINE").stream().map(n -> n.getId()).toList());
        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.get(0).getNextAttemptAt()).isEqualTo(java.time.LocalDateTime.parse("2033-05-18T00:00"));
    }
}
