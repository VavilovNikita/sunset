package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.StaffBotSettingsUpdateInput;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.notification.TelegramClient.Command;
import com.sunsetbeach.notification.TelegramClient.TelegramUpdate;
import com.sunsetbeach.support.FakeTelegramClient;
import com.sunsetbeach.support.FakeTelegramClient.CommandList;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The menu under the input field and the "/" list follow a person's language and the screens that exist: at once
 * when the person changes it in the bot, with the next reply (the "/" list at once) when a manager or an
 * administrator changes it elsewhere. Over a Telegram that records what it was given.
 */
class StaffBotMenuTests extends NotificationTestBase {

    @Autowired private StaffBotSettingsService botSettingsService;

    private UserEntity employee;
    private String chat;
    private long nextUpdate = 1;

    @BeforeEach
    void person() {
        employee = user("Eve Employee", Role.WAITER);
        chat = link(employee);
    }

    private void say(String text) {
        telegram.incoming.add(new TelegramUpdate(nextUpdate++, chat, text));
        pollService.pollOnce();
    }

    private void press(String callbackData) {
        telegram.incoming.add(new TelegramUpdate(nextUpdate++, chat, null, "cq" + nextUpdate, callbackData, "1"));
        pollService.pollOnce();
    }

    private static String t(Key key, NotificationLanguage language) {
        return NotificationMessages.render(key, language, Map.of());
    }

    private List<String> lastKeyboardButtons() {
        List<FakeTelegramClient.Keyboard> mine = telegram.keyboards.stream().filter(k -> k.chatId().equals(chat)).toList();
        return mine.get(mine.size() - 1).rows().stream().flatMap(List::stream).toList();
    }

    private CommandList lastCommandList() {
        return telegram.commandLists.stream().filter(c -> chat.equals(c.chatId())).reduce((a, b) -> b).orElse(null);
    }

    private void awaitCommandListIn(NotificationLanguage language) {
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            CommandList last = lastCommandList();
            assertThat(last).isNotNull();
            assertThat(last.commands()).extracting(Command::description).contains(t(Key.MENU_SETTINGS, language));
        });
    }

    private void admin(Runnable change) {
        UserEntity admin = user("Settings Admin", Role.ADMIN);
        actAs(admin);
        try {
            change.run();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void languageChangedInTheBot_answersWithTheNewKeyboardAndCommandsAtOnce() {
        say("/menu");
        assertThat(lastKeyboardButtons()).contains(t(Key.MENU_SETTINGS, NotificationLanguage.EN));

        say("/settings");
        String russian = FakeTelegramClient.callbackOf(telegram.lastTo(chat), "Русский");
        assertThat(russian).isNotNull();
        telegram.commandLists.clear();
        press(russian);

        // A new message carries the keyboard (Telegram replaces it no other way) and it is the new language's.
        assertThat(telegram.lastTo(chat).text()).isEqualTo(t(Key.BOT_MENU, NotificationLanguage.RU));
        assertThat(lastKeyboardButtons()).contains(t(Key.MENU_SETTINGS, NotificationLanguage.RU)).doesNotContain(t(Key.MENU_SETTINGS, NotificationLanguage.EN));
        // ... and this chat's "/" list in the same breath, not on a later /menu.
        assertThat(lastCommandList().commands()).extracting(Command::description).contains(t(Key.MENU_SETTINGS, NotificationLanguage.RU));
    }

    @Test
    void languageChangedByAManager_renewsTheSlashListAtOnce_andTheKeyboardWithTheNextReply() {
        say("/menu");
        int messagesBefore = telegram.textsTo(chat).size();
        int keyboardsBefore = telegram.keyboards.size();
        telegram.commandLists.clear();

        channelService.setLanguage(employee.getId(), NotificationLanguage.RU);

        awaitCommandListIn(NotificationLanguage.RU);
        // Nothing was sent to the person just to change a keyboard.
        assertThat(telegram.textsTo(chat)).hasSize(messagesBefore);
        assertThat(telegram.keyboards).hasSize(keyboardsBefore);

        // Their next message is answered in the new language, on the new keyboard - no extra message for it.
        say("/summary"); // not theirs: a plain refusal
        assertThat(telegram.textsTo(chat)).hasSize(messagesBefore + 1);
        assertThat(telegram.lastTo(chat).text()).isEqualTo(t(Key.BOT_NOT_ALLOWED, NotificationLanguage.RU));
        assertThat(lastKeyboardButtons()).contains(t(Key.MENU_SETTINGS, NotificationLanguage.RU));

        // Once renewed it is not sent again.
        say("/summary");
        assertThat(telegram.keyboards).hasSize(keyboardsBefore + 1);
    }

    @Test
    void anOldKeyboardButtonStillWorks_andTheNewKeyboardFollowsTheAnswer() {
        say("/menu");
        channelService.setLanguage(employee.getId(), NotificationLanguage.RU);
        awaitCommandListIn(NotificationLanguage.RU);
        int keyboardsBefore = telegram.keyboards.size();

        // The phone still shows the English keyboard; the person taps its "Settings".
        say(t(Key.MENU_SETTINGS, NotificationLanguage.EN));

        // The screen opens (in their language now) ...
        assertThat(telegram.sent.stream().filter(s -> s.chatId().equals(chat) && s.buttons() != null)).isNotEmpty();
        assertThat(telegram.lastTo(chat).text()).isEqualTo(t(Key.BOT_MENU, NotificationLanguage.RU));
        // ... and, as that answer is made of buttons, the new keyboard arrives as its own message.
        assertThat(telegram.keyboards).hasSize(keyboardsBefore + 1);
        assertThat(lastKeyboardButtons()).contains(t(Key.MENU_SETTINGS, NotificationLanguage.RU));
    }

    @Test
    void aScreenSwitchedOffByAnAdministrator_renewsEveryLinkedChatsSlashList() {
        say("/menu");
        assertThat(lastCommandList().commands()).extracting(Command::description).contains(t(Key.MENU_SETTINGS, NotificationLanguage.EN));
        int keyboardsBefore = telegram.keyboards.size();
        telegram.commandLists.clear();

        admin(() -> {
            StaffBotSettingsUpdateInput input = currentSettingsInput();
            input.setBotSettingsEnabled(false);
            botSettingsService.update(input);
        });

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            CommandList last = lastCommandList();
            assertThat(last).isNotNull();
            assertThat(last.commands()).extracting(Command::description).doesNotContain(t(Key.MENU_SETTINGS, NotificationLanguage.EN));
        });
        assertThat(telegram.keyboards).as("the keyboard waits for the next reply").hasSize(keyboardsBefore);

        say("/summary");
        assertThat(lastKeyboardButtons()).doesNotContain(t(Key.MENU_SETTINGS, NotificationLanguage.EN));

        // Switching it back on is the same, the other way.
        telegram.commandLists.clear();
        admin(() -> {
            StaffBotSettingsUpdateInput input = currentSettingsInput();
            input.setBotSettingsEnabled(true);
            botSettingsService.update(input);
        });
        awaitCommandListIn(NotificationLanguage.EN);
        say("/summary");
        assertThat(lastKeyboardButtons()).contains(t(Key.MENU_SETTINGS, NotificationLanguage.EN));
    }

    @Test
    void anUnchangedLanguage_touchesNoChat() {
        say("/menu");
        telegram.commandLists.clear();
        channelService.setLanguage(employee.getId(), null); // still the default
        // The refresh is asynchronous: give a (wrongly) scheduled one the time to show itself.
        Awaitility.await().pollDelay(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(telegram.commandLists).isEmpty());
    }

    private StaffBotSettingsUpdateInput currentSettingsInput() {
        var current = botSettingsService.get();
        return new StaffBotSettingsUpdateInput(
                current.getMyShiftsEnabled(), current.getMyDayEnabled(), current.getMyMonthEnabled(), current.getMyRequestsEnabled(),
                current.getBotSettingsEnabled(), current.getRequestsEnabled(), current.getMoveRequestsEnabled(), current.getDeleteRequestsEnabled(),
                current.getRequestMinHoursBefore(), current.getAllowTodayRequests(), current.getMoveWindowDays(), current.getMoveMaxDaysAhead(), current.getReasonRequired(),
                current.getMyShiftsDays(), current.getFallbackToAdmin(), current.getPendingReminderEnabled(), current.getPendingReminderHours(),
                current.getEveReminderEnabled(), current.getEveReminderTime(), current.getEveReminderDefaultOn(),
                current.getRosterChangeEnabled(), current.getRosterChangeHorizonDays(), current.getRosterChangeDelayMinutes(),
                current.getEngineerTasksEnabled(), current.getHousekeeperBlocksEnabled(), current.getHousekeeperBlocksDays(),
                current.getTherapistScheduleEnabled(), current.getTherapistScheduleDays(),
                current.getTherapistMorningEnabled(), current.getTherapistMorningTime(), current.getTherapistMorningDefaultOn(),
                current.getWhoIsOnShiftEnabled(), current.getWhoIsOnShiftForHr(), current.getSummaryCommandEnabled(),
                current.getMorningSummaryEnabled(), current.getMorningSummaryTime(),
                current.getSummaryArrivals(), current.getSummaryDepartures(), current.getSummaryInHouse(), current.getSummaryOccupancy(),
                current.getSummaryRooms(), current.getSummaryStaff(), current.getSummaryRequests());
    }
}
