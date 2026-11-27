package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.controller.SettingsController;
import com.sunsetbeach.entity.NotificationEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.NotificationSettings;
import com.sunsetbeach.model.NotificationSettingsUpdateInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.notification.ManualUpdateFeed;
import com.sunsetbeach.repository.ManualUpdateEntryRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

/**
 * Who hears about a new note of the staff manual's "What's new", and how often: once per note, one message a hotel day
 * at most, never history, never what was published while switched off. The hotel clock sits on 2033-05-17 10:00.
 */
class ManualUpdateTests extends NotificationTestBase {

    private static final String YESTERDAY = "2033-05-16";
    private static final String TOMORROW = "2033-05-18";
    private static final String BASE = "https://sunsetsamui.com/manual";

    @Autowired private ManualUpdateService manualUpdates;
    @Autowired private ManualUpdateEntryRepository entryRepository;
    @Autowired private SettingsController settingsController;

    private UserEntity director;

    @BeforeEach
    void oneDirector() {
        clean();
        director = user("Director", Role.ADMIN);
        directors(director);
    }

    @AfterEach
    void clean() {
        entryRepository.deleteAll();
        try {
            Files.deleteIfExists(MANUAL_FEED);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---- who is told, and with what -------------------------------------------------------------------

    @Test
    void aNewNote_isToldOnceToEveryDirector_inTheirLanguage() {
        UserEntity thai = speaking("TH", user("Thai director", Role.MANAGER));
        UserEntity russian = speaking("RU", user("Russian director", Role.MANAGER));
        UserEntity bystander = user("Bystander", Role.ADMIN);
        directors(director, thai, russian);
        publish(note(DAY, DAY, "Gym hours", "You can now set the hours."));

        manualUpdates.runOnce();

        assertThat(texts(director)).containsExactly("Staff manual updated: Gym hours. You can now set the hours.\n" + BASE + "/en/updates/" + DAY + ".html");
        assertThat(texts(thai)).containsExactly("คู่มือพนักงานมีการอัปเดต: Gym hours TH. You can now set the hours. TH\n" + BASE + "/th/updates/" + DAY + ".html");
        // A Russian reader gets the English note.
        assertThat(texts(russian)).containsExactly("Обновлено руководство для персонала: Gym hours. You can now set the hours.\n" + BASE + "/en/updates/" + DAY + ".html");
        assertThat(texts(bystander)).isEmpty();
        assertThat(notificationsOf(director).get(0).getType()).isEqualTo("MANUAL_UPDATED");
        assertThat(notificationsOf(director).get(0).getDedupKey()).isEqualTo("MANUAL_UPDATE:" + DAY);
    }

    @Test
    void aThaiReader_getsTheEnglishNote_whenThereIsNoThaiOne() {
        speaking("TH", director);
        publish(englishOnly(DAY, DAY, "Gym hours", "New."));

        manualUpdates.runOnce();

        assertThat(texts(director)).containsExactly("คู่มือพนักงานมีการอัปเดต: Gym hours. New.\n" + BASE + "/en/updates/" + DAY + ".html");
    }

    @Test
    void anotherPass_andACorrectionOfTheNote_sendNothingMore() {
        publish(note(DAY, DAY, "Gym hours", "First wording."));
        manualUpdates.runOnce();
        manualUpdates.runOnce();
        publish(note(DAY, DAY, "Gym hours", "Corrected wording, a second change of the day added."));
        manualUpdates.runOnce();
        clock.set(TOMORROW + "T10:00");
        manualUpdates.runOnce();

        assertThat(texts(director)).hasSize(1);
        assertThat(texts(director).get(0)).contains("First wording.");
    }

    @Test
    void twoPassesAtTheSameMoment_raiseOneMessage() throws Exception {
        publish(note(DAY, DAY, "Gym hours", "New."), note(YESTERDAY, YESTERDAY, "Spa", "New too."));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier together = new CyclicBarrier(2);
        try {
            List<Future<?>> passes = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                passes.add(pool.submit(() -> {
                    together.await(10, TimeUnit.SECONDS);
                    manualUpdates.runOnce();
                    return null;
                }));
            }
            for (Future<?> pass : passes) {
                pass.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(texts(director)).hasSize(1);
    }

    // ---- no history -----------------------------------------------------------------------------------

    @Test
    void onTheFirstRun_onlyNotesFromYesterdayOn_areSent() {
        publish(note("2033-05-15", "2033-05-15", "Two days ago", "Old."), note("2033-05-09-week", "2033-05-09", "A week", "Older."));
        manualUpdates.runOnce();
        assertThat(texts(director)).isEmpty();

        publish(note(YESTERDAY, YESTERDAY, "Yesterday", "Recent."), note("2033-05-15", "2033-05-15", "Two days ago", "Old."));
        manualUpdates.runOnce();

        assertThat(texts(director)).hasSize(1);
        assertThat(texts(director).get(0)).contains("Yesterday. Recent.").doesNotContain("Two days ago");
    }

    // ---- one message a day ----------------------------------------------------------------------------

    @Test
    void twoNotesThatAreNewTogether_shareOneMessage() {
        publish(note(DAY, DAY, "17 May 2033", "The daily one."), note("2033-05-16-week", YESTERDAY, "Week of 16 May", "The weekly one."));

        manualUpdates.runOnce();

        assertThat(texts(director)).containsExactly("Staff manual updated: 17 May 2033. The daily one.\n" + BASE + "/en/updates/" + DAY + ".html\n\n"
                + "Week of 16 May. The weekly one.\n" + BASE + "/en/updates/2033-05-16-week.html");
        manualUpdates.runOnce();
        assertThat(texts(director)).hasSize(1);
    }

    @Test
    void aSecondNoteTheSameDay_waitsForTheNextDay() {
        publish(note(YESTERDAY, YESTERDAY, "Yesterday", "A."));
        manualUpdates.runOnce();
        publish(note(DAY, DAY, "Today", "B."), note(YESTERDAY, YESTERDAY, "Yesterday", "A."));
        clock.set(DAY + "T18:00");
        manualUpdates.runOnce();
        assertThat(texts(director)).hasSize(1);

        clock.set(TOMORROW + "T10:00");
        manualUpdates.runOnce();

        assertThat(texts(director)).hasSize(2);
        assertThat(texts(director).get(1)).contains("Today. B.").doesNotContain("Yesterday");
    }

    @Test
    void aHeldNoteStillGoes_thoughByThenItIsTwoDaysOld() {
        publish(note(DAY, DAY, "Today", "A."));
        manualUpdates.runOnce();
        publish(note("2033-05-16-week", YESTERDAY, "Week", "Late."), note(DAY, DAY, "Today", "A."));
        manualUpdates.runOnce();
        assertThat(texts(director)).hasSize(1);

        clock.set(TOMORROW + "T10:00");
        manualUpdates.runOnce();

        assertThat(texts(director)).hasSize(2);
        assertThat(texts(director).get(1)).contains("Week. Late.");
    }

    @Test
    void aMessageHeldByQuietHours_isTheNextMorningsMessage() {
        clock.set(DAY + "T23:00");
        publish(note(DAY, DAY, "Night", "A."));
        manualUpdates.runOnce();
        NotificationEntity raised = notificationsOf(director).get(0);
        assertThat(raised.getText()).contains("Night. A.");

        // Delivered on the 18th at 07:00 - so a note of the 18th waits for the 19th.
        clock.set(TOMORROW + "T10:00");
        publish(note(TOMORROW, TOMORROW, "Morning", "B."), note(DAY, DAY, "Night", "A."));
        manualUpdates.runOnce();
        assertThat(texts(director)).hasSize(1);

        clock.set("2033-05-19T10:00");
        manualUpdates.runOnce();
        assertThat(texts(director)).hasSize(2);
    }

    @Test
    void quietHoursHoldTheDelivery() {
        link(director);
        clock.set(DAY + "T23:00");
        publish(note(DAY, DAY, "Night", "A."));

        manualUpdates.runOnce();
        deliveryService.runOnce();

        assertThat(telegram.sent).isEmpty();
        clock.set(TOMORROW + "T07:01");
        deliveryService.runOnce();
        assertThat(telegram.sent).hasSize(1);
    }

    // ---- nothing to do --------------------------------------------------------------------------------

    @Test
    void switchedOff_nothingIsRaised_andNothingIsSavedUp() {
        settings(s -> s.setManualUpdatesEnabled(false));
        publish(note(DAY, DAY, "While off", "A."));
        manualUpdates.runOnce();
        assertThat(texts(director)).isEmpty();

        settings(s -> s.setManualUpdatesEnabled(true));
        manualUpdates.runOnce();
        assertThat(texts(director)).isEmpty();
    }

    @Test
    void switchingBackOn_skipsWhatTheSweepHadNotYetSeen() {
        actAs(director);
        NotificationSettingsUpdateInput off = input().manualUpdatesEnabled(false);
        assertThat(settingsController.updateNotificationSettings(off).getBody().getManualUpdatesEnabled()).isFalse();
        publish(note(DAY, DAY, "While off", "A.")); // no pass in between

        settingsController.updateNotificationSettings(input().manualUpdatesEnabled(true));
        manualUpdates.runOnce();

        assertThat(texts(director)).isEmpty();
    }

    @Test
    void aSaveWithoutTheField_keepsIt_andAChangeIsAudited() {
        actAs(director);
        assertThat(settingsService.update(input().manualUpdatesEnabled(false)).getManualUpdatesEnabled()).isFalse();
        assertThat(auditLogRepository.findAll().stream().map(a -> a.getSummary()))
                .anyMatch(s -> s.startsWith("Notification settings:") && s.contains("staff manual update alerts true -> false"));

        assertThat(settingsService.update(input()).getManualUpdatesEnabled()).isFalse();
    }

    @Test
    void withNoDirectors_nothingIsRaised_andOneChosenTheSameDayStillHears() {
        directors();
        publish(note(DAY, DAY, "Today", "A."));
        manualUpdates.runOnce();
        assertThat(notificationRepository.findAll()).noneMatch(n -> n.getType().equals("MANUAL_UPDATED"));

        directors(director);
        manualUpdates.runOnce();
        assertThat(texts(director)).hasSize(1);
    }

    @Test
    void aMissingOrBrokenFile_raisesNothing_throwsNothing_andLeavesTheLastReadTimeAlone() {
        publish(englishOnly("2033-05-01", "2033-05-01", "Old", "Old."));
        manualUpdates.runOnce();
        var readAt = channelService.status().getManualFeedReadAt().get();
        assertThat(channelService.status().getManualFeedConfigured()).isTrue();

        clock.set(DAY + "T11:00");
        clean();
        manualUpdates.runOnce(); // missing
        for (String broken : List.of("", "not json", "{}", "{\"entries\": 5}", "{\"entries\": [{\"id\": \"x\"}]}")) {
            write(broken);
            manualUpdates.runOnce();
        }
        assertThat(texts(director)).isEmpty();
        assertThat(channelService.status().getManualFeedReadAt().get()).isEqualTo(readAt);

        publish(note(DAY, DAY, "Back", "A."));
        manualUpdates.runOnce();
        assertThat(texts(director)).hasSize(1);
        assertThat(channelService.status().getManualFeedReadAt().get()).isAfter(readAt);
    }

    @Test
    void anEntryThatPointsOutsideTheManual_isLeftOut() {
        publish(
                note(DAY, DAY, "Good", "A."),
                note("evil-1", DAY, "Elsewhere", "B.").replace("en/updates/evil-1.html", "https://example.com/x").replace("th/updates/evil-1.html", "//example.com/x"),
                note("evil-2", DAY, "Upwards", "C.").replace("en/updates/evil-2.html", "en/../../x").replace("th/updates/evil-2.html", "/etc/x"),
                "{\"id\": \"no-date\", \"title\": {\"en\": \"t\"}, \"page\": {\"en\": \"en/x.html\"}}");

        manualUpdates.runOnce();

        assertThat(texts(director)).containsExactly("Staff manual updated: Good. A.\n" + BASE + "/en/updates/" + DAY + ".html");
    }

    @Test
    void withoutAFileConfigured_theFeedIsOff() {
        ManualUpdateFeed off = new ManualUpdateFeed("  ", clock, JsonMapper.builder().build(), 600_000);

        assertThat(off.isConfigured()).isFalse();
        assertThat(off.unreadable()).isFalse();
        assertThat(off.read()).isEmpty();
        assertThat(off.lastReadAt()).isNull();
    }

    // ---- what the settings page is told -----------------------------------------------------------------

    @Test
    void theStatusSaysWhenTheConfiguredListCannotBeReliedOn() {
        ManualUpdateFeed feed = new ManualUpdateFeed(MANUAL_FEED.toString(), clock, JsonMapper.builder().build(), 600_000);
        assertThat(feed.unreadable()).as("just started").isFalse();
        clock.set(DAY + "T10:21");
        assertThat(feed.unreadable()).as("no read at all for two intervals").isTrue();

        publish(note(DAY, DAY, "Back", "A."));
        feed.read();
        assertThat(feed.unreadable()).isFalse();
        assertThat(feed.failingSince()).isNull();

        write("broken");
        feed.read();
        assertThat(feed.unreadable()).as("the last read failed").isTrue();
        var since = feed.failingSince();
        clock.set(DAY + "T10:31");
        feed.read();
        assertThat(feed.failingSince()).isEqualTo(since);

        publish(note(DAY, DAY, "Back", "A."));
        feed.read();
        assertThat(feed.unreadable()).isFalse();
        clock.set(DAY + "T10:52");
        assertThat(feed.unreadable()).as("the sweep stopped reading").isTrue();
    }

    @Test
    void theStatusEndpointCarriesTheFailureTime_andNeverThePath() throws Exception {
        write("broken");
        manualUpdates.runOnce();

        var status = channelService.status();

        assertThat(status.getManualFeedUnreadable()).isTrue();
        assertThat(status.getManualFeedFailingSince().isPresent()).isTrue();
        assertThat(JsonMapper.builder().build().writeValueAsString(status.getManualFeedFailingSince().get())).doesNotContain("sunset-test-updates");
        publish(note(DAY, DAY, "Back", "A."));
        manualUpdates.runOnce();
        assertThat(channelService.status().getManualFeedUnreadable()).isFalse();
        assertThat(channelService.status().getManualFeedFailingSince().isPresent()).isFalse();
    }

    // ---- fixtures -------------------------------------------------------------------------------------

    private void directors(UserEntity... users) {
        settings(s -> s.setDirectorUserIds(java.util.Arrays.stream(users).map(UserEntity::getId).toArray(String[]::new)));
    }

    private UserEntity speaking(String language, UserEntity user) {
        user.setNotificationLanguage(language);
        return userRepository.saveAndFlush(user);
    }

    private List<String> texts(UserEntity user) {
        return notificationsOf(user, "MANUAL_UPDATED").stream().map(NotificationEntity::getText).toList();
    }

    private NotificationSettingsUpdateInput input() {
        NotificationSettings now = settingsService.get();
        return new NotificationSettingsUpdateInput(
                now.getLateEnabled(), settingsService.minLateGraceMinutes(), now.getAbsenceEnabled(), 120, true, true, true, true, 15, true, "22:00", "07:00",
                "09:00", now.getDefaultLanguage(), now.getLateRecipientUserIds(), now.getDirectorUserIds());
    }

    private static String note(String id, String date, String title, String summary) {
        return ("{\"id\": \"%1$s\", \"date\": \"%2$s\", \"title\": {\"en\": \"%3$s\", \"th\": \"%3$s TH\"}, \"summary\": {\"en\": \"%4$s\", \"th\": \"%4$s TH\"}, "
                + "\"page\": {\"en\": \"en/updates/%1$s.html\", \"th\": \"th/updates/%1$s.html\"}}").formatted(id, date, title, summary);
    }

    private static String englishOnly(String id, String date, String title, String summary) {
        return ("{\"id\": \"%1$s\", \"date\": \"%2$s\", \"title\": {\"en\": \"%3$s\"}, \"summary\": {\"en\": \"%4$s\"}, \"page\": {\"en\": \"en/updates/%1$s.html\"}}")
                .formatted(id, date, title, summary);
    }

    private static void publish(String... notes) {
        write("{\"version\": 1, \"entries\": [" + String.join(",", notes) + "]}");
    }

    private static void write(String content) {
        try {
            Files.writeString(MANUAL_FEED, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
