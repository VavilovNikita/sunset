package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.BlockResponsibleFunction;
import com.sunsetbeach.model.MaintenanceTask;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitBlockInput;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Room-block notifications: who is told (on shift now + holds the block's function, otherwise the
 * directors), and the morning reminders. "On shift" is the attendance board's own ON_SHIFT, so a
 * person is on shift here the way they are on {@code GET /attendance/today}: rostered and punched in.
 */
class NotificationBlockTests extends NotificationTestBase {

    private UserEntity manager;
    private UserEntity director;
    private UserEntity engineerOnShift;
    private UserEntity engineerNotIn;
    private UserEntity housekeeperOnShift;
    private RoomUnitEntity unit;

    @BeforeEach
    void people() {
        manager = user("Mgr", Role.MANAGER);
        director = user("Director", Role.ADMIN);
        engineerOnShift = user("Eng On", Role.WAITER, "ENGINEER");
        engineerNotIn = user("Eng Off", Role.WAITER, "ENGINEER");
        housekeeperOnShift = user("Maid On", Role.WAITER, "HOUSEKEEPER");
        for (UserEntity u : List.of(director, engineerOnShift, engineerNotIn, housekeeperOnShift)) {
            link(u);
        }
        settings(s -> {
            s.setDirectorUserIds(new String[] {director.getId()});
            s.setQuietHoursEnabled(false);
        });
        unit = roomUnit();
        var code = shiftCode("08:00", "17:00", manager);
        roster(engineerOnShift, code, manager);
        roster(engineerNotIn, code, manager); // rostered, but never clocked in
        roster(housekeeperOnShift, code, manager);
        punchIn(engineerOnShift, "08:00", manager);
        punchIn(housekeeperOnShift, "08:05", manager);
        healthyDevice("Terminal");
    }

    private RoomUnitBlockEntity block(String from, String to, BlockResponsibleFunction function) {
        RoomUnitBlockInput input = new RoomUnitBlockInput(from, to, "Broken air conditioner");
        if (function != null) {
            input.responsibleFunction(function);
        }
        String id = roomUnitService.createBlock(unit.getId(), input, manager.getId()).getBlock().getId();
        blockIds.add(id);
        return blockRepository.findById(id).orElseThrow();
    }

    @Test
    void createdBlock_goesToThoseOnShiftWithTheFunction_notToOthers() {
        block(DAY, "2033-05-20", BlockResponsibleFunction.ENGINEER);

        assertThat(notificationsOf(engineerOnShift, "BLOCK_CREATED")).hasSize(1);
        assertThat(notificationsOf(engineerOnShift, "BLOCK_CREATED").get(0).getText()).contains("Room " + unit.getLabel());
        assertThat(notificationsOf(engineerNotIn)).isEmpty(); // has the function, is not at work
        assertThat(notificationsOf(housekeeperOnShift)).isEmpty(); // at work, wrong function
        assertThat(notificationsOf(director)).isEmpty(); // somebody qualified exists
    }

    @Test
    void aHousekeeperBlock_goesToTheHousekeeper() {
        block(DAY, "2033-05-20", BlockResponsibleFunction.HOUSEKEEPER);

        assertThat(notificationsOf(housekeeperOnShift, "BLOCK_CREATED")).hasSize(1);
        assertThat(notificationsOf(engineerOnShift)).isEmpty();
    }

    @Test
    void theFunctionDefaultsToEngineer() {
        RoomUnitBlockEntity created = block(DAY, "2033-05-20", null);

        assertThat(created.getResponsibleFunction()).isEqualTo("ENGINEER");
        assertThat(notificationsOf(engineerOnShift, "BLOCK_CREATED")).hasSize(1);
    }

    @Test
    void nobodyOnShiftWithTheFunction_fallsBackToTheDirectors() {
        // Nobody holds it at all: housekeeper on shift is the only one at work, the engineers are off.
        punchRepository.deleteAll(punchRepository.findAll().stream().filter(p -> p.getEmployeeUserId().equals(engineerOnShift.getId())).toList());

        block(DAY, "2033-05-20", BlockResponsibleFunction.ENGINEER);

        assertThat(notificationsOf(engineerOnShift)).isEmpty();
        assertThat(notificationsOf(director, "BLOCK_CREATED")).hasSize(1);
    }

    @Test
    void whenNoOneAtAllIsOnShift_theDirectorsGetIt() {
        punchRepository.deleteAll(punchRepository.findAll().stream().filter(p -> userIds.contains(p.getEmployeeUserId())).toList());

        block(DAY, "2033-05-20", BlockResponsibleFunction.HOUSEKEEPER);

        assertThat(notificationsOf(director, "BLOCK_CREATED")).hasSize(1);
        assertThat(notificationsOf(housekeeperOnShift)).isEmpty();
    }

    @Test
    void anOnShiftEngineerWhoIsDeactivated_isNotTold() {
        engineerOnShift.setActive(false);
        userRepository.saveAndFlush(engineerOnShift);

        block(DAY, "2033-05-20", BlockResponsibleFunction.ENGINEER);

        assertThat(notificationsOf(engineerOnShift)).isEmpty();
        assertThat(notificationsOf(director, "BLOCK_CREATED")).hasSize(1);
    }

    @Test
    void aBlockWithoutAFunction_isTheDirectorsMatter() {
        RoomUnitBlockEntity old = block(DAY, "2033-05-20", BlockResponsibleFunction.ENGINEER);
        notificationRepository.deleteAll(notificationRepository.findAll().stream().filter(n -> n.getDedupKey().contains(old.getId())).toList());
        old.setResponsibleFunction(null); // as a block from before V148
        blockRepository.saveAndFlush(old);
        clock.set(DAY + "T09:00");
        // Its last day is the 20th; make today that day.
        clock.set("2033-05-20T09:00");
        touchAll("2033-05-20T09:00");

        scanService.runOnce();

        assertThat(notificationsOf(director, "BLOCK_LAST_DAY")).hasSize(1);
        assertThat(notificationsOf(engineerOnShift, "BLOCK_LAST_DAY")).isEmpty();
    }

    private void touchAll(String hotelLocal) {
        deviceRepository.findAllById(deviceIds).forEach(d -> touchDevice(d, hotelLocal));
    }

    @Test
    void aFailingNotification_neverFailsTheBlockThatWasCreated() {
        telegram.failure = c -> new com.sunsetbeach.notification.TelegramClient.TelegramException(
                com.sunsetbeach.notification.TelegramClient.TelegramException.Kind.UNAVAILABLE, "Telegram did not answer");
        deliveryService.runOnce();

        RoomUnitBlockEntity created = block(DAY, "2033-05-20", BlockResponsibleFunction.ENGINEER);

        assertThat(blockRepository.findById(created.getId())).isPresent();
        deliveryService.runOnce(); // delivery fails later, on its own row
        assertThat(blockRepository.findById(created.getId())).isPresent();
    }

    @Test
    void createdBlockOff_raisesNothing() {
        settings(s -> s.setBlockCreatedEnabled(false));

        block(DAY, "2033-05-20", BlockResponsibleFunction.ENGINEER);

        assertThat(notificationsOf(engineerOnShift)).isEmpty();
        assertThat(notificationsOf(director)).isEmpty();
    }

    // ---- the morning reminders ----------------------------------------------------------------------

    @Test
    void lastDay_isAnnouncedFromTheMorningTimeOn_onceThatDay() {
        block("2033-05-15", DAY, BlockResponsibleFunction.ENGINEER);
        notificationRepository.deleteAll(notificationsOf(engineerOnShift)); // the creation message, not under test

        clock.set(DAY + "T08:59");
        touchAll(DAY + "T08:59");
        scanService.runOnce();
        assertThat(notificationsOf(engineerOnShift, "BLOCK_LAST_DAY")).isEmpty();

        clock.set(DAY + "T09:00");
        touchAll(DAY + "T09:00");
        scanService.runOnce();
        scanService.runOnce();
        clock.set(DAY + "T15:00");
        touchAll(DAY + "T15:00");
        scanService.runOnce();

        assertThat(notificationsOf(engineerOnShift, "BLOCK_LAST_DAY")).hasSize(1);
        assertThat(notificationsOf(engineerOnShift, "BLOCK_LAST_DAY").get(0).getText()).contains("last day of the block");
    }

    @Test
    void theMorningTimeIsConfigurable() {
        block("2033-05-15", DAY, BlockResponsibleFunction.ENGINEER);
        settings(s -> s.setMorningTime(java.time.LocalTime.of(11, 30)));

        clock.set(DAY + "T11:29");
        touchAll(DAY + "T11:29");
        scanService.runOnce();
        assertThat(notificationsOf(engineerOnShift, "BLOCK_LAST_DAY")).isEmpty();

        clock.set(DAY + "T11:30");
        touchAll(DAY + "T11:30");
        scanService.runOnce();
        assertThat(notificationsOf(engineerOnShift, "BLOCK_LAST_DAY")).hasSize(1);
    }

    @Test
    void anExpiredBlockWithAnOpenTask_isRemindedEveryMorning_untilTheTaskIsDone() {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), "AC is broken", List.of(), manager.getId());
        RoomUnitBlockInput input = new RoomUnitBlockInput("2033-05-10", "2033-05-14", "Broken air conditioner");
        var result = maintenanceTaskService.addBlock(task.getId(), input, manager.getId());
        blockIds.add(result.getBlockResult().getBlock().getId());
        notificationRepository.deleteAll(notificationsOf(engineerOnShift));

        clock.set(DAY + "T09:00");
        touchAll(DAY + "T09:00");
        scanService.runOnce();
        scanService.runOnce();
        assertThat(notificationsOf(engineerOnShift, "BLOCK_EXPIRED_OPEN")).hasSize(1);

        // The next morning is a new reminder (the engineer is not on shift that day, so the directors).
        clock.set("2033-05-18T09:00");
        touchAll("2033-05-18T09:00");
        scanService.runOnce();
        assertThat(notificationsOf(director, "BLOCK_EXPIRED_OPEN")).hasSize(1);
        assertThat(notificationsOf(engineerOnShift, "BLOCK_EXPIRED_OPEN")).hasSize(1);

        maintenanceTaskService.updateStatus(task.getId(), MaintenanceTaskStatus.DONE);
        clock.set("2033-05-19T09:00");
        touchAll("2033-05-19T09:00");
        scanService.runOnce();
        assertThat(notificationsOf(director, "BLOCK_EXPIRED_OPEN")).hasSize(1); // no new one once the task is closed
    }

    @Test
    void anExpiredBlockWithNoTask_isNotRemindedAbout() {
        block("2033-05-10", "2033-05-14", BlockResponsibleFunction.ENGINEER);
        notificationRepository.deleteAll(notificationsOf(engineerOnShift));
        clock.set(DAY + "T09:30");
        touchAll(DAY + "T09:30");

        scanService.runOnce();

        assertThat(notificationsOf(engineerOnShift, "BLOCK_EXPIRED_OPEN")).isEmpty();
        assertThat(notificationsOf(director, "BLOCK_EXPIRED_OPEN")).isEmpty();
    }

    @Test
    void aBlockThatEndsTomorrow_isNotAnnouncedToday() {
        block("2033-05-15", LocalDate.parse(DAY).plusDays(1).toString(), BlockResponsibleFunction.ENGINEER);
        notificationRepository.deleteAll(notificationsOf(engineerOnShift));
        clock.set(DAY + "T12:00");
        touchAll(DAY + "T12:00");

        scanService.runOnce();

        assertThat(notificationsOf(engineerOnShift, "BLOCK_LAST_DAY")).isEmpty();
    }
}
